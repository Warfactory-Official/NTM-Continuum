package com.hbm.blocks.machine.storage;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.ItemBlockStorageCrate;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import com.hbm.tileentity.machine.storage.CrateType;
import net.minecraft.world.level.block.SoundType;

public final class ModStorageBlocks {

    private ModStorageBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockCrate> crate_iron = BLOCKS.registerBlock(
            "crate_iron", p -> new BlockCrate(p, CrateType.IRON),
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<BlockCrate> crate_steel = BLOCKS.registerBlock(
            "crate_steel", p -> new BlockCrate(p, CrateType.STEEL),
            p -> p.strength(5.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<BlockCrate> crate_desh = BLOCKS.registerBlock(
            "crate_desh", p -> new BlockCrate(p, CrateType.DESH),
            p -> p.strength(7.5F, 300.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    static {
        tabItem("crate_iron", crate_iron, CrateType.IRON);
        tabItem("crate_steel", crate_steel, CrateType.STEEL);
        tabItem("crate_desh", crate_desh, CrateType.DESH);
    }

    private static void tabItem(String name, RegistryObject<BlockCrate> block, CrateType type) {

        RegistryObject<ItemBlockStorageCrate> item = ITEMS.registerItem(name,
                p -> new ItemBlockStorageCrate(block.get(), p, type), p -> p.useBlockDescriptionPrefix());
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    public static void init() {}
}
