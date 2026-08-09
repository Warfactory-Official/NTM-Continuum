package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Render state for the soldering station: the core's facing (yaw) + the current recipe's output item
 * floating above the machine (the 1.12.2 product display). {@link #showItem} gates the item render. */
public class SolderingRenderState extends BlockEntityRenderState {
    public float yaw;
    public final ItemStackRenderState outputItem = new ItemStackRenderState();
    public boolean showItem;
    /** Whether the output item is a 3D block model (vs a flat item) — picks the float orientation. */
    public boolean itemIs3d;
}
