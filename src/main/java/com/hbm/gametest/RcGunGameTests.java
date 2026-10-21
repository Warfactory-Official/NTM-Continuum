package com.hbm.gametest;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import com.mojang.logging.LogUtils;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.render.anim.BusAnimationKeyframe;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;

/** Release sweep: every gun loads and fires every round its magazines accept, on a player. */
public final class RcGunGameTests {

    private static final Logger LOG = LogUtils.getLogger();

    private RcGunGameTests() {}

    // Stinger fires only locked on, Folly only after its spin-up, the gluon gun from serverTick (as 1.7)
    private static final Set<String> NEEDS_SETUP = Set.of("gun_stinger", "gun_folly", "gun_egon");

    private static List<BulletConfig> accepted(IMagazine<BulletConfig> mag) {
        if (mag instanceof MagazineSingleTypeBase single) return single.acceptedBullets;
        if (mag instanceof MagazineBelt belt) return belt.acceptedBullets;
        return List.of();
    }

    private static int shooterEntities(GameTestHelper helper, Player player) {
        int n = 0;
        for (Entity e : helper.getLevel().getEntities(player, player.getBoundingBox().inflate(96D))) {
            if (!(e instanceof Player) && !(e instanceof ItemEntity)) n++;
        }
        return n;
    }

    private static void clear(GameTestHelper helper, Player player) {
        for (Entity e : helper.getLevel().getEntities(player, player.getBoundingBox().inflate(96D))) {
            if (!(e instanceof Player)) e.discard();
        }
    }

    /** Loads and fires every accepted round of every gun; logs one line per combination. */
    public static void gunAmmoSweep(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        player.setXRot(-90F);
        List<String> failures = new ArrayList<>();
        int combos = 0;
        int fired = 0;
        for (ItemGunBaseNT gun : new ArrayList<>(ItemGunBaseNT.INSTANCES)) {
            var id = BuiltInRegistries.ITEM.getKey(gun);
            if (!"hbm".equals(id.getNamespace())) continue;
            if (NEEDS_SETUP.contains(id.getPath())) continue;
            ItemStack probe = new ItemStack(gun);
            for (int index = 0; index < gun.getConfigCount(); index++) {
                GunConfig config = gun.getConfig(probe, index);
                Receiver[] receivers = config.getReceivers(probe);
                if (receivers == null || receivers.length == 0) continue;
                IMagazine<BulletConfig> mag = receivers[0].getMagazine(probe);
                if (mag == null) continue;
                List<BulletConfig> rounds = accepted(mag);
                for (BulletConfig round : rounds) {
                    Item ammo = round.ammoItem();
                    if (ammo == null) continue;
                    combos++;
                    String tag = id.getPath() + "[" + index + "]/" + BuiltInRegistries.ITEM.getKey(ammo).getPath();
                    try {
                        ItemStack stack = new ItemStack(gun);
                        player.getInventory().clearContent();
                        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
                        player.getInventory().add(new ItemStack(ammo, 64));
                        for (int i = 0; i < 60; i++) gun.serverTick(stack, helper.getLevel(), player, true);
                        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
                        for (int i = 0; i < 400; i++) gun.serverTick(stack, helper.getLevel(), player, true);
                        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, false);
                        for (int i = 0; i < 60; i++) gun.serverTick(stack, helper.getLevel(), player, true);
                        IMagazine<BulletConfig> live = gun.getConfig(stack, index).getReceivers(stack)[0].getMagazine(stack);
                        int loaded = live.getAmount(stack, player.getInventory());
                        if (loaded <= 0) {
                            failures.add(tag + ": reload loaded nothing");
                            continue;
                        }
                        clear(helper, player);
                        EnumKeybind key = EnumKeybind.GUN_PRIMARY;
                        gun.handleKeybind(player, player.getInventory(), stack, key, true);
                        for (int i = 0; i < 3; i++) gun.serverTick(stack, helper.getLevel(), player, true);
                        gun.handleKeybind(player, player.getInventory(), stack, key, false);
                        for (int i = 0; i < 30; i++) gun.serverTick(stack, helper.getLevel(), player, true);
                        int after = live.getAmount(stack, player.getInventory());
                        int spawned = shooterEntities(helper, player);
                        if (after < loaded || spawned > 0) fired++;
                        else failures.add(tag + ": primary fire did nothing (loaded " + loaded + ", after " + after + ")");
                        LOG.info("RCGUN {} loaded={} after={} spawned={}", tag, loaded, after, spawned);
                    } catch (RuntimeException e) {
                        failures.add(tag + ": " + e + " at " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0] : "?"));
                    } finally {
                        clear(helper, player);
                    }
                }
            }
        }
        LOG.info("RCGUN SUMMARY combos={} fired={} failures={}", combos, fired, failures.size());
        for (String f : failures) LOG.warn("RCGUN FAIL {}", f);
        helper.assertTrue(failures.isEmpty(), "gun sweep failures (" + failures.size() + "): " + failures);
        helper.succeed();
    }

    /** GUN_ANIMATION_SPEED scales keyframe durations as they are built, and 1 leaves them alone. */
    public static void gunAnimationSpeed(GameTestHelper helper) {
        double saved = BusAnimationKeyframe.speed;
        try {
            BusAnimationKeyframe.speed = 1D;
            helper.assertTrue(new BusAnimationKeyframe(1D, 500).duration == 500, "speed 1 keeps the duration");
            BusAnimationKeyframe.speed = 2D;
            helper.assertTrue(new BusAnimationKeyframe(1D, 500).duration == 250, "speed 2 halves the duration");
            BusAnimationKeyframe.speed = 0.5D;
            helper.assertTrue(new BusAnimationKeyframe(1D, 500).duration == 1000, "speed 0.5 doubles the duration");
        } finally {
            BusAnimationKeyframe.speed = saved;
        }
        helper.succeed();
    }
}
