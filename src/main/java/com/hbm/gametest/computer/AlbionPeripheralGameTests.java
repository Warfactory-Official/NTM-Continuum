package com.hbm.gametest.computer;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.compat.computercraft.albion.PADetectorPeripheral;
import com.hbm.compat.computercraft.albion.PADipolePeripheral;
import com.hbm.compat.computercraft.albion.PAQuadrupolePeripheral;
import com.hbm.compat.computercraft.albion.PARFCPeripheral;
import com.hbm.compat.computercraft.albion.PASourcePeripheral;
import com.hbm.gametest.FoldedTestSupport;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import com.hbm.tileentity.machine.albion.TileEntityPADetector;
import com.hbm.tileentity.machine.albion.TileEntityPADipole;
import com.hbm.tileentity.machine.albion.TileEntityPAQuadrupole;
import com.hbm.tileentity.machine.albion.TileEntityPARFC;
import com.hbm.tileentity.machine.albion.TileEntityPASource;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.golem.core.computer.PeripheralExposure;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** Peripheral GameTests for {@link com.hbm.compat.computercraft.albion.AlbionRows}; registered only with CC: Tweaked loaded. */
public final class AlbionPeripheralGameTests {

    private AlbionPeripheralGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    static final int PAD = 16;

    public static final GameTests.Function SOURCE = TESTS.function(
            "cc_pa_source", 60, PAD, () -> AlbionPeripheralGameTests::source);
    public static final GameTests.Function DIPOLE = TESTS.function(
            "cc_pa_dipole", 60, PAD, () -> AlbionPeripheralGameTests::dipole);
    public static final GameTests.Function QUADRUPOLE = TESTS.function(
            "cc_pa_quadrupole", 60, PAD, () -> AlbionPeripheralGameTests::quadrupole);
    public static final GameTests.Function RFC = TESTS.function(
            "cc_pa_rfc", 60, PAD, () -> AlbionPeripheralGameTests::rfc);
    public static final GameTests.Function DETECTOR = TESTS.function(
            "cc_pa_detector", 60, PAD, () -> AlbionPeripheralGameTests::detector);

    private static final int PROXY = BlockMultiblockCore.PASSIVE_POWER_IN | BlockMultiblockCore.PASSIVE_FLUID_IN;

    /** Builds {@code block} facing north at the padded plot's centre; returns the relative core. */
    static BlockPos assemble(GameTestHelper helper, BlockMultiblockCore block) {
        BlockPos relative = GameTests.paddedCentre(PAD, 3);
        FoldedTestSupport.assemble(helper, block, relative);
        return relative;
    }

