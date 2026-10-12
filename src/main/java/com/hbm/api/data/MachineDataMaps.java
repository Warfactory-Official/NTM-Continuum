package com.hbm.api.data;

import com.hbm.Constants;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.loader.TableCodecs;
import com.hbm.items.machine.ItemICFPellet.EnumICFFuel;
import com.hbm.tileentity.bomb.TileEntityNukeCustom.EnumBombType;
import com.hbm.tileentity.bomb.TileEntityNukeCustom.EnumEntryType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.DataMaps;
import dev.golem.core.data.DataMaps.DataMap;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/**
 * Per-item and per-fluid machine values as data ({@code data/<ns>/value_maps/...}, see {@link HbmDataMaps}): what an
 * item or fluid is worth to one machine. Converted once from the Java tables (GameTests in {@code ItemRecipeTableGameTests}
 * compare them with 1.7's values). Item ids inside values are plain strings, resolved when the machine needs them: the
 * bundled files are read at mod construction, before items exist.
 */
public final class MachineDataMaps {

    private MachineDataMaps() {}

    private static <V> DataMap<Item, V> item(String name, Codec<V> codec) {
        return DataMaps.map(MachineDataMaps.class, Constants.MOD_ID, name, "item", () -> BuiltInRegistries.ITEM, codec);
    }

    private static <V> DataMap<Fluid, V> fluid(String name, Codec<V> codec) {
        return DataMaps.map(MachineDataMaps.class, Constants.MOD_ID, name, "fluid", () -> BuiltInRegistries.FLUID, codec);
    }

    // ---- ICF pellet press (1.7 ItemICFPellet.fluidMap / materialMap) ----

    /** The ICF fuel a fluid presses into (1,000 mB per pellet half), by vanilla fluid id ({@code hbm:deuterium}). */
    public static final DataMap<Fluid, EnumICFFuel> ICF_FUEL_FLUID = fluid("icf_fuel", EnumICFFuel.CODEC);

    /**
     * The ICF fuel an item made of exactly one ingot of a material presses into, keyed by HBM material name
     * ({@code "lithium"}; {@code data/<ns>/value_maps/material/icf_fuel.json}). Materials are not a registry, so this map
     * is read by name only ({@link #icfFuel(NTMMaterial)}); tags do not apply.
     */
    private static final DataMap<Item, EnumICFFuel> ICF_FUEL_MATERIAL = DataMaps.map(MachineDataMaps.class, Constants.MOD_ID,
            "icf_fuel", "material", () -> BuiltInRegistries.ITEM, EnumICFFuel.CODEC);

    public static EnumICFFuel icfFuel(NTMMaterial material) {
        return material == null ? null : ICF_FUEL_MATERIAL.byId().get(material.name);
    }

    /** The material-keyed map itself (its file is written by the build-time data source). */
    public static DataMap<Item, EnumICFFuel> icfMaterialMap() {
        return ICF_FUEL_MATERIAL;
    }

    /** Every material-keyed ICF fuel, by material name. */
    public static java.util.Map<String, EnumICFFuel> icfMaterialFuels() {
        return ICF_FUEL_MATERIAL.byId();
    }

    // ---- custom nuke (1.7 TileEntityNukeCustom.registerBombItems) ----

    /** One custom nuke ingredient: the stage it feeds, its value and whether it adds or multiplies. */
    public record NukeEntry(EnumBombType type, float value, EnumEntryType op) {

        public static final Codec<NukeEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                TableCodecs.lowerEnum(EnumBombType.class).fieldOf("type").forGetter(NukeEntry::type),
                Codec.FLOAT.fieldOf("value").forGetter(NukeEntry::value),
                TableCodecs.lowerEnum(EnumEntryType.class).optionalFieldOf("op", EnumEntryType.ADD).forGetter(NukeEntry::op)
        ).apply(i, NukeEntry::new));
    }

    /** Item ingredients of the custom nuke. */
    public static final DataMap<Item, NukeEntry> CUSTOM_NUKE = item("custom_nuke", NukeEntry.CODEC);

    /** Full fluid cells as custom nuke ingredients, by the fluid they hold (1.7's metadata cells). */
    public static final DataMap<Fluid, NukeEntry> CUSTOM_NUKE_CELL = fluid("custom_nuke", NukeEntry.CODEC);

    // ---- radiation generator (1.7 TileEntityMachineRadGen fuels) ----

    /**
     * A radiation generator fuel: HE per tick, ticks, and what is left over (an item id and count; none for nothing). The
     * waste drums (every waste class leaving its depleted counterpart of the same class) stay a rule in the machine.
     */
    public record RadGenFuel(int production, int duration, Optional<String> output, int count) {

        public static final Codec<RadGenFuel> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("production").forGetter(RadGenFuel::production),
                Codec.INT.fieldOf("duration").forGetter(RadGenFuel::duration),
                Codec.STRING.optionalFieldOf("output").forGetter(RadGenFuel::output),
                Codec.INT.optionalFieldOf("count", 1).forGetter(RadGenFuel::count)
        ).apply(i, RadGenFuel::new));
    }

    public static final DataMap<Item, RadGenFuel> RADGEN_FUEL = item("radgen_fuel", RadGenFuel.CODEC);

    // ---- research reactor (1.7 TileEntityReactorResearch.fuelMap) ----

    /** What a spent research reactor plate becomes: an item id, and whether it comes out hot (1.7 metadata 1). */
    public record ResearchReactorWaste(String output, boolean hot) {

        public static final Codec<ResearchReactorWaste> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("output").forGetter(ResearchReactorWaste::output),
                Codec.BOOL.optionalFieldOf("hot", false).forGetter(ResearchReactorWaste::hot)
        ).apply(i, ResearchReactorWaste::new));
    }

    public static final DataMap<Item, ResearchReactorWaste> RESEARCH_REACTOR_FUEL =
            item("research_reactor_fuel", ResearchReactorWaste.CODEC);

    /** Loads the class at mod construction (HbmDataMaps.declare). */
    public static void declare() {}
}
