package com.hbm.gametest;

import com.hbm.Constants;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Respawns;
import dev.golem.core.world.TimeOfDay;
import dev.golem.core.world.WorldAttributes;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

/**
 * Golem Core's world rules against a real bed, respawn and zombie, so every node answers as 26.1 does. A rule answers
 * only at the blocks or players of the test that armed it.
 */
public final class WorldRuleGameTests {

    private WorldRuleGameTests() {}

    private static final int PADDING = 8;
    private static final long NOON = 6000L;
    private static final long MIDNIGHT = 18000L;
    /** A burn roll passes 2.5% of steps at block light 14, so 1000 steps all miss about once in 10^11. */
    private static final int BURN_STEPS = 1000;
    private static final int HOLD_TICKS = 3;
    private static final int TRIES = 5;
    private static GameTestHelper clockHolder;
    private static long heldFrom;

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function DAY_SLEEP =
            TESTS.function("world_rule_day_sleep", 200, PADDING, () -> WorldRuleGameTests::daySleep);
    public static final GameTests.Function RESPAWN_BED_CHECK =
            TESTS.function("world_rule_respawn_bed_check", 40, PADDING, () -> WorldRuleGameTests::respawnBedCheck);
    public static final GameTests.Function FALLBACK_NEEDS_POINT =
            TESTS.function("world_rule_fallback_needs_point", 40, () -> WorldRuleGameTests::fallbackNeedsPoint);
    public static final GameTests.Function MONSTERS_BURN =
            TESTS.function("world_rule_monsters_burn", 200, PADDING, () -> WorldRuleGameTests::monstersBurn);

    public static void init() {}

    private static final Scoped SLEEP = new Scoped();
    private static final Scoped SPAWN = new Scoped();
    private static final Scoped BURN = new Scoped();
    private static final Set<UUID> FALLBACK_WATCH = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> FALLBACKS_ASKED = new ConcurrentHashMap<>();
    private static boolean listening;

    private static synchronized void listen() {
        if (listening) return;
        listening = true;
        WorldAttributes.BED_SLEEP.register(SLEEP);
        WorldAttributes.BED_SPAWN.register(SPAWN);
        WorldAttributes.MONSTERS_BURN.register(BURN);
        Respawns.FALLBACKS.register(player -> {
            if (FALLBACK_WATCH.contains(player.getUUID())) FALLBACKS_ASKED.merge(player.getUUID(), 1, Integer::sum);
            return null;
        });
    }

    /** Records the base value at watched blocks and answers at armed ones; every other block keeps its value. */
    private static final class Scoped implements WorldAttributes.Rule {
        private final Set<BlockPos> watched = ConcurrentHashMap.newKeySet();
        private final Map<BlockPos, Boolean> seen = new ConcurrentHashMap<>();
        private final Map<BlockPos, Boolean> answers = new ConcurrentHashMap<>();

        @Override
        public boolean apply(Level level, BlockPos pos, boolean value) {
            if (watched.contains(pos)) seen.put(pos.immutable(), value);
            Boolean answer = answers.get(pos);
            return answer == null ? value : answer;
        }

        void watch(GameTestHelper helper, BlockPos... positions) {
            for (BlockPos pos : positions) watched.add(pos);
            Runnable forget = () -> {
                for (BlockPos pos : positions) {
                    watched.remove(pos);
                    seen.remove(pos);
                    answers.remove(pos);
                }
            };
            GameTestHelpers.onCompletion(helper, forget, forget);
        }
    }

    private static void daySleep(GameTestHelper helper) {
        listen();
        BlockPos foot = GameTests.paddedCentre(PADDING, 9);
        BlockPos head = placeBed(helper, foot);
        ServerPlayer survival = GameTestHelpers.serverPlayerInLevel(helper, GameType.SURVIVAL);
        ServerPlayer creative = GameTestHelpers.serverPlayerInLevel(helper, GameType.CREATIVE);
        stand(survival, helper.absolutePos(foot.east()));
        stand(creative, helper.absolutePos(foot.west()));
        SLEEP.watch(helper, head);
        inTurn(helper, List.of(new Phase(NOON, () -> {
            SLEEP.answers.remove(head);
            var refused = survival.startSleepInBed(head);
            wake(survival);
            Boolean base = SLEEP.seen.get(head);
            helper.assertTrue(base != null, "a bed attempt must ask BED_SLEEP at the bed");
            if (base) return false;
            helper.assertTrue(refused.left().filter(WorldRuleGameTests::timeOfDayProblem).isPresent(),
                    "with no rule a survival player must be refused by day, got " + refused);

            SLEEP.answers.put(head, true);
            var slept = survival.startSleepInBed(head);
            helper.assertTrue(slept.right().isPresent() && survival.isSleeping(),
                    "a BED_SLEEP rule allowing the bed must let a survival player sleep by day, got " + slept);
            wake(survival);

            Zombie zombie = helper.spawn(EntityType.ZOMBIE, foot.offset(2, 0, -3));
            zombie.setNoAi(true);
            try {
                var unsafe = survival.startSleepInBed(head);
                helper.assertTrue(unsafe.left().filter(Player.BedSleepingProblem.NOT_SAFE::equals).isPresent(),
                        "an allowed day sleep must still be NOT_SAFE with a monster by the bed, got " + unsafe);
                var bold = creative.startSleepInBed(head);
                helper.assertTrue(bold.right().isPresent() && creative.isSleeping(),
                        "a creative player must sleep despite the monster, got " + bold);
                wake(creative);
            } finally {
                zombie.discard();
            }
            return true;
        })));
    }

