package com.hbm.blocks.network;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.network.TileEntityDiode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import java.util.function.Consumer;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/** A directional relay with one output and five input faces. */
public class CableDiode extends CompatBlock implements TickingBlock, IEnergyConnectorBlock, ICapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    /** Installed by the actual client screen registrations. */
    public static Consumer<TileEntityDiode> OPEN_SCREEN;
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerIn().powerOut()
            .roleFaces(TileEntityDiode.class, TileEntityDiode::acceptsFace);

    @Override public MachineCaps caps() { return CAPS; }


    public CableDiode(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // BlockPistonBase.determineOrientation: the face turned towards the placer.
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDiode(pos, state);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof TileEntityDiode diode))
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) OPEN_SCREEN.accept(diode);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
    }
}
