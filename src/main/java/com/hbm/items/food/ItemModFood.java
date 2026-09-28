package com.hbm.items.food;

import dev.golem.core.world.CompatItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The shared shape of 1.7's {@code ItemFood} subclasses: the food component carries hunger and saturation
 * (built with {@code CompatConsumables}), and {@link #onFoodEaten} is 1.7's server-side hook that runs after
 * the stack was eaten. The use animation and duration are stated here rather than read off the component
 * because {@link CompatItem} answers both itself.
 */
public class ItemModFood extends CompatItem {

    private final int duration;
    private final boolean drink;

    public ItemModFood(Properties properties) {
        this(properties, 32, false);
    }

    public ItemModFood(Properties properties, int duration, boolean drink) {
        super(properties);
        this.duration = duration;
        this.drink = drink;
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return duration;
    }

    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return drink ? UseAnimation.DRINK : UseAnimation.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack eaten = stack.copyWithCount(1); // the vanilla path may empty the stack before our hook
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel server && entity instanceof Player player) {
            onFoodEaten(eaten, server, player);
        }
        return result;
    }

    /** 1.7 {@code ItemFood.onFoodEaten}, server side only (every override guarded {@code !world.isRemote}). */
    protected void onFoodEaten(ItemStack stack, ServerLevel level, Player player) {
    }
}
