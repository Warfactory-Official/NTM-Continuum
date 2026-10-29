package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.data.MachineDataMaps;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.FluidContainerStack;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.LemegetonRecipes;
import com.hbm.inventory.recipes.MagicRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.RecipeIngredients;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.mojang.serialization.JsonOps;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ItemStacks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The item machine tables as datapack tables and data maps (M2, batch R-C2): each table, read through the API its
 * machines use, describes every row as one line; the lines must equal those the former Java tables gave (recorded
 * before the conversion as a SHA-256 of the joined lines, with the row count and sample lines spelled out).
 */
public final class ItemRecipeTableGameTests {

    private ItemRecipeTableGameTests() {
    }

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    /**
     * Only with HBM_DUMP_TABLES=<dir>: writes every table's lines to <dir>/<table>.txt (how the hashes were taken).
     */
    public static final GameTests.Function DUMP = System.getenv("HBM_DUMP_TABLES") == null ? null
            : TESTS.function("recipe_table_c2_dump", 100, () -> ItemRecipeTableGameTests::dump);

    public static final GameTests.Function ROWS =
            TESTS.function("recipe_table_item_tables_equal_17", 100, () -> ItemRecipeTableGameTests::rows);
    public static final GameTests.Function ROUND_TRIP =
            TESTS.function("recipe_table_item_tables_round_trip", 100, () -> ItemRecipeTableGameTests::roundTrip);
    public static final GameTests.Function CONFIG =
            TESTS.function("recipe_table_item_tables_config_rows", 100, () -> ItemRecipeTableGameTests::configRows);
    public static final GameTests.Function MACHINE_VALUES =
            TESTS.function("data_map_machine_values_equal_17", 20, () -> ItemRecipeTableGameTests::machineValues);
    public static final GameTests.Function SAWMILL =
            TESTS.function("recipe_table_sawmill_rows", 20, () -> ItemRecipeTableGameTests::sawmill);

    public static void init() {
    }

    /**
     * Each table's line count and SHA-256 of its joined lines, taken from the Java tables before they
     * became files (HBM_DUMP_TABLES; the annihilator in 528 mode, every other table in the default config).
     * ammo_press, anvil_construction, arc_welder and lemegeton use 1.7 ore names as tags and KEY_SAND as c:sands.
     * anvil_construction and anvil_smithing name the vanilla copper ingot (HBM has no copper ingot of its own).
     * Material, wire, shell and pipe rows follow 1.7's Mats.orderedList declaration order.
     */
    static final Map<String, String> EXPECTED = Map.ofEntries(
            Map.entry("ammo_press", "90 f3b1dc99e5c2d40f1a32b812ecbea4bc44e7075459ad59ddc2b77734e84c3f0f"),
            Map.entry("annihilator", "16 a2044a6681862dd49d8faba5337d651bb5a51172410b85b2082c32f3daab89cf"),
            Map.entry("anvil_construction", "257 af43b890b355cd087f8db0da946891c2c89c7d3bd4098d91a63cefd733efbfdf"),
            Map.entry("anvil_smithing", "58 5655dfc0e8f89caeec8e0bfc8cd0ce463ef4ea6f218c31f082db49fa3d9076f4"),
            Map.entry("arc_welder", "42 2f80a4b8cd70720cb08d1b0f07623a727ad6f944c17380a68fdf24df65ccda48"),
            Map.entry("breeder", "31 daa249b6c8178775c166e81ac9ef0726914016ea50786592d7e65ce0586ea789"),
            Map.entry("combination", "53 8912926677d5f8a3a57d79ac5d94149055293364f004d6ebeade26aaeceae866"),
            Map.entry("custom_nuke", "55 2f953f6da8a523726534778e7f934622ddb72ddc2ba4898a7b6a6383f64688e7"),
            Map.entry("icf_fuel", "13 e60e1faea2e17ebe0dcfddf98c482f8771056413c585b98af50423ea9b23d244"),
            Map.entry("lemegeton", "37 c755ff48b6584d35100beca981f335ee2e2f1fcbb7c9716de4c20bd496c7c577"),
            Map.entry("magic", "7 a1cb39a71866df9f0e9a269b768396dee4ff2fce1e595fb138e8fbfb403b7ea9"),
            Map.entry("particle_accelerator", "11 d98136d6bd98588759ffc21226705c1bf86b677a8180f8d1227aef53fa3dc7dc"),
            Map.entry("pedestal", "19 dce1c166674ae43b8121d3653f2fedfdf27f33ade9bf77648c8feecbe0efa545"),
            Map.entry("press", "48 4a487f4c553e2065372c439d6e4bae231584bdb55ddce16eb3b816fede56d2f8"),
            Map.entry("soldering", "30 3b20f88748496a0470a5407775dd85bb5244aeae8a1406aaf98a4ac23e11b15c"));

