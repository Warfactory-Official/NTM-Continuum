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
    public static final RegistryObject<Block> steel_wall   = decoObj("steel_wall", Shape.WALL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 15.0F));
    public static final RegistryObject<Block> steel_corner = decoObj("steel_corner", Shape.CORNER, p -> p.requiresCorrectToolForDrops().strength(15.0F, 15.0F));
    // 1.7 ItemRenderDecoBlock: the flat steel_roof block icon in a slot, ModelSteelRoof in hand and on the ground
    public static final RegistryObject<Block> steel_roof   = decoObj("steel_roof", Shape.ROOF, p -> p.requiresCorrectToolForDrops().strength(5.0F, 15.0F))
            .andThen(h -> ItemRendererDeclarations.heldBlockIcon(h.path(), "hbm:blocks/steel_roof", "deco_block"));
    // steel_beam kept as a static JSON central post (its old model was a simple beam.obj post); the model is authored.
    public static final RegistryObject<Block> steel_beam   = deco("steel_beam", Shape.BEAM, p -> p.requiresCorrectToolForDrops().strength(5.0F, 15.0F))
            .transform(Presets.statesOnly(HORIZONTAL))
            .icon(d -> d.models(Models.blockItem("hbm:block/steel_beam")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // Satellite dishes (OBJ-rendered, full-cube collision)
    public static final RegistryObject<Block> deco_sat_mapper    = decoObj("deco_sat_mapper", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_sat_radar     = decoObj("deco_sat_radar", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_sat_scanner   = decoObj("deco_sat_scanner", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_sat_laser     = decoObj("deco_sat_laser", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_sat_foeq      = decoObj("deco_sat_foeq", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_sat_resonator = decoObj("deco_sat_resonator", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(5.0F, 10.0F));

    // Vehicles (OBJ-rendered, metal sound)
    public static final RegistryObject<Block> boxcar = decoObj("boxcar", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(10.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> boat   = decoObj("boat", Shape.FULL, p -> p.requiresCorrectToolForDrops().strength(10.0F, 10.0F).sound(SoundType.METAL));

    // Current ntm-next DecoBlock registrations: ordinary stone-sounding full cubes, hardness 5, resistance 100.
    // They drop themselves even out of an explosion.
    public static final RegistryObject<Block> deco_rbmk = solidDeco("deco_rbmk", p -> p.requiresCorrectToolForDrops().strength(5.0F, 100.0F).sound(SoundType.STONE))
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<Block> deco_rbmk_smooth = solidDeco("deco_rbmk_smooth", p -> p.requiresCorrectToolForDrops().strength(5.0F, 100.0F).sound(SoundType.STONE))
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);

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
