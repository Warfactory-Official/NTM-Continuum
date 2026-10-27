package com.hbm.handler;

import dev.golem.core.hook.Hook;
import java.util.function.Consumer;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Per-tick listeners for dropped items, fired from {@code MixinItemEntity} before vanilla physics so mods like
 * Space's orbit hover can damp motion with the same frozen-entity and ticking-chunk eligibility as the item tick
 * itself. Register once at mod construction; listeners must be side-safe (check {@code level.isClientSide}).
 */
public final class DroppedItemHooks {

    private DroppedItemHooks() {}

    private static final Hook<Consumer<ItemEntity>> BEFORE_TICK = Hook.notifying();

    /** Before vanilla's item physics each tick, with the ticking item. */
    public static void onBeforeTick(Consumer<ItemEntity> listener) {
        BEFORE_TICK.register(listener);
    }

    public static void beforeTick(ItemEntity item) {
        for (Consumer<ItemEntity> listener : BEFORE_TICK) listener.accept(item);
    }
}
