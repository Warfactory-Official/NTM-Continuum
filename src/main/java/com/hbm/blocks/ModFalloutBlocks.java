package com.hbm.blocks;

import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockFallout;
import com.hbm.blocks.generic.BlockSellafieldSlaked;
import com.hbm.blocks.generic.BlockWasteEarth;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The fallout / nuclear-wasteland blocks: the radioactive {@code fallout} ash layer, the waste terrain
 * ({@code waste_earth}/{@code waste_mycelium}/{@code waste_trinitite(_red)}/{@code waste_log}/
 * {@code waste_leaves}), and the sellafite set ({@code sellafield_slaked}/{@code sellafield_bedrock} +
 * the five {@code ore_sellafield_*} shade-graded ores). Hand-registered like {@link ModHazardBlocks};
 * the generator ({@code gen_fallout_blocks.py}) emits only their assets. {@code waste_planks} already
 * lives in {@link ModBlocks}.
 *
 * <p>Strengths/sounds mirror the old {@code ModBlocks} registrations; the mycelium's old
 * {@code setLightLevel(0.25F)} is light 3 ({@code 0.25·15} truncated, as 1.12 did).
 */
public final class ModFalloutBlocks {

    private ModFalloutBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** The radioactive ash layer deposited by fallout rain (drops the {@code falloutitem}). */
    public static final RegistryObject<BlockFallout> fallout = block("fallout", BlockFallout::new,
            p -> p.strength(0.1F).sound(SoundType.SAND).noOcclusion().replaceable()
                    .isValidSpawn((state, level, pos, type) -> false));

    // ---- waste terrain ----
    public static final RegistryObject<BlockWasteEarth> waste_earth = block("waste_earth",
            p -> new BlockWasteEarth(p, false),
            p -> p.strength(0.5F, 1.0F).sound(SoundType.GRAVEL)
                    .isValidSpawn((state, level, pos, type) -> false));
    public static final RegistryObject<BlockWasteEarth> waste_mycelium = block("waste_mycelium",
            p -> new BlockWasteEarth(p, true),
            p -> p.strength(0.5F, 1.0F).sound(SoundType.GRAVEL).lightLevel(s -> 3)
                    .isValidSpawn((state, level, pos, type) -> false));
    public static final RegistryObject<Block> waste_trinitite = block("waste_trinitite", Block::new,
            p -> p.strength(0.5F, 2.5F).sound(SoundType.SAND));
    public static final RegistryObject<Block> waste_trinitite_red = block("waste_trinitite_red", Block::new,
            p -> p.strength(0.5F, 2.5F).sound(SoundType.SAND));
    public static final RegistryObject<RotatedPillarBlock> waste_log = block("waste_log", RotatedPillarBlock::new,
            p -> p.strength(5.0F, 2.5F).sound(SoundType.WOOD));
    public static final RegistryObject<Block> waste_leaves = block("waste_leaves", Block::new,
            p -> p.strength(0.3F, 0.3F).sound(SoundType.GRASS).noOcclusion());

    // ---- sellafite (SHADE 0..9, tinted client-side via HbmBlockColors) ----
    public static final RegistryObject<BlockSellafieldSlaked> sellafield_slaked = sellafield("sellafield_slaked",
            0, p -> p.strength(5.0F, 6.0F));
    public static final RegistryObject<BlockSellafieldSlaked> sellafield_bedrock = sellafield("sellafield_bedrock",
            0, p -> p.strength(-1.0F, 6_000_000.0F));
    public static final RegistryObject<BlockSellafieldSlaked> ore_sellafield_diamond = sellafield("ore_sellafield_diamond",
            3, p -> p.strength(5.0F, 6.0F));
    public static final RegistryObject<BlockSellafieldSlaked> ore_sellafield_emerald = sellafield("ore_sellafield_emerald",
            3, p -> p.strength(5.0F, 6.0F));
    public static final RegistryObject<BlockSellafieldSlaked> ore_sellafield_radgem = sellafield("ore_sellafield_radgem",
            3, p -> p.strength(5.0F, 6.0F));
    public static final RegistryObject<BlockSellafieldSlaked> ore_sellafield_schrabidium = sellafield("ore_sellafield_schrabidium",
            0, p -> p.strength(5.0F, 6.0F));
    public static final RegistryObject<BlockSellafieldSlaked> ore_sellafield_uranium_scorched = sellafield("ore_sellafield_uranium_scorched",
            0, p -> p.strength(5.0F, 6.0F));

    // ---- helpers ----
    private static <B extends Block> RegistryObject<B> block(String id,
            java.util.function.Function<BlockBehaviour.Properties, B> factory,
            java.util.function.UnaryOperator<BlockBehaviour.Properties> props) {
        RegistryObject<B> block = BLOCKS.registerBlock(id, factory, props);
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.BLOCKS, item);
        return block;
    }

    /** {@code xpBase > 0} → old gem-ore XP {@code rand(5) + 3} (3–7). */
    private static RegistryObject<BlockSellafieldSlaked> sellafield(String id, int xpBase,
            java.util.function.UnaryOperator<BlockBehaviour.Properties> props) {
        return block(id,
                p -> new BlockSellafieldSlaked(xpBase > 0 ? UniformInt.of(xpBase, xpBase + 4) : ConstantInt.of(0), p),
                props);
    }

    public static void init() {}
}
