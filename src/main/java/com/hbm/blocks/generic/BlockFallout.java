package com.hbm.blocks.generic;

import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The radioactive fallout layer deposited by {@code EntityFalloutRain}, ported from 1.12.2's
 * {@code BlockFallout}. A thin (2px) replaceable ash layer that steadily pumps chunk radiation:
 * every {@code 10 + rand(30)} ticks it adds 1 RAD (capped at 100) and <b>re-schedules itself</b> —
 * unlike {@link BlockHazard} this is a self-perpetuating scheduled tick, not vanilla random ticks,
 * mirroring the old {@code updateTick → scheduleUpdate} loop. Contact applies the radiation effect
 * (amp 14 ≙ 15 RAD/s for 2 min, the old {@code HbmPotion.radiation} dose).
 *
 * <p>Support rules match the old {@code canPlaceBlockAt}: sits on leaves or any up-sturdy block,
 * never on ice/packed ice ({@link #canSupport}); the sweep also refuses a cell holding fluid
 * ({@link #canOccupy}). A layer that loses its support vanishes <b>without drops</b>: 1.7's
 * {@code onNeighborBlockChange} called {@code setBlockToAir}. Returning air from {@code updateShape}, the
 * snow-layer pattern used before, makes vanilla {@code destroyBlock} it with loot — one ash item per
 * cell, hundreds after a blast whose sweep removed the support (leaves, snow, collapsing sand).
 */
public class BlockFallout extends dev.golem.core.world.CompatBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public BlockFallout(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private static int tickDelay(RandomSource random) {
        return 10 + random.nextInt(30);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, tickDelay(level.getRandom()));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Safety net for a support lost through a shape-only update (no neighbour notification).
        if (!state.canSurvive(level, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        ChunkRadiationManager.proxy.incrementRad(level, pos, 1, 100);
        level.scheduleTick(pos, this, tickDelay(random));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return canSupport(level.getBlockState(below), level, below);
    }

    /** 1.7 {@code canPlaceBlockAt}, on the block below: never ice or packed ice; leaves or an up-sturdy face. */
    public static boolean canSupport(BlockState below, BlockGetter level, BlockPos belowPos) {
        if (below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE)) return false;
        return below.is(BlockTags.LEAVES) || below.isFaceSturdy(level, belowPos, Direction.UP);
    }

    /**
     * Whether ash may go into this cell: air, or a replaceable block with no fluid in it. 1.7 asked
     * {@code !isLiquid()} of the replaced block; seagrass, tall seagrass and waterlogged plants are
     * replaceable but stand in water, which would wash the ash away (with drops) the moment it lands.
     */
    public static boolean canOccupy(BlockState cell) {
        return cell.isAir() || (cell.canBeReplaced() && !cell.liquid() && cell.getFluidState().isEmpty());
    }

    /** Keep the state: an unsupported layer is removed drop-free by the neighbour/tick hooks, never here. */
    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState) {
        return state;
    }

    /** 1.7 {@code onNeighborBlockChange} → {@code setBlockToAir}: an unsupported layer vanishes, dropping nothing. */
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (!level.isClientSide() && !state.canSurvive(level, pos)) level.removeBlock(pos, false);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        super.stepOn(level, pos, onState, entity);
        irradiate(level, entity);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        irradiate(level, entity);
    }

    private static void irradiate(Level level, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            Effects.addRegistered(living, ModEffects.RADIATION.get(), 2 * 60 * 20, 14);
        }
    }
}