    /**
     * A few of the recorded lines, spelled out so a reader sees what the hashes cover.
     */
    static final Map<String, String> SAMPLES = Map.of(
            "press", "FLAT #c:dusts/coalx1 => hbm:briquette_coalx1",
            "ammo_press",
            "hbm:ammo_standard_m357_bpx16 <= - #c:ingots/leadx2 - - minecraft:gunpowderx1 - - hbm:casing_smallx1 -",
            "breeder", "hbm:rod_dual_lithium => hbm:rod_dual_tritiumx1 flux=400",
            "combination", "#c:gems/coalx1 => hbm:coke_coalx1 + coalcreosotex100@0",
            "custom_nuke", "cell aschrab => SCHRAB ADD 15.0",
            "anvil_construction", "t=3..-1 SMITHING in=#c:ingots/ironx1, out=hbm:plate_ironx1%1.0,",
            "magic", "hbm:rod_of_discord <= minecraft:ender_pearl minecraft:blaze_rod hbm:nugget_euphemium");

    /**
     * Every item table equals the Java table it replaced, line for line (count, hash, samples).
     */
    public static void rows(GameTestHelper helper) {
        registries = helper.getLevel().registryAccess();
        List<String> wrong = new ArrayList<>();
        Map<String, Supplier<List<String>>> tables = new TreeMap<>(tables());
        tables.put("annihilator", () -> {
            boolean was528 = GeneralConfig.enable528;
            GeneralConfig.enable528 = true;
            try {
                rebuild(AnnihilatorRecipes.TABLE);
                return annihilator528();
            } finally {
                GeneralConfig.enable528 = was528;
                rebuild(AnnihilatorRecipes.TABLE);
            }
        });
        for (var table : tables.entrySet()) {
            List<String> lines = table.getValue().get();
            String got = lines.size() + " " + sha256(lines);
            String sample = SAMPLES.get(table.getKey());
            if (!got.equals(EXPECTED.get(table.getKey())) || sample != null && !lines.contains(sample)) {
                wrong.add(table.getKey() + " (" + lines.size() + " lines)");
                try {
                    Files.writeString(Path.of("item-table-" + table.getKey() + ".txt"), String.join("\n", lines) + "\n",
                            StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    // the failure message names the table either way
                }
            }
        }
        helper.assertTrue(wrong.isEmpty(), "item tables differ from the former Java tables (lines written to the run "
                + "directory as item-table-<name>.txt): " + wrong);
        helper.succeed();
    }

    /**
     * Every row of every item table survives its JSON (the file form and the client sync form).
     */
    public static void roundTrip(GameTestHelper helper) {
        List<String> broken = new ArrayList<>();
        HolderLookup.Provider access = helper.getLevel().registryAccess();
        for (var table : List.<DataRecipeTable<?>>of(
                AmmoPressRecipes.TABLE, AnnihilatorRecipes.TABLE,
                ArcWelderRecipes.TABLE, BreederRecipes.TABLE,
                CombinationRecipes.TABLE, LemegetonRecipes.TABLE,
                MagicRecipes.TABLE, ParticleAcceleratorRecipes.TABLE,
                PedestalRecipes.TABLE, PressRecipes.TABLE,
                SawmillRecipes.TABLE, SolderingRecipes.TABLE,
                AnvilRecipes.SMITHING, AnvilRecipes.CONSTRUCTION)) {
            roundTrip(table, access, broken);
        }
        helper.assertTrue(broken.isEmpty(), "rows that do not survive their JSON: " + broken);
        helper.succeed();
    }

    private static <R> void roundTrip(DataRecipeTable<R> table, HolderLookup.Provider access,
                                      List<String> broken) {
        for (var entry : table.entries()) {
            var json = table.table.toJson(entry.value(), access);
            if (!table.table.toJson(table.table.fromJson(json, access), access).equals(json)) broken.add(entry.id());
        }
    }

    /**
     * The rows inside 1.7's config branches follow the config when the table rebuilds: 528 mode drops the soldered
     * control units and tier II/III upgrades (13 rows) and the RBMK/research reactor dismantling (5), and gives the
     * annihilator its 15 milestones; expensive mode swaps the assembly machine's vacuum tubes for analog circuits.
     */
    public static void configRows(GameTestHelper helper) {
        var soldering = SolderingRecipes.TABLE;
        var construction = AnvilRecipes.CONSTRUCTION;
        var annihilator = AnnihilatorRecipes.TABLE;
        int solderDefault = SolderingRecipes.recipes.size();
        int anvilDefault = AnvilRecipes.getConstruction().size();
        int annihilatorDefault = AnnihilatorRecipes.recipes().size();
        boolean was528 = GeneralConfig.enable528;
        boolean wasExpensive = GeneralConfig.enableExpensiveMode;
        int solder528;
        int anvil528;
        int annihilator528;
        boolean analog;
        try {
            GeneralConfig.enable528 = true;
            GeneralConfig.enableExpensiveMode = true;
            rebuild(soldering);
            rebuild(construction);
            rebuild(annihilator);
            solder528 = SolderingRecipes.recipes.size();
            anvil528 = AnvilRecipes.getConstruction().size();
            annihilator528 = AnnihilatorRecipes.recipes().size();
            analog = AnvilRecipes.getConstruction().stream()
                                 .anyMatch(recipe -> id(recipe.output.get(0).stack.getItem()).equals(
                                         "hbm:machine_assembly_machine")
                                         && recipe.input.stream()
                                                        .anyMatch(in -> stack(in).equals("hbm:circuit_analogx2")));
        } finally {
            GeneralConfig.enable528 = was528;
            GeneralConfig.enableExpensiveMode = wasExpensive;
            rebuild(soldering);
            rebuild(construction);
            rebuild(annihilator);
        }
        helper.assertTrue(solderDefault == 27 && solder528 == 14,
                "soldering rows default/528: " + solderDefault + "/" + solder528);
        helper.assertTrue(anvilDefault == 257 && anvil528 == 252 && analog,
                "anvil construction rows default/528+expensive: "
                        + anvilDefault + "/" + anvil528 + ", analog assembly machine " + analog);
        helper.assertTrue(annihilatorDefault == 0 && annihilator528 == 16, "annihilator keys default/528: "
                + annihilatorDefault + "/" + annihilator528);
        helper.assertTrue(SolderingRecipes.recipes.size() == 27
                && AnvilRecipes.getConstruction().size() == 257, "default rows not restored");
        helper.succeed();
    }

    /**
     * The radiation generator and research reactor values (1.7 TileEntityMachineRadGen fuels, TileEntityReactorResearch.fuelMap).
     */
    public static void machineValues(GameTestHelper helper) {
        var radgen = MachineDataMaps.RADGEN_FUEL;
        var scrap = radgen.get(ModItems.scrap_nuclear.get());
        var gem = radgen.get(ModItems.gem_rad.get());
        helper.assertTrue(
                scrap != null && scrap.production() == 50 && scrap.duration() == 5 * 60 * 20 && scrap.output().isEmpty()
                        && gem != null && gem.production() == 25_000 && gem.duration() == 30 * 60 * 20
                        && gem.output().equals(Optional.of("minecraft:diamond")) && gem.count() == 1
                        && radgen.byId().size() == 2,
                "radiation generator fuels differ from 1.7: " + radgen.byId());
        List<String> plates = List.of("u233", "u235", "mox", "pu239", "sa326", "ra226be", "pu238be");
        for (String plate : plates) {
            Item fresh = BuiltInRegistries.ITEM.getValue(Ids.of("hbm", "plate_fuel_" + plate));
            ItemStack waste = TileEntityReactorResearch.wasteOf(fresh);
            ItemStack hot = ItemDepletedFuel.makeHot(
                    BuiltInRegistries.ITEM.getValue(Ids.of("hbm", "waste_plate_" + plate)), 1);
            helper.assertTrue(waste != null && ItemStacks.sameItemAndData(waste, hot) && waste.getCount() == 1,
                    "research reactor plate_fuel_" + plate + " must leave a hot waste_plate_" + plate + ": " + waste);
        }
        helper.assertTrue(MachineDataMaps.RESEARCH_REACTOR_FUEL.byId().size() == 7,
                "research reactor fuels: " + MachineDataMaps.RESEARCH_REACTOR_FUEL.byId().keySet());
        helper.succeed();
    }

    /**
     * The sawmill rows give 1.7's outputs and sawdust chances (1.7 TileEntitySawmill.getOutput / sawdustChance).
     */
    public static void sawmill(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack planks = SawmillRecipes.getOutput(level, new ItemStack(Items.BIRCH_LOG));
        helper.assertTrue(planks.is(Items.BIRCH_PLANKS) && planks.getCount() == 6, "birch log: " + planks);
        helper.assertTrue(SawmillRecipes.sawdustChance(new ItemStack(Items.BIRCH_LOG)) == 0.5F
                        && SawmillRecipes.sawdustChance(new ItemStack(Items.OAK_PLANKS)) == 0.1F
                        && SawmillRecipes.sawdustChance(new ItemStack(Items.OAK_SAPLING)) == 0.1F
                        && SawmillRecipes.sawdustChance(new ItemStack(Items.STICK)) == 0F,
                "sawdust chances differ from 1.7");
        helper.assertTrue(SawmillRecipes.getOutput(level, new ItemStack(Items.OAK_SAPLING))
                                        .is(Items.STICK)
                        && SawmillRecipes.getOutput(level, new ItemStack(Items.STICK))
                                         .is(ModItems.powder_sawdust.get())
                        && SawmillRecipes.getOutput(level, new ItemStack(Items.DIRT)).isEmpty()
                        && SawmillRecipes.getRecipes().size() == 4,
                "sawmill outputs differ from 1.7");
        helper.succeed();
    }

    /**
     * Rebuilds a table from the current recipes, as a reload would (its config-dependent rows read the config again).
     */
    static void rebuild(DataRecipeTable<?> table) {
        replace(table);
    }

    private static <R> void replace(DataRecipeTable<R> table) {
        table.table.replace(new ArrayList<>(table.entries()));
    }

    private static HolderLookup.Provider registries;

    // The tables, as lines

    static Map<String, Supplier<List<String>>> tables() {
        Map<String, Supplier<List<String>>> tables = new TreeMap<>();
        tables.put("ammo_press", ItemRecipeTableGameTests::ammoPress);
        tables.put("arc_welder", ItemRecipeTableGameTests::arcWelder);
        tables.put("breeder", ItemRecipeTableGameTests::breeder);
        tables.put("combination", ItemRecipeTableGameTests::combination);
        tables.put("lemegeton", ItemRecipeTableGameTests::lemegeton);
        tables.put("magic", ItemRecipeTableGameTests::magic);
        tables.put("particle_accelerator", ItemRecipeTableGameTests::particleAccelerator);
        tables.put("pedestal", ItemRecipeTableGameTests::pedestal);
        tables.put("press", ItemRecipeTableGameTests::press);
        tables.put("soldering", ItemRecipeTableGameTests::soldering);
        tables.put("anvil_smithing", ItemRecipeTableGameTests::anvilSmithing);
        tables.put("anvil_construction", ItemRecipeTableGameTests::anvilConstruction);
        tables.put("icf_fuel", ItemRecipeTableGameTests::icfFuel);
        tables.put("custom_nuke", ItemRecipeTableGameTests::customNuke);
        return tables;
    }

    static List<String> ammoPress() {
        List<String> lines = new ArrayList<>();
        for (var recipe : AmmoPressRecipes.getRecipes()) {
            StringBuilder line = new StringBuilder(stack(recipe.output)).append(" <=");
            for (AStack in : recipe.input) line.append(' ').append(stack(in));
            lines.add(line.toString());
        }
        return lines;
    }

    static List<String> arcWelder() {
        ArcWelderRecipes.register();
        List<String> lines = new ArrayList<>();
        for (var recipe : ArcWelderRecipes.recipes) {
            StringBuilder line = new StringBuilder(stack(recipe.output)).append(" d=").append(recipe.duration)
                                                                        .append(" p=").append(recipe.consumption)
                                                                        .append(" f=").append(fluid(recipe.fluid))
                                                                        .append(" <=");
            for (AStack in : recipe.ingredients) line.append(' ').append(stack(in));
            lines.add(line.toString());
        }
        return lines;
    }

    static List<String> breeder() {
        List<String> lines = new ArrayList<>();
        for (var entry : BreederRecipes.getAllRecipes().entrySet()) {
            lines.add(id(entry.getKey()) + " => " + stack(entry.getValue().output()) + " flux=" + entry.getValue()
                                                                                                       .flux());
        }
        return lines;
    }

    static List<String> combination() {
        List<String> lines = new ArrayList<>();
        for (var recipe : CombinationRecipes.getRecipes()) {
            lines.add(
                    stack(recipe.input()) + " => " + stack(recipe.itemOutput()) + " + " + fluid(recipe.fluidOutput()));
        }
        return lines;
    }

    static List<String> lemegeton() {
        List<String> lines = new ArrayList<>();
        for (var entry : LemegetonRecipes.recipes().entrySet()) {
            lines.add(entry.getKey() + " => " + entry.getValue());
        }
        return lines;
    }

    static List<String> magic() {
        List<String> lines = new ArrayList<>();
        for (var recipe : MagicRecipes.getRecipes()) {
            StringBuilder line = new StringBuilder(recipe.out).append(" <=");
            for (var in : recipe.in) line.append(' ').append(in.isTag() ? "#" : "").append(in.id());
            lines.add(line.toString());
        }
        return lines;
    }

    static List<String> particleAccelerator() {
        ParticleAcceleratorRecipes.register();
        List<String> lines = new ArrayList<>();
        for (var recipe : ParticleAcceleratorRecipes.recipes) {
            lines.add(stack(recipe.input1) + " + " + stack(recipe.input2) + " m=" + recipe.momentum + " => "
                    + stack(recipe.output1) + " + " + stack(recipe.output2));
        }
        return lines;
    }

    static List<String> pedestal() {
        List<String> lines = new ArrayList<>();
        for (var recipe : PedestalRecipes.recipes()) {
            StringBuilder line = new StringBuilder(recipe.output).append('x').append(recipe.outputCount)
                                                                 .append(" extra=").append(recipe.extra).append(" set=")
                                                                 .append(recipe.recipeSet).append(" <=");
            for (var in : recipe.input) line.append(' ').append(in == null ? "-" : in.id() + "x" + in.count());
            lines.add(line.toString());
        }
        for (int set = 0; set < PedestalRecipes.SETS; set++) {
            StringBuilder line = new StringBuilder("set ").append(set).append(':');
            for (var recipe : PedestalRecipes.recipeSet(set)) line.append(' ').append(recipe.output);
            lines.add(line.toString());
        }
        return lines;
    }

    static List<String> press() {
        List<String> lines = new ArrayList<>();
        for (var recipe : PressRecipes.getRecipes()) {
            lines.add(recipe.type() + " " + stack(recipe.input()) + " => " + stack(recipe.output()));
        }
        return lines;
    }

    static List<String> soldering() {
        SolderingRecipes.register();
        List<String> lines = new ArrayList<>();
        for (var recipe : SolderingRecipes.recipes) {
            lines.add(stack(recipe.output) + " d=" + recipe.duration + " p=" + recipe.consumption + " f=" + fluid(
                    recipe.fluid)
                    + " top=" + stacks(recipe.toppings) + " pcb=" + stacks(recipe.pcb) + " solder=" + stacks(
                    recipe.solder));
        }
        lines.add("toppings " + sortedStacks(SolderingRecipes.toppings));
        lines.add("pcb " + sortedStacks(SolderingRecipes.pcb));
        lines.add("solder " + sortedStacks(SolderingRecipes.solder));
        return lines;
    }

    static List<String> anvilSmithing() {
        List<String> lines = new ArrayList<>();
        for (var recipe : AnvilRecipes.getSmithing()) {
            String kind = recipe.getClass().getSimpleName();
            String sides;
            try {
                sides = displays(recipe.getLeft()) + " + " + displays(recipe.getRight());
            } catch (RuntimeException e) {
                sides = "?";
            }
            lines.add(kind + " t=" + recipe.tier + " " + stack(recipe.getSimpleOutput()) + " <= " + sides
                    + " use=" + recipe.amountConsumed(0, false) + "/" + recipe.amountConsumed(1, false));
        }
        return lines;
    }

    static List<String> anvilConstruction() {
        List<String> lines = new ArrayList<>();
        for (var recipe : AnvilRecipes.getConstruction()) {
            StringBuilder line = new StringBuilder("t=").append(recipe.tierLower).append("..").append(recipe.tierUpper)
                                                        .append(' ').append(recipe.getOverlay()).append(" in=");
            for (AStack in : recipe.input) line.append(stack(in)).append(',');
            line.append(" out=");
            for (var out : recipe.output) line.append(stack(out.stack)).append('%').append(out.chance).append(',');
            lines.add(line.toString());
        }
        return lines;
    }

    static List<String> icfFuel() {
        List<String> lines = new ArrayList<>();
        for (var entry : MachineDataMaps.ICF_FUEL_FLUID.byId().entrySet()) {
            lines.add("fluid " + fluidKey(entry.getKey()) + " => " + entry.getValue());
        }
        for (var entry : MachineDataMaps.icfMaterialFuels().entrySet()) {
            lines.add("material " + entry.getKey() + " => " + entry.getValue());
        }
        lines.sort(null);
        return lines;
    }

    static List<String> customNuke() {
        List<String> lines = new ArrayList<>();
        for (var entry : MachineDataMaps.CUSTOM_NUKE.byId().entrySet()) {
            var value = entry.getValue();
            lines.add("item " + entry.getKey() + " => " + value.type().name() + " " + value.op() + " " + value.value());
        }
        for (var entry : MachineDataMaps.CUSTOM_NUKE_CELL.byId().entrySet()) {
            var value = entry.getValue();
            lines.add("cell " + fluidKey(entry.getKey()) + " => " + value.type()
                                                                         .name() + " " + value.op() + " " + value.value());
        }
        lines.sort(null);
        return lines;
    }

    /**
     * An HBM fluid's key ({@code deuterium}) from its vanilla fluid id ({@code hbm:deuterium}).
     */
    static String fluidKey(String vanillaId) {
        return com.hbm.inventory.fluid.Fluids.fromVanilla(BuiltInRegistries.FLUID.getOptional(Ids.parse(vanillaId))
                                                                                 .orElse(net.minecraft.world.level.material.Fluids.EMPTY))
                                             .getKey();
    }

    /**
     * The annihilator's milestones exist in 528 mode only; read with the mode on.
     */
    static List<String> annihilator528() {
        List<String> lines = new ArrayList<>();
        for (var entry : AnnihilatorRecipes.recipes().entrySet()) {
            Object key = entry.getKey();
            String name = key instanceof Item item ? id(item)
                    : key instanceof FluidType fluid ? "fluid:" + fluid.getKey() : "#" + key;
            StringBuilder line = new StringBuilder(name).append(" =>");
            for (var milestone : entry.getValue().milestones)
                line.append(' ').append(milestone.amount()).append(':').append(stack(milestone.payout()));
            lines.add(line.toString());
        }
        lines.sort(null);
        return lines;
    }

    // Describing stacks

    /**
     * The item's id as 26.1 spells it (1.20.1's {@code grass} is {@code short_grass}), so the lines read the same on every node.
     */
    static String id(Item item) {
        return Ids.canonicalItemId(String.valueOf(BuiltInRegistries.ITEM.getKey(item)));
    }

    static String stack(ItemStack stack) {
        if (stack == null) return "null";
        if (stack.isEmpty()) return "empty";
        String base = id(stack.getItem()) + "x" + stack.getCount();
        if (ItemStacks.sameItemAndData(stack, new ItemStack(stack.getItem(), stack.getCount()))) return base;
        return base + data(stack);
    }

    private static String data(ItemStack stack) {
        if (registries == null) return "{data}";
        // the newest stack JSON on every node: 1.20.1's ItemStack codec writes {"Count", "tag"}, a boolean as a byte
        return ItemStacks.modernJson(stack, RegistryOps.create(JsonOps.INSTANCE, registries)).toString();
    }

    static String stack(AStack stack) {
        if (stack == null) return "-";
        String base;
        if (stack instanceof TagStack tag) {
            base = "#" + tag.tag.location();
        } else if (stack instanceof ComparableStack comparable) {
            base = id(comparable.item);
            if (comparable.exactData() || comparable instanceof RecipeIngredients.Exact) {
                ItemStack display = comparable.getDisplayStack();
                base += "!exact";
                if (!ItemStacks.sameItemAndData(display, new ItemStack(display.getItem(), display.getCount())))
                    base += data(display);
            }
        } else if (stack instanceof MaterialStack material) {
            base = "mat:" + material.material.name + "/" + material.shape;
        } else if (stack instanceof FluidContainerStack container) {
            base = container.toString();
        } else {
            // 1.7 wildcard / metadata ingredients (any of several items, the stirling engine with or without its gear)
            base = "any" + displays(stack.getDisplayStacks());
        }
        return base + "x" + stack.stacksize;
    }

    static String displays(List<ItemStack> stacks) {
        List<String> out = new ArrayList<>();
        for (ItemStack stack : stacks) out.add(stack(stack));
        return out.toString();
    }

    static String stacks(AStack[] stacks) {
        List<String> out = new ArrayList<>();
        for (AStack stack : stacks) out.add(stack(stack));
        return out.toString();
    }

    static String sortedStacks(Collection<AStack> stacks) {
        List<String> out = new ArrayList<>();
        for (AStack stack : stacks) out.add(stack(stack));
        out.sort(null);
        return out.toString();
    }

    static String fluid(FluidStack fluid) {
        return fluid == null ? "-" : fluid.type.getKey() + "x" + fluid.fill + "@" + fluid.pressure;
    }

    static String sha256(List<String> lines) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                                         .digest(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // The dump

    public static void dump(GameTestHelper helper) {
        registries = helper.getLevel().registryAccess();
        try {
            Path dir = Path.of(System.getenv("HBM_DUMP_TABLES"));
            Files.createDirectories(dir);
            for (var table : tables().entrySet()) {
                List<String> lines = table.getValue().get();
                Files.writeString(dir.resolve(table.getKey() + ".txt"), lines.size() + " " + sha256(lines) + "\n"
                        + String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
            }
            boolean was528 = GeneralConfig.enable528;
            GeneralConfig.enable528 = true;
            try {
                rebuild(AnnihilatorRecipes.TABLE);
                List<String> lines = annihilator528();
                Files.writeString(dir.resolve("annihilator.txt"), lines.size() + " " + sha256(lines) + "\n"
                        + String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
            } finally {
                GeneralConfig.enable528 = was528;
                rebuild(AnnihilatorRecipes.TABLE);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }
}
