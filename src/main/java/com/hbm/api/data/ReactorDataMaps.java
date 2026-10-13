package com.hbm.api.data;

import com.hbm.Constants;
import com.hbm.config.MachineConfig;
import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc;
import com.hbm.items.machine.ItemRBMKRod.EnumDepleteFunc;
import com.hbm.items.machine.ItemRBMKRod.NType;
import com.hbm.util.Function;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import dev.golem.core.data.DataMaps;
import dev.golem.core.data.DataMaps.DataMap;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Reactor fuels and other per-item machine values as data ({@code data/<ns>/value_maps/item/...}, see
 * {@link HbmDataMaps}). An addon adds a fuel by registering its own item (of the reactor's item class) and giving it an
 * entry here. The values are 1.7's, checked by the {@code value_map_*} GameTests.
 */
public final class ReactorDataMaps {

    private ReactorDataMaps() {}

    static <V> DataMap<Item, V> item(String name, Codec<V> codec) {
        return DataMaps.map(ReactorDataMaps.class, Constants.MOD_ID, name, "item", () -> BuiltInRegistries.ITEM, codec);
    }

    /** {@code "#RRGGBB"}, as an int. */
    public static final Codec<Integer> COLOR = Codec.STRING.comapFlatMap(text -> {
        try {
            return DataResult.success(Integer.parseUnsignedInt(text.startsWith("#") ? text.substring(1) : text, 16));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Not a colour: " + text);
        }
    }, color -> String.format("#%06X", color));

    static final Codec<NType> NEUTRON = Codec.STRING.xmap(NType::valueOf, NType::name);

    /**
     * An RBMK fuel rod (1.7 {@code ItemRBMKRod} setter chains in {@code ModItems}). {@code function} and
     * {@code depletion} name {@link EnumBurnFunc} / {@link EnumDepleteFunc} entries; {@code pellet} is the recycling
     * pellet item.
     */
    public record RBMKFuel(double yield, double reactivity, double selfRate, EnumBurnFunc function, EnumDepleteFunc depletion,
                           double xenonGen, double xenonBurn, double heat, double diffusion, double meltingPoint,
                           double heatCoeffStart, double heatCoeffLength, NType neutronIn, NType neutronOut, int tint,
                           Optional<String> pellet) {
        public static final Codec<RBMKFuel> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("yield").forGetter(RBMKFuel::yield),
                Codec.DOUBLE.fieldOf("reactivity").forGetter(RBMKFuel::reactivity),
                Codec.DOUBLE.optionalFieldOf("self_rate", 0D).forGetter(RBMKFuel::selfRate),
                EnumBurnFunc.CODEC.optionalFieldOf("function", EnumBurnFunc.LOG_TEN).forGetter(RBMKFuel::function),
                EnumDepleteFunc.CODEC.optionalFieldOf("depletion", EnumDepleteFunc.GENTLE_SLOPE).forGetter(RBMKFuel::depletion),
                Codec.DOUBLE.optionalFieldOf("xenon_gen", 0.5D).forGetter(RBMKFuel::xenonGen),
                Codec.DOUBLE.optionalFieldOf("xenon_burn", 50D).forGetter(RBMKFuel::xenonBurn),
                Codec.DOUBLE.optionalFieldOf("heat", 1D).forGetter(RBMKFuel::heat),
                Codec.DOUBLE.optionalFieldOf("diffusion", 0.02D).forGetter(RBMKFuel::diffusion),
                Codec.DOUBLE.optionalFieldOf("melting_point", 1000D).forGetter(RBMKFuel::meltingPoint),
                Codec.DOUBLE.optionalFieldOf("heat_coeff_start", 0D).forGetter(RBMKFuel::heatCoeffStart),
                Codec.DOUBLE.optionalFieldOf("heat_coeff_length", 0D).forGetter(RBMKFuel::heatCoeffLength),
                NEUTRON.optionalFieldOf("neutron_in", NType.SLOW).forGetter(RBMKFuel::neutronIn),
                NEUTRON.optionalFieldOf("neutron_out", NType.FAST).forGetter(RBMKFuel::neutronOut),
                COLOR.optionalFieldOf("tint", 0x304825).forGetter(RBMKFuel::tint),
                Codec.STRING.optionalFieldOf("pellet").forGetter(RBMKFuel::pellet)
        ).apply(i, RBMKFuel::new));
    }

    public static final DataMap<Item, RBMKFuel> RBMK_FUEL = item("rbmk_fuel", RBMKFuel.CODEC);

    /**
     * A PWR fuel rod, keyed by the fresh rod (1.7 {@code ItemPWRFuel.EnumPWRFuel}): heat per flux, the reaction curve, the
     * flux one rod yields before it is spent, and the hot and depleted items it becomes.
     */
    public record PWRFuel(double heat, Function function, double yield, String hot, String depleted) {
        public static final Codec<PWRFuel> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("heat").forGetter(PWRFuel::heat),
                Function.CODEC.fieldOf("function").forGetter(PWRFuel::function),
                Codec.DOUBLE.optionalFieldOf("yield", 1_000_000_000D).forGetter(PWRFuel::yield),
                Codec.STRING.fieldOf("hot").forGetter(PWRFuel::hot),
                Codec.STRING.fieldOf("depleted").forGetter(PWRFuel::depleted)
        ).apply(i, PWRFuel::new));

        public Item hotItem() { return itemOf(hot); }
        public Item depletedItem() { return itemOf(depleted); }
    }

    public static final DataMap<Item, PWRFuel> PWR_FUEL = item("pwr_fuel", PWRFuel.CODEC);

    /** An item id resolved at use time ({@code minecraft:air} when unknown). */
    public static Item itemOf(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.parse(id));
    }

    /**
     * A ZIRNOX rod, keyed by the active rod (1.7 {@code ItemZirnoxRod.EnumZirnoxType}): its life in operations, heat per
     * operation, whether it only breeds (adds no operations to its neighbours), the radiation fresh and spent, the item
     * it becomes when used up ({@code depletes_into}) and the waste one used-up rod reprocesses into ({@code waste}).
     */
    public record ZirnoxFuel(int maxLife, int heat, boolean breeding, float baseRadiation, float depletedRadiation,
                             String depletesInto, Optional<String> waste) {
        public static final Codec<ZirnoxFuel> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("max_life").forGetter(ZirnoxFuel::maxLife),
                Codec.INT.fieldOf("heat").forGetter(ZirnoxFuel::heat),
                Codec.BOOL.optionalFieldOf("breeding", false).forGetter(ZirnoxFuel::breeding),
                Codec.FLOAT.fieldOf("base_radiation").forGetter(ZirnoxFuel::baseRadiation),
                Codec.FLOAT.fieldOf("depleted_radiation").forGetter(ZirnoxFuel::depletedRadiation),
                Codec.STRING.fieldOf("depletes_into").forGetter(ZirnoxFuel::depletesInto),
                Codec.STRING.optionalFieldOf("waste").forGetter(ZirnoxFuel::waste)
        ).apply(i, ZirnoxFuel::new));
    }

    public static final DataMap<Item, ZirnoxFuel> ZIRNOX_FUEL = item("zirnox_fuel", ZirnoxFuel.CODEC);

    /**
     * A Watz pellet, keyed by the fresh pellet (1.7 {@code ItemWatzPellet.EnumWatzType}): bar colour (and the depleted
     * sprite's darker colour), passive flux, heat per flux, mud per flux (1.7 halved its constructor argument; this is
     * the halved value), the reaction curve, the heat divisor and the absorber curve (each absent when 1.7 had null), the
     * yield, and the depleted pellet.
     */
    public record WatzPellet(int color, int colorDark, double passive, double heat, double mud,
                             Optional<Function> burn, Optional<Function> heatDiv,
                             Optional<Function> absorb, double yield, String depleted) {
        public static final Codec<WatzPellet> CODEC = RecordCodecBuilder.create(i -> i.group(
                COLOR.fieldOf("color").forGetter(WatzPellet::color),
                COLOR.fieldOf("color_dark").forGetter(WatzPellet::colorDark),
                Codec.DOUBLE.optionalFieldOf("passive", 0D).forGetter(WatzPellet::passive),
                Codec.DOUBLE.optionalFieldOf("heat", 0D).forGetter(WatzPellet::heat),
                Codec.DOUBLE.fieldOf("mud").forGetter(WatzPellet::mud),
                Function.CODEC.optionalFieldOf("burn").forGetter(WatzPellet::burn),
                Function.CODEC.optionalFieldOf("heat_div").forGetter(WatzPellet::heatDiv),
                Function.CODEC.optionalFieldOf("absorb").forGetter(WatzPellet::absorb),
                Codec.DOUBLE.optionalFieldOf("yield", 500_000_000D).forGetter(WatzPellet::yield),
                Codec.STRING.fieldOf("depleted").forGetter(WatzPellet::depleted)
        ).apply(i, WatzPellet::new));
    }

    public static final DataMap<Item, WatzPellet> WATZ_PELLET = item("watz_pellet", WatzPellet.CODEC);

    /**
     * A Chicago Pile rod (1.7 {@code ItemPileRodMK2.EnumPileRod}): fission multiplier, lifetime in absorbed neutrons, heat
     * per neutron, constant source term, and the item a spent rod becomes ({@code turns_into}, absent for a source rod).
     */
    public record PileRod(double reaction, double life, double heat, double source, Optional<String> turnsInto) {
        public static final Codec<PileRod> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("reaction", 0D).forGetter(PileRod::reaction),
                Codec.DOUBLE.optionalFieldOf("life", 0D).forGetter(PileRod::life),
                Codec.DOUBLE.optionalFieldOf("heat", 0D).forGetter(PileRod::heat),
                Codec.DOUBLE.optionalFieldOf("source", 0D).forGetter(PileRod::source),
                Codec.STRING.optionalFieldOf("turns_into").forGetter(PileRod::turnsInto)
        ).apply(i, PileRod::new));
    }

    public static final DataMap<Item, PileRod> PILE_ROD = item("pile_rod", PileRod.CODEC);

    /**
     * An RTG pellet (1.7 {@code ItemRTGPellet} constructor arguments in {@code ModItems}): heat, the pellet's whole life in
     * ticks (1.7 {@code setDecays(material, life)}, usually {@code (long) (RTGUtil.getLifespan(halfLife, type, false) * 1.5)})
     * and the depleted pellet it decays to. The output and radiation fall linearly with the life left (1.7). {@code
     * heat_decaying} is the heat when {@code 9.01_doRTGsDecay} is on, where 1.7 gave the gold and lead pellets more.
     */
    public record RTGPellet(int heat, Optional<Integer> heatDecaying, long lifespan, String decaysTo) {
        public static final Codec<RTGPellet> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("heat").forGetter(RTGPellet::heat),
                Codec.INT.optionalFieldOf("heat_decaying").forGetter(RTGPellet::heatDecaying),
                Codec.LONG.fieldOf("lifespan").forGetter(RTGPellet::lifespan),
                Codec.STRING.fieldOf("decays_to").forGetter(RTGPellet::decaysTo)
        ).apply(i, RTGPellet::new));

        /** The heat under the current decay setting. */
        public int effectiveHeat() {
            return MachineConfig.rtgDecay() ? heatDecaying.orElse(heat) : heat;
        }
    }

    public static final DataMap<Item, RTGPellet> RTG_PELLET = item("rtg_pellet", RTGPellet.CODEC);

    /** A betavoltaic cell's constant output in HE/t (1.7 {@code ItemBatterySC.EnumBatterySC}). */
    public static final DataMap<Item, Long> BETAVOLTAIC = item("betavoltaic", Codec.LONG);

    /**
     * A battery or capacitor pack (1.7 {@code ItemBatteryPack.EnumBatteryPack}): capacity, charge and discharge rates in
     * HE, whether the battery socket draws it as a capacitor, and the socket model texture.
     */
    public record BatteryPack(long capacity, long chargeRate, long dischargeRate, boolean capacitor, String texture) {
        public static final Codec<BatteryPack> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("capacity").forGetter(BatteryPack::capacity),
                Codec.LONG.fieldOf("charge_rate").forGetter(BatteryPack::chargeRate),
                Codec.LONG.fieldOf("discharge_rate").forGetter(BatteryPack::dischargeRate),
                Codec.BOOL.optionalFieldOf("capacitor", false).forGetter(BatteryPack::capacitor),
                Codec.STRING.fieldOf("texture").forGetter(BatteryPack::texture)
        ).apply(i, BatteryPack::new));
    }

    public static final DataMap<Item, BatteryPack> BATTERY_PACK = item("battery_pack", BatteryPack.CODEC);

    /**
     * A research-reactor plate (1.7 {@code ItemPlateFuel} constructor arguments, {@code ModIngredientItems.PlateFuelType}):
     * lifetime in reaction events, the reaction curve (one of 1.7's five, {@code ItemPlateFuel.FunctionEnum}) and the
     * reactivity.
     */
    public record PlateFuel(int lifeTime, ItemPlateFuel.FunctionEnum function, int reactivity) {
        public static final Codec<PlateFuel> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("life_time").forGetter(PlateFuel::lifeTime),
                Codec.STRING.xmap(ItemPlateFuel.FunctionEnum::valueOf,
                        ItemPlateFuel.FunctionEnum::name).fieldOf("function").forGetter(PlateFuel::function),
                Codec.INT.fieldOf("reactivity").forGetter(PlateFuel::reactivity)
        ).apply(i, PlateFuel::new));
    }

    public static final DataMap<Item, PlateFuel> PLATE_FUEL = item("plate_fuel", PlateFuel.CODEC);

    /** Loads the class at mod construction (from {@link HbmDataMaps#declare()}). */
    public static void declare() {}
}
