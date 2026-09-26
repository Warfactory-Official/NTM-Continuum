package com.hbm.api.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

// 1.7: api.hbm.block.IFuckingExplode: primes EntityTntNtm, not chain-exploding
public interface IFuckingExplode {

    void explodeEntity(Level level, double x, double y, double z, Entity entity);
}
