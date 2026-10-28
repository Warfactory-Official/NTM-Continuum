package com.hbm.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidHazard;
import com.hbm.inventory.fluid.FluidProperties;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import dev.golem.core.data.DataFiles;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fluid properties as data: HBM's fluids take colour, temperature, hazard diamond and traits from their
 * {@code fluid_property} files, and every registered fluid's properties (an addon's too) survive the file format. Another mod's
 * {@code fluid_property_patch} files change a fluid on top of its file and go away with a reload.
 */
public final class FluidPropertyGameTests {

    private FluidPropertyGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function FILES = TESTS.function("fluid_property_files", 20, () -> FluidPropertyGameTests::files);
    public static final GameTests.Function HAZARDS = TESTS.function("fluid_property_hazards", 20, () -> FluidPropertyGameTests::hazards);
    public static final GameTests.Function PATCHES = TESTS.function("fluid_property_patches", 20, () -> FluidPropertyGameTests::patches);
    public static final GameTests.Function PHASES = TESTS.function("fluid_property_phases", 20, () -> FluidPropertyGameTests::phases);

    public static void init() {}

    public static void files(GameTestHelper helper) {
        List<String> broken = new ArrayList<>();
        for (FluidType fluid : Fluids.getAll()) {
            if (fluid.isForeign()) continue;
            JsonObject before = FluidProperties.write(fluid);
            FluidProperties.apply(fluid, before);
            if (!FluidProperties.write(fluid).equals(before)) broken.add(fluid.getRegistryName());
        }
        helper.assertTrue(broken.isEmpty(), "fluids whose properties do not survive their file: " + broken);
        // one value read straight from data/minecraft/fluid_property/water.json
        FT_Heatable water = Fluids.WATER.getTrait(FT_Heatable.class);
        helper.assertTrue(water != null && water.steps().size() == 4 && water.getFirstStep().heatReq == 200
                        && water.getFirstStep().typeProduced == Fluids.STEAM && water.getFirstStep().amountProduced == 100,
                "water's boiling steps did not come from its property file");
        helper.assertTrue(Fluids.LAVA.getTemperature() == 1200 && Fluids.AIRBLAST.getTemperature() == 1200,
                "temperatures did not come from the property files");
        helper.succeed();
    }

