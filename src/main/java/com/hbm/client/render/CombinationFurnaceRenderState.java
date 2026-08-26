package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Extracted state for the combination furnace's static model and animated flame overlay. */
public final class CombinationFurnaceRenderState extends CompatRenderState {
    public boolean burning;
    public long gameTime;
}
