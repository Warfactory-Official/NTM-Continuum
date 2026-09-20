package com.hbm.blocks.network;

import com.hbm.sound.ModSounds;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.TileEntityCableSwitch;

/** {@code cable_switch}: a hand-toggled cable. ON is 1.7's metadata 1. */
public class CableSwitch extends CompatBlock implements TickingBlock {

    public static final BooleanProperty ON = BooleanProperty.create("on");

    public CableSwitch(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ON, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ON);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCableSwitch(pos, state);
    }

    /** Flips the state, plays the reactor-start click (pitch 1.0 on, 0.85 off) and re-syncs the cell. */
    protected static void setOn(Level level, BlockPos pos, BlockState state, boolean on) {
        level.setBlock(pos, state.setValue(ON, on), 2);
        level.playSound(null, pos, ModSounds.REACTOR_START.get(), SoundSource.BLOCKS, 1.0F, on ? 1.0F : 0.85F);
        if (level.getBlockEntity(pos) instanceof TileEntityCableSwitch te) te.updateState();
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) setOn(level, pos, state, !state.getValue(ON));
        return InteractionResult.SUCCESS;
    }

    /** A block appearing beside an open switch is an endpoint transition nothing else announces. */
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
    }
}
