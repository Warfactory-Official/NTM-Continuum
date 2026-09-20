package com.hbm.blocks.gas;

import com.hbm.blocks.generic.BlockGasBase;
import com.hbm.capability.HbmLivingProps;
import com.hbm.util.ArmorRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockGasCoal} ({@code gas_coal}, "Airborne Coal Dust"): drifting coal dust that gives black
 * lung (+10) to anything inside it without coarse-particle head protection, smokes, dissipates one tick
 * in 20 and otherwise drifts down one time in five (else any direction), then sideways.
 */
public class BlockGasCoal extends BlockGasBase {

    public BlockGasCoal(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + rand.nextFloat(), pos.getY() + rand.nextFloat(),
                pos.getZ() + rand.nextFloat(), 0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        if (!ArmorRegistry.hasProtection(living, EquipmentSlot.HEAD, ArmorRegistry.HazardClass.PARTICLE_COARSE)) {
            HbmLivingProps.incrementBlackLung(living, 10);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(20) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        Direction first = random.nextInt(5) == 0 ? Direction.DOWN : Direction.getRandom(random);
        drift(level, state, pos, random, first, randomHorizontal(random));
    }
}
