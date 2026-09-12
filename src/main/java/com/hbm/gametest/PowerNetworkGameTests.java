package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.EnergyBus;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.energymk2.PowerNetMK2;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.network.TileEntityCableBaseNT;
import com.hbm.uninos.UniNodespace;
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
 * <p>Two behaviours the current network has are deliberately <b>not</b> asserted, because the graph
 * replaces them and a test of either would have to fail for the port to be correct. The first is the
 * three-second subscription timeout, which exists only because machines push themselves into a net
 * every tick; the graph pulls endpoints from a registry and has no timeout. The second is the
 * rotation of receivers within a priority tier, which the graph drops to match 1.7 — every figure
 * asserted below holds whichever end of a tier is served first.
 *
 * <p>The split between world tests and bench tests is forced rather than chosen. A gametest plot is
 * a live server: the block entities placed in it keep ticking and the server keeps walking the
 * nodespace, so any number of distribution rounds can land between placing a machine and reading its
 * buffer. That is fine for asking whether power crossed a cable run and useless for asking how much.
 * So topology is asserted in the world, and arbitration is asserted against a net built by hand with
 * endpoints that are not block entities, where one round means one round.
 *
 * <p>The four bench tests name {@link PowerNetMK2} directly, which is the one thing here that does
 * not survive the port. When the graph lands they keep their figures and change their first two
 * lines to build the graph's net instead.
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
    public static final GameTests.Function BUS =
            TESTS.function("power_bus_matches_contracts", 20, () -> PowerNetworkGameTests::busMatchesContracts);
    public static final GameTests.Function GRAPH_ROUND =
            TESTS.function("power_graph_round", 40, () -> PowerNetworkGameTests::graphRound);

    private static final int CABLE_Z = 10;
    private static final int CABLE_FROM = 4;
    private static final int CABLE_TO = 12;
    private static final long CAP = TileEntityMachineBattery.MAX_POWER;
    /** Both battery speeds are capped at a twentieth of the buffer, so a round can move no more. */
    private static final long SPEED = CAP / 20L;

    private PowerNetworkGameTests() {}

    public static void init() {}

    /** Cables in a line are one network: a provider at one end reaches a receiver at the other. */
    private static void join(GameTestHelper helper) {
        Rig rig = new Rig(helper);
        rig.cables(CABLE_FROM, CABLE_TO);
        TileEntityMachineBattery source = rig.battery(4, 9, TileEntityMachineBattery.MODE_OUTPUT, SPEED);
        TileEntityMachineBattery sink = rig.battery(10, 11, TileEntityMachineBattery.MODE_INPUT, 0L);

        rig.settle();

        helper.assertTrue(sink.power > 0L, "A receiver nine cables from a provider received nothing");
        helper.assertTrue(source.power < SPEED, "The provider was never debited for what it sent");
        helper.succeed();
    }

    /** Breaking a cable in the middle of the run severs the two halves. */
    private static void split(GameTestHelper helper) {
        Rig rig = new Rig(helper);
        rig.cables(CABLE_FROM, CABLE_TO);
        TileEntityMachineBattery source = rig.battery(4, 9, TileEntityMachineBattery.MODE_OUTPUT, SPEED);
        TileEntityMachineBattery sink = rig.battery(10, 11, TileEntityMachineBattery.MODE_INPUT, 0L);
        rig.settle();
        helper.assertTrue(sink.power > 0L, "The split test could not establish a working network first");

        rig.breakCable(8);
        source.power = SPEED;
        sink.power = 0L;
        rig.settle();

        helper.assertTrue(sink.power == 0L, "A receiver kept drawing across a broken cable");
        helper.assertTrue(source.power == SPEED, "A provider kept paying across a broken cable");
        helper.succeed();
    }

    /** A short supply fills the higher tier before the lower one sees anything. */
    private static void priority(GameTestHelper helper) {
        PowerNetMK2 net = new PowerNetMK2();
        Endpoint source = endpoint(10_000L, ConnectionPriority.NORMAL);
        Endpoint high = endpoint(CAP - 10_000L, ConnectionPriority.HIGH);
        Endpoint low = endpoint(CAP - 10_000L, ConnectionPriority.LOW);
        net.addProvider(source);
        net.addReceiver(high);
        net.addReceiver(low);

        net.update();

        helper.assertTrue(high.power == CAP, "The high-priority receiver was not filled first");
        helper.assertTrue(low.power == CAP - 10_000L, "The low-priority receiver was served out of turn");
        helper.succeed();
    }

    /** Inside one tier a short supply is split in proportion to what each receiver asked for. */
    private static void weightedShareWithinTier(GameTestHelper helper) {
        PowerNetMK2 net = new PowerNetMK2();
        Endpoint source = endpoint(20_000L, ConnectionPriority.NORMAL);
        Endpoint wide = endpoint(CAP - 30_000L, ConnectionPriority.NORMAL);
        Endpoint narrow = endpoint(CAP - 10_000L, ConnectionPriority.NORMAL);
        net.addProvider(source);
        net.addReceiver(wide);
        net.addReceiver(narrow);

        net.update();

        long toWide = wide.power - (CAP - 30_000L);
        long toNarrow = narrow.power - (CAP - 10_000L);
        helper.assertTrue(toWide + toNarrow == 20_000L,
                "A tier delivered " + (toWide + toNarrow) + " of an available 20000");
        helper.assertTrue(toWide == 15_000L && toNarrow == 5_000L,
                "A 3:1 demand split 20000 as " + toWide + ":" + toNarrow + " rather than 15000:5000");
        helper.succeed();
    }

    /** What a round costs is charged across the providers in proportion to what each offered. */
    private static void providerDebit(GameTestHelper helper) {
        PowerNetMK2 net = new PowerNetMK2();
        Endpoint rich = endpoint(30_000L, ConnectionPriority.NORMAL);
        Endpoint poor = endpoint(10_000L, ConnectionPriority.NORMAL);
        Endpoint sink = endpoint(CAP - 20_000L, ConnectionPriority.NORMAL);
        net.addProvider(rich);
        net.addProvider(poor);
        net.addReceiver(sink);

        net.update();

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
        PowerNetMK2 net = new PowerNetMK2();
        Endpoint a = endpoint(25_000L, ConnectionPriority.NORMAL);
        Endpoint b = endpoint(25_000L, ConnectionPriority.NORMAL);
        Endpoint x = endpoint(CAP - 40_000L, ConnectionPriority.HIGH);
        Endpoint y = endpoint(CAP - 40_000L, ConnectionPriority.LOW);
        net.addProvider(a);
        net.addProvider(b);
        net.addReceiver(x);
        net.addReceiver(y);

        net.update();

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
        long share = PowerNetMK2.weightedShare(huge, huge, Long.MAX_VALUE, huge);
        helper.assertTrue(share >= 0L && share <= huge, "A huge weighted share overflowed to " + share);

        long capped = PowerNetMK2.weightedShare(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, 1_000L);
        helper.assertTrue(capped == 1_000L, "A weighted share ignored its cap and returned " + capped);

        helper.assertTrue(PowerNetMK2.weightedShare(0L, 1L, 1L, 1L) == 0L, "An empty round still handed out power");
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
     * The ported arbitration, held to the same figures the bench tests pin on {@link PowerNetMK2}.
     * This is the whole point of having pinned them: it says the graph's law agrees with the one
     * running today, before a single machine moves onto it.
     *
     * <p>The one known divergence cannot be seen here and is not meant to be. UNINOS rotates a cursor
     * through a tier between rounds and the bus does not, which 1.7 agrees with; with two receivers
     * and a last-takes-the-rest rule the totals come out the same either way.
     */
    private static void busMatchesContracts(GameTestHelper helper) {
        int normal = ConnectionPriority.NORMAL.ordinal();
        Endpoint wide = endpoint(CAP - 30_000L, ConnectionPriority.NORMAL);
        Endpoint narrow = endpoint(CAP - 10_000L, ConnectionPriority.NORMAL);
        List<Distribution.Share<IEnergyHandlerMK2>>[] tiers = tiers();
        tiers[normal] = List.of(new Distribution.Share<>(wide, 30_000L),
                new Distribution.Share<>(narrow, 10_000L));
        long[] demand = new long[ConnectionPriority.VALUES.length];
        demand[normal] = 40_000L;

        long delivered = EnergyBus.serveShortage(tiers, demand, 20_000L, PowerNetworkGameTests::deliver);

        helper.assertTrue(delivered == 20_000L, "The bus delivered " + delivered + " of an available 20000");
        long toWide = wide.power - (CAP - 30_000L);
        long toNarrow = narrow.power - (CAP - 10_000L);
        helper.assertTrue(toWide == 15_000L && toNarrow == 5_000L,
                "The bus split a 3:1 demand as " + toWide + ":" + toNarrow + " rather than 15000:5000");

        Endpoint high = endpoint(CAP - 10_000L, ConnectionPriority.HIGH);
        Endpoint low = endpoint(CAP - 10_000L, ConnectionPriority.LOW);
        List<Distribution.Share<IEnergyHandlerMK2>>[] tiered = tiers();
        tiered[ConnectionPriority.HIGH.ordinal()] = List.of(new Distribution.Share<>(high, 10_000L));
        tiered[ConnectionPriority.LOW.ordinal()] = List.of(new Distribution.Share<>(low, 10_000L));
        long[] tieredDemand = new long[ConnectionPriority.VALUES.length];
        tieredDemand[ConnectionPriority.HIGH.ordinal()] = 10_000L;
        tieredDemand[ConnectionPriority.LOW.ordinal()] = 10_000L;

        EnergyBus.serveShortage(tiered, tieredDemand, 10_000L, PowerNetworkGameTests::deliver);

        helper.assertTrue(high.power == CAP, "The bus did not fill the high tier first");
        helper.assertTrue(low.power == CAP - 10_000L, "The bus served the low tier out of turn");
        helper.succeed();
    }

    /**
     * A round over the node graph itself, end to end: nodes, a probe pass that finds the machines at
     * the edge faces, and the bus serving them. Cables still mint UNINOS nodes, so the graph is fed
     * by hand here — which is the only way to run this path at all until they move over.
     *
     * <p>Sited at y = 3 so the graph's cells never coincide with the y = 1 blocks the world tests
     * place; the graph is level-wide save data and outlives any one plot.
     */
    private static void graphRound(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelNodeGraph<CableData> graph = PowerGraph.get(level);
        for (int x = CABLE_FROM; x <= CABLE_TO; x++) {
            BlockPos cell = helper.absolutePos(new BlockPos(x, 3, CABLE_Z));
            graph.addNode(cell.asLong(), CableData.INSTANCE, MachineCaps.FACE_MASK_ALL);
        }

        Rig rig = new Rig(helper);
        TileEntityMachineBattery source = rig.batteryAt(4, 3, 9, TileEntityMachineBattery.MODE_OUTPUT, SPEED);
        TileEntityMachineBattery sink = rig.batteryAt(10, 3, 11, TileEntityMachineBattery.MODE_INPUT, 0L);

        NodeNetwork<CableData> net =
                graph.networkAt(helper.absolutePos(new BlockPos(CABLE_FROM, 3, CABLE_Z)).asLong());
        helper.assertTrue(net != null, "Nine cable cells did not form a network");
        helper.assertTrue(net.size() == (CABLE_TO - CABLE_FROM + 1),
                "The network holds " + net.size() + " cells rather than " + (CABLE_TO - CABLE_FROM + 1));

        // Two passes on purpose. The source hangs off a run END, which is first-class already; the
        // sink hangs off a run INTERIOR, which lives in a segment's bitset. A segment face that
        // probes PRESENT promotes that cell to first-class and re-queues it, and the node drain has
        // already run by then, so the promoted face is not gathered until the pass after.
        PowerNetwork.distribute(level, graph, net);
        source.power = SPEED;
        sink.power = 0L;
        PowerNetwork.distribute(level, graph, net);

        helper.assertTrue(sink.power > 0L, "The graph moved nothing to a receiver on its own net");
        helper.assertTrue(source.power + sink.power == SPEED,
                "The graph round created or destroyed energy: " + source.power + " + " + sink.power);
        helper.succeed();
    }

    private static long deliver(IEnergyHandlerMK2 handler, long amount) {
        return amount - handler.transferPower(amount, false);
    }

    @SuppressWarnings("unchecked")
    private static List<Distribution.Share<IEnergyHandlerMK2>>[] tiers() {
        return new List[ConnectionPriority.VALUES.length];
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
        private final List<BlockPos> cables = new ArrayList<>();
        private final List<BlockPos> batteries = new ArrayList<>();

        Rig(GameTestHelper helper) {
            this.helper = helper;
            this.level = helper.getLevel();
        }

        void cables(int fromX, int toX) {
            for (int x = fromX; x <= toX; x++) {
                BlockPos pos = helper.absolutePos(new BlockPos(x, 1, CABLE_Z));
                level.setBlock(pos, ModMachineBlocks.red_cable.get().defaultBlockState(), 3);
                cables.add(pos);
            }
        }

        void breakCable(int x) {
            BlockPos pos = helper.absolutePos(new BlockPos(x, 1, CABLE_Z));
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
         * Cables mint their nodes on their own tick, so the nodespace has to be walked once before a
         * machine can find a network to subscribe to, and again to run the round it subscribed for.
         */
        void settle() {
            tickCables();
            UniNodespace.updateNodespace(level.getServer());
            tickBatteries();
            UniNodespace.updateNodespace(level.getServer());
        }

        private void tickCables() {
            for (BlockPos pos : cables) {
                if (level.getBlockEntity(pos) instanceof TileEntityCableBaseNT cable) {
                    TileEntityCableBaseNT.serverTick(level, pos, cable.getBlockState(), cable);
                }
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