    /**
     * 1.7's {@code new FluidType(name, color, p, f, r, EnumSymbol)} arguments for all 157 fluids, extracted mechanically
     * from {@code 1.7 .../inventory/fluid/Fluids.java} ({@code NAME:poison:flammability:reactivity:symbol},
     * 1.7's own symbol spelling, {@code CROYGENIC} included).
     */
    static final String[] HAZARDS_17 = {
            "NONE:0:0:0:NONE", "WATER:0:0:0:NONE", "STEAM:3:0:0:NONE", "HOTSTEAM:4:0:0:NONE",
            "SUPERHOTSTEAM:4:0:0:NONE", "ULTRAHOTSTEAM:4:0:0:NONE", "COOLANT:1:0:0:NONE", "LAVA:4:0:0:NOWATER",
            "DEUTERIUM:3:4:0:NONE", "TRITIUM:3:4:0:RADIATION", "OIL:2:1:0:NONE", "HOTOIL:2:3:0:NONE",
            "HEAVYOIL:2:1:0:NONE", "BITUMEN:2:0:0:NONE", "SMEAR:2:1:0:NONE", "HEATINGOIL:2:2:0:NONE",
            "RECLAIMED:2:2:0:NONE", "PETROIL:1:3:0:NONE", "LUBRICANT:2:1:0:NONE", "NAPHTHA:2:1:0:NONE",
            "DIESEL:1:2:0:NONE", "LIGHTOIL:1:2:0:NONE", "KEROSENE:1:2:0:NONE", "GAS:1:4:1:NONE",
            "PETROLEUM:1:4:1:NONE", "LPG:1:3:1:NONE", "BIOGAS:1:4:1:NONE", "BIOFUEL:1:2:0:NONE", "NITAN:2:4:1:NONE",
            "UF6:4:0:2:RADIATION", "PUF6:4:0:4:RADIATION", "SAS3:5:0:4:RADIATION", "SCHRABIDIC:5:0:5:ACID",
            "AMAT:5:0:5:ANTIMATTER", "ASCHRAB:5:0:5:ANTIMATTER", "PEROXIDE:3:0:3:OXIDIZER", "WATZ:4:0:3:ACID",
            "CRYOGEL:2:0:0:CROYGENIC", "HYDROGEN:3:4:0:CROYGENIC", "OXYGEN:3:0:0:CROYGENIC", "XENON:0:0:0:ASPHYXIANT",
            "BALEFIRE:4:4:3:RADIATION", "MERCURY:2:0:0:NONE", "PAIN:2:0:1:ACID", "WASTEFLUID:2:0:1:RADIATION",
            "WASTEGAS:2:0:1:RADIATION", "GASOLINE:1:2:0:NONE", "COALGAS:1:2:0:NONE", "SPENTSTEAM:2:0:0:NONE",
            "FRACKSOL:1:3:3:ACID", "PLASMA_DT:0:4:0:RADIATION", "PLASMA_HD:0:4:0:RADIATION",
            "PLASMA_HT:0:4:0:RADIATION", "PLASMA_XM:0:4:1:RADIATION", "PLASMA_BF:4:5:4:ANTIMATTER",
            "CARBONDIOXIDE:3:0:0:ASPHYXIANT", "PLASMA_DH3:0:4:0:RADIATION", "HELIUM3:0:0:0:ASPHYXIANT",
            "DEATH:2:0:1:ACID", "ETHANOL:2:3:0:NONE", "HEAVYWATER:1:0:0:NONE", "CRACKOIL:2:1:0:NONE",
            "COALOIL:2:1:0:NONE", "HOTCRACKOIL:2:3:0:NONE", "NAPHTHA_CRACK:2:1:0:NONE", "LIGHTOIL_CRACK:1:2:0:NONE",
            "DIESEL_CRACK:1:2:0:NONE", "AROMATICS:1:4:1:NONE", "UNSATURATEDS:1:4:1:NONE", "SALIENT:0:0:0:NONE",
            "XPJUICE:0:0:0:NONE", "ENDERJUICE:0:0:0:NONE", "PETROIL_LEADED:1:3:0:NONE", "GASOLINE_LEADED:1:2:0:NONE",
            "COALGAS_LEADED:1:2:0:NONE", "SULFURIC_ACID:3:0:2:ACID", "COOLANT_HOT:1:0:0:NONE", "MUG:0:0:0:NONE",
            "MUG_HOT:0:0:0:NONE", "WOODOIL:2:2:0:NONE", "COALCREOSOTE:3:2:0:NONE", "SEEDSLURRY:0:0:0:NONE",
            "NITRIC_ACID:3:0:2:OXIDIZER", "SOLVENT:2:3:0:NONE", "BLOOD:0:0:0:NONE", "BLOOD_HOT:3:0:0:NONE",
            "SYNGAS:1:4:2:NONE", "OXYHYDROGEN:0:4:2:NONE", "RADIOSOLVENT:3:3:0:NONE", "CHLORINE:3:0:0:OXIDIZER",
            "HEAVYOIL_VACUUM:2:1:0:NONE", "REFORMATE:2:2:0:NONE", "LIGHTOIL_VACUUM:1:2:0:NONE", "SOURGAS:4:4:0:ACID",
            "XYLENE:2:3:0:NONE", "HEATINGOIL_VACUUM:2:2:0:NONE", "DIESEL_REFORM:1:2:0:NONE",
            "DIESEL_CRACK_REFORM:1:2:0:NONE", "KEROSENE_REFORM:1:2:0:NONE", "REFORMGAS:1:4:1:NONE",
            "COLLOID:0:0:0:NONE", "PHOSGENE:4:0:1:NONE", "MUSTARDGAS:4:1:1:NONE", "IONGEL:1:0:4:NONE",
            "OIL_COKER:2:1:0:NONE", "NAPHTHA_COKER:2:1:0:NONE", "GAS_COKER:1:4:0:NONE", "EGG:0:0:0:NONE",
            "CHOLESTEROL:0:0:0:NONE", "ESTRADIOL:0:0:0:NONE", "FISHOIL:0:1:0:NONE", "SUNFLOWEROIL:0:1:0:NONE",
            "NITROGLYCERIN:0:4:0:NONE", "REDMUD:3:0:4:NONE", "CHLOROCALCITE_SOLUTION:0:0:0:NONE",
            "CHLOROCALCITE_MIX:0:0:0:NONE", "CHLOROCALCITE_CLEANED:0:0:0:NONE", "POTASSIUM_CHLORIDE:0:0:0:NONE",
            "CALCIUM_CHLORIDE:0:0:0:NONE", "CALCIUM_SOLUTION:0:0:0:NONE", "SMOKE:0:0:0:NONE",
            "SMOKE_LEADED:0:0:0:NONE", "SMOKE_POISON:0:0:0:NONE", "HELIUM4:0:0:0:ASPHYXIANT",
            "HEAVYWATER_HOT:1:0:0:NONE", "SODIUM:1:2:3:NONE", "SODIUM_HOT:1:2:3:NONE", "THORIUM_SALT:2:0:3:NONE",
            "THORIUM_SALT_HOT:2:0:3:NONE", "THORIUM_SALT_DEPLETED:2:0:3:NONE", "FULLERENE:3:3:3:NONE",
            "PHEROMONE:0:0:0:NONE", "PHEROMONE_M:0:0:0:NONE", "OIL_DS:2:1:0:NONE", "HOTOIL_DS:2:3:0:NONE",
            "CRACKOIL_DS:2:1:0:NONE", "HOTCRACKOIL_DS:2:3:0:NONE", "NAPHTHA_DS:2:1:0:NONE", "LIGHTOIL_DS:1:2:0:NONE",
            "STELLAR_FLUX:0:4:4:ANTIMATTER", "VITRIOL:2:0:1:NONE", "SLOP:0:0:0:NONE", "LEAD:4:0:0:NONE",
            "LEAD_HOT:4:0:0:NONE", "PERFLUOROMETHYL:1:0:1:NONE", "PERFLUOROMETHYL_COLD:1:0:1:NONE",
            "PERFLUOROMETHYL_HOT:1:0:1:NONE", "LYE:3:0:1:ACID", "SODIUM_ALUMINATE:3:0:1:ACID",
            "BAUXITE_SOLUTION:3:0:3:ACID", "ALUMINA:0:0:0:NONE", "AIR:0:0:0:NONE", "CONCRETE:0:0:0:NONE",
            "DHC:0:0:0:NONE", "AIRBLAST:0:3:0:NONE", "FLUE:1:4:1:NONE",
            "TITANIUM_TETRACHLORIDE:3:0:2:NOWATER"
    };

