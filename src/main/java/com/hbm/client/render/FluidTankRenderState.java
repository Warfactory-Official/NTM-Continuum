package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;
import net.minecraft.resources.Identifier;

/** Render state for {@link FluidTankRenderer}: facing yaw + the fluid-specific body texture. */
public class FluidTankRenderState extends CompatRenderState {
    public float yaw;
    public Identifier bodyTexture;
}
