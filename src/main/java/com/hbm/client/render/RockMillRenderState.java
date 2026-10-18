package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render state for the rock mill: facing, stacking frame and the wheel angle. */
public final class RockMillRenderState extends CompatRenderState {
    public float yaw;
    public boolean frame;
    public float wheel;
}
