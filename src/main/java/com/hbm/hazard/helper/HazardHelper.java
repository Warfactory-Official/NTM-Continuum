package com.hbm.hazard.helper;

import com.hbm.config.GeneralConfig;
import com.hbm.items.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardHelper {

    /** 1.7 {@code inventory.hasItem(ModItems.reacher)}: a reacher anywhere in a player's inventory. */
    public static boolean hasReacher(LivingEntity entity) {
        return entity instanceof Player player && player.getInventory().contains(new ItemStack(ModItems.reacher.get()));
    }

    /** 1.12.2's rule for its own cold and toxic hazards: a reacher in either hand, never in 528 mode. */
    public static boolean isHoldingReacher(LivingEntity entity) {
        return entity instanceof Player player && !GeneralConfig.enable528
                && (player.getMainHandItem().is(ModItems.reacher.get()) || player.getOffhandItem().is(ModItems.reacher.get()));
    }
}
