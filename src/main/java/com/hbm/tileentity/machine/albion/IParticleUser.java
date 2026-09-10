package com.hbm.tileentity.machine.albion;

import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.albion.TileEntityPASource.Particle;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * A block entity the particle accelerator's beam can pass through. The source walks the beam one
 * component at a time: it asks {@link #canParticleEnter} whether the particle arrived from a side this
 * component accepts, hands it over with {@link #onEnter}, then reads {@link #getExitPos} to find the
 * next cell to step to. A null exit ends the run (the detector).
 */
public interface IParticleUser {

    boolean canParticleEnter(Particle particle, ForgeDirection dir, BlockPos pos);

    void onEnter(Particle particle, ForgeDirection dir);

    @Nullable BlockPos getExitPos(Particle particle);
}
