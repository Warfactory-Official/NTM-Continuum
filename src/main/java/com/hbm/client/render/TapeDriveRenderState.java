package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render state for the drive enclosure: facing and one light state per slot. */
public final class TapeDriveRenderState extends CompatRenderState {
    public float yaw;
    public final byte[] tapes = new byte[12];
}
