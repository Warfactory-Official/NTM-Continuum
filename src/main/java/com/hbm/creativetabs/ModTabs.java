package com.hbm.creativetabs;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;
import java.util.function.Consumer;

/**
 * Registers the HBM creative tabs (one per {@link HbmTab}) and tracks their contents.
 *
 * <p>Tabs are built with the vanilla {@link CreativeModeTab} builder (cross-loader). Contents can't
 * be filled from common because {@code CreativeModeTab.Output} is protected, so {@link #CONTENTS}
 * records each tab's registrations; {@link #displayStacks} resolves the final listing for each loader's public
 * API (NeoForge {@code BuildCreativeModeTabContentsEvent}, Fabric {@code CreativeModeTabEvents}).
 * Content classes call {@link #addTo(HbmTab, RegistryObject)} during registration.
 */
public final class ModTabs {

    private ModTabs() {}

    // The registry key is named lazily, and CONTENTS/STACK_CONTENTS resolve the tab keys on first read:
    // Reg's .tab(...) reaches this class while Dejson initialises a registration class in a plain JVM at
    // build time, where 1.20.1 cannot create a ResourceKey (see Golem's RegistryObject).
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(() -> Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

    private static final EnumMap<HbmTab, List<RegistryObject<? extends Item>>> LISTS = new EnumMap<>(HbmTab.class);
    private static final EnumMap<HbmTab, RegistryObject<CreativeModeTab>> TAB_OBJECTS = new EnumMap<>(HbmTab.class);

    /** Tab key -> registration-order item handles. Final display ordering is resolved by {@link #displayStacks}. */
    public static final Map<ResourceKey<CreativeModeTab>, List<RegistryObject<? extends Item>>> CONTENTS = new ByTabKey<>(LISTS);

    private static final EnumMap<HbmTab, List<Supplier<List<ItemStack>>>> STACK_LISTS = new EnumMap<>(HbmTab.class);
    /** Tab key -> custom display batches (e.g. one fluid bucket per fluid), resolved by {@link #displayStacks}. */
    public static final Map<ResourceKey<CreativeModeTab>, List<Supplier<List<ItemStack>>>> STACK_CONTENTS = new ByTabKey<>(STACK_LISTS);

    static {
        for (HbmTab tab : HbmTab.values()) {
            List<RegistryObject<? extends Item>> contents = new ArrayList<>();
            LISTS.put(tab, contents);
            List<Supplier<List<ItemStack>>> stacks = new ArrayList<>();
            STACK_LISTS.put(tab, stacks);
            RegistryObject<CreativeModeTab> ro = TABS.register(tab.id, () ->
                    CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                            .title(Component.translatable("itemGroup.hbm." + tab.id))
                            // Icon = first registered entry in this tab, or iron ingot as a fallback.
                            .icon(() -> contents.isEmpty() ? new ItemStack(Items.IRON_INGOT) : new ItemStack(contents.get(0).get()))
                            .build());
            TAB_OBJECTS.put(tab, ro);
        }
    }

    /**
     * A per-tab table read by tab key, in {@link HbmTab} order. Built on the first read, because the keys
     * are the registered tabs' keys and a key is only created when first asked for. Read-only.
     */
    private static final class ByTabKey<V> extends AbstractMap<ResourceKey<CreativeModeTab>, V> {

        private final EnumMap<HbmTab, V> byTab;
        private volatile Map<ResourceKey<CreativeModeTab>, V> byKey;

        ByTabKey(EnumMap<HbmTab, V> byTab) {
            this.byTab = byTab;
        }

        private Map<ResourceKey<CreativeModeTab>, V> resolved() {
            Map<ResourceKey<CreativeModeTab>, V> map = byKey;
            if (map == null) {
                map = new LinkedHashMap<>();
                for (Map.Entry<HbmTab, V> entry : byTab.entrySet()) {
                    map.put(TAB_OBJECTS.get(entry.getKey()).getKey(), entry.getValue());
                }
                byKey = map = Collections.unmodifiableMap(map);
            }
            return map;
        }

        @Override
        public V get(Object key) {
            return resolved().get(key);
        }

        @Override
        public Set<Map.Entry<ResourceKey<CreativeModeTab>, V>> entrySet() {
            return resolved().entrySet();
        }
    }

    /** Add an item (or block item) to a tab's display list. */
    public static void addTo(HbmTab tab, RegistryObject<? extends Item> item) {
        List<RegistryObject<? extends Item>> contents = LISTS.get(tab);
        if (!contents.contains(item)) contents.add(item);
    }

    /** Add a batch of custom display stacks to a tab (resolved lazily at tab-build time). */
    public static void addStacksTo(HbmTab tab, Supplier<List<ItemStack>> stacks) {
        List<Supplier<List<ItemStack>>> contents = STACK_LISTS.get(tab);
        if (!contents.contains(stacks)) contents.add(stacks);
    }

    private static final EnumMap<HbmTab, EntryOrder<String>> ORDER = new EnumMap<>(HbmTab.class);
    private static final EnumMap<HbmTab, Map<RegistryObject<? extends Item>, Supplier<List<ItemStack>>>> DISPLAY_STACKS = new EnumMap<>(HbmTab.class);

    /** Moves the existing entries after the anchor's first stack; shared anchors keep declaration order. */
    public static void insertAfter(HbmTab tab, String anchor, String... entries) {
        ORDER.computeIfAbsent(tab, ignored -> new EntryOrder<>()).insertAfter(anchor, List.of(entries));
    }

    /** Moves the existing entries before the anchor's first stack; inserted entries may themselves be anchors. */
    public static void insertBefore(HbmTab tab, String anchor, String... entries) {
        ORDER.computeIfAbsent(tab, ignored -> new EntryOrder<>()).insertBefore(anchor, List.of(entries));
    }

    /** Moves existing entries to the front of a tab, retaining every component-bearing variant. */
    public static void insertFirst(HbmTab tab, String... entries) {
        ORDER.computeIfAbsent(tab, ignored -> new EntryOrder<>()).insertFirst(List.of(entries));
    }

    /** Removes every display variant of an existing entry. Missing entries and anchors refuse to build. */
    public static void remove(HbmTab tab, String entry) {
        ORDER.computeIfAbsent(tab, ignored -> new EntryOrder<>()).remove(entry);
    }

    /** Replaces an item's default display stack with its real component-bearing variants. */
    public static void displayStacks(HbmTab tab, RegistryObject<? extends Item> item, Supplier<List<ItemStack>> stacks) {
        DISPLAY_STACKS.computeIfAbsent(tab, ignored -> new LinkedHashMap<>()).put(item, stacks);
    }

    /** The final common listing consumed by every loader, including custom display variants and addon positions. */
    public static List<ItemStack> displayStacks(ResourceKey<CreativeModeTab> tab) {
        var contents = CONTENTS.get(tab);
        if (contents == null) return List.of();
        HbmTab group = null;
        for (var candidate : HbmTab.values()) if (get(candidate).getKey().equals(tab)) {
            group = candidate;
            break;
        }
        var replacements = DISPLAY_STACKS.getOrDefault(group, Map.of());
        var result = new ArrayList<ItemStack>();
        for (var handle : contents) {
            var item = handle.get();
            if (!shouldDisplay(tab, item)) continue;
            var replacement = replacements.get(handle);
            if (replacement == null) result.add(new ItemStack(item));
            else result.addAll(replacement.get());
        }
        for (var supplier : STACK_CONTENTS.get(tab)) result.addAll(supplier.get());
        var order = ORDER.get(group);
        return order == null ? result : order.apply(result, stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static void accept(ResourceKey<CreativeModeTab> tab, Consumer<ItemStack> output) {
        displayStacks(tab).forEach(output);
    }

    public static RegistryObject<CreativeModeTab> get(HbmTab tab) {
        return TAB_OBJECTS.get(tab);
    }

    /** Applies upstream's weapon-quality rule at the shared tab boundary, for every loader. */
    public static boolean shouldDisplay(ResourceKey<CreativeModeTab> tab, Item item) {
        if (!get(HbmTab.WEAPONS).getKey().equals(tab)) return true;
        return !(item instanceof ItemGunBaseNT gun)
                || gun.isCreativeTabVisible();
    }

    /** Forces class initialization so the static registrations above run. */
    public static void init() {}
}
