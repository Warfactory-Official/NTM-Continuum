package com.hbm.client.render;

import com.hbm.inventory.fluid.FluidHazard;
import dev.golem.core.render.CompatRenderState;
import net.minecraft.resources.Identifier;

/** Render state for {@link FluidTankRenderer}: facing yaw + the fluid-specific body texture. */
public class FluidTankRenderState extends CompatRenderState {
    public float yaw;
    public Identifier bodyTexture;
    /** 1.7 {@code hasExploded}: draw {@code fluidtank_exploded.obj} instead. */
    public boolean exploded;
    /** The stored fluid's NFPA diamond; null when the tank holds nothing (1.7 draws none then). */
    public FluidHazard hazard;
}
