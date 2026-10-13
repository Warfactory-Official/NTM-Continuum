package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.items.ModCustomMissileItems;
import com.hbm.lib.ModDamageSources;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Explosion values computed from 1.7, the specification.
 */
public final class Fix1DGameTests {

    private Fix1DGameTests() {}

    /**
     * 1.7 ExplosionNukeSmall:52-55: LOW carries radiation level 2, MEDIUM 3, TOTS 1, SAFE 2, and the
     * ring dose is {@code 50 / (|i| + |j| + 1) * level / 3} with the 50 divided as an int.
     */
    public static void smallNukeRadiation(GameTestHelper helper) {
        helper.assertTrue(ExplosionNukeSmall.PARAMS_LOW.radiationLevel == 2F
                        && ExplosionNukeSmall.PARAMS_MEDIUM.radiationLevel == 3F
                        && ExplosionNukeSmall.PARAMS_TOTS.radiationLevel == 1F
                        && ExplosionNukeSmall.PARAMS_SAFE.radiationLevel == 2F,
                "small-nuke radiation levels are not 1.7's LOW 2 / MEDIUM 3 / TOTS 1 / SAFE 2");
        // centre 50 * 3/3 = 50; one chunk out 25; diagonal 50/3 = 16 (int) * 1; LOW diagonal 16 * 2/3
        helper.assertTrue(ExplosionNukeSmall.radiationAt(0, 0, 3F) == 50F
                        && ExplosionNukeSmall.radiationAt(1, 0, 3F) == 25F
                        && ExplosionNukeSmall.radiationAt(1, 1, 3F) == 16F
                        && ExplosionNukeSmall.radiationAt(1, 1, 2F) == 16 * (2F / 3F)
                        && ExplosionNukeSmall.radiationAt(2, 1, 1F) == 12 * (1F / 3F),
                "small-nuke ring dose differs from 1.7's int-divided 50 / (|i| + |j| + 1) * level / 3");
        helper.succeed();
    }

    /** 1.7 EntityProcessorCrossSmooth.calculateDamage:52: below 12.5% exposure the blast deals nothing. */
    public static void crossSmoothDensityGate(GameTestHelper helper) {
        EntityProcessorCrossSmooth processor = new EntityProcessorCrossSmooth(1, 100F);
        helper.assertTrue(processor.calculateDamage(0.5, 0.124, 0, 5) == 0F,
                "a 12.4%-exposed entity still took cross-smooth damage");
        helper.assertTrue(processor.calculateDamage(0.5, 0.125, 0, 5) == 50F,
                "a 12.5%-exposed entity did not take 100 * (1 - 0.5) = 50");
        helper.succeed();
    }

