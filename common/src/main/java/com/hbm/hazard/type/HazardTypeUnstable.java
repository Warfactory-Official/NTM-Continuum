package com.hbm.hazard.type;

import java.util.List;

import com.hbm.hazard.modifier.IHazardModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Timed self-detonating items (old {@code HazardTypeUnstable}). The old class carried a per-stack NBT
 * timer that spawned a {@code EntityNukeExplosionMK5} when it ran out; no ported item uses it yet, so
 * the behaviour is a stub kept for API shape — port the timer/detonation when the first unstable item
 * (e.g. the Solinium grenade chain) lands.
 */
public class HazardTypeUnstable implements IHazardType {

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
        // TODO(items): per-stack decay timer -> nuclear detonation
    }

    @Override
    public void updateEntity(ItemEntity item, double level) {
        // TODO(items): dropped-item decay timer
    }

    @Override
    public void addHazardInformation(Player player, List<Component> list, double level, ItemStack stack, List<IHazardModifier> modifiers) {
        list.add(Component.translatable("trait.unstable").withStyle(ChatFormatting.DARK_RED));
    }
}
