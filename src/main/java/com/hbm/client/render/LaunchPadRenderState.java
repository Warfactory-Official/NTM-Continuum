package com.hbm.client.render;

import com.hbm.client.render.MissileModels.MissileModel;
import dev.golem.core.render.CompatRenderState;
import org.jspecify.annotations.Nullable;

/** The pad's facing, and the body of whatever missile is loaded (null when the pad is empty). */
public class LaunchPadRenderState extends CompatRenderState {
    public float yaw;
    public @Nullable MissileModel missile;
}
