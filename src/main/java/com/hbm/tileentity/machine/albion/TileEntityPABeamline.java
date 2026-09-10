package com.hbm.tileentity.machine.albion;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPASource.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Plain beam pipe: it carries the particle three blocks further along the line and does nothing else.
 * Ported from 1.12.2's {@code albion/TileEntityPABeamline}.
 */
public class TileEntityPABeamline extends BlockEntity implements IParticleUser {

    public TileEntityPABeamline(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_BEAMLINE.get(), pos, state);
    }

    private ForgeDirection getBeamDir() {
        return ForgeDirection.getOrientation(getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset)
                .getRotation(ForgeDirection.DOWN);
    }

    @Override
    public boolean canParticleEnter(Particle particle, ForgeDirection dir, BlockPos pos) {
        ForgeDirection beamlineDir = getBeamDir();
        BlockPos input = worldPosition.offset(-beamlineDir.offsetX, -beamlineDir.offsetY, -beamlineDir.offsetZ);
        return input.equals(pos) && beamlineDir == dir;
    }

    @Override
    public void onEnter(Particle particle, ForgeDirection dir) {
        particle.addDistance(3);
    }

    @Override
    public @Nullable BlockPos getExitPos(Particle particle) {
        ForgeDirection beamlineDir = getBeamDir();
        return worldPosition.offset(beamlineDir.offsetX * 2, beamlineDir.offsetY * 2, beamlineDir.offsetZ * 2);
    }
}
