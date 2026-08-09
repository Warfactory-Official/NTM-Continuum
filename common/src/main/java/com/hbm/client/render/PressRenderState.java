package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for {@link PressRenderer}: the core's facing (yaw) and the stamp-head progress 0..max. */
public class PressRenderState extends BlockEntityRenderState {
    public float yaw;
    public int progress;
}
