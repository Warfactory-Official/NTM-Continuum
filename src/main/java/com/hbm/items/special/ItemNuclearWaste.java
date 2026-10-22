package com.hbm.items.special;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/** Waste with authored protection against damage and despawning while dropped. */
public class ItemNuclearWaste extends ItemCustomLore {
    private final boolean protectDropped;

    public ItemNuclearWaste(Properties properties) {
        this(properties, true);
    }

    protected ItemNuclearWaste(Properties properties, boolean protectDropped) {
        super(properties);
        this.protectDropped = protectDropped;
    }

    /** Applies the old EntityItemWaste invariants when a dropped stack enters a level. */
    public static void preserveDroppedEntity(Entity entity) {
        if (entity instanceof ItemEntity dropped && dropped.getItem().getItem() instanceof ItemNuclearWaste waste
                && waste.protectDropped) {
            dropped.setInvulnerable(true);
            dropped.setUnlimitedLifetime();
        }
    }
}
