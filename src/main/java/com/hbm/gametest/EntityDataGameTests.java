package com.hbm.gametest;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.EntityHbmProps.ContaminationEffect;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.registry.Entities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/**
 * The per-entity HBM state ({@link HbmLivingProps#DATA}) through the real entity save/load path of each loader:
 * NeoForge/Fabric data attachments and Forge capabilities. The bug class is silent data loss, so every
 * persisted field is set to a distinctive value and read back, the non-persisted ones are checked to reset,
 * and the old v1 {@code HbmLivingProps} child tag is fed in by hand to prove the one-time migration.
 */
public final class EntityDataGameTests {

    private EntityDataGameTests() {}

    private static final String ATTACHMENT = "hbm:living_props";
    private static final String LEGACY = "HbmLivingProps";

    /** A mob: set every field, save the entity, load it into a new one, compare. */
    public static void mobRoundTrip(GameTestHelper helper) {
        LivingEntity pig = Entities.createDetached(EntityType.PIG, helper.getLevel());
        fill(HbmLivingProps.getData(pig));

        CompoundTag saved = Entities.saveToTag(pig);
        helper.assertTrue(!saved.contains(LEGACY), "the old HbmLivingProps child tag must no longer be written");
        helper.assertTrue(saved.toString().contains(ATTACHMENT), "the entity save must carry " + ATTACHMENT + ": " + saved);

        LivingEntity copy = Entities.createDetached(EntityType.PIG, helper.getLevel());
        helper.assertTrue(!HbmLivingProps.DATA.has(copy), "a fresh entity must not have HBM data before it loads");
        Entities.loadFromTag(copy, saved);
        helper.assertTrue(HbmLivingProps.DATA.has(copy), "loading the save must restore the HBM data");
        EntityHbmProps loaded = HbmLivingProps.getData(copy);
        helper.assertTrue(loaded != HbmLivingProps.getData(pig), "the loaded entity must own its own value");
        assertFilled(helper, loaded, "mob");
        helper.succeed();
    }

    /** A player: the player-only fields (shield, reputation, marks, toggles) ride the same storage. */
    public static void playerRoundTrip(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        fill(HbmLivingProps.getData(player));

        CompoundTag saved = Entities.saveToTag(player);
        helper.assertTrue(!saved.contains(LEGACY), "the old HbmLivingProps child tag must no longer be written");
        helper.assertTrue(saved.toString().contains(ATTACHMENT), "the player save must carry " + ATTACHMENT);

        Player copy = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        Entities.loadFromTag(copy, saved);
        assertFilled(helper, HbmLivingProps.getData(copy), "player");
        helper.succeed();
    }

    /**
     * A save written before the attachment migration: a v1 {@code HbmLivingProps} child at the root of the entity
     * data, built by hand from the old key names. It must arrive in the attachment, and the next save must carry
     * the attachment and not the old child.
     */
    public static void legacyMigration(GameTestHelper helper) {
        LivingEntity blank = Entities.createDetached(EntityType.PIG, helper.getLevel());
        CompoundTag old = Entities.saveToTag(blank);
        helper.assertTrue(!old.toString().contains(ATTACHMENT), "an untouched entity must save no HBM data: " + old);
        old.put(LEGACY, legacyV1Tag());

        LivingEntity migrated = Entities.createDetached(EntityType.PIG, helper.getLevel());
        Entities.loadFromTag(migrated, old);
        helper.assertTrue(HbmLivingProps.DATA.has(migrated), "the legacy tag must create the HBM data on load");
        assertFilled(helper, HbmLivingProps.getData(migrated), "migrated");

        CompoundTag resaved = Entities.saveToTag(migrated);
        helper.assertTrue(!resaved.contains(LEGACY), "the old child tag must be dropped on the next save");
        helper.assertTrue(resaved.toString().contains(ATTACHMENT), "the next save must carry " + ATTACHMENT);

        LivingEntity again = Entities.createDetached(EntityType.PIG, helper.getLevel());
        Entities.loadFromTag(again, resaved);
        assertFilled(helper, HbmLivingProps.getData(again), "migrated and re-saved");
        helper.succeed();
    }

    /** Stored attachment data wins over a stale legacy tag: the migration only fills an entity that has none. */
    public static void legacyDoesNotOverride(GameTestHelper helper) {
        LivingEntity pig = Entities.createDetached(EntityType.PIG, helper.getLevel());
        fill(HbmLivingProps.getData(pig));
        CompoundTag saved = Entities.saveToTag(pig);
        CompoundTag stale = legacyV1Tag();
        stale.putDouble("rads", 99D);
        stale.putInt("reputation", 3);
        saved.put(LEGACY, stale);

        LivingEntity copy = Entities.createDetached(EntityType.PIG, helper.getLevel());
        Entities.loadFromTag(copy, saved);
        assertFilled(helper, HbmLivingProps.getData(copy), "attachment over legacy");
        helper.succeed();
    }

    // Fixtures

