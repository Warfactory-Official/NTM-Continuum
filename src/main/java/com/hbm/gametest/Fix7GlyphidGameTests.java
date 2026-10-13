package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm.entity.mob.glyphid.EntityGlyphidBombardier;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The glyphids against 1.7. Expected values are the 1.7
 * {@code GlyphidStatsNT} rows (the profile {@code GlyphidStats.getStats()} returns) and each class's
 * {@code setSize}, written out as literals so a change to the port's table cannot also change the expectation.
 */
public final class Fix7GlyphidGameTests {

    private Fix7GlyphidGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function ATTRIBUTES =
            TESTS.function("fix7_glyphid_attributes", 20, () -> Fix7GlyphidGameTests::attributes);
    public static final GameTests.Function MOVEMENT =
            TESTS.function("fix7_glyphid_legacy_movement", 20, () -> Fix7GlyphidGameTests::movement);

    public static void init() {}

    private static final double EPS = 1.0E-6;

    /** health, movementSpeed, attackDamage (GlyphidStatsNT), width, height (setSize), getScale(). */
    private static void check(GameTestHelper helper, EntityType<? extends EntityGlyphid> type, String name,
            double health, double speed, double damage, float width, float height, double scale) {
        EntityGlyphid bug = helper.spawnWithNoFreeWill(type, new BlockPos(1, 2, 1));
        try {
            double h = bug.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
            double s = bug.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            double d = bug.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
            helper.assertTrue(Math.abs(h - health) < EPS, name + " max health " + health + ", have " + h);
            helper.assertTrue(Math.abs(bug.getHealth() - health) < EPS, name + " spawns at full health " + health
                    + ", have " + bug.getHealth());
            helper.assertTrue(Math.abs(s - speed) < EPS, name + " movementSpeed " + speed + ", have " + s);
            helper.assertTrue(Math.abs(d - damage) < EPS, name + " attackDamage " + damage + ", have " + d);
            helper.assertTrue(Math.abs(bug.getBbWidth() - width) < EPS && Math.abs(bug.getBbHeight() - height) < EPS,
                    name + " size " + width + " x " + height + ", have " + bug.getBbWidth() + " x " + bug.getBbHeight());
            helper.assertTrue(Math.abs(bug.getGlyphidScale() - scale) < EPS, name + " scale " + scale);
            // 1.7 EntityGlyphid blastSize/blastResToDig field initialisers
            int blastSize = Math.min((int) (3 * scale) / 2, 5);
            int blastRes = Math.min((int) (50 * (scale * 2)), 150);
            helper.assertTrue(bug.blastSize == blastSize && bug.blastResToDig == blastRes,
                    name + " dig " + blastSize + "/" + blastRes + ", have " + bug.blastSize + "/" + bug.blastResToDig);
            helper.assertTrue(bug.getGlyphidArmor() == 5, name + " spawns with all five armor plates");
        } finally {
            bug.discard();
        }
    }

    public static void attributes(GameTestHelper helper) {
        check(helper, ModNtmEntities.GLYPHID.get(), "grunt", 20, 1, 2, 1.75F, 1F, 1.0);
        check(helper, EntityGlyphidBombardier.TYPE.get(), "bombardier", 15, 1, 2, 1.75F, 1F, 1.0);
        check(helper, EntityGlyphidBrawler.TYPE.get(), "brawler", 35, 1, 10, 2F, 1.125F, 1.25);
        check(helper, EntityGlyphidDigger.TYPE.get(), "digger", 50, 1, 10, 1.75F, 1F, 1.3);
        check(helper, EntityGlyphidBlaster.TYPE.get(), "blaster", 35, 1, 10, 2F, 1.125F, 1.25);
        check(helper, ModNtmEntities.GLYPHID_BEHEMOTH.get(), "behemoth", 125, 0.8, 25, 2.5F, 1.5F, 1.5);
        check(helper, ModNtmEntities.GLYPHID_BRENDA.get(), "brenda", 250, 1.2, 50, 2.5F, 1.75F, 2.0);
        check(helper, ModNtmEntities.GLYPHID_NUCLEAR.get(), "nuclear", 100, 0.8, 50, 2.5F, 1.75F, 2.0);
        check(helper, ModNtmEntities.GLYPHID_SCOUT.get(), "scout", 20, 1.5, 5, 1.25F, 0.75F, 0.75);
        helper.succeed();
    }

    /**
     * 1.7 old-AI movement: {@code getAIMoveSpeed()} is the constant 0.1 and the movementSpeed attribute times the
     * path speed becomes {@code moveForward}. So a move request of 1.0 (grunt) gives speed 0.1 and forward input 1.0,
     * and 0.8 (behemoth) gives 0.1 and 0.8, an acceleration of 0.98 x 0.1 and 0.98 x 0.08 per tick on the ground
     * (modern single-number movement would give 0.98 x 1.0).
     */
    public static void movement(GameTestHelper helper) {
        EntityGlyphid bug = helper.spawnWithNoFreeWill(ModNtmEntities.GLYPHID.get(), new BlockPos(1, 2, 1));
        try {
            bug.setSpeed(1.0F);
            helper.assertTrue(Math.abs(bug.getSpeed() - 0.1F) < EPS && Math.abs(bug.zza - 1.0F) < EPS,
                    "grunt move 1.0 -> speed 0.1, forward 1.0; have " + bug.getSpeed() + ", " + bug.zza);
            bug.setSpeed(0.8F);
            helper.assertTrue(Math.abs(bug.getSpeed() - 0.1F) < EPS && Math.abs(bug.zza - 0.8F) < EPS,
                    "behemoth move 0.8 -> speed 0.1, forward 0.8; have " + bug.getSpeed() + ", " + bug.zza);
            bug.setSpeed(0F);
            helper.assertTrue(Math.abs(bug.zza) < EPS, "a stop request leaves no forward input");
        } finally {
            bug.discard();
        }
        helper.succeed();
    }
}
