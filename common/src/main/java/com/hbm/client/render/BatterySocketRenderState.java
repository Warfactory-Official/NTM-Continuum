package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class BatterySocketRenderState extends BlockEntityRenderState {
    public float yaw;

    public @Nullable String batteryPart;

    public @Nullable Identifier batteryTexture;
}
