package com.hbm.tileentity.machine;

import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModBlockEntities;

/**
 * The extension's render anchor, and nothing else.
 *
 * <p>1.7 keeps a {@code TileEntityProxyCombo} here to forward the furnace's inventory and tank; that
 * half is answered by {@link com.hbm.blocks.machine.MachineDiFurnaceExtension} instead. What is left is
 * the flue's shape, a truncated pyramid no vanilla block model can state, so the block draws through a
 * block-entity renderer and this holds no data of its own.
 */
public class TileEntityDiFurnaceExtension extends CompatBlockEntity {

    public TileEntityDiFurnaceExtension(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_DIFURNACE_EXT.get(), pos, state);
    }
}
