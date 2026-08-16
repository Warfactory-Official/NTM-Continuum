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
 * never on ice/packed ice; loses support → pops off (snow-layer pattern via {@code updateShape},
 * which also covers the old fork's post-placement fallout re-validation pass).
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
        ChunkRadiationManager.proxy.incrementRad(level, pos, 1, 100);
        level.scheduleTick(pos, this, tickDelay(random));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.is(Blocks.ICE) || belowState.is(Blocks.PACKED_ICE)) return false;
        return belowState.is(BlockTags.LEAVES) || belowState.isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState) {
        if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return state;
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
