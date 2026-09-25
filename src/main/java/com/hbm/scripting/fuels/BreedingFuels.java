package com.hbm.scripting.fuels;

import com.hbm.Constants;
import com.hbm.hazard.NuclearMiscHazards;
import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemBreedingRod.RodSize;
import com.hbm.lib.ItemIds;
import com.hbm.scripting.ScriptedContent;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Breeding reactor rods: items {@code rod_<id>}, {@code rod_dual_<id>}, {@code rod_quad_<id>}.
 *
 * <pre>
 * e.breeding.add('kjs_th', 'KJS Thorium', b => b             items '&lt;name&gt; Rod' / 'Dual Rod' / 'Quad Rod'
 *     .breedsInto('thf', 500)                                 breeder output type (built-in or scripted), flux per single rod
 *     .radiation(0.5)                                         single RAD/s; dual, quad scaled as built-ins
 *     .textures('kubejs:item/rod', 'kubejs:item/rod_dual', 'kubejs:item/rod_quad')   default the empty rods
 *     .recipe('#c:billets/thorium'))                          empty rod + 1/2/4; item billet => unload recipes too
 * </pre>
 * Breeder flux: dual x2, quad x3 (1.7). {@code .shapeless(...)} / {@code .shaped(rows, key)} craft the single rod.
 */
public final class BreedingFuels {

    public void add(String id, String name, Consumer<RodSpec> spec) {
        RodSpec rod = new RodSpec();
        spec.accept(rod);
        ModNuclearMiscItems.init();
        BreedingRodType type = BreedingRodType.create(id);
        ModNuclearMiscItems.registerRods(ScriptedContent.items(ModNuclearMiscItems.ITEMS), type);
        String[] names = {name + " Rod", name + " Dual Rod", name + " Quad Rod"};
        for (RodSize size : RodSize.values()) {
            ScriptedContent.itemAsset(size.base + "_" + type.id, rod.textures[size.ordinal()], names[size.ordinal()]);
        }
        if (rod.breedsInto != null) {
            String output = rod.breedsInto;
            ScriptedContent.whenRegistered(() -> {
                if (BreedingRodType.byName(output) == null) throw new IllegalArgumentException("Unknown breeding rod type " + output);
            });
            BreederRecipes.addRodBreeding(type.name(), output, rod.flux);
        }
        if (rod.radiation != null) {
            float base = rod.radiation.floatValue();
            ScriptedContent.whenRegistered(() -> NuclearMiscHazards.registerBreedingRodRadiation(type, base));
        }
        if (rod.billet != null) loadRecipes(type, rod.billet);
        rod.recipes.emit(RodSize.SINGLE.base + "_" + type.id);
    }

    private static void loadRecipes(BreedingRodType type, String billet) {
        String[] empty = {ItemIds.rod_empty, ItemIds.rod_dual_empty, ItemIds.rod_quad_empty};
        int[] billets = {1, 2, 4};
        for (RodSize size : RodSize.values()) {
            String rod = size.base + "_" + type.id;
            int n = billets[size.ordinal()];
            FuelRecipes.shapeless(rod, 1, FuelRecipes.with(empty[size.ordinal()], billet, n));
            if (!billet.startsWith("#")) {
                FuelRecipes.shapeless(billet, n, Constants.MOD_ID + ":" + rod);
            }
        }
    }

    public static final class RodSpec {

        private final String[] textures = {Constants.MOD_ID + ":items/rod_empty", Constants.MOD_ID + ":items/rod_dual_empty",
                Constants.MOD_ID + ":items/rod_quad_empty"};
        private String breedsInto;
        private int flux;
        private Double radiation;
        private String billet;
        private final FuelRecipes.Pending recipes = new FuelRecipes.Pending();

        public RodSpec breedsInto(String type, int flux) { this.breedsInto = type; this.flux = flux; return this; }
        public RodSpec radiation(double single) { this.radiation = single; return this; }
        public RodSpec textures(String single, String dual, String quad) {
            textures[0] = single;
            textures[1] = dual;
            textures[2] = quad;
            return this;
        }
        public RodSpec recipe(String billet) { this.billet = billet; return this; }
        public RodSpec shapeless(String... ingredients) { recipes.shapeless(ingredients); return this; }
        public RodSpec shaped(String[] pattern, Map<?, ?> key) { recipes.shaped(pattern, key); return this; }
    }
}
