package com.hbm.items.machine;

import dev.golem.core.world.CompatItem;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    public static final class BreedingRodType {

        private static final List<BreedingRodType> VALUES = new ArrayList<>();
        private static final List<BreedingRodType> VIEW = Collections.unmodifiableList(VALUES);

        public static final BreedingRodType LITHIUM = create("LITHIUM");
        public static final BreedingRodType TRITIUM = create("TRITIUM");
        public static final BreedingRodType CO = create("CO");
        public static final BreedingRodType CO60 = create("CO60");
        public static final BreedingRodType TH232 = create("TH232");
        public static final BreedingRodType THF = create("THF");
        public static final BreedingRodType U235 = create("U235");
        public static final BreedingRodType NP237 = create("NP237");
        public static final BreedingRodType U238 = create("U238");
        public static final BreedingRodType PU238 = create("PU238");
        public static final BreedingRodType PU239 = create("PU239");
        public static final BreedingRodType RGP = create("RGP");
        public static final BreedingRodType WASTE = create("WASTE");

        //Required for prototype
        public static final BreedingRodType LEAD = create("LEAD");
        public static final BreedingRodType URANIUM = create("URANIUM");

        public static final BreedingRodType RA226 = create("RA226");
        public static final BreedingRodType AC227 = create("AC227");

        private final String name;
        public final String id;

        private BreedingRodType(String name) {
            this.name = name;
            this.id = name.toLowerCase(Locale.ROOT);
        }

        public static BreedingRodType create(String name) {
            if (byName(name) != null) throw new IllegalArgumentException("Duplicate breeding rod type " + name);
            BreedingRodType type = new BreedingRodType(name.toUpperCase(Locale.ROOT));
            VALUES.add(type);
            return type;
        }

        public static List<BreedingRodType> values() {
            return VIEW;
        }

        public static @Nullable BreedingRodType byName(String name) {
            String key = name.toUpperCase(Locale.ROOT);
            for (BreedingRodType type : VALUES) if (type.name.equals(key)) return type;
            return null;
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
