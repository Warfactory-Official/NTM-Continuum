package com.hbm.blocks.machine;

import dev.golem.core.registry.BlockTag;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import dev.golem.core.registry.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;

/** Registration, blockstate, item definition and loot of the legacy weapon modification table. */
public final class ModWeaponTableBlocks {
    private ModWeaponTableBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    public static final RegistryObject<BlockWeaponTable> MACHINE_WEAPON_TABLE = REG.<BlockWeaponTable>block(
                    "machine_weapon_table", BlockWeaponTable::new,
                    // Original: Material.iron, setHardness(5.0F).setResistance(10.0F); no tool requirement.
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL))
            // The block model is authored (models/block/machine_weapon_table.json, a gun_table cube_bottom_top).
            .transform(Presets.statesOnly(Variants.simple()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockItem> MACHINE_WEAPON_TABLE_ITEM = REG.blockItem(
                    MACHINE_WEAPON_TABLE, Compat::blockDescription)
            .transform(Presets.shares("hbm:block/machine_weapon_table"));

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, MACHINE_WEAPON_TABLE_ITEM);
    }

    public static void init() {}
}
