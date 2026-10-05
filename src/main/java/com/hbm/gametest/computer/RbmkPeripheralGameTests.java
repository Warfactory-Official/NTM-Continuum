package com.hbm.gametest.computer;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.compat.computercraft.rbmk.CraneConsolePeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKBoilerPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKConsolePeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKControlManualPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKControlPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKCoolerPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKGaugePeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKGraphPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKHeaterPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKIndicatorPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKKeyPadPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKLeverPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKNumitronPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKOutgasserPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKRodPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKTerminalPeripheral;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModRBMKItems;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKCooler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGraph;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKOutgasser;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKTerminal;
import com.hbm.tileentity.machine.rbmk.RBMKColor;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.Map;

/** Peripheral GameTests for {@link com.hbm.compat.computercraft.rbmk.RbmkRows}; registered only with CC: Tweaked loaded. */
public final class RbmkPeripheralGameTests {

    private RbmkPeripheralGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function FUEL_ROD = TESTS.function(
            "cc_rbmk_fuel_rod", 40, () -> RbmkPeripheralGameTests::fuelRod);
    public static final GameTests.Function CONTROL_ROD = TESTS.function(
            "cc_rbmk_control_rod", 40, () -> RbmkPeripheralGameTests::controlRod);
    public static final GameTests.Function CONTROL_AUTO = TESTS.function(
            "cc_rbmk_control_auto", 40, () -> RbmkPeripheralGameTests::controlAuto);
    public static final GameTests.Function BOILER = TESTS.function(
            "cc_rbmk_boiler", 40, 2, () -> RbmkPeripheralGameTests::boiler);
    public static final GameTests.Function COOLER = TESTS.function(
            "cc_rbmk_cooler", 40, 2, () -> RbmkPeripheralGameTests::cooler);
    public static final GameTests.Function HEATER = TESTS.function(
            "cc_rbmk_heater", 40, 2, () -> RbmkPeripheralGameTests::heater);
    public static final GameTests.Function OUTGASSER = TESTS.function(
            "cc_rbmk_outgasser", 40, 2, () -> RbmkPeripheralGameTests::outgasser);
    public static final GameTests.Function CONSOLE = TESTS.function(
            "cc_rbmk_console", 60, 3, () -> RbmkPeripheralGameTests::console);
    public static final GameTests.Function CRANE = TESTS.function(
            "cc_rbmk_crane", 40, 3, () -> RbmkPeripheralGameTests::crane);
    public static final GameTests.Function TERMINAL = TESTS.function(
            "cc_rbmk_terminal", 40, () -> RbmkPeripheralGameTests::terminal);
    public static final GameTests.Function NUMITRON = TESTS.function(
            "cc_rbmk_numitron", 40, () -> RbmkPeripheralGameTests::numitron);
    public static final GameTests.Function LEVER = TESTS.function(
            "cc_rbmk_lever", 40, () -> RbmkPeripheralGameTests::lever);
    public static final GameTests.Function KEYPAD = TESTS.function(
            "cc_rbmk_keypad", 40, () -> RbmkPeripheralGameTests::keypad);
    public static final GameTests.Function INDICATOR = TESTS.function(
            "cc_rbmk_indicator", 40, () -> RbmkPeripheralGameTests::indicator);
    public static final GameTests.Function GRAPH = TESTS.function(
            "cc_rbmk_graph", 40, () -> RbmkPeripheralGameTests::graph);
    public static final GameTests.Function GAUGE = TESTS.function(
            "cc_rbmk_gauge", 40, () -> RbmkPeripheralGameTests::gauge);

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    // ---- columns ----

