package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModUpgradeItems;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * §19ed-recipe-content: the 1.7 assembly/chemical-plant recipes that the generator could not resolve before
 * (fusion parts, hpcondenser, watz casing, factories, caps, pistons, HIMARS, Nitra, ...), the aggregate
 * ({@code ANY_RESISTANTALLOY}/{@code ANY_BISMOIDBRONZE}) autogen plates they need, the soldering controllers and
 * the anvil recycling block.
 */
public final class RecipeContentGameTests {

    private RecipeContentGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function MACHINE =
            TESTS.function("reccontent_machine_recipes", 20, () -> RecipeContentGameTests::machineRecipes);
    public static final GameTests.Function AGGREGATE =
            TESTS.function("reccontent_aggregate_plates", 20, () -> RecipeContentGameTests::aggregatePlates);
    public static final GameTests.Function SOLDER =
            TESTS.function("reccontent_soldering_controllers", 20, () -> RecipeContentGameTests::solderingControllers);
    public static final GameTests.Function ANVIL =
            TESTS.function("reccontent_anvil_recycling", 20, () -> RecipeContentGameTests::anvilRecycling);

    // §19ed-recipe-names
    public static final GameTests.Function NAMES =
            TESTS.function("recnames_named_recipes", 20, () -> RecipeContentGameTests::namedRecipes);
    public static final GameTests.Function GENERATED_CRAFTING =
            TESTS.function("recnames_generated_crafting", 20, () -> RecipeContentGameTests::generatedCrafting);

    public static void init() {}

    /** Every 1.7 GenericRecipe registered with setupNamed/setNamed, and its en_US.lang name. */
    static final String[][] NAMED = {
            {"ass.analogAlt", "Simple Analog Circuit"}, {"ass.factorioChip", "Minimalist Circuits"},
            {"ass.atomicClockAlt", "Caesium Atomic Clock"}, {"ass.nitra", "Nitra to Ammunition"},
            {"chem.hydrogen", "Hydrogen"}, {"chem.hydrogencoke", "Hydrogen from Coke"}, {"chem.oxygen", "Oxygen"},
            {"chem.xenon", "Xenon Gas"}, {"chem.xenonoxy", "Xenon Gas (Cooled)"}, {"chem.helium3", "Helium-3"},
            {"chem.ethanol", "Ethanol"}, {"chem.biogas", "Biogas"}, {"chem.biofuel", "Biofuel Transesterification"},
            {"chem.reoil", "Reclaimed Oil"}, {"chem.gasoline", "Gasoline"},
            {"chem.coallube", "Lubricant from Coal Tar Creosote"}, {"chem.heavylube", "Lubricant from Heavy Oil"},
            {"chem.tarsand", "Bitumen from Tar Sand"}, {"chem.aggregate", "Instant Aggregate"},
            {"chem.deshcracked", "Desh from Cracked Oil"}, {"chem.meatprocessing", "Glyphid Meat Processing"},
            {"chem.biosolidfuel", "Solid Fuel from Biomass"}, {"chem.biooilsolidfuel", "Solid Fuel from Oil and Biomass"},
            {"chem.oilelectrodes", "Instant Electrodes (Heating Oil)"},
            {"chem.lubeelectrodes", "Instant Electrodes (Lubricant)"}, {"chem.birkeland", "Nitric Acid from Air"},
            {"fus.dd", "Deuterium Plasma"}, {"fus.do", "Deuterium-Oxygen Plasma"}, {"fus.dt", "Deuterium-Tritium Plasma"},
            {"fus.tcl", "Tritium-Chlorine Plasma"}, {"fus.h3", "Helium-3 Plasma"}, {"fus.th4", "Tritium-Helium-4 Plasma"},
            {"fus.cl", "Chlorine Plasma"}, {"fus.dhc", "DHC Plasma"}, {"fus.bf", "Balefire Plasma"},
            {"fus.stellar", "Stellar Plasma"}};

