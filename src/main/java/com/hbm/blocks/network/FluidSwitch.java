package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityFluidSwitch;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.network.TileEntityFluidValve;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code FluidSwitch}: the valve, driven by redstone instead of by hand. Powered = open
 * (1.7 metadata 1), with the reactor-start click at pitch 1.0 opening and 0.85 closing.
 */
public class FluidSwitch extends FluidValve {

    public FluidSwitch(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFluidSwitch(pos, state);
    }

    /** No hand toggle: a redstone valve only listens to its signal. */
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        react(state, level, pos);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        react(state, level, pos);
    }

    private void react(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        boolean on = level.hasNeighborSignal(pos);
        boolean open = state.getValue(OPEN);
        boolean update = false;
        if (on && !open) {
            level.setBlock(pos, state.setValue(OPEN, true), 2);
            level.playSound(null, pos, ModSounds.REACTOR_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            update = true;
        }
        if (!on && open) {
            level.setBlock(pos, state.setValue(OPEN, false), 2);
            level.playSound(null, pos, ModSounds.REACTOR_START.get(), SoundSource.BLOCKS, 1.0F, 0.85F);
            update = true;
        }
        if (update) {
            level.updateNeighborsAt(pos, this);
            if (level.getBlockEntity(pos) instanceof TileEntityFluidValve valve) valve.updateState();
        }
    }
}
