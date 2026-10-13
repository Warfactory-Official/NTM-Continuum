package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;


public class RefuelerRenderState extends CompatRenderState {
    public float yaw;
    /** Interpolated tank fill fraction (0-1), drives the fluid part's vertical offset. */
    public double fillLevel;
    /** Tank fluid colour (RGB) for the fluid part tint. */
    public int fluidColor;
}
