package com.hbm.handler.jei;

import com.hbm.handler.NTMToolHandler;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.ModToolItems;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ToolingHandler}: each in-world tool conversion as the materials plus the block, the tool in the pill and
 * the converted block, read from {@link NTMToolHandler}.
 */
public class ToolingRecipeHandler extends ConstructionRecipeHandler {

    public ToolingRecipeHandler(ViewerType<Row> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    public static ItemStack[] stations() {
        return new ItemStack[]{new ItemStack(ModToolItems.boltgun.get()), new ItemStack(ModToolItems.blowtorch.get()),
                new ItemStack(ModMeleeKitItems.acetylene_torch.get())};
    }

    public static List<Row> buildRecipes() {
        List<Row> rows = new ArrayList<>();
        for (NTMToolHandler.Row conversion : NTMToolHandler.all()) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            for (AStack material : conversion.materials()) inputs.add(material.getDisplayStacks());
            inputs.add(List.of(new ItemStack(conversion.from())));
            rows.add(new Row(inputs, new ItemStack(conversion.to()), conversion.tool().stacksForDisplay()));
        }
        return rows;
    }
}
