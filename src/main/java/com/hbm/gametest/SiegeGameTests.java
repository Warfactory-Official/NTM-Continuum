package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.siege.EntitySiegeCraft;
import com.hbm.entity.mob.siege.EntitySiegeTunneler;
import com.hbm.entity.mob.siege.SiegeTier;
import com.hbm.entity.projectile.EntitySiegeLaser;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageSources;
import dev.golem.core.data.Tags;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The siege craft and tunneler against 1.7's {@code SiegeTier}, damage rules, drops, laser fan, beam and digging. */
public final class SiegeGameTests {

    private SiegeGameTests() {}

    private static final int AI_PADDING = 48;

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function TIER_STATS =
            TESTS.function("siege_tier_stats", 20, () -> SiegeGameTests::tierStats);
    public static final GameTests.Function TIER_SAVES =
            TESTS.function("siege_tier_saves_by_name", 20, () -> SiegeGameTests::tierSaves);
    public static final GameTests.Function DAMAGE_RULES =
            TESTS.function("siege_damage_rules", 20, () -> SiegeGameTests::damageRules);
    public static final GameTests.Function DROPS =
            TESTS.function("siege_drops", 20, () -> SiegeGameTests::drops);
    public static final GameTests.Function CRAFT_LASER =
            TESTS.function("siege_craft_fires_laser", 200, AI_PADDING, () -> SiegeGameTests::craftLaser);
    public static final GameTests.Function CRAFT_BEAM =
            TESTS.function("siege_craft_beams_lockon", 500, AI_PADDING, () -> SiegeGameTests::craftBeam);
    public static final GameTests.Function TUNNELER_DIGS =
            TESTS.function("siege_tunneler_digs_nothing", 80, 8, () -> SiegeGameTests::tunnelerDigsNothing);
    public static final GameTests.Function TUNNELER_RAMS =
            TESTS.function("siege_tunneler_rams", 40, () -> SiegeGameTests::tunnelerRams);

    public static void init() {}

    private static <T extends LivingEntity> T create(GameTestHelper helper, EntityType<T> type) {
        T entity = Entities.createTriggered(type, helper.getLevel());
        helper.assertTrue(entity != null, "could not create " + EntityType.getKey(type));
        return entity;
    }