    /** A {@code NAME:p:f:r:SYMBOL} row as a hazard (1.7 spells the cryogenic symbol {@code CROYGENIC}). */
    public static FluidHazard hazard17(String[] row) {
        return new FluidHazard(Integer.parseInt(row[1]), Integer.parseInt(row[2]), Integer.parseInt(row[3]),
                FluidHazard.Symbol.valueOf(row[4].equals("CROYGENIC") ? "CRYOGENIC" : row[4]));
    }

    /** Every HBM fluid's hazard diamond is 1.7's (from the {@code hazard} field of its property file). */
    public static void hazards(GameTestHelper helper) {
        Map<String, FluidType> byName = new LinkedHashMap<>();
        for (FluidType fluid : FluidRecipeTableGameTests.registered()) {
            if (fluid.getNamespace().equals(Constants.MOD_ID)) byName.put(fluid.getName(), fluid);
        }
        List<String> wrong = new ArrayList<>();
        for (String text : HAZARDS_17) {
            String[] row = text.split(":");
            FluidType fluid = byName.remove(row[0]);
            if (fluid == null) {
                wrong.add(row[0] + " is not registered");
                continue;
            }
            FluidHazard expected = hazard17(row);
            if (!fluid.getHazard().equals(expected)) wrong.add(row[0] + " " + fluid.getHazard() + ", expected " + expected);
        }
        helper.assertTrue(HAZARDS_17.length == 157, "the 1.7 table has " + HAZARDS_17.length + " rows");
        helper.assertTrue(wrong.isEmpty(), "hazard diamonds differ from 1.7: " + wrong);
        helper.assertTrue(byName.isEmpty(), "HBM fluids missing from the 1.7 table: " + byName.keySet());
        helper.succeed();
    }

