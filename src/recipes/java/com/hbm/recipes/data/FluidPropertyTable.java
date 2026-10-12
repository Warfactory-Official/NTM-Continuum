package com.hbm.recipes.data;

import static com.hbm.inventory.fluid.Fluids.*;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.FluidCombustion.FuelGrade;
import com.hbm.inventory.fluid.FluidHazard;
import com.hbm.inventory.fluid.FluidHazard.Symbol;
import com.hbm.inventory.fluid.FluidProperties;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.inventory.fluid.trait.FT_Pheromone;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FT_Toxin;
import com.hbm.inventory.fluid.trait.FT_Toxin.ToxinDirectDamage;
import com.hbm.inventory.fluid.trait.FT_Toxin.ToxinEffects;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.world.Effects;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * 1.7 {@code Fluids.init}: every fluid's colour, hazard diamond ({@code p, f, r, EnumSymbol}), temperature and traits,
 * in 1.7's order, as the rows of the {@code fluid_property} files ({@link FluidPropertySource}). The declarations, the
 * trait calls after {@code metaOrder} and the spreadsheet fuels ({@code registerCalculatedFuel}) are 1.7's lines;
 * {@code addContainers} (canister and gas tank colours) is not a fluid property and is left out, as are the custom fluid
 * and trait JSON files of 1.7's config folder. A trait replaces an earlier trait of its class, as in 1.7's
 * {@code FluidType.addTraits}.
 */
final class FluidPropertyTable {

    private FluidPropertyTable() {}

    /** 1.7 {@code SOOT_UNREFINED_OIL}: burns 4x dirtier than regular fuel. */
    static final float SOOT_UNREFINED_OIL = PollutionHandler.SOOT_PER_SECOND * 0.1F;
    /** 1.7 {@code SOOT_REFINED_OIL}: original baseline, used for most fuels. */
    static final float SOOT_REFINED_OIL = PollutionHandler.SOOT_PER_SECOND * 0.025F;
    /** 1.7 {@code SOOT_GAS}: gasses burn very cleanly. */
    static final float SOOT_GAS = PollutionHandler.SOOT_PER_SECOND * 0.005F;
    /** 1.7 {@code LEAD_FUEL}: original baseline for leaded fuels. */
    static final float LEAD_FUEL = PollutionHandler.HEAVY_METAL_PER_SECOND * 0.025F;
    /** 1.7 {@code POISON_OIL}: poison stat for most petrochemicals. */
    static final float POISON_OIL = PollutionHandler.POISON_PER_SECOND * 0.0025F;
    /** 1.7 {@code POISON_EXTREME}: poison stat for horrible chemicals like red mud or phosgene. */
    static final float POISON_EXTREME = PollutionHandler.POISON_PER_SECOND * 0.025F;
    /** 1.7 {@code POISON_MINOR}: poison stat for mostly inert things like carbon dioxide. */
    static final float POISON_MINOR = PollutionHandler.POISON_PER_SECOND * 0.001F;

    static final FT_Polluting P_OIL = new FT_Polluting().burn(PollutionType.SOOT, SOOT_UNREFINED_OIL).release(PollutionType.POISON, POISON_OIL);
    static final FT_Polluting P_FUEL = new FT_Polluting().burn(PollutionType.SOOT, SOOT_REFINED_OIL).release(PollutionType.POISON, POISON_OIL);
    static final FT_Polluting P_FUEL_LEADED = new FT_Polluting().burn(PollutionType.SOOT, SOOT_REFINED_OIL).burn(PollutionType.HEAVYMETAL, LEAD_FUEL)
            .release(PollutionType.POISON, POISON_OIL).release(PollutionType.HEAVYMETAL, LEAD_FUEL * 0.1F);
    static final FT_Polluting P_GAS = new FT_Polluting().burn(PollutionType.SOOT, SOOT_GAS).release(PollutionType.POISON, POISON_OIL);
    static final FT_Polluting P_LIQUID_GAS = new FT_Polluting().burn(PollutionType.SOOT, SOOT_GAS * 2F);

    /** One 1.7 {@code FluidType} as its constructor, {@code setTemp} and {@code addTraits} set it. */
    static final class Def {
        final int color;
        final FluidHazard hazard;
        int temperature = FluidType.ROOM_TEMPERATURE;
        final Map<Class<?>, Object> traits = new LinkedHashMap<>();

        Def(int color, int p, int f, int r, Symbol symbol) {
            this.color = color;
            this.hazard = new FluidHazard(p, f, r, symbol);
        }

        Def setTemp(int temperature) {
            this.temperature = temperature;
            return this;
        }

        Def addTraits(Object... traits) {
            for (Object trait : traits) this.traits.put(trait.getClass(), trait);
            return this;
        }

        <T> T getTrait(Class<T> type) {
            return type.cast(traits.get(type));
        }

        FluidProperties.Properties row() {
            return new FluidProperties.Properties(color, null, temperature, hazard, new ArrayList<>(traits.values()));
        }
    }

    private static Map<FluidType, Def> defs;

    /** Every fluid's row, in 1.7's registration order. */
    static synchronized Map<FluidType, FluidProperties.Properties> rows() {
        if (defs == null) {
            defs = new LinkedHashMap<>();
            init();
        }
        Map<FluidType, FluidProperties.Properties> rows = new LinkedHashMap<>();
        defs.forEach((fluid, def) -> rows.put(fluid, def.row()));
        return rows;
    }

    /** 1.7 {@code new FluidType(name, color, p, f, r, symbol)}. */
    private static Def fluid(FluidType fluid, int color, int p, int f, int r, Symbol symbol) {
        Def def = new Def(color, p, f, r, symbol);
        if (defs.putIfAbsent(fluid, def) != null) throw new IllegalStateException("fluid declared twice: " + fluid);
        return def;
    }

