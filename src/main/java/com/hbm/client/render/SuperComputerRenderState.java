package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render state for the supercomputer: facing, whether it runs, and the scan scroll. */
public final class SuperComputerRenderState extends CompatRenderState {
    public float yaw;
    public boolean running;
    public float scroll;
}
