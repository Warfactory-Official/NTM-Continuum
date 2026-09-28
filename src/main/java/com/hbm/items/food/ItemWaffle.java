package com.hbm.items.food;

import com.hbm.explosion.ExplosionNukeSmall;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code ItemWaffle}: 20 hunger, then a medium mini-nuke half a block above the eater. */
public class ItemWaffle extends ItemModFood {

    public ItemWaffle(Properties properties) {
        super(properties);
    }

    @Override
    protected void onFoodEaten(ItemStack stack, ServerLevel level, Player player) {
        ExplosionNukeSmall.explode(level, player.getX(), player.getY() + 0.5, player.getZ(), ExplosionNukeSmall.PARAMS_MEDIUM);
    }
}
