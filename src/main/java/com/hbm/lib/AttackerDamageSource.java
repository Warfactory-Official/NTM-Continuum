package com.hbm.lib;

import dev.golem.core.world.ItemStacks;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 1.7's death message with and without an attacker, which vanilla sends to one key: {@code message},
 * {@code message.attacker}, {@code message.item}. */
public final class AttackerDamageSource extends DamageSource {

    private final String message;
    private final boolean itemMessage;

    AttackerDamageSource(Holder<DamageType> type, @Nullable Entity direct, @Nullable Entity causing, String message,
                         boolean itemMessage) {
        super(type, direct, causing);
        this.message = message;
        this.itemMessage = itemMessage;
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity died) {
        Entity attacker = getEntity();
        if (attacker == null) return Component.translatable(message, died.getDisplayName());
        ItemStack held = attacker instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;
        if (itemMessage && !held.isEmpty() && ItemStacks.customName(held) != null) {
            return Component.translatable(message + ".item", died.getDisplayName(), attacker.getDisplayName(),
                    held.getDisplayName());
        }
        return Component.translatable(message + ".attacker", died.getDisplayName(), attacker.getDisplayName());
    }
}
