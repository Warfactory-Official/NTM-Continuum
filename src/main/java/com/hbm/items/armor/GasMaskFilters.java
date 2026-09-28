package com.hbm.items.armor;

import com.hbm.items.ModDataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The filter storage 1.7's {@code ArmorUtil.installGasMaskFilter}/{@code removeFilter}/
 * {@code damageGasMaskFilter} shared between every {@code IGasMask} helmet, on the port's gas-mask filter
 * component.
 */
final class GasMaskFilters {

    private GasMaskFilters() {}

    static ItemStack get(ItemStack mask) {
        return ModDataComponents.gasMaskFilter(mask);
    }

    static boolean applicable(ItemStack filter) {
        return !filter.isEmpty() && filter.isDamageableItem();
    }

    static void install(ItemStack mask, ItemStack filter) {
        if (applicable(filter)) ModDataComponents.gasMaskFilter(mask, filter);
    }

    /** 1.7: wear past {@code getMaxDamage} removes the filter, otherwise it is re-installed worn. */
    static void damage(ItemStack mask, int damage) {
        if (damage <= 0) return;
        ItemStack filter = get(mask);
        if (filter.isEmpty() || !filter.isDamageableItem()) return;
        int next = filter.getDamageValue() + damage;
        if (next > filter.getMaxDamage()) {
            ModDataComponents.removeGasMaskFilter(mask);
        } else {
            filter.setDamageValue(next);
            install(mask, filter);
        }
    }

    /** 1.7 sneak-use: pull the filter out into the inventory. Returns null when the use is not a removal. */
    static InteractionResult sneakRemove(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return null;
        ItemStack mask = player.getItemInHand(hand);
        ItemStack filter = get(mask);
        if (filter.isEmpty()) return null;
        if (!level.isClientSide()) {
            ModDataComponents.removeGasMaskFilter(mask);
            if (!player.getInventory().add(filter)) player.drop(filter, true);
        }
        return InteractionResult.SUCCESS;
    }
}
