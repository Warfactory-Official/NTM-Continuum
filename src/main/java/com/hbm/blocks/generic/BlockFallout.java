package com.hbm.blocks.generic;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.EntityHbmProps;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
 * The radioactive fallout layer deposited by {@code EntityFalloutRain}, 1.7.10's {@code BlockFallout}: a thin
 * (2px) replaceable ash layer. 1.7 gives it no tick and no chunk radiation (History §19ek-small; an earlier port
 * took 1.12.2's self-rescheduling +1 RAD tick). Walking on it gives a non-creative living entity the radiation
 * effect for 10 minutes at amplifier 0; left-clicking it contaminates the player (1 RAD/s for 200 ticks, armor
 * counts). 1.7 also cleared the effect's curative items so milk could not remove it; the port has no cross-loader
 * curative-item hook, so milk still cures it there.
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

    /** Keep the state: an unsupported layer is removed drop-free by the neighbour hook, never here. */
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

    /** 1.7 {@code onEntityWalking}: living, not a creative player, radiation 10 min at amplifier 0. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        super.stepOn(level, pos, onState, entity);
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        if (living instanceof Player player && player.isCreative()) return; // 1.7 capabilities.isCreativeMode
        Effects.addRegistered(living, ModEffects.RADIATION.get(), 10 * 60 * 20, 0);
    }

    /** 1.7 {@code onBlockClicked}: {@code addCont(player, new ContaminationEffect(1F, 200, false))}. */
    @Override
    protected void onAttack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) HbmLivingProps.addCont(player, new EntityHbmProps.ContaminationEffect(1F, 200, false));
    }
}
