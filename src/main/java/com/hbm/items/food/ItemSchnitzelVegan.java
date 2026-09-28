package com.hbm.items.food;

import dev.golem.core.world.Effects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code ItemSchnitzelVegan}: feeds nothing, then blinds, sickens, sets alight and launches the eater. */
public class ItemSchnitzelVegan extends ItemModFood {

    public ItemSchnitzelVegan(Properties properties) {
        super(properties);
    }

    @Override
    protected void onFoodEaten(ItemStack stack, ServerLevel level, Player player) {
        Effects.add(player, MobEffects.BLINDNESS, 10 * 20, 0);
        Effects.add(player, MobEffects.NAUSEA, 30 * 20, 0);
        Effects.add(player, MobEffects.HUNGER, 3 * 60 * 20, 4);
        Effects.add(player, MobEffects.WITHER, 3 * 20, 0);
        Effects.ignite(player, 5 * 20); // 1.7 setFire takes seconds, so this is 100 s of fire, not 5
        // 1.7 sets motionY = 2 on the server copy of the player; the velocity packet carries it to the client
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, 2, motion.z);
        player.hurtMarked = true;
    }
}
