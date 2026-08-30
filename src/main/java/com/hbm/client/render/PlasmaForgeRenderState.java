package com.hbm.client.render;

import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;

/** Render state for the plasma forge body, rotor arms, plasma layers and floating recipe output. */
public class PlasmaForgeRenderState extends CompatRenderState {
    public float yaw;
    public boolean connected;
    public double ring;
    public double[] striker = new double[6];
    public double[] jet = new double[4];
    public boolean burning;
    public float plasmaR;
    public float plasmaG;
    public float plasmaB;
    public float plasmaAlpha;
    public float mainScroll;
    public float glowScroll;
    public float secondGlowScroll;
    public boolean jetPlume;
    public float jetPlumeLength = 1F;
    public boolean showBeam;
    public float beamScroll;
    public final CompatItemRenderState outputItem = new CompatItemRenderState();
    public boolean showItem;
    public boolean itemIsBlock;
    public boolean itemIsGui3d;
    public float itemBob;
}
