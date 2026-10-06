package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.config.RadiationConfig;
import com.hbm.config.WorldConfig;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.handler.ImpactSky;
import com.hbm.main.ModEventHandlerImpact;
import com.hbm.saveddata.TomSaveData;
import com.hbm.world.biome.ModBiomes;
import com.hbm.world.feature.DepthDepositPlacer;
import com.hbm.world.feature.ImpactAftermathFeature;
import com.hbm.world.feature.LegacyPerlinNoise;
import com.hbm.world.feature.OreLayer3DFeature;
import com.hbm.world.feature.SchistStratumFeature;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Biomes;
import dev.golem.core.world.Spawns;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.phys.Vec3;

/**
 * Fix wave phase 6 (History §19ej-6): 1.7.10 worldgen formulas, Tom's impact aftermath and the ambient radiation,
 * each against values computed by hand from the 1.7 source.
 */
public final class WorldgenImpactGameTests {

    private WorldgenImpactGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function SCHIST = TESTS.function(
            "fix6_schist_band_17", 20, () -> WorldgenImpactGameTests::schist);
    public static final GameTests.Function ORE_LAYER = TESTS.function(
            "fix6_ore_layer_noise_17", 20, () -> WorldgenImpactGameTests::oreLayer);
    public static final GameTests.Function HOSTS = TESTS.function(
            "fix6_depth_hosts_and_terrain_17", 20, () -> WorldgenImpactGameTests::hosts);
    public static final GameTests.Function AMBIENT = TESTS.function(
            "fix6_ambient_radiation_17", 20, () -> WorldgenImpactGameTests::ambient);
    public static final GameTests.Function IMPACT_DECAY = TESTS.function(
            "fix6_impact_decay_17", 20, () -> WorldgenImpactGameTests::impactDecay);
    public static final GameTests.Function IMPACT_SPAWNS = TESTS.function(
            "fix6_impact_spawns_17", 20, () -> WorldgenImpactGameTests::impactSpawns);
    public static final GameTests.Function IMPACT_WORLD_SPAWNERS = TESTS.function(
            "impact_refuses_world_spawners", 20, () -> WorldgenImpactGameTests::impactWorldSpawners);
    public static final GameTests.Function IMPACT_WORLDGEN = TESTS.function(
            "fix6_impact_worldgen_17", 20, () -> WorldgenImpactGameTests::impactWorldgen);
    public static final GameTests.Function IMPACT_SKY = TESTS.function(
            "fix6_impact_sky_17", 20, () -> WorldgenImpactGameTests::impactSky);

    public static void init() {}

    private static boolean near(double a, double b) {
        return Math.abs(a - b) <= 1e-5 * Math.max(1, Math.abs(b));
    }

    /** 1.7 SchistStratum: {@code range = (int) ((n - 5) * 3)}, folded at 4 AFTER the truncation; 0 is one layer. */
    private static void schist(GameTestHelper helper) {
        helper.assertTrue(SchistStratumFeature.range17(5.2) == 0, "n 5.2 -> (int) 0.6 = 0: one layer at y 30");
        helper.assertTrue(SchistStratumFeature.range17(6.5) == 4, "n 6.5 -> (int) 4.5 = 4");
        // (int) 5.1 = 5 > 4 -> 8 - 5 = 3 (folding the double first gave (int) 2.9 = 2)
        helper.assertTrue(SchistStratumFeature.range17(6.7) == 3, "n 6.7 -> 3, got " + SchistStratumFeature.range17(6.7));
        helper.assertTrue(SchistStratumFeature.range17(7.9) == 0, "n 7.9 -> (int) 8.7 = 8 -> 0");
        helper.assertTrue(SchistStratumFeature.range17(8.4) < 0, "n 8.4 -> 8 - 10 < 0: nothing");
        helper.succeed();
    }