    private static void fuelRod(GameTestHelper helper) {
        // a ReaSim moderated channel: the rod row covers the subclass, and 1.7 reports the block's moderation
        helper.setBlock(POS, ModRBMKBlocks.rbmk_fuel_rod_reasim_moderated.get().defaultBlockState());
        RBMKRodPeripheral rod = PeripheralTestSupport.expect(helper, POS, Direction.NORTH,
                RBMKRodPeripheral.class, "rbmk_fuel_rod");
        helper.assertTrue("N/A".equals(rod.getDepletion()[0]), "getDepletion without a rod must be N/A");
        BlockEntityRBMKRod be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKRod.class);
        ItemStack fuel = new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get());
        be.inventory.setStackInSlot(0, fuel);
        BlockPos abs = helper.absolutePos(POS);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(fuel.getItem().getDescriptionId().equals(rod.rodType()[0]), "getType after a tick");
            helper.assertTrue(Boolean.TRUE.equals(rod.getModerated()[0]), "getModerated on a moderated channel");
            expectCoordinates(helper, rod.getCoordinates(), 0, abs);
            Object[] info = rod.getInfo();
            helper.assertTrue(info.length == 12 && Boolean.TRUE.equals(info[8]), "getInfo shape " + Arrays.toString(info));
            expectCoordinates(helper, info, 9, abs);
            helper.succeed();
        });
    }

    private static void controlRod(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_control.get().defaultBlockState());
        RBMKControlManualPeripheral rod = PeripheralTestSupport.expect(helper, POS, Direction.NORTH,
                RBMKControlManualPeripheral.class, "rbmk_control_rod");
        expectUnknownError(helper, rod.getColor(), "getColor on an undyed rod");
        BlockEntityRBMKControlManual be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKControlManual.class);
        rod.setColor(2.9D);
        helper.assertTrue(be.color == RBMKColor.VALUES[2], "setColor(2.9) truncates to 2, got " + be.color);
        rod.setColor(9);
        helper.assertTrue(be.color == RBMKColor.VALUES[4], "setColor(9) clamps to 4, got " + be.color);
        rod.setLevel(150);
        helper.assertTrue(be.targetLevel == 1D, "setLevel(150) clamps to 1, got " + be.targetLevel);
        rod.setLevel(37.5D);
        helper.assertTrue(be.targetLevel == 0.375D, "setLevel(37.5) targets 0.375, got " + be.targetLevel);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, rod.getTargetLevel(), 0) == 37.5D,
                    "getTargetLevel after a tick");
            helper.assertTrue(PeripheralTestSupport.number(helper, rod.getColor(), 0) == 4, "getColor after a tick");
            helper.succeed();
        });
    }

    private static void controlAuto(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_control_auto.get().defaultBlockState());
        RBMKControlPeripheral<?> rod = PeripheralTestSupport.expect(helper, POS, Direction.NORTH,
                RBMKControlPeripheral.class, "rbmk_control_rod");
        helper.assertFalse(rod instanceof RBMKControlManualPeripheral, "an automatic rod has no colour methods");
        BlockEntityRBMKControlAuto be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKControlAuto.class);
        rod.setLevel(50);
        helper.assertTrue(be.targetLevel == 0.5D, "setLevel(50) targets 0.5, got " + be.targetLevel);
        BlockPos abs = helper.absolutePos(POS);
        helper.runAfterDelay(2, () -> {
            // 1.7: the automatic rod recomputes its target every tick; at 20 C with no bounds set that is 0
            helper.assertTrue(PeripheralTestSupport.number(helper, rod.getTargetLevel(), 0) == 0D,
                    "the automatic rod overrides a computer's target");
            Object[] info = rod.getInfo();
            helper.assertTrue(info.length == 6, "getInfo shape " + Arrays.toString(info));
            expectCoordinates(helper, info, 3, abs);
            helper.succeed();
        });
    }

    private static void boiler(GameTestHelper helper) {
        RBMKBoilerPeripheral boiler = placeColumn(helper, ModRBMKBlocks.rbmk_boiler.get(),
                RBMKBoilerPeripheral.class, "rbmk_boiler");
        BlockEntityRBMKBoiler be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKBoiler.class);
        be.feed.setFill(1234); // 20 C boils nothing
        boiler.setSteamType(2.7D);
        helper.assertTrue(be.steam.getTankType() == Fluids.SUPERHOTSTEAM, "setSteamType(2.7) is compression 2");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, boiler.getWater(), 0) == 1234, "getWater");
            helper.assertTrue(PeripheralTestSupport.number(helper, boiler.getSteamMax(), 0) == 1_000_000, "getSteamMax");
            helper.assertTrue(PeripheralTestSupport.number(helper, boiler.getSteamType(), 0) == 2, "getSteamType");
            helper.assertTrue(boiler.getInfo().length == 9, "getInfo has nine values");
            helper.succeed();
        });
    }

    private static void cooler(GameTestHelper helper) {
        RBMKCoolerPeripheral cooler = placeColumn(helper, ModRBMKBlocks.rbmk_cooler.get(),
                RBMKCoolerPeripheral.class, "rbmk_cooler");
        BlockEntityRBMKCooler be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKCooler.class);
        be.cold.setFill(40); // under the 50 mB a cooling cycle takes
        helper.runAfterDelay(2, () -> {
            Object[] coolant = cooler.getCoolant();
            helper.assertTrue(PeripheralTestSupport.number(helper, coolant, 0) == 40
                    && PeripheralTestSupport.number(helper, coolant, 1) == 4000
                    && PeripheralTestSupport.number(helper, coolant, 3) == 4000, "getCoolant " + Arrays.toString(coolant));
            helper.succeed();
        });
    }

    private static void heater(GameTestHelper helper) {
        RBMKHeaterPeripheral heater = placeColumn(helper, ModRBMKBlocks.rbmk_heater.get(),
                RBMKHeaterPeripheral.class, "rbmk_heater");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Fluids.COOLANT.getName().equals(heater.getFillType()[0]),
                    "getFillType is the 1.7 fluid name, got " + heater.getFillType()[0]);
            helper.assertTrue(PeripheralTestSupport.number(helper, heater.getFillMax(), 0) == 16_000, "getFillMax");
            helper.assertTrue(heater.getInfo().length == 10, "getInfo has ten values");
            helper.succeed();
        });
    }

    private static void outgasser(GameTestHelper helper) {
        RBMKOutgasserPeripheral outgasser = placeColumn(helper, ModRBMKBlocks.rbmk_outgasser.get(),
                RBMKOutgasserPeripheral.class, "rbmk_outgasser");
        BlockEntityRBMKOutgasser be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKOutgasser.class);
        be.gas.setFill(321);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, outgasser.getGas(), 0) == 321, "getGas");
            helper.assertTrue(Fluids.TRITIUM.getName().equals(outgasser.getGasType()[0]), "getGasType");
            Object[] crafting = outgasser.getCrafting();
            helper.assertTrue("".equals(crafting[0]) && PeripheralTestSupport.number(helper, crafting, 1) == 0,
                    "getCrafting with an empty input " + Arrays.toString(crafting));
            helper.assertTrue(PeripheralTestSupport.number(helper, outgasser.getInfo(), 3) == Fluids.legacyId(Fluids.TRITIUM),
                    "getInfo reports the 1.7 numeric fluid id");
            helper.succeed();
        });
    }

    // ---- consoles ----

    private static void console(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_console.get().defaultBlockState());
        BlockPos rodPos = new BlockPos(4, 1, 1);
        helper.setBlock(rodPos, ModRBMKBlocks.rbmk_control.get().defaultBlockState());
        BlockEntityRBMKConsole be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKConsole.class);
        BlockEntityRBMKControlManual rod = GameTestHelpers.blockEntity(helper, rodPos, BlockEntityRBMKControlManual.class);
        BlockPos target = helper.absolutePos(rodPos);
        be.setTarget(target.getX(), target.getY(), target.getZ());
        RBMKConsolePeripheral console = PeripheralTestSupport.expect(helper, POS, Direction.NORTH,
                RBMKConsolePeripheral.class, "rbmk_console");

        // grid (7, 7) is the target itself
        helper.assertTrue(console.setColumnLevel(7, 7, 0.5D).length == 0 && rod.targetLevel == 0.5D,
                "setColumnLevel(7, 7, 0.5)");
        helper.assertTrue("No control rod found at 0,0".equals(console.setColumnLevel(0, 0, 0.5D)[0]),
                "setColumnLevel on an empty cell");
        helper.assertTrue(console.setColor(7, 7, 3).length == 0 && rod.color == RBMKColor.VALUES[3], "setColor");
        helper.assertTrue("Color 7 does not exist".equals(console.setColor(7, 7, 7)[0]), "setColor out of range");
        helper.assertTrue(console.setColorLevel(3, 0.25D).length == 0 && rod.targetLevel == 0.25D, "setColorLevel");
        helper.assertTrue("No rods for color 1 found".equals(console.setColorLevel(1, 0.9D)[0]),
                "setColorLevel without matching rods");
        helper.assertTrue(console.setLevel(4).length == 0 && rod.targetLevel == 1D, "setLevel clamps to 1");
        helper.assertTrue(console.pressAZ5().length == 0 && rod.targetLevel == 0D, "pressAZ5 drops every rod");

        // the console rescans every ten ticks; getColumnData reads that copy
        helper.runAfterDelay(15, () -> {
            Object[] pos = console.getRBMKPos();
            helper.assertTrue(pos.length == 1 && pos[0] instanceof Map<?, ?>, "getRBMKPos " + Arrays.toString(pos));
            helper.assertTrue(Integer.valueOf(target.getX()).equals(((Map<?, ?>) pos[0]).get("rbmkCenterX")),
                    "getRBMKPos centre");
            Object[] data = console.getColumnData(7, 7);
            helper.assertTrue(data.length == 1 && data[0] instanceof Map<?, ?>, "getColumnData " + Arrays.toString(data));
            Map<?, ?> column = (Map<?, ?>) data[0];
            helper.assertTrue("CONTROL".equals(column.get("type")), "column type " + column.get("type"));
            helper.assertTrue(Short.valueOf((short) 3).equals(column.get("color")), "column color " + column.get("color"));
            Object[] empty = console.getColumnData(0, 0);
            helper.assertTrue(empty.length == 1 && empty[0] == null, "getColumnData on an empty cell");
            helper.succeed();
        });
    }

    private static void crane(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_crane_console.get().defaultBlockState());
        CraneConsolePeripheral crane = PeripheralTestSupport.expect(helper, POS, Direction.NORTH,
                CraneConsolePeripheral.class, "rbmk_crane");
        helper.assertTrue("Crane not found".equals(crane.move("up")[0]), "move without a linked crane");
        helper.assertTrue("Crane not found".equals(crane.load()[0]), "load without a linked crane");
        BlockEntityCraneConsole be = GameTestHelpers.blockEntity(helper, POS, BlockEntityCraneConsole.class);
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        be.setTarget(center.getX(), center.getY(), center.getZ());
        helper.assertTrue(crane.move("up").length == 0 && Math.abs(be.posFront - 0.05D) < 1.0E-9,
                "move(\"up\") steps 0.05 forward, got " + be.posFront);
        helper.assertTrue(crane.load().length == 0, "load with a linked crane");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(be.isCraneLoading(), "load sets the hoist going down");
            Object[] rodInfo = crane.getRodInfo();
            helper.assertTrue(Boolean.FALSE.equals(rodInfo[0]) && "No rod loaded".equals(rodInfo[1]), "getRodInfo empty");
            helper.assertTrue("N/A".equals(crane.getDepletion()[0]), "getDepletion empty");
            Object[] cranePos = crane.getCranePos();
            helper.assertTrue(PeripheralTestSupport.number(helper, cranePos, 0) == be.centerX
                    && PeripheralTestSupport.number(helper, cranePos, 1) == be.centerZ,
                    "getCranePos " + Arrays.toString(cranePos));
            helper.succeed();
        });
    }

    private static void terminal(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_terminal.get().defaultBlockState());
        RBMKTerminalPeripheral terminal = PeripheralTestSupport.expect(helper, POS, Direction.UP,
                RBMKTerminalPeripheral.class, "rbmk_terminal");
        helper.assertTrue(Boolean.FALSE.equals(terminal.write("x")[0]), "write outside OC mode");
        BlockEntityRBMKTerminal be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKTerminal.class);
        terminal.enableOCMode(true);
        helper.assertTrue(be.ocMode && "Terminal ready.".equals(be.history[0]) && "OC MODE ENABLED".equals(be.history[1]),
                "enableOCMode clears and greets");
        helper.assertTrue(Boolean.TRUE.equals(terminal.writeln("hello")[0]) && "hello".equals(be.history[0]), "writeln");
        be.eval("chan test"); // 1.7: in OC mode typed input is echoed, not run
        helper.assertTrue("chan test".equals(be.history[0]) && be.channel.isEmpty(), "OC mode echoes input");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Boolean.TRUE.equals(terminal.isOCMode()[0]), "isOCMode after a tick");
            helper.assertTrue("chan test".equals(terminal.readInput()[0]), "readInput is the newest line");
            Object[] history = terminal.getAllHistory();
            helper.assertTrue(history[0] instanceof String[] lines && lines.length == 17, "getAllHistory");
            terminal.clearScreen();
            helper.assertTrue("".equals(be.history[0]), "clearScreen");
            helper.succeed();
        });
    }

    // ---- panels ----

    private static void numitron(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_numitron.get().defaultBlockState());
        RBMKNumitronPeripheral numitron = PeripheralTestSupport.expect(helper, POS, Direction.UP,
                RBMKNumitronPeripheral.class, "rbmk_numitron");
        BlockEntityRBMKNumitron be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKNumitron.class);
        numitron.setDisplayValue(1, 42.9D);
        numitron.setDisplayLabel(2, "Flux");
        helper.assertTrue(be.displays[0].value == 42 && "Flux".equals(be.displays[1].label), "setDisplayValue/Label");
        helper.assertTrue("Invalid index (1-2)".equals(numitron.setDisplayActive(3, true)[1]), "index 3 is invalid");
        boolean threw = false;
        try {
            numitron.setDisplayActiveDigits(1, 128);
        } catch (LuaException e) {
            threw = true;
        }
        helper.assertTrue(threw, "setDisplayActiveDigits(1, 128) raises an error");
        helper.runAfterDelay(2, () -> {
            Map<?, ?> info = map(helper, numitron.getDisplayInfo(1));
            helper.assertTrue(Long.valueOf(42L).equals(info.get("value")), "getDisplayInfo value " + info.get("value"));
            helper.succeed();
        });
    }

    private static void lever(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_lever.get().defaultBlockState());
        RBMKLeverPeripheral lever = PeripheralTestSupport.expect(helper, POS, Direction.UP,
                RBMKLeverPeripheral.class, "rbmk_lever");
        BlockEntityRBMKLever be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKLever.class);
        lever.setLeverLabel(2, "SCRAM");
        lever.setLeverActive(1, true);
        helper.assertTrue("SCRAM".equals(be.levers[1].label) && be.levers[0].active, "setLeverLabel/Active");
        helper.assertTrue(lever.getLeverInfo(3)[0] == null, "index 3 is invalid");
        helper.runAfterDelay(2, () -> {
            Map<?, ?> info = map(helper, lever.getLeverInfo(2));
            helper.assertTrue("SCRAM".equals(info.get("label")) && Boolean.FALSE.equals(info.get("state")),
                    "getLeverInfo " + info);
            helper.succeed();
        });
    }

    private static void keypad(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_key_pad.get().defaultBlockState());
        RBMKKeyPadPeripheral keypad = PeripheralTestSupport.expect(helper, POS, Direction.UP,
                RBMKKeyPadPeripheral.class, "rbmk_keypad");
        BlockEntityRBMKKeyPad be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKKeyPad.class);
        keypad.setKeyColor(1, -5);
        helper.assertTrue(be.keys[0].color == 0, "setKeyColor clamps to 0");
        helper.assertTrue("Key is not active".equals(keypad.pressKey(1)[1]), "pressKey on an inactive key");
        keypad.setKeyActive(1, true);
        helper.assertTrue(Boolean.TRUE.equals(keypad.pressKey(1)[0]) && be.keys[0].isPressed, "pressKey");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Boolean.TRUE.equals(keypad.getKeyPressed(1)[0]), "getKeyPressed within seven ticks");
            Map<?, ?> info = map(helper, keypad.getKeyInfo(1));
            helper.assertTrue(Integer.valueOf(0).equals(info.get("color")), "getKeyInfo color " + info.get("color"));
            helper.succeed();
        });
    }

    private static void indicator(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_indicator.get().defaultBlockState());
        RBMKIndicatorPeripheral indicator = PeripheralTestSupport.expect(helper, POS, Direction.UP,
                RBMKIndicatorPeripheral.class, "rbmk_indicator");
        BlockEntityRBMKIndicator be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKIndicator.class);
        indicator.setIndicatorBounds(6, 5, 9);
        indicator.setIndicatorColor(6, 0x1000000);
        indicator.setIndicatorLight(6, true);
        helper.assertTrue(be.indicators[5].min == 5 && be.indicators[5].max == 9
                && be.indicators[5].color == 0xffffff && be.indicators[5].light, "indicator 6 setters");
        helper.assertTrue("Invalid index (1-6)".equals(indicator.setIndicatorActive(7, true)[1]), "index 7 is invalid");
        helper.runAfterDelay(2, () -> {
            Map<?, ?> info = map(helper, indicator.getIndicatorInfo(6));
            helper.assertTrue(Long.valueOf(9L).equals(info.get("max")) && Boolean.TRUE.equals(info.get("light")),
                    "getIndicatorInfo " + info);
            helper.succeed();
        });
    }

    private static void graph(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_graph.get().defaultBlockState());
        RBMKGraphPeripheral graph = PeripheralTestSupport.expect(helper, POS, Direction.UP,
                RBMKGraphPeripheral.class, "rbmk_graph");
        BlockEntityRBMKGraph be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKGraph.class);
        graph.pushGraphValue(1, 10);
        graph.pushGraphValue(1, 20.8D);
        helper.assertTrue(be.graphs[0].values[29] == 20 && be.graphs[0].values[28] == 10, "pushGraphValue");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, graph.getGraphMax(1), 0) == 20, "getGraphMax");
            helper.assertTrue(PeripheralTestSupport.number(helper, graph.getGraphMin(1), 0) == 0, "getGraphMin");
            helper.assertTrue(PeripheralTestSupport.number(helper, graph.getGraphAvg(1), 0) == 1D,
                    "getGraphAvg averages all 30 samples");
            helper.succeed();
        });
    }

    private static void gauge(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_gauge.get().defaultBlockState());
        RBMKGaugePeripheral gauge = PeripheralTestSupport.expect(helper, POS, Direction.UP,
                RBMKGaugePeripheral.class, "rbmk_gauge");
        BlockEntityRBMKGauge be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKGauge.class);
        gauge.setGaugeValue(4, 55);
        gauge.setGaugeMin(4, -3);
        gauge.setGaugeColor(4, -1);
        helper.assertTrue(be.gauges[3].value == 55 && be.gauges[3].min == -3 && be.gauges[3].color == 0, "gauge 4 setters");
        helper.runAfterDelay(2, () -> {
            Map<?, ?> info = map(helper, gauge.getGaugeInfo(4));
            helper.assertTrue(Long.valueOf(55L).equals(info.get("value")), "getGaugeInfo value " + info.get("value"));
            helper.succeed();
        });
    }

    // ---- support ----

    /**
     * Places a full column at {@link #POS} as a player would and checks 1.7's reach: the core and the top answer
     * with the same peripheral, a middle segment does not.
     */
    private static <P extends IPeripheral> P placeColumn(GameTestHelper helper, Block block, Class<P> type, String name) {
        helper.setBlock(POS, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(POS), block.defaultBlockState(),
                GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL), new ItemStack(block));
        P core = PeripheralTestSupport.expect(helper, POS, Direction.NORTH, type, name);
        BlockPos top = POS.above(RBMKDials.getColumnHeight(helper.getLevel()));
        P atTop = PeripheralTestSupport.expect(helper, top, Direction.UP, type, name);
        helper.assertTrue(atTop == core, name + ": the column top must answer with the core's peripheral");
        PeripheralTestSupport.expectNone(helper, POS.above(1), Direction.NORTH);
        return core;
    }

    private static void expectCoordinates(GameTestHelper helper, Object[] result, int from, BlockPos abs) {
        helper.assertTrue(PeripheralTestSupport.number(helper, result, from) == abs.getX()
                        && PeripheralTestSupport.number(helper, result, from + 1) == abs.getY()
                        && PeripheralTestSupport.number(helper, result, from + 2) == abs.getZ(),
                "coordinates " + Arrays.toString(result) + ", expected " + abs);
    }

    private static void expectUnknownError(GameTestHelper helper, Object[] result, String what) {
        helper.assertTrue(result.length == 2 && result[0] == null && "unknown error".equals(result[1]),
                what + " must answer nil, \"unknown error\": " + Arrays.toString(result));
    }

    private static Map<?, ?> map(GameTestHelper helper, Object[] result) {
        helper.assertTrue(result.length == 1 && result[0] instanceof Map<?, ?>, "expected a table: " + Arrays.toString(result));
        return (Map<?, ?>) result[0];
    }
}
//?}
