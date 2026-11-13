package com.hbm.client.particle;

/** Collision answers for particles ported from 1.7 and 1.12.2 movers. */
public final class ParticleCollisions {

    private ParticleCollisions() {}

    /**
     * Whether a block clipped this axis. The old offset clipping never touches a free move; vanilla's collision zeroes
     * any move under 1E-7, which is no hit, so a resting particle would bounce on nothing every tick.
     */
    public static boolean blocked(double wanted, double moved) {
        return wanted != moved && Math.abs(wanted) >= 1.0E-7;
    }
}
