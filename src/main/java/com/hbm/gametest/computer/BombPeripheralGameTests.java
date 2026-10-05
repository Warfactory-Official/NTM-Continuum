package com.hbm.gametest.computer;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.blocks.ModCustomMissileBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.compat.computercraft.bomb.LaunchPadPeripheral;
import com.hbm.compat.computercraft.bomb.LaunchTablePeripheral;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMissileItems;
import com.hbm.items.weapon.ItemMissileStandard;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import com.hbm.tileentity.bomb.TileEntityLaunchPadLarge;
import com.hbm.tileentity.bomb.TileEntityLaunchTable;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Peripheral GameTests for {@link com.hbm.compat.computercraft.bomb.BombRows}; registered only with CC: Tweaked
 * loaded. No test launches: each asserts the refusal path and the state the computer reads.
 */
public final class BombPeripheralGameTests {

    private BombPeripheralGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    private static final int PAD = AlbionPeripheralGameTests.PAD;

    public static final GameTests.Function LAUNCH_PAD = TESTS.function(
            "cc_launch_pad", 60, PAD, () -> BombPeripheralGameTests::launchPad);
    public static final GameTests.Function LAUNCH_PAD_LARGE = TESTS.function(
            "cc_launch_pad_large", 60, PAD, () -> BombPeripheralGameTests::launchPadLarge);
    public static final GameTests.Function LAUNCH_TABLE = TESTS.function(
            "cc_launch_table", 60, PAD, () -> BombPeripheralGameTests::launchTable);

    private static double num(GameTestHelper helper, Object[] result, int index) {
        return PeripheralTestSupport.number(helper, result, index);
    }