    /** A fluid declared above, for 1.7's later {@code X.addTraits(...)} calls. */
    private static Def at(FluidType fluid) {
        Def def = defs.get(fluid);
        if (def == null) throw new IllegalStateException("fluid not declared: " + fluid);
        return def;
    }

    /** 1.7 {@code new PotionEffect(id, duration, amplifier)}, resolved when applied. */
    private static Supplier<MobEffectInstance> effect(String id, int duration, int amplifier) {
        return () -> Effects.instance(id, duration, amplifier);
    }

    /** 1.7 {@code Fluids.registerCalculatedFuel}. */
    private static void registerCalculatedFuel(Def type, double base, double combustMult, FuelGrade grade) {

        long flammable = (long) base;
        long combustible = (long) (base * combustMult);

        flammable = round(flammable);
        combustible = round(combustible);

        type.addTraits(new FT_Flammable(flammable));

        if (combustible > 0 && grade != null)
            type.addTraits(new FT_Combustible(grade, combustible));
    }

    /** 1.7 {@code Fluids.round}: ugly but it does the thing well enough. */
    private static long round(long l) {
        if (l > 10_000_000L) return l - (l % 100_000L);
        if (l > 1_000_000L) return l - (l % 10_000L);
        if (l > 100_000L) return l - (l % 1_000L);
        if (l > 10_000L) return l - (l % 100L);
        if (l > 1_000L) return l - (l % 10L);

        return l;
    }

