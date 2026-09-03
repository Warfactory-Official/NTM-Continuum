package com.hbm.items.food;

import com.hbm.items.machine.ItemChemicalDye;
import net.minecraft.world.item.Item;

/** One flattened, always-edible variant of the legacy crayon item. */
public final class ItemCrayon extends ItemChemicalDye {

    public ItemCrayon(Item.Properties properties, EnumChemDye dye) {
        super(properties, dye, "crayon");
    }
}
