package com.hbm.gametest.computer;

import com.hbm.Constants;
import com.hbm.blocks.ModProductionBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.tileentity.machine.TileEntityMicrowave;
import dev.golem.core.computer.Computers;
import dev.golem.core.dejson.BlockStates;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;

/**
 * HBM's components on OpenComputers: Rebooted, reached the way OC reaches them: an OC cable or Adapter placed next to
 * the machine joins it to OC's network, and calls go through OC's own callback path. Registered only with OC loaded.
 */
public final class OpenComputersGameTests {

    private OpenComputersGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function CABLE_GEIGER = TESTS.function(
            "oc_cable_geiger", 60, () -> OpenComputersGameTests::cableGeiger);
    public static final GameTests.Function CABLE_MICROWAVE = TESTS.function(
            "oc_cable_microwave", 60, () -> OpenComputersGameTests::cableMicrowave);
    public static final GameTests.Function ADAPTER_GEIGER = TESTS.function(
            "oc_adapter_geiger", 60, () -> OpenComputersGameTests::adapterGeiger);

    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);
    private static final BlockPos NEXT = new BlockPos(2, 1, 1);

    private static void cableGeiger(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModMachineBlocks.geiger.get().defaultBlockState());
        helper.setBlock(NEXT, oc("cable").defaultBlockState());
        helper.runAfterDelay(10, () -> {
            expectReached(helper, "ntm_geiger");
            Object[] rads = call(helper, "ntm_geiger", "getRads");
            helper.assertTrue(PeripheralTestSupport.number(helper, rads, 0) >= 0.0D, "getRads " + Arrays.toString(rads));
            helper.succeed();
        });
    }

    private static void cableMicrowave(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModProductionBlocks.MACHINE_MICROWAVE.get().defaultBlockState());
        helper.setBlock(NEXT, oc("cable").defaultBlockState());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue("This is a testing device for everything OC.".equals(call(helper, "microwave", "test")[0]),
                    "test()");
            call(helper, "microwave", "variableset", 3.7D);
            TileEntityMicrowave be = GameTestHelpers.blockEntity(helper, MACHINE, TileEntityMicrowave.class);
            helper.assertTrue(be.speed == 3, "variableset(3.7) left speed " + be.speed);
            try {
                call(helper, "microwave", "variableset", "fast");
                helper.fail("variableset(\"fast\") was accepted");
            } catch (IllegalArgumentException e) {
                helper.assertTrue("bad argument #1 (number expected, got string)".equals(e.getMessage()),
                        "bad argument message " + e.getMessage());
            }
            helper.succeed();
        });
    }

    private static void adapterGeiger(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModMachineBlocks.geiger.get().defaultBlockState());
        helper.setBlock(NEXT, oc("adapter").defaultBlockState());
        helper.runAfterDelay(10, () -> {
            expectReached(helper, "ntm_geiger");
            helper.assertTrue(call(helper, "ntm_geiger", "getRads")[0] instanceof Number, "getRads through the adapter");
            helper.succeed();
        });
    }

    private static Block oc(String path) {
        Block block = BlockStates.block(Computers.OPENCOMPUTERS + ":" + path);
        if (block == null) throw new IllegalStateException("no block opencomputers:" + path);
        return block;
    }

    private static void expectReached(GameTestHelper helper, String name) {
        List<String> reached = Computers.openComputersReach(helper.getLevel(), helper.absolutePos(NEXT));
        helper.assertTrue(reached.contains(name), "OC reaches " + reached + ", expected " + name);
    }

    private static Object[] call(GameTestHelper helper, String component, String method, Object... arguments) {
        try {
            Object[] result = Computers.openComputersCall(helper.getLevel(), helper.absolutePos(NEXT), component,
                    method, arguments);
            return result == null ? new Object[0] : result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(component + "." + method + " threw " + e, e);
        }
    }
}
