package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModWeaponModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.items.weapon.sedna.factory.XFactory35800;
import com.hbm.items.weapon.sedna.factory.XFactory40mm;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm.items.weapon.sedna.mags.MagazineEnergy;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.hbm.items.weapon.sedna.mags.MagazineInfinite;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * Fix wave phase 5 (2026-10-03, History section 19ej-5): guns, ammunition and weapon mods against original-hbm
 * (1.7.10). Every Sedna gun's configs and receivers and every registered bullet config are flattened into
 * {@code key=value} pairs with the same key scheme as the 1.7 interpreter that produced {@link Gun17Expectations};
 * the test compares every expected key and fails on any difference that is not an explained port spelling.
 */
public final class Fix5GameTests {

    private Fix5GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function GUN_PARITY =
            TESTS.function("fix5_gun_parity", 20, () -> Fix5GameTests::gunParity);
    public static final GameTests.Function LIBERATOR =
            TESTS.function("fix5_liberator_four_barrels", 300, () -> Fix5GameTests::liberatorFourBarrels);
    public static final GameTests.Function ATLAS =
            TESTS.function("fix5_atlas_delay", 200, () -> Fix5GameTests::atlasDelay);
    public static final GameTests.Function MODS =
            TESTS.function("fix5_weapon_mods_and_names", 40, () -> Fix5GameTests::weaponModsAndNames);
    public static final GameTests.Function SECRET =
            TESTS.function("fix5_secret_ammo_impacts", 200, () -> Fix5GameTests::secretAmmoImpacts);

    public static void init() {}

    // ------------------------------------------------------------------ behaviour

    private static Player player(GameTestHelper helper) {
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        return player;
    }

    private static ItemStack armed(Player player, Item gun, Item ammo, int count) {
        ItemStack stack = new ItemStack(gun);
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        player.getInventory().add(new ItemStack(ammo, count));
        return stack;
    }

