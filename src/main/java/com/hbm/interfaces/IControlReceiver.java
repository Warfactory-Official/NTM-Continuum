package com.hbm.interfaces;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * A BlockEntity that accepts small client→server "control" events — the 26.1 equivalent of HBM's
 * 1.12.2 {@code IControlReceiver}/{@code NBTControlPacket}. The client sends a {@link CompoundTag} of
 * arbitrary control data (e.g. the recipe a player picked in the recipe-selector GUI) via
 * {@link com.hbm.packet.ControlEventPayload}; the server resolves the tile, checks {@link #hasPermission}
 * and applies it in {@link #receiveControl}.
 */
public interface IControlReceiver {

    /** Applies a control event. Runs server-side on the main thread. */
    void receiveControl(CompoundTag data);

    /**
     * 1.7's two-argument {@code receiveControl(EntityPlayer, NBTTagCompound)}: the same event, with the
     * player who sent it, for receivers whose reaction involves them (opening a second menu, a sound played
     * at the player). The packet handler always calls this one; the default forwards to the tag-only form.
     */
    default void receiveControl(Player player, CompoundTag data) {
        receiveControl(data);
    }

    /**
     * Whether {@code player} is allowed to control this tile. Receivers that are controlled through a
     * menu must explicitly verify that the player's current menu owns this receiver. Receivers with an
     * intentional remote-control policy may provide their own server-side check.
     */
    default boolean hasPermission(Player player) {
        return false;
    }
}
