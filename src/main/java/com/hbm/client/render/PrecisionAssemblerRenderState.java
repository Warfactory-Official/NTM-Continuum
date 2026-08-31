package com.hbm.client.render;

import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;

/** Render state for the legacy precision assembler's four shared striker arms. */
public final class PrecisionAssemblerRenderState extends CompatRenderState {
    public float yaw;
    public boolean showFrame;
    public double ring;
    public final double[] arm = new double[3];
    public final double[] strikers = new double[4];
    public final CompatItemRenderState outputItem = new CompatItemRenderState();
    public boolean showItem;
    public boolean itemIsBlock;
    public boolean itemIs3d;
}
