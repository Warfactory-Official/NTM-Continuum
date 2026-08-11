package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class FELRenderState extends BlockEntityRenderState {
    public float yaw;
    /** Visible beam length in blocks (the trace distance minus the 3-block aperture offset). */
    public int length;
    /** Whether the beam should be drawn at all this frame. */
    public boolean beam;
    /** The wavelength's beam colour, with VISIBLE already resolved to its cycling hue. */
    public int color;
}
