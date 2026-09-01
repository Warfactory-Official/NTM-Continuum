package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render state for the combustion engine's facing, canister tint and interpolated hatch angle. */
public final class CombustionEngineRenderState extends CompatRenderState {
    public float yaw;
    public float doorAngle;
    public boolean canisterTint;
    public int canisterColor;
}
