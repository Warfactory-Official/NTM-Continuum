package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.ItemModels;
import dev.golem.core.dejson.Models;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.tool.ItemCasingBag;
import com.hbm.items.weapon.ItemAmmoContainer;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;
import java.util.List;

/** Hand-registered items shared by the Sedna guns and their bag menus. */
public final class ModGunSupportItems {

    private ModGunSupportItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final RegistryObject<ItemAmmoContainer> ammo_container = REG.item(
            "ammo_container", ItemAmmoContainer::new, p -> p.stacksTo(1))
            .icon(d -> d
                    .models(Models.generated("hbm:items/ammo_container"))
                    .definition(ItemModels.select("hbm:item_variant")
                            .when("1", ItemModels.model("hbm:item/ammo_container_alt"))
                            .fallback(ItemModels.model("hbm:item/ammo_container"))));
    public static final RegistryObject<ItemAmmoBag> ammo_bag = REG.item(
            "ammo_bag", p -> new ItemAmmoBag(p, false), p -> p.stacksTo(1)).transform(Presets.sprite());
    public static final RegistryObject<ItemAmmoBag> ammo_bag_infinite = REG.item(
            "ammo_bag_infinite", p -> new ItemAmmoBag(p, true), p -> p.stacksTo(1)).transform(Presets.sprite());
    public static final RegistryObject<ItemCasingBag> casing_bag = REG.item(
            "casing_bag", ItemCasingBag::new, p -> p.stacksTo(1)).transform(Presets.sprite());

    static {
        ModTabs.addStacksTo(HbmTab.WEAPONS, () -> List.of(ammo_container.get().of(0), ammo_container.get().of(1)));
        ModTabs.addTo(HbmTab.WEAPONS, ammo_bag);
        ModTabs.addTo(HbmTab.WEAPONS, ammo_bag_infinite);
        ModTabs.addTo(HbmTab.WEAPONS, casing_bag);
    }

    public static void init() {}
}
