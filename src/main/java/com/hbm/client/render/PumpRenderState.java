package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

public final class PumpRenderState extends CompatRenderState {
    public float yaw;
    public float rotation;
    /** Vertical travel of the piston and the arms riding on it, in blocks. */
    public float lift;
    /** Swing of the arms about the crank pivot, in degrees. */
    public float armAngle;
}
