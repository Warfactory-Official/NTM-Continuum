package com.hbm.handler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

/**
 * The server's view of the copy tool's modifier keys, 1.7 {@code HbmPlayerProps.getKeyPressed(TOOL_ALT /
 * TOOL_CTRL)}. Kept beside the player props rather than in them: nothing about it is saved or synced, it is
 * only the last edge each client reported.
 */
public final class ToolKeybinds {

    public enum EnumToolKey {
        /** 1.7 {@code copyToolAlt}, left Alt: cycle which copied entry is pasted. */
        TOOL_ALT,
        /** 1.7 {@code copyToolCtrl}, left Control: paste into a whole pipe run. */
        TOOL_CTRL;

        public static final EnumToolKey[] VALUES = values();
    }

    private static final Map<UUID, boolean[]> PRESSED = new ConcurrentHashMap<>();

    private ToolKeybinds() {}

    public static boolean getKeyPressed(Player player, EnumToolKey key) {
        boolean[] keys = PRESSED.get(player.getUUID());
        return keys != null && keys[key.ordinal()];
    }

    public static void setKeyPressed(Player player, int key, boolean pressed) {
        if (key < 0 || key >= EnumToolKey.VALUES.length) return;
        PRESSED.computeIfAbsent(player.getUUID(), id -> new boolean[EnumToolKey.VALUES.length])[key] = pressed;
    }

    public static void clear() {
        PRESSED.clear();
    }
}