    // Binary-exact values, so equality is exact; every one differs from its default.
    private static void fill(EntityHbmProps p) {
        p.setRads(1234.5D);
        p.setNeutrons(12.25D);
        p.setRadsEnv(3.5D);
        p.setRadBuf(7.75D);
        p.setDigamma(0.625D);
        p.setAsbestos(111);
        p.setBlacklung(222);
        p.setContagion(444);
        p.setOil(555);
        p.setFire(666);
        p.setPhosphorus(777);
        p.setBalefire(888);
        p.getContaminationEffectList().clear();
        ContaminationEffect first = new ContaminationEffect(10.5D, 100, true);
        first.time = 40;
        p.getContaminationEffectList().add(first);
        p.getContaminationEffectList().add(new ContaminationEffect(2.25D, 60, false));
        p.setEnableBackpack(false);
        p.setEnableHUD(false);
        p.shield = 4.5F;
        p.maxShield = 9.0F;
        p.reputation = -7;
        p.enableMagnet = false;
        p.maskManTimer = 42;
        p.fbiMark = 123_456_789_012L;
        p.radMark = true;
        // Live-only state: 1.7 never persisted these, so they must come back at their defaults.
        p.grenadeDeployment = 5;
        p.lastDamage = 99;
    }

    /** The same values, spelled as the pre-attachment v1 child tag (the EntityHbmProps.save layout). */
    private static CompoundTag legacyV1Tag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("fmt", "v1");
        tag.putDouble("rads", 1234.5D);
        tag.putDouble("neutrons", 12.25D);
        tag.putDouble("envRads", 3.5D);
        tag.putDouble("radBuf", 7.75D);
        tag.putDouble("digamma", 0.625D);
        tag.putInt("asbestos", 111);
        tag.putInt("blacklung", 222);
        tag.putInt("bombtimer", 333);
        tag.putInt("contagion", 444);
        tag.putInt("oil", 555);
        tag.putInt("fire", 666);
        tag.putInt("phosphorus", 777);
        tag.putInt("balefire", 888);
        tag.putBoolean("enableBackpack", false);
        tag.putBoolean("enableHUD", false);
        tag.putFloat("shield", 4.5F);
        tag.putFloat("maxShield", 9.0F);
        tag.putInt("reputation", -7);
        tag.putBoolean("enableMagnet", false);
        tag.putInt("maskManTimer", 42);
        tag.putLong("fbiMark", 123_456_789_012L);
        tag.putBoolean("radMark", true);
        ListTag cont = new ListTag();
        cont.add(effectTag(10.5D, 100, 40, true));
        cont.add(effectTag(2.25D, 60, 60, false));
        tag.put("cont", cont);
        return tag;
    }

    private static CompoundTag effectTag(double maxRad, int maxTime, int time, boolean ignoreArmor) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("maxRad", maxRad);
        tag.putInt("maxTime", maxTime);
        tag.putInt("time", time);
        tag.putBoolean("ignoreArmor", ignoreArmor);
        return tag;
    }

    private static void assertFilled(GameTestHelper helper, EntityHbmProps p, String label) {
        check(helper, p.getRads() == 1234.5D, label, "rads", p.getRads());
        check(helper, p.getNeutrons() == 12.25D, label, "neutrons", p.getNeutrons());
        check(helper, p.getRadsEnv() == 3.5D, label, "envRads", p.getRadsEnv());
        check(helper, p.getRadBuf() == 7.75D, label, "radBuf", p.getRadBuf());
        check(helper, p.getDigamma() == 0.625D, label, "digamma", p.getDigamma());
        check(helper, p.getAsbestos() == 111, label, "asbestos", p.getAsbestos());
        check(helper, p.getBlacklung() == 222, label, "blacklung", p.getBlacklung());
        check(helper, p.getContagion() == 444, label, "contagion", p.getContagion());
        check(helper, p.getOil() == 555, label, "oil", p.getOil());
        check(helper, p.getFire() == 666, label, "fire", p.getFire());
        check(helper, p.getPhosphorus() == 777, label, "phosphorus", p.getPhosphorus());
        check(helper, p.getBalefire() == 888, label, "balefire", p.getBalefire());
        var cont = p.getContaminationEffectList();
        check(helper, cont.size() == 2, label, "cont.size", cont.size());
        ContaminationEffect a = cont.get(0);
        check(helper, a.maxRad == 10.5D && a.maxTime == 100 && a.time == 40 && a.ignoreArmor, label, "cont[0]",
                a.maxRad + "/" + a.maxTime + "/" + a.time + "/" + a.ignoreArmor);
        ContaminationEffect b = cont.get(1);
        check(helper, b.maxRad == 2.25D && b.maxTime == 60 && b.time == 60 && !b.ignoreArmor, label, "cont[1]",
                b.maxRad + "/" + b.maxTime + "/" + b.time + "/" + b.ignoreArmor);
        check(helper, !p.getEnableBackpack(), label, "enableBackpack", p.getEnableBackpack());
        check(helper, !p.getEnableHUD(), label, "enableHUD", p.getEnableHUD());
        check(helper, p.shield == 4.5F, label, "shield", p.shield);
        check(helper, p.maxShield == 9.0F, label, "maxShield", p.maxShield);
        check(helper, p.reputation == -7, label, "reputation", p.reputation);
        check(helper, !p.enableMagnet, label, "enableMagnet", p.enableMagnet);
        check(helper, p.maskManTimer == 42, label, "maskManTimer", p.maskManTimer);
        check(helper, p.fbiMark == 123_456_789_012L, label, "fbiMark", p.fbiMark);
        check(helper, p.radMark, label, "radMark", p.radMark);
        check(helper, p.grenadeDeployment == 0, label, "grenadeDeployment (not persisted)", p.grenadeDeployment);
        check(helper, p.lastDamage == 0, label, "lastDamage (not persisted)", p.lastDamage);
    }

    private static void check(GameTestHelper helper, boolean ok, String label, String field, Object actual) {
        helper.assertTrue(ok, label + ": " + field + " came back as " + actual);
    }
}
