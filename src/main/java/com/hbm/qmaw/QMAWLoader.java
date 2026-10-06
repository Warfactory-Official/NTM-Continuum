package com.hbm.qmaw;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemFluidId;
import dev.golem.core.Ids;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Discovers and indexes QMAW pages from the resolved client resource-pack view. */
public final class QMAWLoader {
    public static final String EN_US = "en_us";

    /** Kept as the old public names so client integrations can inspect the live index. */
    public static volatile Map<String, QuickManualAndWiki> qmaw = Map.of();
    public static volatile List<String> unresolvedTriggers = List.of();
    public static volatile int sourceFiles;
    public static volatile int indexedTriggers;

    private static volatile List<IndexedTrigger> exactTriggers = List.of();
    private static volatile Map<Item, QuickManualAndWiki> wildcardTriggers = Map.of();
    private static volatile Map<String, QuickManualAndWiki> wildcardPrefixes = Map.of();

    private QMAWLoader() {}

    /** Rebuilds the index after a complete client resource reload. */
    public static void reload(ResourceManager resources) {
        long start = System.currentTimeMillis();
        Map<String, QuickManualAndWiki> pages = new LinkedHashMap<>();
        List<String> unresolved = new ArrayList<>();
        int files = 0;

        // ResourceManager.listResources has already applied pack priority: one highest-priority Resource
        // is returned for each path. Do not walk jars directly, or pack overrides would be lost.
        List<Map.Entry<Identifier, Resource>> resourcesToRead = new ArrayList<>(
                resources.listResources("manual", path -> path.getPath().endsWith(".json")).entrySet());
        resourcesToRead.sort(Comparator.comparing(entry -> entry.getKey().toString()));

        for (Map.Entry<Identifier, Resource> entry : resourcesToRead) {
            files++;
            Identifier location = entry.getKey();
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) throw new IllegalArgumentException("root is not an object");
                QuickManualAndWiki page = parse(location.toString(), root.getAsJsonObject(), unresolved);
                if (page != null) {
                    if (pages.containsKey(page.name)) {
                        Constants.LOG.info("[QMAW] Page '{}' overridden by {}", page.name, location);
                    }
                    pages.put(page.name, page);
                }
            } catch (Exception e) {
                Constants.LOG.warn("[QMAW] Skipping malformed manual {}: {}", location, e.toString());
            }
        }

        List<IndexedTrigger> exact = new ArrayList<>();
        Map<Item, QuickManualAndWiki> wildcard = new HashMap<>();
        Map<String, QuickManualAndWiki> prefixes = new LinkedHashMap<>();
        int triggerCount = 0;
        for (QuickManualAndWiki page : pages.values()) {
            for (QuickManualAndWiki.Trigger trigger : page.triggers) {
                triggerCount++;
                if (trigger.itemPathPrefix() != null) prefixes.put(trigger.itemPathPrefix(), page);
                else if (trigger.wildcard()) wildcard.put(trigger.stack().getItem(), page);
                else exact.add(new IndexedTrigger(page, trigger));
            }
        }

        qmaw = Map.copyOf(pages);
        exactTriggers = List.copyOf(exact);
        wildcardTriggers = Map.copyOf(wildcard);
        wildcardPrefixes = Map.copyOf(prefixes);
        unresolvedTriggers = List.copyOf(unresolved);
        sourceFiles = files;
        indexedTriggers = triggerCount;
        Constants.LOG.info("[QMAW] Loaded {}/{} manual pages and indexed {} trigger(s) ({} wildcard, {} unresolved) in {}ms",
                pages.size(), files, triggerCount, wildcard.size() + prefixes.size(), unresolved.size(), System.currentTimeMillis() - start);
    }

    /** Exact component/data match first, then the old item-only wildcard equivalent. */
    public static QuickManualAndWiki find(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (int i = exactTriggers.size() - 1; i >= 0; i--) {
            IndexedTrigger trigger = exactTriggers.get(i);
            if (matches(trigger.trigger().stack(), stack)) return trigger.page();
        }
        QuickManualAndWiki wildcard = wildcardTriggers.get(stack.getItem());
        if (wildcard != null) return wildcard;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return null;
        for (Map.Entry<String, QuickManualAndWiki> entry : wildcardPrefixes.entrySet()) {
            if (id.getPath().startsWith(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    private static boolean matches(ItemStack trigger, ItemStack candidate) {
        if (trigger.getItem() != candidate.getItem()) return false;
        // Fluid icons have extra amount/pressure and tint components at runtime; the old metadata key
        // matched only the fluid subtype, so preserve that identity while retaining component matching for
        // every other flattened item family.
        if (trigger.getItem() == ModFluidItems.fluid_icon.get()
                || trigger.getItem() == ModMachineItems.fluid_identifier.get()) {
            return ModDataComponents.fluid(trigger) == ModDataComponents.fluid(candidate);
        }
        return ItemStacks.sameItemAndData(trigger, candidate);
    }

    private record IndexedTrigger(QuickManualAndWiki page, QuickManualAndWiki.Trigger trigger) {}

    private static QuickManualAndWiki parse(String file, JsonObject json, List<String> unresolved) {
        String name = string(json, "name", "");
        if (name.isEmpty()) throw new IllegalArgumentException("missing name");

        Map<String, String> titles = strings(json.get("title"));
        Map<String, String> contents = strings(json.get("content"));
        if (contents.isEmpty()) {
            Constants.LOG.warn("[QMAW] Skipping manual {} ('{}'): no content", file, name);
            return null;
        }

        ItemStack icon = null;
        JsonElement iconElement = json.get("icon");
        if (iconElement != null) {
            icon = legacyStack(iconElement, false);
            if (icon == null) Constants.LOG.warn("[QMAW] Manual {} ('{}') has an unresolved icon: {}", file, name, iconElement);
        }

        List<QuickManualAndWiki.Trigger> triggers = new ArrayList<>();
        JsonElement triggerElement = json.get("trigger");
        if (triggerElement != null && triggerElement.isJsonArray()) {
            for (JsonElement element : triggerElement.getAsJsonArray()) {
                boolean wildcard = isWildcard(element);
                // a wildcard over a 1.7 metadata family that is one item per type here: every member
                List<ItemStack> members = wildcard ? splitMembers(element) : List.of();
                if (!members.isEmpty()) {
                    for (ItemStack member : members) triggers.add(new QuickManualAndWiki.Trigger(member, true));
                    continue;
                }
                String prefix = wildcardPrefix(element, wildcard);
                if (prefix != null) {
                    triggers.add(new QuickManualAndWiki.Trigger(prefix));
                    continue;
                }
                ItemStack stack = legacyStack(element, wildcard);
                if (stack == null) {
                    String problem = file + " -> " + element;
                    unresolved.add(problem);
                    Constants.LOG.warn("[QMAW] Skipping unresolved trigger {}", problem);
                } else {
                    triggers.add(new QuickManualAndWiki.Trigger(stack, wildcard));
                }
            }
        }
        return new QuickManualAndWiki(name, icon, titles, contents, triggers);
    }

    private static Map<String, String> strings(JsonElement element) {
        Map<String, String> values = new LinkedHashMap<>();
        if (element == null || !element.isJsonObject()) return values;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (entry.getValue().isJsonPrimitive()) values.put(entry.getKey(), entry.getValue().getAsString());
        }
        return values;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static boolean isWildcard(JsonElement element) {
        try {
            if (!element.isJsonArray()) return false;
            JsonArray array = element.getAsJsonArray();
            return array.size() > 2 && array.get(2).isJsonPrimitive() && array.get(2).getAsInt() == 32767;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static List<ItemStack> splitMembers(JsonElement element) {
        try {
            Identifier id = Ids.parse(element.getAsJsonArray().get(0).getAsString());
            if (!id.getNamespace().equals("hbm")) return List.of();
            List<ItemStack> out = new ArrayList<>();
            for (String member : com.hbm.items.LegacyMetaIds.members(id.getPath())) {
                ItemStack stack = stack(Ids.parse(member), 1);
                if (stack != null) out.add(stack);
            }
            return out;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static String wildcardPrefix(JsonElement element, boolean wildcard) {
        try {
            if (!wildcard || !element.isJsonArray() || element.getAsJsonArray().isEmpty()) return null;
            String id = element.getAsJsonArray().get(0).getAsString();
            if (id.equals("hbm:coke")) return "coke_";
            if (id.equals("hbm:wire_dense")) return "wire_dense_";
            return null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Reads the old [item, count, metadata] form and translates only known flattened families. */
    private static ItemStack legacyStack(JsonElement element, boolean wildcard) {
        try {
            return legacyStackUnsafe(element, wildcard);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static ItemStack legacyStackUnsafe(JsonElement element, boolean wildcard) {
        if (!element.isJsonArray()) return null;
        JsonArray array = element.getAsJsonArray();
        if (array.isEmpty() || !array.get(0).isJsonPrimitive()) return null;

        String oldId = array.get(0).getAsString();
        int count = array.size() > 1 ? array.get(1).getAsInt() : 1;
        // A fluid page names its fluid by registry name ("hbm:chlorine"); a number is read as the 1.7 fluid id
        // (the frozen legacy table), which is what the 1.7 manual files carried.
        JsonElement third = array.size() > 2 ? array.get(2) : null;
        String fluidName = third != null && third.isJsonPrimitive() && third.getAsJsonPrimitive().isString()
                ? third.getAsString() : null;
        int metadata = third == null || fluidName != null ? 0 : third.getAsInt();
        if (count <= 0) return null;
        Identifier oldLocation;
        try {
            oldLocation = Ids.parse(oldId);
        } catch (RuntimeException e) {
            return null;
        }

        if (oldLocation.equals(Ids.of("hbm", "fluid_icon"))) {
            if (wildcard) return stack(Ids.of("hbm", "fluid_icon"), count);
            FluidType fluid = fluidName != null ? Fluids.byRegistryName(fluidName) : Fluids.byLegacyId(metadata);
            if (fluid == Fluids.NONE && (metadata != 0 || fluidName != null)) return null;
            return ItemStacks.copyWithCount(ItemFluidIcon.make(fluid), count);
        }
        if (oldLocation.equals(Ids.of("hbm", "fluid_identifier_multi"))) {
            if (wildcard) return stack(Ids.of("hbm", "fluid_identifier"), count);
            FluidType fluid = fluidName != null ? Fluids.byRegistryName(fluidName) : Fluids.byLegacyId(metadata);
            if (fluid == Fluids.NONE && (metadata != 0 || fluidName != null)) return null;
            return ItemStacks.copyWithCount(ItemFluidId.make(fluid), count);
        }

        Identifier current = flattenedId(oldLocation, metadata);
        if (current == null) return null;
        ItemStack stack = stack(current, count);
        if (stack == null) return null;

        // 1.7 metadata 1 of the stirling engines and the sawmill (the gear/blade removed) is a typed state here.
        if (!wildcard && metadata == 1) {
            String path = oldLocation.getPath();
            if (path.equals("machine_sawmill")) ModDataComponents.HAS_BLADE.set(stack, false);
            if (path.startsWith("machine_stirling")) ModDataComponents.HAS_COG.set(stack, false);
        }
        return stack;
    }

    private static ItemStack stack(Identifier id, int count) {
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == null || item == Items.AIR ? null : new ItemStack(item, count);
    }

    private static Identifier flattenedId(Identifier oldId, int metadata) {
        String path = oldId.getPath();
        // the metadata families that are one item per type (History §19es-itemvariant)
        if (oldId.getNamespace().equals("hbm") && com.hbm.items.LegacyMetaIds.isSplit(path)) {
            String split = com.hbm.items.LegacyMetaIds.lookup(path, metadata);
            return split == null ? null : Ids.parse(split);
        }
        if (oldId.equals(Ids.of("hbm", "ammo_standard"))) {
            String[] variants = {
                    "ammo_standard_m357_bp", "ammo_standard_m357_sp", "ammo_standard_m357_fmj", "ammo_standard_m357_jhp", "ammo_standard_m357_ap", "ammo_standard_m357_express",
                    "ammo_standard_m44_bp", "ammo_standard_m44_sp", "ammo_standard_m44_fmj", "ammo_standard_m44_jhp", "ammo_standard_m44_ap", "ammo_standard_m44_express",
                    "ammo_standard_p22_sp", "ammo_standard_p22_fmj", "ammo_standard_p22_jhp", "ammo_standard_p22_ap",
                    "ammo_standard_p9_sp", "ammo_standard_p9_fmj", "ammo_standard_p9_jhp", "ammo_standard_p9_ap"
            };
            if (metadata >= 4 && metadata <= 23) return Ids.of("hbm", variants[metadata - 4]);
            if (metadata == 75) return Ids.of("hbm", "ammo_standard_nuke_high");
            return null;
        }
        if (oldId.equals(Ids.of("hbm", "rbmk_rod"))) return Ids.of("hbm", "rbmk_fuel_rod");
        if (oldId.equals(Ids.of("hbm", "rbmk_rod_mod"))) return Ids.of("hbm", "rbmk_fuel_rod_moderated");
        if (oldId.equals(Ids.of("hbm", "chunk_ore")) && metadata == 0) return Ids.of("hbm", "chunk_ore_rare");
        if (oldId.equals(Ids.of("hbm", "furnace_combination"))) return Ids.of("hbm", "machine_furnace_combination");
        if (oldId.equals(Ids.of("hbm", "wire_dense")) && metadata == 48) return Ids.of("hbm", "wire_dense_bscco");
        if (oldId.equals(Ids.of("hbm", "wire_dense")) && metadata == 7900) return Ids.of("hbm", "wire_dense_gold");
        if (oldId.equals(Ids.of("hbm", "coke"))) return Ids.of("hbm", "coke_coal");
        if (path.equals("pile_brick")) return null;
        return oldId;
    }
}
