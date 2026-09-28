package com.hbm.util;

import com.hbm.advancement.HbmCriteria;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code AchievementHandler}: "the player made this item". 1.7 looked the stack up in its
 * {@code craftingAchievements} table and triggered the matching achievement; the table now lives in the
 * advancement JSON ({@code hbm:item_produced} criteria with item predicates, declared in
 * {@code HbmAdvancementDeclarations}), so this only reports the event.
 *
 * <p>1.7 called it from {@code SlotCraftingOutput}, the anvil packet and the crafted/smelted events. The last two
 * are vanilla's own {@code minecraft:recipe_crafted} criterion now, which every loader fires for the crafting grid
 * and furnaces; only the mod's own output slots and the anvil call this.
 */
public final class AchievementHandler {

    private AchievementHandler() {}

    public static void fire(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;
        HbmCriteria.produced(player, stack);
    }
}
