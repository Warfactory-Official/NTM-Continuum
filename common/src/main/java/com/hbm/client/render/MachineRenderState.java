package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for {@link MachineObjRenderer}: the core's facing (yaw) and whether its frame shows. */
public class MachineRenderState extends BlockEntityRenderState {
    public float yaw;
    /** Whether to draw the conditional "Frame" part — true when a block sits above the machine. */
    public boolean showFrame = true;
}
