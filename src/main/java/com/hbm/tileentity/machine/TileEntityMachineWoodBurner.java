package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.container.ContainerMachineWoodBurner;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wood burner, ported from the 1.12.2 {@code TileEntityMachineWoodBurner}. It remains separate from
 * {@link TileEntityFireboxBase}: the old machine has a liquid mode, two ports, a battery bay, and ash
 * output in addition to its fuel slot.
 */
public class TileEntityMachineWoodBurner extends TileEntityMachineBase
        implements MenuProvider, IEnergyProviderMK2, IFluidStandardReceiverMK2 {

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
        te.powerGen = 0;
        te.setTypeFromFluidId();
        te.loadTank();
        te.power = Library.chargeItemsFromTE(te.inventory, 5, te.power, maxPower);

        for (DirPos connection : te.getConPos()) {
            if (te.power > 0) te.tryProvide(level, connection.getPos(), connection.getDir());
        }

        if (!te.liquidBurn) {
            if (te.burnTime <= 0) {
                ItemStack fuel = te.inventory.getStackInSlot(0);
                int burn = te.burnModule.getBurnTime(level, fuel);
                if (burn > 0) {
                    EnumAshType type = getAshFromFuel(fuel);
                    if (type == EnumAshType.WOOD) te.ashLevelWood += burn;
                    else if (type == EnumAshType.COAL) te.ashLevelCoal += burn;
                    else te.ashLevelMisc += burn;

                    int threshold = 2_000;
                    while (te.processAsh(EnumAshType.WOOD, threshold)) te.ashLevelWood -= threshold;
                    while (te.processAsh(EnumAshType.COAL, threshold)) te.ashLevelCoal -= threshold;
                    while (te.processAsh(EnumAshType.MISC, threshold)) te.ashLevelMisc -= threshold;

                    te.maxBurnTime = te.burnTime = burn;
                    ItemStack remainder = Compat.craftingRemainder(fuel);
                    fuel.shrink(1);
                    if (fuel.isEmpty()) te.inventory.setStackInSlot(0, remainder);
                    te.setChanged();
                }
            } else if (te.power < maxPower && te.isOn) {
                te.burnTime--;
                te.powerGen += 100;
            }
        } else if (te.power < maxPower && te.tank.getFill() > 0 && te.isOn) {
            long heatEnergy = com.hbm.inventory.fluid.FluidCombustion.getFlammableEnergy(te.tank.getTankType());
            if (heatEnergy > 0) {
                int toBurn = Math.min(te.tank.getFill(), 2);
                te.powerGen += (int) (heatEnergy * toBurn / 2_000L);
                te.tank.setFill(te.tank.getFill() - toBurn);
            }
        }

        te.power = Math.min(maxPower, te.power + te.powerGen);
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineWoodBurner te) {
        if (te.powerGen <= 0) return;
        ForgeDirection dir = te.facing(state);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                pos.getX() + 0.5 - dir.offsetX + rot.offsetX, pos.getY() + 4,
                pos.getZ() + 0.5 - dir.offsetZ + rot.offsetZ, 0, 0.05, 0);
    }

    public DirPos[] getConPos() {
        ForgeDirection dir = facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x - dir.offsetX * 2, y, z - dir.offsetZ * 2, rot.getOpposite()),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite())
        };
    }

    private ForgeDirection facing(BlockState state) {
        return MachineCore.facing(state);
    }

    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(2);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType type = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (type != tank.getTankType()) {
            tank.setTankType(type);
            setChanged();
        }
    }

    private void loadTank() {
        ItemStack in = inventory.getStackInSlot(3);
        if (in.isEmpty()) return;

        if (in.getItem() instanceof IFillableItem fillable) {
            FluidType heldType = fillable.getFirstFluidType(in);
            int held = fillable.getFill(in);
            if (held <= 0 || heldType == null || heldType == Fluids.NONE) return;
            int addable = tank.fill(heldType, held, false);
            if (addable <= 0) return;
            ItemStack work = in.copy();
            work.setCount(1);
            int taken = fillable.tryEmpty(heldType, addable, work);
            if (taken <= 0 || !inventory.insertItem(4, work.copy(), true).isEmpty()) return;
            tank.fill(heldType, taken, true);
            inventory.extractItem(3, 1, false);
            inventory.insertItem(4, work, false);
            setChanged();
        } else if (in.getItem() instanceof ItemFluidBucket) {
            FluidType type = ItemFluidBucket.getFluid(in);
            if (type == Fluids.NONE || tank.fill(type, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!inventory.insertItem(4, empty.copy(), true).isEmpty()) return;
            tank.fill(type, ItemFluidBucket.VOLUME, true);
            inventory.extractItem(3, 1, false);
            inventory.insertItem(4, empty, false);
            setChanged();
        }
    }

    public static EnumAshType getAshFromFuel(ItemStack stack) {
        if (stack.isEmpty()) return EnumAshType.MISC;
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (stack.is(net.minecraft.tags.ItemTags.LOGS) || path.contains("wood") || stack.is(net.minecraft.tags.ItemTags.PLANKS)) {
            return EnumAshType.WOOD;
        }
        if (path.contains("coal") || path.contains("coke") || path.contains("lignite")) return EnumAshType.COAL;
        return EnumAshType.MISC;
    }

    private boolean processAsh(EnumAshType type, int threshold) {
        ItemStack ash = new ItemStack(ModIngredientItems.ash(type));
        for (int i = 0; i < 5; i++) {
            ItemStack existing = inventory.getStackInSlot(i);
            if (existing.isEmpty()) {
                inventory.setStackInSlot(i, ash.copy());
                return true;
            }
            if (existing.is(ash.getItem()) && existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(1);
                inventory.setStackInSlot(i, existing);
                return true;
            }
        }
        return false;
    }

    public void handleButton(int id) {
        if (id == 0) isOn = !isOn;
        if (id == 1) liquidBurn = !liquidBurn;
        setChanged();
    }

    @Override public void handleButtonPacket(int value, int meta) { handleButton(value); }

    public boolean isDrainable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack) != Fluids.NONE;
        if (!(stack.getItem() instanceof IFillableItem fillable)) return false;
        FluidType type = fillable.getFirstFluidType(stack);
        return fillable.getFill(stack) > 0 && type != null && type != Fluids.NONE;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return level != null && burnModule.getBurnTime(level, stack) > 0;
        if (slot == 2) return stack.getItem() instanceof IItemFluidId;
        if (slot == 3) return isDrainable(stack);
        if (slot == 5) return Library.isBattery(stack);
        return true;
    }

    @Override public int[] getAccessibleSlotsFromSide(int side) { return new int[]{0, 1}; }
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
