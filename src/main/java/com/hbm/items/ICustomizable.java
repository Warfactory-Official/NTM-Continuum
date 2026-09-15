package com.hbm.items;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface ICustomizable {
    void customize(ServerPlayer player, ItemStack stack, String... args);
}
