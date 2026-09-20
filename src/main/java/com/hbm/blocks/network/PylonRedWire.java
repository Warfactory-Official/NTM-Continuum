package com.hbm.blocks.network;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.network.TileEntityPylon;

/** {@code red_pylon} / {@code red_pylon_steel}: a 5-high single-wire pylon, cables on the foot's four sides. */
public class PylonRedWire extends PylonMultiblockBase implements IEnergyConnectorBlock {

    public PylonRedWire(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPylon(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{4, 0, 0, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }
    @Override public int coreMask() { return MASK_HORIZONTAL; }
}
