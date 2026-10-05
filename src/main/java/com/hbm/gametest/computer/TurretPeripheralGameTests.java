package com.hbm.gametest.computer;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.compat.computercraft.turret.ArtyPeripheral;
import com.hbm.compat.computercraft.turret.HimarsPeripheral;
import com.hbm.compat.computercraft.turret.TurretPeripheral;
import com.hbm.items.ModMachineItems;
import com.hbm.tileentity.turret.TileEntityTurretArty;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.tileentity.turret.TileEntityTurretChekhov;
import com.hbm.tileentity.turret.TileEntityTurretHIMARS;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/** Peripheral GameTests for {@link com.hbm.compat.computercraft.turret.TurretRows}; registered only with CC: Tweaked loaded. */
public final class TurretPeripheralGameTests {

    private TurretPeripheralGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    private static final int PAD = AlbionPeripheralGameTests.PAD;

    public static final GameTests.Function TURRET = TESTS.function(
            "cc_turret", 60, PAD, () -> TurretPeripheralGameTests::turret);
    public static final GameTests.Function ARTY = TESTS.function(
            "cc_turret_arty", 60, PAD, () -> TurretPeripheralGameTests::arty);
    public static final GameTests.Function HIMARS = TESTS.function(
            "cc_turret_himars", 60, PAD, () -> TurretPeripheralGameTests::himars);

    private static double num(GameTestHelper helper, Object[] result, int index) {
        return PeripheralTestSupport.number(helper, result, index);
    }

    /** 1.7 canConnectNode: the core answers from below only, and no cell answers at all. */
    private static void assertBelowOnly(GameTestHelper helper, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (side != Direction.DOWN) PeripheralTestSupport.expectNone(helper, pos, side);
        }
        AlbionPeripheralGameTests.assertNoCells(helper, pos, Direction.DOWN);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void turret(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModTurretBlocks.turret_chekhov.get());
        TileEntityTurretChekhov be = GameTestHelpers.blockEntity(helper, pos, TileEntityTurretChekhov.class);
        TurretPeripheral turret = PeripheralTestSupport.expect(helper, pos, Direction.DOWN,
                TurretPeripheral.class, "ntm_turret");
        assertBelowOnly(helper, pos);

        turret.setActive(true);
        helper.assertTrue(be.isOn, "setActive(true)");
        turret.setTargeting(true, false, true, false);
        helper.assertTrue(be.targetPlayers && !be.targetAnimals && be.targetMobs && !be.targetMachines, "setTargeting");

        helper.assertTrue(turret.getWhitelisted().length == 0, "getWhitelisted without a chip answers nothing");
        Object[] noList = turret.removeWhitelist("Bob");
        helper.assertTrue(noList.length == 2 && noList[0] == null && "unknown error".equals(noList[1]),
                "removeWhitelist without a chip " + Arrays.toString(noList));

