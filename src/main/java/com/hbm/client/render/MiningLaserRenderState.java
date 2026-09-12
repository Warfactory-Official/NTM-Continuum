package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;


/** Render state for the mining laser: whether the beam is on and the interpolated aim vector
 * {@code v = target − machine + (0,3,0)} (the old RenderLaserMiner input). */
public class MiningLaserRenderState extends CompatRenderState {
    public boolean beam;
    public double vx;
    public double vy;
    public double vz;
    /** Where the beam leaves the barrel, 1.5 along the aim, and how far it still has to go. */
    public net.minecraft.world.phys.Vec3 emitter = net.minecraft.world.phys.Vec3.ZERO;
    public net.minecraft.world.phys.Vec3 reach = net.minecraft.world.phys.Vec3.ZERO;
    public float yaw;
    public float pitch;
}
