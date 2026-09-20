package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render snapshot for the conveyor press: facing yaw, piston stroke, stamp presence and belt scroll. */
public final class ConveyorPressRenderState extends CompatRenderState {
    public float yaw;
    public float piston;
    public boolean hasStamp;
    public float beltScroll;
}
