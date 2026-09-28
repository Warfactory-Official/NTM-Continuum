package com.hbm.handler;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.packet.PlayerInformPayload;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.7 {@code HbmPlayerProps.setKeyPressed(TOGGLE_MAGNET)}: on the key's press edge the server flips
 * {@code enableMagnet} and tells the player ("Magnet ON" green / "Magnet OFF" red, info id {@code ID_MAGNET} 6, one
 * second). The lodestone and magnet armour modules read the flag ({@code ItemModLodestone.modUpdate}).
 */
public final class MagnetToggleHandler {

    /** 1.7 {@code ServerProxy.ID_MAGNET}. */
    public static final int ID_MAGNET = 6;

    private static final Map<UUID, Boolean> PRESSED = new ConcurrentHashMap<>();

    private MagnetToggleHandler() {}

    public static void setKeyPressed(ServerPlayer player, boolean pressed) {
        boolean before = Boolean.TRUE.equals(PRESSED.put(player.getUUID(), pressed));
        if (before || !pressed) return;
        var props = HbmPlayerProps.getData(player);
        props.enableMagnet = !props.enableMagnet;
        GolemNetwork.sendToPlayer(player, PlayerInformPayload.CODEC, props.enableMagnet
                ? PlayerInformPayload.of(ID_MAGNET, 1000, ChatFormatting.GREEN, "Magnet ON")
                : PlayerInformPayload.of(ID_MAGNET, 1000, ChatFormatting.RED, "Magnet OFF"));
    }
}
