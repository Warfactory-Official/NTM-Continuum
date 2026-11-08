package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.ItemAmmoArty;
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.tileentity.turret.TileEntityTurretArty;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.tileentity.turret.TileEntityTurretJeremy;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * GitHub issues 159 and 160: the 16" gun and the Jeremy eject 1.7's spent casings after their delays (7 and 22 ticks),
 * the 16" shell flies on gravity alone as 1.7's, and the charge thrower's hook stays stuck in the wall it hits instead of
 * ricocheting away.
 */
public final class Gh159To160GameTests {

    private Gh159To160GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ARTY_CASING =
            TESTS.function("gh159_arty_ejects_16inch_casing", 60, 24, () -> Gh159To160GameTests::artyCasing);
    public static final GameTests.Function JEREMY_CASING =
            TESTS.function("gh159_jeremy_ejects_240mm_casing", 300, 24, () -> Gh159To160GameTests::jeremyCasing);
    public static final GameTests.Function SHELL_FLIGHT =
            TESTS.function("gh159_arty_shell_flies_on_gravity_alone", 40, 8, () -> Gh159To160GameTests::shellFlight);
    public static final GameTests.Function HOOK_STICKS =
            TESTS.function("gh160_hook_stays_in_the_wall", 60, 8, () -> Gh159To160GameTests::hookSticks);

    public static void init() {}

    private static void artyCasing(GameTestHelper helper) {
        BlockPos core = TurretGameTests.place(helper, new BlockPos(4, 1, 4), ModTurretBlocks.turret_arty.get());
        TileEntityTurretArty arty = (TileEntityTurretArty) helper.getLevel().getBlockEntity(core);
        BlockState state = helper.getLevel().getBlockState(core);
        arty.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST, ModAmmoItems.ammo_arty.stack(ItemAmmoArty.ArtilleryShellType.NORMAL));
        arty.mode = TileEntityTurretArty.MODE_MANUAL;

