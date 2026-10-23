package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.handler.radiation.RadiationPayload;
import com.hbm.handler.radiation.RadiationSystemNT;
import dev.golem.core.gametest.GameTests;
import com.hbm.blocks.ModHazardBlocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.blocks.ModDoorBlocks;
import com.hbm.blocks.ModGlassBlocks;
import com.hbm.blocks.ModDecoContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Radiation's trip to disk and back. Every other radiation test reads the live field, which is the half
 * that cannot tell a working store from no store at all.
 */
public final class RadiationPersistenceGameTests {

    private RadiationPersistenceGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function PAYLOAD_ROUND_TRIP = TESTS.function(
            "radiation_payload_round_trips", 20, () -> RadiationPersistenceGameTests::payloadRoundTrip);

    public static final GameTests.Function PAYLOAD_REJECTS_DAMAGE = TESTS.function(
            "radiation_payload_rejects_a_damaged_one", 20,
            () -> RadiationPersistenceGameTests::payloadRejectsDamage);

    public static final GameTests.Function SURVIVES_ROUND_TRIP = TESTS.function(
            "radiation_survives_an_untouched_chunk_round_trip", 60,
            () -> RadiationPersistenceGameTests::radiationSurvivesAnUntouchedRoundTrip);

    public static final GameTests.Function LEAD_WALL = TESTS.function(
            "radiation_lead_box_holds", 200, 8, () -> h -> boxHolds(h, ModHazardBlocks.block_lead.get()));
    public static final GameTests.Function CONCRETE_WALL = TESTS.function(
            "radiation_concrete_box_holds", 200, 8, () -> h -> boxHolds(h, ModHazardBlocks.brick_concrete.get()));

    public static final GameTests.Function PILLAR_WALL = TESTS.function(
            "radiation_pillar_box_holds", 200, 8, () -> h -> boxHolds(h, ModDecoContent.concrete_pillar.get()));
    public static final GameTests.Function GLASS_WALL_REBUILD = TESTS.function(
            "radiation_glass_box_rebuilds", 200, 8, () -> RadiationPersistenceGameTests::glassBoxRebuilds);
    public static final GameTests.Function DOOR_SEALS_CLOSED = TESTS.function(
            "radiation_fire_door_seals_closed", 20, 16, () -> RadiationPersistenceGameTests::fireDoorSealsClosed);

    public static void init() {}

    private static final double TOLERANCE = 1.0e-6D;

    private static void payloadRoundTrip(GameTestHelper helper) {
        double[][] sections = new double[6][];
        sections[0] = new double[] {0D, 1.5D, 4096D};
        sections[3] = new double[] {};
        sections[5] = new double[] {Double.MIN_VALUE, 1D / 3D, Double.MAX_VALUE / 2D};

        double[][] back = RadiationPayload.decode(RadiationPayload.encode(sections));
        helper.assertTrue(back != null, "a payload this encoder wrote must decode");
        helper.assertTrue(back.length == 6, "the highest stored section must set the length, got " + back.length);
        helper.assertTrue(back[1] == null && back[2] == null && back[4] == null,
                "sections that stored nothing must stay absent rather than becoming empty rows");

        for (int i = 0; i < sections.length; i++) {
            if (sections[i] == null) continue;
            helper.assertTrue(back[i] != null && back[i].length == sections[i].length,
                    "section " + i + " must come back with its own pocket count");
            for (int p = 0; p < sections[i].length; p++) {
                helper.assertTrue(back[i][p] == sections[i][p],
                        "section " + i + " pocket " + p + " must come back bit for bit, got " + back[i][p]);
            }
        }

        helper.assertTrue(RadiationPayload.encode(new double[4][]) == null,
                "a chunk with nothing to store must produce no payload at all");
        helper.succeed();
    }

    private static void payloadRejectsDamage(GameTestHelper helper) {
        double[][] sections = new double[1][];
        sections[0] = new double[] {1D, 2D, 3D};
        byte[] good = RadiationPayload.encode(sections);

        byte[] truncated = new byte[good.length - 4];
        System.arraycopy(good, 0, truncated, 0, truncated.length);
        helper.assertTrue(RadiationPayload.decode(truncated) == null, "a truncated payload must read as absent");

        byte[] foreign = good.clone();
        foreign[0] = 99;
        helper.assertTrue(RadiationPayload.decode(foreign) == null,
                "a payload in a format this version does not know must read as absent, not be misread as this one");

        byte[] absurd = good.clone();
        absurd[1] = 0x7F;
        absurd[2] = 0x7F;
        absurd[3] = 0x7F;
        absurd[4] = 0x7F;
        helper.assertTrue(RadiationPayload.decode(absurd) == null,
                "a payload claiming more sections than a chunk has must be refused before it sizes anything");

        helper.assertTrue(RadiationPayload.decode(null) == null && RadiationPayload.decode(new byte[0]) == null,
                "no payload must read as no radiation");
        helper.succeed();
    }

