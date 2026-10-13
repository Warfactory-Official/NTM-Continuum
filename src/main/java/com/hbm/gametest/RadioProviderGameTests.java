package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial;
import com.hbm.tileentity.machine.TileEntityHeaterHeatex;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import com.hbm.tileentity.machine.TileEntityWatz;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKTerminal;
import com.hbm.tileentity.machine.storage.MassStorageType;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.tileentity.machine.TileEntityCargoElevator;
import com.hbm.tileentity.machine.TileEntityGeiger;
import com.hbm.items.ModMachineItems;
import com.hbm.tileentity.turret.TileEntityTurretArty;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.tileentity.turret.TileEntityTurretSentry;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import net.minecraft.world.level.block.state.BlockState;

/** Machine-side Redstone-over-Radio contracts; the parent task wires this suite into the test bootstrap. */
public final class RadioProviderGameTests {
    private RadioProviderGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function DESCRIPTORS = TESTS.function(
            "radio_provider_descriptors", 40, () -> RadioProviderGameTests::descriptors);
    public static final GameTests.Function VALUES = TESTS.function(
            "radio_provider_values", 40, () -> RadioProviderGameTests::values);
    public static final GameTests.Function INTERACTIVE = TESTS.function(
            "radio_provider_interactive", 40, () -> RadioProviderGameTests::interactive);
    public static final GameTests.Function PARSER = TESTS.function(
            "radio_provider_parser", 20, () -> RadioProviderGameTests::parser);

    public static void init() {}

    private static BlockPos pos(int index) {
        return new BlockPos(2 + index * 2, 1, 2);
    }

    private static BlockState state() {
        return Blocks.STONE.defaultBlockState();
    }

    private static BlockState state(
            Block block) {
        return block.defaultBlockState();
    }

    private static void assertInfo(GameTestHelper helper, IRORInfo info, String... expected) {
        helper.assertTrue(info instanceof IRORValueProvider,
                "ROR descriptor target is not a value provider: " + info.getClass().getSimpleName());
        helper.assertTrue(Arrays.equals(expected, info.getFunctionInfo()),
                "Unexpected ROR descriptor list: " + Arrays.toString(info.getFunctionInfo()));
    }

    private static void assertInteractive(GameTestHelper helper, IRORInfo info, String... expected) {
        helper.assertTrue(info instanceof IRORInteractive,
                "ROR descriptor target is not interactive: " + info.getClass().getSimpleName());
        helper.assertTrue(Arrays.equals(expected, info.getFunctionInfo()),
                "Unexpected ROR descriptor list: " + Arrays.toString(info.getFunctionInfo()));
    }

