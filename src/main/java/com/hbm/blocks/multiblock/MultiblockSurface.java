package com.hbm.blocks.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Resolves folded cells to their owning core without the old metadata chain. */
public final class MultiblockSurface {

    private MultiblockSurface() {}

    public static @Nullable BlockMultiblockCore foldedCore(BlockState state) {
        return state.getBlock() instanceof BlockMultiblockCore core && !core.isCellState(state) ? core : null;
    }

    public static boolean isFoldedCell(BlockState state) {
        return state.getBlock() instanceof BlockMultiblockCell
                || state.getBlock() instanceof BlockMultiblockCore core && core.isCellState(state);
    }

    public static @Nullable BlockPos coreOfCell(Level level, BlockPos cell) {
        if (level instanceof ServerLevel server) {
            long packed = MultiblockCoreIndex.lookup(server, cell);
            if (packed == Long.MIN_VALUE) return null;
            BlockPos core = BlockPos.of(packed);
            return foldedCore(server.getBlockState(core)) == null ? null : core;
        }
        return clientCoreOf(level, cell);
    }

    public static BlockMultiblockCore.@Nullable FoldedOwner ownerOf(Level level, BlockPos cell) {
        BlockPos core = coreOfCell(level, cell);
        if (core == null) return null;
        BlockState state = level.getBlockState(core);
        BlockMultiblockCore block = foldedCore(state);
        return block == null ? null : new BlockMultiblockCore.FoldedOwner(core, state, block,
                state.getValue(BlockMultiblockCore.FACING));
    }

    /** The client has no server index, so validate the bounded geometry around the interacted cell. */
    private static @Nullable BlockPos clientCoreOf(BlockGetter level, BlockPos cell) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int dx = -16; dx <= 16; dx++) for (int dy = -16; dy <= 16; dy++) for (int dz = -16; dz <= 16; dz++) {
            probe.set(cell.getX() + dx, cell.getY() + dy, cell.getZ() + dz);
            BlockState state = level.getBlockState(probe);
            BlockMultiblockCore core = foldedCore(state);
            if (core != null && core.claimsCell(probe, cell, state.getValue(BlockMultiblockCore.FACING))) return probe.immutable();
        }
        return null;
    }

    public static @Nullable BlockPos coreOfAny(Level level, BlockPos pos) {
        return foldedCore(level.getBlockState(pos)) != null ? pos : coreOfCell(level, pos);
    }

    public static boolean isOpen(Level level, BlockPos pos, BlockState state, Direction side) {
        BlockPos core = foldedCore(state) != null ? pos : coreOfCell(level, pos);
        if (core == null) return false;
        BlockState coreState = level.getBlockState(core);
        BlockMultiblockCore block = foldedCore(coreState);
        return block != null && block.isOpenAt(coreState.getValue(BlockMultiblockCore.FACING),
                pos.getX() - core.getX(), pos.getY() - core.getY(), pos.getZ() - core.getZ(), side);
    }
}
