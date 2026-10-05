package com.hbm.gametest.computer;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.blocks.ModProductionBlocks;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.blocks.machine.MachinePWRController;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.compat.computercraft.machine.BreedingReactorPeripheral;
import com.hbm.compat.computercraft.machine.GeigerPeripheral;
import com.hbm.compat.computercraft.machine.ICFPeripheral;
import com.hbm.compat.computercraft.machine.MicrowavePeripheral;
import com.hbm.compat.computercraft.machine.PWRControllerPeripheral;
import com.hbm.compat.computercraft.machine.RadarPeripheral;
import com.hbm.compat.computercraft.machine.ReactorControlPeripheral;
import com.hbm.compat.computercraft.machine.ResearchReactorPeripheral;
import com.hbm.compat.computercraft.machine.WatzPeripheral;
import com.hbm.compat.computercraft.machine.ZirnoxPeripheral;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.items.machine.ItemICFPellet.EnumICFFuel;
import com.hbm.tileentity.machine.TileEntityICF;
import com.hbm.tileentity.machine.TileEntityMachineRadarLarge;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityMicrowave;
import com.hbm.tileentity.machine.TileEntityPWRController;
import com.hbm.tileentity.machine.TileEntityReactorControl;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

/** Peripheral GameTests for {@link com.hbm.compat.computercraft.machine.ReactorRows}; registered only with CC: Tweaked loaded. */
public final class ReactorPeripheralGameTests {

    private ReactorPeripheralGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    private static final int PADDING = 10;

    public static final GameTests.Function GEIGER = TESTS.function(
            "cc_geiger", 40, () -> ReactorPeripheralGameTests::geiger);
    public static final GameTests.Function MICROWAVE = TESTS.function(
            "cc_microwave", 40, () -> ReactorPeripheralGameTests::microwave);
    public static final GameTests.Function ZIRNOX = TESTS.function(
            "cc_zirnox", 40, PADDING, () -> ReactorPeripheralGameTests::zirnox);
    public static final GameTests.Function RESEARCH = TESTS.function(
            "cc_research_reactor", 40, () -> ReactorPeripheralGameTests::research);
    public static final GameTests.Function BREEDING = TESTS.function(
            "cc_breeding_reactor", 40, () -> ReactorPeripheralGameTests::breeding);
    public static final GameTests.Function CONTROL = TESTS.function(
            "cc_reactor_control", 40, () -> ReactorPeripheralGameTests::control);
    public static final GameTests.Function PWR = TESTS.function(
            "cc_pwr_control", 40, () -> ReactorPeripheralGameTests::pwr);
    public static final GameTests.Function WATZ = TESTS.function(
            "cc_watz", 40, PADDING, () -> ReactorPeripheralGameTests::watz);
    public static final GameTests.Function ICF = TESTS.function(
            "cc_icf", 60, PADDING, () -> ReactorPeripheralGameTests::icf);
    public static final GameTests.Function RADAR = TESTS.function(
            "cc_radar", 40, () -> ReactorPeripheralGameTests::radar);
    public static final GameTests.Function RADAR_LARGE = TESTS.function(
            "cc_radar_large", 40, PADDING, () -> ReactorPeripheralGameTests::radarLarge);

