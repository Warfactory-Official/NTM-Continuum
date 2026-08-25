package com.hbm.entity.missile;

/**
 * A projectile whose pitch does not fit in {@code xRot}.
 *
 * <p>HBM's missiles use 1.12's convention — 0° straight up, −90° horizontal, −180° straight down —
 * and modern {@code Entity.setXRot} clamps to [−90, 90], so a missile stored in the vanilla field
 * flattens out at horizontal and never noses over on the way down. Every missile therefore carries
 * the real angle in its own synchronized field; this is the seam the renderer reads it through, so
 * that one renderer serves both the ballistic missiles and the ABM, which share no superclass.
 */
public interface IMissilePitch {

    /** This projectile's pitch in the convention above, interpolated across the frame. */
    float getMissilePitch(float partialTick);
}
