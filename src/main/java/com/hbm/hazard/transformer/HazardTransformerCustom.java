package com.hbm.hazard.transformer;

import com.hbm.api.data.HazardDataMaps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.hbm.Constants;
import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardSystem;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Hazards attached to items from outside the registry, as lambdas: the fork's
 * {@code HazardTransformerPostCustom} modernised.
 *
 * <p>{@link HazardSystem#register} answers "this item is radioactive". This answers the cases it
 * cannot: a hazard that depends on the stack's components, one that another mod bolts onto items it
 * does not own, one that is added or taken away at runtime, or a blanket scaling of everything an
 * item already carries.
 *
 * <pre>{@code
 * HazardTransformerCustom.onItem(MyItems.FLASK, (stack, entries) -> {
 *     if (stack.has(MyComponents.CONTENTS)) entries.add(CORROSIVE.entry(4D, stack.get(MyComponents.CONTENTS)));
 * });
 * HazardTransformerCustom.onTag(MyTags.LEAD_LINED, (stack, entries) -> entries.replaceAll(e -> e.clone(0.25D)));
 * HazardTransformerCustom.setMultiplier(Items.NETHER_STAR, 10D);
 * }</pre>
 *
 * <p>Hooks run in the post phase, after overrides and mutexes have settled, so nothing downstream
 * clears what one adds. The order is multiplier, then item hooks, then tag hooks, then predicate
 * hooks, each group in registration order. A hook is handed the live entry list and edits it in
 * place (unlike the fork's copy-and-return shape, there is no pointer-equality merge).
 *
 * <p>Hooks are the one part of the hazard system written by someone else, so a throwing hook is
 * caught rather than allowed to take down a resolve batch, and reported once per hook instead of on
 * every lookup. A tooltip alone would otherwise reprint it every frame.
 *
 * <p>Every mutator drops the caches: a hook added after a stack was resolved would otherwise not be
 * seen until that slot changed again.
 */
public final class HazardTransformerCustom implements IHazardTransformer {

    public static final HazardTransformerCustom INSTANCE = new HazardTransformerCustom();

    private record Matcher(Predicate<ItemStack> when, BiConsumer<ItemStack, List<HazardEntry>> then) {}

    private static final Map<Item, List<BiConsumer<ItemStack, List<HazardEntry>>>> ITEM_HOOKS = new ConcurrentHashMap<>();
    private static final Map<TagKey<Item>, List<BiConsumer<ItemStack, List<HazardEntry>>>> TAG_HOOKS = new ConcurrentHashMap<>();
    private static final List<Matcher> MATCHERS = new CopyOnWriteArrayList<>();
    private static final Map<Item, Double> MULTIPLIERS = new ConcurrentHashMap<>();
    private static final Set<Object> REPORTED = ConcurrentHashMap.newKeySet();

    private HazardTransformerCustom() {}

    static {
        HazardDataMaps.HAZARD_MULTIPLIER.changed.register(HazardSystem::clearCaches);
    }

    public static void onItem(final Item item, final BiConsumer<ItemStack, List<HazardEntry>> hook) {
        ITEM_HOOKS.computeIfAbsent(item, key -> new CopyOnWriteArrayList<>()).add(hook);
        HazardSystem.clearCaches();
    }

    public static boolean removeItem(final Item item, final BiConsumer<ItemStack, List<HazardEntry>> hook) {
        return remove(ITEM_HOOKS, item, hook);
    }

    public static void onTag(final TagKey<Item> tag, final BiConsumer<ItemStack, List<HazardEntry>> hook) {
        TAG_HOOKS.computeIfAbsent(tag, key -> new CopyOnWriteArrayList<>()).add(hook);
        HazardSystem.clearCaches();
    }

    public static boolean removeTag(final TagKey<Item> tag, final BiConsumer<ItemStack, List<HazardEntry>> hook) {
        return remove(TAG_HOOKS, tag, hook);
    }

    /** A hook for stacks no item or tag key can select: component state, durability, enchantments. */
    public static void onMatching(final Predicate<ItemStack> when, final BiConsumer<ItemStack, List<HazardEntry>> hook) {
        MATCHERS.add(new Matcher(when, hook));
        HazardSystem.clearCaches();
    }

    public static boolean removeMatching(final BiConsumer<ItemStack, List<HazardEntry>> hook) {
        final boolean removed = MATCHERS.removeIf(matcher -> matcher.then() == hook);
        if (removed) HazardSystem.clearCaches();
        return removed;
    }

    /** Scales every entry this item already carries, before any hook runs. */
    public static void setMultiplier(final Item item, final double multiplier) {
        MULTIPLIERS.put(item, multiplier);
        HazardSystem.clearCaches();
    }

    public static void clearMultiplier(final Item item) {
        if (MULTIPLIERS.remove(item) != null) HazardSystem.clearCaches();
    }

    public static boolean hasMultiplier(final Item item) {
        return MULTIPLIERS.containsKey(item);
    }

    public static double getMultiplier(final Item item) {
        return MULTIPLIERS.getOrDefault(item, 1D);
    }

    /** Drops every hook and multiplier. For tests and for a full hazard reload. */
    public static void clear() {
        ITEM_HOOKS.clear();
        TAG_HOOKS.clear();
        MATCHERS.clear();
        MULTIPLIERS.clear();
        REPORTED.clear();
        HazardSystem.clearCaches();
    }

    private static <K> boolean remove(final Map<K, List<BiConsumer<ItemStack, List<HazardEntry>>>> hooks,
            final K key, final BiConsumer<ItemStack, List<HazardEntry>> hook) {
        final List<BiConsumer<ItemStack, List<HazardEntry>>> list = hooks.get(key);
        if (list == null || !list.remove(hook)) return false;
        if (list.isEmpty()) hooks.remove(key, list);
        HazardSystem.clearCaches();
        return true;
    }

    @Override
    public void transformPost(final ItemStack stack, final List<HazardEntry> entries) {
        if (stack.isEmpty()) return;

        final Item item = stack.getItem();
        Double multiplier = MULTIPLIERS.get(item);
        // the data form (hbm:hazard_multiplier); a code multiplier set at runtime wins
        if (multiplier == null) multiplier = HazardDataMaps.HAZARD_MULTIPLIER.get(item);
        if (multiplier != null && !entries.isEmpty() && !multiplier.isNaN() && multiplier != 1D) {
            final List<HazardEntry> scaled = new ArrayList<>(entries.size());
            for (final HazardEntry entry : entries) scaled.add(entry.clone(multiplier));
            entries.clear();
            entries.addAll(scaled);
        }

        run(ITEM_HOOKS.get(item), stack, entries);

        if (!TAG_HOOKS.isEmpty()) {
            for (final Map.Entry<TagKey<Item>, List<BiConsumer<ItemStack, List<HazardEntry>>>> tag : TAG_HOOKS.entrySet()) {
                if (stack.is(tag.getKey())) run(tag.getValue(), stack, entries);
            }
        }

        for (final Matcher matcher : MATCHERS) {
            if (test(matcher, stack)) run(matcher.then(), stack, entries);
        }
    }

    private static void run(final @Nullable List<BiConsumer<ItemStack, List<HazardEntry>>> hooks,
            final ItemStack stack, final List<HazardEntry> entries) {
        if (hooks == null) return;
        for (final BiConsumer<ItemStack, List<HazardEntry>> hook : hooks) run(hook, stack, entries);
    }

    private static void run(final BiConsumer<ItemStack, List<HazardEntry>> hook, final ItemStack stack,
            final List<HazardEntry> entries) {
        try {
            hook.accept(stack, entries);
        } catch (Throwable t) {
            report(hook, stack, t);
        }
    }

    private static boolean test(final Matcher matcher, final ItemStack stack) {
        try {
            return matcher.when().test(stack);
        } catch (Throwable t) {
            report(matcher.when(), stack, t);
            return false;
        }
    }

    private static void report(final Object hook, final ItemStack stack, final Throwable t) {
        if (REPORTED.add(hook)) {
            Constants.LOG.error("Custom hazard hook {} failed on {}; later failures from it are not reported",
                    hook.getClass().getName(), stack, t);
        }
    }
}
