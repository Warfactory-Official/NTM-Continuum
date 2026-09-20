package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.TileEntityCraneInserter;
import com.hbm.util.ConveyorInventories;

/**
 * 1.7 {@code blocks/network/CraneInserter}: takes what a belt hands it and puts it into the inventory on its output
 * side, buffering what will not fit.
 */
public class CraneInserter extends BlockCraneBase implements IEnterableBlock {

    public CraneInserter(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCraneInserter(pos, state);
    }

    @Override
    public boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        return inputSide(level.getBlockState(pos)) == dir;
    }

    @Override
    public void onItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        ItemStack carried = entity.getItemStack();
        if (carried.isEmpty()) return;

        Direction output = getOutputSide(level, pos);
        ItemStack toAdd = carried.copy();

        if (!level.hasNeighborSignal(pos)) {
            Container target = ConveyorInventories.containerAt(level, pos.relative(output));
            if (target != null) {
                ConveyorInventories.addToInventory(target, ConveyorInventories.masquerade(target, output.getOpposite()),
                        toAdd, output.getOpposite());
            }
        }

        buffer(level, pos, output, toAdd);
    }

    @Override
    public boolean canPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        return true;
    }

    /** 1.7: a package's stacks go through the neighbour in one pass and only then are the leftovers buffered. */
    @Override
    public void onPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        if (entity.getItemStacks().isEmpty()) return;

        Direction output = getOutputSide(level, pos);

        if (!level.hasNeighborSignal(pos)) {
            Container target = ConveyorInventories.containerAt(level, pos.relative(output));
            if (target != null) {
                int[] access = ConveyorInventories.masquerade(target, output.getOpposite());
                for (ItemStack stack : entity.getItemStacks()) {
                    ConveyorInventories.addToInventory(target, access, stack, output.getOpposite());
                }
            }
        }

        for (ItemStack stack : entity.getItemStacks()) buffer(level, pos, output, stack);
    }

    /** 1.7: the crane's own 21 slots take what the neighbour refused, and a destroyer eats the rest. */
    private static void buffer(Level level, BlockPos pos, Direction output, ItemStack toAdd) {
        if (toAdd.isEmpty()) return;
        if (!(level.getBlockEntity(pos) instanceof TileEntityCraneInserter inserter)) return;

        ConveyorInventories.addToInventory(inserter, null, toAdd, output.getOpposite());

        if (!toAdd.isEmpty() && !inserter.destroyer) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, toAdd.copy()));
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container c ? AbstractContainerMenu.getRedstoneSignalFromContainer(c) : 0;
    }
}
