package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeN2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * N² mine. 1.7 NukeN2 (:113, :171) detonates only with all twelve charges loaded and always at the
 * configured {@code n2Radius} (1.12.2 scaled the radius by the charge count).
 */
public class NukeN2 extends NukeBombBlock {

    public NukeN2(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityNukeN2 newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityNukeN2(pos, state);
    }

    @Override
    protected int radiusFor(TileEntityNukeBomb bomb) {
        return BombConfig.n2Radius;
    }

    @Override
    protected boolean noRadiation() {
        return true;
    }

}
