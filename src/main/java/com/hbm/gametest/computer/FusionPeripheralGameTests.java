package com.hbm.gametest.computer;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.compat.computercraft.fusion.FusionBoilerPeripheral;
import com.hbm.compat.computercraft.fusion.FusionBreederPeripheral;
import com.hbm.compat.computercraft.fusion.FusionKlystronPeripheral;
import com.hbm.compat.computercraft.fusion.FusionMHDTPeripheral;
import com.hbm.compat.computercraft.fusion.FusionTorusPeripheral;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;

/** Peripheral GameTests for {@link com.hbm.compat.computercraft.fusion.FusionRows}; registered only with CC: Tweaked loaded. */
public final class FusionPeripheralGameTests {

    private FusionPeripheralGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    private static final int PAD = AlbionPeripheralGameTests.PAD;

    public static final GameTests.Function TORUS = TESTS.function(
            "cc_fusion_torus", 60, PAD, () -> FusionPeripheralGameTests::torus);
    public static final GameTests.Function KLYSTRON = TESTS.function(
            "cc_fusion_klystron", 60, PAD, () -> FusionPeripheralGameTests::klystron);
    public static final GameTests.Function BOILER = TESTS.function(
            "cc_fusion_boiler", 60, PAD, () -> FusionPeripheralGameTests::boiler);
    public static final GameTests.Function BREEDER = TESTS.function(
            "cc_fusion_breeder", 60, PAD, () -> FusionPeripheralGameTests::breeder);
    public static final GameTests.Function MHDT = TESTS.function(
            "cc_fusion_mhdt", 60, PAD, () -> FusionPeripheralGameTests::mhdt);

    private static double num(GameTestHelper helper, Object[] result, int index) {
        return PeripheralTestSupport.number(helper, result, index);
    }

