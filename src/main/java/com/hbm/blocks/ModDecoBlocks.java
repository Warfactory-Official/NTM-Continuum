package com.hbm.blocks;

import com.hbm.items.ItemRendererDeclarations;
import dev.golem.core.dejson.Dispatch;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockDeco;
import com.hbm.blocks.generic.BlockDeco.Shape;
import com.hbm.blocks.generic.BlockDecoObj;
import dev.golem.core.dejson.Slots;
import com.hbm.creativetabs.ModTabs;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import com.hbm.blocks.generic.BlockConcreteColored;
import com.hbm.blocks.generic.BlockConcreteColoredExt;
import com.hbm.blocks.generic.BlockLightstone;
import com.hbm.blocks.generic.HbmStairBlock;
import com.hbm.blocks.generic.MaterialCompletionBlockItem;
import com.hbm.creativetabs.HbmTab;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The decorative {@code DecoBlock} set from 1.12.2 (12 blocks). Two rendering paths, mirroring the old
 * {@code StaticDecoBakedModels}:
 * <ul>
 *   <li><b>steel_wall/corner/roof/beam</b>: small box models become static JSON {@code elements} models
 *       ({@link BlockDeco}, {@code RenderShape.MODEL}); skins under {@code textures/models/deco/}.</li>
 *   <li><b>boxcar / boat / the 6 satellites</b>: full OBJ meshes become invisible blocks drawn by
 *       {@link com.hbm.client.render.DecoObjRenderer} ({@link BlockDecoObj} + a {@code TileEntityDeco}),
 *       the same invisible-block + BER pattern the machines use.</li>
 * </ul>
 * Hand-registered like {@link ModHazardBlocks}; OBJ meshes via {@code port-tools/gen_deco_blocks.py}. Blockstate,
 * loot and mining tag are stated on each registration; the OBJ item icons are typed data in
 * {@code ObjItemDeclarations}. The 4 {@code statue_elb} {@code DecoBlockAlt} easter-egg statues are not ported.
 * 1.7's colored concrete and lightstone families live here too, one block item per state.
 */
public final class ModDecoBlocks {

    private ModDecoBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** The OBJ-rendered blocks, the {@code deco} block-entity type binds to all of them (see ModBlockEntities). */
    public static final List<RegistryObject<Block>> OBJ_BLOCKS = new ArrayList<>();

