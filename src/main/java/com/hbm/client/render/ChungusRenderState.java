package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Extracted client state for the Leviathan's compressor lever and fan. */
public final class ChungusRenderState extends CompatRenderState {
    public float yaw;
    public float lever;
    public float rotor;
    /** An addon's wrecked view is drawn instead ({@link DamagedView}). */
    public boolean damaged;
}
