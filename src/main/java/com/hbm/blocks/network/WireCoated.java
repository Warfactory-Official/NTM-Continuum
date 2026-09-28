package com.hbm.blocks.network;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerGraphProvider;
import com.hbm.capability.MachineCaps;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.network.TileEntityWireCoated;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code WireCoated} ("Coated Red Copper Cable"): a full, opaque block that is a plain power conductor
 * ({@code TileEntityCableBaseNT}) on all six faces.
 *
 * <p>1.7 drew it with connected textures ({@code IBlockCT}, {@code red_wire_coated} plus its {@code _ct} sheet,
 * joining to neighbouring coated wire). The port has no connected-texture renderer (see {@code BlockICF},
 * {@code BlockPWR}), so every face shows the unconnected {@code red_wire_coated} tile.
 */
public class WireCoated extends CompatBlock implements TickingBlock, IEnergyConnectorBlock {

    public WireCoated(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWireCoated(pos, state);
    }

    /** A full-cube conductor: every face takes a cable, as 1.7's did. */
    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return true;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            PowerGraph.get(server).addNode(pos.asLong(), PowerGraphProvider.INSTANCE.createData(state),
                    MachineCaps.FACE_MASK_ALL);
        }
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
    }
}
