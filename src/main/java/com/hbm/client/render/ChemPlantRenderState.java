package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;


/** Render state for the animated chemical plant: facing, frame visibility, the interpolated animation
 * counter, whether it's processing, and the recipe's averaged fluid colour (for the mixer overlay). */
public class ChemPlantRenderState extends CompatRenderState {
    public float yaw;
    public boolean showFrame;
    public double anim;
    public boolean processing;
    public int fluidColor = 0xFFFFFF;
}
