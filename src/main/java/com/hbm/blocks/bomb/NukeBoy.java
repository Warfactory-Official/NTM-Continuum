package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBoy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Little Boy, the fixed five-part MK5 nuclear bomb. */
public class NukeBoy extends NukeBombBlock {

    public NukeBoy(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityNukeBoy newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityNukeBoy(pos, state);
    }

    @Override
    protected int radiusFor(TileEntityNukeBomb bomb) {
        return BombConfig.boyRadius;
    }

    @Override
    protected double cloudOffset() {
        return 0.0D;
    }
}
