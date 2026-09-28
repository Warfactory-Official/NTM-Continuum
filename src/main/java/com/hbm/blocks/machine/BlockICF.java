package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityBlockICF;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * The block every laser part becomes once the ICF controller assembles the array (1.7 {@code BlockICF}).
 * Metadata 1 marked a converted laser port; that is the {@link #PORT} property here, and a port cell is the
 * only face a cable can feed the controller through.
 *
 * <p>1.7 drew the shell with connected textures ({@code IBlockCT}, joining to other cells and the controller);
 * like the PWR shell, the cells draw the plain {@code icf_block} / {@code icf_block_port} tiles here. The block
 * drops nothing and has no item: breaking a cell puts the laser part back instead
 * (see {@link TileEntityBlockICF#preRemoveSideEffects}).
 */
public class BlockICF extends CompatBlock implements TickingBlock {

    public static final BooleanProperty PORT = BooleanProperty.create("port");

    public BlockICF(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PORT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PORT);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBlockICF(pos, state);
    }

    /**
     * 1.7 {@code TileEntityBlockICF.updateEntity}'s self-break: removing the cell runs the entity's removal hook,
     * which re-sets the original part. A cell that lost its record simply goes away.
     */
    public static void revert(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAround(server, pos);
    }
}
