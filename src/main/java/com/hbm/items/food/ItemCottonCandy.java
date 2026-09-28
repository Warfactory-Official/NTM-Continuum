package com.hbm.items.food;

import dev.golem.core.world.Effects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code ItemCottonCandy}: always edible plutonium candy floss. */
public class ItemCottonCandy extends ItemModFood {

    public ItemCottonCandy(Properties properties) {
        super(properties);
    }

    @Override
    protected void onFoodEaten(ItemStack stack, ServerLevel level, Player player) {
        Effects.add(player, MobEffects.POISON, 15 * 20, 0);
        Effects.add(player, MobEffects.WITHER, 5 * 20, 0);
        Effects.add(player, MobEffects.WEAKNESS, 25 * 20, 2);
        Effects.add(player, Effects.speed(), 25 * 20, 2);
        Effects.add(player, Effects.resistance(), 30 * 20, 4);
    }
}
