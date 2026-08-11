package com.hbm.client.render;

import com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class ZirnoxDebrisRenderState extends EntityRenderState {
    public DebrisType type;
    public int entityId;
    public float rotation;
}