    private static void respawnBedCheck(GameTestHelper helper) {
        listen();
        BlockPos foot = GameTests.paddedCentre(PADDING, 9);
        BlockPos head = placeBed(helper, foot);
        SPAWN.watch(helper, head);
        try (GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper)) {
            ServerGamePacketListenerImpl listener = mock.player().connection;
            UUID id = mock.player().getUUID();
            FALLBACK_WATCH.add(id);
            try {
                stand(listener.player, helper.absolutePos(foot.east()));
                listener.player.startSleepInBed(head);
                wake(listener.player);
                var point = new Respawns.Point(helper.getLevel().dimension(), head);
                helper.assertTrue(point.equals(Respawns.stored(listener.player)),
                        "the bed must set the spawn point, got " + Respawns.stored(listener.player));
                SPAWN.seen.remove(head);
                mock.drainOutbound();

                ServerPlayer kept = respawn(helper, listener);
                helper.assertTrue(Boolean.TRUE.equals(SPAWN.seen.get(head)),
                        "the respawn must ask BED_SPAWN at the saved bed with vanilla's true, got " + SPAWN.seen.get(head));
                helper.assertTrue(kept.position().distanceTo(Vec3.atBottomCenterOf(head)) < 3.0,
                        "with no rule the player must respawn at the bed, not at " + kept.position());
                helper.assertTrue(point.equals(Respawns.stored(kept)), "a bed respawn must keep the spawn point");
                helper.assertFalse(missingRespawnBlock(mock), "a working bed must not report a missing respawn block");
                helper.assertTrue(FALLBACKS_ASKED.getOrDefault(id, 0) == 0, "FALLBACKS must not be asked when the bed works");

                SPAWN.answers.put(head, false);
                ServerPlayer moved = respawn(helper, listener);
                helper.assertTrue(missingRespawnBlock(mock), "a BED_SPAWN rule refusing the bed must make the respawn ignore it");
                helper.assertTrue(Respawns.stored(moved) == null,
                        "a refused bed must clear the spawn point, got " + Respawns.stored(moved));
                helper.assertTrue(FALLBACKS_ASKED.getOrDefault(id, 0) == 1, "a refused bed must ask FALLBACKS once");
            } finally {
                FALLBACK_WATCH.remove(id);
                FALLBACKS_ASKED.remove(id);
                if (listener.player != mock.player()) helper.getLevel().getServer().getPlayerList().remove(listener.player);
            }
        }
        helper.succeed();
    }

    private static void fallbackNeedsPoint(GameTestHelper helper) {
        listen();
        try (GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper)) {
            ServerGamePacketListenerImpl listener = mock.player().connection;
            UUID id = mock.player().getUUID();
            FALLBACK_WATCH.add(id);
            try {
                helper.assertTrue(Respawns.stored(mock.player()) == null, "a new player must have no spawn point");
                respawn(helper, listener);
                helper.assertTrue(FALLBACKS_ASKED.getOrDefault(id, 0) == 0,
                        "FALLBACKS must not be asked for a player without a spawn point");
            } finally {
                FALLBACK_WATCH.remove(id);
                FALLBACKS_ASKED.remove(id);
                if (listener.player != mock.player()) helper.getLevel().getServer().getPlayerList().remove(listener.player);
            }
        }
        helper.succeed();
    }

    private static void monstersBurn(GameTestHelper helper) {
        listen();
        BlockPos centre = GameTests.paddedCentre(PADDING, 9);
        BlockPos ruled = centre.west(2);
        BlockPos control = centre.east(2);
        helper.setBlock(ruled.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(control.below(), Blocks.STONE.defaultBlockState());
        helper.setBlock(ruled.offset(-1, 1, 0), Blocks.GLOWSTONE.defaultBlockState());
        helper.setBlock(control.offset(1, 1, 0), Blocks.GLOWSTONE.defaultBlockState());
        BlockPos ruledAt = helper.absolutePos(ruled);
        BlockPos controlAt = helper.absolutePos(control);
        BURN.watch(helper, ruledAt, controlAt);
        inTurn(helper, List.of(new Phase(MIDNIGHT, () -> {
            if (!lit(helper, ruledAt) || !lit(helper, controlAt) || burns(helper, control)) return false;
            helper.assertTrue(Boolean.FALSE.equals(BURN.seen.get(controlAt)),
                    "MONSTERS_BURN must get vanilla's false at night, got " + BURN.seen.get(controlAt));
            BURN.answers.put(ruledAt, true);
            helper.assertTrue(burns(helper, ruled), "a MONSTERS_BURN rule answering true must burn a zombie at night");
            return true;
        }), new Phase(NOON, () -> {
            if (!lit(helper, ruledAt) || !lit(helper, controlAt) || !burns(helper, control)) return false;
            helper.assertTrue(Boolean.TRUE.equals(BURN.seen.get(controlAt)),
                    "MONSTERS_BURN must get vanilla's true by day, got " + BURN.seen.get(controlAt));
            BURN.answers.put(ruledAt, false);
            helper.assertFalse(burns(helper, ruled), "a MONSTERS_BURN rule answering false must keep a zombie from burning by day");
            return true;
        })));
    }

    /** A red bed on a stone floor with its foot at {@code foot} and its head north of it; returns the head's absolute position. */
    private static BlockPos placeBed(GameTestHelper helper, BlockPos foot) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -3; dz <= 2; dz++) helper.setBlock(foot.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
        }
        BlockState bed = Blocks.RED_BED.defaultBlockState();
        helper.setBlock(foot, bed);
        helper.setBlock(foot.north(), bed.setValue(BedBlock.PART, BedPart.HEAD));
        return helper.absolutePos(foot.north());
    }

    private static void stand(ServerPlayer player, BlockPos at) {
        player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }

    private static void wake(ServerPlayer player) {
        if (player.isSleeping()) player.stopSleepInBed(true, true);
    }

    /** 26.1 has no NOT_POSSIBLE_NOW: its refusal carries the bed rule's message, so it is told from the shared ones. */
    private static boolean timeOfDayProblem(Player.BedSleepingProblem problem) {
        return !problem.equals(Player.BedSleepingProblem.NOT_SAFE) && !problem.equals(Player.BedSleepingProblem.TOO_FAR_AWAY)
                && !problem.equals(Player.BedSleepingProblem.OBSTRUCTED) && !problem.equals(Player.BedSleepingProblem.OTHER_PROBLEM);
    }

    /** Drops the listener's player to 0 health and respawns it the way the client's respawn button does. */
    private static ServerPlayer respawn(GameTestHelper helper, ServerGamePacketListenerImpl listener) {
        ServerPlayer dead = listener.player;
        dead.setHealth(0.0F);
        listener.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        helper.assertTrue(listener.player != dead, "PERFORM_RESPAWN did not respawn the player");
        return listener.player;
    }

    private static boolean missingRespawnBlock(GameTestHelpers.MockServerPlayer mock) {
        for (Packet<?> packet : mock.outboundPackets()) {
            if (packet instanceof ClientboundGameEventPacket event
                    && event.getEvent() == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE) return true;
        }
        return false;
    }

    private static boolean lit(GameTestHelper helper, BlockPos feet) {
        BlockPos eyes = feet.above();
        return helper.getLevel().getBrightness(LightLayer.BLOCK, eyes) >= 14 && helper.getLevel().canSeeSky(eyes);
    }

    /** Whether a still zombie standing at {@code pos} catches fire from the sun; the zombie is gone afterwards. */
    private static boolean burns(GameTestHelper helper, BlockPos pos) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, pos);
        zombie.setNoAi(true);
        try {
            for (int step = 0; step < BURN_STEPS && zombie.getRemainingFireTicks() <= 0; step++) zombie.aiStep();
            return zombie.getRemainingFireTicks() > 0;
        } finally {
            zombie.discard();
        }
    }

    private record Phase(long time, BooleanSupplier check) {}

    /**
     * Runs each phase at its time of day, holding the overworld clock for {@link #HOLD_TICKS} first: 26.1's attribute
     * cache can lag two ticks. Tests in a batch share the clock, so one test here holds it at a time and puts it back
     * after. A check answering false saw another test move the clock and runs again.
     */
    private static void inTurn(GameTestHelper helper, List<Phase> phases) {
        ServerLevel level = helper.getLevel();
        Runnable release = () -> {
            if (clockHolder != helper) return;
            clockHolder = null;
            TimeOfDay.set(level, heldFrom);
        };
        GameTestHelpers.onCompletion(helper, release, release);
        int[] phase = {0};
        int[] held = {0};
        int[] tries = {TRIES};
        helper.onEachTick(() -> {
            if (phase[0] == phases.size()) return;
            if (clockHolder == null) {
                clockHolder = helper;
                heldFrom = TimeOfDay.get(level);
            }
            if (clockHolder != helper) return;
            Phase current = phases.get(phase[0]);
            if (held[0]++ < HOLD_TICKS) {
                TimeOfDay.set(level, current.time());
                return;
            }
            held[0] = 0;
            if (current.check().getAsBoolean()) {
                tries[0] = TRIES;
                if (++phase[0] < phases.size()) return;
                release.run();
                helper.succeed();
            } else if (--tries[0] == 0) {
                helper.fail("the phase at " + current.time() + " never saw its time of day in " + TRIES
                        + " tries: the clock moved, or the fixture was unlit or roofed");
            }
        });
    }
}
