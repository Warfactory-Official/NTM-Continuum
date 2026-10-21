package com.hbm.explosion.vanillant;

import com.hbm.entity.explosive.EntityPrimedExplosive;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code Block.onBlockExploded} as the VNT block processor calls it: 1.7's {@code BlockProcessorStandard} handed
 * every broken block to its own {@code onBlockExploded} (with the explosion's exploder). A block that implements
 * this is asked first; answering true means it handled the hit itself and is not removed.
 */
public interface IBlockExplodedVNT {

    boolean onVNTExploded(ServerLevel level, BlockPos pos, BlockState state, ExplosionVNT explosion);

    /** Who set a chain reaction off: a primed explosive's owner, or the exploder itself when it is a living entity. */
    static @Nullable LivingEntity igniter(ExplosionVNT explosion) {
        Entity exploder = explosion.exploder;
        if (exploder instanceof EntityPrimedExplosive primed) return primed.getOwnerEntity();
        return exploder instanceof LivingEntity living ? living : null;
    }
}
