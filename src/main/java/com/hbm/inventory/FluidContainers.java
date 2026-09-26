package com.hbm.inventory;

import com.hbm.api.fluid.FluidType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The 1.7 per-fluid container data that the port's {@link FluidType} does not carry:
 * {@code CD_Canister(color)} and {@code CD_Gastank(bottleColor, labelColor)} from {@code Fluids},
 * keyed by fluid name, and the {@code FT_LeadContainer}/{@code FT_NoContainer} trait sets. Read by the
 * container items and the creative-tab variant lists.
 */
public final class FluidContainers {

    private FluidContainers() {}

    /** {@code CD_Canister} colours, in {@code Fluids} declaration order. */
    public static final Map<String, Integer> CANISTER = new LinkedHashMap<>();
    /** {@code CD_Gastank} bottle colours. */
    public static final Map<String, Integer> GAS_BOTTLE = new LinkedHashMap<>();
    /** {@code CD_Gastank} label colours. */
    public static final Map<String, Integer> GAS_LABEL = new LinkedHashMap<>();

    /** Fluids with {@code FT_LeadContainer}: only the lead tank may hold them. */
    public static final Set<String> LEAD_ONLY = Set.of("DEATH", "REDMUD");

    /** Fluids with {@code FT_NoContainer}: no portable container at all. */
    public static final Set<String> NO_CONTAINER = Set.of(
            "WASTEFLUID", "WASTEGAS", "SPENTSTEAM", "PLASMA_DT", "PLASMA_HD", "PLASMA_HT", "PLASMA_XM",
            "PLASMA_BF", "PLASMA_DH3", "CHLOROCALCITE_SOLUTION", "CHLOROCALCITE_MIX", "CHLOROCALCITE_CLEANED",
            "POTASSIUM_CHLORIDE", "CALCIUM_CHLORIDE", "CALCIUM_SOLUTION", "SMOKE", "SMOKE_LEADED", "SMOKE_POISON");

    private static void canister(String fluid, int color) { CANISTER.put(fluid, color); }

    private static void gas(String fluid, int bottle, int label) {
        GAS_BOTTLE.put(fluid, bottle);
        GAS_LABEL.put(fluid, label);
    }

    static {
        gas("DEUTERIUM", 0x0000FF, 0xFFFFFF);
        gas("TRITIUM", 0x000099, 0xE9FFAA);
        canister("OIL", 0x424242);
        canister("HEAVYOIL", 0x513F39);
        canister("BITUMEN", 0x5A5877);
        canister("SMEAR", 0x624F3B);
        canister("HEATINGOIL", 0x694235);
        canister("RECLAIMED", 0xF65723);
        canister("PETROIL", 0x2369F6);
        canister("LUBRICANT", 0xF1CC05);
        canister("NAPHTHA", 0x5F6D44);
        canister("DIESEL", 0xFF2C2C);
        canister("LIGHTOIL", 0xB46B52);
        canister("KEROSENE", 0xFF377D);
        gas("GAS", 0xFF4545, 0xFFE97F);
        gas("PETROLEUM", 0x5E7CFF, 0xFFE97F);
        gas("BIOGAS", 0xC8FF1F, 0x303030);
        canister("BIOFUEL", 0x9EB623);
        canister("NITAN", 0x6B238C);
        gas("HYDROGEN", 0x4286f4, 0xffffff);
        gas("OXYGEN", 0x98bdf9, 0xffffff);
        gas("XENON", 0x8C21FF, 0x303030);
        canister("GASOLINE", 0x2F7747);
        canister("COALGAS", 0x2E155F);
        canister("FRACKSOL", 0x4F887F);
        gas("HELIUM3", 0xFD631F, 0xffffff);
        canister("ETHANOL", 0xEAFFF3);
        canister("CRACKOIL", 0x424242);
        canister("COALOIL", 0x424242);
        canister("NAPHTHA_CRACK", 0x5F6D44);
        canister("LIGHTOIL_CRACK", 0xB46B52);
        canister("DIESEL_CRACK", 0xFF2C2C);
        gas("AROMATICS", 0x68A09A, 0xEDCF27);
        gas("UNSATURATEDS", 0x628FAE, 0xEDCF27);
        canister("PETROIL_LEADED", 0x2331F6);
        canister("GASOLINE_LEADED", 0x2F775A);
        canister("COALGAS_LEADED", 0x1E155F);
        canister("WOODOIL", 0xBF7E4F);
        canister("COALCREOSOTE", 0x285A3F);
        canister("SEEDSLURRY", 0x7CC35E);
        canister("SOLVENT", 0xE4E3EF);
        gas("SYNGAS", 0xFFFFFF, 0x131313);
        gas("CHLORINE", 0xBAB572, 0x887B34);
        canister("HEAVYOIL_VACUUM", 0x513F39);
        canister("REFORMATE", 0xD180D6);
        canister("LIGHTOIL_VACUUM", 0xB46B52);
        gas("SOURGAS", 0xC9BE0D, 0x303030);
        canister("XYLENE", 0xA380D6);
        canister("HEATINGOIL_VACUUM", 0x694235);
        canister("DIESEL_REFORM", 0xFFC500);
        canister("DIESEL_CRACK_REFORM", 0xFFC500);
        canister("KEROSENE_REFORM", 0xFF377D);
        gas("REFORMGAS", 0x9392FF, 0xFFB992);
        gas("PHOSGENE", 0xCFC4A4, 0x361414);
        gas("MUSTARDGAS", 0xBAB572, 0x361414);
        gas("HELIUM4", 0xFD631F, 0xffff00);
        canister("OIL_DS", 0x424242);
        canister("CRACKOIL_DS", 0x424242);
        canister("NAPHTHA_DS", 0x5F6D44);
        canister("LIGHTOIL_DS", 0xB46B52);
        gas("FLUE", 0xFF4545, 0xFFE97F);
    }

    public static boolean hasCanister(FluidType type) { return CANISTER.containsKey(type.getName()); }
    public static boolean hasGasTank(FluidType type) { return GAS_BOTTLE.containsKey(type.getName()); }
    public static boolean needsLeadContainer(FluidType type) { return LEAD_ONLY.contains(type.getName()); }
    public static boolean hasNoContainer(FluidType type) { return NO_CONTAINER.contains(type.getName()); }

    public static int canisterColor(FluidType type) { return CANISTER.getOrDefault(type.getName(), 0xFFFFFF); }
    public static int gasBottleColor(FluidType type) { return GAS_BOTTLE.getOrDefault(type.getName(), 0xFFFFFF); }
}
