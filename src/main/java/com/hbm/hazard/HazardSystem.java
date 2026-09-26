package com.hbm.hazard;

import com.hbm.api.hazard.HazardEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hbm.capability.HbmLivingProps;
import com.hbm.config.RadiationConfig;
import com.hbm.api.hazard.IHazardModifier;
import com.hbm.hazard.transformer.IHazardTransformer;
import com.hbm.api.hazard.IHazardType;
import dev.golem.core.player.InventoryScan;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * The item-hazard engine, ported from the fork's {@code HazardSystem} with the lookup semantics kept
 * and the machinery modernized:
 *
 * <ul>
 * <li>Ore-dict keys → item <b>tags</b>: {@link #tagMap} replaces {@code oreMap}; evaluated first, in
 * registration order (the old "ore dict keys for this stack" order), then {@link #itemMap}. The exact
 * (item,meta)-stack map is gone — metadata no longer exists; component-sensitive hazards go through
 * {@link IHazardTransformer}s.</li>
 * <li>Override/mutex semantics are unchanged (override clears prior entries; colliding mutex bits
 * yield).</li>
 * <li>Hazards are open: {@link com.hbm.api.hazard.HazardType} builds one out of lambdas with a
 * payload of the author's own type, {@link com.hbm.hazard.transformer.HazardTransformerCustom}
 * attaches one to items the registry never mentions, and every mutator here drops the caches, so
 * all of it works after boot as well as during it.</li>
 * <li>The async scan pipeline (delta tracking, Guava caches, ASM hooks) is replaced by a synchronous
 * {@link #tickEntity} — players scan their full inventory, other living entities their 6 equipment
 * slots, every {@code hazardRate} ticks. The public API is unchanged so the async version can return
 * later. A per-item chronology memo keeps the tag walk O(1) after first lookup.</li>
 * </ul>
 */
public class HazardSystem {

    /** Tag-keyed hazards (the old ore-dict map), evaluated first in registration order. */
    public static final LinkedHashMap<TagKey<Item>, HazardData> tagMap = new LinkedHashMap<>();

    /** Item-keyed hazards, evaluated after tags. */
    public static final HashMap<Item, HazardData> itemMap = new HashMap<>();

    /** Blacklisted items/tags never yield hazards. */
    public static final HashSet<Item> itemBlacklist = new HashSet<>();
    public static final HashSet<TagKey<Item>> tagBlacklist = new HashSet<>();

    /**
     * Stack-sensitive transformers, run around every lookup. Copy-on-write: the list is read on
     * every resolve, from worker threads and from the client's tooltip build, and a mod adding its
     * own transformer after boot must not land in the middle of one.
     */
    public static final List<IHazardTransformer> trafos = new CopyOnWriteArrayList<>();

    /** Per-item memo of the registered (component-agnostic) HazardData chronology. */
    private static final Map<Item, List<HazardData>> chronologyCache = new ConcurrentHashMap<>();

    private static final HazardEntry[] NO_ENTRIES = new HazardEntry[0];

    private static final EquipmentSlot[] EQUIPMENT_SLOTS = EquipmentSlot.values();

    /**
     * Carried hazards, resolved when a slot changes instead of on every hazard tick.
     *
     * <p>The fork scanned the whole inventory on a timer and re-derived each stack's entries from
     * the tag walk every pass, which is the arrangement this class's javadoc called a stand-in for
     * the async pipeline. Golem Core's {@link InventoryScan} is that pipeline: the fold below runs
     * once per slot edit and off the server thread, and the hazard tick walks the memo.
     *
     * <p>Only the entry list is memoised. Modifiers are still evaluated against the live stack at
     * apply time, so a stack's count and its components are read as they are now — a change to
     * either is itself a slot change, so the entries behind them are never stale either.
     */
    private static final InventoryScan<HazardEntry[]> CARRIED = InventoryScan
            .of(HazardSystem::carriedHazards, HazardSystem::applyCarried)
            .every(() -> RadiationConfig.hazardRate);

    /** Puts the carried-hazard scan on the server tick. Called once, from {@link Hazards#init()}. */
    public static void install() {
        CARRIED.install();
    }

    public static void register(TagKey<Item> tag, HazardData data) {
        tagMap.put(tag, data);
        clearCaches();
    }

    public static void register(Item item, HazardData data) {
        itemMap.put(item, data);
        clearCaches();
    }

    public static void register(Block block, HazardData data) {
        register(block.asItem(), data);
    }

    /** Sets or (null) removes {@code item}'s data; returns the previous. */
    public static @org.jspecify.annotations.Nullable HazardData replace(Item item, @org.jspecify.annotations.Nullable HazardData data) {
        HazardData previous = data == null ? itemMap.remove(item) : itemMap.put(item, data);
        clearCaches();
        return previous;
    }

    /** Sets or (null) removes {@code tag}'s data; returns the previous. */
    public static @org.jspecify.annotations.Nullable HazardData replace(TagKey<Item> tag, @org.jspecify.annotations.Nullable HazardData data) {
        HazardData previous = data == null ? tagMap.remove(tag) : tagMap.put(tag, data);
        clearCaches();
        return previous;
    }

    public static void blacklist(Item item) {
        itemBlacklist.add(item);
        clearCaches();
    }

    public static void blacklist(TagKey<Item> tag) {
        tagBlacklist.add(tag);
        clearCaches();
    }

    public static void unblacklist(Item item) {
        if (itemBlacklist.remove(item)) clearCaches();
    }

    public static void unblacklist(TagKey<Item> tag) {
        if (tagBlacklist.remove(tag)) clearCaches();
    }

    public static boolean isItemBlacklisted(ItemStack stack) {
        if (itemBlacklist.contains(stack.getItem())) return true;
        for (TagKey<Item> tag : tagBlacklist) {
            if (stack.is(tag)) return true;
        }
        return false;
    }

    public static boolean isStackHazardous(ItemStack stack) {
        return !getHazardsFromStack(stack).isEmpty();
    }

    /**
     * Full list of applicable HazardEntries for this stack. ORDER: tags (registration order) → item.
     * Entries marked "overriding" delete all fetched entries before them; colliding mutex bits yield.
     * The returned list has been passed through the transformers but not the modifiers.
     */
    public static List<HazardEntry> getHazardsFromStack(ItemStack stack) {
        if (stack.isEmpty() || isItemBlacklisted(stack)) {
            return Collections.emptyList();
        }

        List<HazardData> chronological = chronologyCache.computeIfAbsent(stack.getItem(), item -> {
            List<HazardData> data = new ArrayList<>();
            ItemStack probe = new ItemStack(item);
            for (Map.Entry<TagKey<Item>, HazardData> entry : tagMap.entrySet()) {
                if (probe.is(entry.getKey())) data.add(entry.getValue());
            }
            HazardData itemData = itemMap.get(item);
            if (itemData != null) data.add(itemData);
            return Collections.unmodifiableList(data);
        });

        if (chronological.isEmpty() && trafos.isEmpty()) {
            return Collections.emptyList();
        }

        List<HazardEntry> entries = new ArrayList<>();
        for (IHazardTransformer trafo : trafos) {
            trafo.transformPre(stack, entries);
        }

        int mutex = 0;
        for (HazardData data : chronological) {
            if (data.doesOverride) entries.clear();
            if ((data.getMutex() & mutex) == 0) {
                entries.addAll(data.entries);
                mutex |= data.getMutex();
            }
        }

        for (IHazardTransformer trafo : trafos) {
            trafo.transformPost(stack, entries);
        }

        return Collections.unmodifiableList(entries);
    }

    /** Effective level for a specific hazard type from the stack. */
    public static double getHazardLevelFromStack(ItemStack stack, IHazardType hazard) {
        double totalLevel = 0.0;
        for (HazardEntry entry : getHazardsFromStack(stack)) {
            if (entry.type == hazard) {
                totalLevel += IHazardModifier.evalAllModifiers(stack, null, entry.baseLevel, entry.mods);
            }
        }
        return totalLevel;
    }

    /**
     * Configured RADIATION on a block's item — read directly from {@link #itemMap} (NOT via an
     * {@link ItemStack}), so it's safe to call during mod setup, before item data components are bound
     * ({@code new ItemStack(...)} throws "Components not bound yet" there). The hazard blocks register
     * their radiation directly on the item ({@code HazardRegistry.registerHazardBlocks}), so a plain
     * item‑map lookup is complete for them.
     */
    public static double getRawRadsFromBlock(Block b) {
        HazardData data = itemMap.get(b.asItem());
        if (data == null) return 0.0;
        double total = 0.0;
        for (HazardEntry entry : data.entries) {
            if (entry.type == HazardRegistry.RADIATION) total += entry.baseLevel;
        }
        return total;
    }

    public static double getRawRadsFromStack(ItemStack stack) {
        return getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
    }

    public static void applyHazards(Block b, LivingEntity entity) {
        applyHazards(new ItemStack(b.asItem()), entity);
    }

    /** Grabs all assigned hazards of the given stack and applies their effects to the holder. */
    public static void applyHazards(ItemStack stack, LivingEntity entity) {
        if (stack.isEmpty()) return;
        List<HazardEntry> hazards = getHazardsFromStack(stack);
        for (HazardEntry hazard : hazards) {
            hazard.applyHazard(stack, entity);
        }
    }

    /**
     * Updates hazards emitted by a dropped item. The old ASM hook called this after every vanilla item
     * tick; the cadence is world-time based and the evaluated level is count/modifier sensitive.
     */
    public static void updateDroppedItem(ItemEntity entity) {
        if (entity == null || entity.level().isClientSide() || entity.isRemoved()) return;

        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) return;

        int tickRate = Math.max(1, RadiationConfig.itemHazardDropTickRate);
        if (Math.floorMod(entity.level().getGameTime(), tickRate) != 0) return;

        for (HazardEntry entry : getHazardsFromStack(stack)) {
            entry.applyDropped(entity);
        }
    }

    /** The resolve half of {@link #CARRIED}: the stack's entries, or null when it carries none. */
    private static HazardEntry @Nullable [] carriedHazards(ItemStack stack) {
        List<HazardEntry> hazards = getHazardsFromStack(stack);
        return hazards.isEmpty() ? null : hazards.toArray(NO_ENTRIES);
    }

    /** The apply half of {@link #CARRIED}, on the server thread at {@code hazardRate}. */
    private static void applyCarried(Player player, ItemStack stack, HazardEntry[] hazards) {
        for (HazardEntry hazard : hazards) hazard.applyHazard(stack, player);
    }

    /**
     * Per-entity hazard tick, called from {@code EntityEffectHandler.onUpdate} (server side).
     *
     * <p>A player's carried items are not here — they ride {@link #CARRIED}. What is left is the
     * equipment of everything else alive, six slots that no menu reports and that no scan would have
     * fewer of, and the neutron buffer the old pipeline reset every hazard tick (activation is
     * config-off).
     */
    public static void tickEntity(LivingEntity entity) {
        if (entity.tickCount % RadiationConfig.hazardRate != 0) return;

        if (entity instanceof Player player) {
            HbmLivingProps.setNeutron(player, 0);
            return;
        }
        for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
            applyHazards(entity.getItemBySlot(slot), entity);
        }
    }

    /**
     * Resolves and applies this player's carried hazards immediately, outside the tick cadence — for
     * a caller that has just rewritten an inventory and for the tests that assert the pass end to
     * end without a connected client to raise slot events.
     */
    public static void scanCarried(Player player) {
        CARRIED.scanNow(player);
    }

    /** Adds hazard tooltip info; wired from each loader's item-tooltip event. */
    public static void addHazardInfo(ItemStack stack, Player player, List<Component> list) {
        // Radiation-resistance line for hazmat/armor (mirrors the 1.12.2 client tooltip handler).
        double rad = com.hbm.handler.HazmatRegistry.getResistance(stack);
        rad = ((int) (rad * 100)) / 100D;
        if (rad > 0) {
            list.add(net.minecraft.network.chat.Component.translatable("trait.radResistance", rad)
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
        }

        for (HazardEntry hazard : getHazardsFromStack(stack)) {
            hazard.addInformation(player, list, stack);
        }
        com.hbm.util.ContaminationUtil.addNeutronRadInfo(stack, list);
    }

    /**
     * Drops the per-item memo and re-resolves what every player is carrying. Both halves are needed:
     * the fold's inputs have changed, and the answers already memoised per slot were folded from the
     * old ones.
     */
    public static void clearCaches() {
        chronologyCache.clear();
        CARRIED.invalidate();
    }
}
