package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModFuelItems;
import dev.golem.core.gametest.GameTests;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The GenericRecipe machine tables as datapack tables: each loads from {@code data/hbm/recipe/<machine>}
 * and is checked against the Java tables' values (row counts per config mode, sample rows field for field, the
 * indexes a table derives); the config modes swap the expensive inputs, 528 pools and conditional rows on a
 * rebuild; and every row survives its file form and the client sync form.
 */
public final class GenericRecipeTableGameTests {

    private GenericRecipeTableGameTests() {
    }

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function COUNTS =
            TESTS.function("recipe_generic_counts", 40, () -> GenericRecipeTableGameTests::counts);
    public static final GameTests.Function SAMPLES =
            TESTS.function("recipe_generic_samples", 40, () -> GenericRecipeTableGameTests::samples);
    public static final GameTests.Function ROUND_TRIP =
            TESTS.function("recipe_generic_round_trip", 100, () -> GenericRecipeTableGameTests::roundTrip);
    public static final GameTests.Function MODES =
            TESTS.function("recipe_generic_modes", 40, () -> GenericRecipeTableGameTests::modes);
    public static final GameTests.Function DERIVED =
            TESTS.function("recipe_generic_derived", 40, () -> GenericRecipeTableGameTests::derived);

    /**
     * Tool, not a test: every table's rows per config mode, as text (declared only when the filter names it).
     */
    public static final GameTests.Function DUMP = dumpRequested()
            ? TESTS.function("recipe_generic_dump", 400, () -> GenericRecipeTableGameTests::dump) : null;

    public static void init() {
    }

    record Mode(boolean expensive, boolean m528) {
        String key() {
            return (expensive ? "ex" : "noex") + "_" + (m528 ? "528" : "no528");
        }
    }

    static final List<Mode> MODE_LIST = List.of(new Mode(false, false), new Mode(true, false), new Mode(false, true),
            new Mode(true, true));

    static Map<String, DataGenericRecipes<?>> tables() {
        Map<String, DataGenericRecipes<?>> tables = new LinkedHashMap<>();
        tables.put("assembly_machine", AssemblyMachineRecipes.INSTANCE);
        tables.put("blast_furnace", BlastFurnaceRecipesNT.INSTANCE);
        tables.put("difurnace", DiFurnaceRecipes.INSTANCE);
        tables.put("fluid_breeder", FluidBreederRecipes.INSTANCE);
        tables.put("fusion", FusionRecipes.INSTANCE);
        tables.put("mixer", MixerRecipes.INSTANCE);
        tables.put("outgasser", OutgasserRecipes.INSTANCE);
        tables.put("purex", PUREXRecipes.INSTANCE);
        tables.put("plasma_forge", PlasmaForgeRecipes.INSTANCE);
        tables.put("precision_assembler", PrecAssRecipes.INSTANCE);
        tables.put("exposure_chamber", ExposureChamberRecipes.INSTANCE);
        return tables;
    }

    /**
     * Runs {@code body} under {@code mode}, rebuilding every table, and restores the config.
     */
    static void under(GameTestHelper helper, Mode mode, Runnable body) {
        RecipeModeFixture.under(helper, mode.expensive(), mode.m528(), body);
    }

    /**
     * Rows per table and config mode of the Java tables (fluid packs included).
     */
    static final Map<String, Integer> EXPECTED_COUNTS = new HashMap<>();

    static {
        // one port-only row (ass.bobbleclawd) more than 1.7 in every mode
        EXPECTED_COUNTS.put("assembly_machine.noex_no528", 632);
        EXPECTED_COUNTS.put("assembly_machine.ex_no528", 632);
        EXPECTED_COUNTS.put("assembly_machine.noex_528", 622);
        EXPECTED_COUNTS.put("assembly_machine.ex_528", 622);
        EXPECTED_COUNTS.put("blast_furnace.noex_no528", 18);
        EXPECTED_COUNTS.put("blast_furnace.ex_no528", 18);
        EXPECTED_COUNTS.put("blast_furnace.noex_528", 18);
        EXPECTED_COUNTS.put("blast_furnace.ex_528", 18);
        EXPECTED_COUNTS.put("difurnace.noex_no528", 13);
        EXPECTED_COUNTS.put("difurnace.ex_no528", 13);
        EXPECTED_COUNTS.put("difurnace.noex_528", 13);
        EXPECTED_COUNTS.put("difurnace.ex_528", 13);
        EXPECTED_COUNTS.put("fluid_breeder.noex_no528", 3);
        EXPECTED_COUNTS.put("fluid_breeder.ex_no528", 3);
        EXPECTED_COUNTS.put("fluid_breeder.noex_528", 3);
        EXPECTED_COUNTS.put("fluid_breeder.ex_528", 3);
        EXPECTED_COUNTS.put("fusion.noex_no528", 10);
        EXPECTED_COUNTS.put("fusion.ex_no528", 10);
        EXPECTED_COUNTS.put("fusion.noex_528", 10);
        EXPECTED_COUNTS.put("fusion.ex_528", 10);
        EXPECTED_COUNTS.put("mixer.noex_no528", 50);
        EXPECTED_COUNTS.put("mixer.ex_no528", 50);
        EXPECTED_COUNTS.put("mixer.noex_528", 50);
        EXPECTED_COUNTS.put("mixer.ex_528", 50);
        EXPECTED_COUNTS.put("outgasser.noex_no528", 19);
        EXPECTED_COUNTS.put("outgasser.ex_no528", 19);
        EXPECTED_COUNTS.put("outgasser.noex_528", 19);
        EXPECTED_COUNTS.put("outgasser.ex_528", 19);
        EXPECTED_COUNTS.put("purex.noex_no528", 57);
        EXPECTED_COUNTS.put("purex.ex_no528", 57);
        EXPECTED_COUNTS.put("purex.noex_528", 57);
        EXPECTED_COUNTS.put("purex.ex_528", 57);
        EXPECTED_COUNTS.put("plasma_forge.noex_no528", 45);
        EXPECTED_COUNTS.put("plasma_forge.ex_no528", 45);
        EXPECTED_COUNTS.put("plasma_forge.noex_528", 45);
        EXPECTED_COUNTS.put("plasma_forge.ex_528", 45);
        EXPECTED_COUNTS.put("precision_assembler.noex_no528", 8);
        EXPECTED_COUNTS.put("precision_assembler.ex_no528", 8);
        EXPECTED_COUNTS.put("precision_assembler.noex_528", 48);
        EXPECTED_COUNTS.put("precision_assembler.ex_528", 48);
        EXPECTED_COUNTS.put("exposure_chamber.noex_no528", 4);
        EXPECTED_COUNTS.put("exposure_chamber.ex_no528", 4);
        EXPECTED_COUNTS.put("exposure_chamber.noex_528", 4);
        EXPECTED_COUNTS.put("exposure_chamber.ex_528", 4);
    }

    public static void counts(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (Mode mode : MODE_LIST) {
            under(helper, mode, () -> {
                for (Map.Entry<String, DataGenericRecipes<?>> e : tables().entrySet()) {
                    int expected = EXPECTED_COUNTS.get(e.getKey() + "." + mode.key());
                    int have = e.getValue().recipeOrderedList.size();
                    if (have != expected)
                        wrong.add(e.getKey() + " " + mode.key() + ": " + have + " rows, expected " + expected);
                }
            });
        }
        helper.assertTrue(wrong.isEmpty(), "row counts differ from the converted Java tables: " + wrong);
        helper.succeed();
    }