    /** 1.7 OreLayer3D reads {@code nz = cacheX[ox][y]}: noiseX again, at z = chunkZ + 8 + xOff. */
    private static void oreLayer(GameTestHelper helper) {
        long seed = 4815162342L;
        int cx = 160, cz = -48, xOff = 3, zOff = 11, y = 30;
        double scaleH = 0.04, scaleV = 0.25; // hematite, id 0
        LegacyPerlinNoise nx = new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(seed + 101)), 4);
        LegacyPerlinNoise ny = new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(seed + 102)), 4);
        LegacyPerlinNoise nz = new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(seed + 103)), 4);
        int x = cx + 8 + xOff, z = cz + 8 + zOff;
        double bug = nx.getValue(y * scaleV, z * scaleH) * ny.getValue(x * scaleH, z * scaleH)
                * nx.getValue(y * scaleV, (cz + 8 + xOff) * scaleH);
        double intended = nx.getValue(y * scaleV, z * scaleH) * ny.getValue(x * scaleH, z * scaleH)
                * nz.getValue(x * scaleH, y * scaleV);
        double got = OreLayer3DFeature.cellProduct17(seed, 0, cx, cz, xOff, zOff, y);
        helper.assertTrue(near(got, bug), "the cell product must use 1.7's cacheX for nz: " + got + " vs " + bug);
        helper.assertTrue(!near(got, intended), "and must not be the intended noiseZ product " + intended);
        helper.succeed();
    }

    /** Deepslate hosts the overworld depth deposits; waste earth, mycelium and the depth tool at 1.7 values. */
    private static void hosts(GameTestHelper helper) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        helper.assertTrue(DepthDepositPlacer.isHost(Blocks.DEEPSLATE.defaultBlockState(), stone),
                "an overworld depth deposit takes deepslate as 1.7's stone");
        helper.assertTrue(!DepthDepositPlacer.isHost(Blocks.DEEPSLATE.defaultBlockState(), Blocks.NETHERRACK.defaultBlockState()),
                "a nether deposit does not");
        BlockState earth = ModFalloutBlocks.waste_earth.get().defaultBlockState();
        BlockState mycelium = ModFalloutBlocks.waste_mycelium.get().defaultBlockState();
        helper.assertTrue(earth.getDestroySpeed(helper.getLevel(), BlockPos.ZERO) == 0.6F, "waste earth hardness 0.6");
        helper.assertTrue(mycelium.getLightEmission() == 15, "waste mycelium light 15 (1.7 setLightLevel(1F))");
        helper.assertTrue(com.hbm.blocks.generic.BlockDepth.DEPTH_TOOL_PROGRESS == (float) (1D / 50D), "depth tool 1/50");
        helper.succeed();
    }

    /** Crater biomes 0.5 / 5 / 25 RAD/s, x5 when wet; the nether floor 0.1; fog 1 in 20 above 100 RAD. */
    private static void ambient(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        float outer = EntityEffectHandler.craterBiomeRad(Biomes.holder(registries, ModBiomes.crater_outer), false);
        float crater = EntityEffectHandler.craterBiomeRad(Biomes.holder(registries, ModBiomes.crater), false);
        float inner = EntityEffectHandler.craterBiomeRad(Biomes.holder(registries, ModBiomes.crater_inner), false);
        float craterWet = EntityEffectHandler.craterBiomeRad(Biomes.holder(registries, ModBiomes.crater), true);
        helper.assertTrue(outer == 0.5F && crater == 5F && inner == 25F,
                "crater rads must be 0.5/5/25, got " + outer + "/" + crater + "/" + inner);
        helper.assertTrue(craterWet == 25F, "wet crater must be 5 * 5 = 25, got " + craterWet);
        helper.assertTrue(EntityEffectHandler.craterBiomeRad(Biomes.holder(registries,
                net.minecraft.world.level.biome.Biomes.PLAINS), true) == 0F, "plains have no ambient radiation");
        helper.assertTrue(WorldConfig.craterBiomeWaterMult == 5F, "water multiplier 5");
        helper.assertTrue(RadiationConfig.hellRad == 0.1D, "hellRad must be 1.7's 0.1");
        helper.assertTrue(RadiationConfig.fogCh == 20 && RadiationConfig.fogRad == 100, "fog 1:20 above 100 RAD");
        helper.succeed();
    }

    /** Fire burns down in one day while the dust rises by as much; dust settles over 600 days, only without fire. */
    private static void impactDecay(GameTestHelper helper) {
        TomSaveData data = new TomSaveData();
        data.fire = 1F;
        data.dust = 0F;
        ModEventHandlerImpact.decay(data);
        helper.assertTrue(data.fire == 1F - 1F / 24000F && data.dust == 1F / 24000F,
                "one tick of fire: fire " + data.fire + ", dust " + data.dust);
        for (int i = 1; i < 24000; i++) ModEventHandlerImpact.decay(data);
        helper.assertTrue(data.fire < 1e-3F && data.dust > 0.999F, "after a day: fire " + data.fire + ", dust " + data.dust);
        data.fire = 0F;
        data.dust = 0.5F;
        ModEventHandlerImpact.decay(data);
        helper.assertTrue(data.dust == 0.5F - 1F / 14400000F, "settling dust: " + data.dust);
        helper.succeed();
    }

    /** 1.7 extinction: 0.85 tall (or wide, unless water or a child) refused in the overworld; generation animals refused. */
    private static void impactSpawns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TomSaveData data = TomSaveData.get(level);
        boolean before = data.impact;
        Mob cow = dev.golem.core.registry.Entities.createDetached(EntityType.COW, level);
        Mob chicken = dev.golem.core.registry.Entities.createDetached(EntityType.CHICKEN, level);
        try {
            data.impact = false;
            helper.assertTrue(!ModEventHandlerImpact.denySpawn(cow, level, Spawns.Reason.NATURAL), "no impact, no veto");
            data.impact = true;
            helper.assertTrue(ModEventHandlerImpact.denySpawn(cow, level, Spawns.Reason.NATURAL), "a cow (1.4 tall) is refused");
            helper.assertTrue(!ModEventHandlerImpact.denySpawn(chicken, level, Spawns.Reason.NATURAL),
                    "a chicken (0.7 tall, 0.4 wide) is not");
            helper.assertTrue(ModEventHandlerImpact.denySpawn(chicken, level, Spawns.Reason.CHUNK_GENERATION),
                    "world-generation animals are always refused (1.7 Populate ANIMALS)");
        } finally {
            data.impact = before;
            if (cow != null) cow.discard();
            if (chicken != null) chicken.discard();
        }
        helper.succeed();
    }

    /**
     * The impact refusal on the modern world spawners (Core {@code Spawns.Reason.WORLD_SPAWNER}), through vanilla's own
     * spawn path ({@code EntityType.spawn} runs {@code finalizeSpawn}, where Forge-like loaders fire FinalizeSpawn and
     * Fabric has Core's mixin): with the impact flag set, 1.7's overworld rule refuses a patrol pillager (1.95 tall),
     * a wandering trader (1.95 tall) and its llama (1.87 tall) and a phantom (0.5 tall but 0.9 wide, not water, not a
     * child). A raid pillager (EVENT) is not asked, and without the impact the patrol pillager spawns.
     */
    private static void impactWorldSpawners(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TomSaveData data = TomSaveData.get(level);
        boolean before = data.impact;
        BlockPos at = helper.absolutePos(new BlockPos(2, 3, 2));
        java.util.List<net.minecraft.world.entity.Entity> spawned = new java.util.ArrayList<>();
        try {
            data.impact = true;
            helper.assertTrue(!added(level, spawned, EntityType.PILLAGER.spawn(level, at, net.minecraft.world.entity.EntitySpawnReason.PATROL)),
                    "a patrol pillager must be refused after the impact");
            helper.assertTrue(!added(level, spawned, EntityType.WANDERING_TRADER.spawn(level, at, net.minecraft.world.entity.EntitySpawnReason.EVENT)),
                    "a wandering trader must be refused after the impact");
            helper.assertTrue(!added(level, spawned, EntityType.TRADER_LLAMA.spawn(level, at, net.minecraft.world.entity.EntitySpawnReason.EVENT)),
                    "a trader llama must be refused after the impact");
            helper.assertTrue(!added(level, spawned, EntityType.PHANTOM.spawn(level, at, net.minecraft.world.entity.EntitySpawnReason.NATURAL)),
                    "a phantom (0.9 wide) must be refused after the impact");
            helper.assertTrue(added(level, spawned, EntityType.PILLAGER.spawn(level, at, net.minecraft.world.entity.EntitySpawnReason.EVENT)),
                    "a raid pillager (EVENT) is not a world spawner and must spawn");
            data.impact = false;
            helper.assertTrue(added(level, spawned, EntityType.PILLAGER.spawn(level, at, net.minecraft.world.entity.EntitySpawnReason.PATROL)),
                    "without the impact a patrol pillager must spawn");
        } finally {
            data.impact = before;
            for (net.minecraft.world.entity.Entity e : spawned) e.discard();
        }
        helper.succeed();
    }

    private static boolean added(ServerLevel level, java.util.List<net.minecraft.world.entity.Entity> spawned,
                                 net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        spawned.add(entity);
        return level.getEntity(entity.getUUID()) == entity;
    }

    /** The worldgen verdicts: dusty chunks get impact dirt and lose decorations; recovered chunks keep 1 in 9. */
    private static void impactWorldgen(GameTestHelper helper) {
        BlockState dirt = ModDecoContent.impact_dirt.get().defaultBlockState();
        RandomSource rand = RandomSource.create(1);
        var dusty = ImpactAftermathFeature.Verdict.of(0.1F, 0F, rand);
        helper.assertTrue(dusty.apply(Blocks.GRASS_BLOCK.defaultBlockState(), dirt) == dirt, "grass -> impact dirt");
        helper.assertTrue(dusty.apply(Blocks.OAK_LOG.defaultBlockState(), dirt).isAir(), "TREE denied");
        helper.assertTrue(dusty.apply(Blocks.DEAD_BUSH.defaultBlockState(), dirt).isAir(), "DEAD_BUSH denied");
        helper.assertTrue(dusty.apply(Blocks.STONE.defaultBlockState(), dirt) == null, "stone untouched");
        helper.assertTrue(dusty.apply(Blocks.WHEAT.defaultBlockState(), dirt) == null,
                "below 0.25 dust only the decorations go, not every plant");
        var wipe = ImpactAftermathFeature.Verdict.of(0.5F, 0F, rand);
        helper.assertTrue(wipe.apply(Blocks.WHEAT.defaultBlockState(), dirt).isAir(), "above 0.25 dust every plant goes");
        int trees = 0;
        for (int i = 0; i < 9000; i++) if (ImpactAftermathFeature.Verdict.of(0F, 0F, rand).trees()) trees++;
        helper.assertTrue(trees > 850 && trees < 1150, "recovered: trees in 1 chunk of 9, got " + trees + "/9000");
        var recovered = new ImpactAftermathFeature.Verdict(false, false, true, false, false);
        helper.assertTrue(recovered.apply(Blocks.GRASS_BLOCK.defaultBlockState(), dirt) == null, "recovered grass stays");
        helper.assertTrue(recovered.apply(Blocks.OAK_LOG.defaultBlockState(), dirt) == null, "an allowed tree stays");
        helper.assertTrue(recovered.apply(Blocks.CACTUS.defaultBlockState(), dirt).isAir(), "a denied cactus goes");
        helper.succeed();
    }

    /** WorldProviderNTM: sky and fog colours against the 1.7 formulas. */
    private static void impactSky(GameTestHelper helper) {
        Vec3 sky = new Vec3(0.5, 0.6, 1.0);
        Vec3 dusty = ImpactSky.skyColor17(sky, 0.5F, 0F);
        // f4 = 0.5, f5 = 0.6 * 0.75, f6 = 1 * 0.5, all times (0 + 0.5)
        helper.assertTrue(near(dusty.x, 0.25) && near(dusty.y, 0.225) && near(dusty.z, 0.25), "dusty sky " + dusty);
        Vec3 burning = ImpactSky.skyColor17(sky, 0F, 1F);
        // fire: f4 = 0.5 * 1.3 = 0.65, f5 = 0.6, f6 = 1.0, all times (1 + 1)
        helper.assertTrue(near(burning.x, 1.3) && near(burning.y, 1.2) && near(burning.z, 2.0), "burning sky " + burning);
        Vec3 fog = ImpactSky.fogColor17(new Vec3(0.75, 0.85, 1.0), 0.4F, 0.5F);
        // f3 = 0.75, f4 = 0.85 * 0.8 = 0.68, f5 = 1 * 0.6, fire: times max(1 - 0.8, 0) = 0.2
        helper.assertTrue(near(fog.x, 0.15) && near(fog.y, 0.136) && near(fog.z, 0.12), "fog " + fog);
        helper.succeed();
    }
}
