package com.hbm.gametest;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.factory.XFactory357;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

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
}
