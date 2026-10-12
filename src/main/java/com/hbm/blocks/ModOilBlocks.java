package com.hbm.blocks;

import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockLayering;
import com.hbm.blocks.generic.BlockOilDeposit;
import com.hbm.creativetabs.HbmTab;
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
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** Live oil deposit — old {@code BlockNTMOre(OreEnum.OIL)}, hardness 5, resistance 10. */
    public static final RegistryObject<BlockOilDeposit> ore_oil = REG.block("ore_oil",
            BlockOilDeposit::new, p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll())
            // 1.7 BlockOre: one oil_tar (crude) on the multiplying fortune curve; canSilkHarvest is false for ore_oil
            .loot(Loot.dropsMultiplied("hbm:oil_tar_crude", 1, 1))
            .tag(BlockTag.PICKAXE);

    /**
     * Drill bore casing — old {@code BlockNoDrop}, Material.IRON, no creative tab. It drops nothing, but it is a
     * plain opaque cube without a tile entity, so 1.7's default {@code canSilkHarvest} lets silk touch keep it.
     */
    public static final RegistryObject<Block> oil_pipe = REG.block("oil_pipe",
            Block::new, p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL))
            .transform(Presets.cubeAll())
            .loot(Loot.silkTouchOnly())
            .tag(BlockTag.PICKAXE);

    /** Surface oil puddle (worldgen vents) — old {@code BlockLayering}, hardness 0.1, snow sound. */
    public static final RegistryObject<BlockLayering> oil_spill = REG.block("oil_spill",
            BlockLayering::new, p -> p.strength(0.1F).sound(SoundType.SNOW).noOcclusion())
            .model(d -> d.models(Models.group()
                            .and("_full", Models.cubeAll("hbm:blocks/oil_spill"))
                            .and("_height10", layer(10))
                            .and("_height12", layer(12))
                            .and("_height14", layer(14))
                            .and("_height2", layer(2))
                            .and("_height4", layer(4))
                            .and("_height6", layer(6))
                            .and("_height8", layer(8)))
                    .states(Variants.select("layers", "1", "_height2", "2", "_height4", "3", "_height6", "4", "_height8",
                            "5", "_height10", "6", "_height12", "7", "_height14", "8", "_full")))
            .loot(Loot.nothing());

    static {
        blockItem(ore_oil, HbmTab.RESOURCES);
        blockItem(oil_pipe, null);   // old setCreativeTab(null): item exists, no tab
        blockItem(oil_spill, HbmTab.BLOCKS).transform(Presets.shares("hbm:block/oil_spill_height2"));
    }

    private static Reg.Handle<BlockItem> blockItem(RegistryObject<? extends Block> block, HbmTab tab) {
        Reg.Handle<BlockItem> item = REG.blockItem(block, p -> Compat.blockDescription(p));
        return tab != null ? item.tab(tab) : item;
    }

    /** One snow-layer height of the puddle (vanilla's snow_height models, retextured). */
    private static dev.golem.core.dejson.Shape layer(int height) {
        return Models.parented("minecraft:block/snow_height" + height,
                Slots.of("particle", "hbm:blocks/oil_spill", "texture", "hbm:blocks/oil_spill"));
    }

    public static void init() {}
}
