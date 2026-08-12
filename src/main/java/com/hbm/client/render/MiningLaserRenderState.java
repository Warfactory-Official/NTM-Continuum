package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;


/** Render state for the mining laser: whether the beam is on and the interpolated aim vector
 * {@code v = target − machine + (0,3,0)} (the old RenderLaserMiner input). */
public class MiningLaserRenderState extends CompatRenderState {
    public boolean beam;
    public double vx;
    public double vy;
    public double vz;
}
