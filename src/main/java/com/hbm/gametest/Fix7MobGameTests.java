package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityParasiteMaggot;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Fix wave phase 7 (2026-10-03): the creepers, ducks, pigeon, plastic bag, maggot, ghost, block spider, undead
 * soldier, FBI, FBI drone, radiation beast and mask man against original-hbm (1.7.10). Each mob is created
 * detached from the world and its base attributes and hitbox are compared with the 1.7
 * {@code applyEntityAttributes} / {@code setSize} values (1.7 defaults: 20 HP, speed 0.7, follow range 16,
 * attack damage 2 on EntityMob).
 *
 * <p>The two old-AI mobs (maggot, radiation beast) walked {@code movementSpeed * 0.1} per tick of friction in 1.7
 * and goal AI walks {@code speed * speed}, so their expected base speed is the square root of that product.
 */
public final class Fix7MobGameTests {

    private Fix7MobGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ATTRIBUTES =
            TESTS.function("fix7_mob_attributes", 20, () -> Fix7MobGameTests::attributes);

    /** 2026-10-12 playtest: a nuclear creeper that blows up beside a villager and a golem must not crash the server. */
    public static final GameTests.Function NUCLEAR_CREEPER_BLAST =
            TESTS.function("pt1012_nuclear_creeper_blast", 400, 48, () -> Fix7MobGameTests::nuclearCreeperBlast);

    public static void init() {}

