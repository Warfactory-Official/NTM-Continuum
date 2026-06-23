package com.hbm.interfaces;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface IControlReceiver {

    void receiveControl(CompoundTag data);

    default boolean hasPermission(Player player) {
        return true;
    }
}
