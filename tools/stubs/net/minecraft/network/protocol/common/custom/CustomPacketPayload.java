package net.minecraft.network.protocol.common.custom;
public interface CustomPacketPayload { record Type<T extends CustomPacketPayload>(net.minecraft.resources.ResourceLocation id){} Type<? extends CustomPacketPayload> type(); }
