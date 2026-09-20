package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render snapshot for the rotary furnace: facing yaw, the piston stroke, and the pour stream. */
public final class RotaryFurnaceRenderState extends CompatRenderState {
    public float yaw;
    public float piston;
    public int dirOrdinal;
    public int pourColor = -1;
    public float pourLen;
}
