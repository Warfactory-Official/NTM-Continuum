package com.hbm.gametest.computer;

//? if opencomputers {
/*import com.hbm.Constants;
import com.hbm.blocks.ModProductionBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.compat.opencomputers.OpenComputersCompat;
import com.hbm.compat.opencomputers.OpenComputersEnvironment;
import com.hbm.compat.opencomputers.OpenComputersPeripherals;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/^*
 * GameTests verifying OpenComputers component exposure, method invocation, and sided reach.
 ^/
public final class OpenComputersGameTests {

    private OpenComputersGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function GEIGER = TESTS.function(
            "oc_geiger", 100, () -> OpenComputersGameTests::geiger);
    public static final GameTests.Function MICROWAVE = TESTS.function(
            "oc_microwave", 100, () -> OpenComputersGameTests::microwave);
    public static final GameTests.Function RBMK_CONSOLE = TESTS.function(
            "oc_rbmk_console", 100, 3, () -> OpenComputersGameTests::rbmkConsole);
    public static final GameTests.Function RBMK_FUEL_ROD = TESTS.function(
            "oc_rbmk_fuel_rod", 100, () -> OpenComputersGameTests::rbmkFuelRod);

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static void geiger(GameTestHelper helper) {
        helper.setBlock(POS, ModMachineBlocks.geiger.get().defaultBlockState());

        OpenComputersEnvironment env = OpenComputersPeripherals.at(
                helper.getLevel(), helper.absolutePos(POS),
                helper.getBlockState(POS), helper.getBlockEntity(POS), Direction.UP);

        helper.assertTrue(env != null, "Environment not found for geiger");
        helper.assertTrue("ntm_geiger".equals(env.preferredName()), "preferredName " + env.preferredName());

        List<String> methods = Arrays.asList(env.methods());
        helper.assertTrue(methods.contains("getRads"), "methods missing getRads: " + methods);

        try {
            Object[] res = env.invoke("getRads", null, new TestArguments());
            helper.assertTrue(res.length == 1, "Expected 1 result from getRads");
            helper.assertTrue(res[0] instanceof Number, "Expected number from getRads: " + res[0]);
        } catch (Exception e) {
            helper.fail("invoke getRads failed: " + e.getMessage());
        }
        helper.succeed();
    }

    private static void microwave(GameTestHelper helper) {
        helper.setBlock(POS, ModProductionBlocks.MACHINE_MICROWAVE.get().defaultBlockState());

        OpenComputersEnvironment env = OpenComputersPeripherals.at(
                helper.getLevel(), helper.absolutePos(POS),
                helper.getBlockState(POS), helper.getBlockEntity(POS), Direction.UP);

        helper.assertTrue(env != null, "Environment not found for microwave");
        helper.assertTrue("microwave".equals(env.preferredName()), "preferredName " + env.preferredName());

        List<String> methods = Arrays.asList(env.methods());
        helper.assertTrue(methods.contains("test"), "methods missing test: " + methods);
        helper.assertTrue(methods.contains("variableset"), "methods missing variableset: " + methods);
        helper.assertTrue(methods.contains("variableget"), "methods missing variableget: " + methods);

        try {
            Object[] res = env.invoke("test", null, new TestArguments());
            helper.assertTrue(res.length == 1, "Expected 1 result from test");
            helper.assertTrue("This is a testing device for everything OC.".equals(res[0]), "test() output " + res[0]);
        } catch (Exception e) {
            helper.fail("invoke test failed: " + e.getMessage());
        }
        helper.succeed();
    }

    private static void rbmkConsole(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_console.get().defaultBlockState());
        BlockPos rodPos = new BlockPos(4, 1, 1);
        helper.setBlock(rodPos, ModRBMKBlocks.rbmk_control.get().defaultBlockState());

        BlockEntityRBMKConsole be = GameTestHelpers.blockEntity(helper, POS, BlockEntityRBMKConsole.class);
        BlockEntityRBMKControlManual rod = GameTestHelpers.blockEntity(helper, rodPos, BlockEntityRBMKControlManual.class);
        BlockPos target = helper.absolutePos(rodPos);
        be.setTarget(target.getX(), target.getY(), target.getZ());

        OpenComputersEnvironment env = OpenComputersPeripherals.at(
                helper.getLevel(), helper.absolutePos(POS),
                helper.getBlockState(POS), be, Direction.NORTH);

        helper.assertTrue(env != null, "Environment not found for rbmk_console");
        helper.assertTrue("rbmk_console".equals(env.preferredName()), "preferredName " + env.preferredName());

        List<String> methods = Arrays.asList(env.methods());
        helper.assertTrue(methods.contains("setColumnLevel"), "missing setColumnLevel: " + methods);
        helper.assertTrue(methods.contains("pressAZ5"), "missing pressAZ5: " + methods);
        helper.assertTrue(methods.contains("getColumnData"), "missing getColumnData: " + methods);
        helper.assertTrue(methods.contains("getRBMKPos"), "missing getRBMKPos: " + methods);

        try {
            // Test setting column level via OpenComputers call: grid (7, 7) corresponds to target
            Object[] res = env.invoke("setColumnLevel", null, new TestArguments(7.0, 7.0, 0.75));
            helper.assertTrue(res.length == 0, "setColumnLevel returned error: " + Arrays.toString(res));
            helper.assertTrue(rod.targetLevel == 0.75, "Expected targetLevel 0.75, got " + rod.targetLevel);

            // Test AZ-5 scram via OpenComputers call
            Object[] az5Res = env.invoke("pressAZ5", null, new TestArguments());
            helper.assertTrue(az5Res.length == 0, "pressAZ5 returned error: " + Arrays.toString(az5Res));
            helper.assertTrue(rod.targetLevel == 0.0, "Expected targetLevel 0.0 after AZ-5, got " + rod.targetLevel);
        } catch (Exception e) {
            helper.fail("Console invocation failed: " + e.getMessage());
        }

        // RBMK console rescans columns periodically; verify readouts after a brief delay
        helper.runAfterDelay(15, () -> {
            try {
                Object[] dataRes = env.invoke("getColumnData", null, new TestArguments(7.0, 7.0));
                helper.assertTrue(dataRes.length == 1 && dataRes[0] instanceof Map<?, ?>,
                        "getColumnData output: " + Arrays.toString(dataRes));
                Map<?, ?> column = (Map<?, ?>) dataRes[0];
                helper.assertTrue("CONTROL".equals(column.get("type")), "Expected column type CONTROL, got " + column.get("type"));
                helper.assertTrue(column.containsKey("realSimWater"), "Missing realSimWater in column data");
                helper.assertTrue(column.containsKey("realSimSteam"), "Missing realSimSteam in column data");

                Object[] posRes = env.invoke("getRBMKPos", null, new TestArguments());
                helper.assertTrue(posRes.length == 1 && posRes[0] instanceof Map<?, ?>,
                        "getRBMKPos output: " + Arrays.toString(posRes));
                Map<?, ?> posMap = (Map<?, ?>) posRes[0];
                helper.assertTrue(Integer.valueOf(target.getX()).equals(posMap.get("rbmkCenterX")), "Centre X mismatch");

                helper.succeed();
            } catch (Exception e) {
                helper.fail("Deferred console readout failed: " + e.getMessage());
            }
        });
    }

    private static void rbmkFuelRod(GameTestHelper helper) {
        helper.setBlock(POS, ModRBMKBlocks.rbmk_fuel_rod_reasim.get().defaultBlockState());

        OpenComputersEnvironment env = OpenComputersPeripherals.at(
                helper.getLevel(), helper.absolutePos(POS),
                helper.getBlockState(POS), helper.getBlockEntity(POS), Direction.UP);

        helper.assertTrue(env != null, "Environment not found for rbmk_fuel_rod_reasim");
        helper.assertTrue("rbmk_fuel_rod".equals(env.preferredName()), "preferredName " + env.preferredName());

        List<String> methods = Arrays.asList(env.methods());
        helper.assertTrue(methods.contains("getHeat"), "methods missing getHeat: " + methods);
        helper.assertTrue(methods.contains("getInfo"), "methods missing getInfo: " + methods);

        try {
            Object[] heat = env.invoke("getHeat", null, new TestArguments());
            helper.assertTrue(heat.length == 1 && heat[0] instanceof Number, "getHeat: " + Arrays.toString(heat));

            Object[] info = env.invoke("getInfo", null, new TestArguments());
            helper.assertTrue(info.length > 0, "getInfo empty: " + Arrays.toString(info));
        } catch (Exception e) {
            helper.fail("Fuel rod invocation failed: " + e.getMessage());
        }
        helper.succeed();
    }

    @SuppressWarnings("rawtypes")
    private static final class TestArguments implements li.cil.oc.api.machine.Arguments {
        private final Object[] values;

        TestArguments(Object... values) {
            this.values = values;
        }

        @Override public int count() { return values.length; }
        @Override public Object checkAny(int index) { return values[index]; }
        @Override public boolean checkBoolean(int index) { return (Boolean) values[index]; }
        @Override public byte[] checkByteArray(int index) { return (byte[]) values[index]; }
        @Override public double checkDouble(int index) { return ((Number) values[index]).doubleValue(); }
        @Override public int checkInteger(int index) { return ((Number) values[index]).intValue(); }
        @Override public long checkLong(int index) { return ((Number) values[index]).longValue(); }
        @Override public String checkString(int index) { return (String) values[index]; }
        @Override public Map checkTable(int index) { return (Map) values[index]; }
        @Override public net.minecraft.world.item.ItemStack checkItemStack(int index) { return (net.minecraft.world.item.ItemStack) values[index]; }
        @Override public boolean isBoolean(int index) { return index < values.length && values[index] instanceof Boolean; }
        @Override public boolean isByteArray(int index) { return index < values.length && values[index] instanceof byte[]; }
        @Override public boolean isDouble(int index) { return index < values.length && values[index] instanceof Number; }
        @Override public boolean isInteger(int index) { return index < values.length && values[index] instanceof Integer; }
        @Override public boolean isLong(int index) { return index < values.length && values[index] instanceof Long; }
        @Override public boolean isString(int index) { return index < values.length && values[index] instanceof String; }
        @Override public boolean isTable(int index) { return index < values.length && values[index] instanceof Map; }
        @Override public boolean isItemStack(int index) { return index < values.length && values[index] instanceof net.minecraft.world.item.ItemStack; }
        @Override public Object optAny(int index, Object def) { return index < values.length && values[index] != null ? values[index] : def; }
        @Override public boolean optBoolean(int index, boolean def) { return index < values.length && values[index] instanceof Boolean b ? b : def; }
        @Override public byte[] optByteArray(int index, byte[] def) { return index < values.length && values[index] instanceof byte[] b ? b : def; }
        @Override public double optDouble(int index, double def) { return index < values.length && values[index] instanceof Number n ? n.doubleValue() : def; }
        @Override public int optInteger(int index, int def) { return index < values.length && values[index] instanceof Number n ? n.intValue() : def; }
        @Override public long optLong(int index, long def) { return index < values.length && values[index] instanceof Number n ? n.longValue() : def; }
        @Override public String optString(int index, String def) { return index < values.length && values[index] instanceof String s ? s : def; }
        @Override public Map optTable(int index, Map def) { return index < values.length && values[index] instanceof Map m ? m : def; }
        @Override public net.minecraft.world.item.ItemStack optItemStack(int index, net.minecraft.world.item.ItemStack def) { return index < values.length && values[index] instanceof net.minecraft.world.item.ItemStack s ? s : def; }
        @Override public Iterator<Object> iterator() { return Arrays.asList(values).iterator(); }
        @Override public Object[] toArray() { return values.clone(); }
    }
}
*///?}
