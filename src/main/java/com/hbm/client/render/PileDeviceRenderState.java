package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Extracted client state shared by the three Chicago Pile devices. */
public final class PileDeviceRenderState extends CompatRenderState {
    public float yaw;
    /** The device's own moving part: rod extraction, ram travel, or fan angle. */
    public float motion;
    /** Whether the loader is carrying a rod on its slider. */
    public boolean loaded;
}
