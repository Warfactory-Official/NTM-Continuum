package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

public final class SteamEngineRenderState extends CompatRenderState {
    public float yaw;
    public float rotor;
    /** Where the connecting rod meets the crank, relative to its rest anchor, in blocks. */
    public float crankX;
    public float crankY;
    /** Swing of the connecting rod about that anchor, in degrees. */
    public float rodAngle;
    /** Travel of the piston along the engine's own axis, in blocks. */
    public float pistonX;
}
