package com.hbm.interfaces;

import java.util.UUID;

/**
 * Interface for procedural (multi-tick) explosions. Ported from 1.12.2. The NBT mid-explosion
 * persistence hooks were dropped for the first bomb port (the batched raytracer keeps only a minimal
 * state save); they can be reinstated when long-running explosions need to survive a save/reload.
 *
 * @author mlbv
 */
public interface IExplosionRay {

    /**
     * Called every tick.
     *
     * @param processTimeMs maximum time to spend processing this tick
     */
    void update(int processTimeMs);

    /** Immediately cancels the explosion. */
    void cancel();

    /** @return true if the explosion is finished or canceled. */
    boolean isComplete();

    /** @return true if all rays stop within the maximum radius. */
    boolean isContained();

    void setDetonator(UUID detonator);
}
