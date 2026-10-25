package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntitySoyuzStruct;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The seed block: an ordinary block whose entity watches for the complex to be finished around it. */
public class BlockSoyuzStruct extends CompatBlock implements TickingBlock {

    public BlockSoyuzStruct(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySoyuzStruct(pos, state);
    }
}
