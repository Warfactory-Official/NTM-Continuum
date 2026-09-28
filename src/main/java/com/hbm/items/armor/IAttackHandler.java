package com.hbm.items.armor;

import dev.golem.core.world.DamageEvents;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code IAttackHandler}: a worn armour piece that sees every {@code LivingAttackEvent} on its player. */
public interface IAttackHandler {

    void handleAttack(DamageEvents.DamageEvent event, ItemStack armor);
}
