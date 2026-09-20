package com.hbm.blocks.network;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.network.TileEntitySubstation;

/**
 * {@code substation}: a 3x5, 5-high four-wire pylon whose four diagonal corners are conductors, each open
 * on its two outward faces (1.7's {@code makeExtra} proxies plus the {@code createNode} connection list).
 */
public class Substation extends PylonMultiblockBase implements IEnergyConnectorBlock {

    public Substation(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySubstation(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{4, 0, 1, 1, 2, 2}; }
    @Override public int getOffset() { return 1; }

    /** The corner set is rotation-symmetric, so world-axis masks in the south frame are world-axis masks everywhere. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.offset(1, 0, 1), TileEntitySubstation.cornerMask(1));
        visitor.cell(core.offset(1, 0, -1), TileEntitySubstation.cornerMask(2));
        visitor.cell(core.offset(-1, 0, 1), TileEntitySubstation.cornerMask(3));
        visitor.cell(core.offset(-1, 0, -1), TileEntitySubstation.cornerMask(4));
    }
}
