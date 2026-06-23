package com.hbm.creativetabs;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.hbm.Constants;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

public final class ModTabs {

    private ModTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

    private static final EnumMap<HbmTab, List<RegistryObject<? extends Item>>> LISTS = new EnumMap<>(HbmTab.class);
    private static final EnumMap<HbmTab, RegistryObject<CreativeModeTab>> TAB_OBJECTS = new EnumMap<>(HbmTab.class);

    public static final Map<ResourceKey<CreativeModeTab>, List<RegistryObject<? extends Item>>> CONTENTS = new LinkedHashMap<>();

    private static final EnumMap<HbmTab, List<Supplier<List<ItemStack>>>> STACK_LISTS = new EnumMap<>(HbmTab.class);

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

                            .icon(() -> contents.isEmpty() ? new ItemStack(Items.IRON_INGOT) : new ItemStack(contents.get(0).get()))
                            .build());
            TAB_OBJECTS.put(tab, ro);
            CONTENTS.put(ro.getKey(), contents);
            STACK_CONTENTS.put(ro.getKey(), stacks);
        }
    }

    public static void addTo(HbmTab tab, RegistryObject<? extends Item> item) {
        LISTS.get(tab).add(item);
    }

    public static void addStacksTo(HbmTab tab, Supplier<List<ItemStack>> stacks) {
        STACK_LISTS.get(tab).add(stacks);
    }

    public static RegistryObject<CreativeModeTab> get(HbmTab tab) {
        return TAB_OBJECTS.get(tab);
    }

    public static void init() {}
}
