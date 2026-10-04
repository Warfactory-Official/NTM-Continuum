package com.hbm.gametest;

import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.XFactory22lr;
import com.hbm.items.weapon.sedna.factory.XFactory357;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.items.weapon.sedna.factory.XFactory9mm;
import com.hbm.items.weapon.sedna.impl.ItemGunChemthrower;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import dev.golem.core.render.anim.BusAnimationKeyframe;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * GameTests pinning the Sedna gun engine to upstream's behaviour: the reload cycle, ammo consumption,
 * the wear curves for damage/spread/jamming, and that firing actually produces a bullet carrying the
 * loaded round's config.
 *
 * <p>These drive the item's server tick directly rather than going through the keybind payload, which
 * keeps them independent of networking — the payload's only job is to call {@code handleKeybind}, which
 * is exactly what these call.
 */
public final class GunGameTests {

    private GunGameTests() {}

    private static final Set<String> SOURCE_SECONDARY_ONLY_CONFIGS = Set.of(
            "gun_aberrator_eott", "gun_light_revolver_dani", "gun_maresleg_akimbo",
            "gun_minigun_dual", "gun_star_f_akimbo", "gun_uzi_akimbo");

    /**
     * A mock player standing inside the test structure.
     *
     * <p>{@code makeMockPlayer} builds a player that is never positioned or added to the level, so it
     * sits at the world origin — which for a GameTest is millions of blocks from the structure. Anything
     * the player spawns would land there instead of here, so it is placed explicitly.
     */
    private static Player player(GameTestHelper helper) {
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        return player;
    }

    /** A gun in hand, its magazine empty, with {@code ammoCount} rounds of {@code ammo} in inventory. */
    private static ItemStack armed(GameTestHelper helper, Player player, Item gun, Item ammo, int ammoCount) {
        ItemStack stack = new ItemStack(gun);
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        if (ammoCount > 0) player.getInventory().add(new ItemStack(ammo, ammoCount));
        return stack;
    }

