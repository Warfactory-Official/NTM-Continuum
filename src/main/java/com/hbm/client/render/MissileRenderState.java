package com.hbm.client.render;

import dev.golem.core.render.CompatEntityRenderState;

/** What {@link MissileRenderer} needs from a missile: where it is pointing. */
public class MissileRenderState extends CompatEntityRenderState {
    public float yaw;
    public float pitch;
}
