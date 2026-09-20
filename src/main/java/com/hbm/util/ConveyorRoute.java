package com.hbm.util;

import com.hbm.items.tool.ItemConveyorWand.ConveyorType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.blocks.network.BlockConveyorBase;
import com.hbm.blocks.network.BlockConveyorBendable;
import com.hbm.blocks.network.BlockConveyorChute;
import com.hbm.blocks.network.BlockConveyorLift;
import com.hbm.blocks.network.BlockCraneBase;
import com.hbm.items.tool.ItemConveyorWand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;

/**
 * 1.7 {@code ItemConveyorWand.construct} and its helpers: attempts to construct a conveyor between two points,
 * including bends, lifts and chutes. 1.7 routed into an {@code IBlockAccess} that was either the world or a
 * {@code WorldInAJar} preview; {@link ConveyorPlacer} is that seam, so the wand's commit and the client's ghost
 * walk exactly the same code. A null placer is the dry run the wand makes before committing.
 */
public final class ConveyorRoute {

    private ConveyorRoute() {}

    /** Writes a belt state and lets it read its neighbours (lift part, chute connections) as a placed block would. */
    public static void setBelt(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        BlockState settled = Block.updateFromNeighbourShapes(state, level, pos);
        if (settled != state) level.setBlock(pos, settled, Block.UPDATE_ALL);
    }

    /**
     * @param build whether to actually place into {@code level}; false routes without touching it
     * @return belts laid, 0 if the budget ran out, -1 if the route is obstructed
     */
    public static int construct(Level level, boolean build, ConveyorType type, Player player,
                                BlockPos anchor, Direction anchorSide, BlockPos end, Direction endSide, int max) {
        return construct(level, build ? into(level) : null, type, player, anchor, anchorSide, end, endSide, max);
    }

    /** The placer that commits a run to {@code level}. */
    public static ConveyorPlacer into(Level level) {
        return new LevelPlacer(level);
    }

