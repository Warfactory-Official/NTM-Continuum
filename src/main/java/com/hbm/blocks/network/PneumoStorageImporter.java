package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageImporter;

/** 1.7 {@code PneumoStorageImporter}: {@code BlockMachineBase(Material.iron, 0)}, an unrotated GUI block. */
public class PneumoStorageImporter extends PneumaticStorageBlockBase {

    public PneumoStorageImporter(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPneumoStorageImporter(pos, state);
    }
}
