package com.hbm.blocks;

import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockNTMGlass;
import com.hbm.blocks.generic.BlockRadResistant;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

/**
 * HBM's glass family (1.12.2 {@code BlockNTMGlass}, 12 variants).
 *
 * <p>{@code glass_quartz}, {@code glass_boron}, and {@code glass_lead} landed in earlier waves. The
 * remaining nine full-block variants are registered here against {@link BlockNTMGlass}.
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

    /** Boron and lead glass are radiation-sealing, silk-touch-only panes in current ntm-next. */
    public static final RegistryObject<BlockRadResistant> glass_boron = resistantGlass("glass_boron");
    public static final RegistryObject<BlockRadResistant> glass_lead = resistantGlass("glass_lead");

    public static final RegistryObject<BlockNTMGlass> reinforced_glass = glass("reinforced_glass", false, true,
            0.3F, 25.0F, SoundType.GLASS, 0, HbmTab.BLOCKS);
    public static final RegistryObject<BlockNTMGlass> reinforced_laminate = glass("reinforced_laminate", true, true,
            15.0F, 300.0F, SoundType.GLASS, 0, HbmTab.BLOCKS);
    public static final RegistryObject<BlockNTMGlass> glass_uranium = glass("glass_uranium", false, false,
            0.3F, 0.3F, SoundType.GLASS, 5, HbmTab.BLOCKS);
    public static final RegistryObject<BlockNTMGlass> glass_trinitite = glass("glass_trinitite", false, false,
            0.3F, 0.3F, SoundType.GLASS, 5, HbmTab.BLOCKS);
    public static final RegistryObject<BlockNTMGlass> glass_polonium = glass("glass_polonium", false, false,
            0.3F, 0.3F, SoundType.GLASS, 5, HbmTab.BLOCKS);
    public static final RegistryObject<BlockNTMGlass> glass_ash = glass("glass_ash", false, false,
            3.0F, 3.0F, SoundType.GLASS, 0, HbmTab.BLOCKS);
    public static final RegistryObject<BlockNTMGlass> glass_polarized = glass("glass_polarized", false, false,
            0.3F, 0.3F, SoundType.GLASS, 0, HbmTab.BLOCKS);
    public static final RegistryObject<BlockNTMGlass> hadron_plating_glass = glass("hadron_plating_glass", false, false,
            5.0F, 10.0F, SoundType.METAL, 0, HbmTab.MACHINES);
    public static final RegistryObject<BlockNTMGlass> hadron_analysis_glass = glass("hadron_analysis_glass", false, false,
            5.0F, 10.0F, SoundType.METAL, 0, HbmTab.MACHINES);

    static {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem("glass_quartz", glass_quartz,
                p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.BLOCKS, item);
        blockItem("glass_boron", glass_boron);
        blockItem("glass_lead", glass_lead);
    }

    private static RegistryObject<BlockRadResistant> resistantGlass(String id) {
        return BLOCKS.registerBlock(id, BlockRadResistant::new,
                p -> p.strength(0.3F).sound(SoundType.GLASS).noOcclusion()
                        .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false));
    }

    private static RegistryObject<BlockNTMGlass> glass(String id, boolean drops, boolean radResistant,
                                                        float hardness, float resistance, SoundType sound, int light,
                                                        HbmTab tab) {
        RegistryObject<BlockNTMGlass> block = BLOCKS.registerBlock(id, p -> new BlockNTMGlass(p, drops, radResistant),
                p -> p.strength(hardness, resistance).sound(sound).lightLevel(s -> light).noOcclusion()
                        .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false));
        blockItem(id, block, tab);
        return block;
    }

    private static void blockItem(String id, RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        blockItem(id, block, HbmTab.BLOCKS);
    }

    private static void blockItem(String id, RegistryObject<? extends net.minecraft.world.level.block.Block> block,
                                  HbmTab tab) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block::get, p -> Compat.blockDescription(p));
        ModTabs.addTo(tab, item);
    }

    public static void init() {}
}
