package com.hbm.inventory.recipes;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Crucible alloying recipes — the 1.12.2 {@code CrucibleRecipes} defaults, restricted to ported
 * materials. Recipes operate on molten {@link Mats.MaterialStack quanta} (n = nugget = 8, i = ingot =
 * 72), run every {@code frequency} ticks while the crucible's recipe stack holds all inputs, and are
 * picked by the player in the recipe-selector GUI.
 *
 * <p><b>IMPORTANT (1.12.2 comment kept):</b> crucibles do not have stack size checks for the recipe's
 * result — make sure material doesn't "expand" when alloying.
 *
 * <p>Deferred with their materials: hematite/malachite ore smelting (source blocks not ported) and CMB
 * (needs MAT_MUD).
 */
public class CrucibleRecipes extends GenericRecipes {

    public static final CrucibleRecipes INSTANCE = new CrucibleRecipes();

    /** One alloying recipe: quanta inputs → quanta outputs, once per {@code frequency} ticks. */
    public static class CrucibleRecipe extends GenericRecipe {

        public Mats.MaterialStack[] input;
        public Mats.MaterialStack[] output;
        public int frequency = 1;

        public CrucibleRecipe(String name) {
            super(name);
        }

        public CrucibleRecipe crucibleSetup(int frequency, ItemStack icon) {
            this.frequency = frequency;
            this.setIcon(icon);
            return this;
        }

        public CrucibleRecipe inputs(Mats.MaterialStack... input) { this.input = input; return this; }
        public CrucibleRecipe outputs(Mats.MaterialStack... output) { this.output = output; return this; }

        public int getInputAmount() {
            int content = 0;
            for (Mats.MaterialStack stack : input) content += stack.amount;
            return content;
        }

        @Override
        public List<Component> print() {
            List<Component> list = new ArrayList<>();
            list.add(Component.literal(getLocalizedName()).withStyle(ChatFormatting.YELLOW));

            list.add(Component.translatable("gui.recipe.input").append(":").withStyle(ChatFormatting.BOLD));
            for (Mats.MaterialStack stack : input) {
                list.add(Component.literal("  " + stack.material.getDisplayName() + ": "
                        + Mats.formatAmount(stack.amount, false)).withStyle(ChatFormatting.GRAY));
            }

            list.add(Component.translatable("gui.recipe.output").append(":").withStyle(ChatFormatting.BOLD));
            for (Mats.MaterialStack stack : output) {
                list.add(Component.literal("  " + stack.material.getDisplayName() + ": "
                        + Mats.formatAmount(stack.amount, false)).withStyle(ChatFormatting.GRAY));
            }

            return list;
        }
    }

    /** Idempotent registration (items must be registry-flushed; call from a BlockEntity ctor, not setup). */
    public static void register() {
        INSTANCE.registerRecipes();
    }

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return; // idempotent

        int n = MaterialShapes.NUGGET.quantity;
        int i = MaterialShapes.INGOT.quantity;

        register(new CrucibleRecipe("crucible.steel").crucibleSetup(20, new ItemStack(ModItems.ingot_steel.get()))
                .inputs(new Mats.MaterialStack(Mats.MAT_IRON, n * 2), new Mats.MaterialStack(Mats.MAT_CARBON, n * 3), new Mats.MaterialStack(Mats.MAT_FLUX, n))
                .outputs(new Mats.MaterialStack(Mats.MAT_STEEL, n * 2)));

        register(new CrucibleRecipe("crucible.redcopper").crucibleSetup(2, new ItemStack(ModItems.ingot_red_copper.get()))
                .inputs(new Mats.MaterialStack(Mats.MAT_COPPER, n), new Mats.MaterialStack(Mats.MAT_REDSTONE, n))
                .outputs(new Mats.MaterialStack(Mats.MAT_RED_COPPER, n * 2)));

        register(new CrucibleRecipe("crucible.hss").crucibleSetup(9, new ItemStack(ModItems.ingot_dura_steel.get()))
                .inputs(new Mats.MaterialStack(Mats.MAT_STEEL, n * 5), new Mats.MaterialStack(Mats.MAT_TUNGSTEN, n * 3), new Mats.MaterialStack(Mats.MAT_COBALT, n))
                .outputs(new Mats.MaterialStack(Mats.MAT_DURA, n * 9)));

        register(new CrucibleRecipe("crucible.ferro").crucibleSetup(3, new ItemStack(ModItems.ingot_ferrouranium.get()))
                .inputs(new Mats.MaterialStack(Mats.MAT_STEEL, n * 2), new Mats.MaterialStack(Mats.MAT_U238, n))
                .outputs(new Mats.MaterialStack(Mats.MAT_FERRO, n * 3)));

        register(new CrucibleRecipe("crucible.tcalloy").crucibleSetup(9, new ItemStack(ModItems.ingot_tcalloy.get()))
                .inputs(new Mats.MaterialStack(Mats.MAT_STEEL, n * 8), new Mats.MaterialStack(Mats.MAT_TECHNETIUM, n))
                .outputs(new Mats.MaterialStack(Mats.MAT_TCALLOY, i)));

        register(new CrucibleRecipe("crucible.cdalloy").crucibleSetup(9, new ItemStack(ModItems.ingot_cdalloy.get()))
                .inputs(new Mats.MaterialStack(Mats.MAT_STEEL, n * 8), new Mats.MaterialStack(Mats.MAT_CADMIUM, n))
                .outputs(new Mats.MaterialStack(Mats.MAT_CDALLOY, i)));

        register(new CrucibleRecipe("crucible.bbronze").crucibleSetup(9, new ItemStack(ModItems.ingot_bismuth_bronze.get()))
                .inputs(new Mats.MaterialStack(Mats.MAT_COPPER, n * 8), new Mats.MaterialStack(Mats.MAT_BISMUTH, n), new Mats.MaterialStack(Mats.MAT_FLUX, n * 3))
                .outputs(new Mats.MaterialStack(Mats.MAT_BBRONZE, i), new Mats.MaterialStack(Mats.MAT_SLAG, n * 3)));

        register(new CrucibleRecipe("crucible.abronze").crucibleSetup(9, new ItemStack(ModItems.ingot_arsenic_bronze.get()))
                .inputs(new Mats.MaterialStack(Mats.MAT_COPPER, n * 8), new Mats.MaterialStack(Mats.MAT_ARSENIC, n), new Mats.MaterialStack(Mats.MAT_FLUX, n * 3))
                .outputs(new Mats.MaterialStack(Mats.MAT_ABRONZE, i), new Mats.MaterialStack(Mats.MAT_SLAG, n * 3)));

        register(new CrucibleRecipe("crucible.magtung").crucibleSetup(3, new ItemStack(ModItems.ingot_magnetized_tungsten.get()))
                .inputs(new Mats.MaterialStack(Mats.MAT_TUNGSTEN, i), new Mats.MaterialStack(Mats.MAT_SCHRABIDIUM, n))
                .outputs(new Mats.MaterialStack(Mats.MAT_MAGTUNG, i)));

        register(new CrucibleRecipe("crucible.bscco").crucibleSetup(3, new ItemStack(ModItems.ingot_bscco.get()))
                .inputs(new Mats.MaterialStack(Mats.MAT_BISMUTH, n * 2), new Mats.MaterialStack(Mats.MAT_STRONTIUM, n * 2), new Mats.MaterialStack(Mats.MAT_CALCIUM, n * 2), new Mats.MaterialStack(Mats.MAT_COPPER, n * 3))
                .outputs(new Mats.MaterialStack(Mats.MAT_BSCCO, i)));
    }
}
