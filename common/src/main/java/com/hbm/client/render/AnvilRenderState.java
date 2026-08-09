package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/** Render state for {@link AnvilObjRenderer}: which anvil block (picks the model/texture) and its yaw. */
public class AnvilRenderState extends BlockEntityRenderState {
    public @Nullable Block block;
    public float yaw;
}
