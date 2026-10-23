package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** 1.7 {@code ReinforcedLamp}: the two-block redstone lamp (off/on) without the tritium beam. */
public class ReinforcedLamp extends BlockRadResistant {

    private final boolean isOn;
    private final Supplier<Block> off;
    private final Supplier<Block> on;

    public ReinforcedLamp(Properties properties, boolean isOn, Supplier<Block> off, Supplier<Block> on) {
        super(properties);
        this.isOn = isOn;
        this.off = off;
        this.on = on;
    }

    private void checkPower(Level level, BlockPos pos) {
        if (isOn && !level.hasNeighborSignal(pos)) {
            level.scheduleTick(pos, this, 4);
        } else if (!isOn && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, on.get().defaultBlockState(), 2);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.is(this) || level.isClientSide()) return;
        checkPower(level, pos);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (level.isClientSide()) return;
        checkPower(level, pos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isOn && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, off.get().defaultBlockState(), 2);
        }
    }
}
