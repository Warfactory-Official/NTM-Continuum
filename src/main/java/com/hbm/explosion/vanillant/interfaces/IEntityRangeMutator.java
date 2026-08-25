package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;

/** Scales the entity-damage radius independently of the block radius. */
public interface IEntityRangeMutator {

    float mutateRange(ExplosionVNT explosion, float range);
}
