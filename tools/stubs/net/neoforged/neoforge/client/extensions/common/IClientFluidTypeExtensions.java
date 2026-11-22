package net.neoforged.neoforge.client.extensions.common;
public interface IClientFluidTypeExtensions { default net.minecraft.resources.ResourceLocation getStillTexture(){return null;} default net.minecraft.resources.ResourceLocation getFlowingTexture(){return null;} default int getTintColor(){return 0;} }
