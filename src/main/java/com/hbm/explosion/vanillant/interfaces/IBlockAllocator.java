package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Decides <i>which</i> blocks an explosion touches. */
public interface IBlockAllocator {

    ObjectOpenHashSet<BlockPos> allocate(ExplosionVNT explosion, Level world, double x, double y, double z, float size);
}
