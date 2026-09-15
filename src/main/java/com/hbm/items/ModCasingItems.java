package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ItemEnums.EnumCasingType;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Every identity from original-hbm's metadata-backed {@code casing} item. */
public final class ModCasingItems {

    private ModCasingItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static final Map<EnumCasingType, RegistryObject<Item>> CASINGS = new EnumMap<>(EnumCasingType.class);
    private static final EnumCasingType[] PORTED = {
            EnumCasingType.SMALL,
            EnumCasingType.LARGE,
            EnumCasingType.SMALL_STEEL,
            EnumCasingType.LARGE_STEEL,
            EnumCasingType.SHOTSHELL,
            EnumCasingType.BUCKSHOT,
            EnumCasingType.BUCKSHOT_ADVANCED
    };

    static {
        for (EnumCasingType type : PORTED) {
            // The original generated catalogue already owns this flattened identity.
            if (type == EnumCasingType.BUCKSHOT) {
                CASINGS.put(type, ModItems.casing_buckshot);
                continue;
            }
            String id = "casing_" + type.name().toLowerCase(Locale.ROOT);
            RegistryObject<Item> casing = ITEMS.registerSimpleItem(id);
            CASINGS.put(type, casing);
            ModTabs.addTo(HbmTab.MATERIALS, casing);
        }
    }

    public static Item casing(EnumCasingType type) {
        return CASINGS.get(type).get();
    }

    public static ItemStack casing(EnumCasingType type, int count) {
        return new ItemStack(casing(type), count);
    }

    public static void init() {}
}
