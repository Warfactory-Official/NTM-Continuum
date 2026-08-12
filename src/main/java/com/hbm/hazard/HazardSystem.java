package com.hbm.hazard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm.capability.HbmLivingProps;
import com.hbm.config.RadiationConfig;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.transformer.IHazardTransformer;
import com.hbm.hazard.type.IHazardType;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

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

    /** Stack-sensitive transformers, run around every lookup. */
    public static final List<IHazardTransformer> trafos = new ArrayList<>();

    /** Per-item memo of the registered (component-agnostic) HazardData chronology. */
    private static final Map<Item, List<HazardData>> chronologyCache = new ConcurrentHashMap<>();

    public static void register(TagKey<Item> tag, HazardData data) {
        tagMap.put(tag, data);
        chronologyCache.clear();
    }

    public static void register(Item item, HazardData data) {
        itemMap.put(item, data);
        chronologyCache.clear();
    }

    public static void register(Block block, HazardData data) {
        register(block.asItem(), data);
    }

    public static void blacklist(Item item) {
        itemBlacklist.add(item);
        chronologyCache.clear();
    }

    public static void blacklist(TagKey<Item> tag) {
        tagBlacklist.add(tag);
        chronologyCache.clear();
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
     * Synchronous per-entity hazard tick, called from {@code EntityEffectHandler.onUpdate} (server
     * side). Runs every {@link RadiationConfig#hazardRate} ticks: players scan their full inventory
     * (36 main + armor + offhand via {@link Inventory#getItem}), other living entities their equipment.
     */
    public static void tickEntity(LivingEntity entity) {
        if (entity.tickCount % RadiationConfig.hazardRate != 0) return;

        if (entity instanceof Player player) {
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                applyHazards(inv.getItem(i), player);
            }
            // old pipeline reset the neutron buffer every hazard tick (activation is config-off)
            HbmLivingProps.setNeutron(player, 0);
        } else {
            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                applyHazards(entity.getItemBySlot(slot), entity);
            }
        }
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
            hazard.type.addHazardInformation(player, list, hazard.baseLevel, stack, hazard.mods);
        }
    }

    public static void clearCaches() {
        chronologyCache.clear();
    }
}
