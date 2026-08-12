package com.hbm.client.render;


import dev.golem.core.render.CompatRenderState;
import java.util.Set;

/** Render state for {@link CableObjRenderer}: which OBJ parts of {@code cable_neo.obj} to draw. */
public class CableRenderState extends CompatRenderState {
    public Set<String> parts = Set.of();
}
