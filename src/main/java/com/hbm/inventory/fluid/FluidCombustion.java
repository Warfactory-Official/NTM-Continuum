package com.hbm.inventory.fluid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The fluid combustion data stripped from the port's slim {@link FluidType} — stand-in for the 1.12.2
 * traits {@code FT_Flammable} (heat energy per bucket; oilburner / gas flare / solidifier SF math),
 * {@code FT_Combustible} (fuel grade + combustion energy per bucket; diesel generator / combustion
 * engine), and {@code FT_Gaseous} (gas flare venting + its burn penalty; this fork has no GASEOUS_ART).
 *
 * <p>Values replicate old {@code Fluids.init()} <b>exactly</b>: the literal trait registrations first,
 * then the "almighty excel spreadsheet" {@code registerCalculatedFuel} block (old L701-790) which
 * overwrites most of them — including its magnitude-based {@code round()}. Do not edit the numbers:
 * change the constants/formulas only if the old source changes.
 */
public final class FluidCombustion {

    private FluidCombustion() {}

    /** Old {@code FT_Combustible.FuelGrade}. */
    public enum FuelGrade { LOW, MEDIUM, HIGH, AERO, GAS }

    public record Combustible(FuelGrade grade, long energyPerBucket) {}

    private static final Map<FluidType, Long> FLAMMABLE = new HashMap<>();
    private static final Map<FluidType, Combustible> COMBUSTIBLE = new HashMap<>();
    private static final Set<FluidType> GASEOUS = new HashSet<>();
    private static final Set<String> LIQUID_VISCOUS = Set.of(
            "OIL", "HEAVYOIL", "SMEAR", "HEATINGOIL", "NAPHTHA", "NITAN", "BALEFIRE",
            "CRACKOIL", "COALOIL", "NAPHTHA_CRACK", "AROMATICS", "HEAVYOIL_VACUUM", "REFORMATE",
            "XYLENE", "HEATINGOIL_VACUUM", "OIL_DS", "CRACKOIL_DS", "OIL_COKER", "NAPHTHA_COKER",
            "REDMUD");
    private static boolean initialized = false;

    /** TU per bucket when burned for heat (old FT_Flammable.getHeatEnergy), 0 when not flammable. */
    public static long getFlammableEnergy(FluidType type) {
        init();
        Long e = FLAMMABLE.get(type);
        return e == null ? 0L : e;
    }

    public static boolean isFlammable(FluidType type) {
        init();
        return FLAMMABLE.containsKey(type);
    }

    /** Fuel grade + combustion energy per bucket (old FT_Combustible), null when not combustible. */
    public static Combustible getCombustible(FluidType type) {
        init();
        return COMBUSTIBLE.get(type);
    }

    /** Old FT_Gaseous (flare venting; gaseous fuels burn with penalty 5 instead of 10). */
    public static boolean isGaseous(FluidType type) {
        init();
        return GASEOUS.contains(type);
    }

    /** The old drain's liquid + viscous gate; the two traits are represented by this exact legacy set. */
    public static boolean isLiquidAndViscous(FluidType type) {
        return type != null && LIQUID_VISCOUS.contains(type.getName());
    }

    public static void setFlammable(FluidType type, long energy) {
        init();
        flam(type, energy);
    }

    public static void setCombustible(FluidType type, FuelGrade grade, long energy) {
        init();
        comb(type, grade, energy);
    }

    public static void setGaseous(FluidType type) {
        init();
        GASEOUS.add(type);
    }

    private static void flam(FluidType type, long energy) {
        FLAMMABLE.put(type, energy);
    }

    private static void comb(FluidType type, FuelGrade grade, long energy) {
        COMBUSTIBLE.put(type, new Combustible(grade, energy));
    }

    /** Old {@code Fluids.registerCalculatedFuel}: rounds and overwrites both maps. */
    private static void registerCalculatedFuel(FluidType type, double base, double combustMult, FuelGrade grade) {
        long flammable = round((long) base);
        long combustible = round((long) (base * combustMult));
        flam(type, flammable);
        if (combustible > 0 && grade != null) comb(type, grade, combustible);
    }

