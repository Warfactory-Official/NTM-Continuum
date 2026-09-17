package com.hbm.entity.mob.glyphid;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ntm-next's glyphid path debug toggle. The watcher set and broadcast cadence are kept; the
 * {@code GlyphidPathPayload} client overlay that drew the path is not ported (debug-only), so a broadcast
 * currently reaches nobody. See the entity port manifest.
 */
public final class GlyphidPathDebug {

    public static final int BROADCAST_INTERVAL = 5;

    private static final Set<UUID> WATCHERS = ConcurrentHashMap.newKeySet();

    private GlyphidPathDebug() {
    }

    public static boolean toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        if (WATCHERS.remove(id)) {
            return false;
        }
        WATCHERS.add(id);
        return true;
    }

    public static boolean hasWatchers() {
        return !WATCHERS.isEmpty();
    }

    public static void broadcast(EntityGlyphid glyphid) {
        if (WATCHERS.isEmpty()) return;

        Path path = glyphid.getNavigation().getPath();
        if (path == null || path.getNodeCount() == 0) return;

        // The path overlay packet is not ported; nothing to send until it is.
    }
}
