package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class RefuelerRenderState extends BlockEntityRenderState {
    public float yaw;
    /** Interpolated tank fill fraction (0-1) — drives the fluid part's vertical offset. */
    public float fillLevel;
    /** Tank fluid colour (RGB) for the fluid part tint. */
    public int fluidColor;
}
