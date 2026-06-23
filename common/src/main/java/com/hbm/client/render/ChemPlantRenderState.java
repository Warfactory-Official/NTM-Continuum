package com.hbm.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class ChemPlantRenderState extends BlockEntityRenderState {
    public float yaw;
    public boolean showFrame;
    public double anim;
    public boolean processing;
    public int fluidColor = 0xFFFFFF;
}
