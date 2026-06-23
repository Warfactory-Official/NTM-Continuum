package com.hbm.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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

    public static void send(CustomPacketPayload payload) {
        if (sender != null) sender.send(payload);
    }
}
