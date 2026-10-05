package com.hbm.gametest.computer;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.blocks.ModPowerGenBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.dfc.ModDFCBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.compat.computercraft.machine.ChungusPeripheral;
import com.hbm.compat.computercraft.machine.CokerPeripheral;
import com.hbm.compat.computercraft.machine.CombustionEnginePeripheral;
import com.hbm.compat.computercraft.machine.CoreEmitterPeripheral;
import com.hbm.compat.computercraft.machine.CoreInjectorPeripheral;
import com.hbm.compat.computercraft.machine.CoreReceiverPeripheral;
import com.hbm.compat.computercraft.machine.CoreStabilizerPeripheral;
import com.hbm.compat.computercraft.machine.GasTurbinePeripheral;
import com.hbm.compat.computercraft.machine.IndustrialTurbinePeripheral;
import com.hbm.compat.computercraft.machine.LargeTurbinePeripheral;
import com.hbm.compat.computercraft.machine.SmallTurbinePeripheral;
import com.hbm.gametest.FoldedTestSupport;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModOilItems;
import com.hbm.items.machine.ItemLens;
import com.hbm.items.machine.ItemPistons;
import com.hbm.tileentity.machine.TileEntityChungus;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
import com.hbm.tileentity.machine.TileEntityMachineTurbine;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import com.hbm.tileentity.machine.dfc.TileEntityCoreInjector;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer;
import com.hbm.tileentity.machine.oil.TileEntityMachineCoker;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/** Peripheral GameTests for {@link com.hbm.compat.computercraft.machine.PowerRows}; registered only with CC: Tweaked loaded. */
public final class PowerPeripheralGameTests {

    private PowerPeripheralGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function SMALL_TURBINE = TESTS.function(
            "cc_small_turbine", 40, () -> PowerPeripheralGameTests::smallTurbine);
    public static final GameTests.Function LARGE_TURBINE = TESTS.function(
            "cc_large_turbine", 40, 8, () -> PowerPeripheralGameTests::largeTurbine);
    public static final GameTests.Function CHUNGUS = TESTS.function(
            "cc_chungus", 40, 24, () -> PowerPeripheralGameTests::chungus);
    public static final GameTests.Function INDUSTRIAL_TURBINE = TESTS.function(
            "cc_industrial_turbine", 40, 8, () -> PowerPeripheralGameTests::industrialTurbine);
    public static final GameTests.Function GAS_TURBINE = TESTS.function(
            "cc_gas_turbine", 40, 8, () -> PowerPeripheralGameTests::gasTurbine);
    public static final GameTests.Function DFC_EMITTER = TESTS.function(
            "cc_dfc_emitter", 40, () -> PowerPeripheralGameTests::dfcEmitter);
    public static final GameTests.Function DFC_RECEIVER = TESTS.function(
            "cc_dfc_receiver", 40, () -> PowerPeripheralGameTests::dfcReceiver);
    public static final GameTests.Function DFC_INJECTOR = TESTS.function(
            "cc_dfc_injector", 40, () -> PowerPeripheralGameTests::dfcInjector);
    public static final GameTests.Function DFC_STABILIZER = TESTS.function(
            "cc_dfc_stabilizer", 40, () -> PowerPeripheralGameTests::dfcStabilizer);
    public static final GameTests.Function COMBUSTION_ENGINE = TESTS.function(
            "cc_combustion_engine", 40, 8, () -> PowerPeripheralGameTests::combustionEngine);
    public static final GameTests.Function COKER = TESTS.function(
            "cc_coker", 40, 24, () -> PowerPeripheralGameTests::coker);

    private static final BlockPos SINGLE = new BlockPos(1, 1, 1);
    private static final BlockPos CORE = new BlockPos(3, 1, 3);
    private static final BlockPos CHUNGUS_CORE = new BlockPos(3, 1, 6);

