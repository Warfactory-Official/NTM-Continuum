package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.tileentity.ModTerrainBlockEntities;

/**
 * 1.7 {@code BlockClorineSeal} + {@code TileEntityChlorineSeal} ({@code vent_chlorine_seal}): while powered,
 * every tick performs one random walk of up to 50 steps from the seal, laying chlorine gas into every
 * replaceable block it passes and stopping at the first block that is neither gas nor the seal itself.
 */
public class BlockChlorineSeal extends CompatBlock implements TickingBlock {

    public BlockChlorineSeal(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityChlorineSeal(pos, state);
    }

    public static class TileEntityChlorineSeal extends BlockEntity {

        public TileEntityChlorineSeal(BlockPos pos, BlockState state) {
            super(ModTerrainBlockEntities.CHLORINE_SEAL.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityChlorineSeal seal) {
            if (level instanceof ServerLevel world && world.hasNeighborSignal(pos)) spread(world, pos, 0);
        }

        private static void spread(ServerLevel world, BlockPos at, int index) {
            if (index > 50) return;

            BlockState gas = ModTerrainBlocks.chlorine_gas.get().defaultBlockState();
            if (world.getBlockState(at).canBeReplaced()) world.setBlockAndUpdate(at, gas);

            BlockState now = world.getBlockState(at);
            if (!now.is(gas.getBlock()) && !now.is(ModTerrainBlocks.vent_chlorine_seal.get())) return;

            spread(world, at.relative(Direction.values()[world.getRandom().nextInt(6)]), index + 1);
        }
    }
}
