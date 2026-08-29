package com.hbm.creativetabs;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * Registers the HBM creative tabs (one per {@link HbmTab}) and tracks their contents.
 *
 * <p>Tabs are built with the vanilla {@link CreativeModeTab} builder (cross-loader). Contents can't
 * be filled from common because {@code CreativeModeTab.Output} is protected, so {@link #CONTENTS}
 * maps each tab to its item list and the loader bootstraps fill the tabs through their own public
 * API (NeoForge {@code BuildCreativeModeTabContentsEvent}, Fabric {@code CreativeModeTabEvents}).
 * Content classes call {@link #addTo(HbmTab, RegistryObject)} during registration.
 */
public final class ModTabs {

    private ModTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

    private static final EnumMap<HbmTab, List<RegistryObject<? extends Item>>> LISTS = new EnumMap<>(HbmTab.class);
    private static final EnumMap<HbmTab, RegistryObject<CreativeModeTab>> TAB_OBJECTS = new EnumMap<>(HbmTab.class);

    /** Tab key -> ordered contents, consumed by the loader bootstraps. */
    public static final Map<ResourceKey<CreativeModeTab>, List<RegistryObject<? extends Item>>> CONTENTS = new LinkedHashMap<>();

    private static final EnumMap<HbmTab, List<Supplier<List<ItemStack>>>> STACK_LISTS = new EnumMap<>(HbmTab.class);
    /** Tab key -> custom display stacks (e.g. one fluid bucket per fluid), consumed by the loader bootstraps. */
    public static final Map<ResourceKey<CreativeModeTab>, List<Supplier<List<ItemStack>>>> STACK_CONTENTS = new LinkedHashMap<>();

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
            CONTENTS.put(ro.getKey(), contents);
            STACK_CONTENTS.put(ro.getKey(), stacks);
        }
    }

    /** Add an item (or block item) to a tab's display list. */
    public static void addTo(HbmTab tab, RegistryObject<? extends Item> item) {
        LISTS.get(tab).add(item);
    }

    /** Add a batch of custom display stacks to a tab (resolved lazily at tab-build time). */
    public static void addStacksTo(HbmTab tab, Supplier<List<ItemStack>> stacks) {
        STACK_LISTS.get(tab).add(stacks);
    }

    public static RegistryObject<CreativeModeTab> get(HbmTab tab) {
        return TAB_OBJECTS.get(tab);
    }

    /** Applies upstream's weapon-quality rule at the shared tab boundary, for every loader. */
    public static boolean shouldDisplay(ResourceKey<CreativeModeTab> tab, Item item) {
        if (!get(HbmTab.WEAPONS).getKey().equals(tab)) return true;
        return !(item instanceof com.hbm.items.weapon.sedna.ItemGunBaseNT gun)
                || gun.isCreativeTabVisible();
    }

    /** Forces class initialization so the static registrations above run. */
    public static void init() {}
}
