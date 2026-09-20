package com.hbm.blocks.network;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.network.TileEntityPylonMedium;

/**
 * The four medium pylons ({@code red_pylon_medium_wood/steel[_transformer]}): 7 high, three wires. Only
 * the transformer variants take a cable, on the face behind the pylon.
 */
public class PylonMedium extends PylonMultiblockBase implements IEnergyConnectorBlock {

    public final boolean steel;
    public final boolean transformer;

    public PylonMedium(Properties properties, boolean steel, boolean transformer) {
        super(properties);
        this.steel = steel;
        this.transformer = transformer;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPylonMedium(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{6, 0, 0, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }

    /** The mask table is baked facing south, so "behind" is north there and turns with the facing. */
    @Override public int coreMask() { return transformer ? MASK_NORTH : MASK_NONE; }
}
