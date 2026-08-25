package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.level.Level;

/** Sound and particles. Kept separate from the damage so an explosion can wear any look. */
public interface IExplosionSFX {

    void doEffect(ExplosionVNT explosion, Level world, double x, double y, double z, float size);
}
