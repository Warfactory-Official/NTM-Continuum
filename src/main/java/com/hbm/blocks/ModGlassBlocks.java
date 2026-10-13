package com.hbm.blocks;

import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import com.hbm.registration.Reg;
import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockNTMGlass;
import com.hbm.blocks.generic.BlockRadResistant;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * HBM's glass family (1.12.2 {@code BlockNTMGlass}, 12 variants).
 *
 * <p>The remaining nine full-block variants are registered here against {@link BlockNTMGlass}.
 */
public final class ModGlassBlocks {

    private ModGlassBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /**
     * Quartz glass: hardness 1, blast resistance 40, and it <b>drops itself</b> (the old
     * {@code doesDrop = true}), unlike vanilla glass. Not rad-resistant.
     */
    public static final RegistryObject<BlockNTMGlass> glass_quartz = REG.block("glass_quartz",
            p -> new BlockNTMGlass(p, false),
            p -> p.strength(1.0F, 40.0F).sound(SoundType.GLASS).noOcclusion().isSuffocating((s, l, pos) -> false)
                    .isViewBlocking((s, l, pos) -> false))
            .model(d -> d.models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_quartz"))).states(Variants.simple()).blockItem())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** Boron and lead glass are radiation-sealing, silk-touch-only panes in current ntm-next. */
    public static final RegistryObject<BlockRadResistant> glass_boron = resistantGlass("glass_boron")
            .model(d -> d.models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_boron"))).states(Variants.simple()).blockItem())
            .loot(Loot.silkTouchOnly());
    public static final RegistryObject<BlockRadResistant> glass_lead = resistantGlass("glass_lead")
            .model(d -> d.models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_lead"))).states(Variants.simple()).blockItem())
            .loot(Loot.silkTouchOnly());

    public static final RegistryObject<BlockNTMGlass> reinforced_glass = glass("reinforced_glass", false, true,
            0.3F, 25.0F, () -> SoundType.GLASS, 0, HbmTab.BLOCKS)
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cubeAll("hbm:blocks/reinforced_glass"))).states(Variants.simple()).blockItem())
            .loot(Loot.silkTouchOnly()) // 1.7 BlockNTMGlass: doesDrop = false, canSilkHarvest() = true
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockNTMGlass> reinforced_laminate = glass("reinforced_laminate", true, true,
            15.0F, 300.0F, () -> SoundType.GLASS, 0, HbmTab.BLOCKS)
            .model(d -> d.models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/reinforced_laminate"))).states(Variants.simple()).blockItem())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockNTMGlass> glass_uranium = glass("glass_uranium", false, false,
            0.3F, 0.3F, () -> SoundType.GLASS, 5, HbmTab.BLOCKS)
            .model(d -> d.models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_uranium"))).states(Variants.simple()).blockItem())
            .loot(Loot.silkTouchOnly()) // 1.7 BlockNTMGlass: doesDrop = false, canSilkHarvest() = true
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockNTMGlass> glass_trinitite = glass("glass_trinitite", false, false,
            0.3F, 0.3F, () -> SoundType.GLASS, 5, HbmTab.BLOCKS)
            .model(d -> d.models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_trinitite"))).states(Variants.simple()).blockItem())
            .loot(Loot.silkTouchOnly()) // 1.7 BlockNTMGlass: doesDrop = false, canSilkHarvest() = true
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockNTMGlass> glass_polonium = glass("glass_polonium", false, false,
            0.3F, 0.3F, () -> SoundType.GLASS, 5, HbmTab.BLOCKS)
            .model(d -> d.models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_polonium"))).states(Variants.simple()).blockItem())
            .loot(Loot.silkTouchOnly()) // 1.7 BlockNTMGlass: doesDrop = false, canSilkHarvest() = true
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockNTMGlass> glass_ash = glass("glass_ash", false, false,
            3.0F, 3.0F, () -> SoundType.GLASS, 0, HbmTab.BLOCKS)
            .model(d -> d.models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_ash"))).states(Variants.simple()).blockItem())
            .loot(Loot.silkTouchOnly()) // 1.7 BlockNTMGlass: doesDrop = false, canSilkHarvest() = true
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockNTMGlass> glass_polarized = glass("glass_polarized", false, false,
            0.3F, 0.3F, () -> SoundType.GLASS, 0, HbmTab.BLOCKS)
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cubeAll("hbm:blocks/glass_polarized"))).states(Variants.simple()).blockItem())
            .loot(Loot.silkTouchOnly()) // 1.7 BlockNTMGlass: doesDrop = false, canSilkHarvest() = true
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockNTMGlass> hadron_plating_glass = glass("hadron_plating_glass", false, false,
            5.0F, 10.0F, () -> SoundType.METAL, 0, HbmTab.MACHINES)
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cubeAll("hbm:blocks/hadron_plating_glass"))).states(Variants.simple()).blockItem())
            .loot(Loot.silkTouchOnly()) // 1.7 BlockNTMGlass: doesDrop = false, canSilkHarvest() = true
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockNTMGlass> hadron_analysis_glass = glass("hadron_analysis_glass", false, false,
            5.0F, 10.0F, () -> SoundType.METAL, 0, HbmTab.MACHINES)
            .model(d -> d.models(Models.renderType("minecraft:cutout", Models.cubeAll("hbm:blocks/hadron_analysis_glass"))).states(Variants.simple()).blockItem())
            .loot(Loot.silkTouchOnly()) // 1.7 BlockNTMGlass: doesDrop = false, canSilkHarvest() = true
            .tag(BlockTag.PICKAXE);

    static {
        Reg.Handle<BlockItem> item = REG.blockItem(glass_quartz,
                p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.BLOCKS, item);
        blockItem("glass_boron", glass_boron);
        blockItem("glass_lead", glass_lead);
    }

    private static Reg.Handle<BlockRadResistant> resistantGlass(String id) {
        return REG.block(id, p -> new BlockRadResistant(p) {
                    // 1.7 BlockNTMGlassCT is a BlockBreakable(.., false): faces against the same glass are not drawn
                    @Override
                    public boolean skipRendering(BlockState state,
                                                 BlockState adjacent,
                                                 Direction side) {
                        return adjacent.is(this) || super.skipRendering(state, adjacent, side);
                    }
                },
                p -> p.strength(0.3F).sound(SoundType.GLASS).noOcclusion()
                        .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false));
    }

    private static Reg.Handle<BlockNTMGlass> glass(String id, boolean drops, boolean radResistant,
                                                        float hardness, float resistance, Supplier<SoundType> sound, int light,
                                                        HbmTab tab) {
        Reg.Handle<BlockNTMGlass> block = REG.block(id, p -> new BlockNTMGlass(p, radResistant),
                p -> p.strength(hardness, resistance).sound(sound.get()).lightLevel(s -> light).noOcclusion()
                        .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false));
        blockItem(id, block, tab);
        return block;
    }

    private static void blockItem(String id, RegistryObject<? extends Block> block) {
        blockItem(id, block, HbmTab.BLOCKS);
    }

    private static void blockItem(String id, RegistryObject<? extends Block> block,
                                  HbmTab tab) {
        Reg.Handle<BlockItem> item = REG.blockItem(block, p -> Compat.blockDescription(p));
        ModTabs.addTo(tab, item);
    }

    public static void init() {}
}
