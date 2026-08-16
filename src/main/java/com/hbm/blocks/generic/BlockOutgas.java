package com.hbm.blocks.generic;

import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.config.GeneralConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Uranium ore that passively outgasses radon and leaves a gas pocket when mined. */
public class BlockOutgas extends dev.golem.core.world.CompatBlock {
    public BlockOutgas(Properties properties) { super(properties); }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!GeneralConfig.enableRadon) return;
        Direction direction = Direction.getRandom(random);
        BlockPos target = pos.relative(direction);
        if (level.getBlockState(target).isAir()) level.setBlock(target, ModNuclearBlocks.gas_radon.get().defaultBlockState(), 2);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide() && GeneralConfig.enableRadon) {
            level.setBlock(pos, ModNuclearBlocks.gas_radon.get().defaultBlockState(), 2);
        }
    }
}
