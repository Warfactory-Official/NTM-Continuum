package com.hbm.inventory.recipes;

import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemScraps;
import dev.golem.core.world.CompatCustomRecipe;
import dev.golem.core.world.CustomRecipes;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ScrapsCraftingHandler}: one stack of foundry scraps alone in the grid, holding at least two quanta,
 * splits into two scraps of half the amount each (integer division - an odd quantum is lost, as in 1.7).
 */
public final class ScrapsCraftingRecipe extends CompatCustomRecipe {
    public static final ScrapsCraftingRecipe INSTANCE = new ScrapsCraftingRecipe();
    public static final RecipeSerializer<ScrapsCraftingRecipe> SERIALIZER = CustomRecipes.serializer(() -> INSTANCE);

    private ScrapsCraftingRecipe() {
        super("hbm", "scraps_split");
    }

    private static Mats.MaterialStack single(List<ItemStack> input) {
        Mats.MaterialStack mat = null;
        for (ItemStack stack : input) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ItemScraps)) return null;
            if (mat != null) return null;
            mat = ItemScraps.getMats(stack);
            if (mat == null || mat.amount < 2) return null;
        }
        return mat;
    }

    @Override
    protected boolean matchesItems(List<ItemStack> input, Level level) {
        return single(input) != null;
    }

    @Override
    protected ItemStack assembleItems(List<ItemStack> input) {
        Mats.MaterialStack mat = single(input);
        if (mat == null) return ItemStack.EMPTY;
        ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(mat.material, mat.amount / 2));
        scrap.setCount(2);
        return scrap;
    }

    @Override
    protected RecipeSerializer<ScrapsCraftingRecipe> serializer() {
        return SERIALIZER;
    }
}
