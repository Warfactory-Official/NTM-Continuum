package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.container.ContainerMachineF6Tank;
import com.hbm.items.machine.ItemFluidBucket;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Common fixed-fluid tank implementation used by the UF6 and PuF6 variants. */
public abstract class TileEntityMachineF6Tank extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardTransceiverMK2, WorldlyContainer {

    public static final int CAPACITY = 64_000;
    private static final int[] IO_SLOTS = {0, 2, 1, 3};

    public final FluidTankNTM tank;
    public byte lastRedstone;

    protected TileEntityMachineF6Tank(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                      BlockPos pos, BlockState state, FluidType fluid) {
        super(type, pos, state, 4);
        this.tank = new FluidTankNTM(fluid, CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineF6Tank tank) {
        for (DirPos connection : tank.getConPos()) {
            if (tank.tank.getFill() > 0) tank.tryProvide(tank.tank, level, connection);
        }

        tank.loadTank(0, 1);
        tank.unloadTank(2, 3);

        byte comparator = tank.tank.getRedstoneComparatorPower();
        if (comparator != tank.lastRedstone) {
            level.updateNeighborsAt(pos, state.getBlock());
            tank.lastRedstone = comparator;
        }

        tank.setChanged();
        tank.networkPackNT();
    }

    private void loadTank(int inSlot, int outSlot) {
        ItemStack input = inventory.getStackInSlot(inSlot);
        if (input.isEmpty()) return;

        if (input.getItem() instanceof IFillableItem fillable) {
            FluidType heldType = fillable.getFirstFluidType(input);
            int held = fillable.getFill(input);
            if (heldType == null || heldType == Fluids.NONE || held <= 0 || heldType != tank.getTankType()) return;

            int accepted = tank.fill(heldType, held, false);
            if (accepted <= 0) return;
            ItemStack result = single(input);
            int emptied = fillable.tryEmpty(heldType, accepted, result);
            if (emptied > 0 && fits(outSlot, result)) {
                tank.fill(heldType, emptied, true);
                moveResult(inSlot, outSlot, result);
            }
            return;
        }

        FluidType bucketFluid = bucketFluid(input);
        if (bucketFluid != tank.getTankType()
                || tank.fill(bucketFluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
        ItemStack empty = new ItemStack(Items.BUCKET);
        if (fits(outSlot, empty)) {
            tank.fill(bucketFluid, ItemFluidBucket.VOLUME, true);
            moveResult(inSlot, outSlot, empty);
        }
    }

    private void unloadTank(int inSlot, int outSlot) {
        ItemStack input = inventory.getStackInSlot(inSlot);
        if (input.isEmpty() || tank.getFill() <= 0) return;

        if (input.getItem() instanceof IFillableItem fillable) {
            if (!fillable.acceptsFluid(tank.getTankType(), input)) return;
            ItemStack result = single(input);
            int remainder = fillable.tryFill(tank.getTankType(), tank.getFill(), result);
            int moved = tank.getFill() - remainder;
            if (moved > 0 && fits(outSlot, result)) {
                tank.setFill(tank.getFill() - moved);
                moveResult(inSlot, outSlot, result);
            }
            return;
        }

        if (!input.is(Items.BUCKET) || tank.getFill() < ItemFluidBucket.VOLUME) return;
        ItemStack filled = ItemFluidBucket.make(tank.getTankType());
        if (fits(outSlot, filled)) {
            tank.setFill(tank.getFill() - ItemFluidBucket.VOLUME);
            moveResult(inSlot, outSlot, filled);
        }
    }

    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        return Fluids.NONE;
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private boolean fits(int slot, ItemStack stack) {
        return inventory.insertItem(slot, stack.copy(), true).isEmpty();
    }

    private void moveResult(int inSlot, int outSlot, ItemStack result) {
        inventory.extractItem(inSlot, 1, false);
        inventory.insertItem(outSlot, result, false);
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

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 || slot == 2 ? isFluidContainer(stack) : false;
    }

    private static boolean isFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof IFillableItem
                || stack.getItem() instanceof ItemFluidBucket
                || stack.is(Items.BUCKET);
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
