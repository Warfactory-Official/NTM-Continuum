package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
import com.hbm.tileentity.machine.TileEntityMachineTurbine;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import dev.golem.core.gametest.GameTests;
import net.minecraft.gametest.framework.GameTestHelper;

/** Shared, server-only GameTest functions and execution budgets. */
public final class HbmGameTestFunctions {

    private HbmGameTestFunctions() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function COOLING_RECIPES =
            TESTS.function("cooling_recipes", 20, () -> HbmGameTestFunctions::coolingRecipes);
    public static final GameTests.Function TURBINE_CONVERSION =
            TESTS.function("turbine_conversion", 20, () -> HbmGameTestFunctions::turbineConversion);
    public static final GameTests.Function TURBINE_IDENTIFIER =
            TESTS.function("turbine_identifier", 20, () -> HbmGameTestFunctions::turbineFluidId);
    public static final GameTests.Function TURBINE_SLOTS =
            TESTS.function("turbine_slots", 20, () -> HbmGameTestFunctions::turbineSlots);
    public static final GameTests.Function TURBINE_MENU =
            TESTS.function("turbine_menu", 20, () -> HbmGameTestFunctions::turbineMenu);
    public static final GameTests.Function CONDENSER_CONVERSION =
            TESTS.function("condenser_conversion", 20, () -> HbmGameTestFunctions::condenserConversion);
    public static final GameTests.Function PUREX_RECIPE_CYCLE =
            TESTS.function("purex_recipe_cycle", 20, () -> HbmGameTestFunctions::purexRecipeCycle);
    public static final GameTests.Function PUREX_HOT_WASTE_REJECTED =
            TESTS.function("purex_hot_waste_rejected", 20, () -> HbmGameTestFunctions::purexHotWasteRejected);
    public static final GameTests.Function PUREX_UPGRADES =
            TESTS.function("purex_upgrades", 20, () -> HbmGameTestFunctions::purexUpgrades);
    public static final GameTests.Function PUREX_MAX_POWER =
            TESTS.function("purex_max_power", 20, () -> HbmGameTestFunctions::purexMaxPower);
    public static final GameTests.Function PUREX_OUTPUT_FLUID =
            TESTS.function("purex_output_fluid", 20, () -> HbmGameTestFunctions::purexOutputFluid);
    public static final GameTests.Function PUREX_SLOTS =
            TESTS.function("purex_slots", 20, () -> HbmGameTestFunctions::purexSlots);
    public static final GameTests.Function PUREX_MENU =
            TESTS.function("purex_menu", 20, () -> HbmGameTestFunctions::purexMenu);
    public static final GameTests.Function PUREX_LAYOUT =
            TESTS.function("purex_layout", 100, () -> HbmGameTestFunctions::purexLayout);
    public static final GameTests.Function ZIRNOX_GOLDEN_TICK =
            TESTS.function("zirnox_golden_tick", 20, () -> HbmGameTestFunctions::zirnoxGoldenTick);
    public static final GameTests.Function ZIRNOX_LAYOUT =
            TESTS.function("zirnox_layout", 100, () -> HbmGameTestFunctions::zirnoxLayout);
    public static final GameTests.Function ZIRNOX_WORLD_TICK =
            TESTS.function("zirnox_world_tick", 100, () -> HbmGameTestFunctions::zirnoxWorldTick);
    public static final GameTests.Function ZIRNOX_STEAM_OUTPUT =
            TESTS.function("zirnox_steam_output", 300, () -> HbmGameTestFunctions::zirnoxSteamOutput);
    public static final GameTests.Function ZIRNOX_MELTDOWN =
            TESTS.function("zirnox_meltdown", 100, () -> HbmGameTestFunctions::zirnoxMeltdown);
    public static final GameTests.Function ZIRNOX_WRECK_HAZARDS =
            TESTS.function("zirnox_wreck_hazards", 100, () -> HbmGameTestFunctions::zirnoxWreckHazards);

    // --- ore worldgen (implementations live in OreWorldgenGameTests) ---
    public static final GameTests.Function ORE_VEIN_SHAPE =
            TESTS.function("ore_vein_shape", 200, () -> OreWorldgenGameTests::oreVeinShape);
    public static final GameTests.Function ORE_VEIN_OFFSET =
            TESTS.function("ore_vein_offset", 200, () -> OreWorldgenGameTests::oreVeinOffset);
    public static final GameTests.Function ORE_VEIN_PREDICATE =
            TESTS.function("ore_vein_predicate", 200, () -> OreWorldgenGameTests::oreVeinPredicate);
    public static final GameTests.Function ORE_TARGETS =
            TESTS.function("ore_targets", 200, () -> OreWorldgenGameTests::oreTargets);
    public static final GameTests.Function ORE_DEPTH_DEPOSIT =
            TESTS.function("ore_depth_deposit", 200, () -> OreWorldgenGameTests::oreDepthDeposit);
    public static final GameTests.Function ORE_COUNTS =
            TESTS.function("ore_counts", 200, () -> OreWorldgenGameTests::oreCounts);
    public static final GameTests.Function ORE_SMOLDERING =
            TESTS.function("ore_smoldering", 300, () -> OreWorldgenGameTests::oreSmoldering);

