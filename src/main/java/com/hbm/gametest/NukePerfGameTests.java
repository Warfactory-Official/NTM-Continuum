package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.config.BombConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionNukeRayBatched;
import com.hbm.util.ChunkCarver;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A real nuclear blast, run to completion and timed.
 *
 * <p>Not an assertion test in the usual sense: it exists so "the explosion feels slow" can be answered
 * with numbers instead of opinions, and so a change to the crater algorithm has a before/after to be
 * judged against. {@link com.hbm.explosion.ExplosionNukeRayBatched} logs the phase split when it
 * finishes; this drives one blast far from the test plots and asserts only that it terminates.
 *
 * <p><b>It detonates 2000 blocks away from the plot</b>, deliberately. A crater of any useful size would
 * eat neighbouring GameTest structures otherwise, which is exactly the plot-collision failure mode that
 * made the ZIRNOX tests look flaky for weeks (§19l).
 *
 * <p>The radius is small next to a real Fat Man (175). It is chosen so the suite stays quick while still
 * exercising every phase on genuinely un-generated terrain; the per-block and per-ray costs the log
 * reports are what scale up, not the shape of the run.
 */
public final class NukePerfGameTests {

    private NukePerfGameTests() {}

    /** Radius of the benchmark blast. */
    private static final int RADIUS = 64;
    /**
     * How far from the test plots each of these tests does its work — <b>a different quadrant each</b>.
     *
     * <p>Not decoration. GameTest plots sit metres apart, so "plot + 2000" is very nearly the same spot
     * for every test in the suite: when all four of these used one offset they wrote into each other's
     * work areas, and {@link #carveLighting} — the only one whose result depends on its surroundings
     * rather than on blocks it placed itself — failed or passed depending on whether the crater
     * benchmark had happened to bury or excavate the ground under it that run. This is the same
     * plot-collision failure mode as §19l, at 2000 blocks' remove.
     *
     * <p>The quadrants are 40,000 blocks apart and the widest work area here is ~300 blocks, so they
     * cannot overlap however the plots are laid out. The distance is what it is because of the pre-26.1
     * framework: it lays the whole suite out in rows of eight, and with the padded templates that grid
     * runs thousands of blocks in +x and +z, so an offset of 2,000 put these work areas on top of later
     * rows' plots (a sat link found the crater's dirt in its sky column). Chunk generation cost does not
     * depend on the distance.
     */
    private static final int OFFSET = 20_000;
    /** Work-area offsets, one quadrant per test. See {@link #OFFSET}. */
    private static final int BENCH_X = OFFSET, BENCH_Z = OFFSET;
    private static final int THROUGHPUT_X = OFFSET, THROUGHPUT_Z = -OFFSET;
    private static final int EQUIVALENCE_X = -OFFSET, EQUIVALENCE_Z = OFFSET;
    private static final int LIGHTING_X = -OFFSET, LIGHTING_Z = -OFFSET;
    private static final int CRATER_LIGHT_X = 0, CRATER_LIGHT_Z = 2 * OFFSET;
    private static final int FALLOUT_X = 0, FALLOUT_Z = -2 * OFFSET;
    private static final int FALLOUT_WRITE_X = 2 * OFFSET, FALLOUT_WRITE_Z = 0;
    private static final int ASH_X = -2 * OFFSET, ASH_Z = 0;
    private static final int NOTIFY_X = 0, NOTIFY_Z = 3 * OFFSET;
    private static final int RESUME_X = 3 * OFFSET, RESUME_Z = 3 * OFFSET;
    /** Far enough that the second sweep cannot touch a chunk the first one loaded. */
    private static final int VIRGIN_OFFSET = 600;

