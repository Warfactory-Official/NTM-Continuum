package com.hbm.blocks.multiblock;

import com.hbm.capability.CapRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Resolves folded cells to their owning core without the old metadata chain. */
public final class MultiblockSurface {

    private static long @Nullable [] clientCandidates;
    private static @Nullable BlockPos clientCoreHint;

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

    /**
     * The client has no server index, so it walks back to the core. The candidates are the exact offsets
     * some registered machine places a cell at, negated — a few hundred deltas rather than the 35937 of a
     * bounding cube, and every one of them a position a core could actually occupy. Cells cluster, so the
     * last answer is tried first and usually stands.
     */
    private static @Nullable BlockPos clientCoreOf(BlockGetter level, BlockPos cell) {
        BlockPos hint = clientCoreHint;
        if (hint != null && coreClaims(level, hint, cell)) return hint;

        long[] candidates = clientCandidates;
        if (candidates == null) candidates = clientCandidates = buildClientCandidates();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (long delta : candidates) {
            probe.set(cell.getX() + BlockPos.getX(delta), cell.getY() + BlockPos.getY(delta),
                    cell.getZ() + BlockPos.getZ(delta));
            if (!coreClaims(level, probe, cell)) continue;
            BlockPos core = probe.immutable();
            clientCoreHint = core;
            return core;
        }
        return null;
    }

    /**
     * Does the folded core standing at {@code core} claim {@code cell}? The walk's acceptance test, factored
     * out so hint and walk cannot disagree.
     */
    public static boolean coreClaims(BlockGetter level, BlockPos core, BlockPos cell) {
        BlockState state = level.getBlockState(core);
        BlockMultiblockCore block = foldedCore(state);
        return block != null && block.claimsCell(core, cell, state.getValue(BlockMultiblockCore.FACING));
    }

    private static long[] buildClientCandidates() {
        it.unimi.dsi.fastutil.longs.LongOpenHashSet deltas = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for (net.minecraft.world.level.block.Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (!(block instanceof BlockMultiblockCore core)) continue;
            for (Direction facing : Direction.values()) {
                if (facing.getAxis().isVertical()) continue;
                core.maskTable().forEachLocalCell((lx, ly, lz) -> deltas.add(BlockPos.asLong(
                        -MultiblockMaskTable.worldX(lx, ly, lz, facing), -ly,
                        -MultiblockMaskTable.worldZ(lx, ly, lz, facing))));
            }
        }
        return deltas.toLongArray();
    }

    public static @Nullable BlockPos coreOfAny(Level level, BlockPos pos) {
        return foldedCore(level.getBlockState(pos)) != null ? pos : coreOfCell(level, pos);
    }

    public static boolean isOpen(Level level, BlockPos pos, BlockState state, Direction side) {
        return isOpen(level, pos, state, side, null);
    }

    /**
     * Is this cell's face open, for one role? The role-blind ask reads the union of every role's plane,
     * which is what a caller with no commodity in hand wants; naming one is what keeps a power-in face from
     * answering for a fluid-out query on a machine whose 1.7 code stated the two separately.
     */
    public static boolean isOpen(Level level, BlockPos pos, BlockState state, Direction side, @Nullable CapRole role) {
        BlockPos core = foldedCore(state) != null ? pos : coreOfCell(level, pos);
        if (core == null) return false;
        BlockState coreState = level.getBlockState(core);
        BlockMultiblockCore block = foldedCore(coreState);
        return block != null && block.isOpenAt(coreState.getValue(BlockMultiblockCore.FACING),
                pos.getX() - core.getX(), pos.getY() - core.getY(), pos.getZ() - core.getZ(), side,
                BlockMultiblockCore.PASSIVE_NONE, role);
    }

    /** The passive accept surface alone — the item route's question; see {@code isPassiveOnlyOpenAt}. */
    public static boolean isPassiveOpen(Level level, BlockPos pos, Direction side, int passiveDomains) {
        BlockMultiblockCore.FoldedOwner owner = ownerOf(level, pos);
        if (owner == null) {
            BlockMultiblockCore self = foldedCore(level.getBlockState(pos));
            if (self == null) return false;
            return self.isPassiveOnlyOpenAt(level.getBlockState(pos).getValue(BlockMultiblockCore.FACING),
                    0, 0, 0, side, passiveDomains);
        }
        BlockPos core = owner.pos();
        return owner.block().isPassiveOnlyOpenAt(owner.facing(), pos.getX() - core.getX(),
                pos.getY() - core.getY(), pos.getZ() - core.getZ(), side, passiveDomains);
    }
}
