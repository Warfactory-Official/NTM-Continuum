package com.hbm.blocks.machine.pile;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.special.ItemCell;
import net.minecraft.world.item.ItemStack;

/** Pile Lithium Fuel (Bred) (LEGACY): a tritium cell sitting in the channel. */
@Deprecated
public class BlockGraphiteBreedingProduct extends BlockGraphiteDrilledBase {

    public BlockGraphiteBreedingProduct(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemStack getInsertedItem() {
        return ItemCell.getFullCell(Fluids.TRITIUM);
    }
}
