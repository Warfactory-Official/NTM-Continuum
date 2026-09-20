package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;
import com.hbm.blocks.bomb.BlockFrozenGrass;
import com.hbm.blocks.bomb.BombFlameWar;
import com.hbm.blocks.bomb.BombFloat;
import com.hbm.blocks.bomb.BombThermo;
import com.hbm.blocks.bomb.NukeBalefire;
import com.hbm.blocks.bomb.NukeCustom;
import com.hbm.blocks.bomb.NukeFleija;
import com.hbm.blocks.bomb.NukePrototype;
import com.hbm.blocks.bomb.NukeSolinium;
import com.hbm.items.bomb.ItemPrototypeBlock;

/**
 * The special bombs of original-hbm's nuke tab that are not MK5 devices, plus the four frozen blocks the
 * endothermic bomb leaves behind. All bombs were {@code Material.iron}, hardness 5, resistance 200.
 */
public final class ModBombs2Blocks {

    private ModBombs2Blocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** {@code Material.iron}, 5.0F / 200.0F; the TESR-drawn ones add noOcclusion so the invisible block does not cull. */
    private static final UnaryOperator<BlockBehaviour.Properties> BOMB =
            p -> p.strength(5.0F, 200.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final UnaryOperator<BlockBehaviour.Properties> RENDERED_BOMB =
            p -> BOMB.apply(p).noOcclusion();

    public static final RegistryObject<NukeCustom> nuke_custom = BLOCKS.registerBlock("nuke_custom", NukeCustom::new, RENDERED_BOMB);
    public static final RegistryObject<NukeFleija> nuke_fleija = BLOCKS.registerBlock("nuke_fleija", NukeFleija::new, RENDERED_BOMB);
    public static final RegistryObject<NukeSolinium> nuke_solinium = BLOCKS.registerBlock("nuke_solinium", NukeSolinium::new, RENDERED_BOMB);
    public static final RegistryObject<NukePrototype> nuke_prototype = BLOCKS.registerBlock("nuke_prototype", NukePrototype::new, RENDERED_BOMB);
    public static final RegistryObject<NukeBalefire> nuke_fstbmb = BLOCKS.registerBlock("nuke_fstbmb", NukeBalefire::new, RENDERED_BOMB);

    public static final RegistryObject<BombThermo> therm_endo = BLOCKS.registerBlock("therm_endo", p -> new BombThermo(p, true), BOMB);
    public static final RegistryObject<BombThermo> therm_exo = BLOCKS.registerBlock("therm_exo", p -> new BombThermo(p, false), BOMB);
    public static final RegistryObject<BombFloat> emp_bomb = BLOCKS.registerBlock("emp_bomb", p -> new BombFloat(p, true), BOMB);
    public static final RegistryObject<BombFloat> float_bomb = BLOCKS.registerBlock("float_bomb", p -> new BombFloat(p, false), BOMB);
    public static final RegistryObject<BombFlameWar> flame_war = BLOCKS.registerBlock("flame_war", BombFlameWar::new, BOMB);

    // 1.7: Material.ground / wood, soundTypeGlass, hardness 0.5, resistance 2.5, block tab.
    public static final RegistryObject<Block> frozen_dirt = BLOCKS.registerBlock("frozen_dirt", Block::new,
            p -> p.strength(0.5F, 2.5F).sound(SoundType.GLASS));
    public static final RegistryObject<BlockFrozenGrass> frozen_grass = BLOCKS.registerBlock("frozen_grass", BlockFrozenGrass::new,
            p -> p.strength(0.5F, 2.5F).sound(SoundType.GLASS));
    public static final RegistryObject<RotatedPillarBlock> frozen_log = BLOCKS.registerBlock("frozen_log", RotatedPillarBlock::new,
            p -> p.strength(0.5F, 2.5F).sound(SoundType.GLASS));
    public static final RegistryObject<Block> frozen_planks = BLOCKS.registerBlock("frozen_planks", Block::new,
            p -> p.strength(0.5F, 2.5F).sound(SoundType.GLASS));

    static {
        bombItem("nuke_custom", nuke_custom);
        bombItem("nuke_fleija", nuke_fleija);
        bombItem("nuke_solinium", nuke_solinium);
        RegistryObject<? extends BlockItem> prototype = ITEMS.registerItem("nuke_prototype",
                p -> new ItemPrototypeBlock(nuke_prototype.get(), p), Compat::blockDescription);
        ModTabs.addTo(HbmTab.BOMBS, prototype);
        bombItem("nuke_fstbmb", nuke_fstbmb);
        bombItem("therm_endo", therm_endo);
        bombItem("therm_exo", therm_exo);
        bombItem("emp_bomb", emp_bomb);
        bombItem("float_bomb", float_bomb);
        bombItem("flame_war", flame_war);

        ModTabs.addTo(HbmTab.BLOCKS, ITEMS.registerBlockItem("frozen_dirt", frozen_dirt));
        ModTabs.addTo(HbmTab.BLOCKS, ITEMS.registerBlockItem("frozen_grass", frozen_grass));
        ModTabs.addTo(HbmTab.BLOCKS, ITEMS.registerBlockItem("frozen_log", frozen_log));
        ModTabs.addTo(HbmTab.BLOCKS, ITEMS.registerBlockItem("frozen_planks", frozen_planks));
    }

    private static void bombItem(String id, RegistryObject<? extends Block> block) {
        ModTabs.addTo(HbmTab.BOMBS, ITEMS.registerBlockItem(id, block));
    }

    public static void init() {}
}
