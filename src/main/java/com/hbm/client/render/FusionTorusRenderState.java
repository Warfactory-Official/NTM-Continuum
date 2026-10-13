package com.hbm.client.render;

import dev.golem.core.render.CompatRenderState;

/** Render state for {@link FusionTorusRenderer}. */
public class FusionTorusRenderState extends CompatRenderState {

    /** Interpolated magnet-ring rotation. */
    public float magnet;
    /** Which of the four arms has something attached (bolt covers come off where it does). */
    public final boolean[] connections = new boolean[4];

    /** True while plasma is burning; gates the glow layers entirely. */
    public boolean burning;
    public float plasmaR = 1F;
    public float plasmaG = 0.2F;
    public float plasmaB = 0.6F;
    /** Breathing alpha of the base plasma sheet. */
    public float plasmaAlpha;

    /** Vertical scroll of the base plasma sheet. */
    public float mainScroll;
    /** Vertical scroll of the bright glow band (two components, as upstream). */
    public float glowScroll;
    /** Horizontal and vertical scroll of the sparkle layer. */
    public float sparkleU;
    public float sparkleV;

    /**
     * Whether the camera is close enough for the glow and sparkle layers. Upstream draws them only
     * within 100 blocks. They are fine detail that costs two extra passes over 768 faces.
     */
    public boolean detailed;
}
