package com.hbm.items.tool;

import net.minecraft.world.item.Item;

/**
 * 1.7 {@code items/tool/ItemCouplingTool}: an empty class. Everything it does lives in
 * {@link com.hbm.entity.train.EntityRailCarBase#interact}, which checks for it by identity.
 */
public class ItemCouplingTool extends Item {

    public ItemCouplingTool(Properties props) {
        super(props);
    }
}
