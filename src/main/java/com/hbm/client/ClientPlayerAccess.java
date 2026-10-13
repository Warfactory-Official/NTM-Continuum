package com.hbm.client;

import com.hbm.inventory.gui.GUIWeaponTable;
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

    /** The original tooltip lists accepted modifiers only while the weapon table screen is open. */
    public static boolean weaponTableOpen() {
        return Minecraft.getInstance().screen instanceof GUIWeaponTable;
    }
}
