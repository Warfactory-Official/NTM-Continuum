package com.hbm.client.render;

import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;

/** Static Arc Welder render state plus the old floating recipe output. */
public class ArcWelderRenderState extends CompatRenderState {
    public float yaw;
    public final CompatItemRenderState outputItem = new CompatItemRenderState();
    public boolean showItem;
}
