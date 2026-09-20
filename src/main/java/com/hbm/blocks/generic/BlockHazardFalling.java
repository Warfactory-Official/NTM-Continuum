package com.hbm.blocks.generic;

import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import dev.golem.core.world.CompatFallingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockHazardFalling} ({@code block_yellowcake}, {@code block_fallout}): a gravity block that,
 * when its item carries a RADIATION hazard, pumps a tenth of that value into the chunk every 20 ticks
 * through a self-rescheduling tick (started when placed). The radiation is read from the hazard registry
 * the first time it is needed, since hazards register after blocks.
 */
public class BlockHazardFalling extends CompatFallingBlock {

    private float rad = -1F;

    public BlockHazardFalling(Properties properties) {
        super(properties, BlockHazardFalling::new);
    }

    private float rad() {
        if (rad < 0F) rad = (float) HazardSystem.getHazardLevelFromStack(new ItemStack(this), HazardRegistry.RADIATION) * 0.1F;
        return rad;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && rad() > 0) level.scheduleTick(pos, this, 20);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (rad() > 0) {
            ChunkRadiationManager.proxy.incrementRad(level, pos, rad());
            level.scheduleTick(pos, this, 20);
        }
        super.tick(state, level, pos, random);
    }

    /** 1.7 {@code BlockFalling} default dust colour (opaque black), as {@code BlockFallingBase} keeps it. */
    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return -16777216;
    }
}
