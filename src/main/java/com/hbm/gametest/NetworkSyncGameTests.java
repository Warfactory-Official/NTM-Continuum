package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.net.GolemSync;
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

    public static void init() {}

    /**
     * Healing, reconciliation and eviction, driven from a stated watcher set. No block is placed: the
     * table is keyed by position and does not care whether one is there, and staying out of the plot
     * grid keeps the stated chunk private to this test.
     */
    public static void replicationContract(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO).offset(1 << 16, 0, 1 << 16);
        try (GameTestHelpers.MockServerPlayer first = GameTestHelpers.mockServerPlayer(helper);
                GameTestHelpers.MockServerPlayer second = GameTestHelpers.mockServerPlayer(helper)) {

            GolemSync.stateWatchers(level, pos, List.of(first.player()));
            drain(first, second);

            publish(level, pos, (byte) 1);
            helper.assertTrue(GolemSync.isPublished(level, pos), "A publish must be recorded");
            helper.assertTrue(sent(first) == 1, "The watcher present at publish must be sent to");
            helper.assertTrue(sent(second) == 0, "A player who is not watching must not be sent to");

            GolemSync.stateWatchers(level, pos, List.of(first.player(), second.player()));
            GolemSync.pumpNow();
            helper.assertTrue(sent(second) == 1,
                    "A player arriving after the publish must be healed with the last wire");
            helper.assertTrue(sent(first) == 0,
                    "A player already holding the state must not be sent it again");

            GolemSync.pumpNow();
            GolemSync.pumpNow();
            helper.assertTrue(sent(first) == 0 && sent(second) == 0,
                    "A steady watcher set must produce no further traffic");

            GolemSync.stateWatchers(level, pos, List.of(first.player()));
            GolemSync.pumpNow();
            helper.assertTrue(GolemSync.markedCount(level, pos) == 1,
                    "A departed player's mark must be dropped, or the mark set grows without bound");

            GolemSync.stateWatchers(level, pos, List.of(first.player(), second.player()));
            GolemSync.pumpNow();
            helper.assertTrue(sent(second) == 1,
                    "A player who left and came back must be sent the state again");

            GolemSync.stateWatchers(level, pos, List.of());
            GolemSync.pumpNow();
            helper.assertTrue(GolemSync.markedCount(level, pos) == 0,
                    "An unwatched entry must hold no marks");

            GolemSync.retract(level, pos);
            helper.assertFalse(GolemSync.isPublished(level, pos),
                    "Retraction must drop the entry, or the table grows for the server's life");

            helper.succeed();
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
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        try (GameTestHelpers.MockServerPlayer watcher = GameTestHelpers.mockServerPlayer(helper);
                GameTestHelpers.MockServerPlayer newcomer = GameTestHelpers.mockServerPlayer(helper)) {

            boolean tracked = !level.getChunkSource().chunkMap
                    .getPlayers(level.getChunkAt(pos).getPos(), false).isEmpty();
            helper.assertTrue(GolemSync.isWatched(level, pos) == tracked,
                    "With nothing stated, the gate must answer exactly what the chunk map answers");
            helper.assertTrue(GolemSync.isWatched(level, pos) == tracked,
                    "The per-tick memo must not change the answer on a second ask");

            TileEntityLoadedBase machine = placeBattery(helper, pos);

            GolemSync.stateWatchers(level, pos, List.of());
            machine.networkPackNT();
            helper.assertFalse(GolemSync.isPublished(level, pos),
                    "An unwatched machine must not publish, so it never serialised either");

            GolemSync.stateWatchers(level, pos, List.of(watcher.player()));
            drain(watcher, newcomer);
            machine.networkPackNT();
            helper.assertTrue(GolemSync.isPublished(level, pos),
                    "A watched machine must leave its wire in the table");
            helper.assertTrue(sent(watcher) == 1,
                    "One publish must put exactly one sync packet on the watcher's wire");

            machine.networkPackNT();
            helper.assertTrue(sent(watcher) == 0, "An unchanged payload must not go out twice");

            GolemSync.stateWatchers(level, pos, List.of(watcher.player(), newcomer.player()));
            machine.networkPackNT();
            GolemSync.pumpNow();
            helper.assertTrue(sent(newcomer) == 1,
                    "State that stopped changing must still reach a player who arrives later");

            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            helper.assertFalse(GolemSync.isPublished(level, pos),
                    "Breaking the block must drop its entry");

            helper.succeed();
        } finally {
            GolemSync.clearStatedWatchers(level, pos);
            GolemSync.retract(level, pos);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
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
        return GameTestHelpers.golemPayloads(player.outboundPackets(), SYNC_CHANNEL);
    }
}
