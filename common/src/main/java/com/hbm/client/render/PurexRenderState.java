package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for the PUREX plant: facing, upper-frame visibility and the interpolated animation counter. */
public class PurexRenderState extends BlockEntityRenderState {
    public float yaw;
    public boolean showFrame;
    public double anim;
}
