package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import dev.golem.core.render.BlockOutlines;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The hovered outline of a folded machine is the whole machine, as in ntm-next: the core's and every cell's own
 * shape, merged. Only the drawn outline changes; picking and collision stay per cell. A cell whose own shape already
 * reaches past its block (a {@code bounding} silhouette, the cargo elevator) keeps it.
 */
public final class MultiblockOutline {

    private static @Nullable BlockPos cachedCore;
    private static @Nullable BlockState cachedState;
    private static long cachedStates;
    private static @Nullable VoxelShape cachedShape;
    private static boolean initialized;

    private MultiblockOutline() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        BlockOutlines.replaceShape(MultiblockOutline::outline);
    }

    private static @Nullable VoxelShape outline(Level level, BlockPos pos, BlockState state) {
        if (MultiblockSurface.foldedCore(state) == null && !MultiblockSurface.isFoldedCell(state)) return null;
        VoxelShape own = state.getShape(level, pos);
        if (!own.isEmpty() && reachesOut(own.bounds())) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return null;
        BlockState coreState = level.getBlockState(core);
        BlockMultiblockCore block = MultiblockSurface.foldedCore(coreState);
        if (block == null) return null;
        return machineShape(level, core, coreState, block)
                .move(core.getX() - pos.getX(), core.getY() - pos.getY(), core.getZ() - pos.getZ());
    }

    private static boolean reachesOut(AABB box) {
        return box.minX < 0 || box.minY < 0 || box.minZ < 0 || box.maxX > 1 || box.maxY > 1 || box.maxZ > 1;
    }

    /** The core-frame union, rebuilt only when the core or a block state of the machine changed. */
    private static VoxelShape machineShape(Level level, BlockPos core, BlockState coreState, BlockMultiblockCore block) {
        List<BlockPos> parts = new ArrayList<>();
        parts.add(core);
        block.visitPlacedCells(level, core, coreState.getValue(BlockMultiblockCore.FACING), (cell, mask) -> {
            if (block.isCellOf(level.getBlockState(cell))) parts.add(cell.immutable());
        });
        long states = 1;
        for (BlockPos part : parts) states = states * 31 + System.identityHashCode(level.getBlockState(part));
        if (cachedShape != null && core.equals(cachedCore) && coreState == cachedState && states == cachedStates) {
            return cachedShape;
        }
        VoxelShape shape = Shapes.empty();
        for (BlockPos part : parts) {
            VoxelShape own = level.getBlockState(part).getShape(level, part);
            if (own.isEmpty()) continue;
            shape = Shapes.joinUnoptimized(shape,
                    own.move(part.getX() - core.getX(), part.getY() - core.getY(), part.getZ() - core.getZ()),
                    BooleanOp.OR);
        }
        cachedCore = core.immutable();
        cachedState = coreState;
        cachedStates = states;
        cachedShape = shape.optimize();
        return cachedShape;
    }
}
