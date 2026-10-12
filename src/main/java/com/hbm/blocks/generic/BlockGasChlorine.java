package com.hbm.blocks.generic;

import com.hbm.handler.ArmorUtil;
import com.hbm.util.ArmorRegistry;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockGasClorine} ({@code chlorine_gas}): the cloud a chlorine seal spreads. Anything inside
 * without full gas-lung protection is blinded (5 s), poisoned III (20 s), withered II (1 s), slowed II
 * and fatigued III (30 s); a mask instead loses one filter point. Dissipates one tick in ten, otherwise
 * drifts down (up one time in five) then sideways. A visible translucent cell, unlike the other gases.
 */
public class BlockGasChlorine extends BlockGasBase {

    public BlockGasChlorine(Properties properties) {
        super(properties);
        cloudColor(0.7F, 0.8F, 0.6F);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;

        if (ArmorRegistry.hasAllProtection(living, EquipmentSlot.HEAD, ArmorRegistry.HazardClass.GAS_LUNG)) {
            ArmorUtil.damageGasMaskFilter(living, 1);
        } else {
            Effects.add(living, MobEffects.BLINDNESS, 5 * 20, 0);
            Effects.add(living, MobEffects.POISON, 20 * 20, 2);
            Effects.add(living, MobEffects.WITHER, 1 * 20, 1);
            Effects.add(living, MobEffects.SLOWNESS, 30 * 20, 1);
            Effects.add(living, MobEffects.MINING_FATIGUE, 30 * 20, 2);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        Direction first = random.nextInt(5) == 0 ? Direction.UP : Direction.DOWN;
        drift(level, state, pos, random, first, randomHorizontal(random));
    }
}