    // --- SILEX (implementations live in SilexGameTests) ---
    public static final GameTests.Function SILEX_PROCESS =
            TESTS.function("silex_process", 100, () -> SilexGameTests::silexProcess);
    public static final GameTests.Function SILEX_LOAD =
            TESTS.function("silex_load", 100, () -> SilexGameTests::silexLoad);
    public static final GameTests.Function SILEX_LOAD_FLUID =
            TESTS.function("silex_load_fluid", 100, () -> SilexGameTests::silexLoadFluid);
    public static final GameTests.Function SILEX_OUTPUT_WEIGHTS =
            TESTS.function("silex_output_weights", 100, () -> SilexGameTests::silexOutputWeights);
    public static final GameTests.Function SILEX_QUEUE =
            TESTS.function("silex_queue", 100, () -> SilexGameTests::silexQueue);
    public static final GameTests.Function SILEX_SLOTS =
            TESTS.function("silex_slots", 100, () -> SilexGameTests::silexSlots);
    public static final GameTests.Function SILEX_MENU =
            TESTS.function("silex_menu", 100, () -> SilexGameTests::silexMenu);
    public static final GameTests.Function SILEX_LAYOUT =
            TESTS.function("silex_layout", 100, () -> SilexGameTests::silexLayout);
    public static final GameTests.Function SILEX_MODE_RESET =
            TESTS.function("silex_mode_reset", 100, () -> SilexGameTests::silexModeReset);

    // --- FEL (implementations live in FelGameTests) ---
    public static final GameTests.Function FEL_BEAM =
            TESTS.function("fel_beam", 100, () -> FelGameTests::felBeam);
    public static final GameTests.Function FEL_BEAM_ILLEGAL =
            TESTS.function("fel_beam_illegal", 100, () -> FelGameTests::felBeamIllegal);
    public static final GameTests.Function FEL_ROTATION =
            TESTS.function("fel_rotation", 100, () -> FelGameTests::felRotation);
    public static final GameTests.Function FEL_POWER =
            TESTS.function("fel_power", 100, () -> FelGameTests::felPower);
    public static final GameTests.Function FEL_BURN =
            TESTS.function("fel_burn", 200, () -> FelGameTests::felBurn);
    public static final GameTests.Function FEL_BURN_INFRARED =
            TESTS.function("fel_burn_infrared", 200, () -> FelGameTests::felBurnInfrared);
    public static final GameTests.Function FEL_POWER_PORT =
            TESTS.function("fel_power_port", 100, () -> FelGameTests::felPowerPort);
    public static final GameTests.Function FEL_LAYOUT =
            TESTS.function("fel_layout", 100, () -> FelGameTests::felLayout);
    public static final GameTests.Function FEL_SLOTS =
            TESTS.function("fel_slots", 100, () -> FelGameTests::felSlots);
    public static final GameTests.Function FEL_MENU =
            TESTS.function("fel_menu", 100, () -> FelGameTests::felMenu);

    public static void init() {}

    private static void coolingRecipes(GameTestHelper helper) {
        Fluids.init();
        check(helper, Fluids.STEAM, 100, Fluids.SPENTSTEAM, 1, 200);
        check(helper, Fluids.HOTSTEAM, 1, Fluids.STEAM, 10, 2);
        check(helper, Fluids.SUPERHOTSTEAM, 1, Fluids.HOTSTEAM, 10, 18);
        check(helper, Fluids.ULTRAHOTSTEAM, 1, Fluids.SUPERHOTSTEAM, 10, 120);
        helper.succeed();
    }

    /** The 85% / 6000 mB-per-tick turbine math, per steam stage, plus the buffer clamp. */
    private static void turbineConversion(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);