    private static void descriptors(GameTestHelper helper) {
        assertInfo(helper, new TileEntityMachineBattery(pos(0), state(ModMachineBlocks.machine_battery.get())),
                "VAL:fill", "VAL:fillpercent", "VAL:delta", "FUN:setmode!mode (0-3)",
                "FUN:setmode!mode:fallback (0-3)", "FUN:setredmode!mode (0-3)",
                "FUN:setredmode!mode:fallback (0-3)", "FUN:setpriority!priority (0-2)");
        assertInfo(helper, new TileEntityBatterySocket(pos(1), state(ModBatteryBlocks.machine_battery_socket.get())),
                "VAL:fill", "VAL:fillpercent", "VAL:delta", "FUN:setmode!mode (0-3)",
                "FUN:setmode!mode:fallback (0-3)", "FUN:setredmode!mode (0-3)",
                "FUN:setredmode!mode:fallback (0-3)", "FUN:setpriority!priority (0-2)");
        assertInfo(helper, new TileEntityMachineFluidTank(pos(2), state(ModMachineBlocks.machine_fluidtank.get())),
                "VAL:type", "VAL:fill", "VAL:fillpercent", "FUN:setmode!mode (0-3)",
                "FUN:setmode!mode:fallback (0-3)");
        assertInfo(helper, new TileEntityMachineChemicalPlant(pos(3), state(ModMachineBlocks.machine_chemical_plant.get())),
                "VAL:progress", "VAL:recipe", "VAL:active", "FUN:setrecipe!name");
        assertInfo(helper, new TileEntityMachineAssemblyMachine(pos(4), state(ModMachineBlocks.machine_assembly_machine.get())),
                "VAL:progress", "VAL:recipe", "VAL:active", "FUN:setrecipe!name");
        assertInfo(helper, new TileEntityMachineChemicalFactory(pos(5), state(ModMachineBlocks.machine_chemical_factory.get())),
                "VAL:progress1", "VAL:progress2", "VAL:progress3", "VAL:progress4",
                "VAL:recipe1", "VAL:recipe2", "VAL:recipe3", "VAL:recipe4", "VAL:anyactive",
                "VAL:active1", "VAL:active2", "VAL:active3", "VAL:active4");
        assertInfo(helper, new TileEntityMachineAssemblyFactory(pos(6), state(ModMachineBlocks.machine_assembly_factory.get())),
                "VAL:progress1", "VAL:progress2", "VAL:progress3", "VAL:progress4",
                "VAL:recipe1", "VAL:recipe2", "VAL:recipe3", "VAL:recipe4", "VAL:anyactive",
                "VAL:active1", "VAL:active2", "VAL:active3", "VAL:active4");
        assertInfo(helper, new TileEntityMachineCombustionEngine(pos(7), state(ModMachineBlocks.machine_combustion_engine.get())),
                "VAL:state", "VAL:throttle", "VAL:power", "VAL:fuel", "VAL:efficiency",
                "FUN:setstate!state", "FUN:setthrottle!throttle");
        assertInfo(helper, new TileEntityMachineTurbineGas(pos(8), state(ModMachineBlocks.machine_turbine_gas.get())),
                "VAL:turbinepercent", "VAL:turbinespeed", "VAL:output", "VAL:state", "VAL:automode",
                "VAL:temp", "VAL:power", "VAL:fuel", "VAL:lubricant", "VAL:water", "VAL:steam",
                "FUN:setauto!auto", "FUN:setthrottle!percent", "FUN:setstate!state");
        assertInfo(helper, new TileEntityMachineIndustrialTurbine(pos(9), state(ModMachineBlocks.machine_industrial_turbine.get())), "VAL:output", "VAL:flywheel");
        assertInfo(helper, new TileEntityPileLoader(pos(34), state(ModPileBlocks.pile_device.get())),
                "VAL:meta", "VAL:depletion", "VAL:deppercent", "VAL:lifetime", "VAL:temp");
        assertInfo(helper, new TileEntityMachinePUREX(pos(10), state(ModMachineBlocks.machine_purex.get())), "VAL:progress", "VAL:recipe", "VAL:active");
        assertInfo(helper, new TileEntityWatz(pos(11), state(ModMachineBlocks.watz.get())),
                "VAL:heat", "VAL:flux", "VAL:mud", "VAL:coolant_hot", "VAL:coolant_cold");
        assertInfo(helper, new TileEntityFusionTorus(pos(12), state(ModMachineBlocks.fusion_torus.get())), "VAL:plasma", "VAL:consumption");
        assertInfo(helper, new TileEntityFusionPlasmaForge(pos(13), state(ModMachineBlocks.fusion_plasma_forge.get())),
                "VAL:progress", "VAL:recipe", "VAL:active", "VAL:booster", "VAL:plasma");
        assertInfo(helper, new TileEntityReactorZirnox(pos(14), state(ModMachineBlocks.machine_zirnox.get())),
                "VAL:heat", "VAL:pressure", "VAL:water", "VAL:steam", "VAL:co2", "VAL:state",
                "FUN:setState!active (0 or 1)", "FUN:ventCO2");
        assertInfo(helper, new TileEntityHeatBoiler(pos(15), state(ModMachineBlocks.machine_boiler.get())), "VAL:input", "VAL:output");
        assertInfo(helper, new TileEntityHeatBoilerIndustrial(pos(16), state(ModMachineBlocks.machine_industrial_boiler.get())), "VAL:input", "VAL:output");
        assertInfo(helper, new TileEntityHeaterHeatex(pos(17), state(ModMachineBlocks.heater_heatex.get())), "VAL:hotfluid", "VAL:coldfluid", "VAL:heat");
        assertInfo(helper, new TileEntityHeaterOilburner(pos(18), state(ModMachineBlocks.heater_oilburner.get())),
                "VAL:heat", "VAL:fuel", "VAL:burnrate", "VAL:state", "FUN:setstate!active", "FUN:setburnrate!rate");
        assertInfo(helper, new TileEntityMassStorage(MassStorageType.IRON, pos(19), state(ModStorageBlocks.mass_storage_iron.get())),
                "VAL:type", "VAL:fill", "VAL:fillpercent", "FUN:toggleoutput");
        assertInfo(helper, new BlockEntityRBMKControlManual(pos(20), state(ModRBMKBlocks.rbmk_control.get())),
                "VAL:extraction", "FUN:setrods!percent", "FUN:extendrods!percent");
        assertInfo(helper, new BlockEntityRBMKControlAuto(pos(21), state(ModRBMKBlocks.rbmk_control_auto.get())), "VAL:extraction");
        assertInfo(helper, new BlockEntityRBMKHeater(pos(22), state(ModRBMKBlocks.rbmk_heater.get())), "VAL:in", "VAL:out");
        assertInfo(helper, new BlockEntityRBMKRod(pos(23), state(ModRBMKBlocks.rbmk_fuel_rod.get())),
                "VAL:columnheat", "VAL:rodheat", "VAL:depletion", "VAL:xenon", "VAL:fastflux", "VAL:slowflux", "VAL:flux");
        assertInfo(helper, new BlockEntityRBMKBoiler(pos(24), state(ModRBMKBlocks.rbmk_boiler.get())), "VAL:feed", "VAL:steam", "VAL:consumption");
        assertInfo(helper, new TileEntityGeiger(pos(25), state(ModMachineBlocks.geiger.get())), "VAL:rad");
        assertInfo(helper, new TileEntityCargoElevator(pos(31), state(ModMachineBlocks.cargo_elevator.get())),
                "VAL:extension", "FUN:setextension!extension");
        assertInteractive(helper, new BlockEntityRBMKTerminal(pos(30), state(ModRBMKBlocks.rbmk_terminal.get())),
                "FUN:clear", "FUN:write!text", "FUN:set<line#>!text", "FUN:submit!command");
        assertInteractive(helper, new TileEntityTurretSentry(pos(25), state(ModTurretBlocks.turret_sentry.get())),
                "FUN:setActive!active (0 or 1)", "FUN:targetPlayers!enabled (0 or 1)",
                "FUN:targetAnimals!enabled (0 or 1)", "FUN:targetMobs!enabled (0 or 1)",
                "FUN:targetMachines!enabled (0 or 1)", "FUN:addWhitelist!name", "FUN:removeWhitelist!name");
        assertInteractive(helper, new TileEntityPileControl(pos(35), state(ModPileBlocks.pile_device.get())),
                "FUN:setrods!percent", "FUN:extendrods!percent");
        assertInteractive(helper, new TileEntityTurretArty(pos(26), state(ModTurretBlocks.turret_arty.get())),
                "FUN:setActive!active (0 or 1)", "FUN:targetPlayers!enabled (0 or 1)",
                "FUN:targetAnimals!enabled (0 or 1)", "FUN:targetMobs!enabled (0 or 1)",
                "FUN:targetMachines!enabled (0 or 1)", "FUN:addWhitelist!name", "FUN:removeWhitelist!name",
                "FUN:enqueue!x:y:z");
        helper.succeed();
    }

