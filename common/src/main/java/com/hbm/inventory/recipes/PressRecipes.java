package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemStamp;
import com.hbm.items.machine.ItemStamp.StampType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

/**
 * Press recipes — shared by the burner press and the electric press. A recipe is keyed by
 * (ingredient, {@link StampType}): inserting an ingredient plus a stamp of the matching type yields the
 * output. Faithful but <b>feasibility-limited</b> port of the 1.12.2 {@code PressRecipes} (see
 * IMPLEMENTATION.md §18): only the metal-<b>plate</b> family is registered here, since every other
 * original recipe needs an un-ported intermediate (dusts, gems, casings, wires, circuits, pages). The
 * plate set matches {@link AssemblyMachineRecipes} (ingot → plate) so the two machines stay consistent.
 */
public final class PressRecipes {

    private PressRecipes() {}

    /** One press recipe: a stamp type + an ingredient matcher → an output stack. */
    public record PressRecipe(StampType type, AStack input, ItemStack output) {}

    private static final List<PressRecipe> RECIPES = new ArrayList<>();
    private static boolean initialized = false;

    /** All press recipes (for JEI / tooling); triggers lazy {@link #register()} first. Mirrors
     * {@link ShredderRecipes#getRecipes()}. */
    public static List<PressRecipe> getRecipes() {
        if (!initialized) register();
        return java.util.Collections.unmodifiableList(RECIPES);
    }

    /**
     * @return the output for {@code ingredient} pressed with {@code stamp}, or {@link ItemStack#EMPTY} if
     * there is no matching recipe (or {@code stamp} is not an {@link ItemStamp}).
     */
    public static ItemStack getOutput(ItemStack ingredient, ItemStack stamp) {
        if (!initialized) register();
        if (ingredient == null || ingredient.isEmpty() || stamp == null || stamp.isEmpty()) return ItemStack.EMPTY;
        if (!(stamp.getItem() instanceof ItemStamp itemStamp)) return ItemStack.EMPTY;

        StampType type = itemStamp.getStampType();
        for (PressRecipe recipe : RECIPES) {
            if (recipe.type == type && recipe.input.matchesRecipe(ingredient, true)) {
                return recipe.output;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Idempotent; recipes reference items so this runs lazily after the registries have flushed. */
    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        // --- plates (ingot -> plate), the feasible slice (matches AssemblyMachineRecipes). Ingredients are
        // material TAGS (c:ingots/<metal>) so any mod's ingot works (the modern ore-dictionary equivalent). ---
        plate(IRON, ModItems.plate_iron.get());
        plate(GOLD, ModItems.plate_gold.get());
        plate(TI, ModItems.plate_titanium.get());
        plate(AL, ModItems.plate_aluminium.get());
        plate(STEEL, ModItems.plate_steel.get());
        plate(PB, ModItems.plate_lead.get());
        plate(CU, ModItems.plate_copper.get());
        plate(GUNMETAL, ModItems.plate_gunmetal.get());
        plate(WEAPONSTEEL, ModItems.plate_weaponsteel.get());
        plate(CMB, ModItems.plate_combine_steel.get());
        plate(DURA, ModItems.plate_dura_steel.get());
        plate(BIGMT, ModItems.plate_saturnite.get());
        plate(SA326, ModItems.plate_schrabidium.get());

        // --- fine wire (ingot -> 8 wire) with the WIRE stamp, for every material that auto-generates WIRE
        // and has a ported ingot — a faithful port of the 1.12.2 PressRecipes Mats.orderedList loop. The
        // output is the autogen wire item; the ingot input is the c:ingots/<mat> tag. ---
        for (NTMMaterial mat : Mats.orderedList) {
            Item wire = ModAutogenItems.get(MaterialShapes.WIRE, mat);
            if (wire != null && AssemblyMachineRecipes.hasItem(MaterialShapes.INGOT, mat)) {
                makeRecipe(StampType.WIRE, mat.ingot(), new ItemStack(wire, 8));
            }
        }
    }

    private static void plate(NTMMaterial mat, Item plate) {
        makeRecipe(StampType.PLATE, mat.ingot(), new ItemStack(plate));
    }

    public static void makeRecipe(StampType type, AStack in, ItemStack out) {
        RECIPES.add(new PressRecipe(type, in, out));
    }
}
