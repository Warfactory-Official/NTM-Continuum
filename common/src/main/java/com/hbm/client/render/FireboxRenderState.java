package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for the firebox family: facing yaw, whether the fire is burning, and the interpolated
 * door swing angle (0 closed .. 135 open). */
public class FireboxRenderState extends BlockEntityRenderState {
    public float yaw;
    public boolean burning;
    public float doorAngle;
}
