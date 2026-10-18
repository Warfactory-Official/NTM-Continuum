package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.data.FluidDataMaps;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.*;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The fluid-processing tables as data: every table loaded from {@code data/hbm/recipe/<table>} equals the
 * values of the former Java tables (1.7's), and the per-fluid values moved into {@code fluid_property} files and value
 * maps equal theirs.
 */
public final class FluidRecipeTableGameTests {

    private FluidRecipeTableGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function TABLES_EQUAL =
            TESTS.function("fluid_table_values", 20, () -> FluidRecipeTableGameTests::values);

    public static final GameTests.Function CODECS =
            TESTS.function("fluid_table_codecs", 20, () -> FluidRecipeTableGameTests::codecs);

    public static final GameTests.Function TURBINE =
            TESTS.function("fluid_table_gas_turbine", 20, () -> FluidRecipeTableGameTests::gasTurbine);

    public static void init() {}

    // Descriptions

    /**
     * HBM's own fluids (and vanilla's): the tables compare against plain HBM, so a row naming an addon's or another
     * mod's fluid or item is left out of the comparison, and HBM's suite passes with an addon loaded.
     */
    static boolean ours(FluidType type) {
        String id = type.getRegistryName();
        return id.startsWith(Constants.MOD_ID + ":") || id.startsWith("minecraft:");
    }

    static boolean ours(Item item) {
        String ns = BuiltInRegistries.ITEM.getKey(item).getNamespace();
        return ns.equals(Constants.MOD_ID) || ns.equals("minecraft");
    }

    /** Set by the describers when a row names something not {@link #ours}; {@link #add} drops that row. */
    private static boolean foreignRow;

    private static String foreignMark() {
        foreignRow = true;
        return "";
    }

    private static void add(TreeSet<String> rows, String row) {
        if (!foreignRow) rows.add(row);
        foreignRow = false;
    }

    static String fluid(FluidStack stack) {
        if (stack == null) return "-";
        if (!ours(stack.type)) foreignRow = true;
        return stack.type.getKey().toLowerCase(Locale.ROOT) + stack.fill + (stack.pressure != 0 ? "@" + stack.pressure : "");
    }

    static String item(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "-";
        if (!ours(stack.getItem())) foreignRow = true;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + "x" + stack.getCount();
    }

    static String astack(AStack stack) {
        if (stack == null) return "-";
        if (stack instanceof TagStack tag) return "#" + tag.tag.location() + "x" + stack.stacksize;
        if (stack instanceof ComparableStack comparable) {
            if (!ours(comparable.item)) foreignRow = true;
            return BuiltInRegistries.ITEM.getKey(comparable.item).getPath() + "x" + stack.stacksize;
        }
        if (stack instanceof MaterialStack material) return material.material.name + ":" + material.shape + "x" + stack.stacksize;
        return stack.getClass().getSimpleName();
    }

    static String key(FluidType type) {
        if (!ours(type)) foreignRow = true;
        return type.getKey().toLowerCase(Locale.ROOT);
    }

    static TreeSet<String> describe(String table) {
        ElectrolyserFluidRecipes.register();
        GasCentrifugeRecipes.register();
        TreeSet<String> rows = new TreeSet<>();
        foreignRow = false;
        switch (table) {
            case "coker" -> CokerRecipes.getAll().forEach((in, r) ->
                    add(rows, key(in) + ":" + r.amount() + ">" + item(r.output()) + "+" + fluid(r.byproduct())));
            case "compressor" -> CompressorRecipes.getAll().forEach((in, r) ->
                    add(rows, key(in.type()) + "@" + in.pressure() + ":" + r.inputAmount() + ">" + fluid(r.output()) + "/" + r.duration()));
            case "cracking" -> CrackingRecipes.getAll().forEach((in, r) ->
                    add(rows, key(in) + ":" + fluid(r.left()) + "," + fluid(r.right())));
            case "electrolyser_fluid" -> ElectrolyserFluidRecipes.RECIPES.forEach((in, r) -> {
                List<String> by = new ArrayList<>();
                for (ItemStack stack : r.byproduct) by.add(item(stack));
                add(rows, key(in) + "=" + key(r.input) + ":" + r.amount + ">" + fluid(r.output1) + "," + fluid(r.output2) + "/" + r.duration + by);
            });
            case "hydrotreating" -> HydrotreatingRecipes.getAll().forEach((in, r) ->
                    add(rows, key(in) + ":" + fluid(r.hydrogen()) + ">" + fluid(r.out1()) + "," + fluid(r.out2())));
            case "liquefaction" -> {
                LiquefactionRecipes.getItemRecipes().forEach((in, out) ->
                        add(rows, (ours(in) ? "" : foreignMark()) + Ids.canonicalItemId(BuiltInRegistries.ITEM.getKey(in).toString()) + ">" + fluid(out)));
                LiquefactionRecipes.getTagRecipes().forEach((in, out) -> add(rows, "#" + in.location() + ">" + fluid(out)));
            }
            case "pyrolysis" -> {
                for (PyroOvenRecipes.PyroOvenRecipe r : PyroOvenRecipes.getAll()) {
                    add(rows, fluid(r.inputFluid) + "+" + astack(r.inputItem) + ">" + fluid(r.outputFluid) + "+" + item(r.outputItem)
                            + "/" + r.duration);
                }
            }
            case "radiolysis" -> RadiolysisRecipes.getAll().forEach((in, r) ->
                    add(rows, key(in) + ":" + fluid(r.left()) + "," + fluid(r.right())));
            case "refinery" -> RefineryRecipes.getAll().forEach((in, r) ->
                    add(rows, key(in) + ":" + fluid(r.frac1()) + "," + fluid(r.frac2()) + "," + fluid(r.frac3()) + "," + fluid(r.frac4())
                            + "+" + item(r.byproduct())));
            case "vacuum_distill" -> RefineryRecipes.getAllVacuum().forEach((in, r) ->
                    add(rows, key(in) + ":" + fluid(r.frac1()) + "," + fluid(r.frac2()) + "," + fluid(r.frac3()) + "," + fluid(r.frac4())));
            case "reforming" -> ReformingRecipes.getAll().forEach((in, r) ->
                    add(rows, key(in) + ":" + fluid(r.out1()) + "," + fluid(r.out2()) + "," + fluid(r.out3())));
            case "solidification" -> SolidificationRecipes.getAll().forEach((in, r) ->
                    add(rows, key(in) + ":" + r.amount() + ">" + item(r.output())));
            case "storage_drum" -> {
                for (StorageDrumRecipes.Recipe r : StorageDrumRecipes.getAll()) {
                    add(rows, item(r.input()) + ">" + item(r.output()) + "/" + r.chance() + "," + r.liquid() + "," + r.gas());
                }
            }
            case "gas_centrifuge" -> {
                for (GasCentrifugeRecipes.GasCentrifugeRecipe r : GasCentrifugeRecipes.RECIPES) {
                    List<String> out = new ArrayList<>();
                    for (ItemStack stack : r.output) out.add(item(stack));
                    add(rows, fluid(r.input) + ">" + out + (r.highSpeed ? "/hs" : "") + "/" + r.centrifuges);
                }
                GasCentrifugeRecipes.FLUID_CONVERSIONS.forEach((in, pseudo) -> add(rows, "from " + key(in) + "=" + pseudo.getName()));
                for (String name : List.of("NUF6", "LEUF6", "MEUF6", "HEUF6", "PF6", "MUD", "MUD_HEAVY")) {
                    GasCentrifugeRecipes.PseudoFluidType type = GasCentrifugeRecipes.PseudoFluidType.fromName(name);
                    List<String> out = new ArrayList<>();
                    for (ItemStack stack : type.getOutput()) out.add(item(stack));
                    add(rows, type.getName() + ":" + type.getFluidConsumed() + ">" + type.getFluidProduced() + " "
                            + type.getOutputType().getName() + (type.getIfHighSpeed() ? "/hs" : "") + out);
                }
            }
            case "cooling" -> CoolingRecipes.getAll().forEach((in, r) -> add(rows, key(in) + "=" + key(r.input()) + ":" + r.inputAmount()
                    + ">" + key(r.output()) + r.outputAmount() + "/" + r.heatEnergy()));
            case "heat" -> {
                HeatRecipes.getAll().forEach((in, r) -> add(rows, key(in) + ":" + r.heatReq() + "," + r.amountReq() + ">"
                        + key(r.output()) + r.amountProduced()));
                HeatRecipes.HeatStep watz = HeatRecipes.getReactorStep(Fluids.COOLANT);
                add(rows, "reactor coolant:" + watz.heatReq() + "," + watz.amountReq() + ">" + key(watz.output()) + watz.amountProduced());
            }
            case "combustion" -> {
                for (FluidType type : Fluids.getAll()) {
                    long flammable = FluidCombustion.getFlammableEnergy(type);
                    FluidCombustion.Combustible combustible = FluidCombustion.getCombustible(type);
                    if (!FluidCombustion.isFlammable(type) && combustible == null) continue;
                    add(rows, key(type) + ":" + (FluidCombustion.isFlammable(type) ? String.valueOf(flammable) : "-") + ","
                            + (combustible == null ? "-" : combustible.grade().name() + combustible.energyPerBucket()));
                }
            }
            default -> throw new IllegalArgumentException(table);
        }
        return rows;
    }

    static final List<String> TABLES = List.of("coker", "compressor", "cracking", "electrolyser_fluid", "hydrotreating",
            "liquefaction", "pyrolysis", "radiolysis", "refinery", "vacuum_distill", "reforming", "solidification",
            "storage_drum", "gas_centrifuge", "cooling", "heat", "combustion");

    // Expected values

    /**
     * Every table as the former Java tables (1.7's values) described it, written once by the conversion: one line per row,
     * in sorted order. "combustion" is every fluid's flammable / combustible trait, "cooling" and "heat" the projections of
     * the coolable / heatable traits.
     */
    static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("coker", """
            aromatics:1700>coke_petroleumx1+gas_coker170
            biofuel:650>coke_petroleumx1+gas_coker65
            bitumen:16000>coke_petroleumx1+oil_coker1600
            calcium_solution:125>powder_calciumx1+spentsteam100
            coalcreosote:3200>coke_petroleumx1+naphtha_coker320
            diesel:590>coke_petroleumx1+naphtha_coker59
            diesel_crack:640>coke_petroleumx1+gas_coker64
            diesel_crack_reform:250>coke_petroleumx1+gas_coker25
            diesel_reform:230>coke_petroleumx1+naphtha_coker23
            fishoil:10000>coke_petroleumx1+mercury1000
            heatingoil:1600>coke_petroleumx1+oil_coker160
            heatingoil_vacuum:500>coke_petroleumx1+oil_coker50
            heavyoil:11000>coke_petroleumx1+oil_coker1100
            heavyoil_vacuum:3500>coke_petroleumx1+reformate350
            lightoil:370>coke_petroleumx1+gas_coker37
            lightoil_crack:590>coke_petroleumx1+gas_coker59
            lightoil_ds:180>coke_petroleumx1+gas_coker18
            lightoil_vacuum:180>coke_petroleumx1+gas_coker18
            lubricant:12000>coke_petroleumx1+oil_coker1200
            naphtha:4900>coke_petroleumx1+naphtha_coker490
            naphtha_crack:6400>coke_petroleumx1+naphtha_coker640
            naphtha_ds:2400>coke_petroleumx1+naphtha_coker240
            reclaimed:5800>coke_petroleumx1+naphtha_coker580
            redmud:450>iron_ingotx1+mercury50
            reformate:130>coke_petroleumx1+gas_coker13
            slop:1000>powder_limestonex1+colloid250
            smear:7900>coke_petroleumx1+oil_coker790
            sourgas:1000>sulfurx1+gas_coker150
            sunfloweroil:16000>coke_petroleumx1+gas_coker1600
            vitriol:4000>powder_ironx1+sulfuric_acid500
            watz:4000>ingot_mudx4+-
            woodoil:3000>charcoalx1+gas_coker300
            xylene:100>coke_petroleumx1+gas_coker10
            """);
        EXPECTED.put("compressor", """
            blood@3:1000>heavyoil250/200
            perfluoromethyl@0:1000>perfluoromethyl1000@1/50
            perfluoromethyl@1:1000>perfluoromethyl_cold1000/50
            petroleum@0:2000>petroleum2000@1/20
            petroleum@1:2000>lpg1000/20
            """);
        EXPECTED.put("cracking", """
            biogas:petroleum20,aromatics20
            bitumen:oil80,aromatics20
            diesel:kerosene40,petroleum30
            diesel_crack:kerosene40,petroleum30
            gas:petroleum30,unsaturateds20
            heatingoil_vacuum:heatingoil80,reformgas20
            kerosene:petroleum60,none0
            oil:crackoil80,petroleum20
            reformate:unsaturateds40,reformgas60
            smear:naphtha60,petroleum40
            woodoil:heatingoil40,aromatics10
            xylene:aromatics80,petroleum20
            """);
        EXPECTED.put("electrolyser_fluid", """
            alumina=alumina:200>carbondioxide100,none0/40[powder_aluminiumx7, fluoritex2]
            calcium_chloride=calcium_chloride:250>chlorine125,calcium_solution125/20[]
            heavywater=heavywater:2000>deuterium200,oxygen200/10[]
            potassium_chloride=potassium_chloride:250>chlorine125,none0/20[dustx1]
            redmud=redmud:450>mercury150,lye50/20[powder_titaniumx3, powder_ironx3, powder_aluminiumx2]
            slop=slop:1000>mercury250,none0/20[niterx2, powder_limestonex2, sulfurx1]
            vitriol=vitriol:1000>sulfuric_acid500,chlorine500/20[powder_ironx1, nugget_mercuryx1]
            water=water:2000>hydrogen200,oxygen200/10[]
            """);
        EXPECTED.put("hydrotreating", """
            coaloil:hydrogen10@1>coalgas80,sourgas15
            crackoil:hydrogen5@1>crackoil_ds90,sourgas15
            diesel_crack:hydrogen10@1>diesel80,sourgas30
            diesel_crack_reform:hydrogen10@1>diesel_reform80,sourgas30
            gas:hydrogen5@1>petroleum80,sourgas15
            oil:hydrogen5@1>oil_ds90,sourgas15
            """);
        EXPECTED.put("liquefaction", """
            #c:dusts/coal>coaloil250
            #c:dusts/lead>lead100
            #c:dusts/lignite>coaloil150
            #c:dusts/sodium>sodium100
            #c:gems/coal>coaloil250
            #c:gems/lignite>coaloil150
            #c:ingots/lead>lead100
            #c:storage_blocks/lead>lead900
            #hbm:raw_fishes>fishoil100
            #minecraft:logs>mug100
            hbm:biomass>biogas125
            hbm:glyphid_gland_empty>biogas2000
            hbm:oil_tar_coal>bitumen50
            hbm:oil_tar_crack>bitumen100
            hbm:oil_tar_crude>bitumen75
            hbm:ore_oil_sand>bitumen100
            hbm:plant_flower_cd0>ethanol50
            hbm:plant_flower_weed>ethanol100
            minecraft:cobblestone>lava250
            minecraft:ender_pearl>enderjuice100
            minecraft:fern>seedslurry100
            minecraft:ice>water1000
            minecraft:melon_slice>ethanol100
            minecraft:netherrack>lava250
            minecraft:obsidian>lava500
            minecraft:packed_ice>water1000
            minecraft:short_grass>seedslurry100
            minecraft:snow_block>water500
            minecraft:snowball>water125
            minecraft:stone>lava250
            minecraft:sugar>ethanol100
            minecraft:sunflower>sunfloweroil100
            minecraft:vine>seedslurry100
            minecraft:wheat_seeds>seedslurry50
            """);
        EXPECTED.put("pyrolysis", """
            -+#c:tarsx4>carbondioxide1000+powder_ash_sootx1/40
            -+bedrock_ore_base_actinidex1>vitriol50+bedrock_ore_base_roasted_actinidex1/10
            -+bedrock_ore_base_crystalx1>vitriol50+bedrock_ore_base_roasted_crystalx1/10
            -+bedrock_ore_base_heavyx1>vitriol50+bedrock_ore_base_roasted_heavyx1/10
            -+bedrock_ore_base_lightx1>vitriol50+bedrock_ore_base_roasted_lightx1/10
            -+bedrock_ore_base_nonmetalx1>vitriol50+bedrock_ore_base_roasted_nonmetalx1/10
            -+bedrock_ore_base_rarex1>vitriol50+bedrock_ore_base_roasted_rarex1/10
            -+bedrock_ore_primary_actinidex1>vitriol50+bedrock_ore_primary_roasted_actinidex1/10
            -+bedrock_ore_primary_crystalx1>vitriol50+bedrock_ore_primary_roasted_crystalx1/10
            -+bedrock_ore_primary_heavyx1>vitriol50+bedrock_ore_primary_roasted_heavyx1/10
            -+bedrock_ore_primary_lightx1>vitriol50+bedrock_ore_primary_roasted_lightx1/10
            -+bedrock_ore_primary_nonmetalx1>vitriol50+bedrock_ore_primary_roasted_nonmetalx1/10
            -+bedrock_ore_primary_rarex1>vitriol50+bedrock_ore_primary_roasted_rarex1/10
            -+bedrock_ore_rad_byproduct_actinidex1>vitriol50+bedrock_ore_rad_roasted_actinidex1/10
            -+bedrock_ore_rad_byproduct_crystalx1>vitriol50+bedrock_ore_rad_roasted_crystalx1/10
            -+bedrock_ore_rad_byproduct_heavyx1>vitriol50+bedrock_ore_rad_roasted_heavyx1/10
            -+bedrock_ore_rad_byproduct_lightx1>vitriol50+bedrock_ore_rad_roasted_lightx1/10
            -+bedrock_ore_rad_byproduct_nonmetalx1>vitriol50+bedrock_ore_rad_roasted_nonmetalx1/10
            -+bedrock_ore_rad_byproduct_rarex1>vitriol50+bedrock_ore_rad_roasted_rarex1/10
            -+bedrock_ore_solvent_byproduct_actinidex1>vitriol50+bedrock_ore_solvent_roasted_actinidex1/10
            -+bedrock_ore_solvent_byproduct_crystalx1>vitriol50+bedrock_ore_solvent_roasted_crystalx1/10
            -+bedrock_ore_solvent_byproduct_heavyx1>vitriol50+bedrock_ore_solvent_roasted_heavyx1/10
            -+bedrock_ore_solvent_byproduct_lightx1>vitriol50+bedrock_ore_solvent_roasted_lightx1/10
            -+bedrock_ore_solvent_byproduct_nonmetalx1>vitriol50+bedrock_ore_solvent_roasted_nonmetalx1/10
            -+bedrock_ore_solvent_byproduct_rarex1>vitriol50+bedrock_ore_solvent_roasted_rarex1/10
            -+bedrock_ore_sulfuric_byproduct_actinidex1>vitriol50+bedrock_ore_sulfuric_roasted_actinidex1/10
            -+bedrock_ore_sulfuric_byproduct_crystalx1>vitriol50+bedrock_ore_sulfuric_roasted_crystalx1/10
            -+bedrock_ore_sulfuric_byproduct_heavyx1>vitriol50+bedrock_ore_sulfuric_roasted_heavyx1/10
            -+bedrock_ore_sulfuric_byproduct_lightx1>vitriol50+bedrock_ore_sulfuric_roasted_lightx1/10
            -+bedrock_ore_sulfuric_byproduct_nonmetalx1>vitriol50+bedrock_ore_sulfuric_roasted_nonmetalx1/10
            -+bedrock_ore_sulfuric_byproduct_rarex1>vitriol50+bedrock_ore_sulfuric_roasted_rarex1/10
            -+biomassx4>syngas1000+charcoalx1/100
            aromatics1500+->-+solid_fuelx1/60
            balefire46+->-+solid_fuel_bfx1/60
            biofuel1400+->-+solid_fuelx1/60
            diesel1300+->-+solid_fuelx1/60
            diesel_crack1300+->-+solid_fuelx1/60
            diesel_crack_reform560+->-+solid_fuelx1/60
            diesel_reform520+->-+solid_fuelx1/60
            gas12000+->hydrogen8000+ingot_graphitex1/60
            gas_coker4000+->reformgas100+-/60
            heatingoil1800+->-+solid_fuelx1/60
            heatingoil_vacuum540+->-+solid_fuelx1/60
            heavyoil500+#c:cokesx1>coalgas1000+-/50
            heavyoil500+#c:dusts/coalx1>coalgas1000+-/50
            heavyoil500+#c:gems/coalx1>coalgas1000+-/50
            hydrogen500+#c:dusts/coalx1>heavyoil1000+-/100
            hydrogen500+#c:gems/coalx1>heavyoil1000+-/100
            kerosene280+->-+solid_fuelx1/60
            kerosene_reform110+->-+solid_fuelx1/60
            lightoil490+->-+solid_fuelx1/60
            lightoil_crack780+->-+solid_fuelx1/60
            lightoil_vacuum240+->-+solid_fuelx1/60
            lpg390+->-+solid_fuelx1/60
            naphtha6500+->-+solid_fuelx1/60
            naphtha_crack8300+->-+solid_fuelx1/60
            petroil5500+->-+solid_fuelx1/60
            petroleum430+->-+solid_fuelx1/60
            reclaimed6300+->-+solid_fuelx1/60
            reformate300+->-+solid_fuelx1/60
            reformgas68+->-+solid_fuelx1/60
            smear8700+->-+solid_fuelx1/60
            sourgas2800+->-+solid_fuelx1/60
            steam250+#c:cokesx1>syngas1000+-/100
            steam500+#c:dusts/coalx1>syngas1000+-/100
            steam500+#c:gems/coalx1>syngas1000+-/100
            syngas2000+#c:dusts/tungstenx1>spentsteam1000+ingot_tungsten_carbidex1/300
            syngas430+->-+solid_fuelx1/60
            titanium_tetrachloride1000+->chlorine125+ingot_titaniumx5/60
            unsaturateds190+->-+solid_fuelx1/60
            xylene220+->-+solid_fuelx1/60
            """);
        EXPECTED.put("radiolysis", """
            biogas:petroleum20,aromatics20
            bitumen:oil80,aromatics20
            diesel:kerosene40,petroleum30
            diesel_crack:kerosene40,petroleum30
            gas:petroleum30,unsaturateds20
            heatingoil_vacuum:heatingoil80,reformgas20
            kerosene:petroleum60,none0
            oil:crackoil80,petroleum20
            reformate:unsaturateds40,reformgas60
            smear:naphtha60,petroleum40
            water:peroxide80,hydrogen20
            woodoil:heatingoil40,aromatics10
            xylene:aromatics80,petroleum20
            """);
        EXPECTED.put("refinery", """
            hotcrackoil:naphtha_crack40,lightoil_crack30,aromatics15,unsaturateds15+oil_tar_crackx1
            hotcrackoil_ds:naphtha_ds35,lightoil_ds35,aromatics15,unsaturateds15+oil_tar_paraffinx1
            hotoil:heavyoil50,naphtha25,lightoil15,petroleum10+sulfurx1
            hotoil_ds:heavyoil30,naphtha_ds35,lightoil_ds20,unsaturateds15+oil_tar_paraffinx1
            """);
        EXPECTED.put("vacuum_distill", """
            oil:heavyoil_vacuum40,reformate25,lightoil_vacuum20,sourgas15
            oil_ds:heavyoil_vacuum40,reformate25,lightoil_vacuum20,reformgas15
            """);
        EXPECTED.put("reforming", """
            cholesterol:estradiol50,reformgas35,hydrogen15
            heatingoil:naphtha50,petroleum15,hydrogen10
            lightoil:aromatics50,reformgas10,hydrogen15
            lightoil_crack:aromatics50,reformgas5,hydrogen20
            naphtha:reformate50,petroleum15,hydrogen10
            naphtha_coker:reformate50,reformgas10,hydrogen5
            naphtha_crack:reformate50,aromatics10,hydrogen5
            petroleum:unsaturateds85,reformgas10,hydrogen5
            sourgas:sulfuric_acid75,petroleum10,hydrogen15
            """);
        EXPECTED.put("solidification", """
            aromatics:3900>solid_fuelx1
            balefire:110>solid_fuel_bfx1
            biofuel:3600>solid_fuelx1
            biogas:250>biomass_compressedx4
            bitumen:100>oil_tar_crudex1
            coalcreosote:200>oil_tar_coalx1
            coaloil:200>oil_tar_coalx1
            crackoil:200>oil_tar_crackx1
            diesel:3200>solid_fuelx1
            diesel_crack:3400>solid_fuelx1
            diesel_crack_reform:1400>solid_fuelx1
            diesel_reform:1300>solid_fuelx1
            enderjuice:100>ender_pearlx1
            heatingoil:4600>solid_fuelx1
            heatingoil_vacuum:1300>solid_fuelx1
            heavyoil:150>oil_tar_crudex1
            heavyoil_vacuum:150>oil_tar_crudex1
            kerosene:700>solid_fuelx1
            kerosene_reform:280>solid_fuelx1
            lava:1000>obsidianx1
            lead:100>ingot_leadx1
            lightoil:1200>solid_fuelx1
            lightoil_crack:1900>solid_fuelx1
            lightoil_vacuum:600>solid_fuelx1
            lpg:990>solid_fuelx1
            lubricant:100>oil_tar_paraffinx1
            mercury:125>nugget_mercuryx1
            naphtha:16000>solid_fuelx1
            naphtha_crack:20000>solid_fuelx1
            oil:200>oil_tar_crudex1
            petroil:13000>solid_fuelx1
            petroleum:1000>solid_fuelx1
            reclaimed:15000>solid_fuelx1
            redmud:450>iron_ingotx1
            reformate:750>solid_fuelx1
            reformgas:170>solid_fuelx1
            salient:1280>bio_waferx8
            slop:250>ore_oil_sandx1
            smear:21000>solid_fuelx1
            sodium:100>powder_sodiumx1
            sourgas:7200>solid_fuelx1
            syngas:1000>solid_fuelx1
            unsaturateds:490>solid_fuelx1
            water:1000>icex1
            watz:1000>ingot_mudx1
            woodoil:1000>oil_tar_woodx1
            xylene:570>solid_fuelx1
            """);
        // 1.7 TileEntityStorageDrum indexes short waste with ItemWasteLong.rectify (meta % 5), so Pu-241, Am-242 and
        // Sa-326 release U-235's, U-233's and Np-237's liquid/gas.
        EXPECTED.put("storage_drum", """
            ingot_au198x1>nugget_mercuryx1/900,0,0
            ingot_pb209x1>ingot_bismuthx1/1800,0,0
            nuclear_waste_long_neptuniumx1>nuclear_waste_long_depleted_neptuniumx1/216000,0,100
            nuclear_waste_long_schrabidiumx1>nuclear_waste_long_depleted_schrabidiumx1/216000,0,250
            nuclear_waste_long_thoriumx1>nuclear_waste_long_depleted_thoriumx1/216000,0,0
            nuclear_waste_long_tiny_neptuniumx1>nuclear_waste_long_depleted_tiny_neptuniumx1/21600,0,10
            nuclear_waste_long_tiny_schrabidiumx1>nuclear_waste_long_depleted_tiny_schrabidiumx1/21600,0,25
            nuclear_waste_long_tiny_thoriumx1>nuclear_waste_long_depleted_tiny_thoriumx1/21600,0,0
            nuclear_waste_long_tiny_uranium233x1>nuclear_waste_long_depleted_tiny_uranium233x1/21600,0,5
            nuclear_waste_long_tiny_uranium235x1>nuclear_waste_long_depleted_tiny_uranium235x1/21600,0,0
            nuclear_waste_long_uranium233x1>nuclear_waste_long_depleted_uranium233x1/216000,0,50
            nuclear_waste_long_uranium235x1>nuclear_waste_long_depleted_uranium235x1/216000,0,0
            nuclear_waste_short_americium242x1>nuclear_waste_short_depleted_americium242x1/18000,50,100
            nuclear_waste_short_neptuniumx1>nuclear_waste_short_depleted_neptuniumx1/18000,150,500
            nuclear_waste_short_plutonium239x1>nuclear_waste_short_depleted_plutonium239x1/18000,250,1000
            nuclear_waste_short_plutonium240x1>nuclear_waste_short_depleted_plutonium240x1/18000,350,1000
            nuclear_waste_short_plutonium241x1>nuclear_waste_short_depleted_plutonium241x1/18000,0,100
            nuclear_waste_short_schrabidiumx1>nuclear_waste_short_depleted_schrabidiumx1/18000,150,500
            nuclear_waste_short_tiny_americium242x1>nuclear_waste_short_depleted_tiny_americium242x1/1800,5,10
            nuclear_waste_short_tiny_neptuniumx1>nuclear_waste_short_depleted_tiny_neptuniumx1/1800,15,50
            nuclear_waste_short_tiny_plutonium239x1>nuclear_waste_short_depleted_tiny_plutonium239x1/1800,25,100
            nuclear_waste_short_tiny_plutonium240x1>nuclear_waste_short_depleted_tiny_plutonium240x1/1800,35,100
            nuclear_waste_short_tiny_plutonium241x1>nuclear_waste_short_depleted_tiny_plutonium241x1/1800,0,10
            nuclear_waste_short_tiny_schrabidiumx1>nuclear_waste_short_depleted_tiny_schrabidiumx1/1800,15,50
            nuclear_waste_short_tiny_uranium233x1>nuclear_waste_short_depleted_tiny_uranium233x1/1800,5,10
            nuclear_waste_short_tiny_uranium235x1>nuclear_waste_short_depleted_tiny_uranium235x1/1800,0,10
            nuclear_waste_short_uranium233x1>nuclear_waste_short_depleted_uranium233x1/18000,50,100
            nuclear_waste_short_uranium235x1>nuclear_waste_short_depleted_uranium235x1/18000,0,100
            nugget_au198x1>nugget_mercury_tinyx1/180,0,0
            nugget_pb209x1>nugget_bismuthx1/360,0,0
            nugget_sr90x1>nugget_zirconiumx1/360,0,0
            powder_sr90x1>powder_zirconiumx1/1800,0,0
            """);
        EXPECTED.put("gas_centrifuge", """
            HEUF6:300>0 NONE/hs[nugget_u238x2, nugget_u235x1, fluoritex1]
            LEUF6:300>200 MEUF6[nugget_u238x1, fluoritex1]
            MEUF6:200>100 HEUF6[nugget_u238x1]
            MUD:1000>500 MUD_HEAVY[powder_leadx1, dustx1]
            MUD_HEAVY:500>0 NONE[powder_ironx1, dustx1, nuclear_waste_tinyx1]
            NUF6:400>300 LEUF6[nugget_u238x1]
            PF6:300>0 NONE[nugget_pu238x1, nugget_pu_mixx2, fluoritex1]
            from puf6=PF6
            from uf6=NUF6
            from watz=MUD
            puf6900>[nugget_pu238x3, nugget_pu_mixx6, fluoritex3]/1
            uf61200>[nugget_u238x11, nugget_u235x1, fluoritex4]/hs/4
            uf61200>[nugget_u238x6, nugget_uranium_fuelx6, fluoritex4]/2
            watz1000>[powder_ironx1, powder_leadx1, nuclear_waste_tinyx1, dustx2]/2
            """);
        EXPECTED.put("cooling", """
            hotsteam=hotsteam:1>steam10/2
            steam=steam:100>spentsteam1/200
            superhotsteam=superhotsteam:1>hotsteam10/18
            ultrahotsteam=ultrahotsteam:1>superhotsteam10/120
            """);
        EXPECTED.put("heat", """
            air:5,1>airblast1
            crackoil:10,1>hotcrackoil1
            crackoil_ds:10,1>hotcrackoil_ds1
            oil:10,1>hotoil1
            oil_ds:10,1>hotoil_ds1
            reactor coolant:300,1>coolant_hot1
            water:200,1>steam100
            """);
        EXPECTED.put("combustion", """
            aromatics:458000,-
            balefire:256000000,HIGH640000000
            biofuel:500000,HIGH1250000
            biogas:62500,GAS78100
            coalcreosote:250000,-
            coalgas:6350000,MEDIUM9520000
            coalgas_leaded:9520000,MEDIUM14200000
            coaloil:1100000,-
            crackoil:31200,-
            crackoil_ds:62500,-
            deuterium:5000,HIGH10000
            diesel:550000,HIGH1370000
            diesel_crack:515000,HIGH1280000
            diesel_crack_reform:1280000,HIGH3210000
            diesel_reform:1370000,HIGH3430000
            ethanol:275000,HIGH687000
            fishoil:75000,-
            flue:25000,-
            gas:50000,GAS75000
            gas_coker:62500,GAS93700
            gasoline:605000,HIGH1510000
            gasoline_leaded:907000,HIGH2260000
            heatingoil:391000,LOW489000
            heatingoil_vacuum:1310000,LOW1640000
            heavyoil:55000,LOW68700
            heavyoil_vacuum:187000,LOW234000
            hydrogen:5000,HIGH10000
            kerosene:2560000,AERO3850000
            kerosene_reform:6400000,AERO9600000
            lightoil:1460000,MEDIUM2200000
            lightoil_crack:916000,MEDIUM1370000
            lightoil_ds:2930000,MEDIUM4400000
            lightoil_vacuum:3000000,MEDIUM4500000
            lpg:1810000,HIGH4530000
            naphtha:110000,MEDIUM165000
            naphtha_coker:125000,MEDIUM187000
            naphtha_crack:85900,MEDIUM128000
            naphtha_ds:220000,MEDIUM330000
            nitan:64000000,HIGH160000000
            oil:25000,-
            oil_coker:31200,-
            oil_ds:50000,-
            oxyhydrogen:5000,GAS15000
            petroil:130000,MEDIUM195000
            petroil_leaded:195000,MEDIUM293000
            petroleum:1650000,GAS2470000
            radiosolvent:150000,-
            reclaimed:113000,LOW141000
            redmud:1000,-
            reformate:2400000,HIGH6000000
            reformgas:10500000,GAS15700000
            smear:82500,LOW103000
            solvent:100000,-
            sourgas:250000,-
            sunfloweroil:50000,-
            syngas:1650000,GAS2060000
            tritium:5000,HIGH10000
            unsaturateds:3660000,-
            woodoil:110000,-
            xylene:3150000,HIGH7870000
            """);
    }

    /** Each table read from its data equals the former Java table, row for row. */
    public static void values(GameTestHelper helper) {
        List<String> broken = new ArrayList<>();
        for (String table : TABLES) {
            TreeSet<String> want = new TreeSet<>();
            for (String line : EXPECTED.get(table).lines().toList()) if (!line.isBlank()) want.add(line.strip());
            TreeSet<String> have = describe(table);
            if (!want.equals(have)) {
                TreeSet<String> missing = new TreeSet<>(want);
                missing.removeAll(have);
                TreeSet<String> extra = new TreeSet<>(have);
                extra.removeAll(want);
                broken.add(table + ": missing " + missing + ", unexpected " + extra);
            }
        }
        helper.assertTrue(broken.isEmpty(), "fluid tables differ from 1.7: " + broken);
        helper.succeed();
    }

    /** Every loaded row of every table survives its own file format (encode, decode, encode gives the same JSON). */
    public static void codecs(GameTestHelper helper) {
        List<String> broken = new ArrayList<>();
        roundTrip(helper, CokerRecipes.TABLE.table, broken);
        roundTrip(helper, CompressorRecipes.TABLE, broken);
        roundTrip(helper, CrackingRecipes.TABLE.table, broken);
        roundTrip(helper, ElectrolyserFluidRecipes.TABLE.table, broken);
        roundTrip(helper, GasCentrifugeRecipes.DISPLAY, broken);
        roundTrip(helper, GasCentrifugeRecipes.PSEUDO_FLUIDS, broken);
        roundTrip(helper, HydrotreatingRecipes.TABLE.table, broken);
        roundTrip(helper, LiquefactionRecipes.TABLE, broken);
        roundTrip(helper, PyroOvenRecipes.TABLE, broken);
        roundTrip(helper, RadiolysisRecipes.TABLE.table, broken);
        roundTrip(helper, RefineryRecipes.TABLE.table, broken);
        roundTrip(helper, RefineryRecipes.VACUUM.table, broken);
        roundTrip(helper, ReformingRecipes.TABLE.table, broken);
        roundTrip(helper, SolidificationRecipes.TABLE.table, broken);
        roundTrip(helper, StorageDrumRecipes.TABLE, broken);
        helper.assertTrue(broken.isEmpty(), "rows that do not survive their JSON: " + broken);
        helper.succeed();
    }

    private static <R> void roundTrip(GameTestHelper helper, DataRecipeTable<R> table,
                                      List<String> broken) {
        var registries = helper.getLevel().registryAccess();
        List<R> rows = table.rows();
        if (rows.isEmpty()) broken.add(table.table.id() + " has no rows");
        for (R row : rows) {
            try {
                var json = table.table.toJson(row, registries);
                var again = table.table.toJson(table.table.fromJson(json, registries), registries);
                if (!json.equals(again)) broken.add(table.table.id() + ": " + json);
            } catch (RuntimeException e) {
                broken.add(table.table.id() + ": " + e.getMessage());
            }
        }
    }

    /** 1.7 TileEntityMachineTurbineGas.fuelMaxCons, a fluid value map; 5 mB/t for any other fuel. */
    public static void gasTurbine(GameTestHelper helper) {
        var map = FluidDataMaps.GAS_TURBINE_FUEL;
        helper.assertTrue(Double.valueOf(50D).equals(map.get(Fluids.GAS.getFluid()))
                        && Double.valueOf(10D).equals(map.get(Fluids.SYNGAS.getFluid()))
                        && Double.valueOf(100D).equals(map.get(Fluids.OXYHYDROGEN.getFluid()))
                        && Double.valueOf(5D).equals(map.get(Fluids.REFORMGAS.getFluid()))
                        && map.get(Fluids.PETROLEUM.getFluid()) == null && map.byId().size() == 4,
                "gas turbine fuel consumption differs from 1.7: " + map.byId());
        helper.succeed();
    }
}