    private static void torus(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModMachineBlocks.fusion_torus.get());
        TileEntityFusionTorus be = GameTestHelpers.blockEntity(helper, pos, TileEntityFusionTorus.class);
        FusionTorusPeripheral torus = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                FusionTorusPeripheral.class, "ntm_fusion_torus");
        AlbionPeripheralGameTests.assertCells(helper, pos, torus);

        be.power = 5_000L;
        be.coolantTanks[0].setFill(0);
        be.coolantTanks[1].setFill(40);
        be.tanks[0].setTankType(Fluids.WATER);
        be.tanks[0].setFill(1_000);
        helper.runAfterDelay(2, () -> {
            Object[] fluid = torus.getFluid();
            helper.assertTrue(fluid.length == 12 && num(helper, fluid, 0) == 1_000 && num(helper, fluid, 1) == 4_000
                    && "hbmfluid.water".equals(fluid[2]) && "hbmfluid.none".equals(fluid[5]),
                    "getFluid " + Arrays.toString(fluid));
            Object[] energy = torus.getEnergyInfo();
            helper.assertTrue(num(helper, energy, 0) == 5_000 && num(helper, energy, 1) == be.getMaxPower(),
                    "getEnergyInfo " + Arrays.toString(energy));
            helper.assertTrue(num(helper, torus.getCoolant(), 2) == 40, "getCoolant");
            helper.assertTrue(torus.getRecipeProgress().length == 2, "getRecipeProgress");
            Object[] info = torus.getInfo();
            helper.assertTrue(info.length == 23 && "hbmfluid.water".equals(info[4])
                    && num(helper, info, 16) == 40, "getInfo " + Arrays.toString(info));
            helper.succeed();
        });
    }

    private static void klystron(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModMachineBlocks.fusion_klystron.get());
        TileEntityFusionKlystron be = GameTestHelpers.blockEntity(helper, pos, TileEntityFusionKlystron.class);
        FusionKlystronPeripheral klystron = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                FusionKlystronPeripheral.class, "ntm_fusion_klystron");
        AlbionPeripheralGameTests.assertCells(helper, pos, klystron);

        klystron.setOutput(2.5E6D);
        helper.assertTrue(be.outputTarget == TileEntityFusionKlystron.MAX_OUTPUT, "setOutput clamps high, got " + be.outputTarget);
        klystron.setOutput(-5D);
        helper.assertTrue(be.outputTarget == 0L, "setOutput clamps low, got " + be.outputTarget);
        klystron.setOutput(1_234.9D);
        helper.assertTrue(be.outputTarget == 1_234L, "setOutput(1234.9) truncates, got " + be.outputTarget);
        be.compair.setFill(500);
        helper.runAfterDelay(2, () -> {
            Object[] output = klystron.getOutput();
            helper.assertTrue(num(helper, output, 1) == 1_234, "getOutput " + Arrays.toString(output));
            Object[] air = klystron.getAir();
            helper.assertTrue(num(helper, air, 1) == be.compair.getMaxFill(), "getAir " + Arrays.toString(air));
            helper.assertTrue(num(helper, klystron.getEnergyInfo(), 1) == be.getMaxPower(), "getEnergyInfo");
            helper.assertTrue(klystron.getInfo().length == 6, "getInfo");
            helper.succeed();
        });
    }

    private static void boiler(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModMachineBlocks.fusion_boiler.get());
        TileEntityFusionBoiler be = GameTestHelpers.blockEntity(helper, pos, TileEntityFusionBoiler.class);
        FusionBoilerPeripheral boiler = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                FusionBoilerPeripheral.class, "ntm_fusion_boiler");
        AlbionPeripheralGameTests.assertCells(helper, pos, boiler);

        be.tanks[0].setFill(2_500);
        helper.runAfterDelay(2, () -> {
            Object[] fluid = boiler.getFluid();
            helper.assertTrue(num(helper, fluid, 0) == 2_500 && num(helper, fluid, 1) == 32_000
                    && num(helper, fluid, 3) == 32_000, "getFluid " + Arrays.toString(fluid));
            helper.assertTrue(num(helper, boiler.getPlasmaEnergy(), 0) == 0, "getPlasmaEnergy without a torus");
            Object[] info = boiler.getInfo();
            helper.assertTrue(info.length == 5 && num(helper, info, 1) == 2_500, "getInfo " + Arrays.toString(info));
            helper.succeed();
        });
    }

    private static void breeder(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModMachineBlocks.fusion_breeder.get());
        TileEntityFusionBreeder be = GameTestHelpers.blockEntity(helper, pos, TileEntityFusionBreeder.class);
        FusionBreederPeripheral breeder = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                FusionBreederPeripheral.class, "ntm_fusion_breeder");
        AlbionPeripheralGameTests.assertCells(helper, pos, breeder);

        be.inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 3));
        helper.runAfterDelay(2, () -> {
            Object[] crafting = breeder.getCrafting();
            helper.assertTrue(crafting.length == 4 && Items.IRON_INGOT.getDescriptionId().equals(crafting[0])
                    && num(helper, crafting, 1) == 3 && "".equals(crafting[2]) && num(helper, crafting, 3) == 0,
                    "getCrafting " + Arrays.toString(crafting));
            Object[] fluid = breeder.getFluid();
            helper.assertTrue(fluid.length == 6 && num(helper, fluid, 1) == 16_000 && "hbmfluid.none".equals(fluid[2]),
                    "getFluid " + Arrays.toString(fluid));
            helper.assertTrue(num(helper, breeder.getProgress(), 0) == 0.0D, "getProgress");
            helper.assertTrue(num(helper, breeder.getNeutronEnergy(), 0) == 0.0D, "getNeutronEnergy");
            helper.assertTrue(breeder.getInfo().length == 12, "getInfo");
            helper.succeed();
        });
    }

    private static void mhdt(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModMachineBlocks.fusion_mhdt.get());
        TileEntityFusionMHDT be = GameTestHelpers.blockEntity(helper, pos, TileEntityFusionMHDT.class);
        FusionMHDTPeripheral mhdt = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                FusionMHDTPeripheral.class, "ntm_fusion_mhdt");
        AlbionPeripheralGameTests.assertCells(helper, pos, mhdt);

        // no cold coolant, so the turbine does not run and the warm tank stays put
        be.tanks[0].setFill(0);
        be.tanks[1].setFill(1_000);
        helper.runAfterDelay(2, () -> {
            Object[] coolant = mhdt.getCoolant();
            helper.assertTrue(num(helper, coolant, 0) == 0 && num(helper, coolant, 1) == 4_000
                    && num(helper, coolant, 2) == 1_000 && num(helper, coolant, 3) == 4_000,
                    "getCoolant " + Arrays.toString(coolant));
            helper.assertTrue(mhdt.getEnergyInfo().length == 1, "1.7 getEnergyInfo answers the buffer alone");
            helper.assertTrue(num(helper, mhdt.getPlasmaEnergy(), 0) == 0, "getPlasmaEnergy without a torus");
            helper.assertTrue(mhdt.getInfo().length == 6, "getInfo");
            helper.succeed();
        });
    }
}
//?}
