package com.hbm.handler.jei;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.items.machine.ModMoldItems;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code CrucibleCastingHandler}: molten material poured into a mold, the mold above the arrow and the caster or
 * basin it sits in below. The rows are 1.7 {@code CrucibleRecipes.getMoldRecipes}: every smeltable material in every
 * mold that has an output for it.
 */
public class CrucibleCastingHandler extends HbmRecipeCategory<CrucibleCastingHandler.Casting> {

    public record Casting(ItemStack material, ItemStack mold, ItemStack basin, ItemStack output) {}

    public CrucibleCastingHandler(ViewerType<Casting> type, Component title, ItemStack icon) {
        super(type, title, icon, "gui_nei_foundry.png");
    }

    public static ItemStack[] stations() {
        return new ItemStack[]{new ItemStack(ModMachineBlocks.foundry_basin.get()),
                new ItemStack(ModMachineBlocks.foundry_mold.get()), new ItemStack(ModMachineBlocks.machine_strand_caster.get())};
    }

    public static List<Casting> buildRecipes() {
        List<Casting> rows = new ArrayList<>();
        for (NTMMaterial material : Mats.orderedList) {
            if (material.smeltable != SmeltingBehavior.SMELTABLE) continue;
            for (RegistryObject<ItemMold> item : ModMoldItems.MOLDS) {
                ItemMold.Mold mold = item.get().mold;
                ItemStack out = mold.getOutput(material);
                if (out.isEmpty()) continue;
                ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(material, mold.getCost()), true);
                ItemStack basin = new ItemStack(mold.size == 0 ? ModMachineBlocks.foundry_mold.get() : ModMachineBlocks.foundry_basin.get());
                rows.add(new Casting(scrap, new ItemStack(item.get()), basin, out.copy()));
            }
        }
        return rows;
    }

    @Override
    public void setRecipe(SlotLayout builder, Casting recipe) {
        builder.addSlot(Role.INPUT, 48, 24).addItemStack(recipe.material());
        builder.addSlot(Role.INPUT, 75, 6).addItemStack(recipe.mold());
        builder.addSlot(Role.INPUT, 75, 42).addItemStack(recipe.basin());
        builder.addSlot(Role.OUTPUT, 102, 24).addItemStack(recipe.output());
    }
}
