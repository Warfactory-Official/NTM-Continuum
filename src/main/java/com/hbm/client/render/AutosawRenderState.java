package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render snapshot for the autosaw's interpolated arms, blade, and engine motion. */
public final class AutosawRenderState extends CompatRenderState {
    public float yaw;
    public float angle;
    public float spin;
    public double engine;
}
