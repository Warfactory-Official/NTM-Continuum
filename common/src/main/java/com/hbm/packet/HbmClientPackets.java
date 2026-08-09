package com.hbm.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tiny client→server send bridge. Common code (e.g. the recipe-selector GUI) calls {@link #send} to
 * push a {@link CustomPacketPayload} to the server, but common can't reference a loader's client
 * networking API. Each loader's client init installs a {@link Sender} ({@code PacketDistributor
 * .sendToServer} on NeoForge, {@code ClientPlayNetworking.send} on Fabric) via {@link #setSender}.
 */
public final class HbmClientPackets {

    private HbmClientPackets() {}

    @FunctionalInterface
    public interface Sender {
        void send(CustomPacketPayload payload);
    }

    private static Sender sender;

    public static void setSender(Sender s) {
        sender = s;
    }

    /** Sends a payload to the server (no-op if no sender installed, e.g. on a dedicated server). */
    public static void send(CustomPacketPayload payload) {
        if (sender != null) sender.send(payload);
    }
}