        List<CasingCreator.Event> casings = new ArrayList<>();
        CasingCreator.setSender((level, event) -> casings.add(event));
        try {
            int fired = -1;
            for (int tick = 0; tick < 60; tick++) {
                if (fired < 0) {
                    arty.tPos = Vec3.atCenterOf(core).add(0, 0, 64);
                    arty.updateFiringTick();
                    if (arty.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).isEmpty()) {
                        fired = tick;
                        // only this gun's shell, which leaves the 9-block barrel's tip; other plots keep theirs
                        for (var shell : helper.getLevel().getEntities(ModEntities.ARTILLERY_SHELL.get(),
                                new AABB(core).inflate(12D), e -> e.tickCount == 0)) shell.discard();
                    }
                }
                TileEntityTurretArty.serverTick(helper.getLevel(), core, state, arty);
                if (fired >= 0 && !named(casings, "ammo_arty").isEmpty()) {
                    // 1.7: the casing delay is 7, counted down once per tick and spent on the eighth
                    helper.assertTrue(tick - fired == 7, "the 16\" casing came " + (tick - fired) + " ticks after the shot, 1.7 waits 7");
                    helper.succeed();
                    return;
                }
            }
            helper.fail(fired < 0 ? "the gun never fired" : "the gun fired but threw no ammo_arty casing: " + casings);
        } finally {
            CasingCreator.setSender(null);
        }
    }

    private static void jeremyCasing(GameTestHelper helper) {
        BlockPos core = TurretGameTests.place(helper, new BlockPos(1, 1, 2), ModTurretBlocks.turret_jeremy.get());
        TileEntityTurretJeremy jeremy = (TileEntityTurretJeremy) helper.getLevel().getBlockEntity(core);
        jeremy.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST, new ItemStack(ModAmmoItems.shell_explosive.get(), 8));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(22, 2, 2));
        zombie.setNoGravity(true);
        zombie.setNoAi(true);
        BlockState state = helper.getLevel().getBlockState(core);
        jeremy.isOn = true;

        List<CasingCreator.Event> casings = new ArrayList<>();
        CasingCreator.setSender((level, event) -> casings.add(event));
        try {
            int rounds = jeremy.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount();
            for (int i = 0; i < 260 && named(casings, "240ext").isEmpty(); i++) {
                jeremy.setPower(jeremy.getMaxPower());
                TileEntityTurretJeremy.serverTick(helper.getLevel(), core, state, jeremy);
            }
            helper.assertTrue(jeremy.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount() < rounds,
                    "the Jeremy never fired");
            List<CasingCreator.Event> shells = named(casings, "240ext");
            helper.assertTrue(!shells.isEmpty(), "the Jeremy fired but threw no 240ext casing: " + casings);
            helper.assertTrue(shells.get(0).smoking() && shells.get(0).smokeLife() == 100,
                    "1.7 smokes the 240 mm casing for 100 ticks: " + shells.get(0));
        } finally {
            CasingCreator.setSender(null);
        }
        zombie.discard();
        helper.succeed();
    }

    /**
     * 1.7 {@code EntityThrowableNT}: each tick the shell moves by its motion, then loses 9.81 * 0.05 of vertical speed.
     * Ticked by hand, outside the level, so the turret tests running beside this one cannot shoot it down.
     */
    private static void shellFlight(GameTestHelper helper) {
        Vec3 start = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        Vec3 motion = new Vec3(0.2D, 0.6D, 0.3D);
        EntityArtilleryShell shell = new EntityArtilleryShell(ModEntities.ARTILLERY_SHELL.get(), helper.getLevel());
        shell.setPos(start.x, start.y, start.z);
        shell.setDeltaMovement(motion);
        shell.setTarget(start.x + 500D, start.y - 40D, start.z - 300D);
        int n = 3;
        for (int i = 0; i < n; i++) shell.tick();
        double g = 9.81D * 0.05D;
        Vec3 expectedMotion = new Vec3(motion.x, motion.y - n * g, motion.z);
        Vec3 expectedFlown = motion.scale(n).subtract(0D, g * n * (n - 1) / 2D, 0D);
        shell.killAndClear();
        helper.assertTrue(shell.getDeltaMovement().distanceTo(expectedMotion) < 1.0E-9D,
                "after " + n + " ticks the motion is " + shell.getDeltaMovement() + ", gravity alone gives " + expectedMotion);
        // plots sit millions of blocks out, so compare the distance flown rather than the coordinates
        Vec3 flown = shell.position().subtract(start);
        helper.assertTrue(flown.distanceTo(expectedFlown) < 1.0E-6D,
                "after " + n + " ticks the shell has flown " + flown + ", gravity alone gives " + expectedFlown);
        helper.succeed();
    }

    private static void hookSticks(GameTestHelper helper) {
        BlockPos wall = helper.absolutePos(new BlockPos(1, 2, 6));
        helper.getLevel().setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        Vec3 from = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        EntityBulletBaseMK4 hook = new EntityBulletBaseMK4(helper.getLevel(), null, XFactoryTool.ct_hook, 0F, 0F,
                from.x, from.y, from.z, 0D, 0D, 1D);
        helper.getLevel().addFreshEntity(hook);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(!hook.isRemoved(), "the hook was removed after hitting the wall");
            helper.assertTrue(hook.getZ() > wall.getZ() - 0.5D && hook.getZ() < wall.getZ() + 0.1D,
                    "the hook is not at the wall's face: z " + hook.getZ() + ", wall " + wall.getZ());
            helper.assertTrue(hook.getDeltaMovement().horizontalDistance() < 0.01D, "the stuck hook still moves: "
                    + hook.getDeltaMovement());
            hook.discard();
            helper.succeed();
        });
    }

    private static List<CasingCreator.Event> named(List<CasingCreator.Event> casings, String name) {
        List<CasingCreator.Event> out = new ArrayList<>();
        for (CasingCreator.Event event : casings) if (name.equals(event.casing())) out.add(event);
        return out;
    }
}
