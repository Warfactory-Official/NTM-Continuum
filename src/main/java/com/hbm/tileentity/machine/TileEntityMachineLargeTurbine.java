package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.client.sound.LargeTurbineSoundHandler;
import com.hbm.inventory.container.ContainerMachineLargeTurbine;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import com.hbm.tileentity.TilePort.PortDef;

/** Logic for the legacy four-long steam turbine. */
public class TileEntityMachineLargeTurbine extends TileEntityMachineBase
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, WorldlyContainer, MenuProvider {

    public static final long MAX_POWER = 100_000_000L;
    public static final double EFFICIENCY = 1D;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.STEAM, 512_000),
            new FluidTankNTM(Fluids.SPENTSTEAM, 10_240_000)
    };
    public long power;
    public int age;
    public float rotor;
    public float lastRotor;
    public float fanAcceleration;
    public boolean shouldTurn;

    private final float audioDesync;

    public TileEntityMachineLargeTurbine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_LARGE_TURBINE.get(), pos, state, 7);
        audioDesync = new Random().nextFloat() * 0.05F;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineLargeTurbine turbine) {
        turbine.setupAllPorts(turbine.getPorts());
        turbine.updatePortPOFIFO();

        turbine.age++;
        if (turbine.age >= 2) turbine.age = 0;

        turbine.updateTypeFromFluidId();
        if (MachineFluidContainerIO.load(turbine.tanks[0], turbine.inventory, 2, 3)) turbine.setChanged();
        turbine.power = Library.chargeItemsFromTE(turbine.inventory, 4, turbine.power, MAX_POWER);
        turbine.convert();
        if (turbine.power > MAX_POWER) turbine.power = MAX_POWER;
        if (MachineFluidContainerIO.unload(turbine.tanks[1], turbine.inventory, 5, 6)) turbine.setChanged();
        turbine.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineLargeTurbine turbine) {
        turbine.lastRotor = turbine.rotor;
        turbine.rotor += turbine.fanAcceleration;
        if (turbine.rotor >= 360F) {
            turbine.rotor -= 360F;
            turbine.lastRotor -= 360F;
        }

        if (turbine.shouldTurn) {
            turbine.fanAcceleration = Math.max(0F, Math.min(15F,
                    turbine.fanAcceleration + 0.075F + turbine.audioDesync));
        } else {
            turbine.fanAcceleration = Math.max(0F, Math.min(15F, turbine.fanAcceleration - 0.1F));
        }
        LargeTurbineSoundHandler.tickLoop(turbine);
    }

    private void updateTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(0);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        if (!inventory.getStackInSlot(1).isEmpty()) return;

        FluidType type = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (type == tanks[0].getTankType()) return;
        tanks[0].setTankType(type);
        tanks[1].setTankType(Fluids.NONE);
        inventory.insertItem(1, id.copy(), false);
        inventory.extractItem(0, 1, false);
        setChanged();
    }

    private void convert() {
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(tanks[0].getTankType());
        if (recipe == null || EFFICIENCY <= 0D) {
            tanks[1].setTankType(Fluids.NONE);
            return;
        }

        tanks[1].setTankType(recipe.output());
        int inputOps = tanks[0].getFill() / recipe.inputAmount();
        int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / recipe.outputAmount();
        int cap = (int) Math.ceil((double) tanks[0].getFill() / recipe.inputAmount() / 5F);
        int ops = Math.min(inputOps, Math.min(outputOps, cap));
        tanks[0].setFill(tanks[0].getFill() - ops * recipe.inputAmount());
        tanks[1].setFill(tanks[1].getFill() + ops * recipe.outputAmount());
        power += (long) (ops * recipe.heatEnergy() * EFFICIENCY);
        shouldTurn = ops > 0;
    }

    /** The container bays 2/5 take anything, as the 1.7 GUI's plain slots did. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidId;
            case 4 -> Library.isBattery(stack);
            default -> true;
        };
    }

    public ForgeDirection getFacing() {
        return MachineCore.facing(getBlockState());
    }

    protected PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = getFacing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + rot.offsetX * 1, y, z + rot.offsetZ * 1, rot),
                    PortDef.make(x - rot.offsetX * 1, y, z - rot.offsetZ * 1, rot.getOpposite()),
                    PortDef.make(x + dir.offsetX * 1, y, z + dir.offsetZ * 1, dir),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    /** Power leaves through the three fluid ports; 1.7 keeps the rear connector cell but no port there. */
    public DirPos[] getPowerPos() { return getConPos(); }

    /** 1.7 {@code getAccessibleSlotsFromSide}: no automation reaches the large turbine. */
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long value) { power = Math.max(0L, Math.min(MAX_POWER, value)); }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.machineLargeTurbine");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerMachineLargeTurbine(id, inventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(shouldTurn);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        shouldTurn = buf.readBoolean();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "water");
        tanks[1].writeToNBT(output, "steam");
        output.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "water");
        tanks[1].readFromNBT(input, "steam");
        power = input.getLongOr("power", 0L);
    }

    public long getPowerScaled(int size) {
        return power * size / MAX_POWER;
    }

    public float getAudioDesync() {
        return audioDesync;
    }

    public float getRotor(float partialTicks) {
        return lastRotor + (rotor - lastRotor) * partialTicks;
    }

    @Override
    public String getDefaultName() {
        return "container.machineLargeTurbine";
    }
}
