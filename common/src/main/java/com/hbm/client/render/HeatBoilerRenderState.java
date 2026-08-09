package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class HeatBoilerRenderState extends BlockEntityRenderState {
    public float yaw;
    public boolean hasExploded;
    /** Pulsing squash factor when the output tank is over 90% (old near-burst wobble); 0 otherwise. */
    public float sine;
}