    public static void nuclearCreeperBlast(GameTestHelper helper) {
        net.minecraft.core.BlockPos floorCorner = helper.absolutePos(new net.minecraft.core.BlockPos(0, 0, 0));
        for (int dx = 0; dx < 40; dx++) {
            for (int dz = 0; dz < 40; dz++) {
                helper.getLevel().setBlock(floorCorner.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            }
        }
        net.minecraft.world.entity.monster.Creeper creeper =
                helper.spawnWithNoFreeWill(ModNtmEntities.CREEPER_NUCLEAR.get(), new net.minecraft.core.BlockPos(20, 1, 20));
        LivingEntity golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new net.minecraft.core.BlockPos(22, 1, 20));
        LivingEntity villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new net.minecraft.core.BlockPos(18, 1, 20));
        creeper.ignite();
        helper.succeedWhen(() -> {
            helper.assertTrue(creeper.isRemoved(), "the nuclear creeper has not exploded yet");
            helper.assertTrue(golem.getHealth() < golem.getMaxHealth() || golem.isRemoved(),
                    "the blast did not hurt the golem: " + golem.getHealth());
            helper.assertTrue(villager.getHealth() < villager.getMaxHealth() || villager.isRemoved(),
                    "the blast did not hurt the villager");
        });
    }

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

    private static LivingEntity mob(GameTestHelper helper, EntityType<? extends LivingEntity> type, double health,
                                    double speed) {
        LivingEntity entity = create(helper, type);
        check(helper, entity, "max health", entity.getAttributeBaseValue(Attributes.MAX_HEALTH), health);
        check(helper, entity, "movement speed", entity.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), speed);
        return entity;
    }

    private static void size(GameTestHelper helper, LivingEntity entity, float width, float height) {
        check(helper, entity, "width", entity.getBbWidth(), width);
        check(helper, entity, "height", entity.getBbHeight(), height);
    }

    public static void attributes(GameTestHelper helper) {
        // creepers: EntityCreeper keeps 20 HP / 0.25; nuclear 50 / 0.3; tainted 15 / 0.35
        size(helper, mob(helper, ModNtmEntities.CREEPER_NUCLEAR.get(), 50D, 0.3D), 0.6F, 1.8F);
        size(helper, mob(helper, ModNtmEntities.CREEPER_TAINTED.get(), 15D, 0.35D), 0.6F, 1.8F);
        size(helper, mob(helper, ModNtmEntities.CREEPER_PHOSGENE.get(), 20D, 0.25D), 0.6F, 1.8F);
        size(helper, mob(helper, ModNtmEntities.CREEPER_VOLATILE.get(), 20D, 0.25D), 0.6F, 1.8F);
        size(helper, mob(helper, ModNtmEntities.CREEPER_GOLD.get(), 20D, 0.25D), 0.6F, 1.8F);

        // EntityPigeon: EntityCreature defaults, setSize(0.5, 1)
        size(helper, mob(helper, ModNtmEntities.PIGEON.get(), 20D, 0.7D), 0.5F, 1.0F);
        // EntityPlasticBag: EntityWaterMob defaults (not the squid's 10 HP), setSize(0.45, 0.45)
        LivingEntity bag = create(helper, ModNtmEntities.PLASTIC_BAG.get());
        check(helper, bag, "max health", bag.getAttributeBaseValue(Attributes.MAX_HEALTH), 20D);
        size(helper, bag, 0.45F, 0.45F);
        // EntityDuck: the 1.7 chicken size 0.3 x 0.7 (the 1.7 creepers above: the default 0.6 x 1.8)
        size(helper, create(helper, ModNtmEntities.DUCK.get()), 0.3F, 0.7F);
        // EntityQuackos: setSize(0.3 * 25, 0.7 * 25)
        size(helper, create(helper, ModNtmEntities.QUACKOS.get()), 7.5F, 17.5F);

        // EntityParasiteMaggot: 8 HP, 1.0 old-AI speed, 2 damage, setSize(0.3, 0.7)
        LivingEntity maggot = mob(helper, EntityParasiteMaggot.TYPE.get(), 8D, Math.sqrt(1.0D * 0.1D));
        check(helper, maggot, "attack damage", maggot.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), 2D);
        size(helper, maggot, 0.3F, 0.7F);

        // EntityGhost 8 / 0.2; EntityBlockSpider 10 / 1.0, setSize(0.95, 1.25)
        mob(helper, ModNtmEntities.GHOST.get(), 8D, 0.2D);
        size(helper, mob(helper, ModNtmEntities.BLOCK_SPIDER.get(), 10D, 1.0D), 0.95F, 1.25F);

        // EntityUndeadSoldier: follow 40, speed 0.25, damage 5
        LivingEntity soldier = mob(helper, ModNtmEntities.UNDEAD_SOLDIER.get(), 20D, 0.25D);
        check(helper, soldier, "follow range", soldier.getAttributeBaseValue(Attributes.FOLLOW_RANGE), 40D);
        check(helper, soldier, "attack damage", soldier.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), 5D);

        // EntityFBI: knockback 0.5, speed 0.3, getTotalArmorValue 20
        LivingEntity fbi = mob(helper, ModNtmEntities.FBI.get(), 20D, 0.3D);
        check(helper, fbi, "knockback resistance", fbi.getAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE), 0.5D);
        check(helper, fbi, "armor", fbi.getAttributeBaseValue(Attributes.ARMOR), 20D);

        // EntityFBIDrone: 35 HP, followRange left at 16
        LivingEntity drone = create(helper, ModNtmEntities.FBI_DRONE.get());
        check(helper, drone, "max health", drone.getAttributeBaseValue(Attributes.MAX_HEALTH), 35D);
        check(helper, drone, "follow range", drone.getAttributeBaseValue(Attributes.FOLLOW_RANGE), 16D);

        // EntityRADBeast: 120 HP, 16 damage, armor 8, 0.7 old-AI speed
        LivingEntity beast = mob(helper, ModNtmEntities.RAD_BEAST.get(), 120D, Math.sqrt(0.7D * 0.1D));
        check(helper, beast, "attack damage", beast.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), 16D);
        check(helper, beast, "armor", beast.getAttributeBaseValue(Attributes.ARMOR), 8D);

        // EntityMaskMan: 1000 HP, 0.25, follow 100, damage 15, knockback 1, setSize(2, 5)
        LivingEntity maskman = mob(helper, ModNtmEntities.MASK_MAN.get(), 1000D, 0.25D);
        check(helper, maskman, "follow range", maskman.getAttributeBaseValue(Attributes.FOLLOW_RANGE), 100D);
        check(helper, maskman, "attack damage", maskman.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), 15D);
        check(helper, maskman, "knockback resistance", maskman.getAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE), 1D);
        size(helper, maskman, 2F, 5F);

        helper.succeed();
    }
}