    /**
     * {table, mode, name, describe} of rows of the Java tables (row conditions not shown).
     */
    static final String[][] SAMPLE_ROWS = {
            {"assembly_machine", "noex_no528", "ass.gascent",
                    "ass.gascent d=400 p=100 in=hbm:centrifuge_elementx4,#c:ingots/any_plasticx8,#c:ingots/deshx2,#c:plates/steelx8,hbm:circuit_advancedx1, fin= out=hbm:machine_gascentx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ex=hbm:centrifuge_elementx4,mat:steel/WELDEDPLATEx4,hbm:item_expensive_heavy_framex1,hbm:item_expensive_circuitx1, pools528=528.gascent"},
            {"assembly_machine", "noex_no528", "ass.minenaval",
                    "ass.minenaval d=300 p=100 in=hbm:sphere_steelx1,mat:steel/PIPEx3,hbm:circuit_advancedx1,#c:ingots/any_plasticexplosivex24, fin= out=hbm:mine_navalx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"assembly_machine", "noex_no528", "ass.nitra",
                    "ass.nitra d=200 p=500 in=hbm:nitrax1, fin= out=hbm:casing_shotshellx1%1.0, fout= icon=hbm:nitrax1 named=true wrap=null pools=- group=null choices=hbm:casing_shotshellx1w20.0,hbm:casing_buckshotx1w10.0,hbm:casing_smallx2w20.0,hbm:casing_largex1w10.0,minecraft:gunpowderx1w30.0,hbm:corditex1w20.0,hbm:ballistitex1w20.0,hbm:grenade_shell_fragx1w5.0,hbm:grenade_shell_stickx1w5.0,hbm:grenade_fuze_s3x1w10.0,"},
            {"assembly_machine", "noex_no528", "ass.overdrive1",
                    "ass.overdrive1 d=200 p=100 in=hbm:upgrade_speed_3x1,hbm:upgrade_effect_3x1,#c:ingots/saturnitex16,#c:ingots/any_hardplasticx16,hbm:circuit_advancedx16, fin= out=hbm:upgrade_overdrive_1x1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"assembly_machine", "noex_no528", "ass.packagehbmfluid.water",
                    "ass.packagehbmfluid.water d=40 p=100 in=hbm:fluid_pack_emptyx1, fin=waterx32000@0, out=hbm:fluid_pack_fullx1{fluid=water;amount=32000}%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"assembly_machine", "noex_no528", "ass.plateiron",
                    "ass.plateiron d=60 p=100 in=#c:ingots/ironx1, fin= out=hbm:plate_ironx1%1.0, fout= icon=- named=false wrap=null pools=alt.plates group=autoswitch.plates"},
            {"assembly_machine", "ex_no528", "ass.gascent",
                    "ass.gascent d=400 p=100 in=hbm:centrifuge_elementx4,mat:steel/WELDEDPLATEx4,hbm:item_expensive_heavy_framex1,hbm:item_expensive_circuitx1, fin= out=hbm:machine_gascentx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ex=hbm:centrifuge_elementx4,mat:steel/WELDEDPLATEx4,hbm:item_expensive_heavy_framex1,hbm:item_expensive_circuitx1, pools528=528.gascent"},
            {"assembly_machine", "ex_no528", "ass.minenaval",
                    "ass.minenaval d=300 p=100 in=hbm:sphere_steelx1,mat:steel/PIPEx3,hbm:circuit_advancedx1,#c:ingots/any_plasticexplosivex24, fin= out=hbm:mine_navalx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"assembly_machine", "ex_no528", "ass.nitra",
                    "ass.nitra d=200 p=500 in=hbm:nitrax1, fin= out=hbm:casing_shotshellx1%1.0, fout= icon=hbm:nitrax1 named=true wrap=null pools=- group=null choices=hbm:casing_shotshellx1w20.0,hbm:casing_buckshotx1w10.0,hbm:casing_smallx2w20.0,hbm:casing_largex1w10.0,minecraft:gunpowderx1w30.0,hbm:corditex1w20.0,hbm:ballistitex1w20.0,hbm:grenade_shell_fragx1w5.0,hbm:grenade_shell_stickx1w5.0,hbm:grenade_fuze_s3x1w10.0,"},
            {"assembly_machine", "ex_no528", "ass.overdrive1",
                    "ass.overdrive1 d=200 p=100 in=hbm:upgrade_speed_3x1,hbm:upgrade_effect_3x1,#c:ingots/saturnitex16,#c:ingots/any_hardplasticx16,hbm:circuit_advancedx16, fin= out=hbm:upgrade_overdrive_1x1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"assembly_machine", "ex_no528", "ass.packagehbmfluid.water",
                    "ass.packagehbmfluid.water d=40 p=100 in=hbm:fluid_pack_emptyx1, fin=waterx32000@0, out=hbm:fluid_pack_fullx1{fluid=water;amount=32000}%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"assembly_machine", "ex_no528", "ass.plateiron",
                    "ass.plateiron d=60 p=100 in=#c:ingots/ironx1, fin= out=hbm:plate_ironx1%1.0, fout= icon=- named=false wrap=null pools=alt.plates group=autoswitch.plates"},
            {"assembly_machine", "noex_528", "ass.gascent",
                    "ass.gascent d=400 p=100 in=hbm:centrifuge_elementx4,#c:ingots/any_plasticx8,#c:ingots/deshx2,#c:plates/steelx8,hbm:circuit_advancedx1, fin= out=hbm:machine_gascentx1%1.0, fout= icon=- named=false wrap=null pools=528.gascent group=null ex=hbm:centrifuge_elementx4,mat:steel/WELDEDPLATEx4,hbm:item_expensive_heavy_framex1,hbm:item_expensive_circuitx1, pools528=528.gascent"},
            {"assembly_machine", "noex_528", "ass.minenaval",
                    "ass.minenaval d=300 p=100 in=hbm:sphere_steelx1,mat:steel/PIPEx3,hbm:circuit_advancedx1,#c:ingots/any_plasticexplosivex24, fin= out=hbm:mine_navalx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"assembly_machine", "noex_528", "ass.nitra",
                    "ass.nitra d=200 p=500 in=hbm:nitrax1, fin= out=hbm:casing_shotshellx1%1.0, fout= icon=hbm:nitrax1 named=true wrap=null pools=- group=null choices=hbm:casing_shotshellx1w20.0,hbm:casing_buckshotx1w10.0,hbm:casing_smallx2w20.0,hbm:casing_largex1w10.0,minecraft:gunpowderx1w30.0,hbm:corditex1w20.0,hbm:ballistitex1w20.0,hbm:grenade_shell_fragx1w5.0,hbm:grenade_shell_stickx1w5.0,hbm:grenade_fuze_s3x1w10.0,"},
            {"assembly_machine", "noex_528", "ass.packagehbmfluid.water",
                    "ass.packagehbmfluid.water d=40 p=100 in=hbm:fluid_pack_emptyx1, fin=waterx32000@0, out=hbm:fluid_pack_fullx1{fluid=water;amount=32000}%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"assembly_machine", "noex_528", "ass.plateiron",
                    "ass.plateiron d=60 p=100 in=#c:ingots/ironx1, fin= out=hbm:plate_ironx1%1.0, fout= icon=- named=false wrap=null pools=alt.plates group=autoswitch.plates"},
            {"assembly_machine", "ex_528", "ass.gascent",
                    "ass.gascent d=400 p=100 in=hbm:centrifuge_elementx4,mat:steel/WELDEDPLATEx4,hbm:item_expensive_heavy_framex1,hbm:item_expensive_circuitx1, fin= out=hbm:machine_gascentx1%1.0, fout= icon=- named=false wrap=null pools=528.gascent group=null ex=hbm:centrifuge_elementx4,mat:steel/WELDEDPLATEx4,hbm:item_expensive_heavy_framex1,hbm:item_expensive_circuitx1, pools528=528.gascent"},
            {"assembly_machine", "ex_528", "ass.minenaval",
                    "ass.minenaval d=300 p=100 in=hbm:sphere_steelx1,mat:steel/PIPEx3,hbm:circuit_advancedx1,#c:ingots/any_plasticexplosivex24, fin= out=hbm:mine_navalx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"assembly_machine", "ex_528", "ass.nitra",
                    "ass.nitra d=200 p=500 in=hbm:nitrax1, fin= out=hbm:casing_shotshellx1%1.0, fout= icon=hbm:nitrax1 named=true wrap=null pools=- group=null choices=hbm:casing_shotshellx1w20.0,hbm:casing_buckshotx1w10.0,hbm:casing_smallx2w20.0,hbm:casing_largex1w10.0,minecraft:gunpowderx1w30.0,hbm:corditex1w20.0,hbm:ballistitex1w20.0,hbm:grenade_shell_fragx1w5.0,hbm:grenade_shell_stickx1w5.0,hbm:grenade_fuze_s3x1w10.0,"},
            {"assembly_machine", "ex_528", "ass.packagehbmfluid.water",
                    "ass.packagehbmfluid.water d=40 p=100 in=hbm:fluid_pack_emptyx1, fin=waterx32000@0, out=hbm:fluid_pack_fullx1{fluid=water;amount=32000}%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"assembly_machine", "ex_528", "ass.plateiron",
                    "ass.plateiron d=60 p=100 in=#c:ingots/ironx1, fin= out=hbm:plate_ironx1%1.0, fout= icon=- named=false wrap=null pools=alt.plates group=autoswitch.plates"},
            {"blast_furnace", "noex_no528", "blast.starmetal",
                    "blast.starmetal d=600 p=0 in=#c:ingots/saturnitex1,hbm:ingot_meteoritex1, fin= out=hbm:ingot_starmetalx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"blast_furnace", "noex_no528", "blast.steelFromIngot",
                    "blast.steelFromIngot d=800 p=0 in=#c:ingots/ironx2,#c:sandsx1, fin= out=hbm:ingot_steelx2%1.0,hbm:ingot_raw_slagx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"blast_furnace", "ex_no528", "blast.starmetal",
                    "blast.starmetal d=600 p=0 in=#c:ingots/saturnitex1,hbm:ingot_meteoritex1, fin= out=hbm:ingot_starmetalx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"blast_furnace", "ex_no528", "blast.steelFromIngot",
                    "blast.steelFromIngot d=800 p=0 in=#c:ingots/ironx2,#c:sandsx1, fin= out=hbm:ingot_steelx2%1.0,hbm:ingot_raw_slagx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"blast_furnace", "noex_528", "blast.starmetal",
                    "blast.starmetal d=600 p=0 in=#c:ingots/saturnitex1,hbm:ingot_meteoritex1, fin= out=hbm:ingot_starmetalx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"blast_furnace", "noex_528", "blast.steelFromIngot",
                    "blast.steelFromIngot d=800 p=0 in=#c:ingots/ironx2,#c:sandsx1, fin= out=hbm:ingot_steelx2%1.0,hbm:ingot_raw_slagx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"blast_furnace", "ex_528", "blast.starmetal",
                    "blast.starmetal d=600 p=0 in=#c:ingots/saturnitex1,hbm:ingot_meteoritex1, fin= out=hbm:ingot_starmetalx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"blast_furnace", "ex_528", "blast.steelFromIngot",
                    "blast.steelFromIngot d=800 p=0 in=#c:ingots/ironx2,#c:sandsx1, fin= out=hbm:ingot_steelx2%1.0,hbm:ingot_raw_slagx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "noex_no528", "difurnace.meteorSword",
                    "difurnace.meteorSword d=400 p=0 in=hbm:meteorite_sword_hardenedx1,any(#c:ingots/cobaltx1|#c:plates/cobaltx1|#c:gems/cobaltx1|#c:dusts/cobaltx1)x1, fin= out=hbm:meteorite_sword_alloyedx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "noex_no528", "difurnace.napalm",
                    "difurnace.napalm d=400 p=0 in=fi:hbm:canister_full/gasolinex1,#c:slime_ballsx1, fin= out=hbm:canister_napalmx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "noex_no528", "difurnace.steelFromIron",
                    "difurnace.steelFromIron d=400 p=0 in=any(#c:ingots/ironx1|#c:plates/ironx1|#c:gems/ironx1|#c:dusts/ironx1)x1,any(#c:ingots/coalx1|#c:plates/coalx1|#c:gems/coalx1|#c:dusts/coalx1)x1, fin= out=hbm:ingot_steelx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "ex_no528", "difurnace.meteorSword",
                    "difurnace.meteorSword d=400 p=0 in=hbm:meteorite_sword_hardenedx1,any(#c:ingots/cobaltx1|#c:plates/cobaltx1|#c:gems/cobaltx1|#c:dusts/cobaltx1)x1, fin= out=hbm:meteorite_sword_alloyedx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "ex_no528", "difurnace.napalm",
                    "difurnace.napalm d=400 p=0 in=fi:hbm:canister_full/gasolinex1,#c:slime_ballsx1, fin= out=hbm:canister_napalmx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "ex_no528", "difurnace.steelFromIron",
                    "difurnace.steelFromIron d=400 p=0 in=any(#c:ingots/ironx1|#c:plates/ironx1|#c:gems/ironx1|#c:dusts/ironx1)x1,any(#c:ingots/coalx1|#c:plates/coalx1|#c:gems/coalx1|#c:dusts/coalx1)x1, fin= out=hbm:ingot_steelx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "noex_528", "difurnace.meteorSword",
                    "difurnace.meteorSword d=400 p=0 in=hbm:meteorite_sword_hardenedx1,any(#c:ingots/cobaltx1|#c:plates/cobaltx1|#c:gems/cobaltx1|#c:dusts/cobaltx1)x1, fin= out=hbm:meteorite_sword_alloyedx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "noex_528", "difurnace.napalm",
                    "difurnace.napalm d=400 p=0 in=fi:hbm:canister_full/gasolinex1,#c:slime_ballsx1, fin= out=hbm:canister_napalmx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "noex_528", "difurnace.steelFromIron",
                    "difurnace.steelFromIron d=400 p=0 in=any(#c:ingots/ironx1|#c:plates/ironx1|#c:gems/ironx1|#c:dusts/ironx1)x1,any(#c:ingots/coalx1|#c:plates/coalx1|#c:gems/coalx1|#c:dusts/coalx1)x1, fin= out=hbm:ingot_steelx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "ex_528", "difurnace.meteorSword",
                    "difurnace.meteorSword d=400 p=0 in=hbm:meteorite_sword_hardenedx1,any(#c:ingots/cobaltx1|#c:plates/cobaltx1|#c:gems/cobaltx1|#c:dusts/cobaltx1)x1, fin= out=hbm:meteorite_sword_alloyedx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "ex_528", "difurnace.napalm",
                    "difurnace.napalm d=400 p=0 in=fi:hbm:canister_full/gasolinex1,#c:slime_ballsx1, fin= out=hbm:canister_napalmx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"difurnace", "ex_528", "difurnace.steelFromIron",
                    "difurnace.steelFromIron d=400 p=0 in=any(#c:ingots/ironx1|#c:plates/ironx1|#c:gems/ironx1|#c:dusts/ironx1)x1,any(#c:ingots/coalx1|#c:plates/coalx1|#c:gems/coalx1|#c:dusts/coalx1)x1, fin= out=hbm:ingot_steelx1%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"fluid_breeder", "noex_no528", "fluid_breeder.gas",
                    "fluid_breeder.gas d=0 p=0 in= fin=gasx1000@0, out= fout=syngasx1000@0, icon=- named=false wrap=null pools=- group=null"},
            {"fluid_breeder", "noex_no528", "fluid_breeder.lightoil_crack",
                    "fluid_breeder.lightoil_crack d=0 p=0 in= fin=lightoil_crackx1000@0, out= fout=reformgasx1000@0, icon=- named=false wrap=null pools=- group=null"},
            {"fluid_breeder", "ex_no528", "fluid_breeder.gas",
                    "fluid_breeder.gas d=0 p=0 in= fin=gasx1000@0, out= fout=syngasx1000@0, icon=- named=false wrap=null pools=- group=null"},
            {"fluid_breeder", "ex_no528", "fluid_breeder.lightoil_crack",
                    "fluid_breeder.lightoil_crack d=0 p=0 in= fin=lightoil_crackx1000@0, out= fout=reformgasx1000@0, icon=- named=false wrap=null pools=- group=null"},
            {"fluid_breeder", "noex_528", "fluid_breeder.gas",
                    "fluid_breeder.gas d=0 p=0 in= fin=gasx1000@0, out= fout=syngasx1000@0, icon=- named=false wrap=null pools=- group=null"},
            {"fluid_breeder", "noex_528", "fluid_breeder.lightoil_crack",
                    "fluid_breeder.lightoil_crack d=0 p=0 in= fin=lightoil_crackx1000@0, out= fout=reformgasx1000@0, icon=- named=false wrap=null pools=- group=null"},
            {"fluid_breeder", "ex_528", "fluid_breeder.gas",
                    "fluid_breeder.gas d=0 p=0 in= fin=gasx1000@0, out= fout=syngasx1000@0, icon=- named=false wrap=null pools=- group=null"},
            {"fluid_breeder", "ex_528", "fluid_breeder.lightoil_crack",
                    "fluid_breeder.lightoil_crack d=0 p=0 in= fin=lightoil_crackx1000@0, out= fout=reformgasx1000@0, icon=- named=false wrap=null pools=- group=null"},
            {"fusion", "noex_no528", "fus.dd",
                    "fus.dd d=100 p=25000 in= fin=deuteriumx20@0, out= fout=helium4x1000@0, icon=hbm:gas_fullx1{fluid=deuterium;amount=1000} named=true wrap=null pools=- group=null ignition=750000 output=1000000 flux=50.0 rgb=1.0,0.2,0.2"},
            {"fusion", "noex_no528", "fus.stellar",
                    "fus.stellar d=100 p=25000 in= fin=stellar_fluxx5@0, out=hbm:powder_goldx1%1.0, fout= icon=hbm:fluid_iconx1{fluid=stellar_flux} named=true wrap=null pools=- group=null ignition=10000000 output=50000000 flux=10000.0 rgb=1.0,0.4,0.1"},
            {"fusion", "ex_no528", "fus.dd",
                    "fus.dd d=100 p=25000 in= fin=deuteriumx20@0, out= fout=helium4x1000@0, icon=hbm:gas_fullx1{fluid=deuterium;amount=1000} named=true wrap=null pools=- group=null ignition=750000 output=1000000 flux=50.0 rgb=1.0,0.2,0.2"},
            {"fusion", "ex_no528", "fus.stellar",
                    "fus.stellar d=100 p=25000 in= fin=stellar_fluxx5@0, out=hbm:powder_goldx1%1.0, fout= icon=hbm:fluid_iconx1{fluid=stellar_flux} named=true wrap=null pools=- group=null ignition=10000000 output=50000000 flux=10000.0 rgb=1.0,0.4,0.1"},
            {"fusion", "noex_528", "fus.dd",
                    "fus.dd d=100 p=25000 in= fin=deuteriumx20@0, out= fout=helium4x1000@0, icon=hbm:gas_fullx1{fluid=deuterium;amount=1000} named=true wrap=null pools=- group=null ignition=750000 output=1000000 flux=50.0 rgb=1.0,0.2,0.2"},
            {"fusion", "noex_528", "fus.stellar",
                    "fus.stellar d=100 p=25000 in= fin=stellar_fluxx5@0, out=hbm:powder_goldx1%1.0, fout= icon=hbm:fluid_iconx1{fluid=stellar_flux} named=true wrap=null pools=- group=null ignition=10000000 output=50000000 flux=10000.0 rgb=1.0,0.4,0.1"},
            {"fusion", "ex_528", "fus.dd",
                    "fus.dd d=100 p=25000 in= fin=deuteriumx20@0, out= fout=helium4x1000@0, icon=hbm:gas_fullx1{fluid=deuterium;amount=1000} named=true wrap=null pools=- group=null ignition=750000 output=1000000 flux=50.0 rgb=1.0,0.2,0.2"},
            {"fusion", "ex_528", "fus.stellar",
                    "fus.stellar d=100 p=25000 in= fin=stellar_fluxx5@0, out=hbm:powder_goldx1%1.0, fout= icon=hbm:fluid_iconx1{fluid=stellar_flux} named=true wrap=null pools=- group=null ignition=10000000 output=50000000 flux=10000.0 rgb=1.0,0.4,0.1"},
            {"mixer", "noex_no528", "mixer.coalgas_leaded.0",
                    "mixer.coalgas_leaded.0 d=40 p=0 in=hbm:fuel_additive_antiknockx1, fin=coalgasx10000@0, out= fout=coalgas_leadedx12000@0, icon=- named=false wrap=null pools=- group=null"},
            {"mixer", "noex_no528", "mixer.coolant.0",
                    "mixer.coolant.0 d=50 p=0 in=#c:dusts/niterx1, fin=waterx1800@0, out= fout=coolantx2000@0, icon=- named=false wrap=null pools=- group=null"},
            {"mixer", "ex_no528", "mixer.coalgas_leaded.0",
                    "mixer.coalgas_leaded.0 d=40 p=0 in=hbm:fuel_additive_antiknockx1, fin=coalgasx10000@0, out= fout=coalgas_leadedx12000@0, icon=- named=false wrap=null pools=- group=null"},
            {"mixer", "ex_no528", "mixer.coolant.0",
                    "mixer.coolant.0 d=50 p=0 in=#c:dusts/niterx1, fin=waterx1800@0, out= fout=coolantx2000@0, icon=- named=false wrap=null pools=- group=null"},
            {"mixer", "noex_528", "mixer.coalgas_leaded.0",
                    "mixer.coalgas_leaded.0 d=40 p=0 in=hbm:fuel_additive_antiknockx1, fin=coalgasx10000@0, out= fout=coalgas_leadedx12000@0, icon=- named=false wrap=null pools=- group=null"},
            {"mixer", "noex_528", "mixer.coolant.0",
                    "mixer.coolant.0 d=50 p=0 in=#c:dusts/niterx1, fin=waterx1800@0, out= fout=coolantx2000@0, icon=- named=false wrap=null pools=- group=null"},
            {"mixer", "ex_528", "mixer.coalgas_leaded.0",
                    "mixer.coalgas_leaded.0 d=40 p=0 in=hbm:fuel_additive_antiknockx1, fin=coalgasx10000@0, out= fout=coalgas_leadedx12000@0, icon=- named=false wrap=null pools=- group=null"},
            {"mixer", "ex_528", "mixer.coolant.0",
                    "mixer.coolant.0 d=50 p=0 in=#c:dusts/niterx1, fin=waterx1800@0, out= fout=coolantx2000@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "noex_no528", "outgasser.tritium_block",
                    "outgasser.tritium_block d=10000 p=0 in=#c:storage_blocks/lithiumx1, fin= out= fout=tritiumx10000@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "noex_no528", "outgasser.tritium_dust_tiny",
                    "outgasser.tritium_dust_tiny d=10000 p=0 in=#c:dusts_tiny/lithiumx1, fin= out= fout=tritiumx100@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "noex_no528", "outgasser.wax_tar",
                    "outgasser.wax_tar d=10000 p=0 in=hbm:oil_tar_waxx1, fin= out= fout=radiosolventx100@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "ex_no528", "outgasser.tritium_block",
                    "outgasser.tritium_block d=10000 p=0 in=#c:storage_blocks/lithiumx1, fin= out= fout=tritiumx10000@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "ex_no528", "outgasser.tritium_dust_tiny",
                    "outgasser.tritium_dust_tiny d=10000 p=0 in=#c:dusts_tiny/lithiumx1, fin= out= fout=tritiumx100@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "ex_no528", "outgasser.wax_tar",
                    "outgasser.wax_tar d=10000 p=0 in=hbm:oil_tar_waxx1, fin= out= fout=radiosolventx100@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "noex_528", "outgasser.tritium_block",
                    "outgasser.tritium_block d=10000 p=0 in=#c:storage_blocks/lithiumx1, fin= out= fout=tritiumx10000@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "noex_528", "outgasser.tritium_dust_tiny",
                    "outgasser.tritium_dust_tiny d=10000 p=0 in=#c:dusts_tiny/lithiumx1, fin= out= fout=tritiumx100@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "noex_528", "outgasser.wax_tar",
                    "outgasser.wax_tar d=10000 p=0 in=hbm:oil_tar_waxx1, fin= out= fout=radiosolventx100@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "ex_528", "outgasser.tritium_block",
                    "outgasser.tritium_block d=10000 p=0 in=#c:storage_blocks/lithiumx1, fin= out= fout=tritiumx10000@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "ex_528", "outgasser.tritium_dust_tiny",
                    "outgasser.tritium_dust_tiny d=10000 p=0 in=#c:dusts_tiny/lithiumx1, fin= out= fout=tritiumx100@0, icon=- named=false wrap=null pools=- group=null"},
            {"outgasser", "ex_528", "outgasser.wax_tar",
                    "outgasser.wax_tar d=10000 p=0 in=hbm:oil_tar_waxx1, fin= out= fout=radiosolventx100@0, icon=- named=false wrap=null pools=- group=null"},
            {"purex", "noex_no528", "purex.pwrmeu",
                    "purex.pwrmeu d=100 p=2500 in=hbm:pwr_fuel_depleted_meu!exactx1, fin=kerosenex500@0,nitric_acidx250@0, out=hbm:nugget_u238x3%1.0,hbm:nugget_plutoniumx4%1.0,hbm:nugget_technetiumx2%1.0,hbm:nuclear_waste_tinyx3%1.0, fout= icon=hbm:pwr_fuel_depleted_meux1 named=false wrap=purex.recycle pools=- group=autoswitch.pwr"},
            {"purex", "noex_no528", "purex.schrabmen",
                    "purex.schrabmen d=200 p=50000 in=hbm:pwr_fuel_depleted_men!exactx1, fin=solventx4000@0,schrabidicx250@0, out=hbm:powder_schrabidiumx1%1.0,hbm:nugget_technetiumx3%1.0,hbm:nuclear_waste_tinyx4%1.0, fout= icon=hbm:pwr_fuel_depleted_menx1 named=false wrap=purex.schrab pools=- group=autoswitch.schrab"},
            {"purex", "noex_no528", "purex.thoriumsalt",
                    "purex.thoriumsalt d=20 p=10000 in=#c:nuggets/thoriumx2, fin=thorium_salt_depletedx16000@0, out=hbm:nugget_u233x1%0.5,hbm:nuclear_waste_tinyx1%0.25, fout=thorium_saltx16000@0, icon=hbm:fluid_iconx1{fluid=thorium_salt} named=false wrap=null pools=- group=null"},
            {"purex", "noex_no528", "purex.uzh",
                    "purex.uzh d=600 p=1000 in=hbm:billet_uranium_fuelx1,#c:billets/zirconiumx3, fin=nitric_acidx1000@0,hydrogenx4000@0, out=hbm:billet_uzhx4%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"purex", "noex_no528", "purex.zirnoxnu",
                    "purex.zirnoxnu d=100 p=1000 in=cold:hbm:waste_natural_uraniumx1, fin=kerosenex500@0,nitric_acidx250@0, out=hbm:nugget_u238x1%1.0,hbm:nugget_pu_mixx2%1.0,hbm:nugget_pu239x1%1.0,hbm:nuclear_waste_tinyx2%1.0, fout= icon=hbm:waste_natural_uraniumx1 named=false wrap=purex.recycle pools=- group=autoswitch.zirnox"},
            {"purex", "ex_no528", "purex.pwrmeu",
                    "purex.pwrmeu d=100 p=2500 in=hbm:pwr_fuel_depleted_meu!exactx1, fin=kerosenex500@0,nitric_acidx250@0, out=hbm:nugget_u238x3%1.0,hbm:nugget_plutoniumx4%1.0,hbm:nugget_technetiumx2%1.0,hbm:nuclear_waste_tinyx3%1.0, fout= icon=hbm:pwr_fuel_depleted_meux1 named=false wrap=purex.recycle pools=- group=autoswitch.pwr"},
            {"purex", "ex_no528", "purex.schrabmen",
                    "purex.schrabmen d=200 p=50000 in=hbm:pwr_fuel_depleted_men!exactx1, fin=solventx4000@0,schrabidicx250@0, out=hbm:powder_schrabidiumx1%1.0,hbm:nugget_technetiumx3%1.0,hbm:nuclear_waste_tinyx4%1.0, fout= icon=hbm:pwr_fuel_depleted_menx1 named=false wrap=purex.schrab pools=- group=autoswitch.schrab"},
            {"purex", "ex_no528", "purex.thoriumsalt",
                    "purex.thoriumsalt d=20 p=10000 in=#c:nuggets/thoriumx2, fin=thorium_salt_depletedx16000@0, out=hbm:nugget_u233x1%0.5,hbm:nuclear_waste_tinyx1%0.25, fout=thorium_saltx16000@0, icon=hbm:fluid_iconx1{fluid=thorium_salt} named=false wrap=null pools=- group=null"},
            {"purex", "ex_no528", "purex.uzh",
                    "purex.uzh d=600 p=1000 in=hbm:billet_uranium_fuelx1,#c:billets/zirconiumx3, fin=nitric_acidx1000@0,hydrogenx4000@0, out=hbm:billet_uzhx4%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"purex", "ex_no528", "purex.zirnoxnu",
                    "purex.zirnoxnu d=100 p=1000 in=cold:hbm:waste_natural_uraniumx1, fin=kerosenex500@0,nitric_acidx250@0, out=hbm:nugget_u238x1%1.0,hbm:nugget_pu_mixx2%1.0,hbm:nugget_pu239x1%1.0,hbm:nuclear_waste_tinyx2%1.0, fout= icon=hbm:waste_natural_uraniumx1 named=false wrap=purex.recycle pools=- group=autoswitch.zirnox"},
            {"purex", "noex_528", "purex.pwrmeu",
                    "purex.pwrmeu d=100 p=2500 in=hbm:pwr_fuel_depleted_meu!exactx1, fin=kerosenex500@0,nitric_acidx250@0, out=hbm:nugget_u238x3%1.0,hbm:nugget_plutoniumx4%1.0,hbm:nugget_technetiumx2%1.0,hbm:nuclear_waste_tinyx3%1.0, fout= icon=hbm:pwr_fuel_depleted_meux1 named=false wrap=purex.recycle pools=- group=autoswitch.pwr"},
            {"purex", "noex_528", "purex.schrabmen",
                    "purex.schrabmen d=200 p=50000 in=hbm:pwr_fuel_depleted_men!exactx1, fin=solventx4000@0,schrabidicx250@0, out=hbm:powder_schrabidiumx1%1.0,hbm:nugget_technetiumx3%1.0,hbm:nuclear_waste_tinyx4%1.0, fout= icon=hbm:pwr_fuel_depleted_menx1 named=false wrap=purex.schrab pools=- group=autoswitch.schrab"},
            {"purex", "noex_528", "purex.thoriumsalt",
                    "purex.thoriumsalt d=20 p=10000 in=#c:nuggets/thoriumx2, fin=thorium_salt_depletedx16000@0, out=hbm:nugget_u233x1%0.5,hbm:nuclear_waste_tinyx1%0.25, fout=thorium_saltx16000@0, icon=hbm:fluid_iconx1{fluid=thorium_salt} named=false wrap=null pools=- group=null"},
            {"purex", "noex_528", "purex.uzh",
                    "purex.uzh d=600 p=1000 in=hbm:billet_uranium_fuelx1,#c:billets/zirconiumx3, fin=nitric_acidx1000@0,hydrogenx4000@0, out=hbm:billet_uzhx4%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"purex", "noex_528", "purex.zirnoxnu",
                    "purex.zirnoxnu d=100 p=1000 in=cold:hbm:waste_natural_uraniumx1, fin=kerosenex500@0,nitric_acidx250@0, out=hbm:nugget_u238x1%1.0,hbm:nugget_pu_mixx2%1.0,hbm:nugget_pu239x1%1.0,hbm:nuclear_waste_tinyx2%1.0, fout= icon=hbm:waste_natural_uraniumx1 named=false wrap=purex.recycle pools=- group=autoswitch.zirnox"},
            {"purex", "ex_528", "purex.pwrmeu",
                    "purex.pwrmeu d=100 p=2500 in=hbm:pwr_fuel_depleted_meu!exactx1, fin=kerosenex500@0,nitric_acidx250@0, out=hbm:nugget_u238x3%1.0,hbm:nugget_plutoniumx4%1.0,hbm:nugget_technetiumx2%1.0,hbm:nuclear_waste_tinyx3%1.0, fout= icon=hbm:pwr_fuel_depleted_meux1 named=false wrap=purex.recycle pools=- group=autoswitch.pwr"},
            {"purex", "ex_528", "purex.schrabmen",
                    "purex.schrabmen d=200 p=50000 in=hbm:pwr_fuel_depleted_men!exactx1, fin=solventx4000@0,schrabidicx250@0, out=hbm:powder_schrabidiumx1%1.0,hbm:nugget_technetiumx3%1.0,hbm:nuclear_waste_tinyx4%1.0, fout= icon=hbm:pwr_fuel_depleted_menx1 named=false wrap=purex.schrab pools=- group=autoswitch.schrab"},
            {"purex", "ex_528", "purex.thoriumsalt",
                    "purex.thoriumsalt d=20 p=10000 in=#c:nuggets/thoriumx2, fin=thorium_salt_depletedx16000@0, out=hbm:nugget_u233x1%0.5,hbm:nuclear_waste_tinyx1%0.25, fout=thorium_saltx16000@0, icon=hbm:fluid_iconx1{fluid=thorium_salt} named=false wrap=null pools=- group=null"},
            {"purex", "ex_528", "purex.uzh",
                    "purex.uzh d=600 p=1000 in=hbm:billet_uranium_fuelx1,#c:billets/zirconiumx3, fin=nitric_acidx1000@0,hydrogenx4000@0, out=hbm:billet_uzhx4%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"purex", "ex_528", "purex.zirnoxnu",
                    "purex.zirnoxnu d=100 p=1000 in=cold:hbm:waste_natural_uraniumx1, fin=kerosenex500@0,nitric_acidx250@0, out=hbm:nugget_u238x1%1.0,hbm:nugget_pu_mixx2%1.0,hbm:nugget_pu239x1%1.0,hbm:nuclear_waste_tinyx2%1.0, fout= icon=hbm:waste_natural_uraniumx1 named=false wrap=purex.recycle pools=- group=autoswitch.zirnox"},
            {"plasma_forge", "noex_no528", "ass.fensusan",
                    "ass.fensusan d=6000 p=50000000 in=hbm:ingot_electroniumx64,hbm:battery_quantumx1,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,#c:plates_cast/any_bismoid_bronzex64,mat:combine_steel/CASTPLATEx32,mat:magnetized_tungsten/DENSEWIREx32,hbm:plate_dineutroniumx64,hbm:powder_magicx64,hbm:ingot_u238m2!exactx1,hbm:ingot_cftx64,hbm:ingot_cftx64, fin= out=hbm:machine_battery_reddx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ex=hbm:ingot_electroniumx64,hbm:battery_quantumx1,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_ferro_platingx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,#c:plates_cast/any_bismoid_bronzex64,mat:combine_steel/CASTPLATEx64,hbm:ingot_u238m2!exactx1,hbm:ingot_cftx64,hbm:ingot_cftx64, ignition=50000000"},
            {"plasma_forge", "noex_no528", "plsm.dfcstabilizer",
                    "plsm.dfcstabilizer d=1200 p=10000000 in=mat:osmiridium/WELDEDPLATEx16,mat:schrabidate/DENSEWIREx16,hbm:circuit_controller_quantumx8, fin=stellar_fluxx4000@0, out=hbm:dfc_stabilizerx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ignition=50000000"},
            {"plasma_forge", "noex_no528", "plsm.gerald",
                    "plsm.gerald d=12000 p=50000000 in=mat:schrabidate/CASTPLATEx64,mat:schrabidate/CASTPLATEx64,mat:bscco/DENSEWIREx64,mat:bscco/DENSEWIREx64,hbm:det_nukex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:circuit_controller_quantumx64,hbm:circuit_crystalx12,hbm:drive_disk_orbitdata_processedx16, fin= out=hbm:sat_geraldx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ex=mat:schrabidate/CASTPLATEx64,mat:bscco/DENSEWIREx64,hbm:det_nukex64,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_cmb_tubesx64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:circuit_controller_quantumx64,hbm:circuit_crystalx64,hbm:item_expensive_computerx64,hbm:drive_disk_orbitdata_processedx64, ignition=25000000"},
            {"plasma_forge", "noex_no528", "plsm.plateeuphemium",
                    "plsm.plateeuphemium d=600 p=10000000 in=#c:ingots/euphemiumx4,#c:dusts/astatinex3,#c:dusts/bismuthx1,#c:gems/volcanicx1,#c:ingots/osmiridiumx1, fin= out=hbm:plate_euphemiumx4%1.0, fout= icon=- named=false wrap=null pools=- group=null ignition=1000000"},
            {"plasma_forge", "noex_no528", "plsm.weldiron",
                    "plsm.weldiron d=50 p=100 in=mat:iron/CASTPLATEx2, fin= out=hbm:plate_welded_ironx1%1.0, fout= icon=- named=false wrap=null pools=- group=autoswitch.weldPlates ignition=500000"},
            {"plasma_forge", "ex_no528", "ass.fensusan",
                    "ass.fensusan d=6000 p=50000000 in=hbm:ingot_electroniumx64,hbm:battery_quantumx1,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_ferro_platingx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,#c:plates_cast/any_bismoid_bronzex64,mat:combine_steel/CASTPLATEx64,hbm:ingot_u238m2!exactx1,hbm:ingot_cftx64,hbm:ingot_cftx64, fin= out=hbm:machine_battery_reddx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ex=hbm:ingot_electroniumx64,hbm:battery_quantumx1,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_ferro_platingx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,#c:plates_cast/any_bismoid_bronzex64,mat:combine_steel/CASTPLATEx64,hbm:ingot_u238m2!exactx1,hbm:ingot_cftx64,hbm:ingot_cftx64, ignition=50000000"},
            {"plasma_forge", "ex_no528", "plsm.dfcstabilizer",
                    "plsm.dfcstabilizer d=1200 p=10000000 in=mat:osmiridium/WELDEDPLATEx16,mat:schrabidate/DENSEWIREx16,hbm:circuit_controller_quantumx8, fin=stellar_fluxx4000@0, out=hbm:dfc_stabilizerx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ignition=50000000"},
            {"plasma_forge", "ex_no528", "plsm.gerald",
                    "plsm.gerald d=12000 p=50000000 in=mat:schrabidate/CASTPLATEx64,mat:bscco/DENSEWIREx64,hbm:det_nukex64,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_cmb_tubesx64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:circuit_controller_quantumx64,hbm:circuit_crystalx64,hbm:item_expensive_computerx64,hbm:drive_disk_orbitdata_processedx64, fin= out=hbm:sat_geraldx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ex=mat:schrabidate/CASTPLATEx64,mat:bscco/DENSEWIREx64,hbm:det_nukex64,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_cmb_tubesx64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:circuit_controller_quantumx64,hbm:circuit_crystalx64,hbm:item_expensive_computerx64,hbm:drive_disk_orbitdata_processedx64, ignition=25000000"},
            {"plasma_forge", "ex_no528", "plsm.plateeuphemium",
                    "plsm.plateeuphemium d=600 p=10000000 in=#c:ingots/euphemiumx4,#c:dusts/astatinex3,#c:dusts/bismuthx1,#c:gems/volcanicx1,#c:ingots/osmiridiumx1, fin= out=hbm:plate_euphemiumx4%1.0, fout= icon=- named=false wrap=null pools=- group=null ignition=1000000"},
            {"plasma_forge", "ex_no528", "plsm.weldiron",
                    "plsm.weldiron d=50 p=100 in=mat:iron/CASTPLATEx2, fin= out=hbm:plate_welded_ironx1%1.0, fout= icon=- named=false wrap=null pools=- group=autoswitch.weldPlates ignition=500000"},
            {"plasma_forge", "noex_528", "ass.fensusan",
                    "ass.fensusan d=6000 p=50000000 in=hbm:ingot_electroniumx64,hbm:battery_quantumx1,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,#c:plates_cast/any_bismoid_bronzex64,mat:combine_steel/CASTPLATEx32,mat:magnetized_tungsten/DENSEWIREx32,hbm:plate_dineutroniumx64,hbm:powder_magicx64,hbm:ingot_u238m2!exactx1,hbm:ingot_cftx64,hbm:ingot_cftx64, fin= out=hbm:machine_battery_reddx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ex=hbm:ingot_electroniumx64,hbm:battery_quantumx1,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_ferro_platingx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,#c:plates_cast/any_bismoid_bronzex64,mat:combine_steel/CASTPLATEx64,hbm:ingot_u238m2!exactx1,hbm:ingot_cftx64,hbm:ingot_cftx64, ignition=50000000"},
            {"plasma_forge", "noex_528", "plsm.dfcstabilizer",
                    "plsm.dfcstabilizer d=1200 p=10000000 in=mat:osmiridium/WELDEDPLATEx16,mat:schrabidate/DENSEWIREx16,hbm:circuit_controller_quantumx8, fin=stellar_fluxx4000@0, out=hbm:dfc_stabilizerx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ignition=50000000"},
            {"plasma_forge", "noex_528", "plsm.gerald",
                    "plsm.gerald d=12000 p=50000000 in=mat:schrabidate/CASTPLATEx64,mat:schrabidate/CASTPLATEx64,mat:bscco/DENSEWIREx64,mat:bscco/DENSEWIREx64,hbm:det_nukex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:circuit_controller_quantumx64,hbm:circuit_crystalx12,hbm:drive_disk_orbitdata_processedx16, fin= out=hbm:sat_geraldx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ex=mat:schrabidate/CASTPLATEx64,mat:bscco/DENSEWIREx64,hbm:det_nukex64,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_cmb_tubesx64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:circuit_controller_quantumx64,hbm:circuit_crystalx64,hbm:item_expensive_computerx64,hbm:drive_disk_orbitdata_processedx64, ignition=25000000"},
            {"plasma_forge", "noex_528", "plsm.plateeuphemium",
                    "plsm.plateeuphemium d=600 p=10000000 in=#c:ingots/euphemiumx4,#c:dusts/astatinex3,#c:dusts/bismuthx1,#c:gems/volcanicx1,#c:ingots/osmiridiumx1, fin= out=hbm:plate_euphemiumx4%1.0, fout= icon=- named=false wrap=null pools=- group=null ignition=1000000"},
            {"plasma_forge", "noex_528", "plsm.weldiron",
                    "plsm.weldiron d=50 p=100 in=mat:iron/CASTPLATEx2, fin= out=hbm:plate_welded_ironx1%1.0, fout= icon=- named=false wrap=null pools=- group=autoswitch.weldPlates ignition=500000"},
            {"plasma_forge", "ex_528", "ass.fensusan",
                    "ass.fensusan d=6000 p=50000000 in=hbm:ingot_electroniumx64,hbm:battery_quantumx1,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_ferro_platingx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,#c:plates_cast/any_bismoid_bronzex64,mat:combine_steel/CASTPLATEx64,hbm:ingot_u238m2!exactx1,hbm:ingot_cftx64,hbm:ingot_cftx64, fin= out=hbm:machine_battery_reddx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ex=hbm:ingot_electroniumx64,hbm:battery_quantumx1,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_ferro_platingx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,mat:osmiridium/WELDEDPLATEx64,#c:plates_cast/any_bismoid_bronzex64,mat:combine_steel/CASTPLATEx64,hbm:ingot_u238m2!exactx1,hbm:ingot_cftx64,hbm:ingot_cftx64, ignition=50000000"},
            {"plasma_forge", "ex_528", "plsm.dfcstabilizer",
                    "plsm.dfcstabilizer d=1200 p=10000000 in=mat:osmiridium/WELDEDPLATEx16,mat:schrabidate/DENSEWIREx16,hbm:circuit_controller_quantumx8, fin=stellar_fluxx4000@0, out=hbm:dfc_stabilizerx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ignition=50000000"},
            {"plasma_forge", "ex_528", "plsm.gerald",
                    "plsm.gerald d=12000 p=50000000 in=mat:schrabidate/CASTPLATEx64,mat:bscco/DENSEWIREx64,hbm:det_nukex64,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_cmb_tubesx64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:circuit_controller_quantumx64,hbm:circuit_crystalx64,hbm:item_expensive_computerx64,hbm:drive_disk_orbitdata_processedx64, fin= out=hbm:sat_geraldx1%1.0, fout= icon=- named=false wrap=null pools=- group=null ex=mat:schrabidate/CASTPLATEx64,mat:bscco/DENSEWIREx64,hbm:det_nukex64,hbm:item_expensive_cmb_tubesx64,hbm:item_expensive_cmb_tubesx64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:part_generic_hdex64,hbm:circuit_controller_quantumx64,hbm:circuit_crystalx64,hbm:item_expensive_computerx64,hbm:drive_disk_orbitdata_processedx64, ignition=25000000"},
            {"plasma_forge", "ex_528", "plsm.plateeuphemium",
                    "plsm.plateeuphemium d=600 p=10000000 in=#c:ingots/euphemiumx4,#c:dusts/astatinex3,#c:dusts/bismuthx1,#c:gems/volcanicx1,#c:ingots/osmiridiumx1, fin= out=hbm:plate_euphemiumx4%1.0, fout= icon=- named=false wrap=null pools=- group=null ignition=1000000"},
            {"plasma_forge", "ex_528", "plsm.weldiron",
                    "plsm.weldiron d=50 p=100 in=mat:iron/CASTPLATEx2, fin= out=hbm:plate_welded_ironx1%1.0, fout= icon=- named=false wrap=null pools=- group=autoswitch.weldPlates ignition=500000"},
            {"precision_assembler", "noex_no528", "precass.beigeprints",
                    "precass.beigeprints d=6000 p=50000 in=minecraft:paperx24,#c:gems/cinnabarx24,minecraft:pufferfishx8, fin= out=hbm:blueprint_folder_discoverx1%1.0, fout= icon=- named=false wrap=null pools=- group=null choices=hbm:blueprint_folder_discoverx1w5.0,minecraft:paperx24w95.0,"},
            {"precision_assembler", "noex_no528", "precass.blueprints",
                    "precass.blueprints d=6000 p=20000 in=minecraft:paperx16,#c:dyes/bluex16,minecraft:pufferfishx4, fin= out=hbm:blueprint_folderx1%1.0, fout= icon=- named=false wrap=null pools=- group=null choices=hbm:blueprint_folderx1w10.0,minecraft:paperx16w90.0,"},
            {"precision_assembler", "ex_no528", "precass.beigeprints",
                    "precass.beigeprints d=6000 p=50000 in=minecraft:paperx24,#c:gems/cinnabarx24,minecraft:pufferfishx8, fin= out=hbm:blueprint_folder_discoverx1%1.0, fout= icon=- named=false wrap=null pools=- group=null choices=hbm:blueprint_folder_discoverx1w5.0,minecraft:paperx24w95.0,"},
            {"precision_assembler", "ex_no528", "precass.blueprints",
                    "precass.blueprints d=6000 p=20000 in=minecraft:paperx16,#c:dyes/bluex16,minecraft:pufferfishx4, fin= out=hbm:blueprint_folderx1%1.0, fout= icon=- named=false wrap=null pools=- group=null choices=hbm:blueprint_folderx1w10.0,minecraft:paperx16w90.0,"},
            {"precision_assembler", "noex_528", "precass.beigeprints",
                    "precass.beigeprints d=6000 p=50000 in=minecraft:paperx24,#c:gems/cinnabarx24,minecraft:pufferfishx8, fin= out=hbm:blueprint_folder_discoverx1%1.0, fout= icon=- named=false wrap=null pools=- group=null choices=hbm:blueprint_folder_discoverx1w5.0,minecraft:paperx24w95.0,"},
            {"precision_assembler", "noex_528", "precass.blueprints",
                    "precass.blueprints d=6000 p=20000 in=minecraft:paperx16,#c:dyes/bluex16,minecraft:pufferfishx4, fin= out=hbm:blueprint_folderx1%1.0, fout= icon=- named=false wrap=null pools=- group=null choices=hbm:blueprint_folderx1w10.0,minecraft:paperx16w90.0,"},
            {"precision_assembler", "noex_528", "precass.chip",
                    "precass.chip d=100 p=200 in=hbm:circuit_siliconx1,hbm:plate_polymerx3,mat:gold/WIREx4, fin= out=hbm:circuit_chipx1%1.0, fout= icon=- named=false wrap=null pools=528.chip group=null choices=hbm:circuit_chipx1w90.0,hbm:broken_itemx1{broken=hbm:circuit_chip}w10.0,"},
            {"precision_assembler", "noex_528", "precass.chip.recycle",
                    "precass.chip.recycle d=100 p=200 in=hbm:broken_item!exact{broken=hbm:circuit_chip}x1, fin= out=hbm:circuit_siliconx1%0.9,hbm:plate_polymerx3%0.9,hbm:wire_fine_goldx4%0.9, fout= icon=hbm:broken_itemx1{broken=hbm:circuit_chip} named=false wrap=precass.recycle pools=- group=null"},
            {"precision_assembler", "ex_528", "precass.beigeprints",
                    "precass.beigeprints d=6000 p=50000 in=minecraft:paperx24,#c:gems/cinnabarx24,minecraft:pufferfishx8, fin= out=hbm:blueprint_folder_discoverx1%1.0, fout= icon=- named=false wrap=null pools=- group=null choices=hbm:blueprint_folder_discoverx1w5.0,minecraft:paperx24w95.0,"},
            {"precision_assembler", "ex_528", "precass.blueprints",
                    "precass.blueprints d=6000 p=20000 in=minecraft:paperx16,#c:dyes/bluex16,minecraft:pufferfishx4, fin= out=hbm:blueprint_folderx1%1.0, fout= icon=- named=false wrap=null pools=- group=null choices=hbm:blueprint_folderx1w10.0,minecraft:paperx16w90.0,"},
            {"precision_assembler", "ex_528", "precass.chip",
                    "precass.chip d=100 p=200 in=hbm:circuit_siliconx1,hbm:plate_polymerx3,mat:gold/WIREx4, fin= out=hbm:circuit_chipx1%1.0, fout= icon=- named=false wrap=null pools=528.chip group=null choices=hbm:circuit_chipx1w90.0,hbm:broken_itemx1{broken=hbm:circuit_chip}w10.0,"},
            {"precision_assembler", "ex_528", "precass.chip.recycle",
                    "precass.chip.recycle d=100 p=200 in=hbm:broken_item!exact{broken=hbm:circuit_chip}x1, fin= out=hbm:circuit_siliconx1%0.5,hbm:plate_polymerx3%0.5,hbm:wire_fine_goldx4%0.5, fout= icon=hbm:broken_itemx1{broken=hbm:circuit_chip} named=false wrap=precass.recycle pools=- group=null"},
            {"exposure_chamber", "noex_no528", "exposure.schrabidate",
                    "exposure.schrabidate d=200 p=10000 in=hbm:particle_sparkticlex1,#c:ingots/schrabidatex8, fin= out=hbm:ingot_dineutroniumx8%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"exposure_chamber", "noex_no528", "exposure.uranium",
                    "exposure.uranium d=200 p=10000 in=hbm:particle_higgsx1,#c:ingots/uraniumx8, fin= out=hbm:ingot_schraraniumx8%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"exposure_chamber", "ex_no528", "exposure.schrabidate",
                    "exposure.schrabidate d=200 p=10000 in=hbm:particle_sparkticlex1,hbm:item_expensive_degenerate_matterx8, fin= out=hbm:ingot_dineutroniumx8%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"exposure_chamber", "ex_no528", "exposure.uranium",
                    "exposure.uranium d=200 p=10000 in=hbm:particle_higgsx1,#c:ingots/uraniumx8, fin= out=hbm:ingot_schraraniumx8%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"exposure_chamber", "noex_528", "exposure.schrabidate",
                    "exposure.schrabidate d=200 p=10000 in=hbm:particle_sparkticlex1,#c:ingots/schrabidatex8, fin= out=hbm:ingot_dineutroniumx8%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"exposure_chamber", "noex_528", "exposure.uranium",
                    "exposure.uranium d=200 p=10000 in=hbm:particle_higgsx1,#c:ingots/uraniumx8, fin= out=hbm:ingot_schraraniumx8%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"exposure_chamber", "ex_528", "exposure.schrabidate",
                    "exposure.schrabidate d=200 p=10000 in=hbm:particle_sparkticlex1,hbm:item_expensive_degenerate_matterx8, fin= out=hbm:ingot_dineutroniumx8%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
            {"exposure_chamber", "ex_528", "exposure.uranium",
                    "exposure.uranium d=200 p=10000 in=hbm:particle_higgsx1,#c:ingots/uraniumx8, fin= out=hbm:ingot_schraraniumx8%1.0, fout= icon=- named=false wrap=null pools=- group=null"},
    };

    public static void samples(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (Mode mode : MODE_LIST) {
            under(helper, mode, () -> {
                for (String[] sample : SAMPLE_ROWS) {
                    if (!sample[1].equals(mode.key())) continue;
                    GenericRecipe recipe = tables().get(sample[0]).getRecipe(sample[2]);
                    String have = recipe == null ? "<missing>" : GenericRecipeCodec.describe(recipe)
                                                                                   .replaceAll(" requires=\\S+", "");
                    if (!have.equals(sample[3])) wrong.add(sample[1] + " " + have + "  EXPECTED  " + sample[3]);
                }
            });
        }
        helper.assertTrue(wrong.isEmpty(), "sample rows differ from the converted Java tables: " + wrong);
        helper.succeed();
    }

    public static void roundTrip(GameTestHelper helper) {
        List<String> broken = new ArrayList<>();
        for (Map.Entry<String, DataGenericRecipes<?>> e : tables().entrySet()) {
            roundTrip(helper, e.getValue(), broken);
        }
        helper.assertTrue(broken.isEmpty(), "rows that do not survive their JSON: " + broken);
        helper.succeed();
    }

    private static <T extends GenericRecipe> void roundTrip(GameTestHelper helper, DataGenericRecipes<T> table,
                                                            List<String> broken) {
        table.registerRecipes();
        for (var entry : table.table.entries()) {
            T recipe = entry.value();
            T copy = table.table.fromJson(table.table.toJson(recipe, helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            copy.bindStacks();
            copy.applyModes(GeneralConfig.enableExpensiveMode, GeneralConfig.enable528);
            recipe.bindStacks();
            if (!GenericRecipeCodec.describe(copy).equals(GenericRecipeCodec.describe(recipe))) broken.add(entry.id());
        }
    }

    public static void modes(GameTestHelper helper) {
        // ass.gascent: 1.7 inputItemsEx (heavy frame, circuit) and setPools528("528.gascent")
        under(helper, MODE_LIST.get(3), () -> {
            GenericRecipe gascent = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.gascent");
            helper.assertTrue(gascent.inputItemEx != null && gascent.inputItem == gascent.inputItemEx,
                    "expensive mode puts ass.gascent's expensive inputs in effect");
            helper.assertTrue(gascent.isPartOfPool("528.gascent")
                            && GenericRecipes.blueprintPools.get("528.gascent").contains("ass.gascent"),
                    "528 mode puts ass.gascent in its 528 pool");
            helper.assertTrue(AssemblyMachineRecipes.INSTANCE.getRecipe("ass.overdrive1") == null,
                    "528 mode drops the assembly overdrive rows (1.7 no528 block)");
            helper.assertTrue(PrecAssRecipes.INSTANCE.getRecipe("precass.chip") != null,
                    "528 mode adds the precision assembler circuits");
        });
        GenericRecipe gascent = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.gascent");
        helper.assertTrue(gascent.inputItem != gascent.inputItemEx && !gascent.isPooled()
                        && !GenericRecipes.blueprintPools.getOrDefault("528.gascent", List.of()).contains("ass.gascent")
                        && AssemblyMachineRecipes.INSTANCE.getRecipe("ass.overdrive1") != null
                        && PrecAssRecipes.INSTANCE.getRecipe("precass.chip") == null,
                "the default modes are restored on the next rebuild");
        helper.succeed();
    }

    public static void derived(GameTestHelper helper) {
        FusionRecipes.INSTANCE.registerRecipes();
        helper.assertTrue(FusionRecipes.INSTANCE.maxInput == 10_000_000L,
                "fusion maxInput " + FusionRecipes.INSTANCE.maxInput);
        // DERIVED_BEGIN
        // DERIVED_END
        helper.assertTrue(DiFurnaceRecipes.getItemPower(new ItemStack(Items.COAL)) == 200
                        && DiFurnaceRecipes.getItemPower(new ItemStack(Items.LAVA_BUCKET)) == 12_800
                        && DiFurnaceRecipes.getItemPower(new ItemStack(ModFuelItems.solid_fuel.get())) == 400
                        && DiFurnaceRecipes.getItemPower(new ItemStack(Items.STICK)) == 0,
                "DiFurnace fuel values from hbm:difurnace_fuel");
        helper.succeed();
    }

    static boolean dumpRequested() {
        String filter = System.getProperty("golem.gametest.filter");
        return filter != null && filter.contains("recipe_generic_dump");
    }

    public static void dump(GameTestHelper helper) {
        Path root = Path.of("recipe-export").toAbsolutePath().resolve("describe-data");
        for (Mode mode : MODE_LIST) {
            under(helper, mode, () -> {
                for (Map.Entry<String, DataGenericRecipes<?>> e : tables().entrySet()) {
                    List<String> lines = new ArrayList<>();
                    for (GenericRecipe recipe : e.getValue().recipeOrderedList)
                        lines.add(GenericRecipeCodec.describe(recipe));
                    write(root.resolve(e.getKey() + "." + mode.key() + ".txt"), String.join("\n", lines) + "\n");
                    write(root.resolve(e.getKey() + "." + mode.key() + ".extra.txt"), extra(e.getValue()));
                }
            });
        }
        helper.succeed();
    }

    /**
     * Indexes a table derives from its rows, as text.
     */
    static String extra(DataGenericRecipes<?> table) {
        StringBuilder out = new StringBuilder();
        if (table == MixerRecipes.INSTANCE) {
            for (Map.Entry<FluidType, MixerRecipes.MixerRecipe[]> e : MixerRecipes.recipes.entrySet()) {
                for (MixerRecipes.MixerRecipe r : e.getValue()) {
                    out.append(e.getKey().getKey()).append(": out=").append(r.output).append(" t=")
                       .append(r.processTime)
                       .append(" f1=").append(r.input1 == null ? "-" : r.input1.type.getKey() + "x" + r.input1.fill)
                       .append(" f2=").append(r.input2 == null ? "-" : r.input2.type.getKey() + "x" + r.input2.fill)
                       .append(" s=").append(r.solidInput == null ? "-" : GenericRecipeCodec.describe(r.solidInput))
                       .append('\n');
                }
            }
        }
        if (table == ExposureChamberRecipes.INSTANCE) {
            for (ExposureChamberRecipes.ExposureChamberRecipe r : ExposureChamberRecipes.recipes) {
                out.append(GenericRecipeCodec.describe(r.particle)).append(' ')
                   .append(GenericRecipeCodec.describe(r.ingredient))
                   .append(' ').append(BuiltInRegistries.ITEM.getKey(r.output.getItem()))
                   .append('x').append(r.output.getCount()).append('\n');
            }
        }
        if (table == FusionRecipes.INSTANCE)
            out.append("maxInput=").append(FusionRecipes.INSTANCE.maxInput).append('\n');
        if (table == DiFurnaceRecipes.INSTANCE) {
            for (DiFurnaceRecipes.FuelEntry fuel : DiFurnaceRecipes.getFuelEntries()) {
                out.append(GenericRecipeCodec.describe(fuel.ingredient())).append(' ').append(fuel.power())
                   .append('\n');
            }
        }
        return out.toString();
    }

    static void write(Path path, String text) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
