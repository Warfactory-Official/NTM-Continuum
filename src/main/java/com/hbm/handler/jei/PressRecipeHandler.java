package com.hbm.handler.jei;

import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp;
import com.hbm.items.machine.ItemStamp.StampType;
import com.hbm.items.machine.ModStampItems;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * JEI category for the press (burner + electric) — a faithful port of the 1.12.2
 * {@code PressRecipeHandler}: its own {@code gui_nei_press.png} background (which carries the vertical
 * stamp→ingredient→output art), the stamp slot on top {@code (48,6)}, the ingredient below {@code (48,42)},
 * the output at {@code (111,24)}, and the animated press-head {@code (0,86,18,18)} cycling downward at
 * {@code (47,24)} between them. The stamp slot cycles every registered stamp of the recipe's type.
 */
public class PressRecipeHandler extends HbmRecipeCategory<PressRecipes.PressRecipe> {

    private final Drawable progress;

    /** StampType → all registered stamp items of that type (built once from ModStampItems). */
    private static Map<StampType, List<ItemStack>> stampsByType;

    public PressRecipeHandler(ViewerType<PressRecipes.PressRecipe> type, Component title, ItemStack icon) {
        super(type, title, icon, "gui_nei_press.png");
        Drawable.Texture progressStatic = Drawable.texture(tex("gui_nei_press.png"), 0, 86, 18, 18);
        this.progress = Drawable.animated(progressStatic, 20, Drawable.Direction.TOP, false);
    }

    private static Map<StampType, List<ItemStack>> stampsByType() {
        if (stampsByType == null) {
            Map<StampType, List<ItemStack>> map = new EnumMap<>(StampType.class);
            for (RegistryObject<Item> ro : ModStampItems.ALL) {
                Item item = ro.get();
                if (item instanceof ItemStamp stamp) {
                    map.computeIfAbsent(stamp.getStampType(), k -> new ArrayList<>()).add(new ItemStack(item));
                }
            }
            stampsByType = map;
        }
        return stampsByType;
    }

    @Override
    public void setRecipe(SlotLayout builder, PressRecipes.PressRecipe recipe) {
        List<ItemStack> stamps = stampsByType().getOrDefault(recipe.type(), List.of());
        builder.addSlot(Role.INPUT, 48, 6).addItemStacks(stamps);                              // stamp (old init 47,5)
        builder.addSlot(Role.INPUT, 48, 42).addItemStacks(recipe.input().getDisplayStacks()); // ingredient (old init 47,41)
        builder.addSlot(Role.OUTPUT, 111, 24).addItemStacks(List.of(recipe.output()));        // output (old init 110,23)
    }

    @Override
    public void draw(PressRecipes.PressRecipe recipe, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, g, mouseX, mouseY);
        progress.draw(g, 47, 24);
    }
}
