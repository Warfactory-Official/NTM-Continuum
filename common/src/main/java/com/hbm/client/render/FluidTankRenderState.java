package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;

/** Render state for {@link FluidTankRenderer}: facing yaw + the fluid-specific body texture. */
public class FluidTankRenderState extends BlockEntityRenderState {
    public float yaw;
    public Identifier bodyTexture;
}
