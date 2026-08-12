package com.hbm.client.render;

import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;

/** Render state for the soldering station: the core's facing (yaw) + the current recipe's output item
 * floating above the machine (the 1.12.2 product display). {@link #showItem} gates the item render. */
public class SolderingRenderState extends CompatRenderState {
    public float yaw;
    public final CompatItemRenderState outputItem = new CompatItemRenderState();
    public boolean showItem;
    /** Whether the output item is a 3D block model (vs a flat item) — picks the float orientation. */
    public boolean itemIs3d;
}
