package com.hbm.blocks.generic;

import com.hbm.config.GeneralConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Invisible, slowly settling radon gas emitted by uranium ore and ZIRNOX accidents. */
public class BlockGasRadon extends BlockGasBase {
    private final float dose;
    private final boolean dense;

    public BlockGasRadon(Properties properties, float dose, boolean dense) {
        super(properties);
        this.dose = dose;
        this.dense = dense;
        // 1.7 BlockGasRadon super(0.1F, 0.8F, 0.1F), BlockGasRadonDense super(0.1F, 0.5F, 0.1F)
        if (dense) cloudColor(0.1F, 0.5F, 0.1F);
        else cloudColor(0.1F, 0.8F, 0.1F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        // 1.7 BlockGasRadonDense.randomDisplayTick: one town aura every display tick
        if (dense) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.MYCELIUM, pos.getX() + random.nextFloat(),
                    pos.getY() + random.nextFloat(), pos.getZ() + random.nextFloat(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!GeneralConfig.enableRadon) {
            level.removeBlock(pos, false);
            return;
        }
        if (dense && random.nextInt(10) == 0 && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos.below(), com.hbm.blocks.ModFalloutBlocks.waste_earth.get().defaultBlockState(), 2);
        }
        if (random.nextInt(Math.max(1, GeneralConfig.radonDecayChance)) == 0) {
            BlockState fallout = com.hbm.blocks.ModFalloutBlocks.fallout.get().defaultBlockState();
            if (dense && GeneralConfig.denseRadonFallout && fallout.canSurvive(level, pos)) {
                level.setBlock(pos, fallout, 2);
            } else {
                level.removeBlock(pos, false);
            }
            return;
        }
        Direction primary = random.nextInt(5) == 0 ? Direction.UP : Direction.DOWN;
        drift(level, state, pos, random, primary, randomHorizontal(random));
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && GeneralConfig.enableRadon && entity instanceof LivingEntity living) {
            boolean protectedByFilter = ArmorRegistry.hasProtection(living, net.minecraft.world.entity.EquipmentSlot.HEAD,
                    ArmorRegistry.HazardClass.PARTICLE_FINE);
            if (protectedByFilter) ArmorUtil.damageGasMaskFilter(living, dense ? 2 : 1);
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                    protectedByFilter ? ContaminationUtil.ContaminationType.CREATIVE
                            : ContaminationUtil.ContaminationType.RAD_BYPASS, dose);
        }
    }
}
