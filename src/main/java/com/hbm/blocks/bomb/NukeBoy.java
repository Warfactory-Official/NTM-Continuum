package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBoy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
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

    /**
     * 1.7 NukeBoy.igniteTestBomb (:130-133) builds its Torex by hand: synced scale exactly 1.5 (not the
     * radius-derived statFac scale) at (x + 0.5, y + 1, z + 0.5).
     */
    @Override
    protected EntityNukeTorex createCloud(Level level, BlockPos pos, int radius) {
        return EntityNukeTorex.statFacExact(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 1.5F, 0);
    }
}