    /** Runs the gun's server tick {@code times} times, as if it were held in the main hand. */
    private static void tick(GameTestHelper helper, Player player, ItemStack stack, int times) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        for (int i = 0; i < times; i++) {
            gun.serverTick(stack, helper.getLevel(), player, true);
        }
    }

    /** Brings a freshly created gun out of DRAWING and into IDLE. */
    private static void draw(GameTestHelper helper, Player player, ItemStack stack) {
        tick(helper, player, stack, 40);
    }

    private static IMagazine<BulletConfig> mag(ItemStack stack) {
        return ((ItemGunBaseNT) stack.getItem()).getPrimaryMagazine(stack, 0);
    }

    private static List<EntityBulletBaseMK4> bullets(GameTestHelper helper, Player player) {
        return helper.getLevel().getEntitiesOfClass(EntityBulletBaseMK4.class,
                player.getBoundingBox().inflate(64D));
    }

    // ---------------------------------------------------------------- reloading

    /**
     * A reload consumes rounds from the inventory and fills the magazine, and the magazine adopts the
     * type it was fed.
     */
    public static void gunReload(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_light_revolver.get(),
                ModAmmoItems.m357_sp.get(), 12);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);

        helper.assertTrue(mag(stack).getAmount(stack, player.getInventory()) == 0,
                "a new gun should start empty");

        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        helper.assertTrue(ItemGunBaseNT.getState(stack, 0) == GunState.RELOADING,
                "pressing reload on an idle gun should start a reload");

        // Reload begin is 55 ticks, plus the end delay before it returns to idle.
        tick(helper, player, stack, 120);

        int loaded = mag(stack).getAmount(stack, player.getInventory());
        helper.assertTrue(loaded == 6, "the revolver should hold 6 rounds after reloading, was " + loaded);
        helper.assertTrue(mag(stack).getType(stack, player.getInventory()) == XFactory357.m357_sp,
                "the magazine should have adopted the loaded round's type");

        int left = player.getInventory().countItem(ModAmmoItems.m357_sp.get());
        helper.assertTrue(left == 6, "6 rounds should have been consumed from the inventory, " + left + " left");
        helper.succeed();
    }

    /** A magazine holding one type refuses to be topped up with a different one. */
    public static void gunReloadKeepsType(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_light_revolver.get(),
                ModAmmoItems.m357_sp.get(), 2);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);

        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        tick(helper, player, stack, 120);
        helper.assertTrue(mag(stack).getAmount(stack, player.getInventory()) == 2,
                "only the two available rounds should have been loaded");

        // Now offer a different round: the partially loaded magazine must not take it.
        player.getInventory().add(new ItemStack(ModAmmoItems.m357_ap.get(), 8));
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, false);
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        tick(helper, player, stack, 120);

        helper.assertTrue(mag(stack).getType(stack, player.getInventory()) == XFactory357.m357_sp,
                "a partially loaded magazine must keep its own ammo type");
        helper.assertTrue(player.getInventory().countItem(ModAmmoItems.m357_ap.get()) == 8,
                "the mismatched rounds must not be consumed");
        helper.succeed();
    }

    // ---------------------------------------------------------------- firing

    /** Every registered source gun must expose a live primary input and receiver fire route. */
    public static void gunFiringRoutes(GameTestHelper helper) {
        int gunCount = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!"hbm".equals(id.getNamespace()) || !id.getPath().startsWith("gun_")
                    || !(item instanceof ItemGunBaseNT gun)) continue;
            gunCount++;
            ItemStack stack = new ItemStack(item);
            if (gun.defaultAmmo != null) {
                helper.assertTrue(gun.defaultAmmo.get() != null && gun.defaultAmmoAmount > 0,
                        id + " has an invalid source default ammo declaration");
            }
            for (int index = 0; index < gun.getConfigCount(); index++) {
                GunConfig config = gun.getConfig(stack, index);
                if (isSourceSecondaryOnly(id, index)) {
                    helper.assertTrue(config.getPressSecondary(stack) != null,
                            id + " config " + index + " has no source secondary callback");
                } else {
                    helper.assertTrue(config.getPressPrimary(stack) != null,
                            id + " config " + index + " has no primary callback");
                }
                helper.assertTrue(config.getDecider(stack) != null,
                        id + " config " + index + " has no state decider");
                Receiver[] receivers = config.getReceivers(stack);
                helper.assertTrue(receivers != null && receivers.length > 0,
                        id + " config " + index + " has no receivers");
                for (int receiverIndex = 0; receiverIndex < receivers.length; receiverIndex++) {
                    Receiver receiver = receivers[receiverIndex];
                    // PA receivers are provider-driven; every other receiver must expose its fire route.
                    if (receiver.getMagazine(stack) == null
                            && gun instanceof com.hbm.items.weapon.sedna.factory.XFactoryPA.ItemGunPA) continue;
                    helper.assertTrue(receiver.getCanFire(stack) != null && receiver.getOnFire(stack) != null,
                            id + " config " + index + " receiver " + receiverIndex
                                    + " has a dormant firing route");
                }
            }
        }
        helper.assertTrue(gunCount == 70, "source gun registry must expose 70 guns, found " + gunCount);
        helper.succeed();
    }

    /** Source akimbo/dual second rails are selected by secondary press, not primary press. */
    private static boolean isSourceSecondaryOnly(net.minecraft.resources.Identifier id, int index) {
        return "hbm".equals(id.getNamespace()) && index == 1
                && SOURCE_SECONDARY_ONLY_CONFIGS.contains(id.getPath());
    }

    /** The source debug secondary receiver remains a real six-projectile firing route. */
    public static void gunDebugSecondaryFire(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_debug.get(),
                ModAmmoItems.ammo_debug.get(), 24);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);

        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        tick(helper, player, stack, 120);

        int before = bullets(helper, player).size();
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_SECONDARY, true);
        List<EntityBulletBaseMK4> spawned = bullets(helper, player);
        helper.assertTrue(spawned.size() == before + 6,
                "debug secondary must fire its source six-pellet load, spawned " + (spawned.size() - before));
        helper.assertTrue(mag(stack).getAmount(stack, player.getInventory()) == 12,
                "debug secondary must leave the primary receiver's twelve rounds untouched");
        helper.succeed();
    }

    /** Standard, Lacunae, and both dual-minigun rails must all reach their source firing callbacks. */
    public static void minigunFiringRoutes(GameTestHelper helper) {
        Player player = player(helper);

        ItemStack standard = armed(helper, player, ModGunItems.gun_minigun.get(),
                ModAmmoItems.r762_fmj.get(), 3);
        ItemGunBaseNT standardGun = (ItemGunBaseNT) standard.getItem();
        draw(helper, player, standard);
        int bulletsBefore = bullets(helper, player).size();
        standardGun.handleKeybind(player, player.getInventory(), standard, EnumKeybind.GUN_PRIMARY, true);
        helper.assertTrue(bullets(helper, player).size() == bulletsBefore + 1,
                "standard minigun primary did not fire one source bullet");

        ItemStack lacunae = armed(helper, player, ModGunItems.gun_minigun_lacunae.get(),
                ModAmmoItems.capacitor.get(), 15);
        ItemGunBaseNT lacunaeGun = (ItemGunBaseNT) lacunae.getItem();
        draw(helper, player, lacunae);
        lacunaeGun.handleKeybind(player, player.getInventory(), lacunae, EnumKeybind.RELOAD, true);
        tick(helper, player, lacunae, 120);
        int beamsBefore = helper.getLevel().getEntitiesOfClass(
                com.hbm.entity.projectile.EntityBulletBeamBase.class,
                player.getBoundingBox().inflate(64D)).size();
        lacunaeGun.handleKeybind(player, player.getInventory(), lacunae, EnumKeybind.GUN_PRIMARY, true);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                        com.hbm.entity.projectile.EntityBulletBeamBase.class,
                        player.getBoundingBox().inflate(64D)).size() == beamsBefore + 1,
                "Lacunae minigun primary did not fire one beam");

        ItemStack dual = armed(helper, player, ModGunItems.gun_minigun_dual.get(),
                ModAmmoItems.r762_sp.get(), 4);
        ItemGunBaseNT dualGun = (ItemGunBaseNT) dual.getItem();
        draw(helper, player, dual);
        int dualBefore = bullets(helper, player).size();
        dualGun.handleKeybind(player, player.getInventory(), dual, EnumKeybind.GUN_PRIMARY, true);
        dualGun.handleKeybind(player, player.getInventory(), dual, EnumKeybind.GUN_PRIMARY, false);
        dualGun.handleKeybind(player, player.getInventory(), dual, EnumKeybind.GUN_SECONDARY, true);
        helper.assertTrue(bullets(helper, player).size() == dualBefore + 2,
                "dual minigun primary and secondary rails must each fire one bullet");
        helper.succeed();
    }

    /** LAG's source callback must retain its ordinary projectile fallback outside the inspect window. */
    public static void gunLagFiringRoute(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_lag.get(),
                ModAmmoItems.p9_jhp.get(), 1);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        tick(helper, player, stack, 120);

        int before = bullets(helper, player).size();
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, true);
        helper.assertTrue(bullets(helper, player).size() == before + 1,
                "LAG's source fire callback must fall back to standard projectile fire");
        helper.assertTrue(mag(stack).getAmount(stack, player.getInventory()) == 0,
                "LAG fallback must consume its loaded round");
        helper.succeed();
    }

    /** Pulling the trigger spawns a bullet carrying the loaded config, and spends a round. */
    public static void gunFire(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_debug.get(), ModAmmoItems.ammo_debug.get(), 12);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);

        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        tick(helper, player, stack, 120);
        int loaded = mag(stack).getAmount(stack, player.getInventory());
        helper.assertTrue(loaded == 12, "the debug gun should hold 12 rounds, was " + loaded);

        int before = bullets(helper, player).size();
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, true);

        List<EntityBulletBaseMK4> spawned = bullets(helper, player);
        helper.assertTrue(spawned.size() == before + 1,
                "firing should spawn exactly one bullet, spawned " + (spawned.size() - before));
        helper.assertTrue(spawned.get(spawned.size() - 1).config != null,
                "the spawned bullet must carry a bullet config");
        helper.assertTrue(mag(stack).getAmount(stack, player.getInventory()) == 11,
                "firing should spend exactly one round");
        helper.assertTrue(ItemGunBaseNT.getState(stack, 0) == GunState.COOLDOWN,
                "the gun should be cooling down after a shot");
        helper.succeed();
    }

    /** An empty gun does not fire, does not spawn anything, and does not go negative. */
    public static void gunDryFire(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_debug.get(), ModAmmoItems.ammo_debug.get(), 0);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);

        int before = bullets(helper, player).size();
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, true);

        helper.assertTrue(bullets(helper, player).size() == before, "an empty gun must not spawn a bullet");
        helper.assertTrue(mag(stack).getAmount(stack, player.getInventory()) == 0,
                "an empty magazine must not go negative");
        helper.succeed();
    }

    // ---------------------------------------------------------------- impact

    /**
     * A bullet in flight actually hurts what it hits.
     *
     * <p>The bullet is constructed and aimed directly rather than fired from a gun, so this covers the
     * projectile and its impact chain on their own: sweep, entity hit, damage, and the round being
     * consumed by a non-penetrating hit.
     */
    public static void bulletDamagesMob(GameTestHelper helper) {
        Player player = player(helper);
        var target = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(1, 2, 3));
        float before = target.getHealth();

        EntityBulletBaseMK4 bullet = aimedAt(helper, player, target, XFactory357.m357_sp, 10F);
        helper.getLevel().addFreshEntity(bullet);
        bullet.tick();

        helper.assertTrue(target.getHealth() < before,
                "the cow should have taken damage, health went " + before + " -> " + target.getHealth());
        helper.assertTrue(bullet.isRemoved(), "a non-penetrating round should stop on its first victim");
        helper.succeed();
    }

    /** Armour piercing gets more damage through armour than a plain round of the same damage. */
    public static void bulletArmorPiercing(GameTestHelper helper) {
        Player player = player(helper);

        float plain = damageThroughArmor(helper, player, new BlockPos(1, 2, 3), XFactory357.m357_fmj);
        float piercing = damageThroughArmor(helper, player, new BlockPos(3, 2, 3), XFactory357.m357_ap);

        helper.assertTrue(piercing > plain,
                "AP should get more damage through armour than FMJ (" + piercing + " vs " + plain + ")");
        helper.succeed();
    }

    /** Fires one round of {@code config} at an armoured zombie and reports the health it removed. */
    private static float damageThroughArmor(GameTestHelper helper, Player player, BlockPos where,
            BulletConfig config) {
        var target = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, where);
        target.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        target.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        float before = target.getHealth();

        EntityBulletBaseMK4 bullet = aimedAt(helper, player, target, config, 20F);
        helper.getLevel().addFreshEntity(bullet);
        bullet.tick();
        return before - target.getHealth();
    }

    /** A bullet placed just short of {@code target} and pointed at it. */
    private static EntityBulletBaseMK4 aimedAt(GameTestHelper helper, Player player,
            net.minecraft.world.entity.LivingEntity target, BulletConfig config, float damage) {
        EntityBulletBaseMK4 bullet =
                new EntityBulletBaseMK4(com.hbm.entity.ModEntities.BULLET_MK4.get(), helper.getLevel());
        bullet.setBulletConfig(config);
        bullet.damage = damage;
        bullet.thrower = player;
        // Start two blocks short so the first tick's sweep crosses the target's box.
        var aim = target.position().add(0D, target.getBbHeight() / 2D, 0D);
        var from = aim.subtract(0D, 0D, 2D);
        bullet.setPos(from);
        bullet.shoot(aim.x - from.x, aim.y - from.y, aim.z - from.z, 0F);
        return bullet;
    }

    /**
     * A round shatters glass and keeps going, as upstream's ricochet lambda does.
     *
     * <p>Deliberately staged well above the mock player: a bullet's sweep takes the <em>nearest</em>
     * hit along its whole path, so a shooter standing anywhere near the line of fire can absorb the
     * shot the moment it stops being exempt, and the glass then survives. Keeping the flight path clear
     * of every other entity is what makes this test deterministic rather than usually-passing.
     */
    public static void bulletBreaksGlass(GameTestHelper helper) {
        Player player = player(helper);
        BlockPos glass = new BlockPos(1, 4, 3);
        helper.setBlock(glass, net.minecraft.world.level.block.Blocks.GLASS);

        EntityBulletBaseMK4 bullet =
                new EntityBulletBaseMK4(com.hbm.entity.ModEntities.BULLET_MK4.get(), helper.getLevel());
        bullet.setBulletConfig(XFactory357.m357_sp);
        bullet.damage = 10F;
        bullet.thrower = player;
        var to = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(glass));
        bullet.setPos(to.subtract(0D, 0D, 1.5D));
        bullet.shoot(0D, 0D, 1D, 0F);

        helper.getLevel().addFreshEntity(bullet);
        bullet.tick();

        helper.assertBlockNotPresent(net.minecraft.world.level.block.Blocks.GLASS, glass);
        helper.succeed();
    }

    // ---------------------------------------------------------------- ammo differences

    /**
     * The .357 rounds differ in the ways upstream declares: AP penetrates and pierces armour, JHP hits
     * harder but is worse against armour, express wears the gun faster.
     */
    public static void ammoStats(GameTestHelper helper) {
        helper.assertTrue(XFactory357.m357_ap.doesPenetrate, "AP should penetrate");
        helper.assertTrue(!XFactory357.m357_sp.doesPenetrate, "soft point should not penetrate");
        helper.assertTrue(XFactory357.m357_ap.armorPiercingPercent > XFactory357.m357_fmj.armorPiercingPercent,
                "AP should out-pierce FMJ");
        helper.assertTrue(XFactory357.m357_jhp.armorPiercingPercent < 0F,
                "JHP's armour piercing is negative upstream");
        helper.assertTrue(XFactory357.m357_jhp.damageMult > XFactory357.m357_fmj.damageMult,
                "JHP should out-damage FMJ");
        helper.assertTrue(XFactory357.m357_express.wear > XFactory357.m357_sp.wear,
                "express rounds should wear the gun faster");
        helper.succeed();
    }

    /** The .22 LR family preserves each upstream damage, armour, and penetration distinction. */
    public static void ammo22Stats(GameTestHelper helper) {
        helper.assertTrue(XFactory22lr.p22_sp.knockbackMult == 0F,
                "soft-point .22 should have no knockback");
        helper.assertTrue(XFactory22lr.p22_fmj.damageMult == 0.8F
                        && XFactory22lr.p22_fmj.armorThresholdNegation == 1F
                        && XFactory22lr.p22_fmj.armorPiercingPercent == 0.1F,
                "FMJ .22 stats must match upstream");
        helper.assertTrue(XFactory22lr.p22_jhp.damageMult == 1.5F
                        && XFactory22lr.p22_jhp.headshotMult == 1.5F
                        && XFactory22lr.p22_jhp.armorPiercingPercent == -0.25F,
                "JHP .22 stats must match upstream");
        helper.assertTrue(XFactory22lr.p22_ap.doesPenetrate
                        && !XFactory22lr.p22_ap.damageFalloffByPen
                        && XFactory22lr.p22_ap.damageMult == 1.25F // 1.7.10 value (fix wave 5; 1.5 was 1.12.2)
                        && XFactory22lr.p22_ap.armorThresholdNegation == 2.5F
                        && XFactory22lr.p22_ap.armorPiercingPercent == 0.15F,
                "AP .22 stats must match upstream");
        helper.succeed();
    }

    /** The 9 mm rounds preserve upstream's damage, armour, and penetration distinctions. */
    public static void ammo9Stats(GameTestHelper helper) {
        helper.assertTrue(XFactory9mm.p9_sp.damageMult == 1F && !XFactory9mm.p9_sp.doesPenetrate,
                "soft-point 9 mm should be the baseline non-penetrating round");
        helper.assertTrue(XFactory9mm.p9_fmj.damageMult == 0.8F
                        && XFactory9mm.p9_fmj.armorThresholdNegation == 2F
                        && XFactory9mm.p9_fmj.armorPiercingPercent == 0.1F,
                "FMJ 9 mm stats must match upstream");
        helper.assertTrue(XFactory9mm.p9_jhp.damageMult == 1.5F
                        && XFactory9mm.p9_jhp.headshotMult == 1.5F
                        && XFactory9mm.p9_jhp.armorPiercingPercent == -0.25F,
                "JHP 9 mm stats must match upstream");
        helper.assertTrue(XFactory9mm.p9_ap.doesPenetrate
                        && !XFactory9mm.p9_ap.damageFalloffByPen
                        && XFactory9mm.p9_ap.damageMult == 1.25F // 1.7.10 value (fix wave 5; 1.5 was 1.12.2)
                        && XFactory9mm.p9_ap.armorThresholdNegation == 5F
                        && XFactory9mm.p9_ap.armorPiercingPercent == 0.15F,
                "AP 9 mm stats must match upstream");
        helper.succeed();
    }

    /** The .44 family preserves upstream's damage, armour, penetration, and black-powder distinctions. */
    public static void ammo44Stats(GameTestHelper helper) {
        helper.assertTrue(XFactory44.m44_bp.damageMult == 0.75F && XFactory44.m44_bp.blackPowder,
                "black-powder .44 must be weaker and marked black powder");
        helper.assertTrue(XFactory44.m44_fmj.damageMult == 0.8F
                        && XFactory44.m44_fmj.armorThresholdNegation == 3F
                        && XFactory44.m44_fmj.armorPiercingPercent == 0.1F,
                "FMJ .44 stats must match upstream");
        helper.assertTrue(XFactory44.m44_jhp.damageMult == 1.5F
                        && XFactory44.m44_jhp.headshotMult == 1.5F
                        && XFactory44.m44_jhp.armorPiercingPercent == -0.25F,
                "JHP .44 stats must match upstream");
        helper.assertTrue(XFactory44.m44_ap.doesPenetrate
                        && !XFactory44.m44_ap.damageFalloffByPen
                        && XFactory44.m44_ap.damageMult == 1.25F // 1.7.10 value (fix wave 5; 1.5 was 1.12.2)
                        && XFactory44.m44_ap.armorThresholdNegation == 7.5F
                        && XFactory44.m44_ap.armorPiercingPercent == 0.15F,
                "AP .44 stats must match upstream");
        helper.assertTrue(XFactory44.m44_express.doesPenetrate
                        && XFactory44.m44_express.damageMult == 1.5F
                        && XFactory44.m44_express.armorThresholdNegation == 3F
                        && XFactory44.m44_express.wear == 1.5F,
                "express .44 stats must match upstream");
        helper.assertTrue(XFactory44.m44_equestrian_pip.ammoItem() == ModAmmoItems.m44_equestrian.get()
                        && XFactory44.m44_equestrian_mn7.ammoItem() == ModAmmoItems.m44_equestrian.get()
                        && XFactory44.m44_equestrian_pip.damageMult == 0F
                        && XFactory44.m44_equestrian_mn7.damageMult == 0F,
                "both Equestrian configs must share the flattened secret ammo item and zero damage");
        helper.succeed();
    }

    /** The six .44 weapons retain their upstream receiver damage and magazine capacities. */
    public static void gun44Configs(GameTestHelper helper) {
        ItemStack henry = new ItemStack(ModGunItems.gun_henry.get());
        ItemStack lincoln = new ItemStack(ModGunItems.gun_henry_lincoln.get());
        ItemStack revolver = new ItemStack(ModGunItems.gun_heavy_revolver.get());
        ItemStack lilmac = new ItemStack(ModGunItems.gun_heavy_revolver_lilmac.get());
        ItemStack protege = new ItemStack(ModGunItems.gun_heavy_revolver_protege.get());
        ItemStack hangman = new ItemStack(ModGunItems.gun_hangman.get());

        var henryReceiver = ((ItemGunBaseNT) henry.getItem()).getConfig(henry, 0).getReceivers(henry)[0];
        var lincolnReceiver = ((ItemGunBaseNT) lincoln.getItem()).getConfig(lincoln, 0).getReceivers(lincoln)[0];
        var revolverReceiver = ((ItemGunBaseNT) revolver.getItem()).getConfig(revolver, 0).getReceivers(revolver)[0];
        var lilmacReceiver = ((ItemGunBaseNT) lilmac.getItem()).getConfig(lilmac, 0).getReceivers(lilmac)[0];
        var protegeReceiver = ((ItemGunBaseNT) protege.getItem()).getConfig(protege, 0).getReceivers(protege)[0];
        var hangmanReceiver = ((ItemGunBaseNT) hangman.getItem()).getConfig(hangman, 0).getReceivers(hangman)[0];

        helper.assertTrue(henryReceiver.getBaseDamage(henry) == 10F
                        && henryReceiver.getMagazine(henry).getCapacity(henry) == 14,
                "Henry receiver must match upstream");
        helper.assertTrue(lincolnReceiver.getBaseDamage(lincoln) == 20F
                        && lincolnReceiver.getMagazine(lincoln).getCapacity(lincoln) == 14,
                "Lincoln receiver must match upstream");
        helper.assertTrue(revolverReceiver.getBaseDamage(revolver) == 15F
                        && revolverReceiver.getMagazine(revolver).getCapacity(revolver) == 6,
                "heavy revolver receiver must match upstream");
        helper.assertTrue(lilmacReceiver.getBaseDamage(lilmac) == 30F
                        && lilmacReceiver.getMagazine(lilmac).getCapacity(lilmac) == 6,
                "Little Macintosh receiver must match upstream");
        helper.assertTrue(protegeReceiver.getBaseDamage(protege) == 30F
                        && protegeReceiver.getMagazine(protege).getCapacity(protege) == 6,
                "Protege receiver must match upstream");
        helper.assertTrue(hangmanReceiver.getBaseDamage(hangman) == 25F
                        && hangmanReceiver.getMagazine(hangman).getCapacity(hangman) == 8,
                "Hangman receiver must match upstream");
        helper.succeed();
    }

    /** Henry loads one .44 round per cycle and stops at the available inventory count. */
    public static void gun44SingleReload(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_henry.get(), ModAmmoItems.m44_sp.get(), 3);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);

        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        tick(helper, player, stack, 300);

        helper.assertTrue(mag(stack).getAmount(stack, player.getInventory()) == 3,
                "Henry should load all three available .44 rounds one at a time");
        helper.assertTrue(mag(stack).getType(stack, player.getInventory()) == XFactory44.m44_sp,
                "Henry should adopt the .44 soft-point config");
        helper.assertTrue(player.getInventory().countItem(ModAmmoItems.m44_sp.get()) == 0,
                "Henry should consume the loaded .44 rounds");
        helper.succeed();
    }

    /** The four 9 mm guns retain their upstream receiver timings and magazine layout. */
    public static void gun9Configs(GameTestHelper helper) {
        ItemStack greasegun = new ItemStack(ModGunItems.gun_greasegun.get());
        ItemStack lag = new ItemStack(ModGunItems.gun_lag.get());
        ItemStack uzi = new ItemStack(ModGunItems.gun_uzi.get());
        ItemStack akimbo = new ItemStack(ModGunItems.gun_uzi_akimbo.get());

        var grease = ((ItemGunBaseNT) greasegun.getItem()).getConfig(greasegun, 0).getReceivers(greasegun)[0];
        var longPistol = ((ItemGunBaseNT) lag.getItem()).getConfig(lag, 0).getReceivers(lag)[0];
        var uziReceiver = ((ItemGunBaseNT) uzi.getItem()).getConfig(uzi, 0).getReceivers(uzi)[0];
        var akimboFirst = ((ItemGunBaseNT) akimbo.getItem()).getConfig(akimbo, 0).getReceivers(akimbo)[0];
        var akimboSecond = ((ItemGunBaseNT) akimbo.getItem()).getConfig(akimbo, 1).getReceivers(akimbo)[0];

        helper.assertTrue(grease.getBaseDamage(greasegun) == 3F && grease.getDelayAfterFire(greasegun) == 4
                        && grease.getDelayAfterDryFire(greasegun) == 40
                        && grease.getRefireOnHold(greasegun)
                        && grease.getMagazine(greasegun).getCapacity(greasegun) == 30,
                "Grease Gun receiver must match upstream");
        helper.assertTrue(longPistol.getBaseDamage(lag) == 25F && longPistol.getDelayAfterFire(lag) == 4
                        && longPistol.getDelayAfterDryFire(lag) == 10
                        && longPistol.getMagazine(lag).getCapacity(lag) == 17,
                "LAG receiver must match upstream");
        helper.assertTrue(uziReceiver.getDelayAfterFire(uzi) == 2
                        && uziReceiver.getDelayAfterDryFire(uzi) == 25
                        && uziReceiver.getRefireOnHold(uzi)
                        && uziReceiver.getMagazine(uzi).getCapacity(uzi) == 30,
                "Uzi receiver must match upstream");
        helper.assertTrue(akimboFirst.getMagazine(akimbo).getCapacity(akimbo) == 30
                        && akimboSecond.getMagazine(akimbo).getCapacity(akimbo) == 30
                        && akimboFirst.getRefireOnHold(akimbo)
                        && akimboSecond.getRefireOnHold(akimbo)
                        && akimboFirst.getHipfireSpread(akimbo) == 0F
                        && akimboSecond.getHipfireSpread(akimbo) == 0F,
                "both akimbo receivers must have independent thirty-round magazines");
        helper.succeed();
    }

    /** A held primary trigger repeatedly fires an automatic receiver after each cooldown. */
    public static void gunAutoRefire(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_am180.get(), ModAmmoItems.p22_sp.get(), 0);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);

        IMagazine<BulletConfig> magazine = mag(stack);
        magazine.setType(stack, XFactory22lr.p22_sp);
        magazine.setAmount(stack, 30);
        helper.assertTrue(gun.getConfig(stack, 0).getReceivers(stack)[0].getRefireOnHold(stack),
                "AM180 receiver must refire while primary remains held");

        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, true);
        tick(helper, player, stack, 20);
        int remaining = magazine.getAmount(stack, player.getInventory());
        helper.assertTrue(remaining < 29,
                "held primary should fire more than the initial AM180 round, remaining=" + remaining);
        helper.succeed();
    }

    /** The Star-F accepts .22 rounds in upstream order and fills its fifteen-round magazine. */
    public static void gunStarFReload(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_star_f.get(),
                ModAmmoItems.p22_sp.get(), 20);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);

        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        tick(helper, player, stack, 100);

        helper.assertTrue(mag(stack).getAmount(stack, player.getInventory()) == 15,
                "the Star-F should load fifteen .22 rounds");
        helper.assertTrue(mag(stack).getType(stack, player.getInventory()) == XFactory22lr.p22_sp,
                "the Star-F should chamber .22 soft point rounds");
        helper.assertTrue(player.getInventory().countItem(ModAmmoItems.p22_sp.get()) == 5,
                "the Star-F should consume fifteen rounds");
        helper.succeed();
    }

    /** Each round is registered under its own id and resolves back to the same object. */
    public static void ammoRegistry(GameTestHelper helper) {
        for (BulletConfig config : XFactory357.all()) {
            helper.assertTrue(config.id != null, "every bullet config must carry a registry id");
            helper.assertTrue(com.hbm.items.weapon.sedna.BulletConfigs.get(config.id) == config,
                    "config " + config.id + " should resolve back to itself");
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------- wear

    /** Wear degrades damage and accuracy on upstream's curves, and eventually causes jams. */
    public static void gunWearCurves(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_light_revolver.get(),
                ModAmmoItems.m357_sp.get(), 0);
        var config = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0);
        float dura = config.getDurability(stack);

        ItemGunBaseNT.setWear(stack, 0, dura * 0.4F);
        helper.assertTrue(com.hbm.items.weapon.sedna.factory.Lego.getStandardWearDamage(stack, config, 0) == 1F,
                "a lightly worn gun should lose no damage");
        helper.assertTrue(com.hbm.items.weapon.sedna.factory.GunStateDecider
                .getStandardJamChance(stack, config, 0) == 0F, "a lightly worn gun should never jam");

        ItemGunBaseNT.setWear(stack, 0, dura * 0.9F);
        helper.assertTrue(com.hbm.items.weapon.sedna.factory.Lego.getStandardWearDamage(stack, config, 0) < 1F,
                "a badly worn gun should lose damage");
        helper.assertTrue(com.hbm.items.weapon.sedna.factory.Lego.getStandardWearSpread(stack, config, 0) > 0F,
                "a badly worn gun should lose accuracy");
        helper.assertTrue(com.hbm.items.weapon.sedna.factory.GunStateDecider
                .getStandardJamChance(stack, config, 0) > 0F, "a badly worn gun should be able to jam");
        helper.succeed();
    }

    /** Firing accumulates wear, capped at the gun's durability. */
    public static void gunWearAccumulates(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_debug.get(), ModAmmoItems.ammo_debug.get(), 12);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        tick(helper, player, stack, 120);

        float before = ItemGunBaseNT.getWear(stack, 0);
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, true);
        float after = ItemGunBaseNT.getWear(stack, 0);
        helper.assertTrue(after > before, "firing should add wear");

        ItemGunBaseNT.setWear(stack, 0, 100_000F);
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, false);
        helper.assertTrue(ItemGunBaseNT.getWear(stack, 0) <= 100_000F, "wear must never exceed what was set");
        helper.succeed();
    }

    // ---------------------------------------------------------------- state

    /** Gun state survives a round-trip through the item's data components. */
    public static void gunStatePersists(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_light_revolver.get(),
                ModAmmoItems.m357_ap.get(), 6);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.RELOAD, true);
        tick(helper, player, stack, 120);
        ItemGunBaseNT.setWear(stack, 0, 42F);

        ItemStack copy = stack.copy();
        helper.assertTrue(ItemGunBaseNT.getWear(copy, 0) == 42F, "wear should survive a stack copy");
        helper.assertTrue(MagazineSingleTypeBase.getMagCount(copy, 0) == 6,
                "the loaded round count should survive a stack copy");
        helper.assertTrue(mag(copy).getType(copy, player.getInventory()) == XFactory357.m357_ap,
                "the loaded ammo type should survive a stack copy");
        helper.succeed();
    }

    /** A gun that is not held resets to DRAWING, so hotbar-swapping cannot skip a reload. */
    public static void gunResetsWhenStowed(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(helper, player, ModGunItems.gun_debug.get(), ModAmmoItems.ammo_debug.get(), 0);
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        draw(helper, player, stack);
        helper.assertTrue(ItemGunBaseNT.getState(stack, 0) == GunState.IDLE, "a drawn gun should be idle");

        gun.serverTick(stack, helper.getLevel(), player, false);
        helper.assertTrue(ItemGunBaseNT.getState(stack, 0) == GunState.DRAWING,
                "a stowed gun should go back to drawing");
        helper.succeed();
    }

    /** Guns never break blocks, whatever the left button is doing. */
    public static void gunDoesNotMine(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = new ItemStack(ModGunItems.gun_debug.get());
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE);

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        boolean canDestroy = gun.compatCanDestroyBlock(stack,
                helper.getLevel().getBlockState(helper.absolutePos(pos)),
                helper.getLevel(), helper.absolutePos(pos), player);
        helper.assertTrue(!canDestroy, "a held gun must not be able to break blocks");
        helper.succeed();
    }

    /** Every current Gen-3 gun keeps its upstream recoil callback, while the debug gun has none. */
    public static void gunRecoilBindings(GameTestHelper helper) {
        assertRecoil(helper, ModGunItems.gun_light_revolver.get(), 0);
        assertRecoil(helper, ModGunItems.gun_am180.get(), 0);
        assertRecoil(helper, ModGunItems.gun_star_f.get(), 0);
        assertRecoil(helper, ModGunItems.gun_star_f_akimbo.get(), 0, 1);
        assertRecoil(helper, ModGunItems.gun_greasegun.get(), 0);
        assertRecoil(helper, ModGunItems.gun_lag.get(), 0);
        assertRecoil(helper, ModGunItems.gun_uzi.get(), 0);
        assertRecoil(helper, ModGunItems.gun_uzi_akimbo.get(), 0, 1);
        assertRecoil(helper, ModGunItems.gun_henry.get(), 0);
        assertRecoil(helper, ModGunItems.gun_henry_lincoln.get(), 0);
        assertRecoil(helper, ModGunItems.gun_heavy_revolver.get(), 0);
        assertRecoil(helper, ModGunItems.gun_heavy_revolver_lilmac.get(), 0);
        assertRecoil(helper, ModGunItems.gun_heavy_revolver_protege.get(), 0);
        assertRecoil(helper, ModGunItems.gun_hangman.get(), 0);

        ItemGunBaseNT debug = ModGunItems.gun_debug.get();
        helper.assertTrue(debug.getConfig(new ItemStack(debug), 0).getReceivers(new ItemStack(debug))[0]
                        .getRecoil(new ItemStack(debug)) == null,
                "the upstream debug gun has no recoil callback");
        helper.succeed();
    }

    /** Disabling visual recoil cannot leave a stale camera kick behind. */
    public static void gunRecoilReset(GameTestHelper helper) {
        float oldRecoilVertical = ItemGunBaseNT.recoilVertical;
        float oldRecoilHorizontal = ItemGunBaseNT.recoilHorizontal;
        float oldOffsetVertical = ItemGunBaseNT.offsetVertical;
        float oldOffsetHorizontal = ItemGunBaseNT.offsetHorizontal;
        try {
            ItemGunBaseNT.recoilVertical = 4F;
            ItemGunBaseNT.recoilHorizontal = -3F;
            ItemGunBaseNT.offsetVertical = 2F;
            ItemGunBaseNT.offsetHorizontal = -1F;
            ItemGunBaseNT.clearRecoil();
            helper.assertTrue(ItemGunBaseNT.recoilVertical == 0F && ItemGunBaseNT.recoilHorizontal == 0F
                            && ItemGunBaseNT.offsetVertical == 0F && ItemGunBaseNT.offsetHorizontal == 0F,
                    "clearing visual recoil must clear both recoil and offset accumulators");
        } finally {
            ItemGunBaseNT.recoilVertical = oldRecoilVertical;
            ItemGunBaseNT.recoilHorizontal = oldRecoilHorizontal;
            ItemGunBaseNT.offsetVertical = oldOffsetVertical;
            ItemGunBaseNT.offsetHorizontal = oldOffsetHorizontal;
        }
        helper.succeed();
    }

    private static void assertRecoil(GameTestHelper helper, Item item, int... indexes) {
        ItemGunBaseNT gun = (ItemGunBaseNT) item;
        ItemStack stack = new ItemStack(item);
        for (int index : indexes) {
            helper.assertTrue(gun.isValidConfigIndex(index), "recoil config index should be valid: " + index);
            var receivers = gun.getConfig(stack, index).getReceivers(stack);
            helper.assertTrue(receivers.length > 0 && receivers[0].getRecoil(stack) != null,
                    "missing upstream recoil callback for " + item);
        }
    }

    /** Resource and payload guards are pure data checks; no camera pixels are claimed here. */
    public static void gunAnimationMath(GameTestHelper helper) {
        BusAnimationKeyframe previous = new BusAnimationKeyframe(0D, 0, BusAnimationKeyframe.IType.BEZIER);
        previous.rightX = 25D;
        previous.rightY = 10D;
        BusAnimationKeyframe current = new BusAnimationKeyframe(10D, 100, BusAnimationKeyframe.IType.LINEAR);
        current.leftX = 75D;
        current.leftY = 10D;
        double bezier = current.interpolate(0D, 50D, previous);
        helper.assertTrue(bezier >= 0D && bezier <= 10D && Math.abs(bezier - 5D) > 0.1D,
                "BEZIER must use the imported handles rather than linear interpolation, got " + bezier);

        BusAnimationSequence sequence = new BusAnimationSequence();
        double[] transform = sequence.getTransformation(0);
        helper.assertTrue(transform[12] == 0D && transform[13] == 1D && transform[14] == 2D,
                "LAG's default rotation order must remain XYZ");
        helper.succeed();
    }

    /** The ADS FOV uses the interpolated aim progress and the upstream 0.33 zoom factor. */
    public static void gunFovMath(GameTestHelper helper) {
        float previous = ItemGunBaseNT.prevAimingProgress;
        float current = ItemGunBaseNT.aimingProgress;
        try {
            ItemGunBaseNT.prevAimingProgress = 0.25F;
            ItemGunBaseNT.aimingProgress = 0.75F;
            helper.assertTrue(Math.abs(ItemGunBaseNT.applyAimingFov(100F, 0.5F, 0.33F) - 83.5F) < 0.0001F,
                    "upstream ADS FOV interpolation must remain exact");
            helper.assertTrue(Math.abs(ItemGunBaseNT.applyAimingFov(100F, 0.5F, 0.66F) - 67F) < 0.0001F,
                    "scoped ADS FOV interpolation must remain exact");
        } finally {
            ItemGunBaseNT.prevAimingProgress = previous;
            ItemGunBaseNT.aimingProgress = current;
        }
        helper.succeed();
    }

    /** Creative-tab filtering is centralized and follows the old quality rule. */
    public static void gunCreativeQualityVisibility(GameTestHelper helper) {
        var weapons = ModTabs.get(HbmTab.WEAPONS).getKey();
        helper.assertTrue(ModTabs.shouldDisplay(weapons, ModGunItems.gun_light_revolver.get()),
                "A_SIDE weapons should be visible");
        helper.assertTrue(ModTabs.shouldDisplay(weapons, ModGunItems.gun_am180.get()),
                "A_SIDE .22 weapons should be visible");
        helper.assertTrue(!ModTabs.shouldDisplay(weapons, ModGunItems.gun_debug.get()),
                "DEBUG weapons should be hidden");
        helper.assertTrue(!ModTabs.shouldDisplay(weapons, ModGunItems.gun_star_f_akimbo.get()),
                "B_SIDE weapons should be hidden");
        helper.assertTrue(!ModTabs.shouldDisplay(weapons, ModGunItems.gun_heavy_revolver_lilmac.get()),
                "LEGENDARY weapons should be hidden");
        helper.assertTrue(ModTabs.CONTENTS.get(weapons).stream()
                        .noneMatch(item -> item.equals(ModAmmoItems.m44_equestrian)),
                "secret equestrian ammunition must remain registered but not appear in Weapons");
        helper.succeed();
    }

    /**
     * The chemthrower's tank is its receiver's magazine, not a private pair of keys beside it. Every
     * reader of a gun's ammunition goes through the receiver — the HUD rail, the tooltip, the fire
     * lambdas — so a fill that only the item can see is a fill nothing displays, and a receiver with no
     * magazine at all takes the HUD down with it.
     */
    public static void chemthrowerTank(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModGunItems.gun_chemthrower.get());
        ItemGunChemthrower gun = (ItemGunChemthrower) stack.getItem();
        IMagazine<?> magazine = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        helper.assertTrue(magazine != null, "the chemthrower's receiver must declare a magazine");
        helper.assertTrue(magazine.getCapacity(stack) == ItemGunChemthrower.CAPACITY,
                "the tank must hold " + ItemGunChemthrower.CAPACITY + "mB, holds " + magazine.getCapacity(stack));

        int rejected = gun.tryFill(Fluids.DIESEL, 500, stack);
        int moved = ItemGunChemthrower.TRANSFER_SPEED;
        helper.assertTrue(rejected == 500 - moved,
                "one fill must turn away all but " + moved + "mB, turned away " + rejected);
        helper.assertTrue(gun.getFill(stack) == moved,
                "the item must report " + moved + "mB, reports " + gun.getFill(stack));
        helper.assertTrue(magazine.getAmount(stack, null) == moved,
                "the magazine must report the fill the item took, reports " + magazine.getAmount(stack, null));
        helper.assertTrue(magazine.getType(stack, null) == Fluids.DIESEL,
                "the magazine must report the fluid that was pumped in");

        Player player = player(helper);
        helper.assertTrue((moved + "mB").equals(magazine.reportAmmoStateForHUD(stack, player)),
                "the HUD readout must be " + moved + "mB, is " + magazine.reportAmmoStateForHUD(stack, player));
        helper.assertTrue(!magazine.getIconForHUD(stack, player).isEmpty(),
                "a filled tank must give the HUD an icon to draw");

        gun.tryEmpty(Fluids.DIESEL, 5_000, stack);
        helper.assertTrue(magazine.getAmount(stack, null) == 0,
                "draining must empty the magazine, left " + magazine.getAmount(stack, null));
        helper.succeed();
    }
}
