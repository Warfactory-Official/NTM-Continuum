package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.foundry.ArcFurnaceRecipes;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.FuelPoolRecipes;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModPwrItems;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemStamp.StampType;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Ingredients;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Fix wave phase 3 (2026-10-03, History §19ej-3): machine recipe tables restored to original-hbm (1.7.10). Expected
 * values are the 1.7 table entries, quoted with their 1.7 line.
 */
public final class Fix3GameTests {

    private Fix3GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    /** Tool, not a test: see {@link RecipeTableDump}. */
    public static final GameTests.Function RECIPE_TABLE_DUMP = RecipeTableDump.requested()
            ? TESTS.function("recipe_table_dump", 100, () -> RecipeTableDump::dump) : null;

    public static final GameTests.Function GENERIC =
            TESTS.function("fix3_generic_tables", 20, () -> Fix3GameTests::generic);
    public static final GameTests.Function FLUID_TABLES =
            TESTS.function("fix3_fluid_tables", 20, () -> Fix3GameTests::fluidTables);
    public static final GameTests.Function SOLID_TABLES =
            TESTS.function("fix3_solid_tables", 20, () -> Fix3GameTests::solidTables);
    public static final GameTests.Function SHREDDER =
            TESTS.function("fix3_shredder", 20, () -> Fix3GameTests::shredder);
    public static final GameTests.Function ANVIL =
            TESTS.function("fix3_anvil", 20, () -> Fix3GameTests::anvil);
    public static final GameTests.Function FUEL_POOL =
            TESTS.function("fix3_fuel_pool", 20, () -> Fix3GameTests::fuelPool);

