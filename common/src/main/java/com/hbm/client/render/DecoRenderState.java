package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/** Render state for {@link DecoObjRenderer}: which deco block (picks the model config) and its facing yaw. */
public class DecoRenderState extends BlockEntityRenderState {
    public @Nullable Block block;
    public float yaw;
}