    /** 1.7 {@code Fluids.init}. */
    private static void init() {

        fluid(NONE, 0x888888, 0, 0, 0, Symbol.NONE);
        fluid(WATER, 0x3333FF, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, UNSIPHONABLE);
        fluid(STEAM, 0xe5e5e5, 3, 0, 0, Symbol.NONE).setTemp(100).addTraits(GASEOUS, UNSIPHONABLE);
        fluid(HOTSTEAM, 0xE7D6D6, 4, 0, 0, Symbol.NONE).setTemp(300).addTraits(GASEOUS, UNSIPHONABLE);
        fluid(SUPERHOTSTEAM, 0xE7B7B7, 4, 0, 0, Symbol.NONE).setTemp(450).addTraits(GASEOUS, UNSIPHONABLE);
        fluid(ULTRAHOTSTEAM, 0xE39393, 4, 0, 0, Symbol.NONE).setTemp(600).addTraits(GASEOUS, UNSIPHONABLE);
        fluid(COOLANT, 0xd8fcff, 1, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(LAVA, 0xFF3300, 4, 0, 0, Symbol.NOWATER).setTemp(1200).addTraits(LIQUID, VISCOUS);
        fluid(DEUTERIUM, 0x0000FF, 3, 4, 0, Symbol.NONE).addTraits(new FT_Flammable(5_000), new FT_Combustible(FuelGrade.HIGH, 10_000), GASEOUS);
        fluid(TRITIUM, 0x000099, 3, 4, 0, Symbol.RADIATION).addTraits(new FT_Flammable(5_000), new FT_Combustible(FuelGrade.HIGH, 10_000), GASEOUS, new FT_VentRadiation(0.001F));
        fluid(OIL, 0x020202, 2, 1, 0, Symbol.NONE).addTraits(new FT_Flammable(10_000), LIQUID, VISCOUS, P_OIL);
        fluid(HOTOIL, 0x300900, 2, 3, 0, Symbol.NONE).setTemp(350).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(HEAVYOIL, 0x141312, 2, 1, 0, Symbol.NONE).addTraits(new FT_Flammable(50_000), new FT_Combustible(FuelGrade.LOW, 25_000), LIQUID, VISCOUS, P_OIL);
        fluid(BITUMEN, 0x1f2426, 2, 0, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(SMEAR, 0x190f01, 2, 1, 0, Symbol.NONE).addTraits(new FT_Flammable(50_000), LIQUID, VISCOUS, P_OIL);
        fluid(HEATINGOIL, 0x211806, 2, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(150_000), new FT_Combustible(FuelGrade.LOW, 100_000), LIQUID, VISCOUS, P_OIL);
        fluid(RECLAIMED, 0x332b22, 2, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(100_000), new FT_Combustible(FuelGrade.LOW, 200_000), LIQUID, VISCOUS, P_FUEL);
        fluid(PETROIL, 0x44413d, 1, 3, 0, Symbol.NONE).addTraits(new FT_Flammable(125_000), new FT_Combustible(FuelGrade.MEDIUM, 300_000), LIQUID, P_FUEL);
        fluid(LUBRICANT, 0x606060, 2, 1, 0, Symbol.NONE).addTraits(LIQUID, P_OIL);
        fluid(NAPHTHA, 0x595744, 2, 1, 0, Symbol.NONE).addTraits(new FT_Flammable(125_000), new FT_Combustible(FuelGrade.MEDIUM, 200_000), LIQUID, VISCOUS, P_FUEL);
        fluid(DIESEL, 0xf2eed5, 1, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(200_000), new FT_Combustible(FuelGrade.HIGH, 500_000), LIQUID, P_FUEL);
        fluid(LIGHTOIL, 0x8c7451, 1, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(200_000), new FT_Combustible(FuelGrade.MEDIUM, 500_000), LIQUID, P_FUEL);
        fluid(KEROSENE, 0xffa5d2, 1, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(300_000), new FT_Combustible(FuelGrade.AERO, 1_250_000), LIQUID, P_FUEL);
        fluid(GAS, 0xfffeed, 1, 4, 1, Symbol.NONE).addTraits(new FT_Flammable(10_000), GASEOUS, P_GAS);
        fluid(PETROLEUM, 0x7cb7c9, 1, 4, 1, Symbol.NONE).addTraits(new FT_Flammable(25_000), GASEOUS, P_GAS);
        fluid(LPG, 0x4747EA, 1, 3, 1, Symbol.NONE).addTraits(new FT_Flammable(200_000), new FT_Combustible(FuelGrade.HIGH, 400_000), LIQUID, P_LIQUID_GAS);
        fluid(BIOGAS, 0xbfd37c, 1, 4, 1, Symbol.NONE).addTraits(new FT_Flammable(25_000), GASEOUS, P_GAS);
        fluid(BIOFUEL, 0xeef274, 1, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(150_000), new FT_Combustible(FuelGrade.HIGH, 400_000), LIQUID, P_FUEL);
        fluid(NITAN, 0x8018ad, 2, 4, 1, Symbol.NONE).addTraits(new FT_Flammable(2_000_000), new FT_Combustible(FuelGrade.HIGH, 5_000_000), LIQUID, P_FUEL);
        fluid(UF6, 0xD1CEBE, 4, 0, 2, Symbol.RADIATION).addTraits(new FT_VentRadiation(0.2F), new FT_Corrosive(15), GASEOUS);
        fluid(PUF6, 0x4C4C4C, 4, 0, 4, Symbol.RADIATION).addTraits(new FT_VentRadiation(0.1F), new FT_Corrosive(15), GASEOUS);
        fluid(SAS3, 0x4ffffc, 5, 0, 4, Symbol.RADIATION).addTraits(new FT_VentRadiation(1F), new FT_Corrosive(30), LIQUID);
        fluid(SCHRABIDIC, 0x006B6B, 5, 0, 5, Symbol.ACID).addTraits(new FT_VentRadiation(1F), new FT_Corrosive(75), new FT_Poison(true, 2), LIQUID);
        fluid(AMAT, 0x010101, 5, 0, 5, Symbol.ANTIMATTER).addTraits(ANTI, GASEOUS);
        fluid(ASCHRAB, 0xb50000, 5, 0, 5, Symbol.ANTIMATTER).addTraits(ANTI, GASEOUS);
        fluid(PEROXIDE, 0xfff7aa, 3, 0, 3, Symbol.OXIDIZER).addTraits(new FT_Corrosive(40), LIQUID);
        fluid(WATZ, 0x86653E, 4, 0, 3, Symbol.ACID).addTraits(new FT_Corrosive(60), new FT_VentRadiation(0.1F), LIQUID, VISCOUS, new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        fluid(CRYOGEL, 0x32ffff, 2, 0, 0, Symbol.CRYOGENIC).setTemp(-170).addTraits(LIQUID, VISCOUS);
        fluid(HYDROGEN, 0x4286f4, 3, 4, 0, Symbol.CRYOGENIC).setTemp(-260).addTraits(new FT_Flammable(5_000), new FT_Combustible(FuelGrade.HIGH, 10_000), LIQUID, EVAP);
        fluid(OXYGEN, 0x98bdf9, 3, 0, 0, Symbol.CRYOGENIC).setTemp(-100).addTraits(LIQUID, EVAP);
        fluid(XENON, 0xba45e8, 0, 0, 0, Symbol.ASPHYXIANT).addTraits(GASEOUS);
        fluid(BALEFIRE, 0x28e02e, 4, 4, 3, Symbol.RADIATION).setTemp(1500).addTraits(new FT_Corrosive(50), new FT_Flammable(1_000_000), new FT_Combustible(FuelGrade.HIGH, 2_500_000), LIQUID, VISCOUS, P_FUEL);
        fluid(MERCURY, 0x808080, 2, 0, 0, Symbol.NONE).addTraits(LIQUID, new FT_Poison(false, 2));
        fluid(PAIN, 0x938541, 2, 0, 1, Symbol.ACID).setTemp(300).addTraits(new FT_Corrosive(30), new FT_Poison(true, 2), LIQUID, VISCOUS);
        fluid(WASTEFLUID, 0x544400, 2, 0, 1, Symbol.RADIATION).addTraits(new FT_VentRadiation(0.5F), NOCON, LIQUID, VISCOUS);
        fluid(WASTEGAS, 0xB8B8B8, 2, 0, 1, Symbol.RADIATION).addTraits(new FT_VentRadiation(0.5F), NOCON, GASEOUS);
        fluid(GASOLINE, 0x445772, 1, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(400_000), new FT_Combustible(FuelGrade.HIGH, 1_000_000), LIQUID, P_FUEL);
        fluid(COALGAS, 0x445772, 1, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(75_000), new FT_Combustible(FuelGrade.MEDIUM, 150_000), LIQUID, P_FUEL);
        fluid(SPENTSTEAM, 0x445772, 2, 0, 0, Symbol.NONE).addTraits(NOCON, GASEOUS);
        fluid(FRACKSOL, 0x798A6B, 1, 3, 3, Symbol.ACID).addTraits(new FT_Corrosive(15), new FT_Poison(false, 0), LIQUID, VISCOUS);
        fluid(PLASMA_DT, 0xF7AFDE, 0, 4, 0, Symbol.RADIATION).setTemp(3250).addTraits(NOCON, NOID, PLASMA);
        fluid(PLASMA_HD, 0xF0ADF4, 0, 4, 0, Symbol.RADIATION).setTemp(2500).addTraits(NOCON, NOID, PLASMA);
        fluid(PLASMA_HT, 0xD1ABF2, 0, 4, 0, Symbol.RADIATION).setTemp(3000).addTraits(NOCON, NOID, PLASMA);
        fluid(PLASMA_XM, 0xC6A5FF, 0, 4, 1, Symbol.RADIATION).setTemp(4250).addTraits(NOCON, NOID, PLASMA);
        fluid(PLASMA_BF, 0xA7F1A3, 4, 5, 4, Symbol.ANTIMATTER).setTemp(8500).addTraits(NOCON, NOID, PLASMA);
        fluid(CARBONDIOXIDE, 0x404040, 3, 0, 0, Symbol.ASPHYXIANT).addTraits(GASEOUS, new FT_Polluting().release(PollutionType.POISON, POISON_MINOR));
        fluid(PLASMA_DH3, 0xFF83AA, 0, 4, 0, Symbol.RADIATION).setTemp(3480).addTraits(NOCON, NOID, PLASMA);
        fluid(HELIUM3, 0xFCF0C4, 0, 0, 0, Symbol.ASPHYXIANT).addTraits(GASEOUS);
        fluid(DEATH, 0x717A88, 2, 0, 1, Symbol.ACID).setTemp(300).addTraits(new FT_Corrosive(80), new FT_Poison(true, 4), LEADCON, LIQUID, VISCOUS);
        fluid(ETHANOL, 0xe0ffff, 2, 3, 0, Symbol.NONE).addTraits(new FT_Flammable(75_000), new FT_Combustible(FuelGrade.HIGH, 200_000), LIQUID, P_FUEL);
        fluid(HEAVYWATER, 0x00a0b0, 1, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(CRACKOIL, 0x020202, 2, 1, 0, Symbol.NONE).addTraits(new FT_Flammable(10_000), LIQUID, VISCOUS, P_OIL);
        fluid(COALOIL, 0x020202, 2, 1, 0, Symbol.NONE).addTraits(new FT_Flammable(10_000), LIQUID, VISCOUS, P_OIL);
        fluid(HOTCRACKOIL, 0x300900, 2, 3, 0, Symbol.NONE).setTemp(350).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(NAPHTHA_CRACK, 0x595744, 2, 1, 0, Symbol.NONE).addTraits(new FT_Flammable(125_000), new FT_Combustible(FuelGrade.MEDIUM, 200_000), LIQUID, VISCOUS, P_FUEL);
        fluid(LIGHTOIL_CRACK, 0x8c7451, 1, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(200_000), new FT_Combustible(FuelGrade.MEDIUM, 500_000), LIQUID, P_FUEL);
        fluid(DIESEL_CRACK, 0xf2eed5, 1, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(200_000), new FT_Combustible(FuelGrade.HIGH, 450_000), LIQUID, P_FUEL);
        fluid(AROMATICS, 0x68A09A, 1, 4, 1, Symbol.NONE).addTraits(new FT_Flammable(25_000), LIQUID, VISCOUS, P_GAS);
        fluid(UNSATURATEDS, 0x628FAE, 1, 4, 1, Symbol.NONE).addTraits(new FT_Flammable(1_000_000), GASEOUS, P_GAS);
        fluid(SALIENT, 0x457F2D, 0, 0, 0, Symbol.NONE).addTraits(DELICIOUS, LIQUID, VISCOUS);
        fluid(XPJUICE, 0xBBFF09, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(ENDERJUICE, 0x127766, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(PETROIL_LEADED, 0x44413d, 1, 3, 0, Symbol.NONE).addTraits(new FT_Flammable(125_000), new FT_Combustible(FuelGrade.MEDIUM, 450_000), LIQUID, P_FUEL_LEADED);
        fluid(GASOLINE_LEADED, 0x445772, 1, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(400_000), new FT_Combustible(FuelGrade.HIGH, 1_500_000), LIQUID, P_FUEL_LEADED);
        fluid(COALGAS_LEADED, 0x445772, 1, 2, 0, Symbol.NONE).addTraits(new FT_Flammable(75_000), new FT_Combustible(FuelGrade.MEDIUM, 250_000), LIQUID, P_FUEL_LEADED);
        fluid(SULFURIC_ACID, 0xB0AA64, 3, 0, 2, Symbol.ACID).addTraits(new FT_Corrosive(50), LIQUID);
        fluid(COOLANT_HOT, 0x99525E, 1, 0, 0, Symbol.NONE).setTemp(600).addTraits(LIQUID);
        fluid(MUG, 0x4B2D28, 0, 0, 0, Symbol.NONE).addTraits(DELICIOUS, LIQUID);
        fluid(MUG_HOT, 0x6B2A20, 0, 0, 0, Symbol.NONE).setTemp(500).addTraits(DELICIOUS, LIQUID);
        fluid(WOODOIL, 0x847D54, 2, 2, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(COALCREOSOTE, 0x51694F, 3, 2, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(SEEDSLURRY, 0x7CC35E, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(NITRIC_ACID, 0xBB7A1E, 3, 0, 2, Symbol.OXIDIZER).addTraits(LIQUID, new FT_Corrosive(60), new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        fluid(SOLVENT, 0xE4E3EF, 2, 3, 0, Symbol.NONE).addTraits(LIQUID, new FT_Corrosive(30));
        fluid(BLOOD, 0xB22424, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, DELICIOUS);
        fluid(BLOOD_HOT, 0xF22419, 3, 0, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS).setTemp(666);
        fluid(SYNGAS, 0x131313, 1, 4, 2, Symbol.NONE).addTraits(GASEOUS);
        fluid(OXYHYDROGEN, 0x483FC1, 0, 4, 2, Symbol.NONE).addTraits(GASEOUS);
        fluid(RADIOSOLVENT, 0xA4D7DD, 3, 3, 0, Symbol.NONE).addTraits(LIQUID, new FT_Corrosive(50));
        fluid(CHLORINE, 0xBAB572, 3, 0, 0, Symbol.OXIDIZER).addTraits(GASEOUS, new FT_Corrosive(25));
        fluid(HEAVYOIL_VACUUM, 0x131214, 2, 1, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(REFORMATE, 0x835472, 2, 2, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_FUEL);
        fluid(LIGHTOIL_VACUUM, 0x8C8851, 1, 2, 0, Symbol.NONE).addTraits(LIQUID, P_FUEL);
        fluid(SOURGAS, 0xC9BE0D, 4, 4, 0, Symbol.ACID).addTraits(GASEOUS, new FT_Corrosive(10), new FT_Poison(false, 1), new FT_Polluting().burn(PollutionType.SOOT, SOOT_GAS).release(PollutionType.POISON, POISON_EXTREME));
        fluid(XYLENE, 0x5C4E76, 2, 3, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_FUEL);
        fluid(HEATINGOIL_VACUUM, 0x211D06, 2, 2, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(DIESEL_REFORM, 0xCDC3C6, 1, 2, 0, Symbol.NONE).addTraits(LIQUID, P_FUEL);
        fluid(DIESEL_CRACK_REFORM, 0xCDC3CC, 1, 2, 0, Symbol.NONE).addTraits(LIQUID, P_FUEL);
        fluid(KEROSENE_REFORM, 0xFFA5F3, 1, 2, 0, Symbol.NONE).addTraits(LIQUID, P_FUEL);
        fluid(REFORMGAS, 0x6362AE, 1, 4, 1, Symbol.NONE).addTraits(GASEOUS, P_GAS);
        fluid(COLLOID, 0x787878, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(PHOSGENE, 0xCFC4A4, 4, 0, 1, Symbol.NONE).addTraits(GASEOUS, new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        fluid(MUSTARDGAS, 0xBAB572, 4, 1, 1, Symbol.NONE).addTraits(GASEOUS, new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        fluid(IONGEL, 0xB8FFFF, 1, 0, 4, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(OIL_COKER, 0x001802, 2, 1, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(NAPHTHA_COKER, 0x495944, 2, 1, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(GAS_COKER, 0xDEF4CA, 1, 4, 0, Symbol.NONE).addTraits(GASEOUS, P_GAS);
        fluid(EGG, 0xD2C273, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(CHOLESTEROL, 0xD6D2BD, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(ESTRADIOL, 0xCDD5D8, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(FISHOIL, 0x4B4A45, 0, 1, 0, Symbol.NONE).addTraits(LIQUID, P_FUEL);
        fluid(SUNFLOWEROIL, 0xCBAD45, 0, 1, 0, Symbol.NONE).addTraits(LIQUID, P_FUEL);
        fluid(NITROGLYCERIN, 0x92ACA6, 0, 4, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(REDMUD, 0xD85638, 3, 0, 4, Symbol.NONE).addTraits(LIQUID, VISCOUS, LEADCON, new FT_Corrosive(60), new FT_Flammable(1_000), new FT_Polluting().release(PollutionType.POISON, POISON_EXTREME));
        fluid(CHLOROCALCITE_SOLUTION, 0x808080, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, NOCON, new FT_Corrosive(60));
        fluid(CHLOROCALCITE_MIX, 0x808080, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, NOCON, new FT_Corrosive(60));
        fluid(CHLOROCALCITE_CLEANED, 0x808080, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, NOCON, new FT_Corrosive(60));
        fluid(POTASSIUM_CHLORIDE, 0x808080, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, NOCON, new FT_Corrosive(60));
        fluid(CALCIUM_CHLORIDE, 0x808080, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, NOCON, new FT_Corrosive(60));
        fluid(CALCIUM_SOLUTION, 0x808080, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, NOCON, new FT_Corrosive(60));
        fluid(SMOKE, 0x808080, 0, 0, 0, Symbol.NONE).addTraits(GASEOUS, NOID, NOCON);
        fluid(SMOKE_LEADED, 0x808080, 0, 0, 0, Symbol.NONE).addTraits(GASEOUS, NOID, NOCON);
        fluid(SMOKE_POISON, 0x808080, 0, 0, 0, Symbol.NONE).addTraits(GASEOUS, NOID, NOCON);
        fluid(HELIUM4, 0xE54B0A, 0, 0, 0, Symbol.ASPHYXIANT).addTraits(GASEOUS);
        fluid(HEAVYWATER_HOT, 0x4D007B, 1, 0, 0, Symbol.NONE).setTemp(600).addTraits(LIQUID, VISCOUS);
        fluid(SODIUM, 0xCCD4D5, 1, 2, 3, Symbol.NONE).setTemp(400).addTraits(LIQUID, VISCOUS);
        fluid(SODIUM_HOT, 0xE2ADC1, 1, 2, 3, Symbol.NONE).setTemp(1200).addTraits(LIQUID, VISCOUS);
        fluid(THORIUM_SALT, 0x7A5542, 2, 0, 3, Symbol.NONE).setTemp(800).addTraits(LIQUID, VISCOUS, new FT_Corrosive(65));
        fluid(THORIUM_SALT_HOT, 0x3E3627, 2, 0, 3, Symbol.NONE).setTemp(1600).addTraits(LIQUID, VISCOUS, new FT_Corrosive(65));
        fluid(THORIUM_SALT_DEPLETED, 0x302D1C, 2, 0, 3, Symbol.NONE).setTemp(800).addTraits(LIQUID, VISCOUS, new FT_Corrosive(65));
        fluid(FULLERENE, 0xFF7FED, 3, 3, 3, Symbol.NONE).addTraits(LIQUID, new FT_Corrosive(65), new FT_Polluting().release(PollutionType.POISON, POISON_MINOR));
        fluid(PHEROMONE, 0x5FA6E8, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, new FT_Pheromone(1));
        fluid(PHEROMONE_M, 0x48C9B0 , 0, 0, 0, Symbol.NONE).addTraits(LIQUID, new FT_Pheromone(2));
        fluid(OIL_DS, 0x121212, 2, 1, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(HOTOIL_DS, 0x3F180F, 2, 3, 0, Symbol.NONE).setTemp(350).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(CRACKOIL_DS, 0x2A1C11, 2, 1, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(HOTCRACKOIL_DS, 0x3A1A28, 2, 3, 0, Symbol.NONE).setTemp(350).addTraits(LIQUID, VISCOUS, P_OIL);
        fluid(NAPHTHA_DS, 0x63614E, 2, 1, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS, P_FUEL);
        fluid(LIGHTOIL_DS, 0x63543E, 1, 2, 0, Symbol.NONE).addTraits(LIQUID, P_FUEL);
        fluid(STELLAR_FLUX, 0xE300FF, 0, 4, 4, Symbol.ANTIMATTER).addTraits(ANTI, GASEOUS);
        fluid(VITRIOL, 0x6E5222, 2, 0, 1, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(SLOP, 0x929D45, 0, 0, 0, Symbol.NONE).addTraits(LIQUID, VISCOUS);
        fluid(LEAD, 0x666672, 4, 0, 0, Symbol.NONE).setTemp(350).addTraits(LIQUID, VISCOUS);
        fluid(LEAD_HOT, 0x776563, 4, 0, 0, Symbol.NONE).setTemp(1500).addTraits(LIQUID, VISCOUS);
        fluid(PERFLUOROMETHYL, 0xBDC8DC, 1, 0, 1, Symbol.NONE).setTemp(15).addTraits(LIQUID);
        fluid(PERFLUOROMETHYL_COLD, 0x99DADE, 1, 0, 1, Symbol.NONE).setTemp(-150).addTraits(LIQUID);
        fluid(PERFLUOROMETHYL_HOT, 0xB899DE, 1, 0, 1, Symbol.NONE).setTemp(250).addTraits(LIQUID);
        fluid(LYE, 0xFFECCC, 3, 0, 1, Symbol.ACID).addTraits(new FT_Corrosive(40), LIQUID);
        fluid(SODIUM_ALUMINATE, 0xFFD191, 3, 0, 1, Symbol.ACID).addTraits(new FT_Corrosive(30), LIQUID);
        fluid(BAUXITE_SOLUTION, 0xE2560F, 3, 0, 3, Symbol.ACID).addTraits(new FT_Corrosive(40), LIQUID, VISCOUS);
        fluid(ALUMINA, 0xDDFFFF, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(AIR, 0xE7EAEB, 0, 0, 0, Symbol.NONE).addTraits(GASEOUS);
        fluid(CONCRETE, 0xA2A2A2, 0, 0, 0, Symbol.NONE).addTraits(LIQUID);
        fluid(DHC, 0xD2AFFF, 0, 0, 0, Symbol.NONE).addTraits(GASEOUS);
        fluid(AIRBLAST, 0xFFDADA, 0, 3, 0, Symbol.NONE).setTemp(1_200).addTraits(GASEOUS);
        fluid(FLUE, 0x131313, 1, 4, 1, Symbol.NONE).addTraits(new FT_Flammable(25_000), GASEOUS, new FT_Polluting().burn(PollutionType.SOOT, SOOT_GAS).release(PollutionType.SOOT, SOOT_GAS * 25));

        // 1.7: the trait calls after metaOrder

        at(CHLORINE).addTraits(new FT_Toxin().addEntry(new ToxinDirectDamage(ModDamageSources.CLOUD, 2F, 20, HazardClass.GAS_LUNG, false)));
        at(PHOSGENE).addTraits(new FT_Toxin().addEntry(new ToxinDirectDamage(ModDamageSources.CLOUD, 4F, 20, HazardClass.GAS_LUNG, false)));
        at(MUSTARDGAS).addTraits(new FT_Toxin().addEntry(new ToxinDirectDamage(ModDamageSources.CLOUD, 4F, 10, HazardClass.GAS_BLISTERING, false))
                .addEntry(new ToxinEffects(HazardClass.GAS_BLISTERING, true).add(effect("minecraft:wither", 100, 1), effect("minecraft:nausea", 100, 0))));
        at(ESTRADIOL).addTraits(new FT_Toxin().addEntry(new ToxinEffects(HazardClass.PARTICLE_FINE, false).add(effect("hbm:death", 60 * 60 * 20, 0))));
        at(REDMUD).addTraits(new FT_Toxin().addEntry(new ToxinEffects(HazardClass.GAS_BLISTERING, false).add(effect("minecraft:wither", 30 * 20, 2))));

        at(AIR).addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).addStep(5, 1, AIRBLAST, 1));

        double eff_steam_boil = 1.0D;
        double eff_steam_heatex = 0.25D;

        at(WATER).addTraits(new FT_Heatable().setEff(HeatingType.BOILER, eff_steam_boil).setEff(HeatingType.HEATEXCHANGER, eff_steam_heatex)
                .addStep(200, 1, STEAM, 100)
                .addStep(220, 1, HOTSTEAM, 10)
                .addStep(238, 1, SUPERHOTSTEAM, 1)
                .addStep(2500, 10, ULTRAHOTSTEAM, 1));

        double eff_steam_turbine = 1.0D;
        double eff_steam_cool = 0.5D;
        at(STEAM).addTraits(new FT_Coolable(SPENTSTEAM, 100, 1, 200).setEff(CoolingType.TURBINE, eff_steam_turbine).setEff(CoolingType.HEATEXCHANGER, eff_steam_cool));
        at(HOTSTEAM).addTraits(new FT_Coolable(STEAM, 1, 10, 2).setEff(CoolingType.TURBINE, eff_steam_turbine).setEff(CoolingType.HEATEXCHANGER, eff_steam_cool));
        at(SUPERHOTSTEAM).addTraits(new FT_Coolable(HOTSTEAM, 1, 10, 18).setEff(CoolingType.TURBINE, eff_steam_turbine).setEff(CoolingType.HEATEXCHANGER, eff_steam_cool));
        at(ULTRAHOTSTEAM).addTraits(new FT_Coolable(SUPERHOTSTEAM, 1, 10, 120).setEff(CoolingType.TURBINE, eff_steam_turbine).setEff(CoolingType.HEATEXCHANGER, eff_steam_cool));

        at(OIL).addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTOIL, 1));
        at(OIL_DS).addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTOIL_DS, 1));
        at(CRACKOIL).addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTCRACKOIL, 1));
        at(CRACKOIL_DS).addTraits(new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).setEff(HeatingType.HEATEXCHANGER, 1.0D).addStep(10, 1, HOTCRACKOIL_DS, 1));

        at(HOTOIL).addTraits(new FT_Coolable(OIL, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        at(HOTOIL_DS).addTraits(new FT_Coolable(OIL_DS, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        at(HOTCRACKOIL).addTraits(new FT_Coolable(CRACKOIL, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        at(HOTCRACKOIL_DS).addTraits(new FT_Coolable(CRACKOIL_DS, 1, 1, 10).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        at(COOLANT).addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.0D).addStep(300, 1, COOLANT_HOT, 1));
        at(COOLANT_HOT).addTraits(new FT_Coolable(COOLANT, 1, 1, 300).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        at(PERFLUOROMETHYL_COLD).addTraits(new FT_Heatable().setEff(HeatingType.PA, 1.0D).addStep(300, 1, PERFLUOROMETHYL, 1));
        at(PERFLUOROMETHYL).addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.0D).addStep(300, 1, PERFLUOROMETHYL_HOT, 1));
        at(PERFLUOROMETHYL_HOT).addTraits(new FT_Coolable(PERFLUOROMETHYL, 1, 1, 300).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        at(MUG).addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.PWR, 1.0D).setEff(HeatingType.ICF, 1.25D).addStep(400, 1, MUG_HOT, 1), new FT_PWRModerator(1.15D));
        at(MUG_HOT).addTraits(new FT_Coolable(MUG, 1, 1, 400).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        at(BLOOD).addTraits(new FT_Heatable().setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.ICF, 1.25D).addStep(500, 1, BLOOD_HOT, 1));
        at(BLOOD_HOT).addTraits(new FT_Coolable(BLOOD, 1, 1, 500).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        at(HEAVYWATER).addTraits(new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(300, 1, HEAVYWATER_HOT, 1), new FT_PWRModerator(1.25D));
        at(HEAVYWATER_HOT).addTraits(new FT_Coolable(HEAVYWATER, 1, 1, 300).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        at(SODIUM).addTraits(new FT_Heatable().setEff(HeatingType.PWR, 2.5D).setEff(HeatingType.ICF, 3D).addStep(400, 1, SODIUM_HOT, 1));
        at(SODIUM_HOT).addTraits(new FT_Coolable(SODIUM, 1, 1, 400).setEff(CoolingType.HEATEXCHANGER, 1.0D));
        at(LEAD).addTraits(new FT_Heatable().setEff(HeatingType.PWR, 0.75D).setEff(HeatingType.ICF, 4D).addStep(800, 1, LEAD_HOT, 1), new FT_PWRModerator(0.75D));
        at(LEAD_HOT).addTraits(new FT_Coolable(LEAD, 1, 1, 680).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        at(THORIUM_SALT).addTraits(new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(400, 1, THORIUM_SALT_HOT, 1), new FT_PWRModerator(2.5D));
        at(THORIUM_SALT_HOT).addTraits(new FT_Coolable(THORIUM_SALT_DEPLETED, 1, 1, 400).setEff(CoolingType.HEATEXCHANGER, 1.0D));

        // 1.7 FINAL: the almighty excel spreadsheet has spoken

        long baseline = 100_000L;
        double demandVeryLow = 0.5D;
        double demandLow = 1.0D;
        double demandMedium = 1.5D;
        double demandHigh = 2.0D;
        double complexityRefinery = 1.1D;
        double complexityFraction = 1.05D;
        double complexityCracking = 1.25D;
        double complexityCoker = 1.25D;
        double complexityChemplant = 1.1D;
        double complexityLubed = 1.15D;
        double complexityLeaded = 1.5D;
        double complexityVacuum = 3.0D;
        double complexityReform = 2.5D;
        double complexityHydro = 2.0D;
        double flammabilityLow = 0.25D;
        double flammabilityNormal = 1.0D;
        double flammabilityHigh = 2.0D;

        registerCalculatedFuel(at(OIL), (baseline / 1D * flammabilityLow * demandLow), 0, null);
        registerCalculatedFuel(at(OIL_DS), (baseline / 1D * flammabilityLow * demandLow * complexityHydro), 0, null);
        registerCalculatedFuel(at(CRACKOIL), (baseline / 1D * flammabilityLow * demandLow * complexityCracking), 0, null);
        registerCalculatedFuel(at(CRACKOIL_DS), (baseline / 1D * flammabilityLow * demandLow * complexityCracking * complexityHydro), 0, null);
        registerCalculatedFuel(at(OIL_COKER), (baseline / 1D * flammabilityLow * demandLow * complexityCoker), 0, null);
        registerCalculatedFuel(at(GAS), (baseline / 1D * flammabilityNormal * demandVeryLow), 1.5, FuelGrade.GAS);
        registerCalculatedFuel(at(GAS_COKER), (baseline / 1D * flammabilityNormal * demandVeryLow * complexityCoker), 1.5, FuelGrade.GAS);
        registerCalculatedFuel(at(HEAVYOIL), (baseline / 0.5 * flammabilityLow * demandLow * complexityRefinery), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(at(SMEAR), (baseline / 0.35 * flammabilityLow * demandLow * complexityRefinery * complexityFraction), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(at(RECLAIMED), (baseline / 0.28 * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(at(PETROIL), (baseline / 0.28 * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant * complexityLubed), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(PETROIL_LEADED), (baseline / 0.28 * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant * complexityLubed * complexityLeaded), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(HEATINGOIL), (baseline / 0.31 * flammabilityNormal * demandLow * complexityRefinery * complexityFraction * complexityFraction), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(at(NAPHTHA), (baseline / 0.25 * flammabilityLow * demandLow * complexityRefinery), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(NAPHTHA_DS), (baseline / 0.25 * flammabilityLow * demandLow * complexityRefinery * complexityHydro), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(NAPHTHA_CRACK), (baseline / 0.40 * flammabilityLow * demandLow * complexityRefinery * complexityCracking), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(NAPHTHA_COKER), (baseline / 0.25 * flammabilityLow * demandLow * complexityCoker), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(GASOLINE), (baseline / 0.20 * flammabilityNormal * demandLow * complexityRefinery * complexityChemplant), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(at(GASOLINE_LEADED), (baseline / 0.20 * flammabilityNormal * demandLow * complexityRefinery * complexityChemplant * complexityLeaded), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(at(DIESEL), (baseline / 0.21 * flammabilityNormal * demandLow * complexityRefinery * complexityFraction), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(at(DIESEL_CRACK), (baseline / 0.28 * flammabilityNormal * demandLow * complexityRefinery * complexityCracking * complexityFraction), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(at(LIGHTOIL), (baseline / 0.15 * flammabilityNormal * demandHigh * complexityRefinery), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(LIGHTOIL_DS), (baseline / 0.15 * flammabilityNormal * demandHigh * complexityRefinery * complexityHydro), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(LIGHTOIL_CRACK), (baseline / 0.30 * flammabilityNormal * demandHigh * complexityRefinery * complexityCracking), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(KEROSENE), (baseline / 0.09 * flammabilityNormal * demandHigh * complexityRefinery * complexityFraction), 1.5D, FuelGrade.AERO);
        registerCalculatedFuel(at(PETROLEUM), (baseline / 0.10 * flammabilityNormal * demandMedium * complexityRefinery), 1.5, FuelGrade.GAS);
        registerCalculatedFuel(at(AROMATICS), (baseline / 0.15 * flammabilityLow * demandHigh * complexityRefinery * complexityCracking), 0, null);
        registerCalculatedFuel(at(UNSATURATEDS), (baseline / 0.15 * flammabilityHigh * demandHigh * complexityRefinery * complexityCracking), 0, null);
        registerCalculatedFuel(at(LPG), (baseline / 0.1 * flammabilityNormal * demandMedium * complexityRefinery * complexityChemplant), 2.5, FuelGrade.HIGH);
        registerCalculatedFuel(at(NITAN), at(KEROSENE).getTrait(FT_Flammable.class).getHeatEnergy() * 25L, 2.5, FuelGrade.HIGH);
        registerCalculatedFuel(at(BALEFIRE), at(KEROSENE).getTrait(FT_Flammable.class).getHeatEnergy() * 100L, 2.5, FuelGrade.HIGH);
        registerCalculatedFuel(at(HEAVYOIL_VACUUM), (baseline / 0.4 * flammabilityLow * demandLow * complexityVacuum), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(at(REFORMATE), (baseline / 0.25 * flammabilityNormal * demandHigh * complexityVacuum), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(at(LIGHTOIL_VACUUM), (baseline / 0.20 * flammabilityNormal * demandHigh * complexityVacuum), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(SOURGAS), (baseline / 0.15 * flammabilityLow * demandVeryLow * complexityVacuum), 0, null);
        registerCalculatedFuel(at(XYLENE), (baseline / 0.15 * flammabilityNormal * demandMedium * complexityVacuum * complexityFraction), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(at(HEATINGOIL_VACUUM), (baseline / 0.24 * flammabilityNormal * demandLow * complexityVacuum * complexityFraction), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(at(DIESEL_REFORM), at(DIESEL).getTrait(FT_Flammable.class).getHeatEnergy() * complexityReform, 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(at(DIESEL_CRACK_REFORM), at(DIESEL_CRACK).getTrait(FT_Flammable.class).getHeatEnergy() * complexityReform, 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(at(KEROSENE_REFORM), at(KEROSENE).getTrait(FT_Flammable.class).getHeatEnergy() * complexityReform, 1.5D, FuelGrade.AERO);
        registerCalculatedFuel(at(REFORMGAS), (baseline / 0.06 * flammabilityHigh * demandLow * complexityVacuum * complexityFraction), 1.5D, FuelGrade.GAS);

        int coalHeat = 400_000;
        registerCalculatedFuel(at(COALOIL), (coalHeat * (1000 / 100) * flammabilityLow * demandLow * complexityChemplant), 0, null); // bucket; mB per coal
        long coaloil = at(COALOIL).getTrait(FT_Flammable.class).getHeatEnergy();
        registerCalculatedFuel(at(COALGAS), (coaloil / 0.3 * flammabilityNormal * demandMedium * complexityChemplant * complexityFraction), 1.5, FuelGrade.MEDIUM);
        registerCalculatedFuel(at(COALGAS_LEADED), (coaloil / 0.3 * flammabilityNormal * demandMedium * complexityChemplant * complexityFraction * complexityLeaded), 1.5, FuelGrade.MEDIUM);

        registerCalculatedFuel(at(ETHANOL), 275_000D, 2.5D, FuelGrade.HIGH); // diesel / 2

        registerCalculatedFuel(at(BIOGAS), 250_000D * flammabilityLow, 1.25, FuelGrade.GAS); // biofuel with half compression, terrible flammability
        registerCalculatedFuel(at(BIOFUEL), 500_000D, 2.5D, FuelGrade.HIGH); // slightly below diesel

        registerCalculatedFuel(at(WOODOIL), 110_000, 0, null); // 20_000 TU per 250mB + a bonus
        registerCalculatedFuel(at(COALCREOSOTE), 250_000, 0, null); // 20_000 TU per 100mB + a bonus
        registerCalculatedFuel(at(FISHOIL), 75_000, 0, null);
        registerCalculatedFuel(at(SUNFLOWEROIL), 50_000, 0, null);

        registerCalculatedFuel(at(SOLVENT), 100_000, 0, null);
        registerCalculatedFuel(at(RADIOSOLVENT), 150_000, 0, null);

        registerCalculatedFuel(at(SYNGAS), (coalHeat * (1000 / 100) * flammabilityLow * demandLow * complexityChemplant) * 1.5, 1.25, FuelGrade.GAS); // bucket; mB per coal
        registerCalculatedFuel(at(OXYHYDROGEN), 5_000, 3, FuelGrade.GAS);
    }
}