    public static void init() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.parse(id));
    }

    private static ItemStack of(String id) {
        return new ItemStack(item(id));
    }

    private static ItemStack tag(GameTestHelper helper, String tag) {
        List<Item> items = Ingredients.tagItems(tag);
        helper.assertTrue(!items.isEmpty(), "tag " + tag + " is empty");
        return new ItemStack(items.get(0));
    }

    private static boolean is(ItemStack stack, String id, int count) {
        return stack != null && stack.is(item(id)) && stack.getCount() == count;
    }

    private static boolean fluid(FluidStack stack, com.hbm.inventory.fluid.FluidType type, int amount) {
        return stack != null && stack.type == type && stack.fill == amount;
    }

    /** The recipe input that accepts {@code stack}, with its count, or -1. */
    private static int countFor(GenericRecipe recipe, ItemStack stack) {
        for (AStack in : recipe.inputItem) if (in.matchesRecipe(stack, true)) return in.stacksize;
        return -1;
    }

    public static void generic(GameTestHelper helper) {
        AssemblyMachineRecipes asm = AssemblyMachineRecipes.INSTANCE;
        asm.registerRecipes();
        // AssemblyMachineRecipes:472 cyclotron: 6 inputs, no steel or aluminium plates
        GenericRecipe cyclotron = asm.getRecipe("ass.cyclotron");
        helper.assertTrue(cyclotron.inputItem.length == 6 && countFor(cyclotron, tag(helper, "c:plates/steel")) == -1,
                "ass.cyclotron must have the 6 1.7 inputs");
        // :423 industrial turbine: 16 steel plates, 4 dense gold wire, 2 circuits
        GenericRecipe turbine = asm.getRecipe("ass.iturbine");
        helper.assertTrue(turbine.inputItem.length == 6 && countFor(turbine, tag(helper, "c:plates/steel")) == 16
                        && countFor(turbine, of("hbm:circuit_basic")) == 2, "ass.iturbine 1.7 counts");
        // :97 lead plating takes 1000 mB lubricant; :94 the circuit 1000 mB sulfuric acid
        GenericRecipe lead = asm.getRecipe("ass.exleadplating");
        helper.assertTrue(lead != null && fluid(lead.inputFluid[0], Fluids.LUBRICANT, 1_000), "ass.exleadplating lubricant");
        GenericRecipe circuit = asm.getRecipe("ass.excircuit");
        helper.assertTrue(circuit != null && fluid(circuit.inputFluid[0], Fluids.SULFURIC_ACID, 1_000), "ass.excircuit acid");
        helper.assertTrue(asm.getRecipe("ass.ex_lead_plating") == null && asm.getRecipe("ass.cast.iron") == null
                && asm.getRecipe("ass.weld.steel") == null && asm.getRecipe("ass.bat9k") == null
                && asm.getRecipe("ass.pilethorium") == null, "port-only assembly recipes are gone");
        helper.assertTrue(asm.getRecipe("ass.packagehbmfluid.water") != null, "1.7 package ids");
        // ChemicalPlantRecipes:236 meth: Items.dye meta 3 is cocoa beans
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        GenericRecipe meth = ChemicalPlantRecipes.INSTANCE.getRecipe("chem.meth");
        helper.assertTrue(countFor(meth, new ItemStack(Items.COCOA_BEANS)) == 2 && countFor(meth, new ItemStack(Items.BROWN_DYE)) == -1,
                "chem.meth takes 2 cocoa beans");
        // PlasmaForgeRecipes:183 REDD; :103 fusion vessel uses the motor component
        PlasmaForgeRecipes.INSTANCE.registerRecipes();
        GenericRecipe redd = PlasmaForgeRecipes.INSTANCE.getRecipe("ass.fensusan");
        helper.assertTrue(redd != null && redd.inputItem.length == 12 && is(redd.outputItem[0], "hbm:machine_battery_redd", 1),
                "ass.fensusan restored");
        GenericRecipe vessel = PlasmaForgeRecipes.INSTANCE.getRecipe("plsm.fusionvessel");
        helper.assertTrue(countFor(vessel, of("hbm:fusion_component_motor")) == 64
                && countFor(vessel, of("hbm:fusion_component_bscco_welded")) == -1, "fusion vessel motors");
        GenericRecipe hde = PlasmaForgeRecipes.INSTANCE.getRecipe("plsm.hde");
        helper.assertTrue(countFor(hde, tag(helper, "c:plates_cast/arsenic_bronze")) == 2, "plsm.hde takes any bismoid bronze");
        // BlastFurnaceRecipesNT:33 slag is ingot_raw[slag]; :52 the redstone ingot input
        BlastFurnaceRecipesNT.INSTANCE.registerRecipes();
        GenericRecipe steel = BlastFurnaceRecipesNT.INSTANCE.getRecipe("blast.steelFromIngot");
        helper.assertTrue(is(steel.outputItem[1], "hbm:ingot_raw_slag", 1), "blast furnace slag ingot, got " + steel.outputItem[1]);
        helper.assertTrue(countFor(BlastFurnaceRecipesNT.INSTANCE.getRecipe("blast.mingradeIngot"), of("hbm:ingot_raw_redstone")) == 1,
                "redstone ingot input");
        // BlastFurnaceRecipes:55 napalm; the neutron reflector recipes were 1.12.2
        ItemStack gasoline = com.hbm.items.ModFluidNetItems.canister_full.get().make(Fluids.GASOLINE, 1_000);
        DiFurnaceRecipes.Match napalm = DiFurnaceRecipes.INSTANCE.getMatch(gasoline, new ItemStack(Items.SLIME_BALL));
        helper.assertTrue(napalm != null && is(napalm.recipe().outputItem[0], "hbm:canister_napalm", 1), "napalm canister");
        helper.assertTrue(DiFurnaceRecipes.INSTANCE.getRecipe("difurnace.neutronReflectorCoal") == null, "port-only reflector gone");
        DiFurnaceRecipes.Match star = DiFurnaceRecipes.INSTANCE.getMatch(tag(helper, "c:ingots/saturnite"), of("hbm:ingot_meteorite"));
        helper.assertTrue(star != null && is(star.recipe().outputItem[0], "hbm:ingot_starmetal", 2), "starmetal x2");
        helper.succeed();
    }

    public static void fluidTables(GameTestHelper helper) {
        // PyroOvenRecipes:80 syngas + tungsten dust; :97 tar has no fluid input; no port-only heavy oil from coke
        boolean carbide = false, soot = false, cokeOil = false;
        for (PyroOvenRecipes.PyroOvenRecipe r : PyroOvenRecipes.getAll()) {
            if (r.outputItem != null && r.outputItem.is(item("hbm:ingot_tungsten_carbide"))) {
                carbide = fluid(r.inputFluid, Fluids.SYNGAS, 2_000) && fluid(r.outputFluid, Fluids.SPENTSTEAM, 1_000) && r.duration == 300;
            }
            if (r.outputItem != null && r.outputItem.is(item("hbm:powder_ash_soot"))) {
                soot = r.inputFluid == null && fluid(r.outputFluid, Fluids.CARBONDIOXIDE, 1_000) && r.inputItem.stacksize == 4;
            }
            if (r.outputFluid != null && r.outputFluid.type == Fluids.HEAVYOIL && r.inputItem != null
                    && r.inputItem.matchesRecipe(tag(helper, "c:cokes"), true)) cokeOil = true;
        }
        helper.assertTrue(carbide && soot && !cokeOil, "pyro oven: carbide " + carbide + ", soot " + soot + ", coke oil " + cokeOil);
        // MixerRecipes:98 LYE: 100 mB out, 100 ticks, 100 mB water, 1 wood ash
        MixerRecipes.MixerRecipe lye = MixerRecipes.getOutput(Fluids.LYE, 0);
        helper.assertTrue(lye.output == 100 && lye.processTime == 100 && fluid(lye.input1, Fluids.WATER, 100)
                && lye.solidInput.stacksize == 1, "mixer lye");
        helper.assertTrue(!MixerRecipes.getOutput(Fluids.FISHOIL, 0).solidInput.matchesRecipe(new ItemStack(Items.COOKED_COD), true),
                "fish oil takes raw fish only");
        // LiquefactionRecipes:47 lead block; :64 gland; :66 sunflower; :96 food -> SALIENT (bread 5 x 0.6 x 20)
        helper.assertTrue(fluid(LiquefactionRecipes.getOutput(tag(helper, "c:storage_blocks/lead")), Fluids.LEAD, 900), "lead block");
        helper.assertTrue(fluid(LiquefactionRecipes.getOutput(of("hbm:glyphid_gland_empty")), Fluids.BIOGAS, 2_000), "gland");
        helper.assertTrue(fluid(LiquefactionRecipes.getOutput(new ItemStack(Items.SUNFLOWER)), Fluids.SUNFLOWEROIL, 100), "sunflower");
        FluidStack bread = LiquefactionRecipes.getOutput(new ItemStack(Items.BREAD));
        helper.assertTrue(fluid(bread, Fluids.SALIENT, 60), "bread -> 60 mB salient, got " + (bread == null ? null : bread.fill));
        helper.assertTrue(fluid(LiquefactionRecipes.getOutput(new ItemStack(Items.COD)), Fluids.FISHOIL, 100), "raw cod");
        // OutgasserRecipes:51 mushroom; :59 PVC
        GenericRecipe mush = OutgasserRecipes.INSTANCE.getRecipe(new ItemStack(Items.RED_MUSHROOM));
        helper.assertTrue(mush != null && is(mush.outputItem[0], "hbm:mush", 1), "outgasser mushroom");
        GenericRecipe pvc = OutgasserRecipes.INSTANCE.getRecipe(tag(helper, "c:ingots/pvc"));
        helper.assertTrue(pvc != null && is(pvc.outputItem[0], "hbm:ingot_c4", 1) && fluid(pvc.outputFluid[0], Fluids.COLLOID, 250),
                "outgasser pvc");
        // CrackingRecipes:54 biogas
        CrackingRecipes.CrackingRecipe biogas = CrackingRecipes.getCracking(Fluids.BIOGAS);
        helper.assertTrue(biogas != null && fluid(biogas.left(), Fluids.PETROLEUM, 20) && fluid(biogas.right(), Fluids.AROMATICS, 20),
                "biogas cracking");
        helper.succeed();
    }

    public static void solidTables(GameTestHelper helper) {
        // CentrifugeRecipes:186 euphemium cluster, :210 slag block; the schraranium ingot entry was 1.12.2
        ItemStack[] euph = CentrifugeRecipes.getOutput(of("hbm:block_euphemium_cluster"));
        helper.assertTrue(euph != null && is(euph[0], "hbm:nugget_euphemium", 7) && is(euph[3], "hbm:nugget_solinium", 2), "euphemium cluster");
        ItemStack[] slag = CentrifugeRecipes.getOutput(of("hbm:block_slag"));
        helper.assertTrue(slag != null && is(slag[0], "minecraft:gravel", 1) && is(slag[3], "hbm:dust", 1), "slag block");
        helper.assertTrue(CentrifugeRecipes.getOutput(of("hbm:ingot_schraranium")) == null, "no schraranium centrifuge");
        // CrystallizerRecipes:126 plastic scrap -> circuit piece, peroxide 500, 600 ticks
        CrystallizerRecipes.CrystallizerRecipe piece = CrystallizerRecipes.getOutput(of("hbm:scrap_plastic_cpu_clock"), Fluids.PEROXIDE);
        helper.assertTrue(piece != null && is(piece.output, "hbm:circuit_star_piece_cpu_clock", 1) && piece.acidAmount == 500
                && piece.duration == 600, "plastic scrap crystallizer");
        // CombinationRecipes:51 crystalCinnabar is the cinnabar gem
        CombinationRecipes.CombinationRecipe cinnabar = CombinationRecipes.getOutput(of("hbm:cinnabar"));
        helper.assertTrue(cinnabar != null && fluid(cinnabar.fluidOutput(), Fluids.MERCURY, 100), "cinnabar roasting");
        // PressRecipes:93 carbon fine wire from the graphite ingot
        ItemStack wire = PressRecipes.getOutput(of("hbm:ingot_graphite"), of("hbm:stamp_iron_wire"));
        helper.assertTrue(is(wire, "hbm:wire_fine_carbon", 8), "carbon wire, got " + wire);
        // SolderingRecipes:185 afterburn I, :210 afterburn II
        SolderingRecipes.register();
        boolean afterburn1 = false, afterburn2 = false;
        for (SolderingRecipes.SolderingRecipe r : SolderingRecipes.recipes) {
            if (r.output.is(item("hbm:upgrade_afterburn_1"))) afterburn1 = r.duration == 200 && r.consumption == 1_000;
            if (r.output.is(item("hbm:upgrade_afterburn_2"))) afterburn2 = true;
        }
        helper.assertTrue(afterburn1 && afterburn2, "afterburn upgrades");
        // ParticleAcceleratorRecipes:99 chicken + chicken
        ParticleAcceleratorRecipes.register();
        boolean chicken = ParticleAcceleratorRecipes.recipes.stream().anyMatch(r -> r.momentum == 100
                && r.input1.matchesRecipe(new ItemStack(Items.CHICKEN), true) && is(r.output1, "hbm:nugget", 1));
        helper.assertTrue(chicken, "chicken nuggets");
        // ArcFurnaceRecipes:59 fiberglass block; :118 furnace autogen (iron ore smelts to its furnace result)
        ArcFurnaceRecipes.ArcFurnaceRecipe fiber = ArcFurnaceRecipes.getOutput(of("hbm:block_fiberglass"), false);
        helper.assertTrue(fiber != null && is(fiber.solidOutput, "hbm:nugget_silicon", 40), "fiberglass block");
        ArcFurnaceRecipes.ArcFurnaceRecipe ore = ArcFurnaceRecipes.getOutput(new ItemStack(Items.IRON_ORE), false, helper.getLevel());
        helper.assertTrue(ore != null && ore.solidOutput.is(Items.IRON_INGOT), "arc furnace smelts iron ore");
        helper.succeed();
    }

    public static void shredder(GameTestHelper helper) {
        // ShredderRecipes:290-295 sellafite 0..5 -> 1/2/3/5/7/15 nuclear scrap
        helper.assertTrue(is(ShredderRecipes.getShredderResult(of("hbm:sellafield_5")), "hbm:scrap_nuclear", 15), "sellafite 5");
        helper.assertTrue(is(ShredderRecipes.getShredderResult(of("hbm:sellafield_3")), "hbm:scrap_nuclear", 5), "sellafite 3");
        helper.assertTrue(is(ShredderRecipes.getShredderResult(new ItemStack(Items.CREEPER_HEAD)), "hbm:biomass", 4), "skull");
        helper.assertTrue(is(ShredderRecipes.getShredderResult(of("hbm:steel_scaffold")), "hbm:powder_steel_tiny", 4), "scaffold");
        helper.assertTrue(is(ShredderRecipes.getShredderResult(of("hbm:dungeon_chain")), "hbm:powder_steel_tiny", 1), "chain");
        helper.assertTrue(is(ShredderRecipes.getShredderResult(of("hbm:stone_resource_limestone")), "hbm:powder_limestone", 4), "limestone");
        // registerPost: a block without an ingot or gem gives 4 dust (sulfur); with one, 9 (redstone has ingot_raw[redstone])
        helper.assertTrue(is(ShredderRecipes.getShredderResult(of("hbm:block_sulfur")), "hbm:sulfur", 4), "sulfur block -> 4");
        helper.assertTrue(is(ShredderRecipes.getShredderResult(new ItemStack(Items.REDSTONE_BLOCK)), "minecraft:redstone", 9), "redstone block -> 9");
        helper.assertTrue(is(ShredderRecipes.getShredderResult(of("hbm:powder_cobalt_tiny")), "hbm:dust_tiny", 1), "tiny dust");
        helper.assertTrue(is(ShredderRecipes.getShredderResult(of("hbm:nugget_bismuth")), "hbm:scrap", 1), "nuggets give scrap");
        // :338-341 a bobblehead gives its own plastic scrap (BOB = CPU_SOCKET)
        ItemStack bob = ((com.hbm.items.block.ItemBlockDecoVariant) item("hbm:bobblehead"))
                .of(com.hbm.blocks.generic.BlockBobble.BobbleType.BOB.ordinal());
        helper.assertTrue(is(ShredderRecipes.getShredderResult(bob), "hbm:scrap_plastic_cpu_socket", 1), "bobblehead scrap");
        helper.succeed();
    }

    public static void anvil(GameTestHelper helper) {
        List<AnvilRecipes.AnvilConstructionRecipe> construction = AnvilRecipes.getConstruction();
        // AnvilRecipes:212 motor = 2 iron plates, a copper coil and a copper torus
        AnvilRecipes.AnvilConstructionRecipe motor = construction.stream()
                .filter(r -> r.output.size() == 1 && is(r.output.get(0).stack, "hbm:motor", 2)).findFirst().orElse(null);
        helper.assertTrue(motor != null && motor.input.stream().anyMatch(a -> a.matchesRecipe(of("hbm:coil_copper"), true))
                && motor.input.stream().noneMatch(a -> a.matchesRecipe(of("hbm:motor"), true)), "motor recipe");
        // :383 arc welder, :532 two AJR plates, :556 duct boxes
        helper.assertTrue(construction.stream().anyMatch(r -> is(r.output.get(0).stack, "hbm:machine_arc_welder", 1) && r.tierLower == 2),
                "arc welder");
        helper.assertTrue(construction.stream().anyMatch(r -> is(r.output.get(0).stack, "hbm:plate_armor_ajr", 2) && r.tierLower == 3),
                "ajr plates");
        helper.assertTrue(construction.stream().filter(r -> BuiltInRegistries.ITEM.getKey(r.output.get(0).stack.getItem()).getPath().startsWith("fluid_duct_box")).count() == 15,
                "15 duct box recipes");
        helper.assertTrue(construction.stream().noneMatch(r -> r.output.get(0).stack.is(item("hbm:plate_fuel_u233"))), "no plate fuel");
        // :93 murky wings; :129 rename
        List<AnvilSmithingRecipe> smithing = AnvilRecipes.getSmithing();
        helper.assertTrue(smithing.stream().anyMatch(r -> r.tier == 1916169 && r.getSimpleOutput().is(item("hbm:wings_murk"))), "murky wings");
        ItemStack tagged = new ItemStack(Items.NAME_TAG);
        ItemStacks.setCustomName(tagged, Component.literal("Bob\\&4"));
        AnvilSmithingRecipe rename = smithing.stream().filter(r -> r.matches(new ItemStack(Items.STICK, 5), tagged)).findFirst().orElse(null);
        helper.assertTrue(rename != null && rename.amountConsumed(1, false) == 0, "rename recipe keeps the name tag");
        ItemStack renamed = rename.getOutput(new ItemStack(Items.STICK, 5), tagged);
        helper.assertTrue(renamed.getCount() == 1 && renamed.getHoverName().getString().equals("§rBob§4"),
                "renamed stick, got " + renamed.getHoverName().getString());
        helper.succeed();
    }

    public static void fuelPool(GameTestHelper helper) {
        // FuelPoolRecipes:44 every hot PWR rod cools into its depleted rod; :36 plate waste; drum slots hold one item
        ItemStack hot = new ItemStack(ModPwrItems.hot(EnumPWRFuel.MEU));
        helper.assertTrue(FuelPoolRecipes.isHot(hot) && FuelPoolRecipes.cool(hot).is(ModPwrItems.depleted(EnumPWRFuel.MEU)), "hot PWR rod");
        ItemStack plate = com.hbm.items.machine.ItemDepletedFuel.makeHot(item("hbm:waste_plate_u235"), 1);
        ItemStack cooled = FuelPoolRecipes.cool(plate);
        helper.assertTrue(FuelPoolRecipes.isHot(plate) && cooled.is(item("hbm:waste_plate_u235")) && !FuelPoolRecipes.isHot(cooled),
                "hot plate waste");
        com.hbm.tileentity.machine.TileEntityWasteDrum drum = new com.hbm.tileentity.machine.TileEntityWasteDrum(
                net.minecraft.core.BlockPos.ZERO, com.hbm.blocks.machine.ModMachineBlocks.machine_waste_drum.get().defaultBlockState());
        helper.assertTrue(drum.getMaxStackSize() == 1 && drum.isItemValidForSlot(0, of("hbm:rbmk_fuel_ueu")), "drum limit and RBMK rods");
        helper.succeed();
    }
}
