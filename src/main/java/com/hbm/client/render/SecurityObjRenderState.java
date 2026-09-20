package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/** Render state for {@link SecurityObjRenderer}: which block (picks the mesh and skin) and the wire's yaw. */
public class SecurityObjRenderState extends CompatRenderState {
    public @Nullable Block block;
    public float yaw;
}
