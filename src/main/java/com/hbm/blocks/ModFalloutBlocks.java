package com.hbm.blocks;

import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockFallout;
import com.hbm.blocks.generic.BlockSellafieldSlaked;
import com.hbm.blocks.generic.BlockWasteEarth;
import com.hbm.creativetabs.HbmTab;
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
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** The radioactive ash layer deposited by fallout rain (drops the {@code falloutitem}). */
    public static final RegistryObject<BlockFallout> fallout = block("fallout", BlockFallout::new,
            // forceSolidOn: 1.7 made this Material.sand, which blocks liquid movement, so water never flowed
            // into the ash. Without it a 2px replaceable layer is not "solid", flowing water spreads into every
            // cell and breaks it with its drop: an ocean next to a crater washed out the fallout as hundreds of
            // items. Vanilla's snow layer uses the same flag for the same reason.
            p -> p.strength(0.1F).sound(SoundType.SAND).noOcclusion().replaceable().forceSolidOn()
                    .isValidSpawn((state, level, pos, type) -> false))
            .model(d -> d.statesOnly().states(Variants.simple()))
            .icon(d -> d.models(Models.blockItem("hbm:block/fallout")))
            .loot(Loot.only("hbm:falloutitem"))
            .tag(BlockTag.SHOVEL);

    // ---- waste terrain ----
    public static final RegistryObject<BlockWasteEarth> waste_earth = block("waste_earth",
            p -> new BlockWasteEarth(p, false),
            // 1.7: Material.ground, soundTypeGrass, setHardness(0.6F) (explosion resistance 0.6 with no setResistance)
            p -> p.strength(0.6F).sound(SoundType.GRASS)
                    .isValidSpawn((state, level, pos, type) -> false))
            .model(d -> d.models(Models.cubeBottomTop("minecraft:block/dirt", "hbm:blocks/contamination/waste_grass_side_0", "hbm:blocks/contamination/waste_grass_top_0")).states(Variants.simple()).blockItem())
            // 1.7 WasteEarth.getItemDropped: dirt, quantity 1, no fortune; silk touch keeps the block
            .loot(Loot.oreNoFortune("minecraft:dirt", 1, 1))
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<BlockWasteEarth> waste_mycelium = block("waste_mycelium",
            p -> new BlockWasteEarth(p, true),
            // 1.7: as waste_earth plus setLightLevel(1F), i.e. light 15
            p -> p.strength(0.6F).sound(SoundType.GRASS).lightLevel(s -> 15)
                    .isValidSpawn((state, level, pos, type) -> false))
            .model(d -> d.models(Models.cubeBottomTop("minecraft:block/dirt", "hbm:blocks/contamination/waste_mycelium_side_0", "hbm:blocks/contamination/waste_mycelium_top")).states(Variants.simple()).blockItem())
            .loot(Loot.oreNoFortune("minecraft:dirt", 1, 1))
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<Block> waste_trinitite = block("waste_trinitite", Block::new,
            p -> p.strength(0.5F, 2.5F).sound(SoundType.SAND))
            // 1.7 BlockTrinitite: four icons picked per position (getIcon(world, x, y, z) hashes the coordinates);
            // the modern spelling of a position-picked texture is a four-model weighted variant pool.
            .model(d -> d.models(Models.group().and("_0", Models.cubeAll("hbm:blocks/waste_trinitite_0")).and("_1", Models.cubeAll("hbm:blocks/waste_trinitite_1")).and("_2", Models.cubeAll("hbm:blocks/waste_trinitite_2")).and("_3", Models.cubeAll("hbm:blocks/waste_trinitite_3"))).states(Variants.pool(4)))
            .icon(d -> d.models(Models.blockItem("hbm:block/waste_trinitite_0")))
            // 1.7 BlockOre (noFortune): one trinitite; silk touch keeps the block
            .loot(Loot.oreNoFortune("hbm:trinitite", 1, 1))
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<Block> waste_trinitite_red = block("waste_trinitite_red", Block::new,
            p -> p.strength(0.5F, 2.5F).sound(SoundType.SAND))
            // 1.7 BlockTrinitite: four icons picked per position (getIcon(world, x, y, z) hashes the coordinates);
            // the modern spelling of a position-picked texture is a four-model weighted variant pool.
            .model(d -> d.models(Models.group().and("_0", Models.cubeAll("hbm:blocks/waste_trinitite_red_0")).and("_1", Models.cubeAll("hbm:blocks/waste_trinitite_red_1")).and("_2", Models.cubeAll("hbm:blocks/waste_trinitite_red_2")).and("_3", Models.cubeAll("hbm:blocks/waste_trinitite_red_3"))).states(Variants.pool(4)))
            .icon(d -> d.models(Models.blockItem("hbm:block/waste_trinitite_red_0")))
            // 1.7 BlockOre (noFortune): one trinitite; silk touch keeps the block
            .loot(Loot.oreNoFortune("hbm:trinitite", 1, 1))
            .tag(BlockTag.SHOVEL);
    public static final RegistryObject<RotatedPillarBlock> waste_log = block("waste_log", RotatedPillarBlock::new,
            p -> p.strength(5.0F, 2.5F).sound(SoundType.WOOD))
            .model(d -> d.models(Models.column("hbm:blocks/waste_log_top", "hbm:blocks/waste_log_side")).states(Variants.axis()).blockItem())
            // 1.7 WasteLog: 2-4 charcoal (Items.coal, damageDropped 1), no fortune; one break in 1,000 drops a single
            // burnt_bark instead; silk touch keeps the log
            .loot(Loot.oreOrRare("minecraft:charcoal", 2, 4, "hbm:burnt_bark", 0.001))
            .tag(BlockTag.AXE);
    /** 1.7 {@code WasteLeaves}: decays into falling leaf layers, sheds dead leaves, drops nothing (no silk touch). */
    public static final RegistryObject<com.hbm.blocks.generic.BlockWasteLeaves> waste_leaves = block("waste_leaves",
            com.hbm.blocks.generic.BlockWasteLeaves::new,
            p -> p.strength(0.3F, 0.3F).sound(SoundType.GRASS).noOcclusion().randomTicks())
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.nothing())
            .tag(BlockTag.HOE);

    // ---- sellafite (SHADE 0..9, tinted client-side via HbmBlockColors) ----
    public static final RegistryObject<BlockSellafieldSlaked> sellafield_slaked = sellafield("sellafield_slaked",
            0, p -> p.strength(5.0F, 6.0F))
            .model(d -> d.models(Models.group().and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked"))).and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_1"))).and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_2"))).and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_3")))).states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4))))
            .icon(d -> d.models(Models.blockItem("hbm:block/sellafield_slaked_0")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockSellafieldSlaked> sellafield_bedrock = sellafield("sellafield_bedrock",
            0, p -> p.strength(-1.0F, 6_000_000.0F))
            .model(d -> d.models(Models.group().and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked"))).and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_1"))).and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_2"))).and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_3")))).states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4))))
            .icon(d -> d.models(Models.blockItem("hbm:block/sellafield_bedrock_0")));
    // 1.7 BlockSellafieldOre: diamond / emerald / radgem x1 on the multiplying fortune curve; a plain cube without
    // a tile entity, so silk touch gives the ore back. The other two drop themselves.
    public static final RegistryObject<BlockSellafieldSlaked> ore_sellafield_diamond = sellafield("ore_sellafield_diamond",
            3, p -> p.strength(5.0F, 6.0F))
            .model(d -> d.models(Models.group().and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_diamond_0"))).and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_diamond_1"))).and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_diamond_2"))).and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_diamond_3")))).states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4))))
            .icon(d -> d.models(Models.blockItem("hbm:block/ore_sellafield_diamond_0")))
            .loot(Loot.oreMultiplied("minecraft:diamond", 1, 1))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockSellafieldSlaked> ore_sellafield_emerald = sellafield("ore_sellafield_emerald",
            3, p -> p.strength(5.0F, 6.0F))
            .model(d -> d.models(Models.group().and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_emerald_0"))).and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_emerald_1"))).and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_emerald_2"))).and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_emerald_3")))).states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4))))
            .icon(d -> d.models(Models.blockItem("hbm:block/ore_sellafield_emerald_0")))
            .loot(Loot.oreMultiplied("minecraft:emerald", 1, 1))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockSellafieldSlaked> ore_sellafield_radgem = sellafield("ore_sellafield_radgem",
            3, p -> p.strength(5.0F, 6.0F))
            .model(d -> d.models(Models.group().and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_radgem_0"))).and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_radgem_1"))).and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_radgem_2"))).and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_radgem_3")))).states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4))))
            .icon(d -> d.models(Models.blockItem("hbm:block/ore_sellafield_radgem_0")))
            .loot(Loot.oreMultiplied("hbm:gem_rad", 1, 1))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockSellafieldSlaked> ore_sellafield_schrabidium = sellafield("ore_sellafield_schrabidium",
            0, p -> p.strength(5.0F, 6.0F))
            .model(d -> d.models(Models.group().and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_schrabidium_0"))).and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_schrabidium_1"))).and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_schrabidium_2"))).and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_schrabidium_3")))).states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4))))
            .icon(d -> d.models(Models.blockItem("hbm:block/ore_sellafield_schrabidium_0")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockSellafieldSlaked> ore_sellafield_uranium_scorched = sellafield("ore_sellafield_uranium_scorched",
            0, p -> p.strength(5.0F, 6.0F))
            .model(d -> d.models(Models.group().and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_uranium_scorched_0"))).and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_uranium_scorched_1"))).and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_uranium_scorched_2"))).and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_uranium_scorched_3")))).states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4))))
            .icon(d -> d.models(Models.blockItem("hbm:block/ore_sellafield_uranium_scorched_0")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // ---- helpers ----
    private static <B extends Block> Reg.Handle<B> block(String id,
            java.util.function.Function<BlockBehaviour.Properties, B> factory,
            java.util.function.UnaryOperator<BlockBehaviour.Properties> props) {
        Reg.Handle<B> block = REG.block(id, factory, props);
        Reg.Handle<BlockItem> item = REG.blockItem(block, p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.BLOCKS, item);
        return block;
    }

    /** {@code xpBase > 0} → old gem-ore XP {@code rand(5) + 3} (3–7). */
    private static Reg.Handle<BlockSellafieldSlaked> sellafield(String id, int xpBase,
            java.util.function.UnaryOperator<BlockBehaviour.Properties> props) {
        return block(id,
                p -> new BlockSellafieldSlaked(xpBase > 0 ? UniformInt.of(xpBase, xpBase + 4) : ConstantInt.of(0), p),
                props);
    }

    public static void init() {}
}
