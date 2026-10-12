package com.hbm.handler.jei;

import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.items.machine.ItemScraps;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI category for crucible alloying — a faithful port of the 1.12.2 {@code CrucibleAlloyingHandler}:
 * its own {@code gui_nei_crucible.png} background, molten quanta shown as tinted MOLTEN {@link ItemScraps}
 * stacks in the liquid form (exactly like the old {@code ItemScraps.create(s, true)} — the scraps_liquid sprite + bare material name), inputs in a 3-wide grid at
 * {@code (12,6)}, the crucible block at {@code (75,42)}, outputs in a 3-wide grid at {@code (102,6)}.
 */
public class CrucibleAlloyingHandler extends HbmRecipeCategory<CrucibleRecipes.CrucibleRecipe> {

    private final ItemStack crucible;

    public CrucibleAlloyingHandler(ViewerType<CrucibleRecipes.CrucibleRecipe> type, Component title, ItemStack crucible) {
        super(type, title, crucible, "gui_nei_crucible.png");
        this.crucible = crucible;
    }

    public static List<CrucibleRecipes.CrucibleRecipe> buildRecipes() {
        CrucibleRecipes.register();
        List<CrucibleRecipes.CrucibleRecipe> out = new ArrayList<>();
        for (var recipe : CrucibleRecipes.INSTANCE.recipeOrderedList) {
            if (recipe instanceof CrucibleRecipes.CrucibleRecipe c) out.add(c);
        }
        return out;
    }

    @Override
    public void setRecipe(SlotLayout builder, CrucibleRecipes.CrucibleRecipe recipe) {
        if (recipe.input != null) {
            for (int i = 0; i < recipe.input.length; i++) {
                ItemStack scrap = ItemScraps.create(recipe.input[i], true);
                if (scrap.isEmpty()) continue;
                builder.addSlot(Role.INPUT, 12 + (i % 3) * 18, 6 + (i / 3) * 18)   // old init 11+,5+
                        .addItemStacks(List.of(scrap));
            }
        }

        builder.addSlot(Role.CRAFTING_STATION, 75, 42)                              // old init 74,41
                .addItemStacks(List.of(crucible));

        if (recipe.output != null) {
            for (int i = 0; i < recipe.output.length; i++) {
                ItemStack scrap = ItemScraps.create(recipe.output[i], true);
                if (scrap.isEmpty()) continue;
                builder.addSlot(Role.OUTPUT, 102 + (i % 3) * 18, 6 + (i / 3) * 18)  // old init 101+,5+
                        .addItemStacks(List.of(scrap));
            }
        }
    }
}
