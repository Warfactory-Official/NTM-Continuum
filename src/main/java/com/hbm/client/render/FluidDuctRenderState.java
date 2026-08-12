package com.hbm.client.render;


import dev.golem.core.render.CompatRenderState;
import java.util.Set;

/** Render state for {@link FluidDuctObjRenderer}: which OBJ parts to draw + the carried fluid's tint colour. */
public class FluidDuctRenderState extends CompatRenderState {
    public Set<String> parts = Set.of();
    /** ARGB tint for the fluid overlay pass, or 0 when the duct is untyped (NONE → no overlay). */
    public int fluidColor = 0;
}
