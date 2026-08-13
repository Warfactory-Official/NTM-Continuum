package com.hbm.blocks;

import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockNTMGlass;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

/**
 * HBM's glass family (1.12.2 {@code BlockNTMGlass}, 12 variants).
 *
 * <p>Only {@code glass_quartz} is here so far — it landed with the SILEX wave because it gates both
 * the SILEX's own assembly recipe and all five laser-crystal crafting recipes. The remaining eleven
 * variants (the coloured and reinforced panes) come with a dedicated glass wave; add them here.
 */
public final class ModGlassBlocks {

    private ModGlassBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /**
     * Quartz glass — hardness 1, blast resistance 40, and it <b>drops itself</b> (the old
     * {@code doesDrop = true}), unlike vanilla glass. Not rad-resistant.
     */
    public static final RegistryObject<BlockNTMGlass> glass_quartz = BLOCKS.registerBlock("glass_quartz",
            p -> new BlockNTMGlass(p, true, false),
            p -> p.strength(1.0F, 40.0F).sound(SoundType.GLASS).noOcclusion().isSuffocating((s, l, pos) -> false)
                    .isViewBlocking((s, l, pos) -> false).noLootTable());

    static {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem("glass_quartz", glass_quartz,
                p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.BLOCKS, item);
    }

    public static void init() {}
}
