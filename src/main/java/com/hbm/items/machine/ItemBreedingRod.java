package com.hbm.items.machine;

import dev.golem.core.world.CompatItem;

import java.util.Locale;

/** One flattened identity of the old metadata {@code rod} / {@code rod_dual} / {@code rod_quad} families. */
public class ItemBreedingRod extends CompatItem {

    public final BreedingRodType type;
    public final RodSize size;

    public ItemBreedingRod(Properties properties, RodSize size, BreedingRodType type) {
        super(properties);
        this.size = size;
        this.type = type;
    }

    public enum RodSize {
        SINGLE("rod", 1),
        DUAL("rod_dual", 2),
        QUAD("rod_quad", 3); // the quad breeder recipe is flux * 3, not * 4 (1.7 BreederRecipes.setRecipe)

        public final String base;
        public final int fluxMultiplier;

        RodSize(String base, int fluxMultiplier) {
            this.base = base;
            this.fluxMultiplier = fluxMultiplier;
        }
    }

    public enum BreedingRodType {
        LITHIUM,
        TRITIUM,
        CO,
        CO60,
        TH232,
        THF,
        U235,
        NP237,
        U238,
        PU238,
        PU239,
        RGP,
        WASTE,

        //Required for prototype
        LEAD,
        URANIUM,

        RA226,
        AC227;

        public final String id = name().toLowerCase(Locale.ROOT);
    }
}
