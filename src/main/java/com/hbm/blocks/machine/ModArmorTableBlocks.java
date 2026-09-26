package com.hbm.blocks.machine;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Hand-registered armor table block and item, kept separate from generated simple blocks. */
public final class ModArmorTableBlocks {

    private ModArmorTableBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockArmorTable> ARMOR_TABLE = BLOCKS.registerBlock(
            "machine_armor_table", BlockArmorTable::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<BlockItem> ARMOR_TABLE_ITEM = ITEMS.registerBlockItem(
            "machine_armor_table", ARMOR_TABLE);

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, ARMOR_TABLE_ITEM);
    }

    public static void init() {}
}
