package com.hbm.blocks.multiblock;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The DE importer's multiblock rebuild. It lives in this package for {@link BlockMultiblockCore#cellStateFor}, and in
 * the importer's source root, so it goes with it. Cells go only into air or replaceable blocks and are written without
 * neighbour updates: a powered bomb beside an imported machine must not see a change.
 */
public final class ImportedCells {
    public record Result(int placed, int blocked) {}

    private ImportedCells() {}

    /**
     * Like {@link #footprint(BlockMultiblockCore, BlockPos, Direction)}, with an RBMK column {@code columnHeight} blocks
     * high (the world rule, core included) instead of the height of whatever world was loaded last.
     */
    public static Map<BlockPos, BlockState> footprint(BlockMultiblockCore machine, BlockPos core, Direction facing, int columnHeight) {
        if (!(machine instanceof RBMKBase)) return footprint(machine, core, facing);
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        int above = columnHeight - 1;
        for (int y = 1; y <= above; y++)
            out.put(core.above(y), machine.defaultBlockState().setValue(RBMKBase.PART, y == above ? RBMKBase.Part.TOP : RBMKBase.Part.MIDDLE));
        return out;
    }

    /** Every cell {@code machine} occupies around {@code core}, with the state written there, from geometry alone. */
    public static Map<BlockPos, BlockState> footprint(BlockMultiblockCore machine, BlockPos core, Direction facing) {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        machine.visitPlacedCells(EmptyBlockGetter.INSTANCE, core, facing, (pos, mask) -> {
            int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
            int lx = MultiblockMaskTable.localX(dx, dy, dz, facing), lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
            out.putIfAbsent(pos.immutable(), machine.cellStateFor(lx, dy, lz, facing, 0));
        });
        return out;
    }

    public static Result rebuild(ServerLevel level, BlockPos core) {
        BlockState coreState = level.getBlockState(core);
        if (!(coreState.getBlock() instanceof BlockMultiblockCore machine)) return new Result(0, 0);
        Direction facing = coreState.getValue(BlockMultiblockCore.FACING);
        int[] counts = new int[2];
        // A footprint may visit a cell more than once (overlapping boxes, ports).
        Set<Long> seen = new HashSet<>();
        BlockMultiblockCore.withoutTeardown(() -> machine.visitPlacedCells(level, core, facing, (pos, mask) -> {
            if (!seen.add(pos.asLong())) return;
            BlockState there = level.getBlockState(pos);
            long owner = MultiblockCoreIndex.lookup(level, pos);
            // a cell of this machine with no index entry is one an earlier, crashed run placed before the index was saved
            if (machine.isCellOf(there) && (owner == core.asLong() || owner == Long.MIN_VALUE)) {
                if (owner == Long.MIN_VALUE) MultiblockCoreIndex.put(level, pos.immutable(), core);
                counts[0]++;
                return;
            }
            if (!there.isAir() && !there.canBeReplaced()) {
                counts[1]++;
                return;
            }
            int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
            int lx = MultiblockMaskTable.localX(dx, dy, dz, facing), lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
            BlockPos at = pos.immutable();
            level.setBlock(at, machine.cellStateFor(lx, dy, lz, facing, 0), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (machine.isCellOf(level.getBlockState(at))) MultiblockCoreIndex.put(level, at, core);
            counts[0]++;
        }));
        return new Result(counts[0], counts[1]);
    }
}