    private static void launchPad(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModMachineBlocks.launch_pad.get());
        TileEntityLaunchPad be = GameTestHelpers.blockEntity(helper, pos, TileEntityLaunchPad.class);
        LaunchPadPeripheral pad = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                LaunchPadPeripheral.class, "ntm_launch_pad");
        AlbionPeripheralGameTests.assertCells(helper, pos, pad);
        padReadsAndRefuses(helper, be, pad);
    }

    private static void launchPadLarge(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModCustomMissileBlocks.launch_pad_large.get());
        TileEntityLaunchPadLarge be = GameTestHelpers.blockEntity(helper, pos, TileEntityLaunchPadLarge.class);
        LaunchPadPeripheral pad = PeripheralTestSupport.expect(helper, pos, Direction.UP,
                LaunchPadPeripheral.class, "ntm_launch_pad");
        // the plain cell found here carries the inventory-only proxy, which 1.7 never connected
        AlbionPeripheralGameTests.assertCells(helper, pos, pad);
        padReadsAndRefuses(helper, be, pad);
    }

    private static void padReadsAndRefuses(GameTestHelper helper, TileEntityLaunchPadBase be, LaunchPadPeripheral pad) {
        helper.assertTrue(pad.getTier().length == 0, "getTier without a missile must answer nothing");

        ItemStack missile = new ItemStack(ModMissileItems.missile_generic.get());
        be.inventory.setStackInSlot(0, missile.copy());
        be.power = 0L;
        Object[] launch = pad.launch(12.7D, -3.2D);
        helper.assertTrue(launch.length == 1 && Boolean.FALSE.equals(launch[0]),
                "launch on an unpowered pad must refuse, got " + Arrays.toString(launch));
        helper.assertTrue(be.inventory.getStackInSlot(0).is(missile.getItem()), "a refused launch kept the missile");

        // below the 75 kHE launch cost, so nothing can fire while the snapshot is taken
        be.power = 50_000L;
        helper.runAfterDelay(2, () -> {
            Object[] energy = pad.getEnergyInfo();
            helper.assertTrue(num(helper, energy, 0) == 50_000 && num(helper, energy, 1) == 100_000,
                    "getEnergyInfo " + Arrays.toString(energy));
            helper.assertTrue(Boolean.FALSE.equals(pad.canLaunch()[0]), "canLaunch without fuel");
            Object[] fluid = pad.getFluid();
            helper.assertTrue(fluid.length == 6 && fluid[2] instanceof String && fluid[5] instanceof String
                    && ((String) fluid[2]).startsWith("hbmfluid."), "getFluid " + Arrays.toString(fluid));
            Object[] tier = pad.getTier();
            if (be.isMissileValid()) {
                int expected = ((ItemMissileStandard) missile.getItem()).tier.ordinal();
                helper.assertTrue(tier.length == 1 && num(helper, tier, 0) == expected,
                        "getTier " + Arrays.toString(tier) + ", expected " + expected);
            } else {
                helper.assertTrue(tier.length == 0, "getTier for an invalid missile " + Arrays.toString(tier));
            }
            Object[] at = pad.getPos();
            BlockPos core = be.getBlockPos();
            helper.assertTrue(num(helper, at, 0) == core.getX() && num(helper, at, 1) == core.getY()
                    && num(helper, at, 2) == core.getZ(), "getPos " + Arrays.toString(at));
            helper.succeed();
        });
    }

    private static void launchTable(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModCustomMissileBlocks.launch_table.get());
        TileEntityLaunchTable be = GameTestHelpers.blockEntity(helper, pos, TileEntityLaunchTable.class);
        LaunchTablePeripheral table = PeripheralTestSupport.expect(helper, pos, Direction.NORTH,
                LaunchTablePeripheral.class, "ntm_custom_launch_pad");
        // 1.7's table cells were plain dummies: not even the power/fluid ports connect
        AlbionPeripheralGameTests.assertNoCells(helper, pos, null);

        Object[] none = table.getCoords();
        helper.assertTrue(none.length == 2 && Boolean.FALSE.equals(none[0]) && "Designator not found".equals(none[1]),
                "getCoords without a designator " + Arrays.toString(none));
        helper.assertTrue(Boolean.FALSE.equals(table.setCoords(1, 2)[0]), "setCoords without a designator");

        be.inventory.setStackInSlot(1, new ItemStack(ModMissileItems.designator.get()));
        Object[] untagged = table.getCoords();
        helper.assertTrue(untagged.length == 1 && Boolean.FALSE.equals(untagged[0]),
                "getCoords of an unset designator " + Arrays.toString(untagged));
        Object[] set = table.setCoords(12.9D, -7.2D);
        helper.assertTrue(set.length == 1 && Boolean.TRUE.equals(set[0]), "setCoords " + Arrays.toString(set));
        ItemStack designator = be.inventory.getStackInSlot(1);
        helper.assertTrue(ModDataComponents.DESIGNATOR_X.getOr(designator, 0) == 12
                && ModDataComponents.DESIGNATOR_Z.getOr(designator, 0) == -7, "setCoords truncates to 12, -7");
        Object[] coords = table.getCoords();
        helper.assertTrue(num(helper, coords, 0) == 12 && num(helper, coords, 1) == -7,
                "getCoords " + Arrays.toString(coords));

        Object[] launch = table.launch();
        helper.assertTrue(launch.length == 1 && Boolean.FALSE.equals(launch[0]),
                "launch with no missile must refuse, got " + Arrays.toString(launch));

        be.power = 60_000L;
        be.solid = 250;
        helper.runAfterDelay(2, () -> {
            Object[] energy = table.getEnergyInfo();
            helper.assertTrue(num(helper, energy, 0) == 60_000 && num(helper, energy, 1) == 100_000,
                    "getEnergyInfo " + Arrays.toString(energy));
            Object[] contents = table.getContents();
            helper.assertTrue(contents.length == 8 && num(helper, contents, 6) == 250
                    && num(helper, contents, 7) == TileEntityLaunchTable.maxSolid, "getContents " + Arrays.toString(contents));
            Object[] info = table.getLaunchInfo();
            helper.assertTrue(info.length == 4 && Boolean.FALSE.equals(info[0]) && Boolean.FALSE.equals(info[1])
                    && Boolean.TRUE.equals(info[2]), "getLaunchInfo " + Arrays.toString(info));
            helper.succeed();
        });
    }
}
//?}
