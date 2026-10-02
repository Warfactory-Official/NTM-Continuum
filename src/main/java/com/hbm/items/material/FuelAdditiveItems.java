package com.hbm.items.material;

import com.hbm.registration.Reg;
import com.hbm.registration.Presets;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/** Feature-local registry for the two unambiguous flattened legacy fuel-additive variants. */
public final class FuelAdditiveItems {

    private FuelAdditiveItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);
    private static final Map<Variant, RegistryObject<FuelAdditiveItem>> BY_VARIANT = new EnumMap<>(Variant.class);

    static {
        for (Variant variant : Variant.values()) {
            // 1.7 metadata sprite: fuel_additive.<variant>
            RegistryObject<FuelAdditiveItem> item = REG.item(variant.id, FuelAdditiveItem::new)
                    .transform(Presets.sprite("hbm:items/" + variant.id.replace("fuel_additive_", "fuel_additive.")));
            BY_VARIANT.put(variant, item);
            ModTabs.addTo(HbmTab.CONTROL, item);
        }
    }

    public static FuelAdditiveItem get(Variant variant) {
        return BY_VARIANT.get(variant).get();
    }

    public static ItemStack stack(Variant variant) {
        return new ItemStack(get(variant));
    }

    public enum Variant {
        ANTIKNOCK("fuel_additive_antiknock"),
        DEICER("fuel_additive_deicer");

        public static final Variant[] VALUES = values();

        public final String id;

        Variant(String id) {
            this.id = id;
        }
    }

    public static void init() {}
}
