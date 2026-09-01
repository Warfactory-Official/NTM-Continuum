package com.hbm.blocks.machine.storage;

import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.ItemBlockStorageCrate;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import com.hbm.tileentity.machine.storage.CrateType;
import com.hbm.tileentity.machine.storage.MassStorageType;
import net.minecraft.world.level.block.SoundType;

/**
 * Hand-registered storage blocks (the iron / steel / desh crates; future: tungsten crate, safe, file
 * cabinet, mass storage). Separate from generated {@link com.hbm.blocks.ModBlocks} so codegen never
 * clobbers them.
 */
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

    public static final RegistryObject<BlockMassStorage> mass_storage_wood = massStorage(MassStorageType.WOOD);
    public static final RegistryObject<BlockMassStorage> mass_storage_iron = massStorage(MassStorageType.IRON);
    public static final RegistryObject<BlockMassStorage> mass_storage_desh = massStorage(MassStorageType.DESH);
    public static final RegistryObject<BlockMassStorage> mass_storage = massStorage(MassStorageType.MASS);
    public static final RegistryObject<BlockTemplateCrate> crate_template = BLOCKS.registerBlock(
            "crate_template", BlockTemplateCrate::new,
            p -> p.strength(7.5F, 300.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    static {
        tabItem("crate_iron", crate_iron, CrateType.IRON);
        tabItem("crate_steel", crate_steel, CrateType.STEEL);
        tabItem("crate_desh", crate_desh, CrateType.DESH);
        massStorageItem(MassStorageType.WOOD, mass_storage_wood);
        massStorageItem(MassStorageType.IRON, mass_storage_iron);
        massStorageItem(MassStorageType.DESH, mass_storage_desh);
        massStorageItem(MassStorageType.MASS, mass_storage);
        RegistryObject<com.hbm.items.block.ItemBlockTemplateCrate> template = ITEMS.registerItem(
                "crate_template", p -> new com.hbm.items.block.ItemBlockTemplateCrate(crate_template.get(), p),
                Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, template);
    }

    private static void tabItem(String name, RegistryObject<BlockCrate> block, CrateType type) {
        // Custom block item so the tooltip shows the "X/Y Slots used" readout (it carries the crate's
        // contents in the CONTAINER component). useBlockDescriptionPrefix() keeps the block.hbm.<id> key.
        RegistryObject<ItemBlockStorageCrate> item = ITEMS.registerItem(name,
                p -> new ItemBlockStorageCrate(block.get(), p, type), p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    private static RegistryObject<BlockMassStorage> massStorage(MassStorageType type) {
        return BLOCKS.registerBlock(type.id, p -> new BlockMassStorage(p, type),
                p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }

    private static void massStorageItem(MassStorageType type, RegistryObject<BlockMassStorage> block) {
        RegistryObject<com.hbm.items.block.ItemBlockMassStorage> item = ITEMS.registerItem(type.id,
                p -> new com.hbm.items.block.ItemBlockMassStorage(block.get(), p, type), Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    public static void init() {}
}
