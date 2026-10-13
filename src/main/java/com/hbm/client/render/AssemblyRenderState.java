package com.hbm.client.render;

import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;

/** Render state for the animated assembly machine: facing, frame visibility, ring spin and arm angles,
 * plus the selected recipe's output item floating above the machine (the 1.12.2 product display). */
public class AssemblyRenderState extends CompatRenderState {
    public float yaw;
    public boolean showFrame;
    public double ring;
    /** Interpolated [pivot, arm, piston, striker] for each of the two arms. */
    public double[] arm1 = new double[4];
    public double[] arm2 = new double[4];

    /** The recipe output item shown floating above the machine; {@link #showItem} gates rendering. */
    public final CompatItemRenderState outputItem = new CompatItemRenderState();
    public boolean showItem;
    /** Whether the output item is a 3D block model (vs a flat item), picks the float orientation. */
    public boolean itemIs3d;
}