    /**
     * A {@code fluid_property_patch} adds and removes traits on an HBM fluid without replacing its file, two mods'
     * patches apply in namespace order, a reload of the property files keeps them on top, and a reload without the
     * patches puts the fluid back exactly.
     */
    public static void patches(GameTestHelper helper) {
        FluidType acid = Fluids.NITRIC_ACID;
        Map<String, JsonElement> loaded = FluidProperties.patchFiles();
        // as loaded, an installed addon's own patches included (NTM: Space takes nitric acid's pollution away)
        JsonObject before = FluidProperties.write(acid);
        FluidHazard hazard = acid.getHazard();
        // the test runs on HBM's own nitric acid, whatever addons are installed
        Map<String, JsonElement> others = new LinkedHashMap<>(loaded);
        others.keySet().removeIf(id -> id.endsWith(":hbm/nitric_acid"));
        Map<String, JsonElement> withTest = new LinkedHashMap<>(others);
        withTest.put("hbmtest:hbm/nitric_acid", JsonParser.parseString("""
                {"remove": ["polluting", "liquid"],
                 "add": {"type": "gaseous", "flags": ["viscous"], "traits": {"pwrmoderator": {"multiplier": 1.5}}},
                 "temperature": 50}"""));
        // a second mod's patch to the same fluid, applied after the first (patch ids sort by the patching namespace)
        withTest.put("hbmtest2:hbm/nitric_acid", JsonParser.parseString("""
                {"remove": ["viscous"]}"""));
        // a patch for the fluid of a mod that is not installed is ignored
        withTest.put("hbmtest:nosuchmod/fluid", JsonParser.parseString("""
                {"temperature": 1}"""));
        try {
            FluidProperties.applyPatches(others);
            helper.assertTrue(acid.hasTrait(FT_Polluting.class) && !acid.hasTrait(FT_PWRModerator.class), "nitric acid is not 1.7's");
            FluidProperties.applyPatches(withTest);
            check(helper, acid, hazard, "");
            // a reload of the property files (which replaces nitric acid's properties) keeps the patches on top
            FluidProperties.apply(DataFiles.read(FluidProperties.FOLDER,
                    helper.getLevel().getServer().getResourceManager()));
            check(helper, acid, hazard, " after the property files were reloaded");
        } finally {
            FluidProperties.applyPatches(loaded);
        }
        helper.assertTrue(FluidProperties.write(acid).equals(before), "a reload without the patch did not restore nitric acid: "
                + FluidProperties.write(acid) + " vs " + before);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, FluidType acid, FluidHazard hazard, String when) {
        FT_PWRModerator moderator = acid.getTrait(FT_PWRModerator.class);
        helper.assertTrue(!acid.hasTrait(FT_Polluting.class), "the patch did not remove the pollution" + when);
        helper.assertTrue(moderator != null && moderator.getMultiplier() == 1.5, "the patch did not add the moderator" + when);
        helper.assertTrue(acid.hasTrait(FluidTraitSimple.FT_Gaseous.class) && !acid.hasTrait(FluidTraitSimple.FT_Liquid.class),
                "the patch did not change the phase" + when);
        helper.assertTrue(!acid.hasTrait(FluidTraitSimple.FT_Viscous.class), "the second patch was not applied after the first" + when);
        helper.assertTrue(acid.getTemperature() == 50, "the patch did not override the temperature" + when);
        helper.assertTrue(acid.isCorrosive() && acid.getHazard().equals(hazard), "the patch touched what it did not name" + when);
    }

    /** {@code "type"} may list several phases (1.7 let a fluid carry several), and the file writer keeps them. */
    public static void phases(GameTestHelper helper) {
        FluidType fluid = Fluids.XYLENE;
        JsonObject before = FluidProperties.write(fluid);
        try {
            JsonObject both = before.deepCopy();
            JsonArray phases = new JsonArray();
            phases.add("liquid");
            phases.add("gaseous");
            both.add("type", phases);
            FluidProperties.apply(fluid, both);
            helper.assertTrue(fluid.hasTrait(FluidTraitSimple.FT_Liquid.class) && fluid.hasTrait(FluidTraitSimple.FT_Gaseous.class),
                    "a two-phase file did not give both phases");
            JsonObject written = FluidProperties.write(fluid);
            helper.assertTrue(written.get("type").isJsonArray() && written.getAsJsonArray("type").size() == 2,
                    "the writer did not keep both phases: " + written.get("type"));
            FluidProperties.apply(fluid, written);
            helper.assertTrue(FluidProperties.write(fluid).equals(written), "a two-phase file does not survive a round trip");
        } finally {
            FluidProperties.apply(fluid, before);
        }
        helper.assertTrue(FluidProperties.write(fluid).equals(before), "xylene was not restored");
        helper.succeed();
    }
}