    /**
     * 1.7 ExplosionNukeGeneric.dealDamage:67-110 over the MK5's entity list: nuclearBlast, linear
     * falloff (250 * (10 - 8) / 10 = 50 on a 100-health golem), creative players exempt.
     */
    public static void nukeDamageSource(GameTestHelper helper) {
        LivingEntity golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 2));
        Vec3 at = golem.position();
        Player creative = GameTestHelpers.mockPlayer(helper, GameType.CREATIVE);
        creative.setPos(at.x, at.y, at.z + 1);
        double cx = at.x + 8, cy = at.y, cz = at.z;
        try {
            float before = creative.getHealth();
            ExplosionNukeGeneric.dealDamage(helper.getLevel(), List.<Entity>of(golem, creative), cx, cy, cz, 10, 250F);
            helper.assertTrue(Math.abs(golem.getHealth() - 50F) < 0.01F,
                    "golem at 8 of 10 blocks has " + golem.getHealth() + " health, expected 100 - 50");
            helper.assertTrue(golem.getLastDamageSource() != null
                            && golem.getLastDamageSource().is(ModDamageSources.NUCLEAR_BLAST),
                    "MK5 blast damage was not 1.7's nuclearBlast: " + golem.getLastDamageSource());
            helper.assertTrue(creative.getHealth() == before, "a creative player was not exempt from the nuke");
            helper.succeed();
        } finally {
            golem.discard();
        }
    }

    /** 1.7 wasteDest / wasteDestNoSchrab: the deterministic conversions. */
    public static void wasteConversions(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos grass = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos log = helper.absolutePos(new BlockPos(2, 1, 1));
        BlockPos planks = helper.absolutePos(new BlockPos(3, 1, 1));
        BlockPos mossy = helper.absolutePos(new BlockPos(4, 1, 1));
        BlockPos clay = helper.absolutePos(new BlockPos(5, 1, 1));
        level.setBlockAndUpdate(grass, Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(log, Blocks.OAK_LOG.defaultBlockState());
        level.setBlockAndUpdate(planks, Blocks.OAK_PLANKS.defaultBlockState());
        level.setBlockAndUpdate(mossy, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        level.setBlockAndUpdate(clay, Blocks.CLAY.defaultBlockState());
        for (BlockPos pos : new BlockPos[] {grass, log, planks, mossy, clay}) ExplosionNukeGeneric.wasteDest(level, pos);
        helper.assertTrue(level.getBlockState(grass).is(ModFalloutBlocks.waste_earth.get())
                        && level.getBlockState(log).is(ModFalloutBlocks.waste_log.get())
                        && level.getBlockState(planks).is(ModBlocks.waste_planks.get())
                        && level.getBlockState(mossy).is(Blocks.COAL_ORE)
                        && level.getBlockState(clay).is(Blocks.TERRACOTTA),
                "wasteDest conversions differ from 1.7 (grass, log, planks, mossy cobble, clay)");

        BlockPos glass = helper.absolutePos(new BlockPos(1, 1, 3));
        BlockPos planks2 = helper.absolutePos(new BlockPos(2, 1, 3));
        level.setBlockAndUpdate(glass, Blocks.GLASS.defaultBlockState());
        level.setBlockAndUpdate(planks2, Blocks.BIRCH_PLANKS.defaultBlockState());
        ExplosionNukeGeneric.wasteDestNoSchrab(level, glass);
        ExplosionNukeGeneric.wasteDestNoSchrab(level, planks2);
        helper.assertTrue(level.getBlockState(glass).isAir() && level.getBlockState(planks2).is(ModBlocks.waste_planks.get()),
                "wasteDestNoSchrab did not clear glass and turn planks into waste planks");
        helper.succeed();
    }

    /** 1.7 ExplosionLarge.cloudFunction:256: base 850 and an INT-divided exponent. */
    public static void cloudFunction(GameTestHelper helper) {
        helper.assertTrue(ExplosionLarge.cloudFunction(10) == 15
                        && ExplosionLarge.cloudFunction(15) == 552
                        && ExplosionLarge.cloudFunction(30) == 749
                        && ExplosionLarge.rubbleFunction(30) == 3 && ExplosionLarge.shrapnelFunction(30) == 10,
                "ExplosionLarge particle/debris counts differ from 1.7 (15 / 552 / 749, 3, 10)");
        helper.succeed();
    }

    /** 1.7 EntityMissileCustom:194: a nuclear custom warhead spawns the MK5 and its Torex cloud. */
    public static void customNuclearCloud(GameTestHelper helper) {
        EntityMissileCustom missile = new EntityMissileCustom(ModNtmEntities.MISSILE_CUSTOM.get(), helper.getLevel());
        missile.setParts(new ItemStack(ModCustomMissileItems.mp_warhead_10_nuclear.get()), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY);
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 3, 2)));
        missile.setPos(at.x, at.y, at.z);
        AABB box = new AABB(at, at).inflate(3);
        try {
            missile.onMissileImpact(null);
            List<EntityNukeExplosionMK5> blasts = helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK5.class, box);
            List<EntityNukeTorex> clouds = helper.getLevel().getEntitiesOfClass(EntityNukeTorex.class, box);
            helper.assertTrue(blasts.size() == 1, "nuclear custom warhead spawned " + blasts.size() + " MK5 blasts");
            helper.assertTrue(clouds.size() == 1, "nuclear custom warhead spawned " + clouds.size() + " Torex clouds");
            helper.assertTrue(clouds.get(0).isChunkLoading(), "the custom-missile Torex did not claim its chunk");
            helper.succeed();
        } finally {
            helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK5.class, box).forEach(Entity::discard);
            helper.getLevel().getEntitiesOfClass(EntityNukeTorex.class, box).forEach(Entity::discard);
            missile.discard();
        }
    }

    /** 1.7 EntityNukeTorex.statFac:573: clamp((float) squirt(r * 0.01) * 1.5F, 0.5F, 5F); lifetime 900 per unit. */
    public static void torexScale(GameTestHelper helper) {
        helper.assertTrue(Math.abs(EntityNukeTorex.scaleFor(100) - 1.0811388F) < 1e-5F
                        && EntityNukeTorex.scaleFor(35) == 0.5F
                        && Math.abs(EntityNukeTorex.scaleFor(500) - 3.1466544F) < 1e-5F
                        && EntityNukeTorex.scaleFor(5000) == 5F,
                "Torex scale is not 1.7's clamp(squirt(r / 100) * 1.5, 0.5, 5): " + EntityNukeTorex.scaleFor(100));
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        EntityNukeTorex torex = EntityNukeTorex.statFac(helper.getLevel(), at.x, at.y, at.z, 100F);
        helper.assertTrue(torex.getMaxAge() == (int) (45 * 20 * EntityNukeTorex.scaleFor(100)),
                "Torex lifetime is not 45 s per unit of scale");
        helper.succeed();
    }
}