    private static void check(GameTestHelper helper, String what, double actual, double expected) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-4D, what + " = " + actual + ", 1.7 " + expected);
    }

    /** 1.7 setTier: craft health x25 and speed, tunneler health x0.5 and damage 1 x (1 + damageMod). */
    public static void tierStats(GameTestHelper helper) {
        helper.assertTrue(SiegeTier.getLength() == 9, "1.7 has nine siege tiers, found " + SiegeTier.getLength());
        double[][] expected = {
                // health, damageMod, speedMod
                {20, 2, 0}, {30, 3, 0}, {40, 5, 0}, {50, 7.5, 0}, {70, 10, 0.5},
                {100, 15, 0.5}, {150, 25, 0.5}, {250, 50, 1}, {500, 100, 1}};
        String[] names = {"buff", "clay", "stone", "iron", "silver", "gold", "desh", "schrab", "dnt"};

        for (int i = 0; i < 9; i++) {
            SiegeTier tier = SiegeTier.tiers[i];
            helper.assertTrue(tier.name.equals(names[i]), "tier " + i + " is " + tier.name + ", 1.7 " + names[i]);

            EntitySiegeCraft craft = create(helper, ModNtmEntities.SIEGE_CRAFT.get());
            craft.setTier(tier);
            check(helper, tier.name + " craft max health", craft.getMaxHealth(), expected[i][0] * 25);
            check(helper, tier.name + " craft health", craft.getHealth(), expected[i][0] * 25);
            check(helper, tier.name + " craft speed", craft.getAttributeValue(Attributes.MOVEMENT_SPEED), expected[i][2]);
            helper.assertTrue(craft.getTier() == tier, tier.name + " craft reads back " + craft.getTier().name);

            EntitySiegeTunneler tunneler = create(helper, ModNtmEntities.SIEGE_TUNNELER.get());
            tunneler.setTier(tier);
            check(helper, tier.name + " tunneler max health", tunneler.getMaxHealth(), expected[i][0] * 0.5);
            check(helper, tier.name + " tunneler attack", tunneler.getAttributeValue(Attributes.ATTACK_DAMAGE), 1 + expected[i][1]);
            check(helper, tier.name + " tunneler speed", tunneler.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.15);
            check(helper, tier.name + " tunneler follow range", tunneler.getAttributeBaseValue(Attributes.FOLLOW_RANGE), 40);
        }

        EntitySiegeCraft craft = create(helper, ModNtmEntities.SIEGE_CRAFT.get());
        check(helper, "craft width", craft.getBbWidth(), 7);
        check(helper, "craft height", craft.getBbHeight(), 1);
        helper.assertTrue(craft.fireImmune(), "1.7 craft isImmuneToFire");
        helper.assertTrue(craft.getTier() == SiegeTier.DEFAULT_BUFF, "an untiered craft reads tier 0, buff");
        EntitySiegeTunneler tunneler = create(helper, ModNtmEntities.SIEGE_TUNNELER.get());
        check(helper, "tunneler width", tunneler.getBbWidth(), 1);
        check(helper, "tunneler height", tunneler.getBbHeight(), 1);
        helper.assertTrue(tunneler.noPhysics, "1.7 EntityBurrowingBase noClip");
        helper.succeed();
    }

    public static void tierSaves(GameTestHelper helper) {
        EntitySiegeCraft craft = create(helper, ModNtmEntities.SIEGE_CRAFT.get());
        craft.setTier(SiegeTier.DESH);
        CompoundTag saved = Entities.saveToTag(craft);
        helper.assertTrue(saved.get("siegeTier") instanceof StringTag, "siegeTier must be saved as a name: " + saved.get("siegeTier"));
        helper.assertTrue(Tags.getStringOr(saved, "siegeTier", "").equals("desh"), "saved " + saved.get("siegeTier"));

        EntitySiegeCraft loaded = create(helper, ModNtmEntities.SIEGE_CRAFT.get());
        Entities.loadFromTag(loaded, saved);
        helper.assertTrue(loaded.getTier() == SiegeTier.DESH, "loaded craft tier " + loaded.getTier().name);
        check(helper, "loaded craft max health", loaded.getMaxHealth(), 150 * 25);

        EntitySiegeTunneler tunneler = create(helper, ModNtmEntities.SIEGE_TUNNELER.get());
        tunneler.setTier(SiegeTier.SCHRAB);
        CompoundTag drill = Entities.saveToTag(tunneler);
        helper.assertTrue(Tags.getStringOr(drill, "siegeTier", "").equals("schrab"), "saved " + drill.get("siegeTier"));
        EntitySiegeTunneler reloaded = create(helper, ModNtmEntities.SIEGE_TUNNELER.get());
        Entities.loadFromTag(reloaded, drill);
        Entities.loadFromTag(reloaded, Entities.saveToTag(reloaded));
        helper.assertTrue(reloaded.getTier() == SiegeTier.SCHRAB, "loaded tunneler tier " + reloaded.getTier().name);
        check(helper, "twice-loaded tunneler attack", reloaded.getAttributeValue(Attributes.ATTACK_DAMAGE), 51);

        CompoundTag untiered = Entities.saveToTag(create(helper, ModNtmEntities.SIEGE_CRAFT.get()));
        untiered.remove("siegeTier");
        EntitySiegeCraft blank = create(helper, ModNtmEntities.SIEGE_CRAFT.get());
        Entities.loadFromTag(blank, untiered);
        helper.assertTrue(blank.getTier() == SiegeTier.DEFAULT_BUFF, "1.7 reads a missing siegeTier as 0, buff");
        helper.succeed();
    }

    private static void hit(GameTestHelper helper, LivingEntity entity, DamageSource source, float amount,
                            float expectedLoss, String what) {
        float before = entity.getHealth();
        entity.invulnerableTime = 0;
        Compat.hurt(entity, helper.getLevel(), source, amount);
        check(helper, what, before - entity.getHealth(), expectedLoss);
    }

    /** damage = (amount - dt) x (1 - dr); below dt nothing; FF tiers ignore mobs, FP tiers ignore fire. */
    public static void damageRules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        DamageSource byPlayer = level.damageSources().playerAttack(player);
        DamageSource byMob = level.damageSources().mobAttack(zombie);

        EntitySiegeCraft silver = helper.spawnWithNoFreeWill(ModNtmEntities.SIEGE_CRAFT.get(), new BlockPos(1, 4, 1));
        silver.setTier(SiegeTier.SILVER);
        hit(helper, silver, byPlayer, 13F, 5F, "silver craft, 13 from a player");
        hit(helper, silver, byPlayer, 2F, 0F, "silver craft, 2 from a player (below dt 3)");
        hit(helper, silver, byMob, 100F, 0F, "silver craft, 100 from a mob (noFriendlyFire)");
        hit(helper, silver, level.damageSources().inFire(), 100F, 0F, "silver craft, fire (fireProof)");

        EntitySiegeCraft clay = helper.spawnWithNoFreeWill(ModNtmEntities.SIEGE_CRAFT.get(), new BlockPos(1, 7, 1));
        clay.setTier(SiegeTier.CLAY);
        hit(helper, clay, byMob, 10F, 8F, "clay craft, 10 from a mob");

        EntitySiegeTunneler stone = helper.spawnWithNoFreeWill(ModNtmEntities.SIEGE_TUNNELER.get(), new BlockPos(5, 2, 1));
        stone.setTier(SiegeTier.STONE);
        hit(helper, stone, byPlayer, 11F, 7F, "stone tunneler, 11 from a player");
        hit(helper, stone, level.damageSources().drown(), 5F, 0F, "stone tunneler, drowning");
        hit(helper, stone, level.damageSources().inWall(), 5F, 0F, "stone tunneler, in a wall");

        silver.discard();
        clay.discard();
        stone.discard();
        zombie.discard();
        helper.succeed();
    }

    private static List<ItemEntity> itemsNear(GameTestHelper helper, LivingEntity entity) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(3D));
    }

    /** 1.7 dropFewItems: every tier drops its steel ingot, only to a player kill; the tunneler drops nothing. */
    public static void drops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);

        EntitySiegeCraft byPlayer = helper.spawnWithNoFreeWill(ModNtmEntities.SIEGE_CRAFT.get(), new BlockPos(4, 3, 4));
        byPlayer.setTier(SiegeTier.GOLD);
        Compat.hurt(byPlayer, level, level.damageSources().playerAttack(player), 1.0E6F);
        helper.assertTrue(byPlayer.isDeadOrDying(), "the craft survived a million damage");
        List<ItemEntity> items = itemsNear(helper, byPlayer);
        helper.assertTrue(items.size() == 1 && items.get(0).getItem().is(ModItems.ingot_steel.get())
                && items.get(0).getItem().getCount() == 1, "player kill dropped " + items);
        items.forEach(ItemEntity::discard);
        byPlayer.discard();

        EntitySiegeCraft unattended = helper.spawnWithNoFreeWill(ModNtmEntities.SIEGE_CRAFT.get(), new BlockPos(4, 3, 4));
        unattended.setTier(SiegeTier.GOLD);
        Compat.hurt(unattended, level, level.damageSources().generic(), 1.0E6F);
        helper.assertTrue(unattended.isDeadOrDying(), "the craft survived a million generic damage");
        helper.assertTrue(itemsNear(helper, unattended).isEmpty(), "an unattended kill dropped " + itemsNear(helper, unattended));
        unattended.discard();

        EntitySiegeTunneler tunneler = helper.spawnWithNoFreeWill(ModNtmEntities.SIEGE_TUNNELER.get(), new BlockPos(4, 3, 4));
        tunneler.setTier(SiegeTier.CLAY);
        Compat.hurt(tunneler, level, level.damageSources().playerAttack(player), 1.0E6F);
        helper.assertTrue(itemsNear(helper, tunneler).isEmpty(), "the tunneler dropped " + itemsNear(helper, tunneler));
        tunneler.discard();
        helper.succeed();
    }

    private static ServerPlayer targetOnFloor(GameTestHelper helper, BlockPos centre) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) helper.setBlock(centre.offset(x, -1, z), Blocks.STONE);
        }
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(centre));
        player.teleportTo(at.x, at.y, at.z);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(5000D);
        player.setHealth(5000F);
        return player;
    }

    /** The craft roams about twenty blocks round its target, so its corridor is held entity-ticking. */
    private static Runnable holdCorridor(GameTestHelper helper, BlockPos centre) {
        return GameTests.holdChunks(helper, centre.getX() - 40, centre.getX() + 40, centre.getZ() - 40, centre.getZ() + 40);
    }

    private static boolean corridorTicking(GameTestHelper helper, BlockPos centre) {
        return GameTests.chunksTicking(helper, centre.getX() - 40, centre.getX() + 40, centre.getZ() - 40, centre.getZ() + 40);
    }

    /**
     * The craft, spawned once the corridor ticks: below 26.1 a padded plot's centre lies outside the chunks the test
     * runner forces, and an entity there never ticks. The player comes first, as a tick listener may not add another.
     */
    private static void onceTicking(GameTestHelper helper, BlockPos centre, SiegeTier tier, EntitySiegeCraft[] craft,
                                    Runnable extra) {
        helper.onEachTick(() -> {
            if (craft[0] != null || !corridorTicking(helper, centre)) return;
            extra.run();
            // never straight above the target: 1.7's course math divides by the horizontal distance
            craft[0] = helper.spawn(ModNtmEntities.SIEGE_CRAFT.get(), centre.offset(3, 10, 2));
            craft[0].setTier(tier);
        });
    }

    /** 1.7: with a target, the first AI tick fires seven lasers, owned by the craft, green at full health. */
    public static void craftLaser(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(AI_PADDING, 2);
        Runnable release = holdCorridor(helper, centre);
        ServerPlayer player = targetOnFloor(helper, centre);
        EntitySiegeCraft[] craft = {null};
        onceTicking(helper, centre, SiegeTier.IRON, craft, () -> {});

        helper.succeedWhen(() -> {
            helper.assertTrue(craft[0] != null, "the craft's corridor is not entity-ticking yet");
            List<EntitySiegeLaser> lasers = helper.getLevel().getEntitiesOfClass(EntitySiegeLaser.class,
                    craft[0].getBoundingBox().inflate(AI_PADDING), laser -> laser.getThrower() == craft[0]);
            helper.assertTrue(lasers.size() >= 7, "craft has fired " + lasers.size() + " lasers, target " + craft[0].target());
            for (EntitySiegeLaser laser : lasers) {
                helper.assertTrue(laser.getColor() == 0x00FF00, "full-health laser colour " + Integer.toHexString(laser.getColor()));
            }
            helper.assertTrue(craft[0].target() == player, "craft target " + craft[0].target());
            craft[0].discard();
            lasers.forEach(EntitySiegeLaser::discard);
            release.run();
        });
    }

    /** 1.7's 300-tick cycle: the lock-on settles on the target and the last 40 ticks deal 1,000 combine damage there. */
    public static void craftBeam(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(AI_PADDING, 2);
        Runnable release = holdCorridor(helper, centre);
        targetOnFloor(helper, centre);
        EntitySiegeCraft[] craft = {null};
        LivingEntity[] golem = {null};
        onceTicking(helper, centre, SiegeTier.CLAY, craft,
                () -> golem[0] = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, centre.east()));
        boolean[] beamSeen = {false};
        helper.onEachTick(() -> beamSeen[0] |= craft[0] != null && craft[0].getBeam());

        helper.succeedWhen(() -> {
            helper.assertTrue(craft[0] != null, "the craft's corridor is not entity-ticking yet");
            helper.assertTrue(!golem[0].isAlive(), "the golem by the target is alive, beam seen " + beamSeen[0]);
            helper.assertTrue(beamSeen[0], "the golem died without the beam");
            DamageSource last = golem[0].getLastDamageSource();
            helper.assertTrue(last != null && last.is(ModDamageSources.COMBINE), "the golem died of " + last);
            craft[0].discard();
            release.run();
        });
    }

    /** 1.7 noClip: the tunneler passes through stone and leaves every block standing. */
    public static void tunnelerDigsNothing(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            }
        }
        EntitySiegeTunneler tunneler = helper.spawn(ModNtmEntities.SIEGE_TUNNELER.get(), new BlockPos(2, 2, 2));
        tunneler.setTier(SiegeTier.CLAY);
        Vec3 start = tunneler.position();
        helper.assertTrue(tunneler.isEntityInsideOpaqueBlock(), "the tunneler does not see itself buried");

        helper.runAfterDelay(40, () -> {
            helper.assertTrue(tunneler.position().distanceTo(start) > 0.5D, "the tunneler did not move through the stone");
            for (int x = 0; x < 5; x++) {
                for (int y = 1; y < 6; y++) {
                    for (int z = 0; z < 5; z++) helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, y, z));
                }
            }
            tunneler.discard();
            helper.succeed();
        });
    }

    /** 1.7 attackEntityAsMob every second tick: a stone tunneler deals 1 x (1 + 5) to what it overlaps. */
    public static void tunnelerRams(GameTestHelper helper) {
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        EntitySiegeTunneler tunneler = helper.spawn(ModNtmEntities.SIEGE_TUNNELER.get(), new BlockPos(1, 2, 1));
        tunneler.setTier(SiegeTier.STONE);

        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the tunneler has not hit the pig");
            check(helper, "pig health after one ram", pig.getHealth(), pig.getMaxHealth() - 6);
            tunneler.discard();
            pig.discard();
        });
    }
}
