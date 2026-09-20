package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.world.Compat;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import com.hbm.blocks.network.PneumoStorageAccess;
import com.hbm.blocks.network.PneumoStorageClutter;
import com.hbm.blocks.network.PneumoStorageExporter;
import com.hbm.blocks.network.PneumoStorageImporter;
import com.hbm.blocks.network.PneumoStorageMono;
import com.hbm.blocks.network.PneumoTube;
import com.hbm.blocks.network.PneumoTubePaintableBlock;
import com.hbm.items.block.DescBlockItem;

/**
 * Feature-local block and block-item registry for the pneumatic tube storage system. Strengths and tabs are
 * 1.7 {@code ModBlocks}: the tube 2.0/10, everything else 5.0/10, all on the machine tab.
 */
public final class ModPneumaticBlocks {
    private ModPneumaticBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<PneumoTube> PNEUMATIC_TUBE = BLOCKS.registerBlock(
            "pneumatic_tube", PneumoTube::new,
            p -> p.strength(2.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final RegistryObject<PneumoTubePaintableBlock> PNEUMATIC_TUBE_PAINTABLE = BLOCKS.registerBlock(
            "pneumatic_tube_paintable", PneumoTubePaintableBlock::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<PneumoStorageAccess> PNEUMATIC_STORAGE_ACCESS = BLOCKS.registerBlock(
            "pneumatic_storage_access", PneumoStorageAccess::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<PneumoStorageClutter> PNEUMATIC_STORAGE_CLUTTER = BLOCKS.registerBlock(
            "pneumatic_storage_clutter", PneumoStorageClutter::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<PneumoStorageMono> PNEUMATIC_STORAGE_MONO = BLOCKS.registerBlock(
            "pneumatic_storage_mono", PneumoStorageMono::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<PneumoStorageImporter> PNEUMATIC_STORAGE_IMPORTER = BLOCKS.registerBlock(
            "pneumatic_storage_importer", PneumoStorageImporter::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<PneumoStorageExporter> PNEUMATIC_STORAGE_EXPORTER = BLOCKS.registerBlock(
            "pneumatic_storage_exporter", PneumoStorageExporter::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    static {
        descItem("pneumatic_tube", PNEUMATIC_TUBE, 5);
        descItem("pneumatic_tube_paintable", PNEUMATIC_TUBE_PAINTABLE, 4);
        tabItem("pneumatic_storage_access", PNEUMATIC_STORAGE_ACCESS);
        tabItem("pneumatic_storage_clutter", PNEUMATIC_STORAGE_CLUTTER);
        tabItem("pneumatic_storage_mono", PNEUMATIC_STORAGE_MONO);
        tabItem("pneumatic_storage_importer", PNEUMATIC_STORAGE_IMPORTER);
        tabItem("pneumatic_storage_exporter", PNEUMATIC_STORAGE_EXPORTER);
    }

    /** 1.7 ITooltipProvider blocks: a block item carrying the description lines. */
    private static void descItem(String name, RegistryObject<? extends Block> block, int lines) {
        RegistryObject<DescBlockItem> item = ITEMS.registerItem(name,
                p -> new DescBlockItem(block.get(), p, name, lines), Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    private static void tabItem(String name, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block);
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    public static void init() {}
}
