package com.hbm.api.data;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.DataMaps;
import dev.golem.core.data.DataMaps.DataMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Per-item values of machine parts (coils, drill bits, pistons, upgrades, boosters) as item data maps
 * ({@code data/<ns>/value_maps/item/...}, see {@link HbmDataMaps}). An addon's part is its own item of the part's class
 * with an entry here. The values are 1.7's, checked by the {@code value_map_*} GameTests.
 */
public final class MachinePartDataMaps {

    private MachinePartDataMaps() {}

    private static <V> DataMap<Item, V> item(String name, Codec<V> codec) {
        return DataMaps.map(MachinePartDataMaps.class, Constants.MOD_ID, name, "item", () -> BuiltInRegistries.ITEM, codec);
    }

    /** A particle accelerator coil's operating ranges (1.7 {@code ItemPACoil.EnumCoilType}). */
    public record Coil(int quadMin, int quadMax, int diMin, int diMax, int diDistMin) {
        public static final Codec<Coil> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("quadrupole_min").forGetter(Coil::quadMin),
                Codec.INT.fieldOf("quadrupole_max").forGetter(Coil::quadMax),
                Codec.INT.fieldOf("dipole_min").forGetter(Coil::diMin),
                Codec.INT.fieldOf("dipole_max").forGetter(Coil::diMax),
                Codec.INT.fieldOf("dipole_min_distance").forGetter(Coil::diDistMin)
        ).apply(i, Coil::new));
    }

    public static final DataMap<Item, Coil> PA_COIL = item("pa_coil", Coil.CODEC);

    /** An excavator drill bit (1.7 {@code ItemDrillbit.EnumDrillType}): speed multiplier, ore tier, fortune, vein miner, silk touch. */
    public record DrillBit(double speed, int tier, int fortune, boolean vein, boolean silk) {
        public static final Codec<DrillBit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("speed").forGetter(DrillBit::speed),
                Codec.INT.fieldOf("tier").forGetter(DrillBit::tier),
                Codec.INT.optionalFieldOf("fortune", 0).forGetter(DrillBit::fortune),
                Codec.BOOL.optionalFieldOf("vein_miner", false).forGetter(DrillBit::vein),
                Codec.BOOL.optionalFieldOf("silk_touch", false).forGetter(DrillBit::silk)
        ).apply(i, DrillBit::new));
    }

    public static final DataMap<Item, DrillBit> DRILL_BIT = item("drill_bit", DrillBit.CODEC);

    /**
     * A combustion engine piston's efficiency per fuel grade (1.7 {@code ItemPistons.EnumPistonType}), keyed by the grade
     * name ({@code LOW}, {@code MEDIUM}, {@code HIGH}, {@code AERO}, {@code GAS}); a grade it omits burns at 0.
     */
    public static final DataMap<Item, Map<String, Double>> PISTON = item("piston", Codec.unboundedMap(Codec.STRING, Codec.DOUBLE));

    /**
     * A machine upgrade (1.7 {@code ItemMachineUpgrade}'s type and tier): which {@code UpgradeType} it raises and by how
     * many levels. Any item with an entry counts as that upgrade in a machine's upgrade slots.
     */
    public record Upgrade(com.hbm.items.machine.ItemMachineUpgrade.UpgradeType type, int tier) {
        public static final Codec<Upgrade> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.xmap(com.hbm.items.machine.ItemMachineUpgrade.UpgradeType::valueOf,
                        com.hbm.items.machine.ItemMachineUpgrade.UpgradeType::name).fieldOf("type").forGetter(Upgrade::type),
                Codec.INT.fieldOf("tier").forGetter(Upgrade::tier)
        ).apply(i, Upgrade::new));
    }

    public static final DataMap<Item, Upgrade> MACHINE_UPGRADE = item("machine_upgrade", Upgrade.CODEC);

    /** Plasma forge boosters: how many ticks one item runs the forge at four times speed (1.7 {@code TileEntityFusionPlasmaForge.boosters}). */
    public static final DataMap<Item, Integer> PLASMA_FORGE_BOOSTER = item("plasma_forge_booster", Codec.INT);

    /**
     * The fuel class a burner's per-class bonuses apply to (1.7 {@code ModuleBurnTime}): {@code log}, {@code wood},
     * {@code coal}, {@code lignite}, {@code coke}, {@code solid}, {@code rocket} or {@code balefire}. Items without an
     * entry are classed by 1.7's name scan (coke, lignite, coal, logs, wood).
     */
    public static final DataMap<Item, String> FUEL_CATEGORY = item("fuel_category", Codec.STRING);

    /** Loads the class at mod construction (from {@link HbmDataMaps#declare()}). */
    public static void declare() {}
}
