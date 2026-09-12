package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Extracted state for the current FEnSU's flywheel, plasma glow, and arcs. */
public final class BatteryReddRenderState extends CompatRenderState {
    public float yaw;
    public float rotation;
    public float speed;
    public long gameTime;
    public long gameMillis;
    public boolean sparkleNearby;
}
