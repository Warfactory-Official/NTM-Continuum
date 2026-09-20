package com.hbm.blocks.generic;

import com.hbm.blocks.machine.ISpotlight;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;
import com.hbm.blocks.machine.SpotlightBeam;

/**
 * 1.7 {@code TritiumLamp}: a redstone lamp pair (off/on are two blocks) whose lit block also lays a
 * {@link SpotlightBeam} of length 8 out of every face. Powered → swaps to the on block at once;
 * unpowered → the on block schedules a 4-tick update and swaps back.
 */
public class TritiumLamp extends CompatBlock implements ISpotlight {

    private final boolean isOn;
    private final int meta;
    private final Supplier<Block> off;
    private final Supplier<Block> on;

    public TritiumLamp(Properties properties, boolean isOn, int meta, Supplier<Block> off, Supplier<Block> on) {
        super(properties);
        this.isOn = isOn;
        this.meta = meta;
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
        updateBeam(level, pos);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (level.isClientSide()) return;
        checkPower(level, pos);
        updateBeam(level, pos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isOn && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, off.get().defaultBlockState(), 2);
        }
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level.isClientSide()) return;
        for (Direction dir : Direction.values()) SpotlightBeams.unpropagateBeam(level, pos, dir);
    }

    private void updateBeam(Level level, BlockPos pos) {
        if (!isOn) return;
        for (Direction dir : Direction.values()) SpotlightBeams.propagateBeam(level, pos, dir, getBeamLength(), meta);
    }

    @Override
    public int getBeamLength() {
        return 8;
    }

    @Override
    public boolean lights(BlockState state, Direction dir) {
        return isOn;
    }
}