    /**
     * The chunk in the middle is the point: it arrives and leaves without anything reading its radiation.
     * Storing what such a chunk holds live would store nothing, and overwrite what it actually has.
     */
    private static void radiationSurvivesAnUntouchedRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos low = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos high = low.above(18);
        LevelChunk chunk = level.getChunk(low.getX() >> 4, low.getZ() >> 4);

        double restoreLow = RadiationSystemNT.getRadForCoord(level, low);
        double restoreHigh = RadiationSystemNT.getRadForCoord(level, high);
        try {
            RadiationSystemNT.setRadForCoord(level, low, 64D);
            RadiationSystemNT.setRadForCoord(level, high, 7D);

            cycle(level, chunk);
            cycle(level, chunk);

            double low2 = RadiationSystemNT.getRadForCoord(level, low);
            double high2 = RadiationSystemNT.getRadForCoord(level, high);
            helper.assertTrue(Math.abs(low2 - 64D) < TOLERANCE,
                    "stored radiation must come back where it was written, got " + low2);
            helper.assertTrue(Math.abs(high2 - 7D) < TOLERANCE,
                    "a second section must come back as itself rather than as its neighbour, got " + high2);
        } finally {
            RadiationSystemNT.setRadForCoord(level, low, restoreLow);
            RadiationSystemNT.setRadForCoord(level, high, restoreHigh);
        }
        helper.succeed();
    }

    private static void cycle(ServerLevel level, LevelChunk chunk) {
        RadiationSystemNT.onChunkUnloaded(level, chunk);
        RadiationSystemNT.flushForSave(level, true);
        RadiationSystemNT.onChunkLoaded(level, chunk);
    }

    /**
     * A hollow 5 x 5 x 5 box with radiation set at its centre: rad-resistant walls are pocket walls, so the cell just
     * outside stays clean. A stone control box cannot run beside these: its radiation reaches the neighbouring plots.
     */
    private static void boxHolds(GameTestHelper helper, Block wall) {
        ServerLevel level = helper.getLevel();
        BlockPos min = helper.absolutePos(new BlockPos(2, 1, 2));
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(4, 4, 4))) {
            boolean shell = pos.getX() == min.getX() || pos.getX() == min.getX() + 4 || pos.getY() == min.getY()
                    || pos.getY() == min.getY() + 4 || pos.getZ() == min.getZ() || pos.getZ() == min.getZ() + 4;
            level.setBlock(pos, shell ? wall.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        }
        BlockPos centre = min.offset(2, 2, 2), outside = min.offset(6, 2, 2);
        helper.runAfterDelay(5, () -> RadiationSystemNT.setRadForCoord(level, centre, 1000D));
        helper.runAfterDelay(180, () -> {
            double in = RadiationSystemNT.getRadForCoord(level, centre), out = RadiationSystemNT.getRadForCoord(level, outside);
            helper.assertTrue(in > 0D, "the box lost all its radiation");
            helper.assertTrue(out < TOLERANCE, wall + " let " + out + " RAD out (inside " + in + ")");
            helper.succeed();
        });
    }

    /** Walls placed after the section was built: the glass must ask for a rebuild, or the room leaks. */
    private static void glassBoxRebuilds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RadiationSystemNT.setRadForCoord(level, helper.absolutePos(new BlockPos(4, 3, 4)), 0D);
        boxHolds(helper, ModGlassBlocks.reinforced_glass.get());
    }

    /** A sealing door's cells are walls while closed and stop being walls as the door clears them. */
    private static void fireDoorSealsClosed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(6, 1, 6));
        BlockDoorGeneric door = ModDoorBlocks.FIRE_DOOR.get();
        MultiblockHandlerXR.placeAssembled(level, core, door, Direction.NORTH);
        BlockPos cell = BlockPos.betweenClosedStream(new AABB(core).inflate(3)).map(BlockPos::immutable)
                .filter(p -> level.getBlockState(p).getBlock() == door && level.getBlockState(p).getValue(BlockDoorGeneric.CELL))
                .findFirst().orElseThrow();
        helper.assertTrue(door.isRadResistant(level, cell), "a closed fire door cell does not seal");
        door.makeExtra(level, cell);
        helper.assertFalse(door.isRadResistant(level, cell), "an opened fire door cell still seals");
        helper.succeed();
    }
}
