package com.hbm.blocks;

import dev.golem.core.world.Compat;
import java.util.List;
import java.util.function.UnaryOperator;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockDeco;
import com.hbm.blocks.generic.BlockDeco.Shape;
import com.hbm.blocks.generic.BlockDecoObj;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The decorative {@code DecoBlock} set from 1.12.2 (12 blocks). Two rendering paths, mirroring the old
 * {@code StaticDecoBakedModels}:
 * <ul>
 *   <li><b>steel_wall/corner/roof/beam</b> — small box models → static JSON {@code elements} models
 *       ({@link BlockDeco}, {@code RenderShape.MODEL}); skins under {@code textures/models/deco/}.</li>
 *   <li><b>boxcar / boat / the 6 satellites</b> — full OBJ meshes → invisible blocks drawn by
 *       {@link com.hbm.client.render.DecoObjRenderer} ({@link BlockDecoObj} + a {@code TileEntityDeco}),
 *       the same invisible-block + BER pattern the machines use.</li>
 * </ul>
 * Hand-registered like {@link ModHazardBlocks}; assets via {@code port-tools/gen_deco_blocks.py}. The 4
 * {@code statue_elb} {@code DecoBlockAlt} easter-egg statues are deferred.
 */
public final class ModDecoBlocks {

    private ModDecoBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** The OBJ-rendered blocks — the {@code deco} block-entity type binds to all of them (see ModBlockEntities). */
    public static final java.util.List<RegistryObject<Block>> OBJ_BLOCKS = new java.util.ArrayList<>();

    // ---- steel structural pieces (OBJ-rendered from their old ModelBase box models; custom collision) ----
    public static final RegistryObject<Block> steel_wall   = decoObj("steel_wall", Shape.WALL, p -> p.strength(5.0F, 15.0F));
    public static final RegistryObject<Block> steel_corner = decoObj("steel_corner", Shape.CORNER, p -> p.strength(15.0F, 15.0F));
    public static final RegistryObject<Block> steel_roof   = decoObj("steel_roof", Shape.ROOF, p -> p.strength(5.0F, 15.0F));
    // steel_beam kept as a static JSON central post (its old model was a simple beam.obj post).
    public static final RegistryObject<Block> steel_beam   = deco("steel_beam", Shape.BEAM, p -> p.strength(5.0F, 15.0F));

    // ---- satellite dishes (OBJ-rendered, full-cube collision) ----
    public static final RegistryObject<Block> deco_sat_mapper    = decoObj("deco_sat_mapper", Shape.FULL, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_sat_radar     = decoObj("deco_sat_radar", Shape.FULL, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_sat_scanner   = decoObj("deco_sat_scanner", Shape.FULL, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_sat_laser     = decoObj("deco_sat_laser", Shape.FULL, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_sat_foeq      = decoObj("deco_sat_foeq", Shape.FULL, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_sat_resonator = decoObj("deco_sat_resonator", Shape.FULL, p -> p.strength(5.0F, 10.0F));

    // ---- vehicles (OBJ-rendered, metal sound) ----
    public static final RegistryObject<Block> boxcar = decoObj("boxcar", Shape.FULL, p -> p.strength(10.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> boat   = decoObj("boat", Shape.FULL, p -> p.strength(10.0F, 10.0F).sound(SoundType.METAL));

    /** A JSON-modelled deco block (steel pieces). */
    private static RegistryObject<Block> deco(String id, Shape shape, UnaryOperator<BlockBehaviour.Properties> props) {
        RegistryObject<Block> block = BLOCKS.registerBlock(id, p -> new BlockDeco(p, shape),
                p -> props.apply(p.noOcclusion()));
        blockItem(id, block);
        return block;
    }

    /** An OBJ-rendered (invisible + BER) deco block with the given collision shape. */
    private static RegistryObject<Block> decoObj(String id, Shape shape, UnaryOperator<BlockBehaviour.Properties> props) {
        RegistryObject<Block> block = BLOCKS.registerBlock(id, p -> new BlockDecoObj(p, shape),
                p -> props.apply(p.noOcclusion()));
        blockItem(id, block);
        OBJ_BLOCKS.add(block);
        return block;
    }

    private static void blockItem(String id, RegistryObject<Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.BLOCKS, item);
    }

    public static void init() {}
}
