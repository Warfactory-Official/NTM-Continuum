package com.hbm.hazard.transformer;

import java.util.List;
import java.util.function.BiConsumer;

import com.hbm.hazard.HazardEntry;
import net.minecraft.world.item.ItemStack;

/**
 * Stack-sensitive hazard rewriting (runs on every lookup, before/after the registered data is
 * collected). Ported from the fork's {@code IHazardTransformer}. The ForgeFluid transformer is
 * deferred with its system.
 *
 * <p>Both phases default to doing nothing, so a transformer that only cares about one of them is a
 * lambda: {@link #post} for the usual case (nothing downstream can clear what it adds, because
 * overrides have already run) and {@link #pre} for entries an overriding {@link com.hbm.hazard.HazardData}
 * is allowed to wipe.
 *
 * <p>Transformers run inside the carried-item resolve, which happens off the server thread. Read the
 * stack and write the list; do not touch the level, the holder, or anything else from here.
 */
public interface IHazardTransformer {

    /** Whether a carried applicator must resolve mutable nested/component state again at use time. */
    default boolean readsLiveStorage() { return false; }

    /** Narrows a live transformer to stacks whose state it actually reads. */
    default boolean readsLiveStorage(ItemStack stack) { return readsLiveStorage(); }

    default void transformPre(ItemStack stack, List<HazardEntry> entries) {}

    default void transformPost(ItemStack stack, List<HazardEntry> entries) {}

    /** A transformer that only seeds entries before the registered data folds in. */
    static IHazardTransformer pre(final BiConsumer<ItemStack, List<HazardEntry>> fn) {
        return new IHazardTransformer() {
            @Override
            public void transformPre(final ItemStack stack, final List<HazardEntry> entries) {
                fn.accept(stack, entries);
            }
        };
    }

    /** A transformer that only rewrites the finished entry list. */
    static IHazardTransformer post(final BiConsumer<ItemStack, List<HazardEntry>> fn) {
        return new IHazardTransformer() {
            @Override
            public void transformPost(final ItemStack stack, final List<HazardEntry> entries) {
                fn.accept(stack, entries);
            }
        };
    }
}
