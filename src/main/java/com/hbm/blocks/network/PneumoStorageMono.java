package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageMono;

/** 1.7 {@code PneumoStorageMono}: three typed cells; type markers and counts ride the dropped item. */
public class PneumoStorageMono extends PneumaticStorageBlockBase {

    public PneumoStorageMono(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPneumoStorageMono(pos, state);
    }
}
