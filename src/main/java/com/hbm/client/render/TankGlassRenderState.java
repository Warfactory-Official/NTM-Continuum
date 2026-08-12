package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;


public class TankGlassRenderState extends CompatRenderState {
    public float yaw;
    /** 0..1 tank fill fraction (Y scale of the Fluid part). */
    public float fillFraction;
    /** RGB colour of the tank fluid. */
    public int fluidColor = 0xFFFFFF;
}
