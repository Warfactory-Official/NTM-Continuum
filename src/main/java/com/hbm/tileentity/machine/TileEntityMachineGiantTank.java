package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.container.ContainerMachineGiantTank;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.server.level.ServerLevel;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;

/** Shared port of the old {@code TileEntityBarrel} behavior used by both giant storage machines. */
public abstract class TileEntityMachineGiantTank extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardTransceiverMK2, WorldlyContainer {

    private static final int[] SLOTS_TOP = {2};
    private static final int[] SLOTS_BOTTOM = {3, 5};
    private static final int[] SLOTS_SIDE = {4};

    /** 0 = receive, 1 = buffer, 2 = send, 3 = disabled. */
    public short mode;
    public byte lastRedstone;
    public boolean tilted;
    public final FluidTankNTM tankNew;

    protected TileEntityMachineGiantTank(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state, 6);
        this.tankNew = new FluidTankNTM(Fluids.NONE, capacity);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineGiantTank tank) {
        tank.setupFluidPorts(tank.getPorts());
        tank.updatePortFIFO();

        tank.tankNew.setType(tank.inventory, 0, 1);
        MachineFluidContainerIO.load(tank.tankNew, tank.inventory, 2, 3);
        MachineFluidContainerIO.unload(tank.tankNew, tank.inventory, 4, 5);

        byte comparator = tank.tankNew.getRedstoneComparatorPower();
        if (comparator != tank.lastRedstone) {
            for (PortDef port : tank.getPorts()) for (DirPos connection : port.portConnections) {
                level.updateNeighborsAt(connection.getPos(), state.getBlock());
            }
            tank.lastRedstone = comparator;
        }

        if (tank.tankNew.getFill() > 0 && tank.checkFluidInteraction(level)) return;
        tank.networkPackNT();
        tank.setChanged();
    }

    /** Retypes the tank; the ports follow the next tick. */
    public void setTankType(FluidType type) {
        if (type == null) type = Fluids.NONE;
        if (type == tankNew.getTankType()) return;
        tankNew.setTankType(type);
        setChanged();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1 -> stack.getItem() instanceof IItemFluidId;
            case 2 -> TileEntityMachineFluidTank.isDrainable(tankNew, stack);
            case 4 -> TileEntityMachineFluidTank.isFillable(tankNew, stack);
            default -> true;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? SLOTS_TOP : side == Direction.DOWN ? SLOTS_BOTTOM : SLOTS_SIDE;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == Direction.UP.ordinal() ? SLOTS_TOP
                : side == Direction.DOWN.ordinal() ? SLOTS_BOTTOM : SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 || slot == 2 || slot == 4 ? isItemValidForSlot(slot, stack) : false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 || slot == 3 || slot == 5;
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        if (isTilted() || mode == 2 || mode == 3 || tankNew.getPressure() != pressure) return 0;
        return type == tankNew.getTankType() ? tankNew.getMaxFill() - tankNew.getFill() : 0;
    }

    @Override
    public long getFluidAvailable(FluidType type, int pressure) {
        if (isTilted() || mode == 0 || mode == 3 || tankNew.getPressure() != pressure) return 0;
        return type == tankNew.getTankType() ? tankNew.getFill() : 0;
    }

    @Override
    public long getProviderSpeed(FluidType type, int pressure) {
        return (mode == 1 || mode == 2) ? 1_000_000_000 : 0;
    }

    @Override
    public long getReceiverSpeed(FluidType type, int pressure) {
        return (mode == 0 || mode == 1) ? 1_000_000_000 : 0;
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        return mode == 1 ? ConnectionPriority.LOW : ConnectionPriority.NORMAL;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        long moved = Math.min(getDemand(type, pressure), amount);
        tankNew.setFill(tankNew.getFill() + (int) moved);
        setChanged();
        return amount - moved;
    }

    protected boolean isTilted() {
        return tilted;
    }

    protected boolean checkFluidInteraction(Level level) {
        return false;
    }

    public abstract PortDef[] getPorts();

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    public ForgeDirection getFacing() {
        return MachineCore.facing(getBlockState());
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return new FluidTankNTM[]{tankNew};
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{tankNew};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tankNew};
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineGiantTank(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        buf.writeBoolean(tilted);
        tankNew.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        tilted = buf.readBoolean();
        tankNew.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putShort("mode", mode);
        output.putBoolean("tilted", tilted);
        tankNew.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        mode = (short) input.getShortOr("mode", (short) 0);
        tilted = input.getBooleanOr("tilted", false);
        tankNew.readFromNBT(input, "tank");
    }
}
