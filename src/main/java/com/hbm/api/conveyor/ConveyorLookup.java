package com.hbm.api.conveyor;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The position-keyed lookup ntm-next's {@code NtmContracts.CONVEYOR_BELT.at} / {@code ENTERABLE.at} gave
 * the moving conveyor objects: the block itself, else its block entity, else the folded multiblock core
 * that owns the cell at {@code pos}. This port has no loader capability bus for block contracts, so the
 * three tests are done directly; a hit is what 1.7 got from {@code instanceof} on the one block class.
 */
public final class ConveyorLookup {

    private ConveyorLookup() {}

    public static <T> @Nullable T at(Class<T> type, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (type.isInstance(block)) return type.cast(block);
        if (state.hasBlockEntity()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (type.isInstance(be)) return type.cast(be);
        }
        if (MultiblockSurface.isFoldedCell(state)) {
            BlockMultiblockCore.FoldedOwner owner = MultiblockSurface.ownerOf(level, pos);
            if (owner != null) {
                if (type.isInstance(owner.block())) return type.cast(owner.block());
                BlockEntity be = level.getBlockEntity(owner.pos());
                if (type.isInstance(be)) return type.cast(be);
            }
        }
        return null;
    }
}