    private static void smallTurbine(GameTestHelper helper) {
        helper.setBlock(SINGLE, ModMachineBlocks.machine_turbine.get().defaultBlockState());
        TileEntityMachineTurbine be = GameTestHelpers.blockEntity(helper, SINGLE, TileEntityMachineTurbine.class);
        be.input.setTankType(Fluids.HOTSTEAM);
        be.output.setFill(500);
        be.power = 12_345L;
        SmallTurbinePeripheral turbine = PeripheralTestSupport.expect(helper, SINGLE, Direction.NORTH,
                SmallTurbinePeripheral.class, "ntm_turbine");
        expectNumbers(helper, "getInfo", turbine.getInfo(), 0, be.input.getMaxFill(), 500, be.output.getMaxFill(), 1,
                12_345);
        expectNumbers(helper, "getFluid", turbine.getFluid(), 0, be.input.getMaxFill(), 500, be.output.getMaxFill());
        expectNumbers(helper, "getPower", turbine.getPower(), 12_345);

        Object[] set = turbine.setType(2.9D);
        helper.assertTrue(set.length == 1 && Boolean.TRUE.equals(set[0]), "setType answers {true}: " + Arrays.toString(set));
        helper.assertTrue(be.input.getTankType() == Fluids.SUPERHOTSTEAM, "setType(2.9) truncates to 2, super dense steam");
        helper.runAfterDelay(2, () -> {
            expectNumbers(helper, "getType after a tick", turbine.steamType(), 2);
            helper.succeed();
        });
    }

