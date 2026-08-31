package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;
import net.minecraft.world.item.DyeColor;

/** Render state for the FEnSU's selected skin and X-axis flywheel. */
public class FensuRenderState extends CompatRenderState {
    public float yaw;
    public float rotation;
    public DyeColor color = DyeColor.LIGHT_BLUE;
}
