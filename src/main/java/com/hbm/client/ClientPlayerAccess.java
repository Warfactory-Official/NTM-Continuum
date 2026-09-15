package com.hbm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Keeps the client-only Minecraft player lookup out of common item classes. */
public final class ClientPlayerAccess {

    private ClientPlayerAccess() {
    }

    public static @Nullable Player player() {
        return Minecraft.getInstance().player;
    }
}
