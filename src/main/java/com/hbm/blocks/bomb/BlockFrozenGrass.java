package com.hbm.blocks.bomb;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code WasteEarth} in its {@code frozen_grass} role: anything living that walks on it gets
 * Slowness III for two minutes ({@code onEntityWalking}). It does not tick (the original was constructed
 * with {@code tick = false}); thawing is done to it by {@code burning_earth} and the exothermic bomb.
 */
public class BlockFrozenGrass extends CompatBlock {

    public BlockFrozenGrass(Properties props) {
        super(props);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        super.stepOn(level, pos, onState, entity);
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 2 * 60 * 20, 2));
        }
    }
}
