package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemBlowtorch;
import com.hbm.items.tool.ItemBoltgun;
import com.hbm.items.tool.ItemDefuser;
import com.hbm.items.tool.ItemTooling;
import com.hbm.items.tool.ItemTurretMobFilter;
import com.hbm.items.machine.DrillBitType;
import com.hbm.items.machine.ItemDrillbit;
import com.hbm.api.block.IToolable.ToolType;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/**
 * HBM's in-world construction tools and excavator drill bits. The ordinary screwdriver and hand drill
 * remain on the consumables tab; the defuser remains on the bombs tab and drill bits on control,
 * matching their separate 1.12.2 registrations.
 */
public final class ModToolItems {

    private ModToolItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final RegistryObject<ItemBlowtorch> blowtorch =
            REG.item("blowtorch", ItemBlowtorch::new, p -> p.stacksTo(1)).transform(Presets.handheld());
    public static final RegistryObject<ItemBoltgun> boltgun =
            REG.item("boltgun", ItemBoltgun::new, p -> p.stacksTo(1))
                    // 1.7 ItemRenderBoltgun handled every slot (inventory included), an IItemRenderer of its own: the
                    // weapon frame. Replaces the authored machine_obj definition, whose scale/yaw were invented framing.
                    .andThen(h -> ItemRendererDeclarations.model(h.path(), "boltgun", "weapon"));
    public static final RegistryObject<ItemTooling> screwdriver =
            REG.item("screwdriver", p -> new ItemTooling(p, ToolType.SCREWDRIVER),
                    p -> p.stacksTo(1).durability(100)).transform(Presets.handheld());
    public static final RegistryObject<ItemTooling> hand_drill =
            REG.item("hand_drill", p -> new ItemTooling(p, ToolType.HAND_DRILL),
                    p -> p.stacksTo(1).durability(100)).transform(Presets.handheld());
    /** Mk.V crate opening device: 1.7 ModSword(tMatSteel), 750 uses and a 4 + 2 = 6 attack modifier. */
    public static final RegistryObject<dev.golem.core.world.CompatSwordItem> crowbar =
            REG.item("crowbar", p -> new dev.golem.core.world.CompatSwordItem(p, 750, 4.0F, -2.4F,
                    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                            dev.golem.core.Ids.of(Constants.MOD_ID, "repairs/steel"))), p -> p)
                    .transform(Presets.handheld());
    public static final RegistryObject<ItemDefuser> defuser =
            REG.item("defuser", ItemDefuser::new,
                    p -> p.stacksTo(1).durability(100)).transform(Presets.handheld());
    public static final RegistryObject<ItemTurretMobFilter> turret_mob_filter =
            REG.item("turret_mob_filter", ItemTurretMobFilter::new,
                    p -> p.stacksTo(1)).transform(Presets.sprite());

    public static final RegistryObject<ItemDrillbit> drillbit_steel = drillbit("drillbit_steel", DrillBitType.STEEL);
    public static final RegistryObject<ItemDrillbit> drillbit_steel_diamond = drillbit("drillbit_steel_diamond", DrillBitType.STEEL_DIAMOND);
    public static final RegistryObject<ItemDrillbit> drillbit_hss = drillbit("drillbit_hss", DrillBitType.HSS);
    public static final RegistryObject<ItemDrillbit> drillbit_hss_diamond = drillbit("drillbit_hss_diamond", DrillBitType.HSS_DIAMOND);
    public static final RegistryObject<ItemDrillbit> drillbit_desh = drillbit("drillbit_desh", DrillBitType.DESH);
    public static final RegistryObject<ItemDrillbit> drillbit_desh_diamond = drillbit("drillbit_desh_diamond", DrillBitType.DESH_DIAMOND);
    public static final RegistryObject<ItemDrillbit> drillbit_tcalloy = drillbit("drillbit_tcalloy", DrillBitType.TCALLOY);
    public static final RegistryObject<ItemDrillbit> drillbit_tcalloy_diamond = drillbit("drillbit_tcalloy_diamond", DrillBitType.TCALLOY_DIAMOND);
    public static final RegistryObject<ItemDrillbit> drillbit_ferro = drillbit("drillbit_ferro", DrillBitType.FERRO);
    public static final RegistryObject<ItemDrillbit> drillbit_ferro_diamond = drillbit("drillbit_ferro_diamond", DrillBitType.FERRO_DIAMOND);

    static {
        ModTabs.addTo(HbmTab.CONTROL, blowtorch);
        ModTabs.addTo(HbmTab.CONTROL, boltgun);
        ModTabs.addTo(HbmTab.CONSUMABLES, screwdriver);
        ModTabs.addTo(HbmTab.CONSUMABLES, hand_drill);
        ModTabs.addTo(HbmTab.CONSUMABLES, crowbar);
        ModTabs.addTo(HbmTab.BOMBS, defuser);
        ModTabs.addTo(HbmTab.WEAPONS, turret_mob_filter);

        ModTabs.addTo(HbmTab.CONTROL, drillbit_steel);
        ModTabs.addTo(HbmTab.CONTROL, drillbit_steel_diamond);
        ModTabs.addTo(HbmTab.CONTROL, drillbit_hss);
        ModTabs.addTo(HbmTab.CONTROL, drillbit_hss_diamond);
        ModTabs.addTo(HbmTab.CONTROL, drillbit_desh);
        ModTabs.addTo(HbmTab.CONTROL, drillbit_desh_diamond);
        ModTabs.addTo(HbmTab.CONTROL, drillbit_tcalloy);
        ModTabs.addTo(HbmTab.CONTROL, drillbit_tcalloy_diamond);
        ModTabs.addTo(HbmTab.CONTROL, drillbit_ferro);
        ModTabs.addTo(HbmTab.CONTROL, drillbit_ferro_diamond);

        ToolType.SCREWDRIVER.register(screwdriver);
        ToolType.HAND_DRILL.register(hand_drill);
        ToolType.DEFUSER.register(defuser);
    }

    /** 1.7 {@code ItemDrillbit}: the sprite {@code drillbit.<type>}, held like a tool. */
    private static RegistryObject<ItemDrillbit> drillbit(String id, DrillBitType type) {
        return REG.item(id, p -> new ItemDrillbit(p, type), p -> p.stacksTo(1))
                .transform(Presets.sprite("hbm:items/drillbit." + id.substring("drillbit_".length()))); // 1.7 ItemDrillbit: not full-3D
    }

    public static void init() {}
}
