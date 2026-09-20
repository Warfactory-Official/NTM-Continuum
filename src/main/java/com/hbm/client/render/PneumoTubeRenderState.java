package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** What {@link PneumoTubeRenderer} reads off a tube: its arm mask and the three nozzle sets. */
public class PneumoTubeRenderState extends CompatRenderState {
    /** {@code 1 << Direction.ordinal()} bits of the faces with an arm. */
    public int arms;
    /** The insertion face, or -1. Draws the {@code pneumatic_tube_in} head. */
    public int insertion = -1;
    /** The ejection face, or -1. Draws the {@code pneumatic_tube_out} head. */
    public int ejection = -1;
    /** Faces with a compressed-air intake nozzle (1.7 canConnectToAir). */
    public int air;
    /** 1.7: a tube with any nozzle never takes the seamless straight form. */
    public boolean hasConnections;
}