    private static void tick(GameTestHelper helper, Player player, ItemStack stack, int times) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        for (int i = 0; i < times; i++) gun.serverTick(stack, helper.getLevel(), player, true);
    }

    private static void press(Player player, ItemStack stack, EnumKeybind key) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        gun.handleKeybind(player, player.getInventory(), stack, key, true);
        gun.handleKeybind(player, player.getInventory(), stack, key, false);
    }

    private static int bullets(GameTestHelper helper, Player player) {
        return helper.getLevel().getEntitiesOfClass(EntityBulletBaseMK4.class, player.getBoundingBox().inflate(64D)).size();
    }

    /** 1.7 {@code XFactory12ga}: the Liberator's {@code rounds(4)}; one pull fires 4 x 8 buckshot pellets. */
    private static void liberatorFourBarrels(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(player, ModGunItems.gun_liberator.get(), ModAmmoItems.g12.get(), 8);
        tick(helper, player, stack, 40);
        press(player, stack, EnumKeybind.RELOAD);
        tick(helper, player, stack, 200);
        IMagazine<BulletConfig> mag = ((ItemGunBaseNT) stack.getItem()).getPrimaryMagazine(stack, 0);
        int loaded = mag.getAmount(stack, player.getInventory());
        helper.assertTrue(loaded == 4, "the Liberator loads 4 shells, has " + loaded);
        int before = bullets(helper, player);
        press(player, stack, EnumKeybind.GUN_PRIMARY);
        int fired = bullets(helper, player) - before;
        helper.assertTrue(fired == 32, "one pull fires 4 rounds x 8 pellets = 32 projectiles, fired " + fired);
        helper.assertTrue(mag.getAmount(stack, player.getInventory()) == 0, "all four barrels are spent");
        helper.succeed();
    }

    /** 1.7 {@code XFactory357}: the Atlas waits 11 ticks after a shot (the port had 16). */
    private static void atlasDelay(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = armed(player, ModGunItems.gun_light_revolver_atlas.get(), ModAmmoItems.m357_sp.get(), 6);
        tick(helper, player, stack, 40);
        press(player, stack, EnumKeybind.RELOAD);
        tick(helper, player, stack, 140);
        press(player, stack, EnumKeybind.GUN_PRIMARY);
        helper.assertTrue(ItemGunBaseNT.getState(stack, 0) == ItemGunBaseNT.GunState.COOLDOWN
                && ItemGunBaseNT.getTimer(stack, 0) == 11, "after a shot the Atlas cools down for 11 ticks, state "
                + ItemGunBaseNT.getState(stack, 0) + " timer " + ItemGunBaseNT.getTimer(stack, 0));
        helper.succeed();
    }

    /**
     * Weapon mods apply through {@code XWeaponModManager.eval} (iron damage x1.15 on the pepperbox), and the 1.7 name
     * mutators rename the gun (silenced AM-180, sawed-off maresleg, scoped heavy revolver, M3 grease gun, Richter).
     */
    private static void weaponModsAndNames(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var generic = ModWeaponModItems.weapon_mod_generic.get();
        var special = ModWeaponModItems.weapon_mod_special.get();

        ItemStack pepperbox = new ItemStack(ModGunItems.gun_pepperbox.get());
        XWeaponModManager.install(level, pepperbox, 0, generic.of(GunFactory.EnumModGeneric.IRON_DAMAGE));
        float dmg = ((ItemGunBaseNT) pepperbox.getItem()).getConfig(pepperbox, 0).getReceivers(pepperbox)[0]
                .getBaseDamage(pepperbox);
        helper.assertTrue(Math.abs(dmg - 5F * 1.15F) < 1e-4F, "iron damage mod: 5 x 1.15 = 5.75, got " + dmg);

        Object[][] names = {
                {ModGunItems.gun_am180.get(), GunFactory.EnumModSpecial.SILENCER, "_silenced"},
                {ModGunItems.gun_maresleg.get(), GunFactory.EnumModSpecial.SAW, "_short"},
                {ModGunItems.gun_heavy_revolver.get(), GunFactory.EnumModSpecial.SCOPE, "_scoped"},
                {ModGunItems.gun_greasegun.get(), GunFactory.EnumModSpecial.GREASEGUN, "_m3"},
                {ModGunItems.gun_uzi.get(), GunFactory.EnumModSpecial.SILENCER, "_richter"}};
        for (Object[] row : names) {
            ItemStack gun = new ItemStack((Item) row[0]);
            ItemGunBaseNT item = (ItemGunBaseNT) gun.getItem();
            helper.assertTrue(item.LAMBDA_NAME_MUTATOR != null && item.LAMBDA_NAME_MUTATOR.apply(gun) == null,
                    gun.getItem() + " has a name mutator that leaves the plain gun alone");
            XWeaponModManager.install(level, gun, 0, special.of((Enum<?>) row[1]));
            String key = item.LAMBDA_NAME_MUTATOR.apply(gun);
            helper.assertTrue(key != null && key.equals(gun.getItem().getDescriptionId() + row[2]),
                    gun.getItem() + " with " + row[1] + " is named " + key);
        }
        helper.succeed();
    }

    private static List<Entity> near(ServerLevel level, EntityType<?> type, Vec3 at, double xz, double y) {
        return new ArrayList<>(level.getEntities(type, new AABB(at, at).inflate(xz, y, xz), e -> true));
    }

    /**
     * The 1.7 secret rounds: .44 equestrian drops a boxcar / a torpedo, 12 gauge BJ the Duchess Gambit, .50 the
     * falling building, each 50 blocks above the hit; the airdrop flare calls a C-130 on its 40th tick; the
     * 358.00 BL adds 200 ticks of black fire.
     */
    private static void secretAmmoImpacts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos hitPos = helper.absolutePos(new BlockPos(1, 1, 1));
        Vec3 hit = Vec3.atCenterOf(hitPos);
        Object[][] cases = {
                {XFactory44.m44_equestrian_pip, ModNtmEntities.BOXCAR.get()},
                {XFactory44.m44_equestrian_mn7, ModNtmEntities.TORPEDO.get()},
                {XFactory12ga.g12_equestrian_bj, ModNtmEntities.DUCHESS_GAMBIT.get()},
                {XFactory50.bmg50_equestrian, ModNtmEntities.FALLING_BUILDING.get()}};
        for (Object[] row : cases) {
            BulletConfig config = (BulletConfig) row[0];
            EntityType<?> type = (EntityType<?>) row[1];
            EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level, config, 1F, 0F, 0F, 0F);
            bullet.setPos(hit);
            helper.assertTrue(config.onImpact != null, config.id + " has an impact");
            config.onImpact.accept(bullet, new BlockHitResult(hit, Direction.UP, hitPos, false));
            List<Entity> props = near(level, type, hit, 2, 60);
            helper.assertTrue(props.size() == 1 && Math.abs(props.get(0).getY() - (hit.y + 50)) < 1e-3,
                    config.id + " drops one " + type + " 50 blocks above the hit, found " + props);
            props.forEach(Entity::discard);
        }

        BulletConfig supply = XFactory40mm.g26_flare_supply;
        EntityBulletBaseMK4 flare = new EntityBulletBaseMK4(level, supply, 1F, 0F, 0F, 0F);
        flare.setPos(hit);
        flare.tickCount = 39;
        supply.onUpdate.accept(flare);
        helper.assertTrue(near(level, ModNtmEntities.C130.get(), hit, 300, 300).isEmpty(),
                "no C-130 before the flare's 40th tick");
        LivingEntity mob = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        EntityBulletBeamBase beam = new EntityBulletBeamBase(level, XFactory35800.p35800_bl, 0F);
        XFactory35800.LAMBDA_BLACK_IMPACT.accept(beam, new EntityHitResult(mob));
        helper.assertTrue(HbmLivingProps.getBlackFire(mob) == 200,
                "358.00 BL adds 200 ticks of black fire, got " + HbmLivingProps.getBlackFire(mob));

        flare.tickCount = 40;
        supply.onUpdate.accept(flare);
        // The C-130 spawns ~200 blocks out; it becomes visible once its force-loaded chunk has loaded.
        helper.succeedWhen(() -> {
            List<Entity> planes = near(level, ModNtmEntities.C130.get(), hit, 300, 300);
            helper.assertTrue(planes.size() == 1, "the supply flare calls one C-130 on tick 40, found " + planes.size());
            planes.forEach(Entity::discard);
        });
    }

    // ------------------------------------------------------------------ flattening

    private static String f(float v) { return Float.toString(v); }
    private static String f(double v) { return Float.toString((float) v); }

    private static String lambda(Object value, Object standard) {
        if (value == null) return "None";
        if (standard != null && value == standard) return "STANDARD";
        return "set";
    }

    private static String itemPath(Item item) {
        if (item == null) return "None";
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (path.startsWith("ammo_standard_")) return path.substring("ammo_standard_".length());
        if (path.startsWith("ammo_secret_")) return path.substring("ammo_secret_".length());
        return path;
    }

    private static String soundId(SoundEvent sound) {
        if (sound == null) return "None";
        return String.valueOf(BuiltInRegistries.SOUND_EVENT.getKey(sound));
    }

    private static String bulletName(Object config) {
        if (config instanceof BulletConfig b && b.id != null) return b.id.getPath();
        return String.valueOf(config);
    }

    private static void vec(Map<String, String> out, String key, Vec3 v) {
        out.put(key + ".f", f(v.x));
        out.put(key + ".u", f(v.y));
        out.put(key + ".s", f(v.z));
    }

    private static void magazine(Map<String, String> out, String q, IMagazine<?> mag) {
        if (mag == null) return;
        String type = mag.getClass().getSimpleName();
        out.put(q + "mag.type", type);
        List<?> configs = List.of();
        if (mag instanceof MagazineSingleTypeBase single) {
            out.put(q + "mag.capacity", Integer.toString(single.capacity));
            out.put(q + "mag.index", Integer.toString(single.index));
            configs = single.acceptedBullets;
        } else if (mag instanceof MagazineBelt belt) {
            configs = belt.acceptedBullets;
        } else if (mag instanceof MagazineInfinite infinite) {
            configs = List.of(infinite.type);
        } else if (mag instanceof MagazineFluid fluid) {
            out.put(q + "mag.capacity", Integer.toString(fluid.capacity));
            out.put(q + "mag.index", Integer.toString(fluid.index));
        } else if (mag instanceof MagazineLiquidEngine engine) {
            out.put(q + "mag.capacity", Integer.toString(engine.capacity));
            out.put(q + "mag.index", Integer.toString(engine.index));
        } else if (mag instanceof MagazineElectricEngine engine) {
            out.put(q + "mag.capacity", Integer.toString(engine.capacity));
            out.put(q + "mag.index", Integer.toString(engine.index));
        } else if (mag instanceof MagazineEnergy energy) {
            out.put(q + "mag.capacity", Integer.toString(energy.capacity));
            out.put(q + "mag.index", Integer.toString(energy.index));
        }
        StringJoiner joiner = new StringJoiner(",");
        for (Object c : configs) joiner.add(bulletName(c));
        out.put(q + "mag.configs", joiner.toString());
    }

    /** The port's guns and bullets flattened into the 1.7 interpreter's key scheme. */
    public static Map<String, String> flatten() {
        Map<String, String> out = new TreeMap<>();
        for (ItemGunBaseNT gun : ItemGunBaseNT.INSTANCES) {
            String id = BuiltInRegistries.ITEM.getKey(gun).getPath();
            String p = "gun/" + id + "/";
            ItemStack stack = new ItemStack(gun);
            out.put(p + "quality", gun.quality.name());
            out.put(p + "cfgs", Integer.toString(gun.getConfigCount()));
            out.put(p + "nameMutator", Boolean.toString(gun.LAMBDA_NAME_MUTATOR != null));
            if (gun.defaultAmmo != null) {
                out.put(p + "defaultAmmo", itemPath(gun.defaultAmmo.get()) + "x" + gun.defaultAmmoAmount);
            }
            for (int ci = 0; ci < gun.getConfigCount(); ci++) {
                GunConfig c = gun.getConfig(stack, ci);
                String q = p + "c" + ci + "/";
                out.put(q + "dura", f(c.getDurability(stack)));
                out.put(q + "draw", Integer.toString(c.getDrawDuration(stack)));
                out.put(q + "inspect", Integer.toString(c.getInspectDuration(stack)));
                out.put(q + "inspectCancel", Boolean.toString(c.getInspectCancel(stack)));
                out.put(q + "crosshair", String.valueOf(c.getCrosshair(stack)));
                out.put(q + "hideCrosshair", Boolean.toString(c.getHideCrosshair(stack)));
                out.put(q + "thermalSights", Boolean.toString(c.hasThermalSights(stack)));
                out.put(q + "reloadChangeType", Boolean.toString(c.getReloadChangesType(stack)));
                out.put(q + "reloadSequential", Boolean.toString(c.getReloadAnimSequential(stack)));
                out.put(q + "pp", Boolean.toString(c.getPressPrimary(stack) != null));
                out.put(q + "ps", Boolean.toString(c.getPressSecondary(stack) != null));
                out.put(q + "pt", Boolean.toString(c.getPressTertiary(stack) != null));
                out.put(q + "pr", Boolean.toString(c.getPressReload(stack) != null));
                out.put(q + "rp", Boolean.toString(c.getReleasePrimary(stack) != null));
                out.put(q + "rs", Boolean.toString(c.getReleaseSecondary(stack) != null));
                out.put(q + "rt", Boolean.toString(c.getReleaseTertiary(stack) != null));
                out.put(q + "rr", Boolean.toString(c.getReleaseReload(stack) != null));
                out.put(q + "decider", Boolean.toString(c.getDecider(stack) != null));
                Receiver[] recs = c.getReceivers(stack);
                out.put(q + "recs", Integer.toString(recs == null ? 0 : recs.length));
                if (recs == null) continue;
                for (int ri = 0; ri < recs.length; ri++) {
                    Receiver r = recs[ri];
                    String rq = q + "r" + ri + "/";
                    out.put(rq + "dmg", f(r.getBaseDamage(stack)));
                    out.put(rq + "delayFire", Integer.toString(r.getDelayAfterFire(stack)));
                    out.put(rq + "delayDry", Integer.toString(r.getDelayAfterDryFire(stack)));
                    out.put(rq + "rounds", Integer.toString(r.getRoundsPerCycle(stack)));
                    out.put(rq + "split", f(r.getSplitProjectiles(stack)));
                    out.put(rq + "spread", f(r.getInnateSpread(stack)));
                    out.put(rq + "spreadAmmo", f(r.getAmmoSpread(stack)));
                    out.put(rq + "spreadHipfire", f(r.getHipfireSpread(stack)));
                    out.put(rq + "spreadDurability", f(r.getDurabilitySpread(stack)));
                    out.put(rq + "auto", Boolean.toString(r.getRefireOnHold(stack)));
                    out.put(rq + "autoAfterDry", Boolean.toString(r.getRefireAfterDry(stack)));
                    out.put(rq + "dryfire", Boolean.toString(r.getDoesDryFire(stack)));
                    out.put(rq + "dryfireAfterAuto", Boolean.toString(r.getDoesDryFireAfterAuto(stack)));
                    out.put(rq + "ejectOnFire", Boolean.toString(r.getEjectOnFire(stack)));
                    out.put(rq + "reloadOnEmpty", Boolean.toString(r.getReloadOnEmpty(stack)));
                    out.put(rq + "reloadBegin", Integer.toString(r.getReloadBeginDuration(stack)));
                    out.put(rq + "reloadCycle", Integer.toString(r.getReloadCycleDuration(stack)));
                    out.put(rq + "reloadEnd", Integer.toString(r.getReloadEndDuration(stack)));
                    out.put(rq + "cockPre", Integer.toString(r.getReloadCockOnEmptyPre(stack)));
                    out.put(rq + "cockPost", Integer.toString(r.getReloadCockOnEmptyPost(stack)));
                    out.put(rq + "jam", Integer.toString(r.getJamDuration(stack)));
                    out.put(rq + "sound", soundId(r.getFireSound(stack)));
                    out.put(rq + "volume", f(r.getFireVolume(stack)));
                    out.put(rq + "pitch", f(r.getFirePitch(stack)));
                    out.put(rq + "canFire", Boolean.toString(r.getCanFire(stack) != null));
                    out.put(rq + "fire", Boolean.toString(r.getOnFire(stack) != null));
                    out.put(rq + "recoil", Boolean.toString(r.getRecoil(stack) != null));
                    vec(out, rq + "offset", r.getProjectileOffset(stack));
                    vec(out, rq + "offsetScoped", r.getProjectileOffsetScoped(stack));
                    magazine(out, rq, r.getMagazine(stack));
                }
            }
        }
        for (BulletConfig b : BulletConfigs.all()) {
            String q = "bullet/" + b.id.getPath() + "/";
            out.put(q + "ammo", itemPath(b.ammoItem()));
            out.put(q + "ammoReloadCount", Integer.toString(b.ammoReloadCount));
            out.put(q + "velocity", f(b.velocity));
            out.put(q + "spread", f(b.spread));
            out.put(q + "wear", f(b.wear));
            out.put(q + "projectilesMin", Integer.toString(b.projectilesMin));
            out.put(q + "projectilesMax", Integer.toString(b.projectilesMax));
            out.put(q + "pType", b.pType.name());
            out.put(q + "damageMult", f(b.damageMult));
            out.put(q + "armorThresholdNegation", f(b.armorThresholdNegation));
            out.put(q + "armorPiercingPercent", f(b.armorPiercingPercent));
            out.put(q + "knockbackMult", f(b.knockbackMult));
            out.put(q + "headshotMult", f(b.headshotMult));
            out.put(q + "dmgClass", b.dmgClass.name());
            out.put(q + "ricochetAngle", f(b.ricochetAngle));
            out.put(q + "maxRicochetCount", Integer.toString(b.maxRicochetCount));
            out.put(q + "damageFalloffByPen", Boolean.toString(b.damageFalloffByPen));
            out.put(q + "gravity", f(b.gravity));
            out.put(q + "expires", Integer.toString(b.expires));
            out.put(q + "impactsEntities", Boolean.toString(b.impactsEntities));
            out.put(q + "doesPenetrate", Boolean.toString(b.doesPenetrate));
            out.put(q + "isSpectral", Boolean.toString(b.isSpectral));
            out.put(q + "selfDamageDelay", Integer.toString(b.selfDamageDelay));
            out.put(q + "blackPowder", Boolean.toString(b.blackPowder));
            out.put(q + "renderRotations", Boolean.toString(b.renderRotations));
            out.put(q + "casingAmount", Integer.toString(b.casingAmount));
            out.put(q + "onUpdate", lambda(b.onUpdate, null));
            out.put(q + "onImpact", lambda(b.onImpact, null));
            out.put(q + "onImpactBeam", lambda(b.onImpactBeam, null));
            out.put(q + "onRicochet", lambda(b.onRicochet, BulletConfig.LAMBDA_STANDARD_RICOCHET));
            out.put(q + "onEntityHit", lambda(b.onEntityHit, BulletConfig.LAMBDA_STANDARD_ENTITY_HIT));
        }
        return out;
    }

    // ------------------------------------------------------------------ comparison

    private static boolean same(String expected, String actual) {
        if (expected.equals(actual)) return true;
        if (expected.equalsIgnoreCase(actual)) return true;
        try {
            double e = Double.parseDouble(expected);
            double a = Double.parseDouble(actual);
            return Math.abs(e - a) <= 1e-5 * Math.max(1D, Math.abs(e));
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /** Differences between the 1.7 expectations and the port, excluding explained port spellings. */
    public static List<String> differences(Map<String, String> actual) {
        List<String> diffs = new ArrayList<>();
        for (String[] row : Gun17Expectations.rows()) {
            String key = row[0];
            String expected = row[1];
            if (Gun17Expectations.ACCEPTED.containsKey(key)) continue;
            String got = actual.get(key);
            if (got == null) {
                diffs.add(key + ": expected " + expected + ", missing");
            } else if (!same(expected, got)) {
                diffs.add(key + ": expected " + expected + ", got " + got);
            }
        }
        return diffs;
    }

    private static void gunParity(GameTestHelper helper) {
        Map<String, String> actual = flatten();
        List<String> diffs = differences(actual);
        try {
            StringBuilder dump = new StringBuilder();
            actual.forEach((k, v) -> dump.append(k).append('=').append(v).append('\n'));
            Files.writeString(Path.of("fix5_gun_dump.txt"), dump.toString(), StandardCharsets.UTF_8);
            Files.writeString(Path.of("fix5_gun_diffs.txt"), String.join("\n", diffs), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // The dump is a diagnostic convenience; the assertion below is the test.
        }
        helper.assertTrue(diffs.isEmpty(), diffs.size() + " gun/bullet value(s) differ from original-hbm 1.7.10, first: "
                + (diffs.isEmpty() ? "" : diffs.get(0)) + " (full list in fix5_gun_diffs.txt)");
        helper.succeed();
    }
}
