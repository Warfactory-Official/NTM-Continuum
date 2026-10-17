package com.hbm.handler.jei;

import com.hbm.inventory.material.MaterialDistribution;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.items.machine.ItemScraps;
import dev.golem.core.Ids;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.7 {@code CrucibleSmeltingHandler}: an item (or tag) and the molten stacks the crucible turns it into, in a 3x3
 * grid. The rows are 1.7 {@code CrucibleRecipes.getSmeltingRecipes}: every material shape tag, then the material
 * distribution's tag and item rows, the same tables the crucible reads.
 */
public class CrucibleSmeltingHandler extends HbmRecipeCategory<CrucibleSmeltingHandler.Smelting> {

    public record Smelting(List<ItemStack> input, List<ItemStack> outputs) {}

    private final ItemStack crucible;

    public CrucibleSmeltingHandler(ViewerType<Smelting> type, Component title, ItemStack crucible) {
        super(type, title, crucible, "gui_nei_crucible_smelting.png");
        this.crucible = crucible;
    }

    public static List<Smelting> buildRecipes() {
        List<Smelting> rows = new ArrayList<>();
        Set<TagKey<Item>> shapeTags = new HashSet<>();
        for (NTMMaterial material : Mats.orderedList) {
            NTMMaterial convert = material.smeltsInto;
            if (convert.smeltable != SmeltingBehavior.SMELTABLE && convert.smeltable != SmeltingBehavior.ADDITIVE) continue;
            for (MaterialShapes shape : MaterialShapes.values()) {
                if (shape.quantity <= 0) continue;
                TagKey<Item> tag = material.tag(shape);
                if (!shapeTags.add(tag)) continue;
                List<ItemStack> input = tagged(tag);
                if (input.isEmpty()) continue;
                int amount = (int) ((long) shape.quantity * material.convOut / material.convIn);
                rows.add(new Smelting(input, List.of(ItemScraps.create(new Mats.MaterialStack(convert, amount), true))));
            }
        }
        for (Map.Entry<String, List<Mats.MaterialStack>> entry : MaterialDistribution.tags().entrySet()) {
            List<ItemStack> input = tagged(TagKey.create(Registries.ITEM, Ids.parse(entry.getKey())));
            if (!input.isEmpty()) rows.add(new Smelting(input, scraps(entry.getValue())));
        }
        for (Map.Entry<Item, List<Mats.MaterialStack>> entry : MaterialDistribution.items().entrySet()) {
            rows.add(new Smelting(List.of(new ItemStack(entry.getKey())), scraps(entry.getValue())));
        }
        return rows;
    }

    static List<ItemStack> tagged(TagKey<Item> tag) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) stacks.add(new ItemStack(holder.value()));
        return stacks;
    }

    private static List<ItemStack> scraps(List<Mats.MaterialStack> stacks) {
        List<ItemStack> scraps = new ArrayList<>();
        for (Mats.MaterialStack stack : stacks) scraps.add(ItemScraps.create(stack, true));
        return scraps;
    }

    @Override
    public void setRecipe(SlotLayout builder, Smelting recipe) {
        builder.addSlot(Role.INPUT, 48, 24).addItemStacks(recipe.input());
        builder.addSlot(Role.CRAFTING_STATION, 75, 42).addItemStack(crucible);
        for (int i = 0; i < recipe.outputs().size(); i++) {
            builder.addSlot(Role.OUTPUT, 102 + (i % 3) * 18, 6 + (i / 3) * 18).addItemStack(recipe.outputs().get(i));
        }
    }
}