    private static void values(GameTestHelper helper) {
        TileEntityMachineBattery battery = new TileEntityMachineBattery(pos(0), state(ModMachineBlocks.machine_battery.get()));
        battery.power = 250_000L;
        battery.delta = 321L;
        helper.assertTrue("250000".equals(battery.provideRORValue("VAL:fill")), "Battery fill changed");
        helper.assertTrue("25".equals(battery.provideRORValue("VAL:fillpercent")), "Battery percentage changed");
        helper.assertTrue("321".equals(battery.provideRORValue("VAL:delta")), "Battery delta changed");

        TileEntityMachineFluidTank tank = new TileEntityMachineFluidTank(pos(1), state(ModMachineBlocks.machine_fluidtank.get()));
        tank.tank.setFill(12_345);
        helper.assertTrue("12345".equals(tank.provideRORValue("VAL:fill")), "Fluid tank fill changed");
        helper.assertTrue("4".equals(tank.provideRORValue("VAL:fillpercent")), "Fluid tank percentage changed");

        TileEntityMachineChemicalFactory factory = new TileEntityMachineChemicalFactory(pos(2), state(ModMachineBlocks.machine_chemical_factory.get()));
        factory.chemplantModule[0].progress = 0.126D;
        factory.chemplantModule[0].recipe = "recipe.zero";
        factory.didProcess[2] = true;
        helper.assertTrue("13".equals(factory.provideRORValue("VAL:progress1")), "Factory progress rounding changed");
        helper.assertTrue("recipe.zero".equals(factory.provideRORValue("VAL:recipe1")), "Factory recipe changed");
        helper.assertTrue("1".equals(factory.provideRORValue("VAL:anyactive")), "Factory anyactive changed");
        helper.assertTrue("1".equals(factory.provideRORValue("VAL:active3")), "Factory lane active changed");

        TileEntityReactorZirnox zirnox = new TileEntityReactorZirnox(pos(3), state(ModMachineBlocks.machine_zirnox.get()));
        zirnox.heat = 50_000;
        zirnox.pressure = 10_000;
        helper.assertTrue("410".equals(zirnox.provideRORValue("VAL:heat")), "ZIRNOX heat conversion changed");
        helper.assertTrue("3".equals(zirnox.provideRORValue("VAL:pressure")), "ZIRNOX pressure conversion changed");

        BlockEntityRBMKRod rod = new BlockEntityRBMKRod(pos(4), state(ModRBMKBlocks.rbmk_fuel_rod.get()));
        rod.heat = 1234.9D;
        rod.lastFluxQuantity = 100D;
        rod.lastFluxRatio = 0.25D;
        helper.assertTrue("1234".equals(rod.provideRORValue("VAL:columnheat")), "RBMK column heat changed");
        helper.assertTrue("25".equals(rod.provideRORValue("VAL:fastflux")), "RBMK fast flux changed");
        helper.assertTrue("75".equals(rod.provideRORValue("VAL:slowflux")), "RBMK slow flux changed");
        helper.assertTrue("100".equals(rod.provideRORValue("VAL:flux")), "RBMK total flux changed");
        helper.succeed();
    }

