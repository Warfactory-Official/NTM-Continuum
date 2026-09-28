package com.hbm.gametest;

import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.bomb.BlockVolcano;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.tileentity.TileEntityVolcanoCore;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The eruption 1.7 {@code BlockVolcano.TileEntityVolcanoCore} runs, checked the way the original grows: a
 * growing core floods its 3x3x3 shell every tenth tick, sits still until its 288th tick, then places itself
 * one block up and leaves lava where it stood; its blobs pool lava and monoxide where they land; and the
 * channel blast refills every destroyed full cube with volcanic lava. Bodies live here, the catalogue rows
 * in {@link HbmGameTestFunctions}.
 */
public final class VolcanoGameTests {

    private VolcanoGameTests() {}

    /** Room for the radius-7 channel blast and the 3x3x3 shell on every side. */
    private static final BlockPos CORE = new BlockPos(4, 4, 4);

    /** The last tick at which the core must still stand where it was planted: 1.7 grows at 288, never before. */
    private static final int STILL_THERE = TileEntityVolcanoCore.GROWING_UPDATE_RATE - 20;
    /** Polling deadline for the climb: the tile's first tick may land one server tick after the placement. */
    private static final int GROWTH_DEADLINE = TileEntityVolcanoCore.GROWING_UPDATE_RATE + 30;

    private static final BlockPos SHARD_FLOOR = new BlockPos(1, 1, 1);
    private static final BlockPos PLAIN_FLOOR = new BlockPos(7, 1, 1);
    /** A shard dropped from here at -0.5/t reaches the floor on its seventh tick, past the five-tick grace, at about -0.66/t. */
    // High enough that the impact lands well past EntityShrapnel.GRACE_TICKS for both shards.
    private static final double DROP_HEIGHT = 8.0D;

    private static final BlockPos BLAST = new BlockPos(4, 4, 4);
    /**
     * A ray pays {@code (resistance + 0.3) * 0.3} per 0.3-block step, so about 2 power per step through stone
     * against 0.225 through air: a size-6 blast reaches every block of a shell one cell out, and stops there.
     */
    private static final float BLAST_SIZE = 6F;

    // ------------------------------------------------------------------ the growing core

    /**
     * volcano_core_growing_active: shell flooded by the first cycle, core unmoved up to tick 268, core one block
     * higher (fresh timer) over lava by tick ~290. Update rates of the other metas are checked on the way in.
     */
    public static void growingCoreClimbs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // 1.7 getUpdateRate per meta, read off real block entities before the eruption starts
        assertUpdateRate(helper, "volcano_core", 10);
        assertUpdateRate(helper, "volcano_core_static_extinguishing", 60 * 60 * 20);
        assertUpdateRate(helper, "volcano_core_smoldering", 10);
        assertUpdateRate(helper, "volcano_rad_core_growing_extinguishing", TileEntityVolcanoCore.GROWING_UPDATE_RATE);

        BlockVolcano growing = ModTerrainBlocks.volcanoCore("volcano_core_growing_active");
        helper.setBlock(CORE, growing);
        TileEntityVolcanoCore core = GameTestHelpers.blockEntity(helper, CORE, TileEntityVolcanoCore.class);
        helper.assertTrue(core.getUpdateRate() == TileEntityVolcanoCore.GROWING_UPDATE_RATE,
                "a growing core must climb every " + TileEntityVolcanoCore.GROWING_UPDATE_RATE + " ticks (1.7: 250x per hour)");
        helper.assertTrue(!core.isRadioactive(), "volcano_core_growing_active is the plain volcanic core");

        List<BlockPos> preLit = shellAround(CORE).stream()
                .filter(p -> !level.getBlockState(helper.absolutePos(p)).isAir()).toList();
        helper.assertTrue(preLit.isEmpty(), "instrument: the shell was not empty before the core ticked (" + preLit + ")");

        // 1.7 surroundLava: every tenth tick the 3x3x3 around the core is volcanic lava
        helper.runAfterDelay(TileEntityVolcanoCore.CYCLE + 2, () -> extinguishOnFailure(helper, level, () -> {
            List<BlockPos> dry = unflooded(helper, level, CORE);
            helper.assertTrue(dry.isEmpty(), "1.7 surroundLava: the 3x3x3 shell must be volcanic lava after the first cycle; dry at " + dry);
            helper.assertTrue(level.getBlockState(helper.absolutePos(CORE)).is(growing),
                    "the core must survive its own channel blast (resistance 10,000)");
        }));

