package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemBombCaller;
import com.hbm.items.tool.ItemDropDetonator;
import com.hbm.items.tool.ItemLaserDetonator;
import com.hbm.items.armor.ItemModDefuser;
import com.hbm.items.tool.ItemMultiDetonator;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/**
 * Hand items of the conventional-explosives family: the multi, laser and dead man's detonators, the
 * dead man's explosive, the golden wire cutter armor module and the airstrike designators. Tabs are
 * the 1.7 ones (detonators on the bombs tab, designators and the armor module on consumables).
 */
public final class ModExplosivesItems {
    private ModExplosivesItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemMultiDetonator> detonator_multi =
            ITEMS.registerItem("detonator_multi", ItemMultiDetonator::new);
    public static final RegistryObject<ItemLaserDetonator> detonator_laser =
            ITEMS.registerItem("detonator_laser", ItemLaserDetonator::new);
    public static final RegistryObject<ItemDropDetonator> detonator_deadman =
            ITEMS.registerItem("detonator_deadman", p -> new ItemDropDetonator(p, ItemDropDetonator.Kind.DEADMAN));
    public static final RegistryObject<ItemDropDetonator> detonator_de =
            ITEMS.registerItem("detonator_de", p -> new ItemDropDetonator(p, ItemDropDetonator.Kind.DE));
    public static final RegistryObject<ItemModDefuser> defuser_gold =
            ITEMS.registerItem("defuser_gold", ItemModDefuser::new, p -> p.stacksTo(1));
    /** 1.7 {@code bomb_caller} metadata 0-7, one item each ({@code bomb_caller_<type>}). */
    public static final ItemFamily<ItemBombCaller.Type, ItemBombCaller> bomb_caller = ItemFamily.of(ITEMS,
            "bomb_caller", ItemBombCaller.Type.class, ItemBombCaller::new, p -> p);

    static {
        ModTabs.addTo(HbmTab.BOMBS, detonator_multi);
        ModTabs.addTo(HbmTab.BOMBS, detonator_laser);
        ModTabs.addTo(HbmTab.BOMBS, detonator_deadman);
        ModTabs.addTo(HbmTab.BOMBS, detonator_de);
        ModTabs.addTo(HbmTab.CONSUMABLES, defuser_gold);
        // 1.7 getSubItems listed only metadata 0-4.
        for (ItemBombCaller.Type type : new ItemBombCaller.Type[] { ItemBombCaller.Type.CARPET, ItemBombCaller.Type.NAPALM,
                ItemBombCaller.Type.CHLORINE, ItemBombCaller.Type.ORANGE, ItemBombCaller.Type.ABOMB }) {
            ModTabs.addTo(HbmTab.CONSUMABLES, bomb_caller.handle(type));
        }
    }

    public static void init() {
        // Components declared inside item classes must be touched before the registry flush (brief addendum).
        Object touch = ItemMultiDetonator.POSITIONS;
    }
}