    private static void largeTurbine(GameTestHelper helper) {
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_large_turbine.get(), CORE);
        TileEntityMachineLargeTurbine be = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachineLargeTurbine.class);
        be.tanks[0].setTankType(Fluids.STEAM);
        be.tanks[1].setFill(700);
        be.power = 4_242L;
        LargeTurbinePeripheral turbine = PeripheralTestSupport.expect(helper, CORE, Direction.UP,
                LargeTurbinePeripheral.class, "ntm_turbine");
        expectNumbers(helper, "getInfo", turbine.getInfo(), 0, be.tanks[0].getMaxFill(), 700,
                be.tanks[1].getMaxFill(), 0, 4_242);
        expectNumbers(helper, "getPower", turbine.getPower(), 4_242);

        helper.assertTrue(turbine.setType(3).length == 0, "the large turbine's setType answers nothing");
        helper.assertTrue(be.tanks[0].getTankType() == Fluids.ULTRAHOTSTEAM, "setType(3) is ultra dense steam");
        cells(helper, CORE, turbine);
        helper.runAfterDelay(2, () -> {
            expectNumbers(helper, "getType after a tick", turbine.steamType(), 3);
            helper.succeed();
        });
    }

    private static void chungus(GameTestHelper helper) {
        FoldedTestSupport.assemble(helper, ModPowerGenBlocks.MACHINE_CHUNGUS.get(), CHUNGUS_CORE);
        TileEntityChungus be = GameTestHelpers.blockEntity(helper, CHUNGUS_CORE, TileEntityChungus.class);
        be.tanks[0].setTankType(Fluids.HOTSTEAM);
        be.tanks[1].setFill(900);
        be.powerBuffer = 31_337L;
        ChungusPeripheral turbine = PeripheralTestSupport.expect(helper, CHUNGUS_CORE, Direction.UP,
                ChungusPeripheral.class, "ntm_turbine");
        expectNumbers(helper, "getInfo", turbine.getInfo(), 0, be.tanks[0].getMaxFill(), 900,
                be.tanks[1].getMaxFill(), 1, 31_337);

        helper.assertTrue(turbine.setType(0).length == 0, "the Leviathan's setType answers nothing");
        helper.assertTrue(be.tanks[0].getTankType() == Fluids.STEAM, "setType(0) is plain steam");
        cells(helper, CHUNGUS_CORE, turbine);
        helper.runAfterDelay(2, () -> {
            expectNumbers(helper, "getType after a tick", turbine.steamType(), 0);
            // the Leviathan's buffer is the tick's output: nothing ran, so nothing
            expectNumbers(helper, "getPower after a tick", turbine.getPower(), 0);
            helper.succeed();
        });
    }

    private static void industrialTurbine(GameTestHelper helper) {
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_industrial_turbine.get(), CORE);
        TileEntityMachineIndustrialTurbine be = GameTestHelpers.blockEntity(helper, CORE,
                TileEntityMachineIndustrialTurbine.class);
        be.tanks[0].setTankType(Fluids.SUPERHOTSTEAM);
        be.tanks[1].setFill(1_100);
        be.powerBuffer = 9_000L;
        be.spin = 0.5D;
        IndustrialTurbinePeripheral turbine = PeripheralTestSupport.expect(helper, CORE, Direction.UP,
                IndustrialTurbinePeripheral.class, "ntm_turbine");
        expectNumbers(helper, "getInfo", turbine.getInfo(), 0, be.tanks[0].getMaxFill(), 1_100,
                be.tanks[1].getMaxFill(), 2, 9_000, 50);
        expectNumbers(helper, "getFlywheel", turbine.getFlywheel(), 50);
        cells(helper, CORE, turbine);
        helper.runAfterDelay(2, () -> {
            // no steam: the flywheel spins down, one ACCELERATION step per tick
            double flywheel = PeripheralTestSupport.number(helper, turbine.getFlywheel(), 0);
            helper.assertTrue(flywheel < 50, "getFlywheel after idle ticks " + flywheel);
            helper.succeed();
        });
    }

    private static void gasTurbine(GameTestHelper helper) {
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_turbine_gas.get(), CORE);
        TileEntityMachineTurbineGas be = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachineTurbineGas.class);
        be.tanks[0].setTankType(Fluids.GAS);
        be.tanks[0].setFill(1_000);
        be.tanks[1].setFill(200);
        be.tanks[2].setFill(300);
        be.tanks[3].setFill(400);
        be.power = 5_000L;
        GasTurbinePeripheral turbine = PeripheralTestSupport.expect(helper, CORE, Direction.UP,
                GasTurbinePeripheral.class, "ntm_gas_turbine");
        expectNumbers(helper, "getFluid", turbine.getFluid(), 1_000, be.tanks[0].getMaxFill(), 200,
                be.tanks[1].getMaxFill(), 300, be.tanks[2].getMaxFill(), 400, be.tanks[3].getMaxFill());
        helper.assertTrue(Fluids.GAS.getName().equals(turbine.fluidType()[0]), "getType " + turbine.fluidType()[0]);
        expectNumbers(helper, "getPower", turbine.getPower(), 5_000);
        expectNumbers(helper, "getInfo", turbine.getInfo(), 0, 0, 1_000, be.tanks[0].getMaxFill(), 200,
                be.tanks[1].getMaxFill(), 300, be.tanks[2].getMaxFill(), 400, be.tanks[3].getMaxFill());
        helper.assertTrue(Boolean.FALSE.equals(turbine.getAuto()[0]), "getAuto");

        helper.assertTrue(Boolean.TRUE.equals(turbine.setThrottle(50.9D)[0]), "setThrottle(50.9)");
        helper.assertTrue(be.powerSliderPos == 30, "setThrottle(50) moves the slider to 30, was " + be.powerSliderPos);
        Object[] refused = turbine.setThrottle(170);
        helper.assertTrue(refused.length == 2 && refused[0] == null && "Input out of range.".equals(refused[1]),
                "setThrottle(170) is out of range: " + Arrays.toString(refused));
        helper.assertTrue(be.powerSliderPos == 30, "a refused throttle leaves the slider");
        turbine.setAuto(true);
        helper.assertTrue(be.autoMode, "setAuto(true)");
        turbine.start();
        helper.assertTrue(be.state == -1, "start() from offline begins the startup, state " + be.state);
        be.state = 1;
        turbine.start();
        helper.assertTrue(be.state == 1, "start() while online changes nothing");
        turbine.stop();
        helper.assertTrue(be.state == 0, "stop() from online goes offline, state " + be.state);
        cells(helper, CORE, turbine);
        helper.runAfterDelay(2, () -> {
            expectNumbers(helper, "getState after a tick", turbine.getState(), 0);
            // the offline turbine's shutdown clears automatic mode
            helper.assertTrue(Boolean.FALSE.equals(turbine.getAuto()[0]), "getAuto after shutdown");
            helper.succeed();
        });
    }

    private static void dfcEmitter(GameTestHelper helper) {
        helper.setBlock(SINGLE, ModDFCBlocks.dfc_emitter.get().defaultBlockState());
        TileEntityCoreEmitter be = GameTestHelpers.blockEntity(helper, SINGLE, TileEntityCoreEmitter.class);
        be.power = 1_000L;
        be.tank.setFill(300);
        be.watts = 40;
        CoreEmitterPeripheral emitter = PeripheralTestSupport.expect(helper, SINGLE, Direction.EAST,
                CoreEmitterPeripheral.class, "dfc_emitter");
        Object[] info = emitter.getInfo();
        expectNumbers(helper, "getInfo", Arrays.copyOf(info, 4), 1_000, TileEntityCoreEmitter.maxPower, 300, 40);
        helper.assertTrue(Boolean.FALSE.equals(info[4]), "getInfo[5] is isOn");
        expectNumbers(helper, "getEnergyInfo", emitter.getEnergyInfo(), 1_000, TileEntityCoreEmitter.maxPower);
        expectNumbers(helper, "getCryogel", emitter.getCryogel(), 300);

        emitter.setActive(true);
        helper.assertTrue(be.isOn, "setActive(true)");
        emitter.setInput(150.9D);
        helper.assertTrue(be.watts == 100, "setInput(150.9) clamps to 100, was " + be.watts);
        emitter.setInput(-3.5D);
        helper.assertTrue(be.watts == 0, "setInput(-3.5) clamps to 0, was " + be.watts);
        emitter.setInput(55);
        emitter.setActive(false);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Boolean.FALSE.equals(emitter.isActive()[0]), "isActive after a tick");
            expectNumbers(helper, "getInput after a tick", emitter.getInput(), 55);
            helper.succeed();
        });
    }

    private static void dfcReceiver(GameTestHelper helper) {
        helper.setBlock(SINGLE, ModDFCBlocks.dfc_receiver.get().defaultBlockState());
        TileEntityCoreReceiver be = GameTestHelpers.blockEntity(helper, SINGLE, TileEntityCoreReceiver.class);
        be.joules = 7L;
        be.power = 35_000L;
        be.tank.setFill(1_000);
        CoreReceiverPeripheral receiver = PeripheralTestSupport.expect(helper, SINGLE, Direction.WEST,
                CoreReceiverPeripheral.class, "dfc_receiver");
        expectNumbers(helper, "getInfo", receiver.getInfo(), 7, 35_000, 1_000);
        expectNumbers(helper, "getEnergyInfo", receiver.getEnergyInfo(), 7, 35_000);
        expectNumbers(helper, "getCryogel", receiver.getCryogel(), 1_000);
        be.joules = 0L;
        helper.runAfterDelay(2, () -> {
            // no beam: the tick turns zero joules into zero HE
            expectNumbers(helper, "getEnergyInfo after a tick", receiver.getEnergyInfo(), 0, 0);
            helper.succeed();
        });
    }

    private static void dfcInjector(GameTestHelper helper) {
        helper.setBlock(SINGLE, ModDFCBlocks.dfc_injector.get().defaultBlockState());
        TileEntityCoreInjector be = GameTestHelpers.blockEntity(helper, SINGLE, TileEntityCoreInjector.class);
        be.tanks[0].setTankType(Fluids.DEUTERIUM);
        be.tanks[1].setTankType(Fluids.TRITIUM);
        be.tanks[0].setFill(1_000);
        be.tanks[1].setFill(2_000);
        CoreInjectorPeripheral injector = PeripheralTestSupport.expect(helper, SINGLE, Direction.SOUTH,
                CoreInjectorPeripheral.class, "dfc_injector");
        expectNumbers(helper, "getFuel", injector.getFuel(), 1_000, 2_000);
        Object[] info = injector.getInfo();
        helper.assertTrue(info.length == 4 && Fluids.DEUTERIUM.getName().equals(info[1])
                && Fluids.TRITIUM.getName().equals(info[3]), "getInfo " + Arrays.toString(info));
        be.tanks[1].setTankType(Fluids.DEUTERIUM);
        helper.runAfterDelay(2, () -> {
            Object[] types = injector.getTypes();
            helper.assertTrue(Fluids.DEUTERIUM.getName().equals(types[0]) && Fluids.DEUTERIUM.getName().equals(types[1]),
                    "getTypes after a tick " + Arrays.toString(types));
            helper.succeed();
        });
    }

    private static void dfcStabilizer(GameTestHelper helper) {
        helper.setBlock(SINGLE, ModDFCBlocks.dfc_stabilizer.get().defaultBlockState());
        TileEntityCoreStabilizer be = GameTestHelpers.blockEntity(helper, SINGLE, TileEntityCoreStabilizer.class);
        be.power = 2_000L;
        be.watts = 20;
        CoreStabilizerPeripheral stabilizer = PeripheralTestSupport.expect(helper, SINGLE, Direction.DOWN,
                CoreStabilizerPeripheral.class, "dfc_stabilizer");
        helper.assertTrue("N/A".equals(stabilizer.getDurability()[0]), "getDurability without a lens");
        Object[] info = stabilizer.getInfo();
        expectNumbers(helper, "getInfo", Arrays.copyOf(info, 3), 2_000, TileEntityCoreStabilizer.maxPower, 20);
        helper.assertTrue("N/A".equals(info[3]), "getInfo[4] without a lens");

        stabilizer.setInput(42.5D);
        helper.assertTrue(be.watts == 42, "setInput(42.5) truncates to 42, was " + be.watts);
        stabilizer.setInput(500);
        helper.assertTrue(be.watts == 100, "setInput(500) clamps to 100, was " + be.watts);
        ItemStack lens = new ItemStack(ModDFCBlocks.ams_lens.get());
        ItemLens.setLensDamage(lens, 1_234L);
        be.inventory.setStackInSlot(0, lens);
        helper.runAfterDelay(2, () -> {
            // power 2000 is below 100^4: no beam, so the lens is not worn
            expectNumbers(helper, "getDurability after a tick", stabilizer.getDurability(), 1_234);
            expectNumbers(helper, "getInput after a tick", stabilizer.getInput(), 100);
            helper.succeed();
        });
    }

    private static void combustionEngine(GameTestHelper helper) {
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_combustion_engine.get(), CORE);
        TileEntityMachineCombustionEngine be = GameTestHelpers.blockEntity(helper, CORE,
                TileEntityMachineCombustionEngine.class);
        be.tank.setTankType(Fluids.DIESEL);
        be.tank.setFill(4_000);
        be.power = 100L;
        be.setting = 5;
        CombustionEnginePeripheral engine = PeripheralTestSupport.expect(helper, CORE, Direction.UP,
                CombustionEnginePeripheral.class, "ntm_combustion_engine");
        expectNumbers(helper, "getFluid", engine.getFluid(), 4_000, be.tank.getMaxFill());
        helper.assertTrue(Fluids.DIESEL.getName().equals(engine.fluidType()[0]), "getType " + engine.fluidType()[0]);
        expectNumbers(helper, "getPower", engine.getPower(), 100);
        expectNumbers(helper, "getThrottle", engine.getThrottle(), 5);
        helper.assertTrue(Boolean.FALSE.equals(engine.getState()[0]), "getState");
        // 1.7 dereferenced the empty piston slot
        Object[] noPiston = engine.getEfficiency();
        helper.assertTrue(noPiston.length == 2 && noPiston[0] == null && "unknown error".equals(noPiston[1]),
                "getEfficiency without pistons " + Arrays.toString(noPiston));
        helper.assertTrue(engine.getInfo()[0] == null, "getInfo without pistons is unknown error");

        helper.assertTrue(Boolean.TRUE.equals(engine.setThrottle(12.7D)[0]), "setThrottle(12.7)");
        helper.assertTrue(be.setting == 12, "setThrottle(12.7) truncates to 12, was " + be.setting);
        Object[] refused = engine.setThrottle(31);
        helper.assertTrue(refused.length == 2 && Boolean.FALSE.equals(refused[0])
                && "Throttle request outside of range 0-30".equals(refused[1]), "setThrottle(31) " + Arrays.toString(refused));
        helper.assertTrue(be.setting == 12, "a refused throttle leaves the setting");
        engine.start();
        helper.assertTrue(be.isOn, "start()");
        engine.stop();
        helper.assertTrue(!be.isOn, "stop()");

        FluidCombustion.Combustible diesel = FluidCombustion.getCombustible(Fluids.DIESEL);
        helper.assertTrue(diesel != null, "diesel is combustible");
        double expected = ItemPistons.Variant.DESH.eff[diesel.grade().ordinal()];
        be.inventory.setStackInSlot(2, new ItemStack(ModOilItems.piston(ItemPistons.Variant.DESH)));
        cells(helper, CORE, engine);
        helper.runAfterDelay(2, () -> {
            expectNumbers(helper, "getEfficiency after a tick", engine.getEfficiency(), expected);
            Object[] info = engine.getInfo();
            helper.assertTrue(info.length == 7 && Fluids.DIESEL.getName().equals(info[6]),
                    "getInfo with pistons " + Arrays.toString(info));
            expectNumbers(helper, "getInfo", Arrays.copyOfRange(info, 2, 6), 100, expected, 4_000, be.tank.getMaxFill());
            helper.succeed();
        });
    }

    private static void coker(GameTestHelper helper) {
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_coker.get(), CORE);
        TileEntityMachineCoker be = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachineCoker.class);
        be.tanks[0].setTankType(Fluids.HEAVYOIL);
        be.tanks[0].setFill(0);
        be.tanks[1].setFill(300);
        be.heat = 5_000;
        CokerPeripheral coker = PeripheralTestSupport.expect(helper, CORE, Direction.UP,
                CokerPeripheral.class, "ntm_coker");
        Object[] info = coker.getInfo();
        helper.assertTrue(info.length == 5 && Fluids.HEAVYOIL.getName().equals(info[0])
                && be.tanks[1].getTankType().getName().equals(info[1]), "getInfo " + Arrays.toString(info));
        expectNumbers(helper, "getInfo", Arrays.copyOfRange(info, 2, 5), 0, 300, 5_000);
        expectNumbers(helper, "getFluidStored", coker.getFluidStored(), 0, 300);
        expectNumbers(helper, "getHeat", coker.getHeat(), 5_000);
        cells(helper, CORE, coker);
        helper.runAfterDelay(2, () -> {
            // with no heater below, the coker loses max(heat / 1000, 1) per tick
            double heat = PeripheralTestSupport.number(helper, coker.getHeat(), 0);
            helper.assertTrue(heat < 5_000 && heat > 4_900, "getHeat after a tick " + heat);
            helper.succeed();
        });
    }

    /**
     * 1.7 {@code TileEntityProxyCombo.canConnectNode}: a power/fluid proxy cell answers with the core's own
     * component through a face that leaves the machine; a plain cell answers nothing.
     */
    private static void cells(GameTestHelper helper, BlockPos relativeCore, IPeripheral peripheral) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(relativeCore);
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int domains = BlockMultiblockCore.PASSIVE_POWER_IN | BlockMultiblockCore.PASSIVE_FLUID_IN;
        boolean proxy = false;
        boolean plain = false;
        for (BlockPos cell : MultiblockSurface.rigidStructureBlocks(level, core)) {
            if (cell.equals(core)) continue;
            if (com.hbm.compat.computercraft.Peripherals.isProxyCell(level, cell)) {
                if (proxy) continue;
                Direction out = null;
                for (Direction side : Direction.values()) {
                    if (!core.equals(MultiblockSurface.coreOfAny(level, cell.relative(side)))) {
                        out = side;
                        break;
                    }
                }
                if (out == null) continue;
                IPeripheral found = PeripheralTestSupport.find(helper, cell.subtract(origin), out);
                helper.assertTrue(found == peripheral, "proxy cell " + cell + " side " + out + " answered "
                        + (found == null ? "nothing" : found.getType()) + ", expected the core's peripheral");
                proxy = true;
            } else if (!plain) {
                PeripheralTestSupport.expectNone(helper, cell.subtract(origin), Direction.UP);
                plain = true;
            }
        }
        helper.assertTrue(proxy, "no power/fluid proxy cell with an outward face at " + relativeCore);
        helper.assertTrue(plain, "no plain cell at " + relativeCore);
    }

    private static void expectNumbers(GameTestHelper helper, String call, Object[] result, double... expected) {
        helper.assertTrue(result != null && result.length == expected.length,
                call + " answered " + Arrays.toString(result) + ", expected " + Arrays.toString(expected));
        for (int i = 0; i < expected.length; i++) {
            double actual = PeripheralTestSupport.number(helper, result, i);
            helper.assertTrue(actual == expected[i], call + "[" + (i + 1) + "] is " + actual + ", expected "
                    + expected[i] + " in " + Arrays.toString(result));
        }
    }
}
//?}
