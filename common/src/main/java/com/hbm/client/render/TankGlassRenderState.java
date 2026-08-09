package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class TankGlassRenderState extends BlockEntityRenderState {
    public float yaw;
    /** 0..1 tank fill fraction (Y scale of the Fluid part). */
    public float fillFraction;
    /** RGB colour of the tank fluid. */
    public int fluidColor = 0xFFFFFF;
}
