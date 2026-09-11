package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** What the core's renderer needs: how hard it is running, how full it is, and what colour it burns. */
public class CoreRenderState extends CompatRenderState {
    public int heat;
    public int color;
    public boolean meltdownTick;
    /** The fuel fraction, which sets the orb's size. */
    public float fill;
    public long gameTime;
    public boolean spark;
}
