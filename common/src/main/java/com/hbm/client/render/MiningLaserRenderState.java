package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for the mining laser: whether the beam is on and the interpolated aim vector
 * {@code v = target − machine + (0,3,0)} (the old RenderLaserMiner input). */
public class MiningLaserRenderState extends BlockEntityRenderState {
    public boolean beam;
    public double vx;
    public double vy;
    public double vz;
}
