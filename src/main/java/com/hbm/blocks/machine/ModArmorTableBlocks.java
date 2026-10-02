package com.hbm.blocks.machine;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;

/** Hand-registered armor table block and item, kept separate from generated simple blocks. */
public final class ModArmorTableBlocks {

    private ModArmorTableBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    public static final RegistryObject<BlockArmorTable> ARMOR_TABLE = REG.<BlockArmorTable>block(
            "machine_armor_table", BlockArmorTable::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL))
            .model(d -> d.models(Models.cubeBottomTop("hbm:blocks/armor_table_bottom", "hbm:blocks/armor_table_side", "hbm:blocks/armor_table_top")).states(Variants.simple()).blockItemDirect());
    public static final RegistryObject<BlockItem> ARMOR_TABLE_ITEM = REG.blockItem(ARMOR_TABLE);

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, ARMOR_TABLE_ITEM);
    }

    public static void init() {}
}