        // not one tick early: the core is still where it was planted shortly before the climb
        helper.runAfterDelay(STILL_THERE, () -> extinguishOnFailure(helper, level, () -> {
            helper.assertTrue(level.getBlockState(helper.absolutePos(CORE)).is(growing),
                    "1.7 grows only when volcanoTimer reaches " + TileEntityVolcanoCore.GROWING_UPDATE_RATE
                            + "; the core had already left " + CORE + " at tick " + STILL_THERE);
            helper.assertTrue(isLava(helper, level, CORE.above()),
                    "the cell the core will climb into must be shell lava, not another core, before the climb");
        }));

        // 1.7: setBlock(y + 1, same block + meta) then setBlock(y, lava)
        poll(helper, STILL_THERE + 2, GROWTH_DEADLINE,
                () -> level.getBlockState(helper.absolutePos(CORE.above())).is(growing),
                () -> "1.7 shouldGrow: a growing core below y 200 must place itself one block up on its "
                        + TileEntityVolcanoCore.GROWING_UPDATE_RATE + "th tick; " + CORE.above() + " is "
                        + level.getBlockState(helper.absolutePos(CORE.above())) + " at the deadline",
                () -> {
                    List<String> broken = new ArrayList<>();
                    check(broken, isLava(helper, level, CORE),
                            "the cell the core left must be volcanic lava (1.7: setBlock(x, y, z, getLava())); it is "
                                    + level.getBlockState(helper.absolutePos(CORE)));
                    TileEntityVolcanoCore climbed = GameTestHelpers.blockEntity(helper, CORE.above(), TileEntityVolcanoCore.class);
                    check(broken, climbed.getUpdateRate() == TileEntityVolcanoCore.GROWING_UPDATE_RATE,
                            "the climbed core keeps its meta: it must still be a growing core");
                    check(broken, climbed.volcanoTimer < 40,
                            "the climbed core is a fresh tile (1.7 creates a new TileEntity with timer 0); it reports "
                                    + climbed.volcanoTimer);
                    check(broken, level.getBlockEntity(helper.absolutePos(CORE)) == null,
                            "the old core's tile must be gone from " + CORE);
                    cleanup(helper, level, CORE);
                    helper.assertTrue(broken.isEmpty(), "volcano growth is wrong (" + broken.size() + " failures): "
                            + String.join(" | ", broken));
                    helper.succeed();
                });
    }

    private static void assertUpdateRate(GameTestHelper helper, String id, int expected) {
        helper.setBlock(CORE, ModTerrainBlocks.volcanoCore(id));
        TileEntityVolcanoCore core = GameTestHelpers.blockEntity(helper, CORE, TileEntityVolcanoCore.class);
        helper.assertTrue(core.getUpdateRate() == expected,
                id + ": 1.7 getUpdateRate is " + expected + ", the port says " + core.getUpdateRate());
        helper.setBlock(CORE, Blocks.AIR);
    }

    // ------------------------------------------------------------------ the blobs

    /** 1.7 EntityShrapnel dataWatcher 2: a falling volcano shard pools lava and outgasses; a plain one only fizzes. */
    public static void volcanoShrapnelPoolsLavaAndMonoxide(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockPos shardOrigin = helper.absolutePos(SHARD_FLOOR);
        int targetChunkX = shardOrigin.getX() >> 4;
        int targetChunkZ = shardOrigin.getZ() >> 4;
        boolean[][] forcedByTest = new boolean[3][3];
        // A GameTest plot can remain loaded while its playerless entity section is outside the
        // DistanceManager's ticking range. Keep the shard flight and delayed death assertion on
        // real server ticks; release only simulation tickets this fixture added.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                forcedByTest[dx + 1][dz + 1] = level.setChunkForced(targetChunkX + dx, targetChunkZ + dz, true);
            }
        }

        helper.setBlock(SHARD_FLOOR, Blocks.STONE);
        helper.setBlock(PLAIN_FLOOR, Blocks.STONE);
        EntityShrapnel volcanoShard = drop(helper, level, SHARD_FLOOR, true);
        EntityShrapnel plainShard = drop(helper, level, PLAIN_FLOOR, false);

        poll(helper, EntityShrapnel.GRACE_TICKS + 2, 50,
                () -> isLava(helper, level, SHARD_FLOOR.above()),
                () -> {
                    String failure = "1.7 EntityShrapnel.onImpact: a volcano shard landing at motionY < -0.2 lays volcanic_lava_block on "
                            + "top of the block it hit; " + SHARD_FLOOR.above() + " is "
                            + level.getBlockState(helper.absolutePos(SHARD_FLOOR.above()))
                            + "; volcano ticks=" + volcanoShard.tickCount + ", removed=" + volcanoShard.isRemoved()
                            + ", position=" + volcanoShard.position();
                    volcanoShard.discard();
                    plainShard.discard();
                    releaseSimulationChunks(level, targetChunkX, targetChunkZ, forcedByTest);
                    return failure;
                },
                () -> {
                    List<String> broken = new ArrayList<>();
                    Block monoxide = ModNuclearBlocks.gas_monoxide.get();

                    long gassed = around(SHARD_FLOOR).stream()
                            .filter(p -> level.getBlockState(helper.absolutePos(p)).is(monoxide)).count();
                    check(broken, gassed > 0, "the same arm fills the 3x3x3 from the hit block up two with gas_monoxide; "
                            + "none of " + around(SHARD_FLOOR).size() + " cells carries any");

                    check(broken, !isLava(helper, level, PLAIN_FLOOR.above()),
                            "control: a plain shard (trail 0) takes the lava-puff arm and must lay no lava");
                    check(broken, around(PLAIN_FLOOR).stream()
                                    .noneMatch(p -> level.getBlockState(helper.absolutePos(p)).is(monoxide)),
                            "control: a plain shard must outgas nothing");
                    if (!broken.isEmpty()) {
                        volcanoShard.discard();
                                plainShard.discard();
                        releaseSimulationChunks(level, targetChunkX, targetChunkZ, forcedByTest);
                        helper.fail("volcano shrapnel is wrong (" + broken.size() + " failures): "
                                + String.join(" | ", broken));
                        return;
                    }
                    // Both shards die on impact (1.7: setDead past the 5-tick grace); the control shard may land a
                    // tick or two apart from the volcano one, so wait for it rather than asserting the same tick.
                    poll(helper, 1, 20,
                            () -> volcanoShard.isRemoved() && plainShard.isRemoved(),
                            () -> {
                                volcanoShard.discard();
                                plainShard.discard();
                                releaseSimulationChunks(level, targetChunkX, targetChunkZ, forcedByTest);
                                return "both shards must be dead after their impact (1.7: setDead past the 5-tick grace)"
                                        + "; volcano ticks=" + volcanoShard.tickCount + ", plain ticks=" + plainShard.tickCount;
                            },
                            () -> {
                                volcanoShard.discard();
                                plainShard.discard();
                                releaseSimulationChunks(level, targetChunkX, targetChunkZ, forcedByTest);
                                helper.succeed();
                            });
                });
    }

    private static EntityShrapnel drop(GameTestHelper helper, ServerLevel level, BlockPos floor, boolean volcano) {
        Vec3 top = helper.absoluteVec(Vec3.atCenterOf(floor)).add(0D, 0.5D + DROP_HEIGHT, 0D);
        EntityShrapnel shard = new EntityShrapnel(level, top.x, top.y, top.z);
        shard.setDeltaMovement(0D, -0.5D, 0D);
        shard.setVolcano(volcano);
        level.addFreshEntity(shard);
        return shard;
    }

    // ------------------------------------------------------------------ the blast

    /** 1.7 ExplosionNT LAVA_V: every destroyed normal cube becomes volcanic lava; a non-cube is emptied and left. */
    public static void lavaBlastFillsDestroyedCubesOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> broken = new ArrayList<>();
        Block lava = ModFluidNetBlocks.volcanic_lava_block.get();
        List<BlockPos> field = shellAround(BLAST);

        for (BlockPos pos : field) helper.setBlock(pos, Blocks.STONE);
        BlockPos glass = field.get(0);
        helper.setBlock(glass, Blocks.GLASS);
        Vec3 c = helper.absoluteVec(Vec3.atCenterOf(BLAST));
        TileEntityVolcanoCore.lavaBlast(level, lava, c.x, c.y, c.z, BLAST_SIZE);

        List<BlockPos> missed = field.stream().filter(p -> !p.equals(glass)).filter(p -> !isLava(helper, level, p)).toList();
        check(broken, missed.isEmpty(), "1.7 ExplosionNT.doExplosionB: every destroyed normal cube becomes volcanic lava; "
                + missed + " came out as something else");
        BlockState atGlass = level.getBlockState(helper.absolutePos(glass));
        check(broken, atGlass.isAir(), "1.7 gates the lava write on isNormalCube(): the glass cell must be emptied and "
                + "never filled; it is " + atGlass);

        helper.assertTrue(broken.isEmpty(), "the LAVA_V blast is wrong (" + broken.size() + " failures): "
                + String.join(" | ", broken));
        helper.succeed();
    }

    // ------------------------------------------------------------------ support

    private static void poll(GameTestHelper helper, int from, int deadline, BooleanSupplier condition,
                             Supplier<String> stuck, Runnable then) {
        helper.runAfterDelay(from, () -> pollStep(helper, deadline - from, condition, stuck, then));
    }

    private static void pollStep(GameTestHelper helper, int ticksLeft, BooleanSupplier condition,
                                 Supplier<String> stuck, Runnable then) {
        if (condition.getAsBoolean()) {
            extinguishOnFailure(helper, helper.getLevel(), then);
            return;
        }
        if (ticksLeft <= 0) {
            String message = stuck.get();
            extinguish(helper, helper.getLevel(), CORE);
            helper.fail(message);
            return;
        }
        helper.runAfterDelay(1, () -> pollStep(helper, ticksLeft - 1, condition, stuck, then));
    }

    private static void cleanup(GameTestHelper helper, ServerLevel level, BlockPos core) {
        extinguish(helper, level, core);
    }

    /**
     * Puts the eruption out: every volcano core in the column the growing core climbs is removed, and its blobs
     * with it. A core left behind keeps erupting in the shared GameTest world after its test has finished —
     * channel blasts every ten ticks, a climb every 288, lava blobs landing on other plots — and on 2026-09-24
     * that starved the rest of the suite (the server thread sat in its blasts for minutes, and the later
     * batches never got their chunks ticking).
     */
    private static void extinguish(GameTestHelper helper, ServerLevel level, BlockPos core) {
        for (int dy = -1; dy <= 8; dy++) {
            BlockPos at = helper.absolutePos(core.above(dy));
            if (level.getBlockEntity(at) instanceof TileEntityVolcanoCore) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        level.getEntitiesOfClass(EntityShrapnel.class, new AABB(helper.absolutePos(core)).inflate(64))
                .forEach(Entity::discard);
    }

    /** Runs a delayed check; a failing check still puts the eruption out before the test is marked failed. */
    private static void extinguishOnFailure(GameTestHelper helper, ServerLevel level, Runnable check) {
        try {
            check.run();
        } catch (RuntimeException | Error failure) {
            extinguish(helper, level, CORE);
            throw failure;
        }
    }

    private static void releaseSimulationChunks(ServerLevel level, int centerChunkX, int centerChunkZ,
                                                boolean[][] forcedByTest) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (forcedByTest[dx + 1][dz + 1]) {
                    level.setChunkForced(centerChunkX + dx, centerChunkZ + dz, false);
                }
            }
        }
    }

    private static boolean isLava(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        return level.getBlockState(helper.absolutePos(pos)).is(ModFluidNetBlocks.volcanic_lava_block.get());
    }

    private static List<BlockPos> unflooded(GameTestHelper helper, ServerLevel level, BlockPos core) {
        return shellAround(core).stream().filter(p -> !isLava(helper, level, p)).toList();
    }

    private static List<BlockPos> shellAround(BlockPos centre) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            if (x != 0 || y != 0 || z != 0) out.add(centre.offset(x, y, z));
        }
        return out;
    }

    private static List<BlockPos> around(BlockPos hit) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = -1; z <= 1; z++) out.add(hit.offset(x, y, z));
        return out;
    }

    private static void check(List<String> broken, boolean ok, String whatBreaksIfNot) {
        if (!ok) broken.add(whatBreaksIfNot);
    }
}
