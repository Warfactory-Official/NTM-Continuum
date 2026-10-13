package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.container.ContainerMachineF6Tank;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityMachineBase;
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

/**
 * Common fixed-fluid tank implementation used by the UF6 and PuF6 variants. port: 1.7's UF6/PuF6 tiles are empty, so
 * the container slots follow the barrel's {@code loadTank(in, out)} / {@code unloadTank(in, out)} on pairs 0/1 and 2/3.
 */
public abstract class TileEntityMachineF6Tank extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardTransceiverMK2, WorldlyContainer {

    public static final int CAPACITY = 64_000;
    private static final int[] IO_SLOTS = {0, 2, 1, 3};

    public final FluidTankNTM tank;
    public byte lastRedstone;

    protected TileEntityMachineF6Tank(BlockEntityType<?> type,
                                      BlockPos pos, BlockState state, FluidType fluid) {
        super(type, pos, state, 4);
        this.tank = new FluidTankNTM(fluid, CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineF6Tank tank) {
        for (DirPos connection : tank.getConPos()) {
            if (tank.tank.getFill() > 0) tank.tryProvide(tank.tank, level, connection);
        }

        MachineFluidContainerIO.load(tank.tank, tank.inventory, 0, 1);
        MachineFluidContainerIO.unload(tank.tank, tank.inventory, 2, 3);

        byte comparator = tank.tank.getRedstoneComparatorPower();
        if (comparator != tank.lastRedstone) {
            level.updateNeighborsAt(pos, state.getBlock());
            tank.lastRedstone = comparator;
        }

        tank.setChanged();
        tank.networkPackNT();
    }

    public DirPos[] getConPos() {
        BlockPos pos = getBlockPos();
        return new DirPos[]{
                new DirPos(pos.east(), Library.POS_X),
                new DirPos(pos.west(), Library.NEG_X),
                new DirPos(pos.south(), Library.POS_Z),
                new DirPos(pos.north(), Library.NEG_Z),
                new DirPos(pos.above(), Library.POS_Y),
                new DirPos(pos.below(), Library.NEG_Y)
        };
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN && type == tank.getTankType();
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    /** Either input slot takes any container a loader handles, full or empty. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return (slot == 0 || slot == 2)
                && (TileEntityMachineFluidTank.isDrainable(tank, stack) || TileEntityMachineFluidTank.isFillable(tank, stack));
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IO_SLOTS;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return IO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot == 0 || slot == 2) && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 || slot == 3;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineF6Tank(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tank.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tank.readFromNBT(input, "tank");
    }
}
