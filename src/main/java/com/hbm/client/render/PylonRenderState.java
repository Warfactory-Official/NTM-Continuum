package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

import java.util.ArrayList;
import java.util.List;

/** What a pylon renderer draws: which model parts, at what yaw, and the wire halves out of this pylon. */
public class PylonRenderState extends CompatRenderState {

    /** Y rotation of the body in degrees (the old per-meta {@code glRotatef} map). */
    public float yaw;
    /** Connector mounts: the old X then Z rotations. */
    public float rotX, rotZ;
    public boolean steel;
    public boolean transformer;

    /** 0 = uncoloured (the coloured wire texture); otherwise an RGB tint over the greyscale one. */
    public int color;
    public final List<WireHalf> wires = new ArrayList<>();

    /**
     * One half-wire, from this pylon's mount to the midpoint between the two mounts, in coordinates
     * relative to this block. The sag is computed at draw time; the light is sampled per segment here.
     */
    public record WireHalf(double x0, double y0, double z0, double x1, double y1, double z1, int[] light) {}
}
