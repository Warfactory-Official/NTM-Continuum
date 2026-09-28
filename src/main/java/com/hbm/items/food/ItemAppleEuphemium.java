package com.hbm.items.food;

import dev.golem.core.world.Effects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code ItemAppleEuphemium}: always edible, always glinting, epic, and effectively invulnerability. */
public class ItemAppleEuphemium extends ItemModFood {

    public ItemAppleEuphemium(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    protected void onFoodEaten(ItemStack stack, ServerLevel level, Player player) {
        Effects.add(player, Effects.resistance(), Integer.MAX_VALUE, 120);
        Effects.add(player, MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0);
        Effects.add(player, MobEffects.SATURATION, Integer.MAX_VALUE, 120);
    }
}
