package com.hbm.items;

import java.util.Locale;
import dev.golem.core.dejson.Tags;
import com.hbm.registration.Reg;
import com.hbm.registration.Presets;
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
    private static final Reg REG = Reg.of(ITEMS);

    public static final RegistryObject<ItemMultiDetonator> detonator_multi =
            REG.item("detonator_multi", ItemMultiDetonator::new).transform(Presets.handheld()).tab(HbmTab.BOMBS);
    public static final RegistryObject<ItemLaserDetonator> detonator_laser =
            REG.item("detonator_laser", ItemLaserDetonator::new).tab(HbmTab.BOMBS)
                    // 1.7 ItemRenderDetonatorLaser handled every slot, the inventory included; the item is setFull3D(), so
                    // its hands are the full-3D weapon frame (its own scale(-0.125) undoes that pose's mirror)
                    .andThen(h -> ItemRendererDeclarations.model(h.path(), "detonator_laser", "weapon_full3d"));
    public static final RegistryObject<ItemDropDetonator> detonator_deadman =
            REG.item("detonator_deadman", p -> new ItemDropDetonator(p, ItemDropDetonator.Kind.DEADMAN), p -> p)
                    .transform(Presets.handheld()).tab(HbmTab.BOMBS);
    public static final RegistryObject<ItemDropDetonator> detonator_de =
            REG.item("detonator_de", p -> new ItemDropDetonator(p, ItemDropDetonator.Kind.DE), p -> p)
                    .transform(Presets.handheld()).tab(HbmTab.BOMBS);
    public static final RegistryObject<ItemModDefuser> defuser_gold =
            REG.item("defuser_gold", ItemModDefuser::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);
    /** 1.7 {@code bomb_caller} metadata 0-7, one item each ({@code bomb_caller_<type>}), all drawing one icon. */
    public static final ItemFamily<ItemBombCaller.Type, ItemBombCaller> bomb_caller = ItemFamily.of(
            ItemBombCaller.Type.class, type -> REG.item("bomb_caller_" + type.name().toLowerCase(Locale.ROOT),
                    p -> new ItemBombCaller(p, type), p -> p).transform(Presets.handheld("hbm:items/bomb_caller")));

    static {
        // 1.7 getSubItems listed only metadata 0-4.
        for (ItemBombCaller.Type type : new ItemBombCaller.Type[] { ItemBombCaller.Type.CARPET, ItemBombCaller.Type.NAPALM,
                ItemBombCaller.Type.CHLORINE, ItemBombCaller.Type.ORANGE, ItemBombCaller.Type.ABOMB }) {
            ModTabs.addTo(HbmTab.CONSUMABLES, bomb_caller.handle(type));
        }
        // 1.7 "record" ore-dict entry for the golden wire cutter: the discs every target version ships.
        Tags.item(Constants.MOD_ID, "music_discs", "minecraft:music_disc_13", "minecraft:music_disc_cat",
                "minecraft:music_disc_blocks", "minecraft:music_disc_chirp", "minecraft:music_disc_far",
                "minecraft:music_disc_mall", "minecraft:music_disc_mellohi", "minecraft:music_disc_stal",
                "minecraft:music_disc_strad", "minecraft:music_disc_ward", "minecraft:music_disc_11",
                "minecraft:music_disc_wait", "minecraft:music_disc_otherside", "minecraft:music_disc_5",
                "minecraft:music_disc_pigstep", "minecraft:music_disc_relic");
    }

    public static void init() {
        // Components declared inside item classes must be touched before the registry flush (brief addendum).
        Object touch = ItemMultiDetonator.POSITIONS;
    }
}
