package com.hbm.items.food;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Vanilla 1.7 {@code ItemSoup}: stacks to one, 0.6 saturation, and hands back the bowl. 26.1 returns the bowl
 * through the {@code usingConvertsTo} component; older targets get it from this override.
 */
public class ItemBowlFood extends ItemModFood {

    public ItemBowlFood(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player && player.getAbilities().instabuild) return result;
        return result.isEmpty() ? new ItemStack(Items.BOWL) : result;
    }
}
