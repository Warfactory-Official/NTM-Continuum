package net.neoforged.neoforge.network.handling;
public interface IPayloadContext { void enqueueWork(Runnable r); net.minecraft.world.entity.player.Player player(); }
