package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

import java.util.Set;

/** Render state for {@link FluidDuctObjRenderer}: which OBJ parts to draw + the carried fluid's tint colour. */
public class FluidDuctRenderState extends BlockEntityRenderState {
    public Set<String> parts = Set.of();
    /** ARGB tint for the fluid overlay pass, or 0 when the duct is untyped (NONE → no overlay). */
    public int fluidColor = 0;
}
