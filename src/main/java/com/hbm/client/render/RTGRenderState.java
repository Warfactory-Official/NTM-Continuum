package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render state for all three RTG bodies and their four horizontal cable connectors. */
public final class RTGRenderState extends CompatRenderState {
    public boolean polonium;
    public boolean grey;
    public boolean positiveX;
    public boolean negativeX;
    public boolean negativeZ;
    public boolean positiveZ;
}
