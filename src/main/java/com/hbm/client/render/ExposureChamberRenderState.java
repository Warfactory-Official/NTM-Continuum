package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Client render state for the Exposure Chamber's animated magnets, core and beams. */
public final class ExposureChamberRenderState extends CompatRenderState {
    public float yaw;
    public float rotation;
    public boolean isOn;
    public long gameTime;
    public long gameMillis;
    public float partialTick;
}