    /**
     * Folds the neighbour-derived properties into a belt state as the block itself would on placement: the
     * lift's {@code PART} and the chute's feed/side connections. Reads come from {@code level}, so a preview
     * volume answers with the run being laid.
     */
    public static BlockState settle(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof BlockConveyorLift) {
            return state.setValue(BlockConveyorLift.PART, BlockConveyorLift.partAt(level, pos));
        }
        if (state.getBlock() instanceof BlockConveyorChute) {
            return BlockConveyorChute.withNeighbours(state, level, pos);
        }
        return state;
    }

    /**
     * @param build the placer to write into, or null to route without placing anything. Obstruction reads
     *              come from {@code level}, the placement reads (lift parts, chute connections) from the placer.
     * @return belts laid, 0 if the budget ran out, -1 if the route is obstructed
     */
    public static int construct(Level level, @Nullable ConveyorPlacer build, ConveyorType type, Player player,
                                BlockPos anchor, Direction anchorSide, BlockPos end, Direction endSide, int max) {
        Direction dir = anchorSide;
        Direction targetDir = endSide;

        // if placing within a single block, we have to handle rotation specially, treating it like a manual placement with player facing
        if (anchor.equals(end) && anchorSide == endSide && dir.getAxis().isVertical()) {
            BlockPos at = anchor.relative(dir);
            if (!level.getBlockState(at).canBeReplaced()) return -1;
            if (build != null) place(build, at, ItemConveyorWand.getConveyorBlock(type), player.getDirection(), ConveyorBend.STRAIGHT);
            return 1;
        }

        boolean hasVertical = ItemConveyorWand.hasSnakesAndLadders(type);

        BlockPos target = end.relative(targetDir);
        BlockPos cursor = anchor.relative(dir);

        if (dir.getAxis().isVertical()) dir = targetDirection(cursor, end, hasVertical);

        Block targetBlock = level.getBlockState(end).getBlock();
        boolean shouldTurnToTarget = targetDir.getAxis().isHorizontal() || targetBlock instanceof BlockCraneBase
                || targetBlock == ModConveyorBlocks.CONVEYOR_LIFT.get() || targetBlock == ModConveyorBlocks.CONVEYOR_CHUTE.get();

        Direction horDir = dir.getAxis().isVertical() ? player.getDirection() : dir;

        // Initial dropdown to floor level, if possible
        if (hasVertical && cursor.getY() > target.getY() && level.getBlockState(cursor.below()).canBeReplaced()) {
            dir = Direction.DOWN;
        }

        for (int loopDepth = 1; loopDepth <= max; loopDepth++) {
            if (!level.getBlockState(cursor).canBeReplaced()) return -1;

            Block block = conveyorForDirection(type, dir);
            // 1.7 computes the facing BEFORE the turn and does not recompute it when the turn swaps the block
            Direction facing = facingFor(block, dir, targetDir, horDir);
            ConveyorBend bend = ConveyorBend.STRAIGHT;

            BlockPos ahead = cursor.relative(dir);

            // check if we should turn before continuing
            int fromDistance = taxi(cursor, target);
            int toDistance = taxi(ahead, target);
            int finalDistance = taxi(ahead, end);
            boolean notAtTarget = (shouldTurnToTarget ? finalDistance : fromDistance) > 0;
            boolean willBeObstructed = notAtTarget && !level.getBlockState(ahead).canBeReplaced();
            boolean shouldTurn = (toDistance >= fromDistance && notAtTarget) || willBeObstructed;

            if (shouldTurn) {
                Direction newDir = targetDirection(cursor, shouldTurnToTarget ? end : target, target, dir, willBeObstructed, hasVertical);

                if (newDir == Direction.UP) block = ModConveyorBlocks.CONVEYOR_LIFT.get();
                else if (newDir == Direction.DOWN) block = ModConveyorBlocks.CONVEYOR_CHUTE.get();
                // Axis overload as in ntm-next: after a lift/chute {@code dir} is vertical and the plain
                // getClockWise() throws ("Unable to get Y-rotated facing of up"); the Y-axis form is a no-op there.
                else if (dir.getClockWise(Direction.Axis.Y) == newDir) bend = ConveyorBend.RIGHT;          // meta += 8
                else if (dir.getCounterClockWise(Direction.Axis.Y) == newDir) bend = ConveyorBend.LEFT;    // meta += 4

                dir = newDir;
                if (dir.getAxis().isHorizontal()) horDir = dir;
            }

            if (build != null) place(build, cursor, block, facing, bend);

            if (cursor.equals(target)) return loopDepth;

            cursor = cursor.relative(dir);
        }

        return 0;
    }

    private static void place(ConveyorPlacer build, BlockPos pos, Block block, Direction facing, ConveyorBend bend) {
        BlockState state = block.defaultBlockState().setValue(BlockConveyorBase.FACING, facing);
        if (state.hasProperty(BlockConveyorBendable.BEND)) state = state.setValue(BlockConveyorBendable.BEND, bend);
        build.place(pos, settle(build, pos, state));
    }

    /** The commit placer: {@link #setBelt} lets the placed block re-read its real neighbours as well. */
    private record LevelPlacer(Level level) implements ConveyorPlacer {

        @Override
        public void place(BlockPos pos, BlockState state) {
            setBelt(level, pos, state);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return level.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return level.getFluidState(pos);
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return level.getBlockEntity(pos);
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinY() {
            return level.getMinY();
        }
    }

    private static Block conveyorForDirection(ConveyorType type, Direction dir) {
        if (dir == Direction.UP) return ModConveyorBlocks.CONVEYOR_LIFT.get();
        if (dir == Direction.DOWN) return ModConveyorBlocks.CONVEYOR_CHUTE.get();
        return ItemConveyorWand.getConveyorBlock(type);
    }

    /** 1.7 {@code getConveyorMetaForDirection}, with the metadata (input side) turned into the travel facing. */
    private static Direction facingFor(Block block, Direction dir, Direction targetDir, Direction horDir) {
        if (block != ModConveyorBlocks.CONVEYOR_CHUTE.get() && block != ModConveyorBlocks.CONVEYOR_LIFT.get()) return dir;
        if (targetDir.getAxis().isVertical()) return horDir;
        return targetDir.getOpposite();
    }

    private static Direction targetDirection(BlockPos from, BlockPos to, boolean hasVertical) {
        return targetDirection(from, to, to, null, false, hasVertical);
    }

    private static Direction targetDirection(BlockPos from, BlockPos to, BlockPos target, @Nullable Direction heading,
                                             boolean willBeObstructed, boolean hasVertical) {
        if (hasVertical && (from.getY() != to.getY() || from.getY() != target.getY())
                && (willBeObstructed
                || (from.getX() == to.getX() && from.getZ() == to.getZ())
                || (from.getX() == target.getX() && from.getZ() == target.getZ()))) {
            return from.getY() > to.getY() ? Direction.DOWN : Direction.UP;
        }

        if (Math.abs(from.getX() - to.getX()) > Math.abs(from.getZ() - to.getZ())) {
            if (heading == Direction.EAST || heading == Direction.WEST) {
                return from.getZ() > to.getZ() ? Direction.NORTH : Direction.SOUTH;
            }
            return from.getX() > to.getX() ? Direction.WEST : Direction.EAST;
        }

        if (heading == Direction.NORTH || heading == Direction.SOUTH) {
            return from.getX() > to.getX() ? Direction.WEST : Direction.EAST;
        }
        return from.getZ() > to.getZ() ? Direction.NORTH : Direction.SOUTH;
    }

    private static int taxi(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }
}
