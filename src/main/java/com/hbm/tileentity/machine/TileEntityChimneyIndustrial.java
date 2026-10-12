package com.hbm.tileentity.machine;

import com.hbm.smoke.SmokeChainRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 23-high industrial chimney: fly ash plus soot and 0.1 pollution multiplier when integrated. */
public class TileEntityChimneyIndustrial extends TileEntityChimneyBase {

    public TileEntityChimneyIndustrial(BlockPos pos, BlockState state) {
        super(SmokeChainRegistration.CHIMNEY_INDUSTRIAL.get(), pos, state);
    }

    @Override
    public void spawnParticles() {
        spawnTowerSmoke(worldPosition.getY() + 22D, 0.75F);
    }

    @Override
    public double getPollutionMod() {
        // 1.7: rampant mode swaps in half of 12.R06_rampantSmokeStackOverride
        return com.hbm.config.MobConfig.rampantMode ? com.hbm.config.MobConfig.rampantSmokeStackOverride / 2 : 0.1D;
    }

    @Override
    public boolean cpaturesSoot() {
        return true;
    }
}
