package com.hbm.blocks.network;

import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerGraphProvider;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.network.TileEntityCableGauge;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * An inline power gauge: a cable that shows the network's throughput beside the crosshair.
 * Ported from 1.12.2's {@code BlockCableGauge}.
 *
 * <p><b>Deviation from the fork.</b> Its blockstate declares {@code facing=up} and {@code facing=down}
 * variants that nothing can select — placement uses the horizontal facing and its meta round-trip folds
 * the Y axis back to north — so this port declares the horizontal property alone.
 */
public class BlockCableGauge extends dev.golem.core.world.CompatBlock implements TickingBlock, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockCableGauge(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCableGauge(pos, state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** The gauge conducts, so it mints the same power cell an ordinary cable does. */
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

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityCableGauge gauge)) return null;
        List<String> text = new ArrayList<>();
        text.add(Library.getShortNumber(gauge.deltaLastSecond) + "HE/s");
        return new ILookOverlay.Data("Power Gauge", 0xFFFF00, 0x404000, text);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

}
