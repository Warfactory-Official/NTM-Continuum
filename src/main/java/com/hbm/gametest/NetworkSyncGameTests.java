package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.net.GolemSync;
import dev.golem.core.net.GolemTransport;
import dev.golem.core.net.GolemWire;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Contracts for the block-entity replication engine.
 *
 * <p>The engine has two halves that are only correct together: a sender asks whether anyone is
 * watching before it serialises anything, and the table replays the last wire to whoever arrives
 * afterwards. Each half alone is a defect — the gate alone desynchronises a player who arrives after
 * the last change, the table alone keeps paying to serialise state nobody reads — so what is asserted
 * here is both directions, not merely that a packet goes out.
 *
 * <p>Live chunk tracking cannot be produced on a headless test server: a mock player is in the level
 * but never passes through the chunk map, so {@code getPlayers} never names it. The watcher set is
 * therefore stated for the replication cases and checked against the real chunk map on its own in
 * {@link #machineWiring}. That split is deliberate — it puts the logic that has the subtle failure
 * modes under a deterministic test on all six nodes, and leaves the one line that cannot be
 * determinised asserting an invariant instead.
 *
 * <p>The two functions do not share a chunk. {@link #replicationContract} works on a position far
 * outside any plot and places no block, so its statements cannot collide with a concurrently running
 * test's.
 */
public final class NetworkSyncGameTests {

    private NetworkSyncGameTests() {}

    private static final String SYNC_CHANNEL = "hbm:be_sync";

    public static final GameTests.Suite TESTS = GameTests.suite(com.hbm.Constants.MOD_ID);

    public static final GameTests.Function REPLICATION = TESTS.function(
            "sync_replication_contract", 20, () -> NetworkSyncGameTests::replicationContract);
    public static final GameTests.Function WIRING = TESTS.function(
            "sync_machine_wiring", 20, () -> NetworkSyncGameTests::machineWiring);
    public static final GameTests.Function BACKENDS_AGREE = TESTS.function(
            "sync_backends_deliver_the_same_payload", 20,
            () -> NetworkSyncGameTests::backendsDeliverTheSamePayload);
    public static final GameTests.Function ACCELERATION_AVAILABLE = TESTS.function(
            "sync_acceleration_is_available", 20, () -> NetworkSyncGameTests::accelerationIsAvailable);
    public static final GameTests.Function BATCH_WAITS = TESTS.function(
            "sync_batched_send_waits_for_tick_end", 20,
            () -> NetworkSyncGameTests::batchedSendWaitsForTickEnd);
    public static final GameTests.Function FALLS_BACK = TESTS.function(
            "sync_falls_back_when_unaccelerated", 20,
            () -> NetworkSyncGameTests::fallsBackWhenUnaccelerated);

    public static void init() {}

    /**
     * Runs a body once per backend. Every behavioural assertion goes through this: acceleration is a
     * change of transport, not of contract, so a test that passes on one backend and not the other is
     * the failure this whole arrangement exists to catch.
     */
    private static void onEachBackend(java.util.function.Consumer<GolemTransport.Backend> body) {
        GolemTransport.Backend before = GolemTransport.requested();
        try {
            for (GolemTransport.Backend backend : GolemTransport.Backend.values()) {
                GolemTransport.requested(backend);
                body.accept(backend);
            }
        } finally {
            GolemTransport.requested(before);
        }
    }

    /**
     * Healing, reconciliation and eviction, driven from a stated watcher set. No block is placed: the
     * table is keyed by position and does not care whether one is there, and staying out of the plot
     * grid keeps the stated chunk private to this test.
     */
    public static void replicationContract(GameTestHelper helper) {
        onEachBackend(backend -> replicationContract(helper, backend));
        helper.succeed();
    }

    private static void replicationContract(GameTestHelper helper, GolemTransport.Backend backend) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO).offset(1 << 16, 0, 1 << 16);
        try (GameTestHelpers.MockServerPlayer first = GameTestHelpers.mockServerPlayer(helper);
                GameTestHelpers.MockServerPlayer second = GameTestHelpers.mockServerPlayer(helper)) {

            GolemSync.stateWatchers(level, pos, List.of(first.player()));
            drain(first, second);

            publish(level, pos, (byte) 1);
            helper.assertTrue(GolemSync.isPublished(level, pos), "A publish must be recorded" + on(backend));
            helper.assertTrue(sent(first) == 1, "The watcher present at publish must be sent to" + on(backend));
            helper.assertTrue(sent(second) == 0, "A player who is not watching must not be sent to" + on(backend));

            GolemSync.stateWatchers(level, pos, List.of(first.player(), second.player()));
            GolemSync.pumpNow();
            helper.assertTrue(sent(second) == 1,
                    "A player arriving after the publish must be healed with the last wire" + on(backend));
            helper.assertTrue(sent(first) == 0,
                    "A player already holding the state must not be sent it again" + on(backend));

            GolemSync.pumpNow();
            GolemSync.pumpNow();
            helper.assertTrue(sent(first) == 0 && sent(second) == 0,
                    "A steady watcher set must produce no further traffic" + on(backend));

            GolemSync.stateWatchers(level, pos, List.of(first.player()));
            GolemSync.pumpNow();
            helper.assertTrue(GolemSync.markedCount(level, pos) == 1,
                    "A departed player's mark must be dropped, or the mark set grows without bound" + on(backend));

            GolemSync.stateWatchers(level, pos, List.of(first.player(), second.player()));
            GolemSync.pumpNow();
            helper.assertTrue(sent(second) == 1,
                    "A player who left and came back must be sent the state again" + on(backend));

            GolemSync.stateWatchers(level, pos, List.of());
            GolemSync.pumpNow();
            helper.assertTrue(GolemSync.markedCount(level, pos) == 0,
                    "An unwatched entry must hold no marks" + on(backend));

            GolemSync.retract(level, pos);
            helper.assertFalse(GolemSync.isPublished(level, pos),
                    "Retraction must drop the entry, or the table grows for the server's life" + on(backend));
        } finally {
            GolemSync.clearStatedWatchers(level, pos);
            GolemSync.retract(level, pos);
        }
    }

    /**
     * That {@code networkPackNT} is actually on the engine: shut gate means nothing serialised, open
     * gate means one packet on the wire, and an unchanged payload still reaches a newcomer.
     */
    public static void machineWiring(GameTestHelper helper) {
        onEachBackend(backend -> machineWiring(helper, backend));
        helper.succeed();
    }

    private static void machineWiring(GameTestHelper helper, GolemTransport.Backend backend) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        try (GameTestHelpers.MockServerPlayer watcher = GameTestHelpers.mockServerPlayer(helper);
                GameTestHelpers.MockServerPlayer newcomer = GameTestHelpers.mockServerPlayer(helper)) {

            boolean tracked = !level.getChunkSource().chunkMap
                    .getPlayers(level.getChunkAt(pos).getPos(), false).isEmpty();
            helper.assertTrue(GolemSync.isWatched(level, pos) == tracked,
                    "With nothing stated, the gate must answer exactly what the chunk map answers" + on(backend));
            helper.assertTrue(GolemSync.isWatched(level, pos) == tracked,
                    "The per-tick memo must not change the answer on a second ask" + on(backend));

            TileEntityLoadedBase machine = placeBattery(helper, pos);

            GolemSync.stateWatchers(level, pos, List.of());
            machine.networkPackNT();
            helper.assertFalse(GolemSync.isPublished(level, pos),
                    "An unwatched machine must not publish, so it never serialised either" + on(backend));

            GolemSync.stateWatchers(level, pos, List.of(watcher.player()));
            drain(watcher, newcomer);
            machine.networkPackNT();
            helper.assertTrue(GolemSync.isPublished(level, pos),
                    "A watched machine must leave its wire in the table" + on(backend));
            helper.assertTrue(sent(watcher) == 1,
                    "One publish must put exactly one sync packet on the watcher's wire" + on(backend));

            machine.networkPackNT();
            helper.assertTrue(sent(watcher) == 0, "An unchanged payload must not go out twice" + on(backend));

            GolemSync.stateWatchers(level, pos, List.of(watcher.player(), newcomer.player()));
            machine.networkPackNT();
            GolemSync.pumpNow();
            helper.assertTrue(sent(newcomer) == 1,
                    "State that stopped changing must still reach a player who arrives later" + on(backend));

            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            helper.assertFalse(GolemSync.isPublished(level, pos),
                    "Breaking the block must drop its entry" + on(backend));
        } finally {
            GolemSync.clearStatedWatchers(level, pos);
            GolemSync.retract(level, pos);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    /**
     * The two backends must be indistinguishable from the client's side. The safe one hands the
     * channel an encoded packet and the accelerated one hands it finished bytes, so the assertion is
     * that both carry a payload equal to the one sent — decoded, for the accelerated case, by the
     * same codec the client decodes with.
     */
    public static void backendsDeliverTheSamePayload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO).offset(3 << 16, 0, 3 << 16);
        BlockEntitySyncPayload sent = new BlockEntitySyncPayload(pos, new byte[] {7, 11, -3, 0, 42});
        GolemTransport.Backend before = GolemTransport.requested();
        try (GameTestHelpers.MockServerPlayer watcher = GameTestHelpers.mockServerPlayer(helper)) {
            GolemSync.stateWatchers(level, pos, List.of(watcher.player()));

            GolemTransport.requested(GolemTransport.Backend.SAFE);
            drain(watcher);
            GolemSync.publish(level, pos, BlockEntitySyncPayload.CODEC, sent);
            GameTestHelpers.Outbound safe = watcher.drainOutbound();
            helper.assertTrue(GameTestHelpers.golemPayloads(safe, SYNC_CHANNEL) == 1,
                    "The safe backend must put exactly one payload on the wire");

            GolemSync.retract(level, pos);
            GolemTransport.requested(GolemTransport.Backend.ACCELERATED);
            helper.assertTrue(GolemTransport.effective() == GolemTransport.Backend.ACCELERATED,
                    "This node must be able to accelerate, or this comparison proves nothing");
            drain(watcher);
            GolemSync.publish(level, pos, BlockEntitySyncPayload.CODEC, sent);
            GameTestHelpers.Outbound fast = watcher.drainOutbound();
            helper.assertTrue(GameTestHelpers.golemPayloads(fast, SYNC_CHANNEL) == 1,
                    "The accelerated backend must put exactly one payload on the wire");
            helper.assertTrue(fast.wires().size() == 1,
                    "The accelerated backend must write finished bytes, not an encoded packet");

            BlockEntitySyncPayload decoded =
                    GameTestHelpers.decodeWire(fast.wires().get(0), BlockEntitySyncPayload.CODEC);
            helper.assertTrue(decoded.pos().equals(sent.pos()),
                    "The accelerated frame must decode to the position that was sent");
            helper.assertTrue(java.util.Arrays.equals(decoded.data(), sent.data()),
                    "The accelerated frame must decode to the bytes that were sent");
            helper.succeed();
        } finally {
            GolemTransport.requested(before);
            GolemSync.clearStatedWatchers(level, pos);
            GolemSync.retract(level, pos);
        }
    }

    /**
     * Acceleration is optional, but on these nodes it is meant to be on. If the packet id cannot be
     * read or a mixin did not land, everything still works by the safe path and every other test here
     * still passes — which is exactly why this has to be asserted separately rather than inferred.
     */
    public static void accelerationIsAvailable(GameTestHelper helper) {
        helper.assertTrue(GolemWire.available(),
                "The custom-payload packet id must be readable on this node");
        helper.assertTrue(GolemWire.packetId() >= 0, "The probed packet id must be a real id");
        helper.assertTrue(GolemTransport.requested() == GolemTransport.Backend.ACCELERATED,
                "Acceleration must be the default");
        helper.assertTrue(GolemTransport.effective() == GolemTransport.Backend.ACCELERATED,
                "The default must actually be in force, not silently degraded");
        try (GameTestHelpers.MockServerPlayer player = GameTestHelpers.mockServerPlayer(helper)) {
            helper.assertTrue(GolemWire.channelOf(player.player()) != null,
                    "The channel mixins must resolve a player's channel, or nothing can be accelerated");
        }
        helper.succeed();
    }

    /**
     * A replay to someone who has just come into view may wait for the end of the tick; a state change
     * may not. Batching is what turns one player's arrival into one wake-up instead of one per sender,
     * so it matters that the batched write really is still queued when the pump returns.
     */
    public static void batchedSendWaitsForTickEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO).offset(5 << 16, 0, 5 << 16);
        GolemTransport.Backend before = GolemTransport.requested();
        try (GameTestHelpers.MockServerPlayer first = GameTestHelpers.mockServerPlayer(helper);
                GameTestHelpers.MockServerPlayer second = GameTestHelpers.mockServerPlayer(helper)) {
            GolemTransport.requested(GolemTransport.Backend.ACCELERATED);
            GolemSync.stateWatchers(level, pos, List.of(first.player()));
            drain(first, second);

            publish(level, pos, (byte) 9);
            helper.assertTrue(sent(first) == 1,
                    "A state change must go out at once, not wait for the end of the tick");

            GolemSync.stateWatchers(level, pos, List.of(first.player(), second.player()));
            GolemSync.pumpHealOnly();
            helper.assertTrue(sent(second) == 0, "A heal must still be queued before the tick ends");

            GolemTransport.endTick();
            helper.assertTrue(sent(second) == 1, "The end of the tick must flush what was queued");
            helper.succeed();
        } finally {
            GolemTransport.requested(before);
            GolemSync.clearStatedWatchers(level, pos);
            GolemSync.retract(level, pos);
        }
    }

    /**
     * Turning acceleration off must change nothing a player can see. This is the whole basis for
     * shipping a bytecode-assisted transport on by default: the fallback is a supported path, not a
     * theoretical one.
     */
    public static void fallsBackWhenUnaccelerated(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO).offset(7 << 16, 0, 7 << 16);
        GolemTransport.Backend before = GolemTransport.requested();
        try (GameTestHelpers.MockServerPlayer watcher = GameTestHelpers.mockServerPlayer(helper)) {
            GolemTransport.requested(GolemTransport.Backend.SAFE);
            helper.assertTrue(GolemTransport.effective() == GolemTransport.Backend.SAFE,
                    "Asking for the safe backend must get the safe backend");

            GolemSync.stateWatchers(level, pos, List.of(watcher.player()));
            drain(watcher);
            publish(level, pos, (byte) 4);
            GameTestHelpers.Outbound outbound = watcher.drainOutbound();
            helper.assertTrue(GameTestHelpers.golemPayloads(outbound, SYNC_CHANNEL) == 1,
                    "The safe backend must still deliver");
            helper.assertTrue(outbound.wires().isEmpty(),
                    "The safe backend must not be writing raw wire buffers");
            helper.succeed();
        } finally {
            GolemTransport.requested(before);
            GolemSync.clearStatedWatchers(level, pos);
            GolemSync.retract(level, pos);
        }
    }

    private static String on(GolemTransport.Backend backend) {
        return " [" + backend + " backend]";
    }

    private static void publish(ServerLevel level, BlockPos pos, byte marker) {
        GolemSync.publish(level, pos, BlockEntitySyncPayload.CODEC,
                new BlockEntitySyncPayload(pos, new byte[] {marker}));
    }

    private static TileEntityLoadedBase placeBattery(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos,
                ModMachineBlocks.machine_battery.get().defaultBlockState());
        if (!(helper.getLevel().getBlockEntity(pos) instanceof TileEntityLoadedBase machine)) {
            throw new IllegalStateException("machine_battery placed no block entity at " + pos);
        }
        return machine;
    }

    private static void drain(GameTestHelpers.MockServerPlayer... players) {
        for (GameTestHelpers.MockServerPlayer player : players) player.outboundPackets();
    }

    private static int sent(GameTestHelpers.MockServerPlayer player) {
        return GameTestHelpers.golemPayloads(player.drainOutbound(), SYNC_CHANNEL);
    }
}
