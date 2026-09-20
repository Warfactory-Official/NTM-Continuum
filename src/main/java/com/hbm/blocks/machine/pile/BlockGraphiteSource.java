package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.items.ModNuclearMiscItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.machine.pile.TileEntityPileSource;

/** Pile Neutron Source / Pile Fuel (Bred) (LEGACY): the same class serves both source blocks. */
@Deprecated
public class BlockGraphiteSource extends BlockGraphiteDrilledTE {

    public BlockGraphiteSource(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPileSource(pos, state);
    }

    @Override
    protected ItemStack getInsertedItem() {
        return new ItemStack(this == ModNuclearMiscBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()
                ? ModNuclearMiscItems.PILE_ROD_PLUTONIUM.get() : ModNuclearMiscItems.PILE_ROD_SOURCE.get());
    }
}
