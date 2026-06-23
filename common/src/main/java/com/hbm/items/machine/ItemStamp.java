package com.hbm.items.machine;

import net.minecraft.world.item.Item;

public class ItemStamp extends Item {

    private final StampType type;

    public ItemStamp(Properties properties, StampType type) {
        super(properties);
        this.type = type;
    }

    public StampType getStampType() {
        return type;
    }

    public enum StampType {
        FLAT,
        PLATE,
        WIRE,
        CIRCUIT,
        C357,
        C44,
        C50,
        C9,
        PRINTING1,
        PRINTING2,
        PRINTING3,
        PRINTING4,
        PRINTING5,
        PRINTING6,
        PRINTING7,
        PRINTING8
    }
}
