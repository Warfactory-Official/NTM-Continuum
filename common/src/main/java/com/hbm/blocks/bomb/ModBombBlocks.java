package com.hbm.blocks.bomb;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

public final class ModBombBlocks {

    private ModBombBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<NukeMan> nuke_man = BLOCKS.registerBlock(
            "nuke_man", NukeMan::new,

            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    static {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem("nuke_man", nuke_man);
        ModTabs.addTo(HbmTab.BOMBS, item);
    }

    public static void init() {}
}
