package com.hbm.blocks.machine;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityCreativeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockCreativeGenerator extends dev.golem.core.world.CompatBlock implements EntityBlock {

    public BlockCreativeGenerator(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCreativeGenerator(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.CREATIVE_GENERATOR.get(), TileEntityCreativeGenerator::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
