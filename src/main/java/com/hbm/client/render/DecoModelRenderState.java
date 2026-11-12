package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** Render state for {@link DecoModelRenderer}: the block (picks mesh and skin), its yaw and the blinking CRT's frame. */
public class DecoModelRenderState extends CompatRenderState {
    public @Nullable Block block;
    public float yaw;
    /** Blinking CRT: the animation frame of its texture strip. */
    public int frame;
}
