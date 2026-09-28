package com.hbm.items.tool;

import dev.golem.core.world.CompatItem;

/**
 * 1.7 {@code ItemCraftingDegradation} (laboratory glassware): a crafting tool that stays in the grid. The iron
 * set has 100 uses and takes one damage per craft; the boron set (durability 0) never wears.
 *
 * <p>The per-craft wear lives in the recipe, not the item: the {@code KEY_TOOL_CHEMISTRYSET} recipes (dynamite,
 * TNT, C4, bakelite, solid fuel, lubricant) are component shapeless recipes whose tool ingredient
 * {@code degrade}s (see {@link com.hbm.inventory.recipes.ModContainerRecipes}). A plain vanilla recipe using the
 * set would still consume it.
 */
public class ItemCraftingDegradation extends CompatItem {

    public ItemCraftingDegradation(Properties properties) {
        super(properties);
    }
}
