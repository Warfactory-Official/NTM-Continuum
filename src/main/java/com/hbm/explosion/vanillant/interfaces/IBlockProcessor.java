package com.hbm.explosion.vanillant.interfaces;

import net.minecraft.world.level.Explosion;
import com.hbm.explosion.vanillant.ExplosionVNT;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Decides what <i>happens</i> to the blocks an {@link IBlockAllocator} picked. */
public interface IBlockProcessor {

    void process(ExplosionVNT explosion, Level world, double x, double y, double z, ObjectOpenHashSet<BlockPos> affectedBlocks);

    default Explosion.BlockInteraction interaction() {
        return Explosion.BlockInteraction.DESTROY_WITH_DECAY;
    }
}
