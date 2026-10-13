package com.hbm.blocks.fluid;

import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.ModOutgasBlocks;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import dev.golem.core.fluid.FluidBlock;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code CoriumFinite} ({@code corium_block}), a {@code BlockFluidFinite} of 5 quanta on a 30-tick flow: it
 * keeps its volume, levelling out evenly, and melts through what it touches, a block of explosion resistance
 * {@code r} giving way one flow in {@code (int) (3 * sqrt(r))} (1.7 {@code getExplosionResistance(null)} is the
 * modern resistance; anything under 1/9 always). It webs, sets alight for 3 seconds, deals 2 radiation damage and 1
 * rad of contamination per contact tick. After a flow update with nothing corium below it, one time in ten it
 * solidifies: a third of those into solid {@code block_corium}, the rest into {@code block_corium_cobble}.
 */
public class BlockCorium extends FluidBlock {

    public BlockCorium(Properties properties) {
        super(properties, ModWorldFluids.CORIUM);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        ModWorldFluids.setInWeb(entity, state);
        Effects.ignite(entity, 3); // 1.7 setFire(3), Fire Protection applies
        if (level.isClientSide()) return;
        entity.hurt(ModDamageSources.source(level, ModDamageSources.RADIATION), 2F);
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 1F);
        }
    }

    /** 1.7 CoriumFinite.canDisplace: liquids always, anything else by chance against its blast resistance. */
    @Override
    protected boolean canDisplace(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        float res = (float) (Math.sqrt(state.getBlock().getExplosionResistance()) * 3);
        if (res < 1) return true;
        // 1.7 rolled a fresh java.util.Random here; the level's random is the same distribution
        RandomSource random = level instanceof Level world ? world.getRandom() : RandomSource.create();
        return isLiquid(state) || random.nextInt((int) res) == 0;
    }

    @Override
    protected boolean displaceIfPossible(Level level, BlockPos pos) {
        if (isLiquid(level.getBlockState(pos))) return false;
        return canDisplace(level, pos);
    }

    /** 1.7 updateTick tail: one in ten, with no corium below, solidify (a third as block_corium). */
    @Override
    protected void afterFlow(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0 && !level.getBlockState(pos.below()).is(this)) {
            if (random.nextInt(3) == 0) level.setBlock(pos, ModHazardBlocks.block_corium.get().defaultBlockState(), 3);
            else level.setBlock(pos, ModOutgasBlocks.block_corium_cobble.get().defaultBlockState(), 3);
        }
    }
}
