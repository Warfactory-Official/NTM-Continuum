package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.network.TileEntityPylonLarge;

/**
 * {@code red_pylon_large}: a 3x3, 14-high four-wire pylon with no cable face; a substation ends the line.
 * 1.7 read the placer's yaw at twice the resolution ({@code yaw * 4 / 180}), so its facing advances every
 * 45 degrees -- the model is drawn diagonally for two of the four.
 */
public class PylonLarge extends PylonMultiblockBase {

    public PylonLarge(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPylonLarge(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{13, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 0; }
    @Override public float placementYawSpan() { return 180.0F; }
}
