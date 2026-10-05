package com.hbm.compat.sable;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.Constants;
import com.hbm.entity.item.EntityMovingConveyorObject;
import com.hbm.tileentity.IRelocatable;
import dev.golem.core.world.SubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Finishes a sub-level move for HBM's blocks. The sub-level system has copied each block entity's saved data to
 * the new position, placed the block there with {@code onPlace} suppressed, and cleared the old position (whose
 * removal hooks saw {@link SubLevels#isMoving} and so neither dropped nor demolished anything). What it cannot
 * know about is everything an HBM block keeps outside its saved data; this restores it, in dependency order:
 *
 * <ol>
 *   <li>stored positions of other blocks follow them ({@link IRelocatable});</li>
 *   <li>multiblock cells are indexed to their core's new position (the cell-to-core index is level data keyed by
 *       absolute position, and only the old entries were removed);</li>
 *   <li>placement-time registration the system skipped: {@code onPlace} again, which is where cables join the power
 *       graph and blocks schedule their first tick or cast their beams;</li>
 *   <li>load-time registration, now that the saved data is in place: {@code clearRemoved} again, which the block
 *       entity first ran before its data was loaded. It is the chunk-load hook every HBM block entity already makes
 *       idempotent (graph cells, endpoints, remote links of pylons and anchors).</li>
 * </ol>
 */
final class CarriedBlocks {

    private CarriedBlocks() {}

    static void afterMove(SubLevels.Move move) {
        ServerLevel level = move.to();
        int carried = 0;
        for (BlockPos pos : move.moved().values()) {
            if (isHbm(level.getBlockState(pos))) carried++;
        }
        if (carried == 0) return;

        for (BlockPos pos : move.moved().values()) {
            if (level.getBlockEntity(pos) instanceof IRelocatable relocatable) relocatable.relocate(move);
        }
        for (BlockPos pos : move.moved().values()) {
            BlockMultiblockCore core = MultiblockSurface.foldedCore(level.getBlockState(pos));
            if (core != null) core.reindexLoadedCells(level, pos);
        }
        for (BlockPos pos : move.moved().values()) {
            BlockState state = level.getBlockState(pos);
            if (isHbm(state)) state.onPlace(level, pos, Blocks.AIR.defaultBlockState(), true);
        }
        for (BlockPos pos : move.moved().values()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null && !be.isRemoved() && isHbm(be.getBlockState())) be.clearRemoved();
        }
        carryConveyorObjects(move);
        Constants.LOG.debug("Sub-level move carried {} HBM block(s)", carried);
    }

    /**
     * Items and packages riding the belts that were carried go with them. Sable carries only hanging entities, so
     * without this they would stay where the belts were and drop off at once. Each keeps its place within its belt's
     * cell, turned with the move; on a sub-level they then stay in its plot ({@code sable:retain_in_sub_level}),
     * where the belts are.
     */
    private static void carryConveyorObjects(SubLevels.Move move) {
        AABB around = null;
        for (BlockPos old : move.moved().keySet()) around = around == null ? new AABB(old) : around.minmax(new AABB(old));
        if (around == null) return;
        for (EntityMovingConveyorObject object : move.from().getEntitiesOfClass(EntityMovingConveyorObject.class,
                around.inflate(1D))) {
            BlockPos cell = object.blockPosition();
            if (!move.carried(cell)) continue;
            Vec3 at = object.position();
            double dx = at.x - cell.getX() - 0.5D, dz = at.z - cell.getZ() - 0.5D, dy = at.y - cell.getY();
            // BlockPos.rotate's turn of (x, z), about the cell's centre
            double rx, rz;
            switch (move.rotation()) {
                case CLOCKWISE_90 -> { rx = -dz; rz = dx; }
                case CLOCKWISE_180 -> { rx = -dx; rz = -dz; }
                case COUNTERCLOCKWISE_90 -> { rx = dz; rz = -dx; }
                default -> { rx = dx; rz = dz; }
            }
            BlockPos landed = move.apply(cell);
            object.setPos(landed.getX() + 0.5D + rx, landed.getY() + dy, landed.getZ() + 0.5D + rz);
            object.setDeltaMovement(Vec3.ZERO);
            object.setOldPosAndRot();
        }
    }

    private static boolean isHbm(BlockState state) {
        return Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }
}