    /** Old {@code Fluids.round} — magnitude-stepped truncation, "ugly but it does the thing". */
    private static long round(long l) {
        if (l > 10_000_000L) return l - (l % 100_000L);
        if (l > 1_000_000L) return l - (l % 10_000L);
        if (l > 100_000L) return l - (l % 1_000L);
        if (l > 10_000L) return l - (l % 100L);
        if (l > 1_000L) return l - (l % 10L);
        return l;
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        // --- literal trait registrations (old Fluids.init fluid declarations) ---
        flam(Fluids.DEUTERIUM, 5_000);        comb(Fluids.DEUTERIUM, FuelGrade.HIGH, 10_000);
        flam(Fluids.TRITIUM, 5_000);          comb(Fluids.TRITIUM, FuelGrade.HIGH, 10_000);
        flam(Fluids.OIL, 10_000);
        flam(Fluids.HEAVYOIL, 50_000);        comb(Fluids.HEAVYOIL, FuelGrade.LOW, 25_000);
        flam(Fluids.SMEAR, 50_000);
        flam(Fluids.HEATINGOIL, 150_000);     comb(Fluids.HEATINGOIL, FuelGrade.LOW, 100_000);
        flam(Fluids.RECLAIMED, 100_000);      comb(Fluids.RECLAIMED, FuelGrade.LOW, 200_000);
        flam(Fluids.PETROIL, 125_000);        comb(Fluids.PETROIL, FuelGrade.MEDIUM, 300_000);
        flam(Fluids.NAPHTHA, 125_000);        comb(Fluids.NAPHTHA, FuelGrade.MEDIUM, 200_000);
        flam(Fluids.DIESEL, 200_000);         comb(Fluids.DIESEL, FuelGrade.HIGH, 500_000);
        flam(Fluids.LIGHTOIL, 200_000);       comb(Fluids.LIGHTOIL, FuelGrade.MEDIUM, 500_000);
        flam(Fluids.KEROSENE, 300_000);       comb(Fluids.KEROSENE, FuelGrade.AERO, 1_250_000);
        flam(Fluids.GAS, 10_000);
        flam(Fluids.PETROLEUM, 25_000);
        flam(Fluids.LPG, 200_000);            comb(Fluids.LPG, FuelGrade.HIGH, 400_000);
        flam(Fluids.BIOGAS, 25_000);
        flam(Fluids.BIOFUEL, 150_000);        comb(Fluids.BIOFUEL, FuelGrade.HIGH, 400_000);
        flam(Fluids.NITAN, 2_000_000);        comb(Fluids.NITAN, FuelGrade.HIGH, 5_000_000);
        flam(Fluids.HYDROGEN, 5_000);         comb(Fluids.HYDROGEN, FuelGrade.HIGH, 10_000);
        flam(Fluids.BALEFIRE, 1_000_000);     comb(Fluids.BALEFIRE, FuelGrade.HIGH, 2_500_000);
        flam(Fluids.GASOLINE, 400_000);       comb(Fluids.GASOLINE, FuelGrade.HIGH, 1_000_000);
        flam(Fluids.COALGAS, 75_000);         comb(Fluids.COALGAS, FuelGrade.MEDIUM, 150_000);
        flam(Fluids.ETHANOL, 75_000);         comb(Fluids.ETHANOL, FuelGrade.HIGH, 200_000);
        flam(Fluids.CRACKOIL, 10_000);
        flam(Fluids.COALOIL, 10_000);
        flam(Fluids.NAPHTHA_CRACK, 125_000);  comb(Fluids.NAPHTHA_CRACK, FuelGrade.MEDIUM, 200_000);
        flam(Fluids.LIGHTOIL_CRACK, 200_000); comb(Fluids.LIGHTOIL_CRACK, FuelGrade.MEDIUM, 500_000);
        flam(Fluids.DIESEL_CRACK, 200_000);   comb(Fluids.DIESEL_CRACK, FuelGrade.HIGH, 450_000);
        flam(Fluids.AROMATICS, 25_000);
        flam(Fluids.UNSATURATEDS, 1_000_000);
        flam(Fluids.PETROIL_LEADED, 125_000); comb(Fluids.PETROIL_LEADED, FuelGrade.MEDIUM, 450_000);
        flam(Fluids.GASOLINE_LEADED, 400_000); comb(Fluids.GASOLINE_LEADED, FuelGrade.HIGH, 1_500_000);
        flam(Fluids.COALGAS_LEADED, 75_000);  comb(Fluids.COALGAS_LEADED, FuelGrade.MEDIUM, 250_000);
        flam(Fluids.HYDRAZINE, 500_000);      comb(Fluids.HYDRAZINE, FuelGrade.HIGH, 1_250_000);
        flam(Fluids.REDMUD, 1_000);
        flam(Fluids.FLUE, 10_000);

        // --- "the almighty excel spreadsheet has spoken!" (old Fluids.init L701-790) ---
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

        registerCalculatedFuel(Fluids.OIL, (baseline / 1D * flammabilityLow * demandLow), 0, null);
        registerCalculatedFuel(Fluids.OIL_DS, (baseline / 1D * flammabilityLow * demandLow * complexityHydro), 0, null);
        registerCalculatedFuel(Fluids.CRACKOIL, (baseline / 1D * flammabilityLow * demandLow * complexityCracking), 0, null);
        registerCalculatedFuel(Fluids.CRACKOIL_DS, (baseline / 1D * flammabilityLow * demandLow * complexityCracking * complexityHydro), 0, null);
        registerCalculatedFuel(Fluids.OIL_COKER, (baseline / 1D * flammabilityLow * demandLow * complexityCoker), 0, null);
        registerCalculatedFuel(Fluids.GAS, (baseline / 1D * flammabilityNormal * demandVeryLow), 1.5, FuelGrade.GAS);
        registerCalculatedFuel(Fluids.GAS_COKER, (baseline / 1D * flammabilityNormal * demandVeryLow * complexityCoker), 1.5, FuelGrade.GAS);
        registerCalculatedFuel(Fluids.HEAVYOIL, (baseline / 0.5 * flammabilityLow * demandLow * complexityRefinery), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(Fluids.SMEAR, (baseline / 0.35 * flammabilityLow * demandLow * complexityRefinery * complexityFraction), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(Fluids.RECLAIMED, (baseline / 0.28 * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(Fluids.PETROIL, (baseline / 0.28 * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant * complexityLubed), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.PETROIL_LEADED, (baseline / 0.28 * flammabilityLow * demandLow * complexityRefinery * complexityFraction * complexityChemplant * complexityLubed * complexityLeaded), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.HEATINGOIL, (baseline / 0.31 * flammabilityNormal * demandLow * complexityRefinery * complexityFraction * complexityFraction), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(Fluids.NAPHTHA, (baseline / 0.25 * flammabilityLow * demandLow * complexityRefinery), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.NAPHTHA_DS, (baseline / 0.25 * flammabilityLow * demandLow * complexityRefinery * complexityHydro), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.NAPHTHA_CRACK, (baseline / 0.40 * flammabilityLow * demandLow * complexityRefinery * complexityCracking), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.NAPHTHA_COKER, (baseline / 0.25 * flammabilityLow * demandLow * complexityCoker), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.GASOLINE, (baseline / 0.20 * flammabilityNormal * demandLow * complexityRefinery * complexityChemplant), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.GASOLINE_LEADED, (baseline / 0.20 * flammabilityNormal * demandLow * complexityRefinery * complexityChemplant * complexityLeaded), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.DIESEL, (baseline / 0.21 * flammabilityNormal * demandLow * complexityRefinery * complexityFraction), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.DIESEL_CRACK, (baseline / 0.28 * flammabilityNormal * demandLow * complexityRefinery * complexityCracking * complexityFraction), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.LIGHTOIL, (baseline / 0.15 * flammabilityNormal * demandHigh * complexityRefinery), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.LIGHTOIL_DS, (baseline / 0.15 * flammabilityNormal * demandHigh * complexityRefinery * complexityHydro), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.LIGHTOIL_CRACK, (baseline / 0.30 * flammabilityNormal * demandHigh * complexityRefinery * complexityCracking), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.KEROSENE, (baseline / 0.09 * flammabilityNormal * demandHigh * complexityRefinery * complexityFraction), 1.5D, FuelGrade.AERO);
        registerCalculatedFuel(Fluids.PETROLEUM, (baseline / 0.10 * flammabilityNormal * demandMedium * complexityRefinery), 1.5, FuelGrade.GAS);
        registerCalculatedFuel(Fluids.AROMATICS, (baseline / 0.15 * flammabilityLow * demandHigh * complexityRefinery * complexityCracking), 0, null);
        registerCalculatedFuel(Fluids.UNSATURATEDS, (baseline / 0.15 * flammabilityHigh * demandHigh * complexityRefinery * complexityCracking), 0, null);
        registerCalculatedFuel(Fluids.LPG, (baseline / 0.1 * flammabilityNormal * demandMedium * complexityRefinery * complexityChemplant), 2.5, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.NITAN, getFlam(Fluids.KEROSENE) * 25L, 2.5, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.BALEFIRE, getFlam(Fluids.KEROSENE) * 100L, 2.5, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.HEAVYOIL_VACUUM, (baseline / 0.4 * flammabilityLow * demandLow * complexityVacuum), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(Fluids.REFORMATE, (baseline / 0.25 * flammabilityNormal * demandHigh * complexityVacuum), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.LIGHTOIL_VACUUM, (baseline / 0.20 * flammabilityNormal * demandHigh * complexityVacuum), 1.5D, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.SOURGAS, (baseline / 0.15 * flammabilityLow * demandVeryLow * complexityVacuum), 0, null);
        registerCalculatedFuel(Fluids.XYLENE, (baseline / 0.15 * flammabilityNormal * demandMedium * complexityVacuum * complexityFraction), 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.HEATINGOIL_VACUUM, (baseline / 0.24 * flammabilityNormal * demandLow * complexityVacuum * complexityFraction), 1.25D, FuelGrade.LOW);
        registerCalculatedFuel(Fluids.DIESEL_REFORM, getFlam(Fluids.DIESEL) * complexityReform, 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.DIESEL_CRACK_REFORM, getFlam(Fluids.DIESEL_CRACK) * complexityReform, 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.KEROSENE_REFORM, getFlam(Fluids.KEROSENE) * complexityReform, 1.5D, FuelGrade.AERO);
        registerCalculatedFuel(Fluids.REFORMGAS, (baseline / 0.06 * flammabilityHigh * demandLow * complexityVacuum * complexityFraction), 1.5D, FuelGrade.GAS);

        int coalHeat = 400_000; // 200TU/t for 2000 ticks
        registerCalculatedFuel(Fluids.COALOIL, (coalHeat * (1000 / 100) * flammabilityLow * demandLow * complexityChemplant), 0, null);
        long coaloil = getFlam(Fluids.COALOIL);
        registerCalculatedFuel(Fluids.COALGAS, (coaloil / 0.3 * flammabilityNormal * demandMedium * complexityChemplant * complexityFraction), 1.5, FuelGrade.MEDIUM);
        registerCalculatedFuel(Fluids.COALGAS_LEADED, (coaloil / 0.3 * flammabilityNormal * demandMedium * complexityChemplant * complexityFraction * complexityLeaded), 1.5, FuelGrade.MEDIUM);

        registerCalculatedFuel(Fluids.ETHANOL, 275_000D, 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.BIOGAS, 250_000D * flammabilityLow, 1.25, FuelGrade.GAS);
        registerCalculatedFuel(Fluids.BIOFUEL, 500_000D, 2.5D, FuelGrade.HIGH);
        registerCalculatedFuel(Fluids.WOODOIL, 110_000, 0, null);
        registerCalculatedFuel(Fluids.COALCREOSOTE, 250_000, 0, null);
        registerCalculatedFuel(Fluids.FISHOIL, 75_000, 0, null);
        registerCalculatedFuel(Fluids.SUNFLOWEROIL, 50_000, 0, null);
        registerCalculatedFuel(Fluids.SOLVENT, 100_000, 0, null);
        registerCalculatedFuel(Fluids.RADIOSOLVENT, 150_000, 0, null);
        registerCalculatedFuel(Fluids.SYNGAS, (coalHeat * (1000 / 100) * flammabilityLow * demandLow * complexityChemplant) * 1.5, 1.25, FuelGrade.GAS);
        registerCalculatedFuel(Fluids.OXYHYDROGEN, 5_000, 3, FuelGrade.GAS);

        // BITUMEN / LUBRICANT / HOTOIL etc. never carry FT_Flammable in this fork — the oilburner
        // can't burn them; their flame TU lives in FluidCombustionRecipes (boiler flame table) instead.

        // --- FT_Gaseous (old fluid declarations; this fork has no GASEOUS_ART) ---
        java.util.Collections.addAll(GASEOUS,
                Fluids.STEAM, Fluids.HOTSTEAM, Fluids.SUPERHOTSTEAM, Fluids.ULTRAHOTSTEAM,
                Fluids.DEUTERIUM, Fluids.TRITIUM, Fluids.GAS, Fluids.PETROLEUM, Fluids.BIOGAS,
                Fluids.UF6, Fluids.PUF6, Fluids.AMAT, Fluids.ASCHRAB, Fluids.XENON, Fluids.WASTEGAS,
                Fluids.SPENTSTEAM, Fluids.CARBONDIOXIDE, Fluids.HELIUM3, Fluids.UNSATURATEDS,
                Fluids.SYNGAS, Fluids.OXYHYDROGEN, Fluids.CHLORINE, Fluids.SOURGAS, Fluids.REFORMGAS,
                Fluids.PHOSGENE, Fluids.MUSTARDGAS, Fluids.GAS_COKER, Fluids.SMOKE, Fluids.SMOKE_LEADED,
                Fluids.SMOKE_POISON, Fluids.HELIUM4, Fluids.STELLAR_FLUX, Fluids.AIR, Fluids.DHC,
                Fluids.AIRBLAST, Fluids.FLUE);
    }

    private static long getFlam(FluidType type) {
        Long e = FLAMMABLE.get(type);
        return e == null ? 0L : e;
    }
}
