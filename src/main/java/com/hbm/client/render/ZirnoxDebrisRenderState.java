package com.hbm.client.render;

import com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType;
import dev.golem.core.render.CompatEntityRenderState;

public class ZirnoxDebrisRenderState extends CompatEntityRenderState {
    public DebrisType type;
    public int entityId;
    public float rotation;
    /** The tumble about the model diagonal, composed in extract. */
    public final org.joml.Matrix4f tumble = new org.joml.Matrix4f();
}
