package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;
import net.minecraft.core.Direction;

/** Extracted six-way pose for the redstone fan. */
public final class FanRenderState extends CompatRenderState {
    public Direction facing = Direction.NORTH;
    public float rotation;
}
