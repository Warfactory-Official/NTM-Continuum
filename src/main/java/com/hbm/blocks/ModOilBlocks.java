package com.hbm.blocks;

import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockLayering;
import com.hbm.blocks.generic.BlockOilDeposit;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * The special oil-chain blocks (oil wave) — the ones whose 1.12.2 behavior a generated plain block
 * can't carry. {@code ore_oil} is hand-registered here (excluded from the gen_wave1 ore set via
 * {@code HAND_ORE_IDS}) as {@link BlockOilDeposit}; the generator still emits its blockstate/model/lang
 * and mineable tag. {@code oil_pipe} is the drill bore casing (old {@code BlockNoDrop}, no creative
 * tab, empty loot). {@code oil_spill} is the worldgen vent puddle (old {@code BlockLayering}, no loot).
 */
public final class ModOilBlocks {

    private ModOilBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Live oil deposit — old {@code BlockNTMOre(OreEnum.OIL)}, hardness 5, resistance 10. */
    public static final RegistryObject<BlockOilDeposit> ore_oil = BLOCKS.registerBlock("ore_oil",
            BlockOilDeposit::new, p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops());

    /** Drill bore casing — old {@code BlockNoDrop}, Material.IRON, no creative tab, drops nothing. */
    public static final RegistryObject<Block> oil_pipe = BLOCKS.registerBlock("oil_pipe",
            Block::new, p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));

    /** Surface oil puddle (worldgen vents) — old {@code BlockLayering}, hardness 0.1, snow sound. */
    public static final RegistryObject<BlockLayering> oil_spill = BLOCKS.registerBlock("oil_spill",
            BlockLayering::new, p -> p.strength(0.1F).sound(SoundType.SNOW).noOcclusion());

    static {
        blockItem("ore_oil", ore_oil, HbmTab.RESOURCES);
        blockItem("oil_pipe", oil_pipe, null);   // old setCreativeTab(null): item exists, no tab
        blockItem("oil_spill", oil_spill, HbmTab.BLOCKS);
    }

    private static void blockItem(String id, RegistryObject<? extends Block> block, HbmTab tab) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, p -> Compat.blockDescription(p));
        if (tab != null) ModTabs.addTo(tab, item);
    }

    public static void init() {}
}
