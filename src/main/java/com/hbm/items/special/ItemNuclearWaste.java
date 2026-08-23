package com.hbm.items.special;

import dev.golem.core.world.CompatItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/** Base for waste whose dropped form must survive damage and despawning. */
public class ItemNuclearWaste extends CompatItem {
    public ItemNuclearWaste(Properties properties) {
        super(properties);
    }

    /** Applies the old EntityItemWaste invariants when a dropped stack enters a level. */
    public static void preserveDroppedEntity(Entity entity) {
        if (entity instanceof ItemEntity dropped && dropped.getItem().getItem() instanceof ItemNuclearWaste) {
            dropped.setInvulnerable(true);
            dropped.setUnlimitedLifetime();
        }
    }
}
