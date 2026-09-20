package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render snapshot for the annihilator: facing yaw, the roller spin and the belt scroll. */
public final class AnnihilatorRenderState extends CompatRenderState {
    public float yaw;
    public float roller;
    public float belt;
}
