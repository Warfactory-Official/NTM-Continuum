package com.hbm.tileentity.machine;

import com.hbm.api.environment.Environment;
import com.hbm.inventory.fluid.FluidCombustion;
import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.util.TickPhase;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.container.ContainerMachineWoodBurner;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.module.ModuleBurnTime;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * Wood burner, ported from the 1.12.2 {@code TileEntityMachineWoodBurner}. It remains separate from
 * {@link TileEntityFireboxBase}: the old machine has a liquid mode, two ports, a battery bay, and ash
 * output in addition to its fuel slot.
 */
public class TileEntityMachineWoodBurner extends TileEntityMachineBase
        implements MenuProvider, IEnergyProviderMK2, IFluidStandardReceiverMK2, WorldlyContainer {

    public static final long maxPower = 100_000L;
    public static final ModuleBurnTime burnModule = new ModuleBurnTime().setLogTimeMod(4).setWoodTimeMod(2);

    public long power;
    public int burnTime;
    public int maxBurnTime;
    public boolean liquidBurn;
    public boolean isOn;
    public int powerGen;
    public int ashLevelWood;
    public int ashLevelCoal;
    public int ashLevelMisc;
    public int playersUsing;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.WOODOIL, 16_000);

    public TileEntityMachineWoodBurner(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_WOOD_BURNER.get(), pos, state, 6);
    }

    @Override public String getDefaultName() { return "container.machineWoodBurner"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineWoodBurner te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPOFIFO();

        te.powerGen = 0;
        if (te.tank.setType(te.inventory, 2, 2)) te.setChanged();
        if (MachineFluidContainerIO.load(te.tank, te.inventory, 3, 4)) te.setChanged();
        te.power = Library.chargeItemsFromTE(te.inventory, 5, te.power, maxPower);

        if (!te.liquidBurn) {
            if (te.burnTime <= 0) {
                ItemStack fuel = te.inventory.getStackInSlot(0);
                int burn = te.burnModule.getBurnTime(level, fuel);
                if (burn > 0) {
                    EnumAshType type = TileEntityFireboxBase.getAshFromFuel(fuel);
                    if (type == EnumAshType.WOOD) te.ashLevelWood += burn;
                    else if (type == EnumAshType.COAL) te.ashLevelCoal += burn;
                    else if (type == EnumAshType.MISC) te.ashLevelMisc += burn;

                    int threshold = 2_000;
                    while (te.processAsh(te.ashLevelWood, EnumAshType.WOOD, threshold)) te.ashLevelWood -= threshold;
                    while (te.processAsh(te.ashLevelCoal, EnumAshType.COAL, threshold)) te.ashLevelCoal -= threshold;
                    while (te.processAsh(te.ashLevelMisc, EnumAshType.MISC, threshold)) te.ashLevelMisc -= threshold;

                    te.maxBurnTime = te.burnTime = burn;
                    ItemStack remainder = Compat.craftingRemainder(fuel);
                    fuel.shrink(1);
                    if (fuel.isEmpty()) te.inventory.setStackInSlot(0, remainder);
                    te.setChanged();
                }
            // hbm-space: isOn && breatheAir(1), here and for liquid fuel (com.hbm.api.environment.Environment)
            } else if (te.power < maxPower && te.isOn && Environment.breatheAir(level, pos, 1)) {
                te.burnTime--;
                te.powerGen += 100;
                if (TickPhase.every(level, pos, 20L)) PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);
            }
        } else if (te.power < maxPower && te.tank.getFill() > 0 && te.isOn
                && Environment.breatheAir(level, pos, 1)) {
            long heatEnergy = FluidCombustion.getFlammableEnergy(te.tank.getTankType());
            if (heatEnergy > 0) {
                int toBurn = Math.min(te.tank.getFill(), 2);
                te.powerGen += (int) (heatEnergy * toBurn / 2_000L);
                te.tank.setFill(te.tank.getFill() - toBurn);
                if (TickPhase.every(level, pos, 20L)) PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * toBurn / 2F);
            }
        }

        te.power = Math.min(maxPower, te.power + te.powerGen);
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineWoodBurner te) {
        if (te.powerGen <= 0) return;
        ForgeDirection dir = te.facing(state);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + 0.5 - dir.offsetX + rot.offsetX, pos.getY() + 4,
                pos.getZ() + 0.5 - dir.offsetZ + rot.offsetZ, 0, 0.05, 0);
    }

    protected PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = facing(getBlockState());
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x - dir.offsetX, y, z - dir.offsetZ, dir.getOpposite()),
                    PortDef.make(x - dir.offsetX + rot.offsetX, y, z - dir.offsetZ + rot.offsetZ, dir.getOpposite()),
            };
        }
        return cachedPorts;
    }

    private ForgeDirection facing(BlockState state) {
        return MachineCore.facing(state);
    }

    private boolean processAsh(int level, EnumAshType type, int threshold) {
        if (level < threshold) return false;
        ItemStack ash = new ItemStack(ModIngredientItems.ash(type));
        ItemStack existing = inventory.getStackInSlot(1);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(1, ash);
            return true;
        }
        if (existing.is(ash.getItem()) && existing.getCount() < existing.getMaxStackSize()) {
            existing.grow(1);
            inventory.setStackInSlot(1, existing);
            return true;
        }
        return false;
    }

    public void handleButton(int id) {
        if (id == 0) isOn = !isOn;
        if (id == 1) liquidBurn = !liquidBurn;
        setChanged();
    }

    @Override public void handleButtonPacket(int value, int meta) { handleButton(value); }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return level != null && burnModule.getBurnTime(level, stack) > 0;
        if (slot == 2) return stack.getItem() instanceof IItemFluidId;
        if (slot == 5) return Library.isBattery(stack);
        return true;
    }

    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0, 1}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }
    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public boolean canConnect(ForgeDirection direction) { return direction == facing(getBlockState()).getOpposite(); }
    @Override public boolean canConnect(FluidType type, ForgeDirection direction) { return canConnect(direction); }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    @Override public Component getDisplayName() { return Component.translatable("container.machineWoodBurner"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineWoodBurner(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(burnTime);
        buf.writeInt(powerGen);
        buf.writeInt(maxBurnTime);
        buf.writeBoolean(isOn);
        buf.writeBoolean(liquidBurn);
        buf.writeInt(ashLevelWood);
        buf.writeInt(ashLevelCoal);
        buf.writeInt(ashLevelMisc);
        buf.writeInt(playersUsing);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        burnTime = buf.readInt();
        powerGen = buf.readInt();
        maxBurnTime = buf.readInt();
        isOn = buf.readBoolean();
        liquidBurn = buf.readBoolean();
        ashLevelWood = buf.readInt();
        ashLevelCoal = buf.readInt();
        ashLevelMisc = buf.readInt();
        playersUsing = buf.readInt();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("burnTime", burnTime);
        output.putInt("maxBurnTime", maxBurnTime);
        output.putBoolean("isOn", isOn);
        output.putBoolean("liquidBurn", liquidBurn);
        output.putInt("ashLevelWood", ashLevelWood);
        output.putInt("ashLevelCoal", ashLevelCoal);
        output.putInt("ashLevelMisc", ashLevelMisc);
        tank.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        burnTime = input.getIntOr("burnTime", 0);
        maxBurnTime = input.getIntOr("maxBurnTime", 0);
        isOn = input.getBooleanOr("isOn", false);
        liquidBurn = input.getBooleanOr("liquidBurn", false);
        ashLevelWood = input.getIntOr("ashLevelWood", 0);
        ashLevelCoal = input.getIntOr("ashLevelCoal", 0);
        ashLevelMisc = input.getIntOr("ashLevelMisc", 0);
        tank.readFromNBT(input, "tank");
    }
}
