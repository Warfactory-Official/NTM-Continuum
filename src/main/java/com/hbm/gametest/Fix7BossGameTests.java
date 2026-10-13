package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityCyberCrab;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Crabs, the hunter chopper, the UFO and Balls-O-Tron against 1.7
 * (1.7.10). Each mob is created detached from the world and its base attributes and hitbox are compared with the
 * 1.7 {@code applyEntityAttributes} and {@code setSize} values.
 */
public final class Fix7BossGameTests {

    private Fix7BossGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ATTRIBUTES =
            TESTS.function("fix7_boss_attributes", 20, () -> Fix7BossGameTests::attributes);
    public static final GameTests.Function SPAWNED_HEALTH =
            TESTS.function("caps_boss_spawned_health", 20, () -> Fix7BossGameTests::spawnedHealth);

    public static void init() {}

    private static <T extends LivingEntity> T create(GameTestHelper helper, EntityType<T> type) {
        ServerLevel level = helper.getLevel();
        T entity = Entities.createTriggered(type, level);
        helper.assertTrue(entity != null, "could not create " + EntityType.getKey(type));
        return entity;
    }

    private static void check(GameTestHelper helper, LivingEntity entity, String what, double actual, double expected) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-4D,
                EntityType.getKey(entity.getType()) + " " + what + " = " + actual + ", 1.7 " + expected);
    }

    private static void mob(GameTestHelper helper, EntityType<? extends LivingEntity> type, double health,
                            double speed, double knockback, float width, float height) {
        LivingEntity entity = create(helper, type);
        // Effective values, not base values: vanilla clamps MAX_HEALTH at 1024 unless CommonClass raised the bound
        // (fix phase CAPS), and a base-value check cannot see the clamp.
        check(helper, entity, "max health", entity.getMaxHealth(), health);
        check(helper, entity, "health at creation", entity.getHealth(), health);
        if (speed >= 0) check(helper, entity, "movement speed", entity.getAttributeValue(Attributes.MOVEMENT_SPEED), speed);
        check(helper, entity, "knockback resistance", entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), knockback);
        check(helper, entity, "width", entity.getBbWidth(), width);
        check(helper, entity, "height", entity.getBbHeight(), height);
    }

    public static void attributes(GameTestHelper helper) {
        // 1.7 EntityCyberCrab/TeslaCrab/TaintCrab applyEntityAttributes + setSize
        mob(helper, ModNtmEntities.CYBER_CRAB.get(), 4D, 0.75D, 0D, 0.75F, 0.35F);
        mob(helper, ModNtmEntities.TESLA_CRAB.get(), 10D, 0.5D, 0D, 0.75F, 1.25F);
        mob(helper, ModNtmEntities.TAINT_CRAB.get(), 25D, 0.5D, 0D, 1.25F, 1.25F);
        // 1.7 EntityHunterChopper 750 HP, 8.25 x 3; EntityUFO 20000 HP, 15 x 4
        mob(helper, ModNtmEntities.HUNTER_CHOPPER.get(), 750D, -1D, 0D, 8.25F, 3F);
        mob(helper, ModNtmEntities.UFO.get(), 20000D, -1D, 0D, 15F, 4F);
        // 1.7 EntityBOTPrimeBase 15000 HP, knockback resistance 1; the head adds speed 0.15 and is 3 x 3
        mob(helper, ModNtmEntities.BALLS_O_TRON.get(), 15000D, 0.15D, 1D, 3F, 3F);
        mob(helper, ModNtmEntities.BALLS_O_TRON_SEG.get(), 15000D, -1D, 1D, 2F, 2F);

        // 1.7 EntityMob: attack damage 2, EntityLiving: follow range 16 (the crabs keep both defaults)
        EntityCyberCrab crab = create(helper, ModNtmEntities.CYBER_CRAB.get());
        check(helper, crab, "attack damage", crab.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), 2D);
        check(helper, crab, "follow range", crab.getAttributeBaseValue(Attributes.FOLLOW_RANGE), 16D);
        // 1.7 getMaxSafePointTries: 3 with no target
        check(helper, crab, "max fall distance", crab.getMaxFallDistance(), 3D);

        helper.succeed();
    }

    /**
     * Fix phase CAPS: a UFO and a Balls-O-Tron spawned into the world have their full 1.7 health (20,000 and
     * 15,000), above vanilla's MAX_HEALTH bound of 1024, effective as well as base.
     */
    public static void spawnedHealth(GameTestHelper helper) {
        spawned(helper, ModNtmEntities.UFO.get(), 20000D);
        spawned(helper, ModNtmEntities.BALLS_O_TRON.get(), 15000D);
        helper.succeed();
    }

    private static void spawned(GameTestHelper helper, EntityType<? extends LivingEntity> type, double health) {
        LivingEntity entity = helper.spawn(type, new BlockPos(1, 2, 1));
        try {
            check(helper, entity, "spawned max health", entity.getMaxHealth(), health);
            check(helper, entity, "spawned health", entity.getHealth(), health);
            check(helper, entity, "spawned base max health", entity.getAttributeBaseValue(Attributes.MAX_HEALTH), health);
        } finally {
            entity.discard();
        }
    }
}