    private static void interactive(GameTestHelper helper) {
        TileEntityMachineBattery battery = new TileEntityMachineBattery(pos(0), state(ModMachineBlocks.machine_battery.get()));
        battery.runRORFunction("FUN:setmode", new String[]{"2"});
        battery.runRORFunction("FUN:setmode", new String[]{"2", "3"});
        battery.runRORFunction("FUN:setredmode", new String[]{"1"});
        battery.runRORFunction("FUN:setpriority", new String[]{"2"});
        helper.assertTrue(battery.redLow == 3 && battery.redHigh == 1
                        && battery.getPriority() == IEnergyReceiverMK2.ConnectionPriority.HIGH,
                "Battery ROR mode or priority command changed");

        TileEntityMachineAssemblyMachine assembly = new TileEntityMachineAssemblyMachine(pos(1), state(ModMachineBlocks.machine_assembly_machine.get()));
        assembly.runRORFunction("FUN:setrecipe", new String[]{"ass.plate"});
        helper.assertTrue("ass.plate".equals(assembly.assemblerModule.recipe)
                        && assembly.assemblerModule.restrictedMode,
                "Assembly ROR recipe selection lost restricted mode");

        TileEntityMachineTurbineGas turbine = new TileEntityMachineTurbineGas(pos(2), state(ModMachineBlocks.machine_turbine_gas.get()));
        turbine.runRORFunction("FUN:setthrottle", new String[]{"125"});
        helper.assertTrue(turbine.powerSliderPos == 60, "Gas-turbine throttle was not clamped to 100");
        turbine.runRORFunction("FUN:setthrottle", new String[]{"not-a-number"});
        helper.assertTrue(turbine.powerSliderPos == 60, "Gas-turbine invalid throttle did not remain a no-op");

        TileEntityHeaterOilburner oilburner = new TileEntityHeaterOilburner(pos(3), state(ModMachineBlocks.heater_oilburner.get()));
        oilburner.runRORFunction("FUN:setburnrate", new String[]{"10"});
        oilburner.runRORFunction("FUN:setstate", new String[]{"1"});
        helper.assertTrue(oilburner.setting == 10 && oilburner.isOn, "Oilburner ROR command changed");

        BlockEntityRBMKControlManual control = new BlockEntityRBMKControlManual(pos(4),
                state(ModRBMKBlocks.rbmk_control.get()));
        control.runRORFunction("FUN:setrods", new String[]{"50"});
        control.runRORFunction("FUN:extendrods", new String[]{"-100"});
        helper.assertTrue(control.targetLevel == 0D, "RBMK rod extension command changed");

        TileEntityReactorZirnox zirnox = new TileEntityReactorZirnox(pos(5), state(ModMachineBlocks.machine_zirnox.get()));
        zirnox.carbonDioxide.setFill(2_000);
        zirnox.runRORFunction("FUN:ventCO2", new String[0]);
        helper.assertTrue(zirnox.carbonDioxide.getFill() == 1_000, "ZIRNOX vent command changed");
        zirnox.redstonePowered = true;
        zirnox.isOn = false;
        zirnox.runRORFunction("FUN:setState", new String[]{"1"});
        helper.assertTrue(!zirnox.isOn, "ZIRNOX ROR ignored redstone lockout");

        TileEntityMassStorage storage = new TileEntityMassStorage(MassStorageType.IRON, pos(6), state(ModStorageBlocks.mass_storage_iron.get()));
        storage.runRORFunction("FUN:toggleoutput", new String[0]);
        helper.assertTrue(storage.output, "Mass-storage output toggle changed");

        TileEntityCargoElevator lift =
                new TileEntityCargoElevator(pos(12), state(ModMachineBlocks.cargo_elevator.get()));
        lift.height = 4;
        lift.runRORFunction("FUN:setextension", new String[]{"3"});
        helper.assertTrue(lift.targetExtension == 3, "Elevator ROR extension target changed");
        expectException(helper, () -> lift.runRORFunction("FUN:setextension", new String[]{"5"}),
                IRORInteractive.EX_FORMAT);
        lift.extension = 2.75D;
        helper.assertTrue("2".equals(lift.provideRORValue("VAL:extension")),
                "Elevator reports whole courses, so a partly-raised platform reads as the one below");

        BlockEntityRBMKTerminal terminal =
                new BlockEntityRBMKTerminal(pos(11), state(ModRBMKBlocks.rbmk_terminal.get()));
        terminal.runRORFunction("FUN:write", new String[]{"hello", "world"});
        helper.assertTrue("hello world".equals(terminal.history[0]),
                "Terminal write must join its parameters back into one line");
        terminal.runRORFunction("FUN:set3", new String[]{"third"});
        helper.assertTrue("third".equals(terminal.history[2]),
                "Terminal set<line#> is 1-based, so set3 writes history[2]");
        expectException(helper, () -> terminal.runRORFunction("FUN:set0", new String[]{"x"}),
                IRORInteractive.EX_FORMAT);
        expectException(helper, () -> terminal.runRORFunction("FUN:set18", new String[]{"x"}),
                IRORInteractive.EX_FORMAT);
        terminal.runRORFunction("FUN:clear", new String[0]);
        helper.assertTrue(terminal.history[0].isEmpty() && terminal.history[2].isEmpty(),
                "Terminal clear must empty the whole scrollback");

        TileEntityGeiger geiger = new TileEntityGeiger(pos(10), state(ModMachineBlocks.geiger.get()));
        geiger.ticker = 3.2F;
        helper.assertTrue("4".equals(geiger.provideRORValue("VAL:rad")),
                "Geiger rounds its reading up, so 3.2 must read as 4");

        TileEntityTurretSentry sentry = new TileEntityTurretSentry(pos(7), state(ModTurretBlocks.turret_sentry.get()));
        sentry.runRORFunction("FUN:setActive", new String[]{"1"});
        sentry.runRORFunction("FUN:targetPlayers", new String[]{"1"});
        sentry.runRORFunction("FUN:targetMobs", new String[]{"0"});
        sentry.runRORFunction("FUN:targetMachines", new String[]{"0"});
        helper.assertTrue(sentry.isOn && sentry.targetPlayers && !sentry.targetMobs && !sentry.targetMachines,
                "Turret ROR toggle command changed");
        expectException(helper, () -> sentry.runRORFunction("FUN:setActive", new String[]{"2"}),
                IRORInteractive.EX_FORMAT);

        sentry.runRORFunction("FUN:addWhitelist", new String[]{"Steve"});
        helper.assertTrue(sentry.getWhitelist() == null, "Turret ROR wrote a whitelist with no chip installed");
        sentry.setItem(TileEntityTurretBaseNT.SLOT_CHIP, new ItemStack(ModMachineItems.turret_chip.get()));
        sentry.runRORFunction("FUN:addWhitelist", new String[]{"Steve"});
        helper.assertTrue(sentry.getWhitelist() != null && sentry.getWhitelist().contains("Steve"),
                "Turret ROR whitelist addition changed");
        sentry.runRORFunction("FUN:removeWhitelist", new String[]{"Steve"});
        helper.assertTrue(sentry.getWhitelist() == null, "Turret ROR whitelist removal changed");

        TileEntityTurretArty arty = new TileEntityTurretArty(pos(8), state(ModTurretBlocks.turret_arty.get()));
        arty.runRORFunction("FUN:setActive", new String[]{"1"});
        helper.assertTrue(arty.isOn, "Artillery ROR lost the inherited turret commands");
        arty.runRORFunction("FUN:enqueue", new String[]{"" + (pos(8).getX() + 20), "1"});
        helper.assertTrue(arty.queuedTargets() == 0, "Artillery ROR enqueued a fire mission without a Z");
        arty.runRORFunction("FUN:enqueue",
                new String[]{"" + (pos(8).getX() + 20), "1", "" + pos(8).getZ()});
        helper.assertTrue(arty.queuedTargets() == 1, "Artillery ROR fire mission changed");
        helper.succeed();
    }

    private static void parser(GameTestHelper helper) {
        helper.assertTrue("setmode".equals(IRORInteractive.getCommand("setmode!2:3")),
                "ROR command extraction changed");
        helper.assertTrue(Arrays.equals(new String[]{"2", "3"}, IRORInteractive.getParams("setmode!2:3")),
                "ROR parameter extraction changed");
        helper.assertTrue(IRORInteractive.parseInt("1.6", 0, 2) == 2,
                "ROR decimal integer parsing changed");
        expectException(helper, () -> IRORInteractive.getCommand(null), IRORInteractive.EX_NULL);
        expectException(helper, () -> IRORInteractive.getCommand("a!b!c"), IRORInteractive.EX_NAME);
        expectException(helper, () -> IRORInteractive.parseInt("bad", 0, 1), IRORInteractive.EX_FORMAT);
        helper.succeed();
    }

    private static void expectException(GameTestHelper helper, Runnable action, String message) {
        boolean thrown = false;
        try {
            action.run();
        } catch (RuntimeException exception) {
            thrown = true;
            helper.assertTrue(message.equals(exception.getMessage()),
                    "Wrong ROR exception: " + exception.getMessage());
        }
        helper.assertTrue(thrown, "Expected ROR exception: " + message);
    }
}
