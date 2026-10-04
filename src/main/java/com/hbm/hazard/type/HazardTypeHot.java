package com.hbm.hazard.type;

import java.util.List;

import com.hbm.config.RadiationConfig;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import dev.golem.core.world.Effects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeHot implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {

        final boolean wetOrReacher = HazardHelper.isHoldingReacher(target) || target.isInWaterOrRain();
        if (RadiationConfig.disableHot || wetOrReacher) return;
        // 1.7 setFire(ceil(level)) every tick: a duration in seconds that only ever raises the remaining burn, so
        // one application per hazardRate ticks keeps it identical; multiplying it by the rate made a nugget burn 5x
        // longer after being dropped. 1.7 exempted no creative player either (they burn without taking damage).
        if (level > 0) Effects.ignite(target, (int) Math.ceil(level));
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, double level, final ItemStack stack, final List<IHazardModifier> modifiers) {

        level = IHazardModifier.evalAllModifiers(stack, player, level, modifiers);

        if (level > 0)
            list.add(Component.literal("[").append(Component.translatable("trait.hot")).append(Component.literal("]")).withStyle(ChatFormatting.GOLD));
    }
}
