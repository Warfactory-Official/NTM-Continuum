package com.hbm.client.render;

import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;

/** Render state for the Assembly Factory's two carriages, four arms, and four recipe displays. */
public final class AssemblyFactoryRenderState extends CompatRenderState {
    public float yaw;
    public boolean showFrame;
    public final double[] sliders = new double[2];
    public final double[][] arms = {new double[5], new double[5], new double[5], new double[5]};
    public final CompatItemRenderState[] outputItems = {
            new CompatItemRenderState(), new CompatItemRenderState(),
            new CompatItemRenderState(), new CompatItemRenderState()
    };
    public final boolean[] showItems = new boolean[4];
    public boolean nearby;
    public float sparksU;
}
