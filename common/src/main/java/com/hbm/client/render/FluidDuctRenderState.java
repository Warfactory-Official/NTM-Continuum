package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

import java.util.Set;

public class FluidDuctRenderState extends BlockEntityRenderState {
    public Set<String> parts = Set.of();

    public int fluidColor = 0;
}
