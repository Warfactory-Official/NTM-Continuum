package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for the ore acidizer: facing, the interpolated spinner angle, whether it's
 * processing, and the tank fluid's colour (for the acid overlay). */
public class CrystallizerRenderState extends BlockEntityRenderState {
    public float yaw;
    public float angle;
    public boolean processing;
    public int fluidColor = 0xFFFFFF;
}
