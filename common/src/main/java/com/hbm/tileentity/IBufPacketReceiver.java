package com.hbm.tileentity;

import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IBufPacketReceiver {

    void serialize(RegistryFriendlyByteBuf buf);

    void deserialize(RegistryFriendlyByteBuf buf);
}
