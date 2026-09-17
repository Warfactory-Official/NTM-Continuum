package com.hbm.client.render;

import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;


/** Render state for {@link PressRenderer}: the core's facing (yaw), the stamp-head progress 0..max, and
 * the ingredient lying on the anvil under the head. {@link #showItem} gates the item render. */
public class PressRenderState extends CompatRenderState {
    public float yaw;
    public float progress;
    public final CompatItemRenderState pressed = new CompatItemRenderState();
    public boolean showItem;
    /** Whether the pressed item is a 3D block model (vs a flat sprite) — picks how it sits on the anvil. */
    public boolean itemIs3d;
}
