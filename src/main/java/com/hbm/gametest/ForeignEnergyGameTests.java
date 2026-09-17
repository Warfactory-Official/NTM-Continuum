package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.EnergyCompat;
import dev.golem.core.world.EnergyView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The foreign energy bridge against a real third-party mod rather than against our own facades meeting
 * at the capability. Pipez is the peer: it is published for all three forge-like nodes, it needs no
 * other mod, and its energy pipe both exposes a handler per side and drives the transfer itself.
 *
 * <p>What a real peer proves that a synthetic one cannot is the transaction contract. Pipez decides
 * where to send by opening a transaction, calling {@code insert(1, …)} on every candidate destination,
 * and then <b>closing that transaction without committing it</b> — every tick, against every connection.
 * A handler that treats the call as final rather than journalling it does not fail loudly; it quietly
 * mints energy, one unit per destination per tick. The conservation assertion in
 * {@link #roundTrip} is what catches that, which is why it is asserted in a test whose headline claim
 * is about something else.
 *
 * <p>These tests skip where there is nothing to test: Fabric ships no standard energy capability, and a
 * node whose {@code deps.pipez} is blank has no peer on the runtime classpath.
 */
public final class ForeignEnergyGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function ROUND_TRIP =
            TESTS.function("power_foreign_mod_round_trip", 120, () -> ForeignEnergyGameTests::roundTrip);
    public static final GameTests.Function FACE =
            TESTS.function("power_foreign_mod_face", 40, () -> ForeignEnergyGameTests::face);

    /** Pipez's energy pipe, the peer these tests drive. */
    private static final String PIPE_NAMESPACE = "pipez", PIPE_PATH = "energy_pipe";

    /**
     * Pipez gives a pipe a block entity only where its state says it carries configuration, and a pipe
     * with no block entity exposes no handler and moves nothing. Set by name rather than compiled
     * against, like everything else these tests touch of the peer.
     */
    private static final String PIPE_DATA_PROPERTY = "has_data";

    /**
     * A row per test at a height nothing else uses. Plots sit close enough that a cable run reaching one
     * edge is collinear with its neighbour's, and two collinear runs are correctly one network — at
     * which point a test is measuring the plot next door through cells that park the moment that plot is
     * released. {@code PowerNetworkGameTests} keeps its runs at y=1; these stay clear of them entirely.
     */
    private static final int Y = 3;
    private static final int ROUND_TRIP_Z = 6;
    private static final int FACE_Z = 12;

    /** The pipe sits between the two runs, so neither is a neighbour of the other. */
    private static final int RUN_A_FROM = 6, RUN_A_TO = 10, PIPE_X = 11, RUN_B_FROM = 12, RUN_B_TO = 16;

    private ForeignEnergyGameTests() {}

    public static void init() {}

    /**
     * A charged net, a pipez pipe, and a second net that shares no cable with the first. Energy that
     * arrives at the far battery crossed the capability boundary twice — out of our handler under
     * pipez's transactions, and back into it — so the two directions are asserted by one reading.
     */
    private static void roundTrip(GameTestHelper helper) {
        if (skip(helper)) return;
        run(helper, RUN_A_FROM, RUN_A_TO, ROUND_TRIP_Z);
        run(helper, RUN_B_FROM, RUN_B_TO, ROUND_TRIP_Z);
        BlockPos pipe = pipe(helper, PIPE_X, ROUND_TRIP_Z, Direction.WEST);

        TileEntityMachineBattery source = battery(helper, RUN_A_FROM, ROUND_TRIP_Z - 1,
                TileEntityMachineBattery.MODE_OUTPUT, TileEntityMachineBattery.MAX_POWER);
        TileEntityMachineBattery sink = battery(helper, RUN_B_TO, ROUND_TRIP_Z + 1,
                TileEntityMachineBattery.MODE_INPUT, 0L);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(sink.power > 0L,
                    "Nothing crossed a real mod's pipe at " + pipe + ": the far battery is still empty");
            helper.assertTrue(source.power < TileEntityMachineBattery.MAX_POWER,
                    "The far battery filled without the near one being debited");
            helper.assertTrue(source.power + sink.power <= TileEntityMachineBattery.MAX_POWER,
                    "Energy was created: " + source.power + " + " + sink.power + " exceeds the "
                            + TileEntityMachineBattery.MAX_POWER + " the net started with, which is what a "
                            + "rolled-back insert looks like when it is not journalled");
            helper.succeed();
        });
    }

    /** Each side can see the other's handler — the lookup both mods' discovery is built on. */
    private static void face(GameTestHelper helper) {
        if (skip(helper)) return;
        ServerLevel level = helper.getLevel();

        run(helper, RUN_A_FROM, RUN_A_TO, FACE_Z);
        BlockPos pipe = pipe(helper, PIPE_X, FACE_Z, Direction.WEST);
        BlockPos cable = helper.absolutePos(new BlockPos(RUN_A_TO, Y, FACE_Z));

        EnergyView theirs = EnergyCompat.find(level, pipe, Direction.WEST);
        helper.assertTrue(theirs != null, "A real mod's energy block exposed nothing our net can find");

        EnergyView ours = EnergyCompat.find(level, cable, Direction.EAST);
        helper.assertTrue(ours != null,
                "A cable exposed nothing at the face a real mod's connection check reads");
        helper.succeed();
    }

    /**
     * Places a pipe already pulling from {@code from}. Pipez calls the side it draws from its extracting
     * side and answers a pusher on that side too, so nothing moves either way — and no handler is even
     * exposed — until one is set. Its one public setter is reached reflectively: the peer is a
     * runtime-only dependency, and a node without it must still build.
     */
    private static BlockPos pipe(GameTestHelper helper, int x, int z, Direction from) {
        Block block = pipeBlock();
        BlockState state = block.defaultBlockState();
        if (block.getStateDefinition().getProperty(PIPE_DATA_PROPERTY) instanceof BooleanProperty carriesData) {
            state = state.setValue(carriesData, true);
        }
        BlockPos pos = helper.absolutePos(new BlockPos(x, Y, z));
        helper.getLevel().setBlock(pos, state, 3);

        BlockEntity be = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(be != null, "A pipez pipe placed with " + PIPE_DATA_PROPERTY
                + " set still has no block entity, so it can carry nothing");
        try {
            be.getClass().getMethod("setExtracting", Direction.class, boolean.class).invoke(be, from, true);
        } catch (ReflectiveOperationException e) {
            helper.fail("Pipez no longer has setExtracting(Direction, boolean): " + e);
        }
        return pos;
    }

    /** Nothing to test where there is no foreign energy system, or no foreign mod to be one. */
    private static boolean skip(GameTestHelper helper) {
        if (EnergyCompat.supported() && pipeBlock() != null) return false;
        helper.succeed();
        return true;
    }

    private static Block pipeBlock() {
        return BuiltInRegistries.BLOCK.getOptional(Ids.of(PIPE_NAMESPACE, PIPE_PATH)).orElse(null);
    }

    private static void run(GameTestHelper helper, int fromX, int toX, int z) {
        for (int x = fromX; x <= toX; x++) place(helper, x, z, ModMachineBlocks.red_cable.get());
    }

    private static BlockPos place(GameTestHelper helper, int x, int z, Block block) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, Y, z));
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        return pos;
    }

    private static TileEntityMachineBattery battery(GameTestHelper helper, int x, int z, int mode, long power) {
        BlockPos pos = place(helper, x, z, ModMachineBlocks.machine_battery.get());
        TileEntityMachineBattery battery = (TileEntityMachineBattery) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(battery != null, "A battery block entity failed to appear at " + pos);
        battery.redLow = (short) mode;
        battery.redHigh = (short) mode;
        battery.power = power;
        return battery;
    }
}