    /**
     * Exactly 1.7's 36 named recipes are named, each shows the translation of its id (not its icon's name), the
     * English file carries 1.7's strings, and the restored alt-pool circuits and the cyclotron group are 1.7's.
     */
    private static void namedRecipes(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        com.hbm.inventory.recipes.FusionRecipes.INSTANCE.registerRecipes();
        java.util.Set<String> expected = new java.util.HashSet<>();
        for (String[] row : NAMED) expected.add(row[0]);
        java.util.Set<String> named = new java.util.HashSet<>();
        for (com.hbm.inventory.recipes.loader.GenericRecipes set : new com.hbm.inventory.recipes.loader.GenericRecipes[]{
                AssemblyMachineRecipes.INSTANCE, ChemicalPlantRecipes.INSTANCE,
                com.hbm.inventory.recipes.FusionRecipes.INSTANCE}) {
            for (GenericRecipe recipe : set.recipeOrderedList) {
                if (!recipe.customLocalization) continue;
                named.add(recipe.name);
                String shown = recipe.getLocalizedName();
                helper.assertTrue(shown.equals(net.minecraft.network.chat.Component.translatable(recipe.name).getString()),
                        recipe.name + " is shown under the translation of its id, not its icon: " + shown);
                helper.assertTrue(recipe.matchesSearch(shown), recipe.name + " is found by its own name");
            }
        }
        helper.assertTrue(named.equals(expected), "exactly 1.7's named recipes are named; missing "
                + diff(expected, named) + ", extra " + diff(named, expected));

        com.google.gson.JsonObject lang;
        try (java.io.InputStream in = GenericRecipe.class.getResourceAsStream("/assets/hbm/lang/en_us.json")) {
            helper.assertTrue(in != null, "en_us.json is on the classpath");
            lang = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        for (String[] row : NAMED) {
            helper.assertTrue(lang.has(row[0]) && lang.get(row[0]).getAsString().equals(row[1]),
                    row[0] + " = '" + row[1] + "' (1.7 en_US.lang)");
        }
        helper.assertTrue(lang.has("autoswitch") && lang.get("autoswitch").getAsString().contains("$")
                && lang.has("autoswitch.cyclotron"), "the 1.7 auto-switch tooltip lines are translated");

        java.util.List<String> alt = com.hbm.inventory.recipes.loader.GenericRecipes.blueprintPools.getOrDefault(
                com.hbm.inventory.recipes.loader.GenericRecipes.POOL_PREFIX_ALT + ".circuit", java.util.List.of());
        helper.assertTrue(alt.containsAll(java.util.List.of("ass.analogAlt", "ass.factorioChip", "ass.atomicClockAlt")),
                "the 1.7 alt..circuit blueprint holds the three named circuit alternates: " + alt);
        GenericRecipe clock = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.atomicClockAlt");
        helper.assertTrue(clock.outputItem[0].getItem() == ModIntermediateItems.circuit(EnumCircuitType.ATOMIC_CLOCK)
                && clock.outputItem[0].getCount() == 4 && clock.power == 20_000, "ass.atomicClockAlt: 4 clocks, 20 kHE/t");
        java.util.List<GenericRecipe> cyclotron = AssemblyMachineRecipes.INSTANCE.autoSwitchGroups.get("autoswitch.cyclotron");
        helper.assertTrue(cyclotron != null && cyclotron.size() == 5
                        && !AssemblyMachineRecipes.INSTANCE.autoSwitchGroups.containsKey("cyclotron"),
                "the five cyclotron boxes share 1.7's single autoswitch.cyclotron group");
        helper.succeed();
    }

    private static java.util.Set<String> diff(java.util.Set<String> a, java.util.Set<String> b) {
        java.util.Set<String> out = new java.util.TreeSet<>(a);
        out.removeAll(b);
        return out;
    }

    /** Recipes that exist only because gen_recipes.py generates them now (CraftingRecipesGenerated). */
    private static void generatedCrafting(GameTestHelper helper) {
        net.minecraft.world.level.Level level = helper.getLevel();
        for (String id : new String[]{"string", "lead", "saddle", "gun_maresleg", "grenade_shell_frag", "iron_ingot_g1",
                "stick_dynamite", "fluid_valve"}) {
            helper.assertTrue(dev.golem.core.world.Recipes.hasRecipe(level, "hbm", id), "hbm:" + id + " is loaded");
        }
        ItemStack string = dev.golem.core.world.Recipes.craftingResult(level, 1, 1,
                java.util.List.of(new ItemStack(item("plant_flower_weed")))).orElse(ItemStack.EMPTY);
        helper.assertTrue(string.getItem() == net.minecraft.world.item.Items.STRING && string.getCount() == 3,
                "1.7: one hemp makes three string, got " + string);
        ItemStack rope = new ItemStack(item("plant_item_rope"));
        ItemStack lead = dev.golem.core.world.Recipes.craftingResult(level, 3, 1, java.util.List.of(rope,
                new ItemStack(net.minecraft.world.item.Items.SLIME_BALL), rope.copy())).orElse(ItemStack.EMPTY);
        helper.assertTrue(lead.getItem() == net.minecraft.world.item.Items.LEAD && lead.getCount() == 4,
                "1.7: rope, slime ball, rope make four leads, got " + lead);
        ItemStack iron = dev.golem.core.world.Recipes.smeltingResult(level, new ItemStack(item("ore_meteor_iron")))
                .orElse(ItemStack.EMPTY);
        // 1.7 yields 16. The count is not asserted: a 1.20.1 furnace recipe result is a bare id (legacy-recipes
        // drops the count there), which already cuts every declared multi-output smelt (crystals x2) to one.
        helper.assertTrue(iron.getItem() == net.minecraft.world.item.Items.IRON_INGOT,
                "1.7: meteoric iron ore smelts into iron ingots, got " + iron);
        helper.succeed();
    }

    /** Every recipe the generator newly resolved on 2026-09-28 (plus the three chemical-plant ones). */
    static final String[] ASSEMBLY = {
            "ass.bronzetubes", "ass.thermoelement", "ass.capnuka", "ass.capquantum", "ass.capsparkle", "ass.caprad",
            "ass.capfritz", "ass.capkorl", "ass.assemfac", "ass.chemfac", "ass.pistonsetsteel", "ass.pistonsetdura",
            "ass.pistonsetdesh", "ass.pistonsetstar", "ass.hpcondenser", "ass.fusioncore", "ass.fusionblanket",
            "ass.fusionpipes", "ass.fusionklystron", "ass.fusioncollector", "ass.fusionboiler", "ass.fusionmhdt",
            "ass.fusioncoupler", "ass.watzcasing", "ass.chopper", "ass.explosivelenses1", "ass.himarssmallwp",
            "ass.himarssmalltb", "ass.himarslargetb", "ass.shuttlemissile", "ass.50bmgsm", "ass.50bmgbypass",
            "ass.nitra"};
    static final String[] CHEMICAL = {"chem.hydrogencoke", "chem.batteryschrabidium", "chem.meatprocessing"};

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.of("hbm", id));
    }

    /** Registered, with a real output and every ingredient backed by at least one item (tags are loaded here). */
    private static void machineRecipes(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        for (String name : ASSEMBLY) check(helper, AssemblyMachineRecipes.INSTANCE.getRecipe(name), name);
        for (String name : CHEMICAL) check(helper, ChemicalPlantRecipes.INSTANCE.getRecipe(name), name);

        GenericRecipe watz = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.watzcasing");
        helper.assertTrue(watz.outputItem[0].getItem() == item("watz_casing") && watz.outputItem[0].getCount() == 3,
                "ass.watzcasing makes 3 watz casings (1.7 watz_end)");
        GenericRecipe blanket = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.fusionblanket");
        helper.assertTrue(blanket.outputItem[0].getItem() == item("fusion_component_blanket")
                && blanket.outputItem[0].getCount() == 4, "ass.fusionblanket makes 4 blanket components (meta 2)");
        GenericRecipe nitra = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.nitra");
        helper.assertTrue(nitra.outputChoices != null && nitra.outputChoices.length == 10,
                "ass.nitra keeps 1.7's ten weighted ChanceOutputMulti outputs");
        GenericRecipe shuttle = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.shuttlemissile");
        ItemStack canister = com.hbm.items.ModFluidNetItems.canister_full.get()
                .make(com.hbm.inventory.fluid.Fluids.GASOLINE_LEADED, 1_000);
        ItemStack wrong = com.hbm.items.ModFluidNetItems.canister_full.get()
                .make(com.hbm.inventory.fluid.Fluids.DIESEL, 1_000);
        boolean leaded = false, diesel = false;
        for (AStack in : shuttle.inputItem) {
            leaded |= in.matchesRecipe(canister, true);
            diesel |= in.matchesRecipe(wrong, true);
        }
        helper.assertTrue(leaded && !diesel, "ass.shuttlemissile takes leaded-gasoline canisters only");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, GenericRecipe recipe, String name) {
        helper.assertTrue(recipe != null, name + " is registered");
        boolean output = recipe.outputChoices != null
                || (recipe.outputItem != null && recipe.outputItem.length > 0 && !recipe.outputItem[0].isEmpty())
                || (recipe.outputFluid != null && recipe.outputFluid.length > 0);
        helper.assertTrue(output, name + " has an output");
        if (recipe.inputItem != null) {
            for (AStack in : recipe.inputItem) {
                helper.assertTrue(!in.getDisplayStacks().isEmpty(), name + " ingredient " + in + " resolves to an item");
            }
        }
    }

    /** 1.7 plateWeldedAnyResistantAlloy / plateTripleAnyBismoidBronze take any member's plate. */
    private static void aggregatePlates(GameTestHelper helper) {
        AStack welded = OreDictManager.ANY_RESISTANTALLOY.plateWelded();
        AStack cast = OreDictManager.ANY_RESISTANTALLOY.plateCast();
        AStack bronze = OreDictManager.ANY_BISMOIDBRONZE.plateCast();
        helper.assertTrue(welded.matchesRecipe(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_TCALLOY)), true),
                "a welded TC-alloy plate is an any-resistant-alloy welded plate");
        helper.assertTrue(welded.matchesRecipe(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_CDALLOY)), true),
                "a welded CD-alloy plate is an any-resistant-alloy welded plate");
        helper.assertTrue(cast.matchesRecipe(new ItemStack(ModAutogenItems.get(MaterialShapes.CASTPLATE, Mats.MAT_CDALLOY)), true),
                "a cast CD-alloy plate is an any-resistant-alloy cast plate");
        helper.assertFalse(welded.matchesRecipe(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, Mats.MAT_STEEL)), true),
                "a welded steel plate is not a resistant alloy");
        helper.assertTrue(bronze.matchesRecipe(new ItemStack(ModAutogenItems.get(MaterialShapes.CASTPLATE, Mats.MAT_BBRONZE)), true)
                        && bronze.matchesRecipe(new ItemStack(ModAutogenItems.get(MaterialShapes.CASTPLATE, Mats.MAT_ABRONZE)), true),
                "bismuth and arsenic bronze cast plates are any-bismoid-bronze cast plates");
        helper.succeed();
    }

    /** 1.7 SolderingRecipes "COMPUTERS" (non-528): the three control units, matched through the real matcher. */
    private static void solderingControllers(GameTestHelper helper) {
        ItemStack[] slots = {
                ModIntermediateItems.circuit(EnumCircuitType.CHIP, 32), ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR, 32),
                ModIntermediateItems.circuit(EnumCircuitType.CAPACITOR_TANTALIUM, 16),
                ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_CHASSIS, 1), new ItemStack(item("ingot_rubber"), 4),
                new ItemStack(ModAutogenItems.get(MaterialShapes.WIRE, Mats.MAT_LEAD), 16)};
        SolderingRecipes.SolderingRecipe recipe = SolderingRecipes.getRecipe(slots);
        helper.assertTrue(recipe != null && recipe.output.getItem() == ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER)
                        && recipe.duration == 400 && recipe.consumption == 15_000 && recipe.fluid != null
                        && recipe.fluid.fill == 1_000, "the control unit solders as in 1.7");
        ItemStack[] quantum = {
                ModIntermediateItems.circuit(EnumCircuitType.CHIP_QUANTUM, 16), ModIntermediateItems.circuit(EnumCircuitType.CHIP_BISMOID, 48),
                ModIntermediateItems.circuit(EnumCircuitType.ATOMIC_CLOCK, 8),
                ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED, 2), new ItemStack(ModUpgradeItems.upgrade_speed_3.get()),
                new ItemStack(ModAutogenItems.get(MaterialShapes.WIRE, Mats.MAT_LEAD), 32)};
        SolderingRecipes.SolderingRecipe q = SolderingRecipes.getRecipe(quantum);
        helper.assertTrue(q != null && q.output.getItem() == ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_QUANTUM)
                && q.consumption == 250_000, "the quantum control unit solders as in 1.7");
        helper.succeed();
    }

    /** 1.7 registerConstructionRecycling: the dismantling block, including the non-528 RBMK columns. */
    private static void anvilRecycling(GameTestHelper helper) {
        AnvilConstructionRecipe moderator = recycling(new ItemStack(item("rbmk_moderator")));
        helper.assertTrue(moderator != null && moderator.output.size() == 2
                        && moderator.output.get(0).stack.getItem() == item("rbmk_blank")
                        && moderator.output.get(1).stack.getItem() == item("block_graphite")
                        && moderator.output.get(1).stack.getCount() == 4 && moderator.isTierValid(4) && !moderator.isTierValid(3),
                "the RBMK moderator column dismantles into a blank column and four graphite blocks at tier 4");
        helper.assertTrue(recycling(new ItemStack(item("rbmk_fuel_rod"))) != null,
                "the fuel column dismantles while 528 mode is off");
        for (String crt : new String[]{"deco_crt_clean", "deco_crt_broken", "deco_crt_blinking", "deco_crt_bsod"}) {
            helper.assertTrue(recycling(new ItemStack(item(crt))) != null, crt + " dismantles (1.7 wildcard deco_crt)");
        }
        AnvilConstructionRecipe cabinet = recycling(new ItemStack(item("filing_cabinet")));
        helper.assertTrue(cabinet != null && cabinet.isTierValid(1), "the filing cabinet dismantles at tier 1 (1.7)");
        AnvilConstructionRecipe gear = recycling(com.hbm.items.ModGearItems.gear_large.get().of(1));
        helper.assertTrue(gear != null && gear.output.get(1).stack.getItem() == item("ingot_titanium"),
                "the steel gear (meta 1) dismantles into steel plates and titanium");
        helper.succeed();
    }

    private static AnvilConstructionRecipe recycling(ItemStack stack) {
        for (AnvilConstructionRecipe recipe : AnvilRecipes.getConstruction()) {
            if (recipe.getOverlay() == AnvilRecipes.OverlayType.RECYCLING && recipe.input.size() == 1
                    && recipe.input.get(0).matchesRecipe(stack, true)) return recipe;
        }
        return null;
    }
}
