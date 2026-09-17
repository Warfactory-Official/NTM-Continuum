package com.hbm.tileentity.machine;

import com.hbm.blocks.machine.DiFurnaceContent;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The extension's render anchor, and nothing else.
 *
 * <p>1.7 keeps a {@code TileEntityProxyCombo} here to forward the furnace's inventory and tank; that
 * half is answered by {@link com.hbm.blocks.machine.MachineDiFurnaceExtension} instead. What is left is
 * the flue's shape — a truncated pyramid no vanilla block model can state — so the block draws through a
 * block-entity renderer and this holds no data of its own.
 */
public class TileEntityDiFurnaceExtension extends CompatBlockEntity {

    public TileEntityDiFurnaceExtension(BlockPos pos, BlockState state) {
        super(DiFurnaceContent.DI_FURNACE_EXTENSION.get(), pos, state);
    }
}
