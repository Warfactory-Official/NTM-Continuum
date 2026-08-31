package com.hbm.client.render;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.render.CompatRenderState;

/** Extracted state shared by the two legacy giant-tank renderers. */
public final class GiantTankRenderState extends CompatRenderState {
    public FluidType fluidType = Fluids.NONE;
    public int fill;
    public int maxFill;
    public long gameTime;
    public float partialTick;
    public float yaw;
    public boolean tilted;
}
