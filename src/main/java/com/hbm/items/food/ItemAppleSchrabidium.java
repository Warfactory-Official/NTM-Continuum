package com.hbm.items.food;

import com.hbm.lib.ModDamageSources;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.Effects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ItemAppleSchrabidium}, which served both the schrabidium and the lead apple: three metadata tiers
 * (crafted from nuggets, ingots or blocks) that brand alike - uncommon/rare/epic, the block tier glinting - and
 * then do entirely different things. The port registers one item per tier ({@code <id>}, {@code <id>_ingot},
 * {@code <id>_block}); meta 0 keeps the bare id so {@code new ItemStack(apple)} references still resolve.
 */
public class ItemAppleSchrabidium extends ItemModFood {

    private final boolean lead;
    private final int tier;

    public ItemAppleSchrabidium(Properties properties, boolean lead, int tier) {
        super(properties);
        this.lead = lead;
        this.tier = tier;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier == 2;
    }

    @Override
    protected void onFoodEaten(ItemStack stack, ServerLevel level, Player player) {
        if (!lead) {
            switch (tier) {
                case 0 -> {
                    Effects.add(player, MobEffects.REGENERATION, 600, 4);
                    Effects.add(player, Effects.resistance(), 6000, 0);
                    Effects.add(player, MobEffects.FIRE_RESISTANCE, 6000, 0);
                }
                case 1 -> {
                    Effects.add(player, MobEffects.REGENERATION, 1200, 4);
                    Effects.add(player, Effects.resistance(), 1200, 4);
                    Effects.add(player, MobEffects.FIRE_RESISTANCE, 1200, 0);
                    Effects.add(player, Effects.strength(), 1200, 4);
                    Effects.add(player, Effects.haste(), 1200, 2);
                    Effects.add(player, Effects.speed(), 1200, 2);
                    Effects.add(player, Effects.jumpBoost(), 1200, 4);
                    Effects.add(player, MobEffects.HEALTH_BOOST, 1200, 9);
                    Effects.add(player, MobEffects.ABSORPTION, 1200, 4);
                    Effects.add(player, MobEffects.SATURATION, 1200, 9);
                }
                default -> {
                    Effects.add(player, MobEffects.REGENERATION, Integer.MAX_VALUE, 4);
                    Effects.add(player, Effects.resistance(), Integer.MAX_VALUE, 1);
                    Effects.add(player, MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0);
                    Effects.add(player, Effects.strength(), Integer.MAX_VALUE, 9);
                    Effects.add(player, Effects.haste(), Integer.MAX_VALUE, 4);
                    Effects.add(player, Effects.speed(), Integer.MAX_VALUE, 3);
                    Effects.add(player, Effects.jumpBoost(), Integer.MAX_VALUE, 4);
                    Effects.add(player, MobEffects.HEALTH_BOOST, Integer.MAX_VALUE, 24);
                    Effects.add(player, MobEffects.ABSORPTION, Integer.MAX_VALUE, 14);
                    Effects.add(player, MobEffects.SATURATION, Integer.MAX_VALUE, 99);
                }
            }
        } else {
            switch (tier) {
                case 0 -> Effects.addRegistered(player, ModEffects.LEAD.get(), 15 * 20, 2);
                case 1 -> Effects.addRegistered(player, ModEffects.LEAD.get(), 60 * 20, 4);
                default -> player.hurt(ModDamageSources.lead(level), 500F);
            }
        }
    }
}