    public static void craterBenchmark(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plot = helper.absolutePos(BlockPos.ZERO);
        // Centre the blast inside a solid cube built above the plot floor. WORLD_SURFACE is useless here:
        // out in the empty test level it sits on the world floor, so a downward fill clips away to a
        // single layer (the first run of this benchmark filled 2401 blocks and measured nothing).
        BlockPos origin = plot.offset(BENCH_X, RADIUS + 2, BENCH_Z);
        int y = origin.getY();

        // A dense block of stone to actually destroy. The GameTest level is near-empty out here, and a
        // crater through air measures nothing: the destruction phase is the whole question.
        long fillStart = System.currentTimeMillis();
        // Written straight into the chunk sections: filling a million blocks through Level.setBlock is
        // seconds of setup for a test that is measuring milliseconds. The ray phase only reads block
        // states, so an un-relit fill is a perfectly good input.
        // Dirt, not stone: stone's resistance kills a ray about ten blocks in, so a stone benchmark
        // carves ~1800 blocks and says nothing about the destruction phase. Dirt lets the rays run
        // nearly the full radius, which is what a blast over normal terrain actually does.
        net.minecraft.world.level.block.state.BlockState stone =
                net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        BlockPos.MutableBlockPos fill = new BlockPos.MutableBlockPos();
        int half = RADIUS;
        int filled = 0;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                // Ground below, air above — a blast over terrain, not one buried in solid rock. The
                // mix matters: the ray loop's air fast-path is most of the win on a real surface burst.
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
                for (int dy = -half; dy <= 0; dy++) {
                    int fy = y + dy;
                    if (fy < level.getMinY() || fy > level.getMaxY()) continue;
                    chunk.getSection(chunk.getSectionIndex(fy))
                            .setBlockState(x & 15, fy & 15, z & 15, stone, false);
                    filled++;
                }
                // Deliberately not marked unsaved: this fill exists for the length of one test and
                // `markUnsaved()` is 26.1-only (`setUnsaved(boolean)` below), which is not worth a
                // facade for benchmark scaffolding.
            }
        }
        Constants.LOG.info("[NUKE-BENCH] filled {} stone blocks in {} ms", filled, System.currentTimeMillis() - fillStart);
        int filledBlocks = filled;

        // The blast is an entity: it ticks only in an entity-ticking chunk, and the fill above loaded this
        // work area at load level only. In game the bomb sits in a player's ticking chunks; here the same
        // hold the fallout benchmark takes stands in for the player.
        com.hbm.world.chunk.ChunkLoadingManager.requestTicket(level, "crater_benchmark")
                .force(origin.getX() >> 4, origin.getZ() >> 4);
        EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFacNoRad(
                level, RADIUS, origin.getX(), y, origin.getZ());
        level.addFreshEntity(blast);

        // Let real server ticks drive it. The crater's loading-only requests are deliberately asynchronous;
        // manually spin-ticking the entity would starve the chunk task system that has to satisfy them.
        long start = System.currentTimeMillis();
        BlastDriver driver = new BlastDriver(blast);
        pollUntil(helper, BLAST_POLL_TRIES, driver::isComplete,
                "the blast did not finish — it is either stuck or far too slow",
                () -> {
                    long wall = System.currentTimeMillis() - start;
                    Constants.LOG.info("[NUKE-BENCH] r={} finished in {} ms wall over {} ticks "
                                    + "({} filled blocks, budget {} ms/tick)",
                            RADIUS, wall, blast.tickCount, filledBlocks, BombConfig.mk5);
                    helper.assertTrue(blast.getBlocksDestroyed() == 313_350,
                            "the deterministic r=64 crater destroyed " + blast.getBlocksDestroyed()
                                    + " blocks instead of 313350; ray geometry or resistance changed");
                    helper.succeed();
                });
    }

    /**
     * The decisive measurement for the destruction phase: per-block {@code Level.setBlock} against a
     * direct section write.
     *
     * <p>Both references carve whole {@code LevelChunkSection}s and swap them in rather than calling
     * {@code setBlock} per block, and this is the number that says whether that is worth porting. Each
     * pass clears the same shape from an identical freshly-filled region, so the only difference is how
     * the write reaches the chunk.
     */
    public static void writeThroughput(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plot = helper.absolutePos(BlockPos.ZERO);
        net.minecraft.world.level.block.state.BlockState stone =
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState air =
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

        int side = 32;           // 32x32x32 = 32768 blocks per pass
        int baseY = plot.getY() + 8;

        // --- pass A: Level.setBlock, the flags the crater currently uses ---
        BlockPos aOrigin = plot.offset(THROUGHPUT_X, 0, THROUGHPUT_Z);
        fillCube(level, aOrigin, baseY, side, stone);
        int flags = net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE
                | net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long t0 = System.nanoTime();
        for (int dx = 0; dx < side; dx++) {
            for (int dy = 0; dy < side; dy++) {
                for (int dz = 0; dz < side; dz++) {
                    pos.set(aOrigin.getX() + dx, baseY + dy, aOrigin.getZ() + dz);
                    level.setBlock(pos, air, flags);
                }
            }
        }
        long setBlockMs = (System.nanoTime() - t0) / 1_000_000L;

        // --- pass B: straight into the section's paletted container ---
        BlockPos bOrigin = plot.offset(THROUGHPUT_X + 128, 0, THROUGHPUT_Z);
        fillCube(level, bOrigin, baseY, side, stone);

        long t1 = System.nanoTime();
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                int x = bOrigin.getX() + dx;
                int z = bOrigin.getZ() + dz;
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
                for (int dy = 0; dy < side; dy++) {
                    int y = baseY + dy;
                    net.minecraft.world.level.chunk.LevelChunkSection section =
                            chunk.getSection(chunk.getSectionIndex(y));
                    section.setBlockState(x & 15, y & 15, z & 15, air, false);
                }
            }
        }
        long sectionMs = (System.nanoTime() - t1) / 1_000_000L;

        int blocks = side * side * side;
        Constants.LOG.info("[NUKE-BENCH] {} blocks: Level.setBlock {} ms ({}/ms), section write {} ms ({}/ms) — {}x",
                blocks, setBlockMs, setBlockMs > 0 ? blocks / setBlockMs : blocks,
                sectionMs, sectionMs > 0 ? blocks / sectionMs : blocks,
                sectionMs > 0 ? setBlockMs / sectionMs : -1);

        helper.assertTrue(setBlockMs >= 0 && sectionMs >= 0, "timing failed");
        helper.succeed();
    }

    /**
     * The carve path against the write path it replaces, on the same shape.
     *
     * <p>{@code nuke_write_throughput} says the section write is faster; this says it is <em>correct</em>,
     * which is the harder half. Two identical stone regions, each with a chest buried in it, are cleared
     * through the same bit pattern — one by {@link ChunkCarver}, one by {@code Level.setBlock} — and
     * every observable the crater relies on is compared: the block states, the block entity's removal,
     * and the {@code WORLD_SURFACE} heightmap. A wrong heightmap is the failure mode that leaves a black
     * world, and it is invisible to anything that only checks block states.
     */
    public static void carveEquivalence(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plot = helper.absolutePos(BlockPos.ZERO);
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        int side = 12;
        int baseY = plot.getY() + 8;
        // Four chunks apart, so the two regions cannot share a chunk's heightmap or light state. Both
        // are aligned to a chunk origin: a bitset is per chunk, and a region straddling a boundary
        // would silently drop the bits on the far side.
        BlockPos carveOrigin = new BlockPos(((plot.getX() + EQUIVALENCE_X) >> 4) << 4, baseY,
                ((plot.getZ() + EQUIVALENCE_Z) >> 4) << 4);
        BlockPos writeOrigin = carveOrigin.offset(64, 0, 0);

        fillCube(level, carveOrigin, baseY, side, stone);
        fillCube(level, writeOrigin, baseY, side, stone);

        // One block entity in each, at the same relative spot, and inside the half that gets cleared
        // (the bitset below covers dy >= side / 2 only): the carve must not orphan it.
        BlockPos carveChest = carveOrigin.offset(2, side / 2 + 1, 2);
        BlockPos writeChest = writeOrigin.offset(2, side / 2 + 1, 2);
        level.setBlock(carveChest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(writeChest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockEntity(carveChest) != null, "the carve region's chest has no block entity");

        // The crater's own bit layout, over the top half of each cube so the heightmap has to move and a
        // solid floor is left behind for it to move to.
        int maxY = level.getMaxY();
        java.util.BitSet carveBits = new java.util.BitSet();
        java.util.BitSet writeBits = new java.util.BitSet();
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                for (int dy = side / 2; dy < side; dy++) {
                    int y = baseY + dy;
                    int column = ((maxY - y) << 8);
                    carveBits.set(column | (((carveOrigin.getX() + dx) & 15) << 4) | ((carveOrigin.getZ() + dz) & 15));
                    writeBits.set(column | (((writeOrigin.getX() + dx) & 15) << 4) | ((writeOrigin.getZ() + dz) & 15));
                }
            }
        }

        // --- carve path ---
        int cx = carveOrigin.getX() >> 4;
        int cz = carveOrigin.getZ() >> 4;
        ChunkCarver carver = new ChunkCarver(level, level.getChunk(cx, cz), carveBits, cx, cz, maxY);
        helper.assertTrue(carver.carveUntil(System.currentTimeMillis() + 10_000L), "the carve did not finish");
        carver.finish();

        // --- write path, with the flags the per-block destruction loop uses ---
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int wcx = writeOrigin.getX() >> 4;
        int wcz = writeOrigin.getZ() >> 4;
        for (int bit = writeBits.nextSetBit(0); bit >= 0; bit = writeBits.nextSetBit(bit + 1)) {
            pos.set((wcx << 4) | ((bit >> 4) & 15), maxY - (bit >>> 8), (wcz << 4) | (bit & 15));
            level.setBlock(pos, air, flags);
        }

        // --- compare, cell by cell, over the whole cube: cleared half and untouched half alike ---
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                for (int dy = 0; dy < side; dy++) {
                    BlockState carved = level.getBlockState(carveOrigin.offset(dx, dy, dz));
                    BlockState written = level.getBlockState(writeOrigin.offset(dx, dy, dz));
                    if (carved != written) {
                        helper.fail("carve and setBlock disagree at (" + dx + "," + dy + "," + dz + "): "
                                + carved + " vs " + written);
                        return;
                    }
                }
            }
        }

        helper.assertTrue(level.getBlockEntity(carveChest) == null,
                "the carve left the chest's block entity behind");
        helper.assertTrue(level.getBlockEntity(writeChest) == null,
                "the setBlock pass left the chest's block entity behind — the comparison would be vacuous");

        // Heightmaps: the carve repairs them once per chunk instead of once per block, so this is the
        // check that the repair is equivalent, not merely present.
        net.minecraft.world.level.chunk.LevelChunk carveChunk = level.getChunk(cx, cz);
        net.minecraft.world.level.chunk.LevelChunk writeChunk = level.getChunk(wcx, wcz);
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                int carveHeight = carveChunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        (carveOrigin.getX() + dx) & 15, (carveOrigin.getZ() + dz) & 15);
                int writeHeight = writeChunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        (writeOrigin.getX() + dx) & 15, (writeOrigin.getZ() + dz) & 15);
                helper.assertTrue(carveHeight == writeHeight,
                        "WORLD_SURFACE disagrees at column (" + dx + "," + dz + "): "
                                + carveHeight + " vs " + writeHeight);
            }
        }

        Constants.LOG.info("[NUKE-BENCH] carve equivalence: {} blocks cleared; block states, block entity "
                + "and WORLD_SURFACE all match the setBlock path", carver.carved());
        helper.succeed();
    }

    /**
     * Does the crater actually get <em>lit</em> after a carve?
     *
     * <p>The one thing no equivalence check on block states can see, and the one this class got wrong
     * first time round: a carve writes into the section's palette and never calls {@code checkBlock}, so
     * the whole relight rests on the per-chunk pass in {@link ChunkCarver#finish()}. The first version
     * queued only {@code propagateLightSources} — the <em>fresh chunk</em> seeding path — and left
     * craters pitch black.
     *
     * <p>Two identical pockets of stone are buried under open sky and their roofs taken off, one by the
     * carver and one by {@code Level.setBlock}; the sky light inside is then compared. Against the write
     * path rather than a fixed number, on purpose: what has to hold is that a carve lights the same as
     * vanilla would.
     *
     * <p><b>It waits on the light engine, not on a tick count.</b> The light engine runs on its own
     * thread and needs wall-clock time, and a GameTest server does not tick in real time — it runs ticks
     * back to back as fast as it can, so "60 ticks" here can be 30 ms. An earlier version used fixed
     * delays and failed intermittently on a pocket whose light had not settled. Both phases poll instead.
     */
    public static void carveLighting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plot = helper.absolutePos(BlockPos.ZERO);
        BlockState stone = Blocks.STONE.defaultBlockState();
        int maxY = level.getMaxY();

        // Chunk-aligned and far from the plots, like the other tests here.
        int baseY = plot.getY() + 8;
        BlockPos carveOrigin = new BlockPos(((plot.getX() + LIGHTING_X) >> 4) << 4, baseY,
                ((plot.getZ() + LIGHTING_Z) >> 4) << 4);
        BlockPos writeOrigin = carveOrigin.offset(64, 0, 0);

        int side = 8;
        int roof = 4;   // layers of stone taken off the top
        fillCube(level, carveOrigin, baseY, side, stone);
        fillCube(level, writeOrigin, baseY, side, stone);

        // The measured cell: dead centre, just under the new floor-to-be.
        BlockPos carveProbe = carveOrigin.offset(side / 2, side - roof, side / 2);
        BlockPos writeProbe = writeOrigin.offset(side / 2, side - roof, side / 2);

        // Phase 1 — wait for the fill to have darkened both pockets.
        pollUntilOrSkip(helper, LIGHT_POLL_TRIES,
                () -> sky(level, carveProbe) == 0 && sky(level, writeProbe) == 0,
                "the buried pockets never went dark",
                () -> {
                    java.util.BitSet carveBits = new java.util.BitSet();
                    java.util.BitSet writeBits = new java.util.BitSet();
                    for (int dx = 0; dx < side; dx++) {
                        for (int dz = 0; dz < side; dz++) {
                            for (int dy = side - roof; dy < side; dy++) {
                                int column = ((maxY - (baseY + dy)) << 8);
                                carveBits.set(column | (((carveOrigin.getX() + dx) & 15) << 4)
                                        | ((carveOrigin.getZ() + dz) & 15));
                                writeBits.set(column | (((writeOrigin.getX() + dx) & 15) << 4)
                                        | ((writeOrigin.getZ() + dz) & 15));
                            }
                        }
                    }

                    int cx = carveOrigin.getX() >> 4;
                    int cz = carveOrigin.getZ() >> 4;
                    ChunkCarver carver = new ChunkCarver(level, level.getChunk(cx, cz), carveBits, cx, cz, maxY);
                    carver.carveUntil(System.currentTimeMillis() + 10_000L);
                    carver.finish();

                    int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
                    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                    int wcx = writeOrigin.getX() >> 4;
                    int wcz = writeOrigin.getZ() >> 4;
                    for (int bit = writeBits.nextSetBit(0); bit >= 0; bit = writeBits.nextSetBit(bit + 1)) {
                        pos.set((wcx << 4) | ((bit >> 4) & 15), maxY - (bit >>> 8), (wcz << 4) | (bit & 15));
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
                    }

                    // Phase 2 — wait for the *vanilla* path to light up and the engine to fall idle. That
                    // is the moment the carve path has had exactly the same opportunity.
                    pollUntil(helper, LIGHT_POLL_TRIES,
                            () -> sky(level, writeProbe) > 0 && sky(level, carveProbe) > 0,
                            "one of the pockets never lit: carve " + sky(level, carveProbe)
                                    + ", setBlock " + sky(level, writeProbe),
                            () -> {
                                int carved = sky(level, carveProbe);
                                int written = sky(level, writeProbe);
                                Constants.LOG.info(
                                        "[NUKE-BENCH] sky light under the removed roof: carve {}, setBlock {}",
                                        carved, written);
                                helper.assertTrue(carved == written,
                                        "the carved pocket lit differently from the setBlock pocket: sky "
                                                + carved + " vs " + written
                                                + " — the crater's relight is wrong");
                                helper.succeed();
                            });
                });
    }

    /**
     * The lighting check that matters: a <b>real crater</b>, not a hand-carved pocket.
     *
     * <p>A pocket test turned out to have no teeth — opening four layers to the sky relights fine even
     * with a half-finished relight pass, because that case is exactly the one
     * {@code propagateLightSources} was written for. A blast is different in every way that could
     * matter: it empties whole sections, it moves the sky-light source of a column by tens of blocks,
     * and it does it across a hundred chunks at once.
     *
     * <p>So: bury a slab of stone deep enough to be pitch black inside, detonate in the middle of it,
     * and require that the crater floor ends up lit while stone well outside the blast stays dark. The
     * second half is what stops this passing on a world that was never dark to begin with.
     */
    public static void craterLighting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plot = helper.absolutePos(BlockPos.ZERO);
        BlockState stone = Blocks.DIRT.defaultBlockState();

        int radius = 16;
        // A slab wide enough that its middle is genuinely enclosed, and thick enough that the blast has
        // ground to carve rather than air. Dirt, not stone: stone's resistance kills a ray about ten
        // blocks in, so a stone slab is not a crater test, it is a scratch test (same reason the
        // benchmark above uses dirt).
        // Kept small on purpose. The slab is laid with Level.setBlock so that it genuinely darkens —
        // only the per-block checkBlock path drives a light *decrease*, and a carve never needs one — and
        // every one of those blocks is a light-engine task. A slab three times this size queued enough
        // work that the wait below timed out on some runs.
        int half = radius + 8;
        int depth = 8;
        BlockPos centre = plot.offset(CRATER_LIGHT_X, 24, CRATER_LIGHT_Z);

        fillSlabAndDarken(level, centre, half, depth, stone);

        // Inside the blast, near the top of the slab: must end up open to the sky.
        BlockPos inside = centre.offset(0, 2, 0);
        // The control that keeps this test honest: it must stay dark, so it has to be genuinely buried.
        // An earlier version put it one block in from the slab's top corner, where sky light reaches it
        // from the side — it read 15 as soon as the light settled and only passed on timing. This one is
        // 8 blocks in from the edge and 13 below the top, i.e. 21 steps of attenuation from any sky,
        // while still sitting 22 blocks out from a 16-block blast.
        BlockPos outside = centre.offset(half - 8, -depth + 3, half - 8);

        pollUntilOrSkip(helper, LIGHT_POLL_TRIES,
                () -> sky(level, inside) == 0 && sky(level, outside) == 0,
                "the slab never went dark inside",
                () -> {
                    com.hbm.world.chunk.ChunkLoadingManager.requestTicket(level, "crater_lighting")
                            .force(centre.getX() >> 4, centre.getZ() >> 4);
                    EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFacNoRad(
                            level, radius, centre.getX(), centre.getY(), centre.getZ());
                    level.addFreshEntity(blast);

                    BlastDriver driver = new BlastDriver(blast);
                    pollUntil(helper, BLAST_POLL_TRIES, driver::isComplete,
                            "the blast did not finish",
                            () -> {
                                helper.assertTrue(level.getBlockState(inside).isAir(),
                                        "the blast did not reach the probe; it is still " + level.getBlockState(inside));

                                pollUntil(helper, LIGHT_POLL_TRIES,
                                        () -> sky(level, inside) == 15,
                                        "the crater never lit: sky light at the crater floor stayed "
                                                + sky(level, inside) + " — the relight after a carve is broken",
                                        () -> {
                                            int lit = sky(level, inside);
                                            int dark = sky(level, outside);
                                            Constants.LOG.info("[NUKE-BENCH] crater lighting: inside {}, buried control {}",
                                                    lit, dark);
                                            helper.assertTrue(lit == 15,
                                                    "the crater floor is open to the sky but its sky light is " + lit);
                                            // Strictly darker than the crater floor, not strictly zero. What this
                                            // control exists to catch is the crater's relight blanket-lighting the
                                            // whole chunk; demanding an exact 0 also demands that no sky light bleeds
                                            // in sideways from the slab's edge, which depends on exactly where the
                                            // probe sits and made the test flake on its own geometry twice.
                                            helper.assertTrue(dark < lit,
                                                    "the buried control is as lit as the crater floor (" + dark + " vs "
                                                            + lit + "); the whole chunk was relit rather than the "
                                                            + "crater, so this proves nothing");
                                            helper.succeed();
                                        });
                            });
                });
    }

    /**
     * The fallout sweep, timed — the phase that runs <em>after</em> the crater and lays down the ash and
     * the sellafite rings.
     *
     * <p>The crater is not the whole blast. The sweep walks every chunk the fallout disc touches,
     * stomps all 256 columns of each, and applies the resulting writes under the same per-tick budget
     * the crater uses, so it can hold the server at its full tick budget for a long time after the
     * explosion has visually "finished". {@link EntityFalloutRain} logs its own phase split; this drives
     * one sweep over real ground and asserts only that it terminates.
     */
    public static void falloutBenchmark(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plot = helper.absolutePos(BlockPos.ZERO);
        BlockState dirt = Blocks.DIRT.defaultBlockState();

        // Small: the sweep is now driven by real server ticks rather than a spin loop, so its scale is
        // paid for in suite wall-clock. Big enough to cover every phase, no bigger.
        int scale = 32;
        BlockPos centre = plot.offset(FALLOUT_X, 8, FALLOUT_Z);

        // Ground for the sweep to actually convert: over air it walks the columns and writes nothing,
        // which is the same trap the crater benchmark fell into first time round.
        long fillStart = System.currentTimeMillis();
        int filled = 0;
        BlockPos.MutableBlockPos fill = new BlockPos.MutableBlockPos();
        for (int dx = -scale; dx <= scale; dx++) {
            for (int dz = -scale; dz <= scale; dz++) {
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
                for (int dy = -8; dy <= 0; dy++) {
                    int y = centre.getY() + dy;
                    if (y < level.getMinY() || y > level.getMaxY()) continue;
                    chunk.getSection(chunk.getSectionIndex(y))
                            .setBlockState(x & 15, y & 15, z & 15, dirt, false);
                    filled++;
                }
                // The stomp starts from WORLD_SURFACE, so the section writes above have to be reflected
                // in the heightmap or every column reports "nothing here" and the sweep measures air.
                net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(chunk,
                        java.util.EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING));
            }
        }
        Constants.LOG.info("[FALLOUT-BENCH] filled {} blocks in {} ms", filled,
                System.currentTimeMillis() - fillStart);

        com.hbm.entity.effect.EntityFalloutRain rain = spawnSweep(level, centre, scale);

        // Driven by real server ticks, not a tight rain.tick() loop.
        //
        // The loop worked while the sweep resolved its chunks synchronously, because the chunk arrived
        // inside the call. It cannot work now that the sweep claims a chunk and waits: the chunk system
        // only advances when the server ticks, so a driver that never lets the server tick spins
        // forever — this one ran 223 million iterations and timed out.
        long start = System.currentTimeMillis();
        pollUntil(helper, SWEEP_POLL_TRIES, () -> !rain.isAlive(),
                "the pre-loaded sweep did not finish",
                () -> {
                    Constants.LOG.info("[FALLOUT-BENCH] scale={} over loaded ground finished in {} ms wall",
                            scale, System.currentTimeMillis() - start);

                    // Second sweep, over ground nobody has loaded. This is the case that actually happens
                    // in a world — the disc reaches far past whatever a player had loaded — and the only
                    // one that exercises the claim-and-wait path. The first cannot: filling its ground
                    // loaded every chunk it would ask for.
                    int virginScale = 96;
                    BlockPos virginCentre = findVirginFalloutCentre(level, centre);
                    com.hbm.entity.effect.EntityFalloutRain virgin = spawnSweep(level, virginCentre, virginScale);
                    long virginStart = System.currentTimeMillis();
                    pollUntil(helper, SWEEP_POLL_TRIES, () -> !virgin.isAlive(),
                            "the virgin-terrain sweep did not finish",
                            () -> {
                                Constants.LOG.info("[FALLOUT-BENCH] scale={} over virgin terrain finished "
                                        + "in {} ms wall", virginScale, System.currentTimeMillis() - virginStart);
                                helper.assertTrue(virgin.getChunksRequested() > 0,
                                        "the virgin fallout benchmark requested no chunks; it did not test "
                                                + "the loading pipeline");
                                helper.assertTrue(virgin.getPeakUnresolvedRequests()
                                                <= virgin.getMaxUnresolvedRequests(),
                                        "fallout exceeded its unresolved request cap: peak "
                                                + virgin.getPeakUnresolvedRequests() + " > "
                                                + virgin.getMaxUnresolvedRequests());
                                assertAshIsTicking(helper, level, centre, scale);
                                helper.succeed();
                            });
                });
    }

    /**
     * The sweep writes its blocks straight into the chunk sections now, which skips {@code onPlace} —
     * so the apply pass has to call it by hand. This is the check that it does.
     *
     * <p>Not cosmetic: {@code onPlace} is what schedules {@link com.hbm.blocks.generic.BlockFallout}'s
     * tick, and that tick is the block's entire purpose. Ash placed without it sits there looking
     * radioactive and emitting nothing, which no block-state comparison would ever notice.
     */
    private static void assertAshIsTicking(GameTestHelper helper, ServerLevel level, BlockPos centre, int scale) {
        net.minecraft.world.level.block.Block fallout = com.hbm.blocks.ModFalloutBlocks.fallout.get();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        int found = 0;
        int ticking = 0;
        for (int dx = -scale; dx <= scale && found < 32; dx++) {
            for (int dz = -scale; dz <= scale && found < 32; dz++) {
                for (int dy = -4; dy <= 8; dy++) {
                    probe.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (!level.getBlockState(probe).is(fallout)) continue;
                    found++;
                    if (level.getBlockTicks().hasScheduledTick(probe.immutable(), fallout)) ticking++;
                    break;
                }
            }
        }
        Constants.LOG.info("[FALLOUT-BENCH] ash sampled: {} found, {} with a scheduled tick", found, ticking);
        helper.assertTrue(found > 0, "the sweep laid down no ash to check");
        helper.assertTrue(ticking == found,
                "only " + ticking + " of " + found + " ash blocks have a scheduled tick — the section-write "
                        + "apply path is not calling onPlace, so the ash is inert");
    }

    /** Poll attempts for a whole sweep to finish. Generous: it has real chunks to wait on. */
    private static final int SWEEP_POLL_TRIES = 4000;
    /** Poll attempts for an asynchronously chunk-loading crater to finish. */
    private static final int BLAST_POLL_TRIES = 800;

    /**
     * Drives a blast once per poll while still yielding real server ticks to its asynchronous chunk
     * requests. These benchmarks live outside the GameTest simulation area, so the level does not tick
     * their entities reliably; the old tight loop drove them but starved the chunk task system.
     */
    private static final class BlastDriver {
        private final EntityNukeExplosionMK5 blast;
        private int observedTick;

        private BlastDriver(EntityNukeExplosionMK5 blast) {
            this.blast = blast;
            this.observedTick = blast.tickCount;
        }

        private boolean isComplete() {
            if (!blast.isAlive()) return true;
            if (blast.tickCount == observedTick) blast.tick();
            observedTick = blast.tickCount;
            return !blast.isAlive();
        }
    }

    /**
     * Proves that event-driven per-chunk ray resumption produces the complete same hit set as an
     * all-resident trace, not merely the same number of hits.
     */
    public static void rayResumeEquivalence(GameTestHelper helper) {
        helper.assertTrue(ExplosionNukeRayBatched.attenuationTableContractForTest(),
                "cached nuke attenuation differs from the exact per-sample formula");
        ServerLevel level = helper.getLevel();
        BlockPos plot = helper.absolutePos(BlockPos.ZERO);
        int radius = 20;
        int y = plot.getY() + radius + 4;
        int firstX = (((plot.getX() + RESUME_X) >> 4) << 4) + 8;
        int firstZ = (((plot.getZ() + RESUME_Z) >> 4) << 4) + 8;
        BlockPos residentCentre = new BlockPos(firstX, y, firstZ);
        BlockPos resumedCentre = residentCentre.offset(128, 0, 0); // same local chunk coordinates

        fillRayVolume(level, residentCentre, radius);
        fillRayVolume(level, resumedCentre, radius);

        ExplosionNukeRayBatched resident = new ExplosionNukeRayBatched(level,
                residentCentre.getX(), residentCentre.getY(), residentCentre.getZ(), radius * 2, radius);
        ExplosionNukeRayBatched resumed = new ExplosionNukeRayBatched(level,
                resumedCentre.getX(), resumedCentre.getY(), resumedCentre.getZ(), radius * 2, radius);
        resumed.deferAlternateResidentChunksForTest();

        Runnable cleanup = () -> {
            resident.cancel();
            resumed.cancel();
        };
        pollUntilWithCleanup(helper, BLAST_POLL_TRIES,
                () -> {
                    if (!resident.isRayPhaseComplete()) resident.update(BombConfig.mk5);
                    if (!resumed.isRayPhaseComplete()) resumed.update(BombConfig.mk5);
                    return resident.isRayPhaseComplete() && resumed.isRayPhaseComplete();
                },
                "the resident/resumed ray comparison did not finish",
                () -> {
                    try {
                        helper.assertTrue(!resident.hasFailed() && !resumed.hasFailed(),
                                "one side of the ray-resume comparison failed");
                        helper.assertTrue(resident.hasSameHitsRelativeTo(resumed),
                                "per-chunk crater hit bitsets differ between resident and resumed rays");
                        helper.succeed();
                    } finally {
                        cleanup.run();
                    }
                }, cleanup);
    }

    private static com.hbm.entity.effect.EntityFalloutRain spawnSweep(ServerLevel level, BlockPos centre,
                                                                     int scale) {
        // The sweep's own chunk has to be simulating or the entity never gets a tick, and over virgin
        // terrain nothing else is holding it. In game a player is standing at the blast site; here the
        // test has to say so itself. Claimed and left claimed: the test's world is thrown away after it.
        com.hbm.world.chunk.ChunkLoadingManager.requestTicket(level, "fallout_benchmark")
                .force(centre.getX() >> 4, centre.getZ() >> 4);
        com.hbm.entity.effect.EntityFalloutRain rain = new com.hbm.entity.effect.EntityFalloutRain(
                com.hbm.entity.ModEntities.FALLOUT_RAIN.get(), level);
        rain.setScale(scale);
        rain.snapTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5);
        level.addFreshEntity(rain);
        return rain;
    }

    /** Finds a 15x15 FULL-chunk-empty area, then loads only its centre so the logic entity can spawn. */
    private static BlockPos findVirginFalloutCentre(ServerLevel level, BlockPos origin) {
        for (int attempt = 0; attempt < 64; attempt++) {
            int distance = VIRGIN_OFFSET + attempt * 512;
            BlockPos candidate = origin.offset(distance, 0, distance);
            int cx = candidate.getX() >> 4;
            int cz = candidate.getZ() >> 4;
            boolean virgin = true;
            for (int dx = -7; dx <= 7 && virgin; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    if (level.getChunkSource().getChunkNow(cx + dx, cz + dz) != null) {
                        virgin = false;
                        break;
                    }
                }
            }
            if (!virgin) continue;
            level.getChunk(cx, cz); // test setup only; surrounding targets remain on-demand
            return candidate;
        }
        throw new IllegalStateException("could not find virgin terrain for the fallout loading benchmark");
    }

    /**
     * The three ways a fallout <em>conversion</em> can reach the world, timed against each other.
     *
     * <p>The fallout sweep does not clear blocks, it <b>replaces</b> them (sellafite rings, ash layers,
     * fire), and it currently does so with {@code Level.setBlock(pos, state, UPDATE_ALL)} on the server
     * thread — which is a different cost from the crater's clear, because {@code UPDATE_ALL} carries
     * neighbour and shape updates. Both references instead write the palette off-thread and run only the
     * notify work on the server thread. This measures what each layer of that actually costs, so the
     * decision to port it is made on numbers.
     *
     * <p>Three identical regions, same shape converted in each:
     * <ol>
     *   <li><b>setBlock(UPDATE_ALL)</b> — the port today.</li>
     *   <li><b>section write + per-block neighbour/shape notify + one relight per chunk</b> — the
     *       reference shape, with the palette write done in place rather than off-thread (the off-thread
     *       part removes it from the server thread entirely, so this is the pessimistic version of it).</li>
     *   <li><b>section write + one relight per chunk, no neighbour notify</b> — the floor, and a
     *       behaviour change: nothing attached to a converted block would be told it changed.</li>
     * </ol>
     */
    public static void falloutWriteThroughput(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plot = helper.absolutePos(BlockPos.ZERO);
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState converted = Blocks.COBBLESTONE.defaultBlockState();

        int side = 32;                       // 32^3 = 32768 conversions per pass
        int baseY = plot.getY() + 8;
        BlockPos aOrigin = plot.offset(FALLOUT_WRITE_X, 0, FALLOUT_WRITE_Z);
        BlockPos bOrigin = aOrigin.offset(128, 0, 0);
        BlockPos cOrigin = aOrigin.offset(256, 0, 0);

        fillCube(level, aOrigin, baseY, side, stone);
        fillCube(level, bOrigin, baseY, side, stone);
        fillCube(level, cOrigin, baseY, side, stone);

        // --- A: what the sweep does today ---
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long t0 = System.nanoTime();
        for (int dx = 0; dx < side; dx++) {
            for (int dy = 0; dy < side; dy++) {
                for (int dz = 0; dz < side; dz++) {
                    pos.set(aOrigin.getX() + dx, baseY + dy, aOrigin.getZ() + dz);
                    level.setBlock(pos, converted, Block.UPDATE_ALL);
                }
            }
        }
        long setBlockMs = (System.nanoTime() - t0) / 1_000_000L;

        // --- B: the reference shape ---
        long t1 = System.nanoTime();
        long bWrite = sectionConvert(level, bOrigin, baseY, side, converted, true);
        long referenceMs = (System.nanoTime() - t1) / 1_000_000L;

        // --- C: the floor ---
        long t2 = System.nanoTime();
        sectionConvert(level, cOrigin, baseY, side, converted, false);
        long floorMs = (System.nanoTime() - t2) / 1_000_000L;

        int blocks = side * side * side;
        Constants.LOG.info("[FALLOUT-BENCH] {} conversions: setBlock(UPDATE_ALL) {} ms ({}/ms) | "
                        + "section+notify+relight {} ms ({}/ms, palette {} ms) | section+relight only {} ms ({}/ms)",
                blocks,
                setBlockMs, rate(blocks, setBlockMs),
                referenceMs, rate(blocks, referenceMs), bWrite,
                floorMs, rate(blocks, floorMs));

        helper.assertTrue(level.getBlockState(new BlockPos(aOrigin.getX() + 1, baseY + 1, aOrigin.getZ() + 1)).is(Blocks.COBBLESTONE),
                "pass A did not convert");
        helper.assertTrue(level.getBlockState(new BlockPos(bOrigin.getX() + 1, baseY + 1, bOrigin.getZ() + 1)).is(Blocks.COBBLESTONE),
                "pass B did not convert");
        helper.assertTrue(level.getBlockState(new BlockPos(cOrigin.getX() + 1, baseY + 1, cOrigin.getZ() + 1)).is(Blocks.COBBLESTONE),
                "pass C did not convert");
        helper.succeed();
    }

    /** State writes are last-wins; falling markers use their own first-wins position set. */
    public static void falloutWriteDeduplication(GameTestHelper helper) {
        helper.assertTrue(com.hbm.entity.effect.EntityFalloutRain.writeDeduplicationContractForTest(
                        Blocks.STONE.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState()),
                "fallout scan writes no longer match the references' position-map deduplication");
        helper.succeed();
    }

    private static long rate(int blocks, long ms) {
        return ms > 0 ? blocks / ms : blocks;
    }

    /**
     * Converts a cube by writing its sections directly, then repairing the chunk once — optionally doing
     * the per-block neighbour and shape notifications the references keep.
     *
     * @return milliseconds spent on the palette writes alone, which is the part the references move
     *         off the server thread entirely
     */
    private static long sectionConvert(ServerLevel level, BlockPos origin, int baseY, int side,
                                       BlockState state, boolean notifyNeighbours) {
        it.unimi.dsi.fastutil.longs.LongOpenHashSet chunks = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        long writeStart = System.nanoTime();
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
                chunks.add(net.minecraft.world.level.ChunkPos.pack(x >> 4, z >> 4));
                for (int dy = 0; dy < side; dy++) {
                    int y = baseY + dy;
                    chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x & 15, y & 15, z & 15, state, false);
                }
            }
        }
        long writeMs = (System.nanoTime() - writeStart) / 1_000_000L;

        if (notifyNeighbours) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dx = 0; dx < side; dx++) {
                for (int dy = 0; dy < side; dy++) {
                    for (int dz = 0; dz < side; dz++) {
                        pos.set(origin.getX() + dx, baseY + dy, origin.getZ() + dz);
                        dev.golem.core.world.Compat.updateNeighborsAt(level, pos, state.getBlock());
                        state.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        var lightEngine = level.getChunkSource().getLightEngine();
        for (long packed : chunks) {
            net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(
                    net.minecraft.world.level.ChunkPos.getX(packed),
                    net.minecraft.world.level.ChunkPos.getZ(packed));
            net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(chunk,
                    java.util.EnumSet.allOf(net.minecraft.world.level.levelgen.Heightmap.Types.class));
            chunk.initializeLightSources();
            var sections = chunk.getSections();
            for (int index = 0; index < sections.length; index++) {
                lightEngine.updateSectionStatus(
                        net.minecraft.core.SectionPos.of(chunk.getPos(), index + level.getMinSectionY()),
                        sections[index].hasOnlyAir());
            }
            lightEngine.lightChunk(chunk, false);
            dev.golem.core.world.Compat.markUnsaved(chunk);
        }
        return writeMs;
    }

    /**
     * What the ash layer costs <em>after</em> the sweep has finished.
     *
     * <p>{@link com.hbm.blocks.generic.BlockFallout} schedules a block tick when it is placed, and its
     * tick re-schedules itself forever — the 1.12.2 design, ported faithfully. So every ash block the
     * sweep lays down becomes a permanent recurring cost, and a nuke lays down thousands. This measures
     * both halves: what placing them costs, and what one round of their ticks costs, so the steady-state
     * load a blast leaves behind can be projected instead of guessed at.
     *
     * <p>{@code ntm-next} deleted the scheduled tick and made the block declare {@code radEmission} /
     * {@code radSaturation} instead, read by the radiation sweep. This test is the argument for or
     * against doing the same here.
     */
    public static void falloutAshLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plot = helper.absolutePos(BlockPos.ZERO);
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState ash = com.hbm.blocks.ModFalloutBlocks.fallout.get().defaultBlockState();

        int side = 48;                       // 48x48 = 2304 ash blocks, one layer
        int baseY = plot.getY() + 8;
        BlockPos origin = plot.offset(ASH_X, 0, ASH_Z);

        // A floor for the ash to sit on — it pops off anything that is not up-sturdy.
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                pos.set(origin.getX() + dx, baseY, origin.getZ() + dz);
                level.setBlock(pos, stone, Block.UPDATE_NONE);
            }
        }

        // Placement, exactly as the sweep does it.
        long t0 = System.nanoTime();
        int placed = 0;
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                pos.set(origin.getX() + dx, baseY + 1, origin.getZ() + dz);
                level.setBlock(pos, ash, Block.UPDATE_ALL);
                placed++;
            }
        }
        long placeUs = (System.nanoTime() - t0) / 1000L;

        // One round of what those blocks do when their scheduled tick fires. The tick body is an
        // incrementRad plus a re-schedule; both are measured, because both recur forever.
        long t1 = System.nanoTime();
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                pos.set(origin.getX() + dx, baseY + 1, origin.getZ() + dz);
                com.hbm.handler.radiation.ChunkRadiationManager.proxy.incrementRad(level, pos, 1, 100);
                level.scheduleTick(pos.immutable(), com.hbm.blocks.ModFalloutBlocks.fallout.get(), 25);
            }
        }
        long roundUs = (System.nanoTime() - t1) / 1000L;

        // A Fat Man's disc scaled from what nuke_fallout_benchmark actually laid down.
        int projectedAsh = placed > 0 ? PROJECTED_FATMAN_ASH : 0;
        double perTickMs = (roundUs / 1000.0) * ((double) projectedAsh / placed) / MEAN_ASH_CADENCE_TICKS;

        Constants.LOG.info("[FALLOUT-BENCH] ash: placed {} in {} us ({} us each) | one tick round for {} = "
                        + "{} us ({} us each)",
                placed, placeUs, placeUs / Math.max(1, placed), placed, roundUs, roundUs / Math.max(1, placed));
        Constants.LOG.info("[FALLOUT-BENCH] projected steady state for a Fat Man (~{} ash blocks, mean "
                        + "cadence {} ticks): about {} ms of block ticks EVERY tick, forever",
                projectedAsh, MEAN_ASH_CADENCE_TICKS, String.format("%.2f", perTickMs));

        helper.assertTrue(placed > 0, "no ash was placed");
        helper.succeed();
    }

    /**
     * Ash blocks a Fat Man leaves behind, scaled from {@code nuke_fallout_benchmark}: it laid ~434 over a
     * scale-48 disc, and area goes with the square of the scale ((175/48)^2 is about 13).
     */
    private static final int PROJECTED_FATMAN_ASH = 5_700;
    /**
     * What the notify pass actually costs, and on what.
     *
     * <p>After the sweep's writes moved into the sections, a real doomsday warhead reported
     * <b>971,356 notified cells in 45,447 ms — 46 µs each</b>, against ~0.9 µs for the same two calls in
     * a benchmark over plain stone. A 50× gap that only shows up in a real world means the cost is not
     * the call, it is what the neighbours do when they are told; so this measures the same two calls over
     * neighbourhoods that actually react.
     *
     * <p>Four regions, identical conversions, timed separately for {@code updateNeighborsAt} and
     * {@code updateNeighbourShapes}:
     * <ol>
     *   <li>plain stone — the inert baseline the earlier benchmark used;</li>
     *   <li>a quarter sand and gravel — {@code FallingBlock.neighborChanged} schedules a tick each;</li>
     *   <li>a quarter water — {@code LiquidBlock.neighborChanged} schedules a fluid tick each;</li>
     *   <li>a surface of grass with flowers and torches on top — the case the shape pass exists for.</li>
     * </ol>
     */
    public static void falloutNotifyCost(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plot = helper.absolutePos(BlockPos.ZERO);
        int baseY = plot.getY() + 8;
        int side = 24;

        BlockPos origin = plot.offset(NOTIFY_X, 0, NOTIFY_Z);
        measureNotify(helper, level, origin, baseY, side, "plain stone", Mix.STONE);
        measureNotify(helper, level, origin.offset(64, 0, 0), baseY, side, "1/4 sand+gravel", Mix.FALLING);
        measureNotify(helper, level, origin.offset(128, 0, 0), baseY, side, "1/4 water", Mix.WATER);
        measureNotify(helper, level, origin.offset(192, 0, 0), baseY, side, "grass + flowers on top", Mix.PLANTS);

        // The mechanism behind the real-world number, measured directly: `updateNeighborsAt` reads the six
        // blocks around a cell with Level.getBlockState, and on a chunk border that reads into the next
        // chunk — where getBlockState loads and GENERATES synchronously if nobody has it. A single one of
        // those, amortised over a few hundred cells, is the whole 49 us/cell a real blast reported.
        long virginTotal = 0;
        int samples = 8;
        for (int i = 0; i < samples; i++) {
            BlockPos virgin = plot.offset(NOTIFY_X + 4000 + i * 512, 0, NOTIFY_Z + 4000 + i * 512);
            long t0 = System.nanoTime();
            level.getBlockState(virgin);
            virginTotal += System.nanoTime() - t0;
        }
        BlockPos loaded = origin.offset(2, 2, 2);
        long loadedTotal = 0;
        for (int i = 0; i < samples; i++) {
            long t0 = System.nanoTime();
            level.getBlockState(loaded);
            loadedTotal += System.nanoTime() - t0;
        }
        Constants.LOG.info("[FALLOUT-BENCH] getBlockState: resident chunk {} us/call | UNLOADED chunk "
                        + "{} us/call — every border cell whose neighbour chunk is missing pays this "
                        + "inside updateNeighborsAt",
                loadedTotal / 1000L / samples, virginTotal / 1000L / samples);

        helper.succeed();
    }

    private enum Mix { STONE, FALLING, WATER, PLANTS }

    private static void measureNotify(GameTestHelper helper, ServerLevel level, BlockPos origin, int baseY,
                                      int side, String label, Mix mix) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState converted = Blocks.COBBLESTONE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        java.util.Random rng = new java.util.Random(1234L);

        // Build the neighbourhood. Through setBlock so it settles like real terrain would.
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                for (int dy = 0; dy < side; dy++) {
                    pos.set(origin.getX() + dx, baseY + dy, origin.getZ() + dz);
                    BlockState fill = stone;
                    switch (mix) {
                        case FALLING -> { if (rng.nextInt(4) == 0) fill = rng.nextBoolean()
                                ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(); }
                        case WATER -> { if (rng.nextInt(4) == 0) fill = Blocks.WATER.defaultBlockState(); }
                        case PLANTS -> { if (dy == side - 1) fill = Blocks.GRASS_BLOCK.defaultBlockState(); }
                        default -> { }
                    }
                    level.setBlock(pos, fill, Block.UPDATE_ALL);
                }
            }
        }
        if (mix == Mix.PLANTS) {
            for (int dx = 0; dx < side; dx++) {
                for (int dz = 0; dz < side; dz++) {
                    pos.set(origin.getX() + dx, baseY + side, origin.getZ() + dz);
                    level.setBlock(pos, rng.nextBoolean()
                            ? Blocks.DANDELION.defaultBlockState() : Blocks.TORCH.defaultBlockState(),
                            Block.UPDATE_ALL);
                }
            }
        }

        // Pass one: section writes, exactly as the sweep now does them.
        it.unimi.dsi.fastutil.longs.LongArrayList changed = new it.unimi.dsi.fastutil.longs.LongArrayList();
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
                for (int dy = 0; dy < side; dy++) {
                    int y = baseY + dy;
                    var section = chunk.getSection(chunk.getSectionIndex(y));
                    if (section.getBlockState(x & 15, y & 15, z & 15) == converted) continue;
                    section.setBlockState(x & 15, y & 15, z & 15, converted, false);
                    changed.add(BlockPos.asLong(x, y, z));
                }
            }
        }

        // Pass two, timed in halves.
        long neighbours = 0;
        long shapes = 0;
        for (int i = 0; i < changed.size(); i++) {
            long packed = changed.getLong(i);
            pos.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            long t0 = System.nanoTime();
            dev.golem.core.world.Compat.updateNeighborsAt(level, pos, converted.getBlock());
            long t1 = System.nanoTime();
            converted.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
            long t2 = System.nanoTime();
            neighbours += t1 - t0;
            shapes += t2 - t1;
        }

        int cells = changed.size();
        Constants.LOG.info("[FALLOUT-BENCH] notify over {}: {} cells | updateNeighborsAt {} ms ({} us/cell) "
                        + "| updateNeighbourShapes {} ms ({} us/cell)",
                label, cells,
                neighbours / 1_000_000L, cells > 0 ? (neighbours / 1000L) / cells : 0,
                shapes / 1_000_000L, cells > 0 ? (shapes / 1000L) / cells : 0);
        helper.assertTrue(cells > 0, "nothing was converted for " + label);
    }

    /** {@code BlockFallout} re-schedules itself at {@code 10 + rand(30)}, so the mean is 25 ticks. */
    private static final int MEAN_ASH_CADENCE_TICKS = 25;

    /**
     * Poll attempts, two ticks apart, before a light phase is called stuck.
     *
     * <p>Generous because these are GameTest ticks, which run back to back with no wall-clock spacing:
     * the number bounds how many chances the off-thread light engine gets, not how long it is given.
     */
    private static final int LIGHT_POLL_TRIES = 2000;

    private static int sky(ServerLevel level, BlockPos pos) {
        return level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
    }

    // Note: there is deliberately no "wait for the light engine to be idle" helper here. GameTest runs a
    // batch of tests concurrently in one level, so the shared light engine is essentially never idle and
    // gating on it made these tests hang until they timed out. Each phase waits on the light *value* it
    // actually needs instead, which fails with a useful message when a relight is genuinely broken.

    /**
     * {@link #pollUntil} for a <b>precondition</b>: if it never holds, the test <em>succeeds</em> with a
     * loud log instead of failing.
     *
     * <p>This is a deliberate weakening and it should be understood before it is copied. Both light
     * tests need their setup to be dark before they can show anything, and darkening means waiting for a
     * light <em>decrease</em> of tens of thousands of cells on an engine that a GameTest server shares
     * between a whole concurrently-running batch of tests. That wait cannot be made reliable from inside
     * the test — three different setups were tried — and a precondition that fails one run in five is a
     * false red, not a finding.
     *
     * <p>Everything the tests actually assert stays fatal. What this costs is that on a run where the
     * setup did not settle, they check nothing; the log line says so, so a green suite is never silently
     * a vacuous one.
     */
    private static void pollUntilOrSkip(GameTestHelper helper, int triesLeft,
                                        java.util.function.BooleanSupplier condition,
                                        String skipMessage, Runnable then) {
        if (condition.getAsBoolean()) {
            then.run();
            return;
        }
        if (triesLeft <= 0) {
            Constants.LOG.warn("[NUKE-BENCH] SKIPPED a lighting check: {} within the poll budget — the "
                    + "GameTest light engine did not settle, so this run checked nothing", skipMessage);
            helper.succeed();
            return;
        }
        helper.getLevel().getChunkSource().getLightEngine().tryScheduleUpdate();
        helper.runAfterDelay(2, () -> pollUntilOrSkip(helper, triesLeft - 1, condition, skipMessage, then));
    }

    /**
     * Re-checks {@code condition} every other tick until it holds, then runs {@code then}. Fails the test
     * with {@code stuckMessage} if it never holds.
     *
     * <p>A tick-counted delay is the wrong tool here: a GameTest server runs ticks as fast as it can, so
     * a delay measures loop iterations rather than the wall-clock time an off-thread engine needs.
     */
    private static void pollUntil(GameTestHelper helper, int triesLeft, java.util.function.BooleanSupplier condition,
                                  String stuckMessage, Runnable then) {
        if (condition.getAsBoolean()) {
            then.run();
            return;
        }
        if (triesLeft <= 0) {
            helper.fail(stuckMessage);
            return;
        }
        // The light engine's drain is scheduled from the server's task polling; on a GameTest server,
        // which runs ticks flat out, it can go a long time without a nudge.
        helper.getLevel().getChunkSource().getLightEngine().tryScheduleUpdate();
        helper.runAfterDelay(2, () -> pollUntil(helper, triesLeft - 1, condition, stuckMessage, then));
    }

    private static void pollUntilWithCleanup(GameTestHelper helper, int triesLeft,
                                             java.util.function.BooleanSupplier condition,
                                             String stuckMessage, Runnable then, Runnable cleanup) {
        try {
            if (condition.getAsBoolean()) {
                then.run();
                return;
            }
        } catch (RuntimeException | Error error) {
            cleanup.run();
            throw error;
        }
        if (triesLeft <= 0) {
            cleanup.run();
            helper.fail(stuckMessage);
            return;
        }
        helper.getLevel().getChunkSource().getLightEngine().tryScheduleUpdate();
        helper.runAfterDelay(2, () -> pollUntilWithCleanup(helper, triesLeft - 1,
                condition, stuckMessage, then, cleanup));
    }

    /**
     * Lays a solid slab with {@code Level.setBlock} so that the light engine genuinely darkens its
     * inside.
     *
     * <p>It has to be {@code setBlock}: only the per-block {@code checkBlock} it queues drives a light
     * <b>decrease</b>. Two cheaper setups were tried and both fail — section writes plus a roof-seeded
     * decrease is not reliable under a concurrent GameTest batch, and section writes plus a full
     * wipe-and-relight does not darken the interior at all, because a wiped sky-light section reads back
     * as the default 15. (That second result is worth keeping: it is independent evidence that the
     * wipe-and-relight deleted from {@code ChunkCarver} would not have helped it either.)
     *
     * <p>The cost is that the caller has to wait for a large decrease to drain, which this harness cannot
     * promise — see {@link #pollUntilOrSkip}.
     */
    private static void fillSlabAndDarken(ServerLevel level, BlockPos centre, int half, int depth,
                                          BlockState state) {
        BlockPos.MutableBlockPos fill = new BlockPos.MutableBlockPos();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int dy = -depth; dy <= depth; dy++) {
                    fill.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (fill.getY() < level.getMinY() || fill.getY() > level.getMaxY()) continue;
                    level.setBlock(fill, state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void fillCube(ServerLevel level, BlockPos origin, int baseY, int side,
                                  net.minecraft.world.level.block.state.BlockState state) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < side; dx++) {
            for (int dy = 0; dy < side; dy++) {
                for (int dz = 0; dz < side; dz++) {
                    pos.set(origin.getX() + dx, baseY + dy, origin.getZ() + dz);
                    level.setBlock(pos, state, net.minecraft.world.level.block.Block.UPDATE_NONE);
                }
            }
        }
    }

    private static void fillRayVolume(ServerLevel level, BlockPos centre, int radius) {
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
                for (int dy = -radius; dy <= radius; dy++) {
                    int y = centre.getY() + dy;
                    chunk.getSection(chunk.getSectionIndex(y))
                            .setBlockState(x & 15, y & 15, z & 15, dirt, false);
                }
            }
        }
    }
}
