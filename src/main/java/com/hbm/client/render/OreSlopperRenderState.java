package com.hbm.client.render;

import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;

/** Render snapshot for the ore slopper: facing yaw, slider/bucket travel, blade and fan spin, and the lifted ore chunk. */
public final class OreSlopperRenderState extends CompatRenderState {
    public float yaw;
    public float slide;
    public float extend;
    public float blades;
    public float fan;
    public boolean lifting;
    public final CompatItemRenderState ore = new CompatItemRenderState();
}
