package com.hbm.blocks.multiblock;

import com.hbm.capability.CapRole;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    public static @Nullable BlockPos coreOfCell(BlockGetter level, BlockPos cell) {
        if (level instanceof ServerLevel server) {
            long packed = MultiblockCoreIndex.lookup(server, cell);
            if (packed == Long.MIN_VALUE) return null;
            BlockPos core = BlockPos.of(packed);
            return foldedCore(server.getBlockState(core)) == null ? null : core;
        }
        return clientCoreOf(level, cell);
    }

    public static BlockMultiblockCore.@Nullable FoldedOwner ownerOf(BlockGetter level, BlockPos cell) {
        BlockPos core = coreOfCell(level, cell);
        if (core == null) return null;
        BlockState state = level.getBlockState(core);
        BlockMultiblockCore block = foldedCore(state);
        return block == null ? null : new BlockMultiblockCore.FoldedOwner(core, state, block,
                state.getValue(BlockMultiblockCore.FACING));
    }

    /**
     * The client has no server index, so it walks back to the core. The candidates are the exact offsets
     * some registered machine places a cell at, negated: a few hundred deltas rather than the 35937 of a
     * bounding cube, each a position a core could occupy. Cells cluster, so the last answer is tried first
     * and usually stands. The candidates are sorted nearest first.
     *
     * <p>A {@link BlockGetter} that is not a {@link Level} may be a section-compile region, which can read only
     * {@link #REGION_REACH} blocks around any position and throws beyond that (vanilla's
     * {@code RenderSectionRegion} indexes a 3x3x3 section array). Some machine's deltas exceed that reach, and a
     * door cell next to a section edge would crash the chunk compile (AO asks the neighbour's collision shape).
     * Reads past the reach are tried and skipped when the getter cannot answer them; the cell's own core is
     * within reach for every machine small enough to be drawn from such a region.
     */
    private static @Nullable BlockPos clientCoreOf(BlockGetter level, BlockPos cell) {
        boolean bounded = !(level instanceof Level);
        BlockPos hint = clientCoreHint;
        if (hint != null && claimsSafely(level, hint, cell, bounded && !withinReach(hint, cell))) return hint;

        long[] candidates = clientCandidates;
        if (candidates == null) candidates = clientCandidates = buildClientCandidates();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (long delta : candidates) {
            probe.set(cell.getX() + BlockPos.getX(delta), cell.getY() + BlockPos.getY(delta),
                    cell.getZ() + BlockPos.getZ(delta));
            if (!claimsSafely(level, probe, cell, bounded && reach(delta) > REGION_REACH)) continue;
            BlockPos core = probe.immutable();
            clientCoreHint = core;
            return core;
        }
        return null;
    }

    /** How far around a queried position a section-compile region can always read (one section minus the cell). */
    private static final int REGION_REACH = 15;

    private static int reach(long delta) {
        return Math.max(Math.abs(BlockPos.getX(delta)), Math.max(Math.abs(BlockPos.getY(delta)), Math.abs(BlockPos.getZ(delta))));
    }

    private static boolean withinReach(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) <= REGION_REACH && Math.abs(a.getY() - b.getY()) <= REGION_REACH
                && Math.abs(a.getZ() - b.getZ()) <= REGION_REACH;
    }

    private static boolean claimsSafely(BlockGetter level, BlockPos core, BlockPos cell, boolean mayBeOutOfRange) {
        if (!mayBeOutOfRange) return coreClaims(level, core, cell);
        try {
            return coreClaims(level, core, cell);
        } catch (IndexOutOfBoundsException outsideRegion) {
            return false;
        }
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
        LongOpenHashSet deltas = new LongOpenHashSet();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof BlockMultiblockCore core)) continue;
            for (Direction facing : Direction.values()) {
                if (facing.getAxis().isVertical()) continue;
                core.maskTable().forEachLocalCell((lx, ly, lz) -> deltas.add(BlockPos.asLong(
                        -MultiblockMaskTable.worldX(lx, ly, lz, facing), -ly,
                        -MultiblockMaskTable.worldZ(lx, ly, lz, facing))));
            }
        }
        long[] sorted = deltas.toLongArray();
        // nearest first: a section-compile region answers those without a range check (see clientCoreOf)
        return Arrays.stream(sorted).boxed()
                .sorted(Comparator.comparingInt(MultiblockSurface::reach)
                        .thenComparingLong(Long::longValue))
                .mapToLong(Long::longValue).toArray();
    }

    public static @Nullable BlockPos coreOfAny(BlockGetter level, BlockPos pos) {
        return foldedCore(level.getBlockState(pos)) != null ? pos : coreOfCell(level, pos);
    }

    /**
     * The block entity of the multiblock {@code pos} belongs to. This is what 1.7's {@code findCoreTE} answered,
     * off the index instead of a metadata walk. {@code pos} may be the core itself or any of its cells.
     */
    public static @Nullable BlockEntity coreEntity(BlockGetter level, BlockPos pos) {
        BlockPos core = coreOfAny(level, pos);
        return core == null ? null : level.getBlockEntity(core);
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

    /** {@link BlockMultiblockCore#carriesPassiveAt} reached through any cell of the machine. */
    public static boolean carriesPassive(BlockGetter level, BlockPos pos, int domains) {
        BlockMultiblockCore.FoldedOwner owner = ownerOf(level, pos);
        if (owner == null) {
            BlockState state = level.getBlockState(pos);
            BlockMultiblockCore self = foldedCore(state);
            return self != null && self.carriesPassiveAt(state.getValue(BlockMultiblockCore.FACING),
                    0, 0, 0, domains);
        }
        BlockPos core = owner.pos();
        return owner.block().carriesPassiveAt(owner.facing(), pos.getX() - core.getX(),
                pos.getY() - core.getY(), pos.getZ() - core.getZ(), domains);
    }

    /** The passive accept surface alone. This is the item route's question; see {@code isPassiveOnlyOpenAt}. */
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

    /**
     * Every block a folded machine at {@code core} is made of: the core plus each placed cell the core
     * index still attributes to it (ntm-next's {@code rigidStructureBlocks}, for the falling multiblock).
     */
    public static List<BlockPos> rigidStructureBlocks(Level level, BlockPos core) {
        BlockState state = level.getBlockState(core);
        BlockMultiblockCore block = foldedCore(state);
        if (block == null) return List.of();
        List<BlockPos> out = new ArrayList<>();
        out.add(core.immutable());
        long corePacked = core.asLong();
        ServerLevel server = level instanceof ServerLevel s ? s : null;
        block.visitPlacedCells(core, state.getValue(BlockMultiblockCore.FACING), (pos, mask) -> {
            if (server != null) {
                long recorded = MultiblockCoreIndex.lookup(server, pos);
                if (recorded != Long.MIN_VALUE && recorded != corePacked) return;
            }
            out.add(pos.immutable());
        });
        return out;
    }
}
