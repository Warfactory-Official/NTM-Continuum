package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Where the dish is pointing this frame, interpolated, and which way the machine was placed. */
public class SatLinkRenderState extends CompatRenderState {
    public float rot;
    public float lift;
    public float offsetX;
    public float offsetZ;
}
