package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;


/** Render state for {@link PressRenderer}: the core's facing (yaw) and the stamp-head progress 0..max. */
public class PressRenderState extends CompatRenderState {
    public float yaw;
    public int progress;
}
