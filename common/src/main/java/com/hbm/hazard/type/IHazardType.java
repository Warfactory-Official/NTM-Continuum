package com.hbm.hazard.type;

import java.util.List;

import com.hbm.hazard.modifier.IHazardModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A hazard behaviour (radiation, hot, explosive, …), ported from the fork's {@code IHazardType}.
 * Tooltips build {@link Component}s (translatables resolve client-side) instead of the old
 * pre-resolved strings.
 */
public interface IHazardType {

    /** Ticks between hazard applications — multiply per-tick rates by this (the caller runs every hazardRate ticks). */
    int hazardRate = com.hbm.config.RadiationConfig.hazardRate;

    /**
     * Does the thing. Called by HazardEntry.applyHazard.
     * @param level the final level after calculating all the modifiers
     */
    void onUpdate(LivingEntity target, double level, ItemStack stack);

    /** Updates the hazard for dropped items (explosive/hydroactive). Dropped-item ticking is deferred. */
    void updateEntity(ItemEntity item, double level);

    /** Adds item tooltip info (client side). {@code player} may be null in some tooltip contexts. */
    void addHazardInformation(Player player, List<Component> list, double level, ItemStack stack, List<IHazardModifier> modifiers);
}
