package com.hbm.api.data;

import com.hbm.Constants;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.DamageResistanceHandler.ResistanceStats;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.DataMaps;
import dev.golem.core.data.DataMaps.DataMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Armor protection as data ({@code data/<ns>/value_maps/...}, see {@link HbmDataMaps}): radiation resistance
 * (1.7 {@code HazmatRegistry}), hazard classes (1.7 {@code ArmorRegistry}), full sets (1.7 {@code ArmorUtil} set checks
 * and {@code DamageResistanceHandler} set stats) and damage threshold/resistance (1.7 {@code DamageResistanceHandler}).
 * The values were exported once from the old Java registration and are checked against it by the
 * {@code value_map_armor_*} GameTests.
 */
public final class ArmorDataMaps {

    private ArmorDataMaps() {}

    private static <V> DataMap<Item, V> item(String name, Codec<V> codec) {
        return DataMaps.map(ArmorDataMaps.class, Constants.MOD_ID, name, "item", () -> BuiltInRegistries.ITEM, codec);
    }

    /**
     * A piece's radiation resistance (1.7 {@code HazmatRegistry.registerHazmat}): the worn pieces sum, and radiation is
     * multiplied by {@code 10^-sum}. A full set's value is split by slot (helmet .2, chest .4, legs .3, boots .1).
     */
    public static final DataMap<Item, Double> RADIATION_RESISTANCE = item("radiation_resistance", Codec.DOUBLE);

    /** What a piece, a gas-mask filter or an armor module protects against (1.7 {@code ArmorRegistry.registerHazard}). */
    public static final DataMap<Item, List<HazardClass>> PROTECTION_CLASSES = item("protection_classes", HazardClass.CODEC.listOf());

    /**
     * Whether a piece is conductive ("Faraday") armor. 1.7 decided by substrings of the item's name
     * ({@code ArmorUtil.metals}), which stays the rule for items this map does not name; {@code true} or {@code false}
     * here overrides it.
     */
    public static final DataMap<Item, Boolean> FARADAY_ARMOR = item("faraday_armor", Codec.BOOL);

    /**
     * Full four-piece sets, keyed by the chestplate: the other three pieces (absent = that slot must be empty), the set
     * checks the set satisfies ({@code hazmat}, {@code hazmat2}, {@code digamma}, {@code asbestos}; 1.7's
     * {@code ArmorUtil.checkForHazmat/Haz2/Digamma/Asbestos}) and the set's damage resistance
     * (1.7 {@code DamageResistanceHandler.registerSet}).
     */
    public static final DataMap<Item, ArmorSet> ARMOR_SETS = item("armor_sets", ArmorSet.CODEC);

    /** One piece's damage resistance, worn alone or in a set (1.7 {@code DamageResistanceHandler.itemStats}). */
    public static final DataMap<Item, ResistanceStats> DAMAGE_RESISTANCE = item("damage_resistance", ResistanceStats.CODEC);

    /** An entity's innate damage resistance (1.7 {@code DamageResistanceHandler.entityStats}, keyed by class there). */
    public static final DataMap<EntityType<?>, ResistanceStats> ENTITY_DAMAGE_RESISTANCE = DataMaps.map(ArmorDataMaps.class,
            Constants.MOD_ID, "damage_resistance", "entity_type", () -> BuiltInRegistries.ENTITY_TYPE, ResistanceStats.CODEC);

    /** One full set (see {@link #ARMOR_SETS}); pieces are item ids. */
    public record ArmorSet(Optional<String> helmet, Optional<String> legs, Optional<String> boots, List<String> checks,
                           Optional<ResistanceStats> resistance) {
        public static final Codec<ArmorSet> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("helmet").forGetter(ArmorSet::helmet),
                Codec.STRING.optionalFieldOf("legs").forGetter(ArmorSet::legs),
                Codec.STRING.optionalFieldOf("boots").forGetter(ArmorSet::boots),
                Codec.STRING.listOf().optionalFieldOf("checks", List.of()).forGetter(ArmorSet::checks),
                ResistanceStats.CODEC.optionalFieldOf("resistance").forGetter(ArmorSet::resistance)
        ).apply(i, ArmorSet::new));
    }

    /** Chest pieces that multiply the lock-pick chance a hundredfold ({@code true}; 1.7 {@code TileEntityLockableBase}: the two jackets). */
    public static final DataMap<Item, Boolean> LOCKPICK_JACKET = item("lockpick_jacket", Codec.BOOL);

    /** Loads the class at mod construction (from {@link HbmDataMaps#declare()}). */
    public static void declare() {}
}