        be.inventory.setStackInSlot(TileEntityTurretBaseNT.SLOT_CHIP, new ItemStack(ModMachineItems.turret_chip.get()));
        helper.assertTrue(Boolean.TRUE.equals(turret.addWhitelist("Bob")[0]), "addWhitelist(Bob)");
        helper.assertTrue(Boolean.FALSE.equals(turret.addWhitelist("Bob")[0]), "addWhitelist(Bob) twice");
        Object[] listed = turret.getWhitelisted();
        helper.assertTrue(listed.length == 1 && listed[0] instanceof String[] names
                && Arrays.asList(names).contains("Bob"), "getWhitelisted " + Arrays.deepToString(listed));
        helper.assertTrue(Boolean.FALSE.equals(turret.removeWhitelist("Alice")[0]), "removeWhitelist(Alice)");
        helper.assertTrue(Boolean.TRUE.equals(turret.removeWhitelist("Bob")[0]), "removeWhitelist(Bob)");
        helper.assertTrue(be.getWhitelist() == null || !be.getWhitelist().contains("Bob"), "Bob was removed");

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Boolean.TRUE.equals(turret.isActive()[0]), "isActive");
            Object[] targeting = turret.getTargeting();
            helper.assertTrue(Arrays.equals(targeting, new Object[]{true, false, true, false}),
                    "getTargeting " + Arrays.toString(targeting));
            helper.assertTrue(num(helper, turret.getEnergyInfo(), 1) == be.getMaxPower(), "getEnergyInfo");
            helper.assertTrue(turret.getAngle().length == 2 && turret.isAligned()[0] instanceof Boolean
                    && turret.hasTarget()[0] instanceof Boolean, "getAngle/isAligned/hasTarget");
            Object[] at = turret.getPos();
            BlockPos core = be.getBlockPos();
            helper.assertTrue(num(helper, at, 0) == core.getX() && num(helper, at, 1) == core.getY()
                    && num(helper, at, 2) == core.getZ(), "getPos " + Arrays.toString(at));
            helper.succeed();
        });
    }

    private static void arty(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModTurretBlocks.turret_arty.get());
        TileEntityTurretArty be = GameTestHelpers.blockEntity(helper, pos, TileEntityTurretArty.class);
        ArtyPeripheral arty = PeripheralTestSupport.expect(helper, pos, Direction.DOWN,
                ArtyPeripheral.class, "ntm_artillery");
        assertBelowOnly(helper, pos);

        Object[] empty = arty.getCurrentTarget();
        helper.assertTrue(empty.length == 2 && empty[0] == null && "Index: 0, Size: 0".equals(empty[1]),
                "getCurrentTarget on an empty queue " + Arrays.toString(empty));

        BlockPos core = be.getBlockPos();
        Object[] far = arty.addCoords(core.getX() + 5_000D, core.getY(), core.getZ());
        helper.assertTrue(far.length == 1 && Boolean.FALSE.equals(far[0]), "addCoords out of range " + Arrays.toString(far));
        helper.assertTrue(be.mode == TileEntityTurretArty.MODE_MANUAL, "addCoords switches to manual even when refused");
        helper.assertTrue(be.queuedTargets() == 0, "a refused addCoords queued a target");
        Object[] near = arty.addCoords(core.getX() + 10.5D, core.getY(), core.getZ());
        helper.assertTrue(near.length == 1 && Boolean.TRUE.equals(near[0]), "addCoords in range " + Arrays.toString(near));
        Object[] current = arty.getCurrentTarget();
        helper.assertTrue(num(helper, current, 0) == core.getX() + 10.5D && num(helper, current, 1) == core.getY(),
                "getCurrentTarget " + Arrays.toString(current));

        helper.runAfterDelay(2, () -> {
            double distance = num(helper, arty.getTargetDistance(core.getX() + 3D, core.getY() + 4D, core.getZ()), 0);
            helper.assertTrue(distance == 5.0D, "getTargetDistance " + distance);
            helper.succeed();
        });
    }

    private static void himars(GameTestHelper helper) {
        BlockPos pos = AlbionPeripheralGameTests.assemble(helper, ModTurretBlocks.turret_himars.get());
        TileEntityTurretHIMARS be = GameTestHelpers.blockEntity(helper, pos, TileEntityTurretHIMARS.class);
        HimarsPeripheral himars = PeripheralTestSupport.expect(helper, pos, Direction.DOWN,
                HimarsPeripheral.class, "ntm_artillery");
        assertBelowOnly(helper, pos);

        BlockPos core = be.getBlockPos();
        Object[] added = himars.addCoords(core.getX() + 20D, core.getY(), core.getZ() - 4D);
        helper.assertTrue(added.length == 0, "HIMARS addCoords answers nothing");
        helper.assertTrue(be.mode == TileEntityTurretHIMARS.MODE_MANUAL && be.queuedTargets() == 1,
                "addCoords switches to manual and queues the target");
        Object[] current = himars.getCurrentTarget();
        helper.assertTrue(num(helper, current, 2) == core.getZ() - 4D, "getCurrentTarget " + Arrays.toString(current));
        helper.succeed();
    }
}
//?}
