package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import com.hbm.items.weapon.sedna.impl.ItemGunEgon;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.TimeOfDay;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** The two 1.12.2 guns: the gluon gun's beam, ammo and kill, the jade shotgun's pellets, reload and flashlight. */
public final class ReloadedGunGameTests {

    private ReloadedGunGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function GLUON_BEAM =
            TESTS.function("reloaded_gun_gluon_beam", 60, () -> ReloadedGunGameTests::gluonBeam);
    public static final GameTests.Function GLUON_AMMO =
            TESTS.function("reloaded_gun_gluon_ammo", 40, () -> ReloadedGunGameTests::gluonAmmo);
    public static final GameTests.Function JADE_SHOT =
            TESTS.function("reloaded_gun_jade_shot", 20, () -> ReloadedGunGameTests::jadeShot);
    public static final GameTests.Function JADE_RELOAD =
            TESTS.function("reloaded_gun_jade_reload", 120, () -> ReloadedGunGameTests::jadeReload);

    public static void init() {}

    /**
     * Trigger held with tau ammo: the gun flags itself firing and takes 2 health a tick off the zombie in front until
     * it is gone. Released, the flag clears.
     */
    public static void gluonBeam(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        player.setYRot(0F);
        player.setXRot(0F);
        ItemStack stack = new ItemStack(ModGunItems.gun_egon.get());
        ItemGunEgon gun = (ItemGunEgon) stack.getItem();
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        player.getInventory().add(new ItemStack(ModAmmoItems.tau_uranium.get(), 64));

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 4));
        zombie.setNoAi(true);
        zombie.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 3.5D);
        float startHealth = zombie.getHealth();

        long savedTime = TimeOfDay.get(helper.getLevel());
        try {
            // equipping clears the buttons, so draw the gun before pulling the trigger
            gun.serverTick(stack, helper.getLevel(), player, true);
            TimeOfDay.set(helper.getLevel(), 1000L);
            gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, true);
            gun.serverTick(stack, helper.getLevel(), player, true);
            helper.assertTrue(ItemGunEgon.getIsFiring(stack), "the held trigger did not set the firing flag");
            helper.assertTrue(Math.abs(zombie.getHealth() - (startHealth - 2F)) < 1.0E-4F,
                    "the beam took " + (startHealth - zombie.getHealth()) + " health, not 2");

            TimeOfDay.set(helper.getLevel(), 1001L);
            for (int i = 0; i < 20 && zombie.isAlive() && !zombie.isRemoved(); i++) {
                gun.serverTick(stack, helper.getLevel(), player, true);
            }
            helper.assertTrue(zombie.isRemoved(), "the zombie was not taken apart by the beam");

            gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, false);
            gun.serverTick(stack, helper.getLevel(), player, true);
            helper.assertTrue(!ItemGunEgon.getIsFiring(stack), "the released trigger left the firing flag set");
        } finally {
            TimeOfDay.set(helper.getLevel(), savedTime);
        }
        helper.succeed();
    }

    /** 10 game ticks of beam on a stopped day clock spend 4 rounds: two on each game tick divisible by 5. */
    public static void gluonAmmo(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        player.setXRot(-90F);
        ItemStack stack = new ItemStack(ModGunItems.gun_egon.get());
        ItemGunEgon gun = (ItemGunEgon) stack.getItem();
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        player.getInventory().add(new ItemStack(ModAmmoItems.tau_uranium.get(), 64));

        long savedTime = TimeOfDay.get(helper.getLevel());
        gun.serverTick(stack, helper.getLevel(), player, true);
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, true);
        for (int tick = 1; tick <= 10; tick++) {
            helper.runAfterDelay(tick, () -> {
                TimeOfDay.set(helper.getLevel(), 1001L);
                gun.serverTick(stack, helper.getLevel(), player, true);
            });
        }
        helper.runAfterDelay(11, () -> {
            TimeOfDay.set(helper.getLevel(), savedTime);
            helper.assertTrue(ItemGunEgon.getIsFiring(stack), "the held trigger did not keep the beam on");
            helper.assertTrue(rounds(player) == 60, "10 ticks of beam spent " + (64 - rounds(player)) + " rounds, not 4");
            helper.succeed();
        });
    }

    private static int rounds(Player player) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.is(ModAmmoItems.tau_uranium.get())) n += slot.getCount();
        }
        return n;
    }

    /** One trigger pull: 8 to 12 jade pellets, each with a damage of 4 to 10 and a life of 4 to 6; one shell spent. */
    public static void jadeShot(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        ItemStack stack = new ItemStack(ModGunItems.gun_jshotgun.get());
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        for (int i = 0; i < 5; i++) gun.serverTick(stack, helper.getLevel(), player, true);
        MagazineSingleTypeBase magazine = (MagazineSingleTypeBase) gun.getPrimaryMagazine(stack, 0);
        magazine.setType(stack, XFactory10ga.g10_jade);
        magazine.setAmount(stack, 2);
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, true);
        // only this player's pellets: neighbouring tests fire guns too
        List<EntityBulletBaseMK4> pellets = helper.getLevel().getEntitiesOfClass(EntityBulletBaseMK4.class,
                player.getBoundingBox().inflate(64D), bullet -> bullet.thrower == player);
        int fired = pellets.size();
        helper.assertTrue(fired >= 8 && fired <= 12, "the jade shotgun fired " + fired + " pellets, not 8 to 12");
        for (EntityBulletBaseMK4 pellet : pellets) {
            helper.assertTrue(pellet.config == XFactory10ga.g10_jade, "a pellet was not g10_jade");
            helper.assertTrue(pellet.damage >= 4F && pellet.damage <= 10F, "a pellet carried damage " + pellet.damage);
            helper.assertTrue(pellet.expiresOverride >= 4 && pellet.expiresOverride <= 6,
                    "a pellet lived " + pellet.expiresOverride + " ticks");
        }
        helper.assertTrue(magazine.getAmount(stack, player.getInventory()) == 1, "the shot did not spend one shell");
        helper.succeed();
    }

    /**
     * Empty with two shells carried: one shell after 32 ticks, the second after 31 more (1.12's single reload), and
     * the secondary button toggles the flashlight.
     */
    public static void jadeReload(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModGunItems.gun_jshotgun.get());
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        player.getInventory().add(new ItemStack(ModAmmoItems.g10_du.get(), 2));
        for (int i = 0; i < 5; i++) gun.serverTick(stack, helper.getLevel(), player, true);
        MagazineSingleTypeBase magazine = (MagazineSingleTypeBase) gun.getPrimaryMagazine(stack, 0);

        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        for (int i = 0; i < 31; i++) gun.serverTick(stack, helper.getLevel(), player, true);
        helper.assertTrue(magazine.getAmount(stack, player.getInventory()) == 0, "a shell went in before tick 32");
        gun.serverTick(stack, helper.getLevel(), player, true);
        helper.assertTrue(magazine.getAmount(stack, player.getInventory()) == 1, "no shell at tick 32");
        for (int i = 0; i < 31; i++) gun.serverTick(stack, helper.getLevel(), player, true);
        helper.assertTrue(magazine.getAmount(stack, player.getInventory()) == 2, "no second shell 31 ticks later");

        boolean before = XFactory10ga.getFlashlightActive(stack);
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_SECONDARY, true);
        helper.assertTrue(XFactory10ga.getFlashlightActive(stack) != before, "the secondary button did not toggle the flashlight");
        helper.succeed();
    }
}
