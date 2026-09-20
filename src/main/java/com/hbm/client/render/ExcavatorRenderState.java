package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render snapshot for the large mining drill: facing yaw, crusher and drill spin, drill extension and the chute rubble. */
public final class ExcavatorRenderState extends CompatRenderState {
    public float yaw;
    public float crusher;
    public float drillRotation;
    public float extension;
    public boolean chute;
    public boolean crusherOn;
}
