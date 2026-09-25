package com.hbm.items.machine;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Immutable fuel specification copied from the 1.12.2 ZIRNOX rod enum. */
public final class ZirnoxFuel {

    private static final List<ZirnoxFuel> VALUES = new ArrayList<>();
    private static final List<ZirnoxFuel> VIEW = Collections.unmodifiableList(VALUES);

    public static final ZirnoxFuel NATURAL_URANIUM = create("NATURAL_URANIUM", "natural_uranium_fuel", 250_000, 30, false, 0.35F, 172.5F, "natural_uranium");
    public static final ZirnoxFuel URANIUM = create("URANIUM", "uranium_fuel", 200_000, 50, false, 0.5F, 150F, "uranium");
    public static final ZirnoxFuel TH232 = create("TH232", "th232_fuel", 20_000, 0, true, 0.1F, 1.75F, null);
    public static final ZirnoxFuel THORIUM = create("THORIUM", "thorium_fuel", 200_000, 40, false, 1.75F, 112.5F, "thorium");
    public static final ZirnoxFuel MOX = create("MOX", "mox_fuel", 165_000, 75, false, 2.5F, 150F, "mox");
    public static final ZirnoxFuel PLUTONIUM = create("PLUTONIUM", "plutonium_fuel", 175_000, 65, false, 4.25F, 187.5F, "plutonium");
    public static final ZirnoxFuel U233 = create("U233", "u233_fuel", 150_000, 100, false, 5F, 150F, "u233");
    public static final ZirnoxFuel U235 = create("U235", "u235_fuel", 165_000, 85, false, 1F, 165F, "u235");
    public static final ZirnoxFuel LES = create("LES", "les_fuel", 150_000, 150, false, 5.85F, 225F, "les");
    public static final ZirnoxFuel LITHIUM = create("LITHIUM", "lithium_fuel", 20_000, 0, true, 0F, 0.001F, null);
    public static final ZirnoxFuel ZFB_MOX = create("ZFB_MOX", "zfb_mox_fuel", 50_000, 35, false, 2.5F, 75F, "zfb_mox");

    public static final List<ZirnoxFuel> BUILTIN = List.copyOf(VALUES);

    private final String name;
    public final String id;
    public final int maxLife;
    public final int heat;
    public final boolean breeding;
    public final float baseRadiation;
    public final float depletedRadiation;
    /** The matching waste item suffix, or {@code null} for breeding rods. */
    public final @Nullable String wasteId;

    private ZirnoxFuel(String name, String id, int maxLife, int heat, boolean breeding, float baseRadiation,
                       float depletedRadiation, @Nullable String wasteId) {
        this.name = name;
        this.id = id;
        this.maxLife = maxLife;
        this.heat = heat;
        this.breeding = breeding;
        this.baseRadiation = baseRadiation;
        this.depletedRadiation = depletedRadiation;
        this.wasteId = wasteId;
    }

    public static ZirnoxFuel create(String name, String id, int maxLife, int heat, boolean breeding,
                                    float baseRadiation, float depletedRadiation, @Nullable String wasteId) {
        if (byName(name) != null) throw new IllegalArgumentException("Duplicate ZIRNOX fuel " + name);
        ZirnoxFuel fuel = new ZirnoxFuel(name.toUpperCase(Locale.ROOT), id, maxLife, heat, breeding,
                baseRadiation, depletedRadiation, wasteId);
        VALUES.add(fuel);
        return fuel;
    }

    public static List<ZirnoxFuel> values() {
        return VIEW;
    }

    public static @Nullable ZirnoxFuel byName(String name) {
        String key = name.toUpperCase(Locale.ROOT);
        for (ZirnoxFuel fuel : VALUES) if (fuel.name.equals(key)) return fuel;
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
