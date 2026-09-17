package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.items.weapon.sedna.factory.XFactory35800;
import com.hbm.items.weapon.sedna.factory.XFactory40mm;
import com.hbm.items.weapon.sedna.factory.XFactory45;
import com.hbm.items.weapon.sedna.factory.XFactory75Bolt;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import dev.golem.core.gametest.GameTests;
import net.minecraft.gametest.framework.GameTestHelper;

/** Focused contracts for the remaining original-hbm ballistic families. */
public final class BallisticGunFactoryGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function FAMILIES = TESTS.function(
            "ballistic_factory_families", 20, () -> BallisticGunFactoryGameTests::families);

    private BallisticGunFactoryGameTests() {}

    public static void init() {}

    public static void families(GameTestHelper helper) {
        assertFamily(helper, XFactory45.all(), 5);
        assertFamily(helper, XFactory762mm.all(), 6);
        assertFamily(helper, XFactory12ga.all(), 9);
        assertFamily(helper, XFactory40mm.all40(), 5);
        assertFamily(helper, XFactory50.all(), 7);
        assertFamily(helper, XFactory75Bolt.all(), 3);
        assertFamily(helper, XFactory10ga.all(), 5);
        assertFamily(helper, XFactory35800.all(), 2);
        helper.assertTrue(XFactory12ga.g12_bp.projectilesMin == 8
                        && XFactory12ga.g12_bp.damageMult == 0.75F / 8F
                        && XFactory10ga.g10_shrapnel.maxRicochetCount == 15,
                "shotgun projectile formulas changed");
        helper.assertTrue(XFactory45.p45_ap.doesPenetrate
                        && XFactory45.p45_ap.damageMult == 1.25F
                        && XFactory762mm.r762_he.wear == 3F,
                "small-rifle AP/HE formulas changed");
        helper.assertTrue(XFactory35800.p35800.pType == BulletConfig.ProjectileType.BEAM
                        && XFactory35800.p35800.armorThresholdNegation == 50F,
                "secret .358-00 beam definition changed");
        helper.assertTrue(BulletConfigs.get(XFactory75Bolt.b75.id) == XFactory75Bolt.b75,
                "factory config ids must resolve through BulletConfigs");
        helper.succeed();
    }

    private static void assertFamily(GameTestHelper helper, BulletConfig[] configs, int expected) {
        helper.assertTrue(configs.length == expected, "expected " + expected + " configs, got " + configs.length);
        for (BulletConfig config : configs) {
            helper.assertTrue(config != null && config.id != null && BulletConfigs.get(config.id) == config,
                    "unregistered ballistic config: " + config);
        }
    }
}