    /**
     * A {@code CELLS} row: one power/fluid proxy cell answers, through a face that leaves the machine, with the core's
     * own peripheral object; one cell without such a proxy answers nothing.
     */
    static void assertCells(GameTestHelper helper, BlockPos relativeCore, IPeripheral expected) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(relativeCore);
        BlockPos proxy = null;
        Direction outward = null;
        BlockPos plain = null;
        for (BlockPos cell : MultiblockSurface.rigidStructureBlocks(level, core)) {
            if (cell.equals(core)) continue;
            boolean carries = com.hbm.compat.computercraft.Peripherals.isProxyCell(level, cell);
            if (carries && proxy == null) {
                outward = outward(level, cell, core);
                if (outward != null) proxy = cell;
            }
            if (!carries && plain == null) plain = cell;
        }
        helper.assertTrue(proxy != null, expected.getType() + " has no power/fluid proxy cell on its surface");
        helper.assertTrue(PeripheralExposure.find(level, proxy, outward) == expected,
                "proxy cell " + proxy + " side " + outward + " does not answer with the core's " + expected.getType());
        helper.assertTrue(plain != null, expected.getType() + " has no plain cell");
        helper.assertTrue(PeripheralExposure.find(level, plain, null) == null,
                "plain cell " + plain + " answers as " + expected.getType());
    }

    /** Assert no cell of the machine answers, as for a {@code BLOCK} or {@code DOWN} row. */
    static void assertNoCells(GameTestHelper helper, BlockPos relativeCore, @Nullable Direction side) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(relativeCore);
        int cells = 0;
        for (BlockPos cell : MultiblockSurface.rigidStructureBlocks(level, core)) {
            if (cell.equals(core)) continue;
            cells++;
            IPeripheral found = PeripheralExposure.find(level, cell, side);
            helper.assertTrue(found == null, "cell " + cell + " answers as " + (found == null ? "" : found.getType()));
        }
        helper.assertTrue(cells > 0, "the machine at " + relativeCore + " placed no cells");
    }

    private static @Nullable Direction outward(ServerLevel level, BlockPos cell, BlockPos core) {
        for (Direction side : Direction.values()) {
            if (!core.equals(MultiblockSurface.coreOfAny(level, cell.relative(side)))) return side;
        }
        return null;
    }

    /** Power that no battery changes, and coolant that neither cools nor flows with nothing connected. */
    private static void prime(TileEntityCooledBase machine, long power, int hot) {
        machine.power = power;
        machine.coolantTanks[0].setFill(0);
        machine.coolantTanks[1].setFill(hot);
    }

    private static void assertCooled(GameTestHelper helper, Object[] info, long power, long maxPower, int hot) {
        helper.assertTrue(PeripheralTestSupport.number(helper, info, 0) == power, "power " + info[0] + ", expected " + power);
        helper.assertTrue(PeripheralTestSupport.number(helper, info, 1) == maxPower, "max power " + info[1]);
        helper.assertTrue(PeripheralTestSupport.number(helper, info, 2) == 0, "cold coolant " + info[2]);
        helper.assertTrue(PeripheralTestSupport.number(helper, info, 3) == 4_000, "cold coolant max " + info[3]);
        helper.assertTrue(PeripheralTestSupport.number(helper, info, 4) == hot, "hot coolant " + info[4]);
        helper.assertTrue(PeripheralTestSupport.number(helper, info, 5) == 4_000, "hot coolant max " + info[5]);
    }

    private static void source(GameTestHelper helper) {
        BlockPos pos = assemble(helper, ModMachineBlocks.pa_source.get());
        TileEntityPASource be = GameTestHelpers.blockEntity(helper, pos, TileEntityPASource.class);
        PASourcePeripheral source = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                PASourcePeripheral.class, "ntm_pa_source");
        assertCells(helper, pos, source);

        be.particle = new TileEntityPASource.Particle(be, be.getBlockPos(), ForgeDirection.NORTH,
                ItemStack.EMPTY, ItemStack.EMPTY);
        be.state = TileEntityPASource.PAState.RUNNING;
        source.cancelOperation();
        helper.assertTrue(be.particle == null && be.state == TileEntityPASource.PAState.IDLE,
                "cancelOperation left " + be.state);

        prime(be, 1_234L, 123);
        be.lastSpeed = 777;
        be.state = TileEntityPASource.PAState.CRASH_DERAIL;
        be.inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PeripheralTestSupport.number(helper, source.getMomentum(), 0) == 777, "getMomentum");
            helper.assertTrue("CRASH_DERAIL".equals(source.getState()[0]), "getState " + source.getState()[0]);
            Object[] crafting = source.getCrafting();
            helper.assertTrue(crafting.length == 8 && Items.IRON_INGOT.getDescriptionId().equals(crafting[0])
                    && PeripheralTestSupport.number(helper, crafting, 1) == 1 && "".equals(crafting[2])
                    && PeripheralTestSupport.number(helper, crafting, 3) == 0, "getCrafting "
                    + java.util.Arrays.toString(crafting));
            Object[] info = source.getInfo();
            helper.assertTrue(info.length == 16 && "CRASH_DERAIL".equals(info[15])
                    && PeripheralTestSupport.number(helper, info, 14) == 777, "getInfo " + java.util.Arrays.toString(info));
            assertCooled(helper, info, 1_234L, be.getMaxPower(), 123);
            helper.succeed();
        });
    }

    private static void dipole(GameTestHelper helper) {
        BlockPos pos = assemble(helper, ModMachineBlocks.pa_dipole.get());
        TileEntityPADipole be = GameTestHelpers.blockEntity(helper, pos, TileEntityPADipole.class);
        PADipolePeripheral dipole = PeripheralTestSupport.expect(helper, pos, Direction.NORTH,
                PADipolePeripheral.class, "ntm_pa_dipole");
        assertCells(helper, pos, dipole);

        be.dirRedstone = 0;
        dipole.setDirLower("east");
        dipole.setDirUpper("west");
        dipole.setDirRedstone("up");
        helper.assertTrue(be.dirLower == 1 && be.dirUpper == 3, "setDirLower/setDirUpper");
        helper.assertTrue(be.dirRedstone == 0, "an unknown name must leave the direction, got " + be.dirRedstone);
        dipole.setThreshold(1.0E12D);
        helper.assertTrue(be.threshold == 999_999_999, "setThreshold clamps, got " + be.threshold);
        dipole.setThreshold(12.9D);
        helper.assertTrue(be.threshold == 12, "setThreshold(12.9) truncates, got " + be.threshold);
        prime(be, 55L, 9);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue("east".equals(dipole.getDirLower()[0]), "getDirLower");
            helper.assertTrue("west".equals(dipole.getDirUpper()[0]), "getDirUpper");
            helper.assertTrue("north".equals(dipole.getDirRedstone()[0]), "getDirRedstone");
            helper.assertTrue(PeripheralTestSupport.number(helper, dipole.getThreshold(), 0) == 12, "getThreshold");
            Object[] info = dipole.getInfo();
            helper.assertTrue(info.length == 10 && "east".equals(info[6]) && "west".equals(info[7])
                    && "north".equals(info[8]) && PeripheralTestSupport.number(helper, info, 9) == 12,
                    "getInfo " + java.util.Arrays.toString(info));
            assertCooled(helper, info, 55L, be.getMaxPower(), 9);
            helper.succeed();
        });
    }

    private static void quadrupole(GameTestHelper helper) {
        BlockPos pos = assemble(helper, ModMachineBlocks.pa_quadrupole.get());
        TileEntityPAQuadrupole be = GameTestHelpers.blockEntity(helper, pos, TileEntityPAQuadrupole.class);
        PAQuadrupolePeripheral quad = PeripheralTestSupport.expect(helper, pos, Direction.EAST,
                PAQuadrupolePeripheral.class, "ntm_pa_quad");
        assertCells(helper, pos, quad);
        prime(be, 4_321L, 77);
        helper.runAfterDelay(2, () -> {
            assertCooled(helper, quad.getInfo(), 4_321L, be.getMaxPower(), 77);
            Object[] energy = quad.getEnergyInfo();
            helper.assertTrue(PeripheralTestSupport.number(helper, energy, 0) == 4_321, "getEnergyInfo");
            helper.assertTrue(PeripheralTestSupport.number(helper, quad.getCoolant(), 2) == 77, "getCoolant");
            helper.succeed();
        });
    }

    private static void rfc(GameTestHelper helper) {
        BlockPos pos = assemble(helper, ModMachineBlocks.pa_rfc.get());
        TileEntityPARFC be = GameTestHelpers.blockEntity(helper, pos, TileEntityPARFC.class);
        PARFCPeripheral rfc = PeripheralTestSupport.expect(helper, pos, Direction.WEST,
                PARFCPeripheral.class, "ntm_pa_rfc");
        assertCells(helper, pos, rfc);
        prime(be, 8_765L, 31);
        helper.runAfterDelay(2, () -> {
            assertCooled(helper, rfc.getInfo(), 8_765L, be.getMaxPower(), 31);
            helper.succeed();
        });
    }

    private static void detector(GameTestHelper helper) {
        BlockPos pos = assemble(helper, ModMachineBlocks.pa_detector.get());
        TileEntityPADetector be = GameTestHelpers.blockEntity(helper, pos, TileEntityPADetector.class);
        PADetectorPeripheral detector = PeripheralTestSupport.expect(helper, pos, Direction.SOUTH,
                PADetectorPeripheral.class, "ntm_pa_detector");
        assertCells(helper, pos, detector);
        prime(be, 99L, 5);
        be.inventory.setStackInSlot(2, new ItemStack(Items.GOLD_INGOT));
        helper.runAfterDelay(2, () -> {
            Object[] crafting = detector.getCrafting();
            helper.assertTrue(crafting.length == 8 && "".equals(crafting[0])
                    && Items.GOLD_INGOT.getDescriptionId().equals(crafting[2])
                    && PeripheralTestSupport.number(helper, crafting, 3) == 1, "getCrafting "
                    + java.util.Arrays.toString(crafting));
            Object[] info = detector.getInfo();
            helper.assertTrue(info.length == 14 && Items.GOLD_INGOT.getDescriptionId().equals(info[8]),
                    "getInfo " + java.util.Arrays.toString(info));
            assertCooled(helper, info, 99L, be.getMaxPower(), 5);
            helper.succeed();
        });
    }
}
//?}
