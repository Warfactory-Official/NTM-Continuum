package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
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
 * Each block's model, blockstate, loot and mining tag is stated on its registration (Reg; emitted by Dejson).
 *
 * <p>The five OBJ bombs share the MK5 bombs' empty placeholder blockstate (facing 2..5) and carry an
 * authored {@code hbm:machine_obj} item definition ({@code ObjItemDeclarations}); the icon-drawn bombs
 * reproduce the 1.7 {@code getIcon} side/top split.
 */
public final class ModBombs2Blocks {

    private ModBombs2Blocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** {@code Material.iron}, 5.0F / 200.0F; the TESR-drawn ones add noOcclusion so the invisible block does not cull. */
    private static final UnaryOperator<BlockBehaviour.Properties> BOMB =
            p -> p.strength(5.0F, 200.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final UnaryOperator<BlockBehaviour.Properties> RENDERED_BOMB =
            p -> BOMB.apply(p).noOcclusion();

    /** Every bomb drops itself and is pickaxe-mined ({@code Material.iron}). */
    private static final Preset BOMB_DROP = handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE);
    /** Renderer-drawn: the MK5 bombs' empty placeholder blockstate, 1.7 facing metadata 2..5. */
    private static final Preset RENDERED = Presets.rendered(Variants.uniformRange("facing", 2, 5)).and(BOMB_DROP);

    public static final RegistryObject<NukeCustom> nuke_custom = REG.block("nuke_custom", NukeCustom::new, RENDERED_BOMB)
            .transform(RENDERED);
    public static final RegistryObject<NukeFleija> nuke_fleija = REG.block("nuke_fleija", NukeFleija::new, RENDERED_BOMB)
            .transform(RENDERED);
    public static final RegistryObject<NukeSolinium> nuke_solinium = REG.block("nuke_solinium", NukeSolinium::new, RENDERED_BOMB)
            .transform(RENDERED);
    public static final RegistryObject<NukePrototype> nuke_prototype = REG.block("nuke_prototype", NukePrototype::new, RENDERED_BOMB)
            .transform(RENDERED);
    public static final RegistryObject<NukeBalefire> nuke_fstbmb = REG.block("nuke_fstbmb", NukeBalefire::new, RENDERED_BOMB)
            .transform(RENDERED);

    // 1.7 BombThermo.getIcon: therm_top on up/down, the endo/exo icon on the four sides.
    public static final RegistryObject<BombThermo> therm_endo = REG.block("therm_endo", p -> new BombThermo(p, true), BOMB)
            .transform(Presets.column("hbm:blocks/therm_top", "hbm:blocks/therm_endo")).transform(BOMB_DROP);
    public static final RegistryObject<BombThermo> therm_exo = REG.block("therm_exo", p -> new BombThermo(p, false), BOMB)
            .transform(Presets.column("hbm:blocks/therm_top", "hbm:blocks/therm_exo")).transform(BOMB_DROP);
    // 1.7 BombFloat.getIcon: its own top on up/down, side elsewhere.
    public static final RegistryObject<BombFloat> emp_bomb = REG.block("emp_bomb", p -> new BombFloat(p, true), BOMB)
            .transform(Presets.column("hbm:blocks/bomb_emp_top", "hbm:blocks/bomb_emp_side")).transform(BOMB_DROP);
    public static final RegistryObject<BombFloat> float_bomb = REG.block("float_bomb", p -> new BombFloat(p, false), BOMB)
            .transform(Presets.column("hbm:blocks/bomb_float_top", "hbm:blocks/bomb_float")).transform(BOMB_DROP);
    public static final RegistryObject<BombFlameWar> flame_war = REG.block("flame_war", BombFlameWar::new, BOMB)
            .transform(Presets.cubeAll()).transform(BOMB_DROP);

    // 1.7: Material.ground / wood, soundTypeGlass, hardness 0.5, resistance 2.5, block tab.
    // 1.7 BlockOre: frozen_dirt and frozen_planks drop one snowball on the multiplying fortune curve; silk touch
    // keeps the block.
    public static final RegistryObject<Block> frozen_dirt = REG.<Block>block("frozen_dirt", Block::new,
                    p -> p.strength(0.5F, 2.5F).sound(SoundType.GLASS))
            .transform(Presets.cubeAll()).loot(Loot.oreMultiplied("minecraft:snowball", 1, 1));
    // 1.7 WasteEarth icons: the block's own top, frozen dirt underneath, the grass side around.
    // 1.7 WasteEarth.getItemDropped: frozen_grass drops one snowball (silk touch keeps the block).
    public static final RegistryObject<BlockFrozenGrass> frozen_grass = REG.block("frozen_grass", BlockFrozenGrass::new,
                    p -> p.strength(0.5F, 2.5F).sound(SoundType.GLASS))
            .transform(Presets.cubeBottomTop("hbm:blocks/frozen_dirt", "hbm:blocks/frozen_grass_side",
                    "hbm:blocks/frozen_grass_top"))
            .loot(Loot.oreNoFortune("minecraft:snowball", 1, 1));
    // 1.7 WasteLog: 2 + rand(3) snowballs, silk touch keeps the block.
    public static final RegistryObject<RotatedPillarBlock> frozen_log = REG.block("frozen_log", RotatedPillarBlock::new,
                    p -> p.strength(0.5F, 2.5F).sound(SoundType.GLASS))
            .transform(Presets.column("hbm:blocks/frozen_log_top", "hbm:blocks/frozen_log").states(Variants.axis()))
            .loot(Loot.oreNoFortune("minecraft:snowball", 2, 4));
    public static final RegistryObject<Block> frozen_planks = REG.<Block>block("frozen_planks", Block::new,
                    p -> p.strength(0.5F, 2.5F).sound(SoundType.GLASS))
            .transform(Presets.cubeAll()).loot(Loot.oreMultiplied("minecraft:snowball", 1, 1));

    static {
        REG.blockItem(nuke_custom).tab(HbmTab.BOMBS);
        REG.blockItem(nuke_fleija).tab(HbmTab.BOMBS);
        REG.blockItem(nuke_solinium).tab(HbmTab.BOMBS);
        REG.blockItem(nuke_prototype, ItemPrototypeBlock::new, Compat::blockDescription).tab(HbmTab.BOMBS);
        REG.blockItem(nuke_fstbmb).tab(HbmTab.BOMBS);
        REG.blockItem(therm_endo).tab(HbmTab.BOMBS);
        REG.blockItem(therm_exo).tab(HbmTab.BOMBS);
        REG.blockItem(emp_bomb).tab(HbmTab.BOMBS);
        REG.blockItem(float_bomb).tab(HbmTab.BOMBS);
        REG.blockItem(flame_war).tab(HbmTab.BOMBS);

        REG.blockItem(frozen_dirt).tab(HbmTab.BLOCKS);
        REG.blockItem(frozen_grass).tab(HbmTab.BLOCKS);
        REG.blockItem(frozen_log).tab(HbmTab.BLOCKS);
        REG.blockItem(frozen_planks).tab(HbmTab.BLOCKS);
    }

    public static void init() {}
}
