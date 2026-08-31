package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Interpolated state for the Ammo Press's four-stage lift/press animation. */
public class AmmoPressRenderState extends CompatRenderState {
    public float yaw;
    public float lift;
    public float press;
    public boolean showBullets;
}