    private static final Dispatch HORIZONTAL = Variants.uniform("facing", "north", "east", "south", "west");
    /** Renderer-drawn: the blockstate points every facing at the empty placeholder; drops itself, pickaxe. */
    private static final Preset OBJ_DECO = Presets.rendered(HORIZONTAL)
            .and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE));

    // Steel structural pieces (OBJ-rendered from their old ModelBase box models; custom collision)
    public static final RegistryObject<Block> steel_wall   = decoObj("steel_wall", Shape.WALL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 9.0F));
    public static final RegistryObject<Block> steel_corner = decoObj("steel_corner", Shape.CORNER, p -> p.requiresCorrectToolForDrops().strength(15.0F, 9.0F));
    // 1.7 ItemRenderDecoBlock: the flat steel_roof block icon in a slot, ModelSteelRoof in hand and on the ground
    public static final RegistryObject<Block> steel_roof   = decoObj("steel_roof", Shape.ROOF, p -> p.requiresCorrectToolForDrops().strength(5.0F, 9.0F))
            .andThen(h -> ItemRendererDeclarations.heldBlockIcon(h.path(), "hbm:blocks/steel_roof", "deco_block"));
    // steel_beam kept as a static JSON central post (its old model was a simple beam.obj post); the model is authored.
    public static final RegistryObject<Block> steel_beam   = deco("steel_beam", Shape.BEAM, p -> p.requiresCorrectToolForDrops().strength(5.0F, 9.0F))
            .transform(Presets.statesOnly(HORIZONTAL))
            .icon(d -> d.models(Models.blockItem("hbm:block/steel_beam")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // Satellite dishes (OBJ-rendered, full-cube collision)
    public static final RegistryObject<Block> deco_sat_mapper    = decoObj("deco_sat_mapper", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 6.0F));
    public static final RegistryObject<Block> deco_sat_radar     = decoObj("deco_sat_radar", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 6.0F));
    public static final RegistryObject<Block> deco_sat_scanner   = decoObj("deco_sat_scanner", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 6.0F));
    public static final RegistryObject<Block> deco_sat_laser     = decoObj("deco_sat_laser", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 6.0F));
    public static final RegistryObject<Block> deco_sat_foeq      = decoObj("deco_sat_foeq", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 6.0F));
    public static final RegistryObject<Block> deco_sat_resonator = decoObj("deco_sat_resonator", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 6.0F));

    // Vehicles (OBJ-rendered, metal sound)
    public static final RegistryObject<Block> boxcar = decoObj("boxcar", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(10.0F, 6.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> boat   = decoObj("boat", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(10.0F, 6.0F).sound(SoundType.METAL));

    // Current ntm-next DecoBlock registrations: ordinary stone-sounding full cubes, hardness 5, resistance 100.
    // They drop themselves even out of an explosion.
    public static final RegistryObject<Block> deco_rbmk = solidDeco("deco_rbmk", p -> p.requiresCorrectToolForDrops().strength(5.0F, 60.0F).sound(SoundType.STONE))
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<Block> deco_rbmk_smooth = solidDeco("deco_rbmk_smooth", p -> p.requiresCorrectToolForDrops().strength(5.0F, 60.0F).sound(SoundType.STONE))
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);

    // 1.7 concrete_colored, concrete_colored_ext and lightstone, with their stairs

    /**
     * {@link BlockConcreteColored#legacyName}, spelled out: vanilla's {@code DyeColor} order with LIGHT_GRAY under
     * its 1.7 name "silver". Neither {@code DyeColor} nor the block class may be touched here, since this class initialises in DejsonMain's plain JVM (1.20.1 builds its registries the moment either loads).
     */
    private static final String[] LEGACY_DYE_NAMES = {"white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    public static final RegistryObject<BlockConcreteColored> CONCRETE_COLORED = REG.block(
            "concrete_colored", BlockConcreteColored::new,
            p -> p.mapColor(state -> state.getValue(BlockConcreteColored.COLOR).getMapColor())
                    .strength(15.0F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.group()
                    .and("_black", Models.cubeAll("hbm:blocks/concrete_black"))
                    .and("_blue", Models.cubeAll("hbm:blocks/concrete_blue"))
                    .and("_brown", Models.cubeAll("hbm:blocks/concrete_brown"))
                    .and("_cyan", Models.cubeAll("hbm:blocks/concrete_cyan"))
                    .and("_gray", Models.cubeAll("hbm:blocks/concrete_gray"))
                    .and("_green", Models.cubeAll("hbm:blocks/concrete_green"))
                    .and("_light_blue", Models.cubeAll("hbm:blocks/concrete_light_blue"))
                    .and("_lime", Models.cubeAll("hbm:blocks/concrete_lime"))
                    .and("_magenta", Models.cubeAll("hbm:blocks/concrete_magenta"))
                    .and("_orange", Models.cubeAll("hbm:blocks/concrete_orange"))
                    .and("_pink", Models.cubeAll("hbm:blocks/concrete_pink"))
                    .and("_purple", Models.cubeAll("hbm:blocks/concrete_purple"))
                    .and("_red", Models.cubeAll("hbm:blocks/concrete_red"))
                    .and("_silver", Models.cubeAll("hbm:blocks/concrete_silver"))
                    .and("_white", Models.cubeAll("hbm:blocks/concrete_white"))
                    .and("_yellow", Models.cubeAll("hbm:blocks/concrete_yellow")))
                    .states(Variants.select("color", "white", "_white", "orange", "_orange", "magenta", "_magenta", "light_blue", "_light_blue", "yellow", "_yellow", "lime", "_lime", "pink", "_pink", "gray", "_gray", "light_gray", "_silver", "cyan", "_cyan", "purple", "_purple", "blue", "_blue", "brown", "_brown", "green", "_green", "red", "_red", "black", "_black")))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockConcreteColoredExt> CONCRETE_COLORED_EXT = REG.block(
            "concrete_colored_ext", BlockConcreteColoredExt::new,
            p -> p.strength(15.0F, 84.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.group()
                    .and("_bronze", Models.cubeAll("hbm:blocks/concrete_colored_ext.bronze"))
                    .and("_hazard", Models.cubeAll("hbm:blocks/concrete_colored_ext.hazard"))
                    .and("_indigo", Models.cubeAll("hbm:blocks/concrete_colored_ext.indigo"))
                    .and("_machine", Models.cubeAll("hbm:blocks/concrete_colored_ext.machine"))
                    .and("_machine_stripe", Models.parented("minecraft:block/cube", Slots.of("up", "hbm:blocks/concrete_colored_ext.machine")
                    .and("down", "hbm:blocks/concrete_colored_ext.machine")
                    .and("north", "hbm:blocks/concrete_colored_ext.machine_stripe")
                    .and("south", "hbm:blocks/concrete_colored_ext.machine_stripe")
                    .and("west", "hbm:blocks/concrete_colored_ext.machine_stripe")
                    .and("east", "hbm:blocks/concrete_colored_ext.machine_stripe")
                    .and("particle", "hbm:blocks/concrete_colored_ext.machine_stripe")))
                    .and("_pink", Models.cubeAll("hbm:blocks/concrete_colored_ext.pink"))
                    .and("_purple", Models.cubeAll("hbm:blocks/concrete_colored_ext.purple"))
                    .and("_sand", Models.cubeAll("hbm:blocks/concrete_colored_ext.sand")))
                    .states(Variants.select("type", "machine", "_machine", "machine_stripe", "_machine_stripe", "indigo", "_indigo", "purple", "_purple", "pink", "_pink", "hazard", "_hazard", "sand", "_sand", "bronze", "_bronze")))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockLightstone> LIGHTSTONE = REG.block(
            "lightstone", BlockLightstone::new,
            p -> p.strength(20.0F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.group()
                    .and("_bricks", Models.cubeAll("hbm:blocks/lightstone.bricks"))
                    .and("_bricks_chiseled", Models.parented("minecraft:block/cube", Slots.of("up", "hbm:blocks/lightstone.bricks_chiseled.top")
                    .and("down", "hbm:blocks/lightstone.bricks_chiseled.top")
                    .and("north", "hbm:blocks/lightstone.bricks_chiseled")
                    .and("south", "hbm:blocks/lightstone.bricks_chiseled")
                    .and("west", "hbm:blocks/lightstone.bricks_chiseled")
                    .and("east", "hbm:blocks/lightstone.bricks_chiseled")
                    .and("particle", "hbm:blocks/lightstone.bricks_chiseled")))
                    .and("_chiseled", Models.parented("minecraft:block/cube", Slots.of("up", "hbm:blocks/lightstone.chiseled.top")
                    .and("down", "hbm:blocks/lightstone.chiseled.top")
                    .and("north", "hbm:blocks/lightstone.chiseled")
                    .and("south", "hbm:blocks/lightstone.chiseled")
                    .and("west", "hbm:blocks/lightstone.chiseled")
                    .and("east", "hbm:blocks/lightstone.chiseled")
                    .and("particle", "hbm:blocks/lightstone.chiseled")))
                    .and("_tile", Models.cubeAll("hbm:blocks/lightstone.tile"))
                    .and("_unrefined", Models.cubeAll("hbm:blocks/lightstone.unrefined")))
                    .states(Variants.select("type", "unrefined", "_unrefined", "tile", "_tile", "bricks", "_bricks", "bricks_chiseled", "_bricks_chiseled", "chiseled", "_chiseled")))
            .tag(BlockTag.PICKAXE);

    public static final List<RegistryObject<Block>> CONCRETE_COLORED_STAIRS = stairs(
            "concrete_colored_stairs", CONCRETE_COLORED, 16, 15.0F, 94.0F);
    public static final List<RegistryObject<Block>> CONCRETE_COLORED_EXT_STAIRS = stairs(
            "concrete_colored_ext_stairs", CONCRETE_COLORED_EXT, 8, 15.0F, 94.0F);
    public static final RegistryObject<Block> LIGHTSTONE_TILE_STAIRS = stair(
            "lightstone_tile_stairs", LIGHTSTONE, lightstoneStairTexture(false), 15.0F, 12.0F);
    public static final RegistryObject<Block> LIGHTSTONE_BRICKS_STAIRS = stair(
            "lightstone_bricks_stairs", LIGHTSTONE, lightstoneStairTexture(true), 15.0F, 12.0F);

    /** {@link BlockConcreteColoredExt.EnumConcreteType}'s names in metadata order (the enum is not touched here). */
    private static final String[] EXT_NAMES = {"machine", "machine_stripe", "indigo", "purple", "pink", "hazard", "sand",
            "bronze"};
    /** {@link BlockLightstone}'s types in metadata order. */
    private static final String[] LIGHTSTONE_NAMES = {"unrefined", "tile", "bricks", "bricks_chiseled", "chiseled"};

    /*
     * 1.7's metadata items, one registered block item per state as ntm-next names them:
     * concrete_<dye> (light gray is 1.7's "silver"), concrete_ext_<type>, lightstone_<type>. The blocks keep their one
     * id and named state property; each item places its state, and drops/pick-block answer the state's item. Member 0
     * is registered last so the block's own item (Block.asItem) stays the 1.7 metadata-0 item on every loader.
     */
    public static final List<RegistryObject<MaterialCompletionBlockItem>> CONCRETE_COLORED_ITEMS = stateItems(
            "concrete_colored", CONCRETE_COLORED, LEGACY_DYE_NAMES, "concrete_", "hbm:block/concrete_colored_");
    public static final List<RegistryObject<MaterialCompletionBlockItem>> CONCRETE_COLORED_EXT_ITEMS = stateItems(
            "concrete_colored_ext", CONCRETE_COLORED_EXT, EXT_NAMES, "concrete_ext_", "hbm:block/concrete_colored_ext_");
    public static final List<RegistryObject<MaterialCompletionBlockItem>> LIGHTSTONE_ITEMS = stateItems(
            "lightstone", LIGHTSTONE, LIGHTSTONE_NAMES, "lightstone_", "hbm:block/lightstone_");

    private static List<RegistryObject<MaterialCompletionBlockItem>> stateItems(String blockId,
            RegistryObject<? extends Block> block, String[] names, String idPrefix, String modelPrefix) {
        @SuppressWarnings("unchecked")
        RegistryObject<MaterialCompletionBlockItem>[] items = new RegistryObject[names.length];
        for (int k = 1; k <= names.length; k++) {
            int variant = k % names.length; // 1, 2, ..., n-1, then 0
            String name = names[variant];
            items[variant] = REG.item(idPrefix + name, p -> new MaterialCompletionBlockItem(block.get(), p, blockId,
                            variant, name), Compat::blockDescription)
                    .transform(Presets.shares(modelPrefix + name));
        }
        return List.of(items);
    }

    static {
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, 16)
                .mapToObj(ModDecoBlocks::concreteColoredItem).toList());
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, 8)
                .mapToObj(ModDecoBlocks::concreteColoredExtItem).toList());
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, 5)
                .mapToObj(ModDecoBlocks::lightstoneItem).toList());
        for (RegistryObject<Block> stair : CONCRETE_COLORED_STAIRS) {
            REG.blockItem(stair, Compat::blockDescription).tab(HbmTab.BLOCKS);
        }
        for (RegistryObject<Block> stair : CONCRETE_COLORED_EXT_STAIRS) {
            REG.blockItem(stair, Compat::blockDescription).tab(HbmTab.BLOCKS);
        }
        REG.blockItem(LIGHTSTONE_TILE_STAIRS, Compat::blockDescription).tab(HbmTab.BLOCKS);
        REG.blockItem(LIGHTSTONE_BRICKS_STAIRS, Compat::blockDescription).tab(HbmTab.BLOCKS);
    }

    private static List<RegistryObject<Block>> stairs(String prefix, Supplier<? extends Block> base, int count,
                                                      float hardness, float resistance) {
        List<RegistryObject<Block>> stairs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // the variant names come from the blocks' own enums, read without initialising the block classes
            // (a Block subclass needs a running game; this class also initialises in DejsonMain's plain JVM)
            String id = prefix + "_" + (prefix.startsWith("concrete_colored_ext")
                    ? BlockConcreteColoredExt.EnumConcreteType.VALUES[i].getSerializedName()
                    : legacyDyeName(i));
            String texture = prefix.startsWith("concrete_colored_ext") ? concreteColoredExtStairTexture(i)
                    : concreteColoredStairTexture(i);
            stairs.add(stair(id, base, texture, hardness, resistance));
        }
        return stairs;
    }

    /** A stair over one texture; drops itself, pickaxe. */
    private static RegistryObject<Block> stair(String id, Supplier<? extends Block> base, String texture,
                                                float hardness, float resistance) {
        return REG.<Block>block(id, p -> new HbmStairBlock(
                                base.get().defaultBlockState(), p),
                        p -> p.strength(hardness, resistance).sound(SoundType.STONE).requiresCorrectToolForDrops())
                .transform(Presets.shape(Models.stairsAll(texture)).states(Variants.stairs()))
                .loot(Loot.self())
                .tag(BlockTag.PICKAXE);
    }

    public static ItemStack concreteColoredItem(int variant) {
        return member(CONCRETE_COLORED_ITEMS, variant);
    }

    public static ItemStack concreteColoredExtItem(int variant) {
        return member(CONCRETE_COLORED_EXT_ITEMS, variant);
    }

    public static ItemStack lightstoneItem(int variant) {
        return member(LIGHTSTONE_ITEMS, variant);
    }

    private static ItemStack member(List<RegistryObject<MaterialCompletionBlockItem>> items, int variant) {
        return new ItemStack(items.get(Math.floorMod(variant, items.size())).get());
    }

    public static String concreteColoredStairTexture(int variant) {
        return "hbm:blocks/concrete_" + legacyDyeName(variant);
    }

    public static String concreteColoredExtStairTexture(int variant) {
        return "hbm:blocks/concrete_colored_ext."
                + BlockConcreteColoredExt.EnumConcreteType.VALUES[variant].getSerializedName();
    }

    private static String legacyDyeName(int variant) {
        return LEGACY_DYE_NAMES[Math.floorMod(variant, LEGACY_DYE_NAMES.length)];
    }

    public static String lightstoneStairTexture(boolean bricks) {
        return "hbm:blocks/lightstone." + (bricks ? "bricks" : "tile");
    }

    /** A JSON-modelled deco block (steel pieces). */
    private static Reg.Handle<Block> deco(String id, Shape shape, UnaryOperator<BlockBehaviour.Properties> props) {
        Reg.Handle<Block> block = REG.<Block>block(id, p -> new BlockDeco(p, shape), p -> props.apply(p.noOcclusion()));
        blockItem(block);
        return block;
    }

    /** An OBJ-rendered (invisible + BER) deco block with the given collision shape. */
    private static Reg.Handle<Block> decoObj(String id, Shape shape, UnaryOperator<BlockBehaviour.Properties> props) {
        Reg.Handle<Block> block = REG.<Block>block(id, p -> new BlockDecoObj(p, shape), p -> props.apply(p.noOcclusion()));
        blockItem(block);
        OBJ_BLOCKS.add(block);
        return block.transform(OBJ_DECO);
    }

    private static Reg.Handle<Block> solidDeco(String id, UnaryOperator<BlockBehaviour.Properties> props) {
        Reg.Handle<Block> block = REG.<Block>block(id, Block::new, props);
        blockItem(block);
        return block;
    }

    private static void blockItem(Reg.Handle<Block> block) {
        REG.blockItem(block, p -> Compat.blockDescription(p)).tab(HbmTab.BLOCKS);
    }

    public static void init() {}
}
