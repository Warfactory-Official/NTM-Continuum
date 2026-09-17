package com.hbm.blocks.machine;

import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.registry.TickingBlock;
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

public class BlockCreativeGenerator extends dev.golem.core.world.CompatBlock implements TickingBlock {

    public BlockCreativeGenerator(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCreativeGenerator(pos, state);
    }

}
