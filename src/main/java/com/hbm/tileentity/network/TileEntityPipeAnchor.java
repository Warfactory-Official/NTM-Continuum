package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import com.hbm.blocks.network.FluidPipeAnchor;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code TileEntityPipeAnchor}: a one-faced duct mounted on a wall that conducts only through the
 * face it is mounted against. 1.7 also let two anchors be linked by a physical pipeline of up to 10 m
 * ({@code TileEntityPipelineBase}); that pipeline entity is not ported, so the anchor is the single-port
 * duct end only.
 */
public class TileEntityPipeAnchor extends TileEntityFluidDuctVariant {

    public TileEntityPipeAnchor(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.ANCHOR.get(), pos, state);
    }

    /** The open face: opposite the face the anchor was placed against (1.7 {@code orientation(meta).getOpposite()}). */
    public ForgeDirection openSide() {
        BlockState state = getBlockState();
        if (!state.hasProperty(FluidPipeAnchor.FACING)) return ForgeDirection.UNKNOWN;
        Direction facing = state.getValue(FluidPipeAnchor.FACING);
        return ForgeDirection.getOrientation(facing).getOpposite();
    }

    private int faceMask() {
        ForgeDirection side = openSide();
        return side == ForgeDirection.UNKNOWN ? 0 : 1 << side.ordinal();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved(); // mints/retypes the cell with every face open...
        if (!(level instanceof ServerLevel server)) return;
        LevelNodeGraph<DuctData> graph = FluidGraph.get(server);
        graph.updateConnections(getBlockPos().asLong(), faceMask()); // ...and an anchor opens one face only
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir == openSide() && type == this.type;
    }
}
