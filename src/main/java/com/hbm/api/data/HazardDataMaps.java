package com.hbm.api.data;

import com.hbm.Constants;
import com.hbm.hazard.HazardSpec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.DataMaps;
import dev.golem.core.data.DataMaps.DataMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Item hazards as data ({@code data/<ns>/value_maps/...}, see {@link HbmDataMaps}). These were 1.7's
 * {@code HazardRegistry} / {@code OreDictManager} hazard frames; the values are the port's verified 1.7 numbers,
 * exported once from the old Java registration (GameTest {@code value_map_hazards_equal_17}).
 */
public final class HazardDataMaps {

    private HazardDataMaps() {}

    private static <V> DataMap<Item, V> item(String name, Codec<V> codec) {
        return DataMaps.map(HazardDataMaps.class, Constants.MOD_ID, name, "item", () -> BuiltInRegistries.ITEM, codec);
    }

    /**
     * Item and item-tag hazards ({@link HazardSpec}). Unlike an ordinary data map, every matching {@code #tag} value
     * applies, in the order they are given, and then the item's own value (1.7's ore-dictionary keys, then the item),
     * with 1.7's override and mutex rules deciding which survive ({@code HazardSystem.getHazardsFromStack}).
     */
    public static final DataMap<Item, HazardSpec> HAZARDS = item("hazards", HazardSpec.CODEC);

    /** Scales every hazard an item carries ({@code HazardTransformerCustom.setMultiplier} as data). None in 1.7. */
    public static final DataMap<Item, Double> HAZARD_MULTIPLIER = item("hazard_multiplier", Codec.DOUBLE);

    /**
     * Hazards of a fluid cell by the fluid it holds (vanilla fluid id; every HBM fluid is one, {@code hbm:<key>}). 1.7
     * registered {@code cell_tritium} and {@code cell_sas3} as items of their own; the port has one cell item.
     */
    public static final DataMap<Fluid, HazardSpec> CELL_HAZARDS =
            DataMaps.map(HazardDataMaps.class, Constants.MOD_ID, "cell_hazards", "fluid", () -> BuiltInRegistries.FLUID, HazardSpec.CODEC);

    /**
     * A hazard block's own radiation (1.7 {@code BlockHazard.addRadiation} / {@code addRad3d}): {@code radiation} feeds
     * the chunk (a tenth per tick received, capped at the full value), {@code area} is an entity pulse every second.
     * 1.7 took {@code radiation} from the block item's RADIATION hazard once at start-up; it is stated per block here.
     */
    public static final DataMap<Block, Emission> RADIATION_EMISSION = DataMaps.map(HazardDataMaps.class,
            Constants.MOD_ID, "radiation_emission", "block", () -> BuiltInRegistries.BLOCK, Emission.CODEC);

    public record Emission(float radiation, float area) {
        public static final Codec<Emission> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("radiation", 0F).forGetter(Emission::radiation),
                Codec.FLOAT.optionalFieldOf("area", 0F).forGetter(Emission::area)
        ).apply(i, Emission::new));
    }

    /** Loads the class at mod construction (from {@link HbmDataMaps#declare()}). */
    public static void declare() {}
}
