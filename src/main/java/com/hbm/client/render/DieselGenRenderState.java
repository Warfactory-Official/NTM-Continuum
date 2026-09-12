package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;


public class DieselGenRenderState extends CompatRenderState {
    public float yaw;
    public boolean running;
    /** The engine block's idle jitter along the machine's own axes, in blocks. */
    public float swingFront;
    public float swingSide;
}
