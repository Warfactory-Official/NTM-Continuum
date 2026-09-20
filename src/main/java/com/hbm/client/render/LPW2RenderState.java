package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** The LPW-2's per-frame animation curves, all sampled from world time. */
public final class LPW2RenderState extends CompatRenderState {
    public float yaw;
    public float sway;
    public float h;
    public float v;
    public float piston;
    public float rotor;
    public float turbine;
    public float cover;
    public float sx;
    public float sy;
    public float errorScroll;
}
