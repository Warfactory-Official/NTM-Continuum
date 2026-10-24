package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** The settings shown by the shared throughput and priority screen. */
public interface DiodeControl {
    BlockPos getBlockPos();
    long getConfiguredLimit();
    ConnectionPriority getPriority();
    default Component getControlTitle() { return Component.translatable("block.hbm.network_separator"); }
}
