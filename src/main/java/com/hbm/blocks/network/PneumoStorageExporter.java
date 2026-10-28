package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageExporter;

/**
 * 1.7 {@code PneumoStorageExporter}: {@code BlockMachineBase(Material.iron, 0)}, an unrotated GUI block. Breaking it
 * spills only the output slots (9..17); the nine filters are ghosts and the block entity's removal hook skips them.
 */
public class PneumoStorageExporter extends PneumaticStorageBlockBase {

    public PneumoStorageExporter(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPneumoStorageExporter(pos, state);
    }
}
