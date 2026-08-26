package com.hbm.blocks.generic;

import com.hbm.config.GeneralConfig;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Airborne asbestos dust, ported from the old {@code BlockGasAsbestos}. */
public class BlockGasAsbestos extends BlockGasBase {
    public BlockGasAsbestos(Properties properties) {
        super(properties);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!GeneralConfig.enableAsbestosDust || !(entity instanceof LivingEntity)) return;
        ContaminationUtil.applyAsbestos(entity, 10, 1);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.MYCELIUM,
                    pos.getX() + random.nextFloat(), pos.getY() + random.nextFloat(),
                    pos.getZ() + random.nextFloat(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!GeneralConfig.enableAsbestosDust) {
            level.removeBlock(pos, false);
            return;
        }
        if (random.nextInt(10) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        Direction first = random.nextInt(5) == 0 ? Direction.DOWN : Direction.getRandom(random);
        drift(level, state, pos, random, first, randomHorizontal(random));
    }
}
