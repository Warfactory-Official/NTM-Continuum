package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Render state for {@link BatterySocketRenderer}: facing yaw + the inserted battery's part/texture. */
public class BatterySocketRenderState extends BlockEntityRenderState {
    public float yaw;
    /** The OBJ part to draw for the inserted item ("Battery" / "Capacitor"), or null if empty. */
    public @Nullable String batteryPart;
    /** Texture for that part, or null if no battery is inserted. */
    public @Nullable Identifier batteryTexture;
}