    private static void geiger(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.geiger.get().defaultBlockState());
        GeigerPeripheral geiger = PeripheralTestSupport.expect(helper, pos, Direction.NORTH,
                GeigerPeripheral.class, "ntm_geiger");
        double rads = PeripheralTestSupport.number(helper, geiger.getRads(), 0);
        helper.assertTrue(rads >= 0.0D, "negative rads " + rads);
        helper.succeed();
    }

    private static void microwave(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModProductionBlocks.MACHINE_MICROWAVE.get().defaultBlockState());
        MicrowavePeripheral microwave = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                MicrowavePeripheral.class, "microwave");
        helper.assertTrue("This is a testing device for everything OC.".equals(microwave.test()[0]), "test()");
        microwave.variableset(3.7D);
        TileEntityMicrowave be = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, TileEntityMicrowave.class);
        helper.assertTrue(be.speed == 3, "variableset(3.7) left speed " + be.speed + ", 1.7 truncates to 3");
        microwave.variableset(9);
        helper.assertTrue(be.speed == 5, "variableset(9) left speed " + be.speed + ", 1.7 clamps to 5");
        // the computer-thread getter reads the snapshot taken after the next server tick
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, microwave.variableget(), 0) == 5,
                    "variableget after a tick");
            helper.succeed();
        });
    }

    /** CO2 5000 mB at zero heat: pressure 10,000, which 1.7 reports as round(10000 * 1e-5 * 30) = 3 bar. */
    private static void zirnox(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(PADDING, 1);
        placeFolded(helper, ModMachineBlocks.machine_zirnox.get(), core);
        TileEntityReactorZirnox be = GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class);
        be.water.setFill(1234);
        be.carbonDioxide.setFill(5000);
        ZirnoxPeripheral zirnox = PeripheralTestSupport.expect(helper, core, Direction.UP,
                ZirnoxPeripheral.class, "zirnox_reactor");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, zirnox.getWater(), 0) == 1234, "getWater");
            helper.assertTrue(PeripheralTestSupport.number(helper, zirnox.getCarbonDioxide(), 0) == 5000,
                    "getCarbonDioxide");
            helper.assertTrue(PeripheralTestSupport.number(helper, zirnox.getPressure(), 0) == 3, "getPressure");
            helper.assertTrue(PeripheralTestSupport.number(helper, zirnox.getTemp(), 0) == 20, "getTemp at zero heat");
            Object[] info = zirnox.getInfo();
            helper.assertTrue(info.length == 6 && Boolean.FALSE.equals(info[5]), "getInfo " + java.util.Arrays.toString(info));

            zirnox.setActive(true);
            helper.assertTrue(be.isOn, "setActive(true)");
            zirnox.ventCarbonDioxide(Optional.of(1500.9D));
            helper.assertTrue(be.carbonDioxide.getFill() == 3500, "ventCarbonDioxide(1500.9) left "
                    + be.carbonDioxide.getFill() + ", 1.7 truncates to 1500");
            zirnox.ventCarbonDioxide(Optional.empty());
            helper.assertTrue(be.carbonDioxide.getFill() == 2500, "ventCarbonDioxide() vents 1000, left "
                    + be.carbonDioxide.getFill());
            zirnox.ventCarbonDioxide(Optional.of(99_999D));
            helper.assertTrue(be.carbonDioxide.getFill() == 0, "ventCarbonDioxide clamps to the tank");

            expectCells(helper, core, zirnox, 5);
            helper.succeed();
        });
    }

    private static void research(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModNuclearMiscBlocks.REACTOR_RESEARCH.get().defaultBlockState());
        TileEntityReactorResearch be = GameTestHelpers.blockEntity(helper, pos, TileEntityReactorResearch.class);
        ResearchReactorPeripheral reactor = PeripheralTestSupport.expect(helper, pos, Direction.NORTH,
                ResearchReactorPeripheral.class, "research_reactor");
        reactor.setLevel(250);
        helper.assertTrue(be.targetLevel == 1.0D, "setLevel(250) clamps to 1.0, got " + be.targetLevel);
        reactor.setLevel(50);
        helper.assertTrue(be.targetLevel == 0.5D, "setLevel(50) targets 0.5, got " + be.targetLevel);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, reactor.getTargetLevel(), 0) == 0.5D,
                    "getTargetLevel is the 0..1 fraction");
            double level = PeripheralTestSupport.number(helper, reactor.getLevel(), 0);
            helper.assertTrue(level >= 0 && level <= 50, "getLevel is a percent moving toward 50, got " + level);
            Object[] info = reactor.getInfo();
            helper.assertTrue(info.length == 4 && PeripheralTestSupport.number(helper, info, 2) == 0.5D,
                    "getInfo " + java.util.Arrays.toString(info));
            helper.assertTrue(PeripheralTestSupport.number(helper, reactor.getTemp(), 0) == 0, "getTemp of a cold reactor");
            helper.succeed();
        });
    }

    private static void breeding(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get().defaultBlockState());
        BreedingReactorPeripheral breeder = PeripheralTestSupport.expect(helper, pos, Direction.EAST,
                BreedingReactorPeripheral.class, "breeding_reactor");
        helper.runAfterDelay(2, () -> {
            Object[] info = breeder.getInfo();
            helper.assertTrue(info.length == 2 && Integer.valueOf(0).equals(info[0]) && Float.valueOf(0F).equals(info[1]),
                    "an idle breeder reports int flux 0 and float progress 0: " + java.util.Arrays.toString(info));
            helper.assertTrue(breeder.getProgress()[0] instanceof Float, "1.7 progress is a float");
            helper.assertTrue(PeripheralTestSupport.number(helper, breeder.getFlux(), 0) == 0, "getFlux");
            helper.succeed();
        });
    }

    private static void control(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModNuclearMiscBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        TileEntityReactorControl be = GameTestHelpers.blockEntity(helper, pos, TileEntityReactorControl.class);
        ReactorControlPeripheral control = PeripheralTestSupport.expect(helper, pos, Direction.SOUTH,
                ReactorControlPeripheral.class, "reactor_control");
        control.setParams(1.9D, 12_000D, 100D, 80D, -5D);
        helper.assertTrue(be.function == TileEntityReactorControl.RodFunction.QUAD, "function 1.9 truncates to QUAD");
        helper.assertTrue(be.heatUpper == 9999D && be.heatLower == 100D, "heat bounds " + be.heatUpper + "/" + be.heatLower);
        helper.assertTrue(be.levelUpper == 0.8D && be.levelLower == 0D, "level bounds " + be.levelUpper + "/" + be.levelLower);
        helper.runAfterDelay(2, () -> {
            Object[] params = control.getParams();
            helper.assertTrue(PeripheralTestSupport.number(helper, params, 0) == 1
                    && PeripheralTestSupport.number(helper, params, 1) == 9999D
                    && PeripheralTestSupport.number(helper, params, 3) == 0.8D, "getParams " + java.util.Arrays.toString(params));
            helper.assertTrue(Boolean.FALSE.equals(control.isLinked()[0]), "no sensor, not linked");
            helper.assertTrue(List.of(0, 0, 0).equals(control.getReactor()[0]), "an unlinked getReactor is {0, 0, 0}");
            helper.succeed();
        });
    }

    private static void pwr(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModPwrBlocks.PWR_CONTROLLER.get().defaultBlockState()
                .setValue(MachinePWRController.FACING, Direction.NORTH));
        TileEntityPWRController be = GameTestHelpers.blockEntity(helper, pos, TileEntityPWRController.class);
        PWRControllerPeripheral pwr = PeripheralTestSupport.expect(helper, pos, Direction.SOUTH,
                PWRControllerPeripheral.class, "ntm_pwr_control");
        helper.assertTrue(Boolean.TRUE.equals(pwr.setLevel(150)[0]), "setLevel answers true");
        helper.assertTrue(be.rodTarget == 100D, "setLevel(150) clamps to 100, got " + be.rodTarget);
        pwr.setLevel(37.5D);
        helper.assertTrue(be.rodTarget == 37.5D, "setLevel(37.5) keeps the fraction, got " + be.rodTarget);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, pwr.getLevel(), 0) == 37.5D, "getLevel target");
            helper.assertTrue(PeripheralTestSupport.number(helper, pwr.getHeat(), 3)
                    == TileEntityPWRController.hullHeatCapacityBase, "getHeat hull capacity");
            Object[] coolant = pwr.getCoolantInfo();
            helper.assertTrue(PeripheralTestSupport.number(helper, coolant, 1) == be.tanks[0].getMaxFill()
                    && PeripheralTestSupport.number(helper, coolant, 3) == be.tanks[1].getMaxFill(),
                    "getCoolantInfo " + java.util.Arrays.toString(coolant));
            helper.assertTrue(pwr.getInfo().length == 14, "getInfo has 1.7's fourteen values");
            helper.succeed();
        });
    }

    private static void watz(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(PADDING, 1);
        placeFolded(helper, ModMachineBlocks.watz.get(), core);
        WatzPeripheral watz = PeripheralTestSupport.expect(helper, core, Direction.UP,
                WatzPeripheral.class, "watz_reactor");
        helper.runAfterDelay(2, () -> {
            Object[] coolant = watz.getCoolantInfo();
            helper.assertTrue(coolant.length == 4 && PeripheralTestSupport.number(helper, coolant, 1) == 64_000
                    && PeripheralTestSupport.number(helper, coolant, 3) == 64_000,
                    "getCoolantInfo " + java.util.Arrays.toString(coolant));
            helper.assertTrue(PeripheralTestSupport.number(helper, watz.getWasteInfo(), 1) == 64_000, "getWasteInfo");
            helper.assertTrue(PeripheralTestSupport.number(helper, watz.getHeat(), 0) == 0, "getHeat of an empty Watz");
            helper.assertTrue(PeripheralTestSupport.number(helper, watz.getFlux(), 0) == 0, "getFlux of an empty Watz");
            helper.assertTrue(watz.getInfo().length == 9, "getInfo has 1.7's nine values");
            expectCells(helper, core, watz, 4);
            helper.succeed();
        });
    }

    private static void icf(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(PADDING, 1);
        placeFolded(helper, ModIcfBlocks.icf.get(), core);
        TileEntityICF be = GameTestHelpers.blockEntity(helper, core, TileEntityICF.class);
        be.tanks[0].setFill(1234);
        ICFPeripheral icf = PeripheralTestSupport.expect(helper, core, Direction.UP, ICFPeripheral.class, "ntm_icf_reactor");
        helper.assertTrue(Long.valueOf(1_000_000_000_000L).equals(icf.getMaxHeat()[0]), "getMaxHeat");
        helper.runAfterDelay(2, () -> {
            Object[] fluid = icf.getFluid();
            helper.assertTrue(fluid.length == 8 && PeripheralTestSupport.number(helper, fluid, 0) == 1234
                    && "hbmfluid.sodium".equals(fluid[2]) && "hbmfluid.sodium_hot".equals(fluid[5]),
                    "getFluid " + java.util.Arrays.toString(fluid));
            Object[] none = icf.getPelletStats();
            helper.assertTrue(none.length == 2 && none[0] == null && "unknown error".equals(none[1]),
                    "an empty chamber is 1.7's dereference error: " + java.util.Arrays.toString(none));
            be.inventory.setStackInSlot(5, ItemICFPellet.setup(EnumICFFuel.DEUTERIUM, EnumICFFuel.TRITIUM, false));
            helper.runAfterDelay(2, () -> {
                Object[] stats = icf.getPelletStats();
                helper.assertTrue(stats.length == 5 && PeripheralTestSupport.number(helper, stats, 0) == 0
                        && "DEUTERIUM".equals(stats[3]) && "TRITIUM".equals(stats[4]),
                        "getPelletStats " + java.util.Arrays.toString(stats));
                expectCells(helper, core, icf, 8);
                helper.succeed();
            });
        });
    }

    private static void radar(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModRadarUtilityBlocks.machine_radar.get().defaultBlockState());
        TileEntityMachineRadarNT be = GameTestHelpers.blockEntity(helper, pos, TileEntityMachineRadarNT.class);
        RadarPeripheral radar = PeripheralTestSupport.expect(helper, pos, Direction.WEST, RadarPeripheral.class, "ntm_radar");
        radar.setSettings(false, true, false, true);
        helper.assertTrue(!be.scanMissiles && be.scanShells && !be.scanPlayers && be.smartMode, "setSettings");
        Object[] none = radar.getEntityAtIndex(0);
        helper.assertTrue(none.length == 2 && none[0] == null && "No entity exists at that index.".equals(none[1]),
                "index 0 is below 1.7's 1-based range: " + java.util.Arrays.toString(none));
        helper.runAfterDelay(2, () -> {
            Object[] settings = radar.getSettings();
            helper.assertTrue(Boolean.FALSE.equals(settings[0]) && Boolean.TRUE.equals(settings[1])
                    && Boolean.FALSE.equals(settings[2]) && Boolean.TRUE.equals(settings[3]),
                    "getSettings " + java.util.Arrays.toString(settings));
            helper.assertTrue(PeripheralTestSupport.number(helper, radar.getRange(), 0) == TileEntityMachineRadarNT.radarRange,
                    "getRange");
            BlockPos abs = helper.absolutePos(pos);
            Object[] at = radar.getPos();
            helper.assertTrue(PeripheralTestSupport.number(helper, at, 0) == abs.getX()
                    && PeripheralTestSupport.number(helper, at, 1) == abs.getY()
                    && PeripheralTestSupport.number(helper, at, 2) == abs.getZ(), "getPos " + java.util.Arrays.toString(at));
            helper.succeed();
        });
    }

    /** 1.7's four {@code makeExtra} power cells answer (the port states them as active power cells); a plain one does not. */
    private static void radarLarge(GameTestHelper helper) {
        BlockPos core = GameTests.paddedCentre(PADDING, 1);
        placeFolded(helper, ModRadarUtilityBlocks.machine_radar_large.get(), core);
        RadarPeripheral radar = PeripheralTestSupport.expect(helper, core, Direction.UP, RadarPeripheral.class, "ntm_radar");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, radar.getRange(), 0)
                    == TileEntityMachineRadarLarge.radarLargeRange, "getRange of the large radar");
            expectCells(helper, core, radar, 4);
            helper.succeed();
        });
    }

    // --- folded multiblocks ---

    private static final int PROXY = BlockMultiblockCore.PASSIVE_POWER_IN | BlockMultiblockCore.PASSIVE_FLUID_IN;

    private record Cell(BlockPos rel, Direction side) {}

    private static void placeFolded(GameTestHelper helper, Block block, BlockPos coreRel) {
        helper.setBlock(coreRel, block.defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH));
        ((BlockMultiblockCore) block).fillSpace(helper.getLevel(), helper.absolutePos(coreRel), Direction.NORTH);
    }

    /** A power/fluid proxy cell answers with the core's peripheral object; a plain cell answers none. */
    private static void expectCells(GameTestHelper helper, BlockPos coreRel, IPeripheral expected, int reach) {
        Cell proxy = cell(helper, coreRel, reach, true);
        IPeripheral found = PeripheralTestSupport.find(helper, proxy.rel(), proxy.side());
        helper.assertTrue(found == expected, "proxy cell " + proxy.rel() + " side " + proxy.side() + " answered "
                + (found == null ? "none" : found.getType()) + ", expected the core's peripheral");
        Cell plain = cell(helper, coreRel, reach, false);
        PeripheralTestSupport.expectNone(helper, plain.rel(), plain.side());
    }

    /**
     * The first cell of the machine at {@code coreRel} that does ({@code proxy}) or does not carry a power/fluid
     * proxy, with a face that touches no block of the same machine (1.7 {@code isComponent}).
     */
    private static Cell cell(GameTestHelper helper, BlockPos coreRel, int reach, boolean proxy) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(coreRel);
        for (int dy = 0; dy <= reach; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos pos = core.offset(dx, dy, dz);
                    if (!MultiblockSurface.isFoldedCell(level.getBlockState(pos))
                            || !core.equals(MultiblockSurface.coreOfCell(level, pos))
                            || com.hbm.compat.computercraft.Peripherals.isProxyCell(level, pos) != proxy) continue;
                    for (Direction side : Direction.values()) {
                        if (!core.equals(MultiblockSurface.coreOfAny(level, pos.relative(side)))) {
                            return new Cell(coreRel.offset(dx, dy, dz), side);
                        }
                    }
                }
            }
        }
        helper.assertTrue(false, "no " + (proxy ? "proxy" : "plain") + " cell around " + coreRel);
        throw new IllegalStateException("unreachable");
    }
}
//?}
