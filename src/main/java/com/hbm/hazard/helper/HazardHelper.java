package com.hbm.hazard.helper;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.hbm.items.ModItems;

/** Inventory checks shared by radiation and neutron hazards. */
public class HazardHelper {

    public static boolean isHoldingReacher(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.reacher.get())) return true;
        }
        return false;
    }
}