        // STEAM: ops = min(64000/100, 128000/1, 6000/100) = 60 -> 6000 mB in, 60 mB out, 60*200*0.85 HE
        TileEntityMachineTurbine turbine = placeTurbine(helper, pos);
        turbine.input.setFill(64_000);
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getFill() == 58_000, "Turbine drained " + (64_000 - turbine.input.getFill()) + " mB of steam, expected 6000");
        helper.assertTrue(turbine.output.getTankType() == Fluids.SPENTSTEAM && turbine.output.getFill() == 60,
                "Turbine spent-steam output regressed");
        helper.assertTrue(turbine.power == 10_200, "Turbine steam power regressed: " + turbine.power);

        // SUPERHOTSTEAM: ops = min(64000/1, 128000/10, 6000/1) = 6000 -> 6000 mB in, 60000 mB hot steam out
        turbine = placeTurbine(helper, pos);
        turbine.input.setTankType(Fluids.SUPERHOTSTEAM);
        turbine.input.setFill(64_000);
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getFill() == 58_000, "Turbine superhot steam throughput must cap at 6000 mB/t");
        helper.assertTrue(turbine.output.getTankType() == Fluids.HOTSTEAM && turbine.output.getFill() == 60_000,
                "Turbine must cool superhot steam into hot steam");
        helper.assertTrue(turbine.power == 91_800, "Turbine superhot steam power regressed: " + turbine.power);

        // the buffer clamps at maxPower
        turbine = placeTurbine(helper, pos);
        turbine.input.setFill(64_000);
        turbine.power = TileEntityMachineTurbine.MAX_POWER - 1;
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.power == TileEntityMachineTurbine.MAX_POWER, "Turbine power must clamp to maxPower");

        // a full output tank stalls the machine outright
        turbine = placeTurbine(helper, pos);
        turbine.input.setFill(64_000);
        turbine.output.setFill(turbine.output.getMaxFill());
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getFill() == 64_000 && turbine.power == 0,
                "Turbine must stall when the output tank is full");
        helper.succeed();
    }

    /** The fluid identifier picks the steam stage and passes from slot 0 to slot 1. */
    private static void turbineFluidId(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);

        TileEntityMachineTurbine turbine = placeTurbine(helper, pos);
        turbine.inventory.setStackInSlot(0, com.hbm.items.machine.ItemFluidId.make(Fluids.SUPERHOTSTEAM));
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getTankType() == Fluids.SUPERHOTSTEAM, "Identifier did not retype the input tank");
        helper.assertTrue(turbine.inventory.getStackInSlot(0).isEmpty(), "Consumed identifier must leave slot 0");
        helper.assertTrue(turbine.inventory.getStackInSlot(1).getItem() instanceof com.hbm.items.machine.IItemFluidId,
                "Identifier must pass down into slot 1");

        // a fluid the turbine cannot cool: no power, and the output tank goes NONE (the GUI's warning state)
        turbine = placeTurbine(helper, pos);
        turbine.inventory.setStackInSlot(0, com.hbm.items.machine.ItemFluidId.make(Fluids.WATER));
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getTankType() == Fluids.WATER, "Identifier must retype even to a non-coolable fluid");
        helper.assertTrue(turbine.output.getTankType() == Fluids.NONE, "Non-coolable input must blank the output tank");
        helper.assertTrue(turbine.power == 0, "Non-coolable input must not generate power");
        helper.succeed();
    }

    /** Slot validity, both canister bays and the battery slot. */
    private static void turbineSlots(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachineTurbine turbine = placeTurbine(helper, pos);

        var identifier = com.hbm.items.machine.ItemFluidId.make(Fluids.STEAM);
        var bucket = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET);
        var battery = new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModBatteryItems.PACKS.get(com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD).get());
        helper.assertTrue(turbine.isItemValidForSlot(0, identifier) && !turbine.isItemValidForSlot(0, bucket),
                "Slot 0 must take fluid identifiers only");
        helper.assertTrue(turbine.isItemValidForSlot(2, bucket) && !turbine.isItemValidForSlot(2, identifier),
                "Slot 2 must take fluid containers only");
        helper.assertTrue(turbine.isItemValidForSlot(4, battery) && !turbine.isItemValidForSlot(4, bucket),
                "Slot 4 must take batteries only");
        helper.assertTrue(turbine.isItemValidForSlot(3, bucket) && turbine.isItemValidForSlot(6, bucket),
                "The bay output slots must accept the machine's own writes");

        // input bay: a full container drains into the input tank (WATER so the conversion stays out of it)
        turbine.input.setTankType(Fluids.WATER);
        turbine.inventory.setStackInSlot(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getFill() == 1_000, "Input canister bay did not drain into the tank");
        helper.assertTrue(turbine.inventory.getStackInSlot(2).isEmpty()
                        && turbine.inventory.getStackInSlot(3).is(net.minecraft.world.item.Items.BUCKET),
                "Emptied container must move to slot 3");

        // output bay: an empty bucket comes back filled with the spent steam
        turbine = placeTurbine(helper, pos);
        turbine.output.setFill(2_000);
        turbine.inventory.setStackInSlot(5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.output.getFill() == 1_000, "Output canister bay did not drain the output tank");
        helper.assertTrue(turbine.inventory.getStackInSlot(5).isEmpty()
                        && com.hbm.items.machine.ItemFluidBucket.getFluid(turbine.inventory.getStackInSlot(6)) == Fluids.SPENTSTEAM,
                "Filled spent-steam bucket must move to slot 6");

        // battery slot: the buffer empties into the item
        turbine = placeTurbine(helper, pos);
        turbine.power = 5_000;
        turbine.inventory.setStackInSlot(4, battery.copy());
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.power == 0, "Turbine must push its buffer into the battery slot");
        var charged = turbine.inventory.getStackInSlot(4);
        helper.assertTrue(((com.hbm.api.energymk2.IBatteryItem) charged.getItem()).getCharge(charged) == 5_000,
                "Battery did not receive the turbine's power");
        helper.succeed();
    }

    /** The menu layout — the old container's slot count and coordinates. */
    private static void turbineMenu(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachineTurbine turbine = placeTurbine(helper, pos);
        helper.assertTrue(turbine instanceof net.minecraft.world.MenuProvider, "The turbine must open a menu");

        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerMachineTurbine(0, player.getInventory(), turbine);
        helper.assertTrue(menu.slots.size() == 7 + 36, "Turbine menu must have 7 machine slots + the player inventory");
        assertSlot(helper, menu, 0, 8, 17);
        assertSlot(helper, menu, 1, 8, 53);
        assertSlot(helper, menu, 2, 44, 17);
        assertSlot(helper, menu, 3, 44, 53);
        assertSlot(helper, menu, 4, 98, 53);
        assertSlot(helper, menu, 5, 152, 17);
        assertSlot(helper, menu, 6, 152, 53);
        helper.assertFalse(menu.slots.get(1).mayPlace(com.hbm.items.machine.ItemFluidId.make(Fluids.STEAM)),
                "The identifier output slot must be take-only");
        helper.succeed();
    }

    private static void assertSlot(GameTestHelper helper, net.minecraft.world.inventory.AbstractContainerMenu menu,
                                   int index, int x, int y) {
        var slot = menu.slots.get(index);
        helper.assertTrue(slot.x == x && slot.y == y,
                "Turbine slot " + index + " sits at " + slot.x + "," + slot.y + ", expected " + x + "," + y);
    }

    /** The condenser closes the loop: spent steam in, the same amount of water out. */
    private static void condenserConversion(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.machine_condenser.get().defaultBlockState());
        var condenser = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, com.hbm.tileentity.machine.TileEntityCondenser.class);
        condenser.input.setFill(100);
        com.hbm.tileentity.machine.TileEntityCondenser.serverTick(helper.getLevel(), helper.absolutePos(pos),
                condenser.getBlockState(), condenser);
        helper.assertTrue(condenser.input.getFill() == 0, "Condenser must consume the spent steam");
        helper.assertTrue(condenser.output.getTankType() == Fluids.WATER && condenser.output.getFill() == 100,
                "Condenser must return the spent steam as water 1:1");
        helper.succeed();
    }

    private static TileEntityMachineTurbine placeTurbine(GameTestHelper helper, net.minecraft.core.BlockPos pos) {
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.setBlock(pos, ModMachineBlocks.machine_turbine.get().defaultBlockState());
        return dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, TileEntityMachineTurbine.class);
    }

    private static void tick(GameTestHelper helper, net.minecraft.core.BlockPos pos, TileEntityMachineTurbine turbine) {
        TileEntityMachineTurbine.serverTick(helper.getLevel(), helper.absolutePos(pos), turbine.getBlockState(), turbine);
    }

    // --- PUREX ---------------------------------------------------------------------------------------

    /** One full ZIRNOX reprocessing cycle: 100 ticks at 1000 HE/t, exact reagent draw and the four products. */
    private static void purexRecipeCycle(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachinePUREX purex = placePurex(helper, pos);

        purex.purexModule.recipe = "purex.zirnoxnu";
        tick(helper, pos, purex); // lets setupTanks conform the reagent tanks to the recipe
        helper.assertTrue(purex.inputTanks[0].getTankType() == Fluids.KEROSENE
                        && purex.inputTanks[1].getTankType() == Fluids.NITRIC_ACID,
                "PUREX reagent tanks must conform to the selected recipe");

        purex.inputTanks[0].setFill(24_000);
        purex.inputTanks[1].setFill(24_000);
        purex.inventory.setStackInSlot(4, new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModZirnoxItems.waste(ZirnoxFuel.NATURAL_URANIUM)));
        purex.power = 1_000_000;

        for (int i = 0; i < 100; i++) tick(helper, pos, purex);

        helper.assertTrue(purex.power == 900_000, "PUREX must draw 1000 HE/t for 100 ticks, power is " + purex.power);
        helper.assertTrue(purex.inputTanks[0].getFill() == 23_500 && purex.inputTanks[1].getFill() == 23_750,
                "PUREX must consume 500 mB kerosene + 250 mB nitric acid per cycle");
        helper.assertTrue(purex.inventory.getStackInSlot(4).isEmpty(), "PUREX must consume the spent-fuel input");
        assertOutput(helper, purex, 7, com.hbm.items.ModItems.nugget_u238.get(), 1);
        assertOutput(helper, purex, 8, com.hbm.items.ModItems.nugget_pu_mix.get(), 2);
        assertOutput(helper, purex, 9, com.hbm.items.ModItems.nugget_pu239.get(), 1);
        assertOutput(helper, purex, 10, com.hbm.items.ModItems.nuclear_waste_tiny.get(), 2);
        helper.succeed();
    }

    /** Hot spent fuel must be cooled in a waste drum first — the recipes only match cold stacks. */
    private static void purexHotWasteRejected(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachinePUREX purex = placePurex(helper, pos);
        purex.purexModule.recipe = "purex.zirnoxnu";

        var cold = new net.minecraft.world.item.ItemStack(com.hbm.items.ModZirnoxItems.waste(ZirnoxFuel.NATURAL_URANIUM));
        var hot = cold.copy();
        com.hbm.items.ModDataComponents.wasteHot(hot, true);

        helper.assertTrue(com.hbm.inventory.recipes.PUREXRecipes.INSTANCE.matchesInput(cold), "Cold waste must match a PUREX recipe");
        helper.assertFalse(com.hbm.inventory.recipes.PUREXRecipes.INSTANCE.matchesInput(hot), "Hot waste must not match any PUREX recipe");
        helper.assertTrue(purex.isItemValidForSlot(4, cold), "Cold waste must be insertable");
        helper.assertFalse(purex.isItemValidForSlot(4, hot), "Hot waste must be rejected by the input slot");
        helper.succeed();
    }

    /** The 1.12.2 upgrade formulas, pinned through observable speed (progress/tick) and draw (HE/tick). */
    private static void purexUpgrades(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);

        // SPEED 3: speed 2.0 (progress 0.02/t), pow 4.0 (4000 HE/t)
        assertUpgradeRun(helper, pos, com.hbm.items.ModUpgradeItems.upgrade_speed_3.get(), 25, 0.5D, 4_000L * 25);
        // POWER 3: speed 1.0 (progress 0.01/t), pow 0.25 (250 HE/t)
        assertUpgradeRun(helper, pos, com.hbm.items.ModUpgradeItems.upgrade_power_3.get(), 25, 0.25D, 250L * 25);
        // OVERDRIVE 3: speed 4.0 (progress 0.04/t), pow 11.0 (11000 HE/t)
        assertUpgradeRun(helper, pos, com.hbm.items.ModUpgradeItems.upgrade_overdrive_3.get(), 20, 0.8D, 11_000L * 20);
        helper.succeed();
    }

    private static void assertUpgradeRun(GameTestHelper helper, net.minecraft.core.BlockPos pos,
                                         net.minecraft.world.item.Item upgrade, int ticks, double expectedProgress, long expectedDraw) {
        TileEntityMachinePUREX purex = placePurex(helper, pos);
        purex.purexModule.recipe = "purex.zirnoxnu";
        purex.inventory.setStackInSlot(2, new net.minecraft.world.item.ItemStack(upgrade));
        tick(helper, pos, purex);
        purex.inputTanks[0].setFill(24_000);
        purex.inputTanks[1].setFill(24_000);
        purex.inventory.setStackInSlot(4, new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModZirnoxItems.waste(ZirnoxFuel.NATURAL_URANIUM), 64));
        purex.power = 1_000_000;

        for (int i = 0; i < ticks; i++) tick(helper, pos, purex);

        double progress = purex.purexModule.progress;
        helper.assertTrue(Math.abs(progress - expectedProgress) < 1.0E-6,
                upgrade + " progress after " + ticks + " ticks was " + progress + ", expected " + expectedProgress);
        long drawn = 1_000_000L - purex.power;
        helper.assertTrue(drawn == expectedDraw, upgrade + " drew " + drawn + " HE, expected " + expectedDraw);
    }

    /** The dynamic power ceiling: {@code recipe.power * 100}, floored at the buffer and at 1,000,000. */
    private static void purexMaxPower(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);

        TileEntityMachinePUREX purex = placePurex(helper, pos);
        purex.purexModule.recipe = "purex.zirnoxnu"; // 1000 HE/t -> 100k, floored to the 1M base
        tick(helper, pos, purex);
        helper.assertTrue(purex.maxPower == TileEntityMachinePUREX.BASE_MAX_POWER,
                "A cheap recipe must keep the 1,000,000 HE base ceiling, got " + purex.maxPower);

        purex.purexModule.recipe = "purex.schrabzirnox"; // 50,000 HE/t -> 5M
        tick(helper, pos, purex);
        helper.assertTrue(purex.maxPower == 5_000_000L,
                "maxPower must track recipe.power * 100, got " + purex.maxPower);

        // dropping back to a cheap recipe must not strand power above the ceiling
        purex.power = 5_000_000L;
        purex.purexModule.recipe = "purex.zirnoxnu";
        tick(helper, pos, purex);
        helper.assertTrue(purex.maxPower >= purex.power, "maxPower must never fall below the stored power");
        helper.succeed();
    }

    /** A recipe with a fluid product fills the output tank, and that tank is offered to the network. */
    private static void purexOutputFluid(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachinePUREX purex = placePurex(helper, pos);

        purex.purexModule.recipe = "purex.icf";
        tick(helper, pos, purex);
        purex.inventory.setStackInSlot(4, new net.minecraft.world.item.ItemStack(com.hbm.items.ModItems.icf_pellet_depleted.get()));
        // 305 ticks, not 300: progress accumulates 1/300 per tick in double precision, so the sum reaches
        // 1.0 a tick or two late — the machine is not tick-exact, and the assertion should not pretend it is.
        for (int i = 0; i < 305; i++) {
            purex.power = purex.maxPower; // the ICF recipe draws 10k HE/t throughout
            tick(helper, pos, purex);
        }

        helper.assertTrue(purex.outputTanks[0].getTankType() == Fluids.HELIUM4 && purex.outputTanks[0].getFill() == 1_250,
                "The ICF recipe must leave 1250 mB of helium-4 in the product tank");
        helper.assertTrue(purex.getSendingTanks().length == 1 && purex.getSendingTanks()[0] == purex.outputTanks[0],
                "The product tank must be offered to the fluid network");
        assertOutput(helper, purex, 7, com.hbm.items.ModItems.icf_pellet_empty.get(), 1);
        helper.succeed();
    }

    /** The 13-slot layout: what each slot accepts, and what automation may reach. */
    private static void purexSlots(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachinePUREX purex = placePurex(helper, pos);
        purex.purexModule.recipe = "purex.zirnoxnu";

        var battery = new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModBatteryItems.PACKS.get(com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD).get());
        var blueprint = new net.minecraft.world.item.ItemStack(com.hbm.items.ModMachineItems.blueprint_plate.get());
        var upgrade = new net.minecraft.world.item.ItemStack(com.hbm.items.ModUpgradeItems.upgrade_speed_3.get());
        var waste = new net.minecraft.world.item.ItemStack(com.hbm.items.ModZirnoxItems.waste(ZirnoxFuel.NATURAL_URANIUM));
        var junk = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE);

        helper.assertTrue(purex.isItemValidForSlot(0, battery) && !purex.isItemValidForSlot(0, junk), "Slot 0 takes batteries only");
        helper.assertTrue(purex.isItemValidForSlot(1, blueprint) && !purex.isItemValidForSlot(1, junk), "Slot 1 takes blueprints only");
        helper.assertTrue(purex.isItemValidForSlot(2, upgrade) && !purex.isItemValidForSlot(2, junk), "Slot 2 takes upgrades only");
        helper.assertTrue(purex.isItemValidForSlot(4, waste) && !purex.isItemValidForSlot(4, junk),
                "Slot 4 takes the selected recipe's ingredients only");

        int[] faceSlots = purex.getSlotsForFace(net.minecraft.core.Direction.NORTH);
        helper.assertTrue(faceSlots.length == 9 && faceSlots[0] == 4 && faceSlots[8] == 12,
                "Automation must see slots 4..12");
        helper.assertTrue(purex.canPlaceItemThroughFace(4, waste, net.minecraft.core.Direction.NORTH)
                        && !purex.canPlaceItemThroughFace(7, waste, net.minecraft.core.Direction.NORTH),
                "Automation may only insert into the item inputs");
        helper.assertTrue(purex.canTakeItemThroughFace(7, waste, net.minecraft.core.Direction.NORTH),
                "Automation must be able to pull the outputs");

        // an unusable item in an input slot must be extractable again, or it would deadlock the machine
        purex.inventory.setStackInSlot(5, junk.copy());
        helper.assertTrue(purex.canTakeItemThroughFace(5, junk, net.minecraft.core.Direction.NORTH),
                "A clogged input slot must be extractable");
        helper.succeed();
    }

    /** The menu layout — the original slot count and coordinates. */
    private static void purexMenu(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachinePUREX purex = placePurex(helper, pos);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerMachinePUREX(0, player.getInventory(), purex);

        helper.assertTrue(menu.slots.size() == 13 + 36, "PUREX menu must have 13 machine slots + the player inventory");
        assertSlot(helper, menu, 0, 152, 81);
        assertSlot(helper, menu, 1, 35, 126);
        assertSlot(helper, menu, 2, 152, 108);
        assertSlot(helper, menu, 3, 152, 126);
        assertSlot(helper, menu, 4, 8, 90);
        assertSlot(helper, menu, 6, 44, 90);
        assertSlot(helper, menu, 7, 80, 36);
        assertSlot(helper, menu, 12, 98, 72);
        helper.assertFalse(menu.slots.get(7).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)),
                "PUREX output slots must be take-only");
        helper.succeed();
    }

    /** The 5×5 footprint and the ring of 20 network ports around it, built through the real placement path. */
    private static void purexLayout(GameTestHelper helper) {
        Fluids.init();
        var block = ModMachineBlocks.machine_purex.get();
        var clicked = new net.minecraft.core.BlockPos(4, 1, 2);
        // setPlacedBy resolves the facing from the placer's yaw (0 → NORTH) and puts the core `offset`
        // blocks along it: NORTH with offset 2 → two blocks +Z from the clicked cell.
        var core = clicked.offset(0, 0, 2);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new net.minecraft.world.item.ItemStack(block));

        TileEntityMachinePUREX purex = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityMachinePUREX.class);
        helper.assertTrue(purex != null, "PUREX controller setup failed");

        int cells = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (helper.getBlockState(core.offset(dx, 0, dz)).getBlock() == block) cells++;
            }
        }
        helper.assertTrue(cells == 25, "The PUREX footprint must fill 5x5 cells, found " + cells);

        helper.assertTrue(purex.getConPos().length == 20, "PUREX must expose 20 network ports");
        var eastEdge = helper.absolutePos(core.offset(2, 0, 0));
        helper.assertTrue(block.canConnect(helper.getLevel(), eastEdge, com.hbm.lib.ForgeDirection.EAST),
                "The east edge cell must accept a connection outward");
        helper.assertFalse(block.canConnect(helper.getLevel(), eastEdge, com.hbm.lib.ForgeDirection.WEST),
                "An edge cell must not connect back into the machine");
        helper.succeed();
    }

    private static TileEntityMachinePUREX placePurex(GameTestHelper helper, net.minecraft.core.BlockPos pos) {
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.setBlock(pos, ModMachineBlocks.machine_purex.get().defaultBlockState().setValue(BlockDummyable.META, 12));
        return dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, TileEntityMachinePUREX.class);
    }

    private static void tick(GameTestHelper helper, net.minecraft.core.BlockPos pos, TileEntityMachinePUREX purex) {
        TileEntityMachinePUREX.serverTick(helper.getLevel(), helper.absolutePos(pos), purex.getBlockState(), purex);
    }

    private static void assertOutput(GameTestHelper helper, TileEntityMachinePUREX purex, int slot,
                                     net.minecraft.world.item.Item item, int count) {
        var stack = purex.inventory.getStackInSlot(slot);
        helper.assertTrue(stack.is(item) && stack.getCount() == count,
                "PUREX output slot " + slot + " holds " + stack + ", expected " + count + "x " + item);
    }

    private static void zirnoxGoldenTick(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.machine_zirnox.get().defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityReactorZirnox reactor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, TileEntityReactorZirnox.class);
        reactor.heat = 50_000;
        reactor.isOn = true;
        reactor.carbonDioxide.setFill(14_000);
        reactor.water.setFill(32_000);
        reactor.inventory.setStackInSlot(0, new net.minecraft.world.item.ItemStack(ModZirnoxItems.active(ZirnoxFuel.NATURAL_URANIUM)));
        TileEntityReactorZirnox.serverTick(helper.getLevel(), helper.absolutePos(pos), reactor.getBlockState(), reactor);
        helper.assertTrue(reactor.pressure == 71_776, "ZIRNOX pressure formula regressed");
        helper.assertTrue(reactor.steam.getFill() == 49 && reactor.water.getFill() == 31_951,
                "ZIRNOX steam conversion regressed");
        helper.assertTrue(reactor.heat == 46_440, "ZIRNOX cooling order regressed");
        helper.succeed();
    }

    private static void zirnoxLayout(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(3, 1, 3);
        var block = ModMachineBlocks.machine_zirnox.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        helper.assertTrue(dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class) != null,
                "ZIRNOX controller setup failed");
        block.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);

        helper.assertBlockProperty(core, BlockDummyable.META, 12);
        int occupied = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean expected = dy <= 1
                            || (Math.abs(dx) <= 1 && Math.abs(dz) <= 1)
                            || (dz == 0 && Math.abs(dx) == 2);
                    boolean present = helper.getBlockState(core.offset(dx, dy, dz)).getBlock() == block;
                    helper.assertTrue(present == expected,
                            "Incorrect ZIRNOX cell at " + dx + "," + dy + "," + dz);
                    if (present) occupied++;
                }
            }
        }
        helper.assertTrue(occupied == 83, "ZIRNOX must contain exactly 83 cells, found " + occupied);
        helper.assertBlockProperty(core.offset(2, 1, 0), BlockDummyable.META, 7);
        helper.assertBlockProperty(core.offset(2, 3, 0), BlockDummyable.META, 7);
        helper.assertBlockProperty(core.offset(-2, 1, 0), BlockDummyable.META, 7);
        helper.assertBlockProperty(core.offset(-2, 3, 0), BlockDummyable.META, 7);
        helper.assertBlockProperty(core.above(4), BlockDummyable.META, 7);

        TileEntityReactorZirnox reactor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class);
        var ports = reactor.getConPos();
        helper.assertTrue(ports.size() == 4, "ZIRNOX must expose four lateral duct ports");
        helper.assertTrue(ports.stream().anyMatch(p -> p.getPos().equals(helper.absolutePos(core.offset(3, 1, 0)))
                        && p.getDir() == com.hbm.lib.ForgeDirection.EAST), "Missing lower east port");
        helper.assertTrue(ports.stream().anyMatch(p -> p.getPos().equals(helper.absolutePos(core.offset(3, 3, 0)))
                        && p.getDir() == com.hbm.lib.ForgeDirection.EAST), "Missing upper east port");
        helper.assertTrue(ports.stream().anyMatch(p -> p.getPos().equals(helper.absolutePos(core.offset(-3, 1, 0)))
                        && p.getDir() == com.hbm.lib.ForgeDirection.WEST), "Missing lower west port");
        helper.assertTrue(ports.stream().anyMatch(p -> p.getPos().equals(helper.absolutePos(core.offset(-3, 3, 0)))
                        && p.getDir() == com.hbm.lib.ForgeDirection.WEST), "Missing upper west port");
        var lowerEastCell = helper.absolutePos(core.offset(2, 1, 0));
        helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), lowerEastCell.getX(), lowerEastCell.getY(),
                lowerEastCell.getZ(), com.hbm.lib.ForgeDirection.EAST), "Lower east shell must connect to its external duct");
        helper.assertFalse(block.canConnect(Fluids.WATER, helper.getLevel(), lowerEastCell.getX(), lowerEastCell.getY(),
                lowerEastCell.getZ(), com.hbm.lib.ForgeDirection.NORTH), "Port must not connect through the wrong shell face");
        helper.succeed();
    }

    private static void zirnoxWorldTick(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(3, 1, 3);
        var block = ModMachineBlocks.machine_zirnox.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityReactorZirnox reactor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class);
        helper.assertTrue(reactor != null, "ZIRNOX controller setup failed");
        block.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);
        var rod = new net.minecraft.world.item.ItemStack(ModZirnoxItems.active(ZirnoxFuel.NATURAL_URANIUM));
        reactor.inventory.setStackInSlot(0, rod);
        reactor.isOn = true;

        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.heat > 0, "Registered ZIRNOX world ticker did not generate heat");
            helper.assertTrue(((com.hbm.items.machine.ItemZirnoxRod) rod.getItem()).getLife(rod) > 0,
                    "Registered ZIRNOX world ticker did not consume rod life");
        });
    }

    private static void zirnoxSteamOutput(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(3, 1, 3);
        var reactorBlock = ModMachineBlocks.machine_zirnox.get();
        helper.setBlock(core, reactorBlock.defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityReactorZirnox reactor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class);
        helper.assertTrue(reactor != null, "ZIRNOX controller setup failed");
        reactorBlock.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);

        var reactorDuctPos = core.offset(3, 1, 0);
        var tankDuctPos = core.offset(4, 1, 0);
        helper.setBlock(reactorDuctPos, ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState());
        helper.setBlock(tankDuctPos, ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState());
        var reactorDuct = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, reactorDuctPos, com.hbm.tileentity.network.TileEntityFluidDuctNT.class);
        var tankDuct = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, tankDuctPos, com.hbm.tileentity.network.TileEntityFluidDuctNT.class);
        reactorDuct.setFluidType(Fluids.SUPERHOTSTEAM);
        tankDuct.setFluidType(Fluids.SUPERHOTSTEAM);

        var tankCore = core.offset(6, 1, 1);
        helper.setBlock(tankCore, ModMachineBlocks.machine_fluidtank.get().defaultBlockState()
                .setValue(BlockDummyable.META, 13));
        var tank = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, tankCore, com.hbm.tileentity.machine.TileEntityMachineFluidTank.class);
        helper.assertTrue(tank != null, "Fluid tank controller setup failed");
        tank.mode = 1; // Buffer mode owns the network node at its physical port cells.
        tank.tank.setTankType(Fluids.SUPERHOTSTEAM);

        reactor.heat = 50_000;
        reactor.carbonDioxide.setFill(14_000);
        reactor.water.setFill(32_000);

        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.water.getFill() < 32_000,
                    "ZIRNOX did not consume water for steam production");
            helper.assertTrue(tank.tank.getFill() > 0,
                    "ZIRNOX steam did not travel through the ducts into the tank");
            helper.assertTrue(tank.tank.getTankType() == Fluids.SUPERHOTSTEAM,
                    "ZIRNOX steam changed type while entering the tank");
        });
    }

    private static void zirnoxMeltdown(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(3, 1, 3);
        var block = ModMachineBlocks.machine_zirnox.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityReactorZirnox reactor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class);
        helper.assertTrue(reactor != null, "ZIRNOX controller setup failed");
        block.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);
        reactor.heat = TileEntityReactorZirnox.MAX_HEAT + 1;
        TileEntityReactorZirnox.serverTick(helper.getLevel(), helper.absolutePos(core), reactor.getBlockState(), reactor);

        helper.assertTrue(helper.getBlockState(core).getBlock() == ModMachineBlocks.zirnox_destroyed.get(),
                "Meltdown did not form the destroyed ZIRNOX core");
        var debris = helper.getLevel().getEntitiesOfClass(com.hbm.entity.projectile.EntityZirnoxDebris.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(core)).inflate(16.0D));
        helper.assertTrue(debris.size() == 72, "Expected 72 ZIRNOX debris entities, found " + debris.size());
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.EXCHANGER, 2);
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.CONCRETE, 20);
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.BLANK, 20);
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.ELEMENT, 10);
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.GRAPHITE, 10);
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.SHRAPNEL, 10);
        helper.succeed();
    }

    private static void assertDebrisCount(GameTestHelper helper,
                                          java.util.List<com.hbm.entity.projectile.EntityZirnoxDebris> debris,
                                          com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType type, int expected) {
        long count = debris.stream().filter(entity -> entity.getDebrisType() == type).count();
        helper.assertTrue(count == expected, "Expected " + expected + " " + type + " debris, found " + count);
    }

    private static void zirnoxWreckHazards(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(3, 1, 3);
        helper.setBlock(core, ModMachineBlocks.zirnox_destroyed.get().defaultBlockState().setValue(BlockDummyable.META, 12));
        var wreck = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, com.hbm.tileentity.machine.TileEntityZirnoxDestroyed.class);
        helper.assertTrue(wreck != null && wreck.isOnFire(), "Fresh ZIRNOX wreck must start burning");
        wreck.extinguish();
        helper.assertFalse(wreck.isOnFire(), "ZIRNOX wreck extinguish did not persist");
        helper.setBlock(core.above(), com.hbm.blocks.ModNuclearBlocks.gas_meltdown.get().defaultBlockState());
        helper.assertBlockPresent(com.hbm.blocks.ModNuclearBlocks.gas_meltdown.get(), core.above());
        helper.succeed();
    }

    private static void check(GameTestHelper helper, com.hbm.inventory.fluid.FluidType input, int inputAmount,
                              com.hbm.inventory.fluid.FluidType output, int outputAmount, int heat) {
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(input);
        helper.assertTrue(recipe != null, "Missing cooling recipe for " + input.getName());
        helper.assertTrue(recipe.inputAmount() == inputAmount && recipe.output() == output
                        && recipe.outputAmount() == outputAmount && recipe.heatEnergy() == heat,
                "Incorrect cooling recipe for " + input.getName());
    }
}
