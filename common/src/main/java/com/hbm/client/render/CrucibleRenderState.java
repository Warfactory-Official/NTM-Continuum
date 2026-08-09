package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Render state for the crucible: facing, the molten pool level (0 = empty) and the two pour streams. */
public class CrucibleRenderState extends BlockEntityRenderState {
    public float yaw;
    /** ForgeDirection ordinal of the facing (recipe pours this way; waste pours the opposite way). */
    public int dirOrdinal = 3;
    /** Pool surface height above the base (0..0.875), 0 = nothing to draw. */
    public float poolLevel;
    public int wastePourColor = -1;
    public float wastePourLen;
    public int recipePourColor = -1;
    public float recipePourLen;
}
