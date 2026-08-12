package com.hbm.blocks.generic;

import com.hbm.potion.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The contaminated ground blocks from 1.12.2's {@code WasteEarth}: {@code waste_earth} (dead grass —
 * inert) and {@code waste_mycelium} (glowing mycelium — heavy radiation on contact + spore particles).
 * Only the mycelium had an {@code onEntityWalk} effect in the original (amp 29 ≙ 30 RAD/s for 30 s);
 * the frozen/burning variants and the auto-cleanup decay tick are not ported (config default off).
 */
public class BlockWasteEarth extends Block {

    private final boolean mycelium;

    public BlockWasteEarth(Properties props, boolean mycelium) {
        super(props);
        this.mycelium = mycelium;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        super.stepOn(level, pos, onState, entity);
        irradiate(level, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effects, isPrecise);
        irradiate(level, entity);
    }

    private void irradiate(Level level, Entity entity) {
        if (mycelium && !level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModEffects.radiation(), 30 * 20, 29));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (mycelium) {
            level.addParticle(ParticleTypes.MYCELIUM,
                    pos.getX() + random.nextFloat(), pos.getY() + 1.1F, pos.getZ() + random.nextFloat(), 0, 0, 0);
        }
    }
}
