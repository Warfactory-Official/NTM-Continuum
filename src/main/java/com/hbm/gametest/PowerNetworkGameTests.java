package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.EnergyBus;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.uninos.graph.Distribution;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.EnergyCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * The energy contracts that must survive the move off UNINOS onto the node graph: what the network
 * does, never how it is built.
 *
 * <p>Two behaviours the old network had are deliberately <b>not</b> asserted, because the graph
 * replaces them and a test of either would have to fail for the port to be correct. The first is the
 * three-second subscription timeout, which existed only because machines pushed themselves into a net
 * every tick; the graph pulls endpoints from the faces of its own cells and has no timeout. The second
 * is the rotation of receivers within a priority tier, which the graph drops to match 1.7 — every
 * figure asserted below holds whichever end of a tier is served first.
 *
 * <p>The split between world tests and bench tests is forced rather than chosen. A gametest plot is
 * a live server: the block entities placed in it keep ticking and the server keeps walking the graph,
 * so any number of distribution rounds can land between placing a machine and reading its buffer.
 * That is fine for asking whether power crossed a cable run and useless for asking how much. So
 * topology is asserted in the world, and arbitration is asserted against tiers built by hand with
 * endpoints that are not block entities, where one round means one round.
 *
 * <p>The figures the bench tests hold were pinned against the network this replaced, and are
 * unchanged: that they still hold against {@link EnergyBus} is the whole reason they were pinned.
 */
