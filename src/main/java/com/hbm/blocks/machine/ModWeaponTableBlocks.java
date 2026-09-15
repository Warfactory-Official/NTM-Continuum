package com.hbm.blocks.machine;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

/** Registration and generated model declaration for the legacy weapon modification table. */
public final class ModWeaponTableBlocks {
    private ModWeaponTableBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockWeaponTable> MACHINE_WEAPON_TABLE = BLOCKS.registerBlock(
            "machine_weapon_table", BlockWeaponTable::new,
            p -> p.strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final RegistryObject<BlockItem> MACHINE_WEAPON_TABLE_ITEM = ITEMS.registerBlockItem(
            "machine_weapon_table", MACHINE_WEAPON_TABLE, Compat::blockDescription);

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, MACHINE_WEAPON_TABLE_ITEM);
    }

    public static void init() {
        Dejson.block("hbm", "machine_weapon_table")
                .models(Models.cubeBottomTop("hbm:blocks/gun_table_bottom", "hbm:blocks/gun_table_side",
                        "hbm:blocks/gun_table_top"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();
    }
}
