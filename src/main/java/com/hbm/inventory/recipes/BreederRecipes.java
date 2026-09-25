package com.hbm.inventory.recipes;

import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemBreedingRod.RodSize;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Breeding reactor recipes — 1:1 port of the 1.7 {@code BreederRecipes.registerDefaults}. Each rod type
 * is registered for the single, dual (flux x2) and quad (flux x3) rods. The etched meteorite sword entry
 * waits on the meteorite swords.
 */
public final class BreederRecipes {

    private BreederRecipes() {}

    /** nicer than opaque object arrays */
    public record BreederRecipe(ItemStack output, int flux) {}

    private record RodBreeding(String input, String output, int flux) {}

    private static final Map<Item, BreederRecipe> recipes = new LinkedHashMap<>();
    private static final List<RodBreeding> ROD_TYPES = new ArrayList<>();
    private static boolean initialized = false;

    public static Map<Item, BreederRecipe> getAllRecipes() {
        init();
        return Collections.unmodifiableMap(recipes);
    }

    public static BreederRecipe getOutput(ItemStack stack) {
        if (stack.isEmpty()) return null;
        init();
        return recipes.get(stack.getItem());
    }

    /** Sets recipes for single, dual, and quad rods */
    private static void setRecipe(BreedingRodType inputType, BreedingRodType outputType, int flux) {
        for (RodSize size : RodSize.values()) {
            recipes.put(ModNuclearMiscItems.rod(size, inputType),
                    new BreederRecipe(new ItemStack(ModNuclearMiscItems.rod(size, outputType)), flux * size.fluxMultiplier));
        }
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        setRecipe(BreedingRodType.LITHIUM, BreedingRodType.TRITIUM, 200);
        setRecipe(BreedingRodType.CO, BreedingRodType.CO60, 100);
        setRecipe(BreedingRodType.RA226, BreedingRodType.AC227, 300);
        setRecipe(BreedingRodType.TH232, BreedingRodType.THF, 500);
        setRecipe(BreedingRodType.U235, BreedingRodType.NP237, 300);
        setRecipe(BreedingRodType.NP237, BreedingRodType.PU238, 200);
        setRecipe(BreedingRodType.PU238, BreedingRodType.PU239, 1000);
        setRecipe(BreedingRodType.U238, BreedingRodType.RGP, 300);
        setRecipe(BreedingRodType.URANIUM, BreedingRodType.RGP, 200);
        setRecipe(BreedingRodType.RGP, BreedingRodType.WASTE, 200);
        for (RodBreeding row : ROD_TYPES) setRecipe(rodType(row.input()), rodType(row.output()), row.flux());
        com.hbm.scripting.ScriptedRecipes.apply(BreederRecipes.class);
    }

    /** Every size of {@code input} breeds into the same size of {@code output}; types resolve by name at table build. */
    public static synchronized void addRodBreeding(String input, String output, int flux) {
        ROD_TYPES.add(new RodBreeding(input, output, flux));
        reset();
    }

    private static BreedingRodType rodType(String name) {
        BreedingRodType type = BreedingRodType.byName(name);
        if (type == null) throw new IllegalArgumentException("Unknown breeding rod type " + name);
        return type;
    }

    public static void put(Item input, BreederRecipe recipe) {
        init();
        recipes.put(input, recipe);
    }

    public static boolean remove(Item input) {
        init();
        return recipes.remove(input) != null;
    }

    public static void removeAll() {
        init();
        recipes.clear();
    }

    public static synchronized void reset() {
        recipes.clear();
        initialized = false;
    }
}
