package com.hbm.tileentity.machine;

import com.hbm.smoke.SmokeChainRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 13-high brick chimney: fly ash only and 0.25 pollution multiplier when pollution is integrated. */
public class TileEntityChimneyBrick extends TileEntityChimneyBase {

    public TileEntityChimneyBrick(BlockPos pos, BlockState state) {
        super(SmokeChainRegistration.CHIMNEY_BRICK.get(), pos, state);
    }

    @Override
    public void spawnParticles() {
        spawnTowerSmoke(worldPosition.getY() + 12D, 0.5F);
    }

    @Override
    public double getPollutionMod() {
        // 1.7: rampant mode swaps in 12.R06_rampantSmokeStackOverride
        return com.hbm.config.MobConfig.rampantMode ? com.hbm.config.MobConfig.rampantSmokeStackOverride : 0.25D;
    }
}
