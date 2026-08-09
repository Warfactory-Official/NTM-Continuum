package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

import java.util.Set;

/** Render state for {@link CableObjRenderer}: which OBJ parts of {@code cable_neo.obj} to draw. */
public class CableRenderState extends BlockEntityRenderState {
    public Set<String> parts = Set.of();
}
