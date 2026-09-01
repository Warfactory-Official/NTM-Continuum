package com.hbm.tileentity.machine;

import com.hbm.smoke.SmokeChainRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 23-high industrial chimney: fly ash plus soot and 0.1 pollution multiplier when integrated. */
public final class TileEntityChimneyIndustrial extends TileEntityChimneyBase {

    public TileEntityChimneyIndustrial(BlockPos pos, BlockState state) {
        super(SmokeChainRegistration.CHIMNEY_INDUSTRIAL.get(), pos, state);
    }

    @Override
    public void spawnParticles() {
        spawnTowerSmoke(worldPosition.getY() + 22D, 0.75F);
    }

    @Override
    public double getPollutionMod() {
        return 0.1D;
    }

    @Override
    public boolean cpaturesSoot() {
        return true;
    }
}
