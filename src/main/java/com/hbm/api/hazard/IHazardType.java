package com.hbm.api.hazard;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A hazard behaviour (radiation, hot, explosive, …), ported from the fork's {@code IHazardType}.
 * Tooltips build {@link Component}s (translatables resolve client-side) instead of the old
 * pre-resolved strings.
 *
 * <p>Each of the three behaviours has a second overload taking the entry's <b>payload</b>, and those
 * are what the engine calls; the default forwards to the payload-free one, so every hazard written
 * against the old three-method shape keeps working untouched. A hazard that wants per-registration
 * data — a fuse length, a damage source, a table of effects — is built with {@link HazardType},
 * which types the payload and hands it to its lambdas.
 *
 * <p>The payload belongs to the <em>entry</em>, not to the stack: it is fixed when the hazard is
 * registered and shared by every stack that resolves to that entry. That is what makes it safe to
 * read from the carried-item scan's worker thread, and why attaching one never invalidates a memo.
 * Per-stack state still belongs in the stack's components.
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

    /** As {@link #onUpdate}, with the payload the entry was registered with. */
    default void onUpdate(LivingEntity target, double level, ItemStack stack, @Nullable Object payload) {
        onUpdate(target, level, stack);
    }

    /** As {@link #updateEntity}, with the payload the entry was registered with. */
    default void updateEntity(ItemEntity item, double level, @Nullable Object payload) {
        updateEntity(item, level);
    }

    /** As {@link #addHazardInformation}, with the payload the entry was registered with. */
    default void addHazardInformation(Player player, List<Component> list, double level, ItemStack stack,
            List<IHazardModifier> modifiers, @Nullable Object payload) {
        addHazardInformation(player, list, level, stack, modifiers);
    }
}
