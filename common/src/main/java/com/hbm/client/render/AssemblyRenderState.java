package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class AssemblyRenderState extends BlockEntityRenderState {
    public float yaw;
    public boolean showFrame;
    public double ring;

    public double[] arm1 = new double[4];
    public double[] arm2 = new double[4];

    public final ItemStackRenderState outputItem = new ItemStackRenderState();
    public boolean showItem;

    public boolean itemIs3d;
}
