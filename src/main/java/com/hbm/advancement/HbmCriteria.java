package com.hbm.advancement;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 triggerAchievement sites a stock trigger cannot observe, grouped by EVENT SHAPE — the call
 * surface of ntm-next's {@code HbmCriteria}.
 *
 * <p>ntm-next registers eleven {@code SimpleCriterionTrigger}s behind these. This port has no
 * advancement subsystem yet (no advancement JSON, no trigger registration facade in Golem Core), so
 * the methods keep their exact signatures and semantics — who is credited, within what radius — and
 * hand the credit to {@link Listener}s instead of a criterion. When the advancement wave lands it
 * registers a listener; until then the call sites in the entity port are complete and inert.
 */
public final class HbmCriteria {

    private HbmCriteria() {
    }

    /** Receives every award the entity code hands out. */
    public interface Listener {
        default void orbit(ServerPlayer player, ItemStack stack) {}

        default void detonation(ServerPlayer player, DetonationTrigger.Kind kind) {}

        default void bossKilled(ServerPlayer player, BossKilledTrigger.Kind kind) {}

        default void satelliteAction(ServerPlayer player, ItemStack stack) {}
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    /**
     * 1.7 {@code EntitySoyuz:107/112}, {@code SatelliteRelay:44} - a payload made orbit; awarded world-wide.
     */
    public static void orbit(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) for (Listener listener : LISTENERS) listener.orbit(player, stack);
    }

    /** 1.7 {@code SatelliteHorizons:54} - a satellite already in orbit carried out its command. */
    public static void satelliteAction(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) for (Listener listener : LISTENERS) listener.satelliteAction(player, stack);
    }

    public static void detonation(ServerPlayer player, DetonationTrigger.Kind kind) {
        for (Listener listener : LISTENERS) listener.detonation(player, kind);
    }

    /** Credits every player inside {@code radius} of {@code boss}, which is how 1.7 awarded these. */
    public static void bossKilled(Entity boss, BossKilledTrigger.Kind kind, double radius) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        for (Player player : level.getPlayers(p -> p.distanceToSqr(boss) <= radius * radius)) {
            if (player instanceof ServerPlayer serverPlayer) {
                for (Listener listener : LISTENERS) listener.bossKilled(serverPlayer, kind);
            }
        }
    }
}