public final class PowerNetworkGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function JOIN =
            TESTS.function("power_net_join", 40, () -> PowerNetworkGameTests::join);
    public static final GameTests.Function SPLIT =
            TESTS.function("power_net_split", 40, () -> PowerNetworkGameTests::split);
    public static final GameTests.Function PRIORITY =
            TESTS.function("power_net_priority", 20, () -> PowerNetworkGameTests::priority);
    public static final GameTests.Function SHARE =
            TESTS.function("power_net_weighted_share", 20, () -> PowerNetworkGameTests::weightedShareWithinTier);
    public static final GameTests.Function DEBIT =
            TESTS.function("power_net_provider_debit", 20, () -> PowerNetworkGameTests::providerDebit);
    public static final GameTests.Function CONSERVATION =
            TESTS.function("power_net_conservation", 20, () -> PowerNetworkGameTests::conservation);
    public static final GameTests.Function OVERFLOW =
            TESTS.function("power_net_share_overflow", 20, () -> PowerNetworkGameTests::shareOverflow);
    public static final GameTests.Function FOREIGN =
            TESTS.function("power_net_foreign_lookup", 20, () -> PowerNetworkGameTests::foreignLookup);
    public static final GameTests.Function GRAPH_ROUND =
            TESTS.function("power_graph_round", 40, () -> PowerNetworkGameTests::graphRound);

    /**
     * A row per test, because a gametest plot is not alone in the level and the graph is. Plots sit
     * close enough that a run reaching one edge is collinear with its neighbour's, and two collinear
     * runs are correctly one network — at which point this test is measuring the plot next door, and
     * measuring it through cells that park the moment that plot is released.
     */
    private static final int JOIN_Z = 6;
    private static final int SPLIT_Z = 12;
    private static final int GRAPH_Z = 18;
    private static final int CABLE_FROM = 6;
    private static final int CABLE_TO = 14;
    private static final long CAP = TileEntityMachineBattery.MAX_POWER;
    /** Both battery speeds are capped at a twentieth of the buffer, so a round can move no more. */
    private static final long SPEED = CAP / 20L;

    private PowerNetworkGameTests() {}

    public static void init() {}

    /** Cables in a line are one network: a provider at one end reaches a receiver at the other. */
    private static void join(GameTestHelper helper) {
        Rig rig = new Rig(helper, JOIN_Z);
        rig.cables(CABLE_FROM, CABLE_TO);
        TileEntityMachineBattery source = rig.battery(CABLE_FROM, JOIN_Z - 1, TileEntityMachineBattery.MODE_OUTPUT, SPEED);
        TileEntityMachineBattery sink = rig.battery(CABLE_FROM + 6, JOIN_Z + 1, TileEntityMachineBattery.MODE_INPUT, 0L);

        rig.settle();

        helper.assertTrue(sink.power > 0L, "A receiver nine cables from a provider received nothing; " + rig.state());
        helper.assertTrue(source.power < SPEED, "The provider was never debited for what it sent");
        helper.succeed();
    }

    /** Breaking a cable in the middle of the run severs the two halves. */
    private static void split(GameTestHelper helper) {
        Rig rig = new Rig(helper, SPLIT_Z);
        rig.cables(CABLE_FROM, CABLE_TO);
        TileEntityMachineBattery source = rig.battery(CABLE_FROM, SPLIT_Z - 1, TileEntityMachineBattery.MODE_OUTPUT, SPEED);
        TileEntityMachineBattery sink = rig.battery(CABLE_FROM + 6, SPLIT_Z + 1, TileEntityMachineBattery.MODE_INPUT, 0L);
        rig.settle();
        helper.assertTrue(sink.power > 0L,
                "The split test could not establish a working network first; " + rig.state());

        rig.breakCable(CABLE_FROM + 4);
        source.power = SPEED;
        sink.power = 0L;
        rig.settle();

        helper.assertTrue(sink.power == 0L, "A receiver kept drawing across a broken cable");
        helper.assertTrue(source.power == SPEED, "A provider kept paying across a broken cable");
        helper.succeed();
    }

    /** A short supply fills the higher tier before the lower one sees anything. */
    private static void priority(GameTestHelper helper) {
        Endpoint high = endpoint(CAP - 10_000L, ConnectionPriority.HIGH);
        Endpoint low = endpoint(CAP - 10_000L, ConnectionPriority.LOW);
        Bench bench = new Bench();
        bench.want(high, 10_000L);
        bench.want(low, 10_000L);

        bench.serve(10_000L);

        helper.assertTrue(high.power == CAP, "The high-priority receiver was not filled first");
        helper.assertTrue(low.power == CAP - 10_000L, "The low-priority receiver was served out of turn");
        helper.succeed();
    }

    /** Inside one tier a short supply is split in proportion to what each receiver asked for. */
    private static void weightedShareWithinTier(GameTestHelper helper) {
        Endpoint wide = endpoint(CAP - 30_000L, ConnectionPriority.NORMAL);
        Endpoint narrow = endpoint(CAP - 10_000L, ConnectionPriority.NORMAL);
        Bench bench = new Bench();
        bench.want(wide, 30_000L);
        bench.want(narrow, 10_000L);

        long delivered = bench.serve(20_000L);

        long toWide = wide.power - (CAP - 30_000L);
        long toNarrow = narrow.power - (CAP - 10_000L);
        helper.assertTrue(delivered == 20_000L, "A tier delivered " + delivered + " of an available 20000");
        helper.assertTrue(toWide + toNarrow == 20_000L,
                "A tier moved " + (toWide + toNarrow) + " of an available 20000");
        helper.assertTrue(toWide == 15_000L && toNarrow == 5_000L,
                "A 3:1 demand split 20000 as " + toWide + ":" + toNarrow + " rather than 15000:5000");
        helper.succeed();
    }

    /** What a round costs is charged across the providers in proportion to what each offered. */
    private static void providerDebit(GameTestHelper helper) {
        Endpoint rich = endpoint(30_000L, ConnectionPriority.NORMAL);
        Endpoint poor = endpoint(10_000L, ConnectionPriority.NORMAL);

        charge(List.of(new Distribution.Share<>(rich, 30_000L), new Distribution.Share<>(poor, 10_000L)),
                20_000L, 40_000L);

        long fromRich = 30_000L - rich.power;
        long fromPoor = 10_000L - poor.power;
        helper.assertTrue(fromRich + fromPoor == 20_000L,
                "Providers were charged " + (fromRich + fromPoor) + " for a 20000 round");
        helper.assertTrue(fromRich == 15_000L && fromPoor == 5_000L,
                "A 3:1 supply was charged as " + fromRich + ":" + fromPoor + " rather than 15000:5000");
        helper.succeed();
    }

    /** Whatever leaves the providers arrives at the receivers; a round neither mints nor burns. */
    private static void conservation(GameTestHelper helper) {
        Endpoint a = endpoint(25_000L, ConnectionPriority.NORMAL);
        Endpoint b = endpoint(25_000L, ConnectionPriority.NORMAL);
        Endpoint x = endpoint(CAP - 40_000L, ConnectionPriority.HIGH);
        Endpoint y = endpoint(CAP - 40_000L, ConnectionPriority.LOW);
        Bench bench = new Bench();
        bench.want(x, 40_000L);
        bench.want(y, 40_000L);

        long delivered = bench.serve(50_000L);
        charge(List.of(new Distribution.Share<>(a, 25_000L), new Distribution.Share<>(b, 25_000L)),
                delivered, 50_000L);

        long paid = (25_000L - a.power) + (25_000L - b.power);
        long got = (x.power - (CAP - 40_000L)) + (y.power - (CAP - 40_000L));
        helper.assertTrue(paid == got, "Providers paid " + paid + " but receivers gained " + got);
        helper.assertTrue(paid == 50_000L, "A 50000 supply against 80000 of demand moved " + paid);
        helper.assertTrue(x.power == CAP, "The high tier was not satisfied before the low tier was served");
        helper.succeed();
    }

    /**
     * The share is taken on {@code long} buffers that reach {@link Long#MAX_VALUE} in normal play, so
     * the plain multiply this replaced handed out a negative share.
     */
    private static void shareOverflow(GameTestHelper helper) {
        long huge = Long.MAX_VALUE / 2L;
        long share = Distribution.weightedShare(huge, huge, Long.MAX_VALUE, huge);
        helper.assertTrue(share >= 0L && share <= huge, "A huge weighted share overflowed to " + share);

        long capped = Distribution.weightedShare(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, 1_000L);
        helper.assertTrue(capped == 1_000L, "A weighted share ignored its cap and returned " + capped);

        helper.assertTrue(Distribution.weightedShare(0L, 1L, 1L, 1L) == 0L, "An empty round still handed out power");
        helper.succeed();
    }

    /**
     * The foreign-energy seam answers on this node and answers nothing where nothing is exposed. Three
     * capability systems hide behind {@code find}, and only the one this node was built against is
     * compiled in, so linking it at all is most of what there is to check until we expose our own.
     */
    private static void foreignLookup(GameTestHelper helper) {
        helper.assertTrue(EnergyCompat.supported(),
                "The gametest runner is a forge-like node, so foreign energy must be supported");
        BlockPos empty = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().removeBlock(empty, false);
        helper.assertTrue(EnergyCompat.find(helper.getLevel(), empty, Direction.NORTH) == null,
                "Empty air answered a foreign energy lookup");
        helper.assertTrue(EnergyCompat.find(helper.getLevel(), empty, null) == null,
                "Empty air answered a sideless foreign energy lookup");
        EnergyCompat.invalidate(helper.getLevel(), empty);
        helper.succeed();
    }

    /**
     * A round over cells the graph was handed directly, rather than ones a cable block minted. It is
     * the same path {@link #join} runs, minus the blocks: it says the graph still works when something
     * other than a cable is the conductor, which is what a buffered battery and a battery bank are.
     *
     * <p>Sited at y = 3 so the cells never coincide with the y = 1 blocks the world tests place, and
     * torn down in a finally, because the graph is level-wide save data that outlives any one plot: a
     * cell this test leaked would join the next test's network, in this run or in a later one.
     */
    private static void graphRound(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelNodeGraph<CableData> graph = PowerGraph.get(level);
        long[] cells = new long[CABLE_TO - CABLE_FROM + 1];
        for (int x = CABLE_FROM; x <= CABLE_TO; x++) {
            cells[x - CABLE_FROM] = helper.absolutePos(new BlockPos(x, 3, GRAPH_Z)).asLong();
            graph.addNode(cells[x - CABLE_FROM], CableData.INSTANCE, MachineCaps.FACE_MASK_ALL);
        }

        String failure;
        long moved;
        long total;
        try {
            Rig rig = new Rig(helper, GRAPH_Z);
            TileEntityMachineBattery source =
                    rig.batteryAt(CABLE_FROM, 3, GRAPH_Z - 1, TileEntityMachineBattery.MODE_OUTPUT, SPEED);
            TileEntityMachineBattery sink =
                    rig.batteryAt(CABLE_FROM + 6, 3, GRAPH_Z + 1, TileEntityMachineBattery.MODE_INPUT, 0L);

            NodeNetwork<CableData> net = graph.networkAt(cells[0]);
            failure = net == null ? "Nine cells did not form a network"
                    : net.size() < cells.length ? "The network holds only " + net.size() + " of " + cells.length
                    + " cells" : null;
            if (net != null) {
                // The sink hangs off a run INTERIOR, which lives in a segment's bitset: a segment face
                // that probes PRESENT promotes the cell and re-queues it after the pass that found it,
                // so the round that serves it is never the first one.
                for (int pass = 0; pass < 8 && sink.power == 0L; pass++) {
                    PowerNetwork.distribute(level, graph, net);
                    source.power = SPEED;
                    sink.power = 0L;
                }
                sink.power = 0L;
                source.power = SPEED;
                PowerNetwork.distribute(level, graph, net);
            }
            moved = sink.power;
            total = source.power + sink.power;
        } finally {
            for (long cell : cells) graph.removeNode(cell);
        }

        for (long cell : cells) {
            helper.assertTrue(!graph.containsCell(cell),
                    "A hand-fed cell outlived the test at " + BlockPos.of(cell));
            NodeNetwork<CableData> orphan = graph.networkAt(cell);
            helper.assertTrue(orphan == null, "A removed cell still answers with a network of "
                    + (orphan == null ? 0 : orphan.size()) + " cells at " + BlockPos.of(cell));
        }
        helper.assertTrue(failure == null, String.valueOf(failure));
        helper.assertTrue(moved > 0L, "The graph moved nothing to a receiver on its own net");
        helper.assertTrue(total == SPEED, "The graph round created or destroyed energy: " + total);
        helper.succeed();
    }

    private static long deliver(IEnergyHandlerMK2 handler, long amount) {
        return amount - handler.transferPower(amount, false);
    }

    /** Charges {@code owed} back across a round's providers, exactly as a net settles one. */
    private static void charge(List<Distribution.Share<IEnergyHandlerMK2>> providers, long owed, long supply) {
        Distribution.debit(providers, owed, supply, new int[1], 0,
                (provider, amount) -> { provider.usePower(amount); return amount; });
    }

    /** One round's receiver tiers, built by hand so that one call to {@link EnergyBus} is one round. */
    private static final class Bench {
        @SuppressWarnings("unchecked")
        private final List<Distribution.Share<IEnergyHandlerMK2>>[] tiers = new List[ConnectionPriority.VALUES.length];
        private final long[] demand = new long[ConnectionPriority.VALUES.length];

        Bench() {
            for (int i = 0; i < tiers.length; i++) tiers[i] = new ArrayList<>();
        }

        void want(Endpoint receiver, long headroom) {
            int tier = receiver.getPriority().ordinal();
            tiers[tier].add(new Distribution.Share<>(receiver, headroom));
            demand[tier] += headroom;
        }

        long serve(long budget) {
            return EnergyBus.serveShortage(tiers, demand, budget, PowerNetworkGameTests::deliver);
        }
    }

    private static Endpoint endpoint(long power, ConnectionPriority priority) {
        return new Endpoint(power, priority);
    }

    /**
     * A provider and receiver that is not a block entity, so a bench round is exactly one round. The
     * net only reads the map it is iterating, so one class serves both roles.
     */
    private static final class Endpoint implements IEnergyReceiverMK2, IEnergyProviderMK2 {

        private final ConnectionPriority priority;
        long power;

        Endpoint(long power, ConnectionPriority priority) {
            this.power = power;
            this.priority = priority;
        }

        @Override public long getPower() { return power; }
        @Override public void setPower(long power) { this.power = power; }
        @Override public long getMaxPower() { return CAP; }
        @Override public long getReceiverSpeed() { return CAP; }
        @Override public long getProviderSpeed() { return CAP; }
        @Override public ConnectionPriority getPriority() { return priority; }
        @Override public boolean isLoaded() { return true; }
    }

    /** One test's blocks, and the tick order that gets a network built and a round run. */
    private static final class Rig {

        private final GameTestHelper helper;
        private final ServerLevel level;
        private final int row;
        private final List<BlockPos> cables = new ArrayList<>();
        private final List<BlockPos> batteries = new ArrayList<>();

        Rig(GameTestHelper helper, int row) {
            this.helper = helper;
            this.level = helper.getLevel();
            this.row = row;
        }

        void cables(int fromX, int toX) {
            for (int x = fromX; x <= toX; x++) {
                BlockPos pos = helper.absolutePos(new BlockPos(x, 1, row));
                level.setBlock(pos, ModMachineBlocks.red_cable.get().defaultBlockState(), 3);
                cables.add(pos);
            }
        }

        void breakCable(int x) {
            BlockPos pos = helper.absolutePos(new BlockPos(x, 1, row));
            level.removeBlock(pos, false);
            cables.remove(pos);
        }

        TileEntityMachineBattery battery(int x, int z, int mode, long power) {
            return batteryAt(x, 1, z, mode, power);
        }

        TileEntityMachineBattery batteryAt(int x, int y, int z, int mode, long power) {
            BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
            level.setBlock(pos, ModMachineBlocks.machine_battery.get().defaultBlockState(), 3);
            TileEntityMachineBattery battery = (TileEntityMachineBattery) level.getBlockEntity(pos);
            helper.assertTrue(battery != null, "A battery block entity failed to appear at " + pos);
            battery.redLow = (short) mode;
            battery.redHigh = (short) mode;
            battery.power = power;
            batteries.add(pos);
            return battery;
        }

        /**
         * The cables minted their cells when their blocks were placed, so all that is left is rounds.
         * More than one, because discovery is not instant: a machine on a run END hangs off a
         * first-class node and is gathered at once, while one on a run INTERIOR lives in a segment's
         * bitset, and probing that face PRESENT promotes the cell and re-queues it after the pass that
         * found it. How many passes that takes depends on the shape of the run, so this settles until
         * something moves rather than pinning a number the topology decides.
         */
        void settle() {
            for (int pass = 0; pass < 8; pass++) {
                tickBatteries();
                round();
                if (moved()) return;
            }
        }

        private boolean moved() {
            for (BlockPos pos : batteries) {
                if (level.getBlockEntity(pos) instanceof TileEntityMachineBattery battery && battery.power > 0L
                        && battery.getRelevantMode() == TileEntityMachineBattery.MODE_INPUT) {
                    return true;
                }
            }
            return false;
        }

        /**
         * What the graph makes of the run, for a failure that has to say which half went wrong. The
         * plot is not alone in the graph — it is level-wide save data and 500-odd other tests are
         * running in it — so a net wider than the run is reported, not assumed away.
         */
        String state() {
            LevelNodeGraph<CableData> graph = PowerGraph.get(level);
            NodeNetwork<CableData> net = graph.networkAt(cables.get(0).asLong());
            if (net == null) return cables.size() + " cables, no network at the first one";
            return cables.size() + " cables from " + cables.get(0) + ", a network of " + net.size()
                    + " cells with " + graph.activeFaces(net).size() + " nodes looking at something";
        }

        void round() {
            LevelNodeGraph<CableData> graph = PowerGraph.get(level);
            graph.applyCompiledSwaps();
            for (NodeNetwork<CableData> net : List.copyOf(graph.networks())) {
                PowerNetwork.distribute(level, graph, net);
            }
        }

        private void tickBatteries() {
            for (BlockPos pos : batteries) {
                if (level.getBlockEntity(pos) instanceof TileEntityMachineBattery battery) {
                    TileEntityMachineBattery.serverTick(level, pos, battery.getBlockState(), battery);
                }
            }
        }
    }
}
