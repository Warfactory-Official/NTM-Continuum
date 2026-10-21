package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.config.WorldConfig;
import com.hbm.world.feature.CrustColumnFeature;
import com.hbm.world.feature.DepthDepositPlacer;
import com.hbm.world.feature.OreVeinPlacer;
import dev.golem.core.Ids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * GameTests pinning the ore-worldgen port to the 1.12.2 behaviour.
 *
 * <p>These run against the real {@link ServerLevel} rather than the GameTest structure area: worldgen
 * needs a chunk-sized volume of stone, which does not fit in a test structure. Each test carves its own
 * scratch box in the air well above the test platform, writes into it with absolute coordinates, then
 * clears it. {@link #scratch} centralises that so the boxes never overlap the harness.
 */
public final class OreWorldgenGameTests {

    private OreWorldgenGameTests() {}

    /** Height above the test platform where scratch volumes are carved. Air in the gametest world. */
    private static final int SCRATCH_Y = 24;
    private static final int BOX = 40;      // x/z extent of a scratch box
    private static final int BOX_H = 28;    // y extent

    // Ore vein shape

    /**
     * The vein generator must be vanilla's swelling ellipsoid, not a random walk.
     *
     * <p>The discriminator is volume: a walk of {@code size} steps can place at most {@code size}
     * blocks (and usually fewer, since steps revisit cells), whereas the ellipsoid sweeps a swelling
     * tube and places many times that. A size-16 vein placing ≤ 16 blocks means the generator is a walk.
     */
    public static void oreVeinShape(GameTestHelper helper) {
        BlockPos origin = scratch(helper);
        ServerLevel level = helper.getLevel();
        fill(level, origin, Blocks.STONE.defaultBlockState());

        BlockState ore = ModBlocks.ore_titanium.get().defaultBlockState();
        int size = 16;
        BlockPos target = origin.offset(8, BOX_H / 2, 8);
        RandomSource rand = RandomSource.create(0xC0FFEEL);
        OreVeinPlacer.placeVein(level, rand, target.getX(), target.getY(), target.getZ(), size, ore,
                OreVeinPlacer.STONE);

        int placed = count(level, origin, ore.getBlock());
        helper.assertTrue(placed > size,
                "A size-" + size + " vein placed only " + placed + " blocks; the ellipsoid generator "
                        + "always places well over `size` (the old random-walk placeholder never did)");
        helper.assertTrue(placed < 4000, "Vein of " + placed + " blocks is implausibly large");

        clear(level, origin);
        helper.succeed();
    }

    /**
     * The {@code +8} x/z offset inherited from vanilla {@code WorldGenMinable}. It is a real quirk of
     * HBM's generator: every vein sits 8 blocks off the position the caller chose, and removing it
     * would shift the entire ore layer, so it is pinned here.
     */
    public static void oreVeinOffset(GameTestHelper helper) {
        BlockPos origin = scratch(helper);
        ServerLevel level = helper.getLevel();
        fill(level, origin, Blocks.STONE.defaultBlockState());

        BlockState ore = ModBlocks.ore_titanium.get().defaultBlockState();
        BlockPos target = origin.offset(4, BOX_H / 2, 4);
        // Average many veins so the per-vein random bearing cancels out and only the bias remains.
        long sumX = 0, sumZ = 0, n = 0;
        for (int i = 0; i < 24; i++) {
            RandomSource rand = RandomSource.create(1000L + i);
            OreVeinPlacer.placeVein(level, rand, target.getX(), target.getY(), target.getZ(), 8, ore,
                    OreVeinPlacer.STONE);
        }
        for (BlockPos pos : box(origin)) {
            if (level.getBlockState(pos).is(ore.getBlock())) {
                sumX += pos.getX();
                sumZ += pos.getZ();
                n++;
            }
        }
        helper.assertTrue(n > 0, "No ore placed at all");
        double cx = sumX / (double) n - target.getX();
        double cz = sumZ / (double) n - target.getZ();
        helper.assertTrue(Math.abs(cx - 8.0D) < 2.0D && Math.abs(cz - 8.0D) < 2.0D,
                "Vein centroid should sit ~(+8, +8) from the requested position (the upstream vanilla "
                        + "offset), but was (" + String.format("%.2f", cx) + ", " + String.format("%.2f", cz) + ")");

        clear(level, origin);
        helper.succeed();
    }

    /**
     * The replaceable predicate. Stone and its natural variants (plus deepslate/tuff, the documented
     * 26.1 extension) are replaced; dirt, an existing ore and air never are.
     */
    public static void oreVeinPredicate(GameTestHelper helper) {
        BlockPos origin = scratch(helper);
        ServerLevel level = helper.getLevel();

        // Alternating stone / dirt columns: half the volume must be untouchable.
        for (BlockPos pos : box(origin)) {
            boolean stone = ((pos.getX() + pos.getZ()) & 1) == 0;
            level.setBlock(pos, stone ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
        }

        BlockState ore = ModBlocks.ore_titanium.get().defaultBlockState();
        for (int i = 0; i < 12; i++) {
            RandomSource rand = RandomSource.create(500L + i);
            OreVeinPlacer.placeVein(level, rand, origin.getX() + 4, origin.getY() + BOX_H / 2, origin.getZ() + 4,
                    12, ore, OreVeinPlacer.STONE);
        }

        int placed = 0, onOddColumn = 0, dirtLost = 0;
        for (BlockPos pos : box(origin)) {
            boolean stoneColumn = ((pos.getX() + pos.getZ()) & 1) == 0;
            if (level.getBlockState(pos).is(ore.getBlock())) {
                placed++;
                if (!stoneColumn) onOddColumn++;
            }
            if (!stoneColumn && level.getBlockState(pos).isAir()) dirtLost++;
        }
        helper.assertTrue(placed > 0, "The predicate rejected everything — no ore placed");
        helper.assertTrue(onOddColumn == 0, onOddColumn + " ore blocks replaced DIRT; the predicate must "
                + "only accept stone and its natural variants");
        helper.assertTrue(dirtLost == 0, "Dirt was deleted rather than skipped");

        clear(level, origin);
        helper.succeed();
    }

    /**
     * Host-rock targeting. The gneiss family replaces {@code stone_gneiss} and nothing else. This is
     * what makes the schist stratum a hard ordering dependency, since a gneiss vein run before the band
     * exists silently places nothing.
     */
    public static void oreTargets(GameTestHelper helper) {
        BlockPos origin = scratch(helper);
        ServerLevel level = helper.getLevel();
        Block gneissBlock = ModBlocks.stone_gneiss.get();

        // Left half gneiss, right half plain stone.
        for (BlockPos pos : box(origin)) {
            boolean left = pos.getX() - origin.getX() < BOX / 2;
            level.setBlock(pos, left ? gneissBlock.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
        }

        BlockState ore = ModBlocks.ore_gneiss_iron.get().defaultBlockState();
        var gneiss = OreVeinPlacer.exactly(gneissBlock);
        for (int i = 0; i < 12; i++) {
            RandomSource rand = RandomSource.create(770L + i);
            // aim at the boundary so both halves are in range
            OreVeinPlacer.placeVein(level, rand, origin.getX() + BOX / 2 - 8, origin.getY() + BOX_H / 2,
                    origin.getZ() + BOX / 2 - 8, 12, ore, gneiss);
        }

        int inGneiss = 0, inStone = 0;
        for (BlockPos pos : box(origin)) {
            if (!level.getBlockState(pos).is(ore.getBlock())) continue;
            if (pos.getX() - origin.getX() < BOX / 2) inGneiss++;
            else inStone++;
        }
        helper.assertTrue(inGneiss > 0, "No gneiss ore placed in the gneiss half at all");
        helper.assertTrue(inStone == 0, inStone + " gneiss ore blocks landed in PLAIN STONE; the gneiss "
                + "family must target stone_gneiss exactly");

        clear(level, origin);
        helper.succeed();
    }

    /**
     * The depth deposit: an ore core inside a depth-rock shell. It replaces bedrock too, as the upstream
     * comment says: "yes you've heard right, bedrock".
     */
    public static void oreDepthDeposit(GameTestHelper helper) {
        BlockPos origin = scratch(helper);
        ServerLevel level = helper.getLevel();
        fill(level, origin, Blocks.STONE.defaultBlockState());

        // A bedrock slab through the middle: the deposit must eat into it.
        int bedrockY = origin.getY() + BOX_H / 2;
        for (BlockPos pos : box(origin)) {
            if (pos.getY() == bedrockY) level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 2);
        }

        int size = 5;
        double fillRatio = 0.6D;
        BlockState ore = ModOreBlocks.cluster_depth_iron.get().defaultBlockState();
        BlockState filler = ModOreBlocks.stone_depth.get().defaultBlockState();
        DepthDepositPlacer.generateSphere(level, RandomSource.create(4242L),
                origin.getX() + BOX / 2, bedrockY, origin.getZ() + BOX / 2, size, fillRatio,
                ore, Blocks.STONE.defaultBlockState(), filler);

        int oreCount = count(level, origin, ore.getBlock());
        int fillerCount = count(level, origin, filler.getBlock());
        helper.assertTrue(oreCount > 0, "Depth deposit placed no ore");
        helper.assertTrue(fillerCount > 0, "Depth deposit placed no depth-rock shell");
        helper.assertTrue(fillerCount > oreCount,
                "The shell must be larger than the core (fill = " + fillRatio + "): ore " + oreCount
                        + " vs filler " + fillerCount);

        int bedrockLeftInCore = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = new BlockPos(origin.getX() + BOX / 2 + dx, bedrockY, origin.getZ() + BOX / 2 + dz);
                if (level.getBlockState(pos).is(Blocks.BEDROCK)) bedrockLeftInCore++;
            }
        }
        helper.assertTrue(bedrockLeftInCore == 0,
                bedrockLeftInCore + " bedrock cells survived at the deposit centre; upstream replaces "
                        + "bedrock on purpose so deposits can carve the world floor");

        clear(level, origin);
        helper.succeed();
    }

    /**
     * The config gates are real, not decorative: a zeroed count must generate nothing, and a non-zero
     * one must generate. This is what catches a vein wired to a hardcoded number instead of
     * {@link WorldConfig}.
     */
    public static void oreCounts(GameTestHelper helper) {
        BlockPos origin = scratch(helper);
        ServerLevel level = helper.getLevel();
        fill(level, origin, Blocks.STONE.defaultBlockState());

        BlockState ore = ModBlocks.ore_titanium.get().defaultBlockState();
        int chunkX = origin.getX();
        int chunkZ = origin.getZ();

        OreVeinPlacer.generateOre(level, RandomSource.create(9L), chunkX, chunkZ, 0, 6,
                origin.getY() + 4, 8, ore, OreVeinPlacer.STONE);
        helper.assertTrue(count(level, origin, ore.getBlock()) == 0,
                "A vein count of 0 must place nothing — the config gate is not wired through");

        OreVeinPlacer.generateOre(level, RandomSource.create(9L), chunkX, chunkZ, 8, 6,
                origin.getY() + 4, 8, ore, OreVeinPlacer.STONE);
        helper.assertTrue(count(level, origin, ore.getBlock()) > 0,
                "A vein count of 8 placed nothing");

        clear(level, origin);
        helper.succeed();
    }

    /**
     * Smoldering netherrack only forms on an <b>exposed</b> face: netherrack with air directly above
     * becomes {@code ore_nether_smoldering}, netherrack buried under more netherrack never does.
     */
    public static void oreSmoldering(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos scratch = scratch(helper);
        // The pass picks an ABSOLUTE depth d = 16 + rand.nextInt(96) and scans [d-5, d], so the test
        // slab has to live inside that band whatever height the test platform sits at.
        final int lowY = 16, highY = 111;
        BlockPos origin = new BlockPos(scratch.getX(), lowY, scratch.getZ());
        BlockPos far = new BlockPos(origin.getX() + BOX - 1, highY, origin.getZ() + BOX - 1);
        Iterable<BlockPos> region = BlockPos.betweenClosed(origin, far);

        // Solid netherrack up to a flat top face; everything above is air.
        int exposedY = 60;
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos pos : region) {
            level.setBlock(pos, pos.getY() <= exposedY ? Blocks.NETHERRACK.defaultBlockState() : air, 2);
        }

        // Enough attempts that some roll lands its 5-block scan window on the exposed face.
        // 1.7: 30 columns a chunk, x/z in the chunk and d = 16 + rand(96) (the placed feature's in_square + height_range)
        BlockState smolderingState = ModOreBlocks.ore_nether_smoldering.get().defaultBlockState();
        for (int i = 0; i < 200; i++) {
            RandomSource rand = RandomSource.create(31L + i);
            for (int k = 0; k < 30; k++) {
                CrustColumnFeature.scan(level, new BlockPos(origin.getX() + rand.nextInt(16),
                        16 + rand.nextInt(96), origin.getZ() + rand.nextInt(16)), smolderingState);
            }
        }

        Block smoldering = ModOreBlocks.ore_nether_smoldering.get();
        int onTop = 0, buried = 0;
        for (BlockPos pos : region) {
            if (!level.getBlockState(pos).is(smoldering)) continue;
            if (pos.getY() == exposedY) onTop++;
            else buried++;
        }
        helper.assertTrue(buried == 0,
                buried + " smoldering blocks formed BELOW the surface; the pass requires air directly above");
        helper.assertTrue(onTop > 0,
                "No smoldering netherrack formed on the exposed face after 200 attempts");

        for (BlockPos pos : region) level.setBlock(pos, air, 2);
        helper.succeed();
    }

    /** Veins across a stone/deepslate boundary put the twin only in deepslate; both uranium bands share one vein. */
    public static void oreDeepslateTwin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = scratch(helper);
        int split = origin.getY() + BOX_H / 2;
        for (BlockPos pos : box(origin)) {
            level.setBlock(pos, (pos.getY() < split ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState(), 2);
        }

        Holder<PlacedFeature> stoneBand = null;
        Holder<PlacedFeature> deepBand = null;
        for (Holder<PlacedFeature> holder : level.getBiome(origin).value().getGenerationSettings().features().get(6)) {
            String id = holder.unwrapKey().map(key -> Ids.keyId(key).toString()).orElse("");
            if (id.equals("hbm:ore_uranium")) stoneBand = holder;
            if (id.equals("hbm:ore_uranium_deepslate")) deepBand = holder;
        }
        helper.assertTrue(stoneBand != null && deepBand != null, "hbm:ore_uranium and hbm:ore_uranium_deepslate are not both listed");
        helper.assertTrue(stoneBand.value().feature().value() == deepBand.value().feature().value(),
                "the two uranium bands place different veins");

        for (int i = 0; i < 16; i++) {
            stoneBand.value().feature().value().place(level, level.getChunkSource().getGenerator(),
                    RandomSource.create(70L + i), new BlockPos(origin.getX() + 4, split, origin.getZ() + 4));
        }
        Block ore = ModNuclearBlocks.ore_uranium.get();
        Block twin = ModOreBlocks.ore_uranium_deepslate.get();
        int inStone = 0, inDeepslate = 0, misplaced = 0;
        for (BlockPos pos : box(origin)) {
            BlockState state = level.getBlockState(pos);
            boolean deep = pos.getY() < split;
            if (state.is(ore)) {
                if (deep) misplaced++;
                else inStone++;
            } else if (state.is(twin)) {
                if (deep) inDeepslate++;
                else misplaced++;
            }
        }
        helper.assertTrue(misplaced == 0, misplaced + " ore blocks sit in the other host");
        helper.assertTrue(inStone > 0 && inDeepslate > 0, "veins across the boundary placed " + inStone
                + " stone and " + inDeepslate + " deepslate ore blocks");

        clear(level, origin);
        helper.succeed();
    }

    // Scratch-volume helpers

    /** Bottom-north-west corner of this test's scratch box, well above the test platform. */
    private static BlockPos scratch(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(0, SCRATCH_Y, 0));
    }

    private static Iterable<BlockPos> box(BlockPos origin) {
        return BlockPos.betweenClosed(origin,
                origin.offset(BOX - 1, BOX_H - 1, BOX - 1));
    }

    private static void fill(ServerLevel level, BlockPos origin, BlockState state) {
        for (BlockPos pos : box(origin)) level.setBlock(pos, state, 2);
    }

    private static void clear(ServerLevel level, BlockPos origin) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos pos : box(origin)) level.setBlock(pos, air, 2);
    }

    private static int count(ServerLevel level, BlockPos origin, Block block) {
        int n = 0;
        for (BlockPos pos : box(origin)) {
            if (level.getBlockState(pos).is(block)) n++;
        }
        return n;
    }
}
