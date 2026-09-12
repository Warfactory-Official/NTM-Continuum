package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.ModDecoBlocks;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.blocks.generic.BlockScaffold;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.deco.TileEntityDeco;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The OBJ-modelled decorations — boxcar, boat, satellites, the steel structural pieces and the
 * scaffold — all served by one renderer that looks its model up from the block it is on.
 *
 * <p>Each family poses differently, so the model states one joint per family and fills the one or two
 * mesh-shaped slots under it from the block. A part per decoration would put a hundred buckets on
 * every steel wall a player stacks.
 */
public class DecoObjRenderer extends GemBER<TileEntityDeco, DecoRenderState> {

    private enum Style { BOXCAR, BOAT, SAT, STEEL, SCAFFOLD }

    private record Piece(Geometry mesh, Surface surface) {}

    private record Config(List<Piece> pieces, Style style, Map<Direction, Float> yaw,
                          Map<BlockScaffold.Variant, Piece> variants) {}

    private final Map<Block, Config> configs = new HashMap<>();
    private Piece fallback;

    @Override
    protected void setup(Gem.Model<DecoRenderState> declaration) {
        build();
        declaration.offScreen().viewDistance(256);

        style(declaration, Style.BOXCAR)
                .translate(0.5F, 0.0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .child(fallback.mesh(), fallback.surface())
                .mesh(state -> piece(state, 0).mesh())
                .skin(state -> piece(state, 0).surface());

        style(declaration, Style.BOAT)
                .translate(0.5F, 0.0F, 0.5F)
                .child(fallback.mesh(), fallback.surface())
                .mesh(state -> piece(state, 0).mesh())
                .skin(state -> piece(state, 0).surface());

        // compose(translate(0.5,1.5,0.5), rotateZ(180), rotateY(legacyFacingYaw)) + the ModelBase 0.0625 scale.
        style(declaration, Style.STEEL)
                .translate(0.5F, 1.5F, 0.5F)
                .rotate(Gem.Axes.Z, 180F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .scale(0.0625F)
                .child(fallback.mesh(), fallback.surface())
                .mesh(state -> piece(state, 0).mesh())
                .skin(state -> piece(state, 0).surface());

        // The port's BER basis is the old bake's post-flip basis (cf. BombObjRenderer), so drop the old
        // rotateZ(180)+1.5 Y-offset compensation; keep the dish-orienting rotateY(90)+yaw.
        Gem.Part<DecoRenderState> satellite = style(declaration, Style.SAT)
                .translate(0.5F, 0.0F, 0.5F)
                .rotate(Gem.Axes.Y, 90F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        satellite.child(fallback.mesh(), fallback.surface())
                .mesh(state -> piece(state, 0).mesh())
                .skin(state -> piece(state, 0).surface());
        satellite.child(fallback.mesh(), fallback.surface())
                .visibleIf(state -> pieces(state).size() > 1)
                .mesh(state -> piece(state, 1).mesh())
                .skin(state -> piece(state, 1).surface());

        for (BlockScaffold.Orientation orientation : BlockScaffold.Orientation.values()) {
            Gem.Part<DecoRenderState> scaffold = declaration.joint()
                    .visibleIf(state -> styleOf(state) == Style.SCAFFOLD && orientationOf(state) == orientation)
                    .translate(0.5F, 0.5F, 0.5F);
            switch (orientation) {
                case HORIZONTAL_NS -> scaffold.rotate(Gem.Axes.Y, -90F).translate(0F, -0.5F, 0F);
                case HORIZONTAL_EW -> scaffold.rotate(Gem.Axes.Y, -180F).translate(0F, -0.5F, 0F);
                case VERTICAL_NS -> scaffold.rotate(Gem.Axes.Z, -90F).rotate(Gem.Axes.Y, -90F).translate(0F, 0F, 0.5F);
                case VERTICAL_EW -> scaffold.rotate(Gem.Axes.Z, -90F).rotate(Gem.Axes.Y, -180F).translate(-0.5F, 0F, 0F);
            }
            scaffold.child(fallback.mesh(), fallback.surface())
                    .mesh(state -> scaffoldPiece(state).mesh())
                    .skin(state -> scaffoldPiece(state).surface());
        }
    }

    private Gem.Part<DecoRenderState> style(Gem.Model<DecoRenderState> declaration, Style style) {
        return declaration.joint().visibleIf(state -> styleOf(state) == style);
    }

    private Style styleOf(DecoRenderState state) {
        Config config = configs.get(state.block);
        return config == null ? null : config.style();
    }

    private static BlockScaffold.Orientation orientationOf(DecoRenderState state) {
        return state.scaffoldOrient == null ? BlockScaffold.Orientation.HORIZONTAL_NS : state.scaffoldOrient;
    }

    private List<Piece> pieces(DecoRenderState state) {
        Config config = configs.get(state.block);
        return config == null ? List.of() : config.pieces();
    }

    private Piece piece(DecoRenderState state, int index) {
        List<Piece> pieces = pieces(state);
        return index < pieces.size() ? pieces.get(index) : fallback;
    }

    private Piece scaffoldPiece(DecoRenderState state) {
        Config config = configs.get(state.block);
        if (config == null) return fallback;
        BlockScaffold.Variant variant = state.scaffoldVariant == null
                ? BlockScaffold.Variant.STEEL : state.scaffoldVariant;
        return config.variants().getOrDefault(variant,
                config.variants().getOrDefault(BlockScaffold.Variant.STEEL, fallback));
    }

    /** The yaw maps are copied from StaticDecoBakedModels (boxcarYaw / satelliteYaw / resonatorYaw). */
    private void build() {
        Map<Direction, Float> boxcarYaw = yawMap(270F, 90F, 0F, 180F);
        Map<Direction, Float> satYaw = yawMap(180F, 0F, 90F, 270F);
        Map<Direction, Float> resonatorYaw = yawMap(180F, 0F, 270F, 90F);
        Map<Direction, Float> legacyYaw = yawMap(180F, 0F, 90F, 270F);
        Map<Direction, Float> noYaw = yawMap(0F, 0F, 0F, 0F);

        // steel structural pieces: generated OBJs (from the old ModelBase boxes, in pixel units).
        put(ModDecoBlocks.steel_wall.get(), Style.STEEL, legacyYaw, piece("models/deco/steel_wall.obj", "deco/steelwall"));
        put(ModDecoBlocks.steel_corner.get(), Style.STEEL, legacyYaw, piece("models/deco/steel_corner.obj", "deco/steelcorner"));
        // steel_roof has no per-facing rotation in the old bake (only rotateZ 180); noYaw keeps it fixed.
        put(ModDecoBlocks.steel_roof.get(), Style.STEEL, noYaw, piece("models/deco/steel_roof.obj", "deco/steelroof"));

        Geometry scaffold = WavefrontObj.load(
                dev.golem.core.Ids.of(Constants.MOD_ID, "models/blocks/scaffold.obj")).all();
        Map<BlockScaffold.Variant, Piece> scaffoldPieces = new EnumMap<>(BlockScaffold.Variant.class);
        for (BlockScaffold.Variant variant : BlockScaffold.Variant.values()) {
            scaffoldPieces.put(variant, new Piece(scaffold,
                    Surface.cutout(Constants.MOD_ID + ":textures/blocks/" + variant.texture + ".png")));
        }
        configs.put(ModSteelBlocks.steel_scaffold.get(),
                new Config(List.of(), Style.SCAFFOLD, Map.of(), scaffoldPieces));

        put(ModDecoBlocks.boxcar.get(), Style.BOXCAR, boxcarYaw, piece("models/boxcar.obj", "misc/boxcar"));
        put(ModDecoBlocks.boat.get(), Style.BOAT, noYaw, piece("models/duchessgambit.obj", "misc/duchessgambit"));

        // Satellites = a shared sat_base.obj + the per-type dish OBJ (except foeq, one combined mesh).
        Piece base = piece("models/sat_base.obj", "sat/sat_base");
        put(ModDecoBlocks.deco_sat_mapper.get(), Style.SAT, satYaw, base, piece("models/sat_mapper.obj", "sat/sat_mapper"));
        put(ModDecoBlocks.deco_sat_radar.get(), Style.SAT, satYaw, base, piece("models/sat_radar.obj", "sat/sat_radar"));
        put(ModDecoBlocks.deco_sat_scanner.get(), Style.SAT, satYaw, base, piece("models/sat_scanner.obj", "sat/sat_scanner"));
        put(ModDecoBlocks.deco_sat_laser.get(), Style.SAT, satYaw, base, piece("models/sat_laser.obj", "sat/sat_laser"));
        put(ModDecoBlocks.deco_sat_resonator.get(), Style.SAT, resonatorYaw, base, piece("models/sat_resonator.obj", "sat/sat_resonator"));
        put(ModDecoBlocks.deco_sat_foeq.get(), Style.SAT, satYaw, piece("models/sat_foeq.obj", "sat/sat_foeq"));

        fallback = base;
    }

    private static Map<Direction, Float> yawMap(float north, float south, float west, float east) {
        Map<Direction, Float> map = new HashMap<>();
        map.put(Direction.NORTH, north);
        map.put(Direction.SOUTH, south);
        map.put(Direction.WEST, west);
        map.put(Direction.EAST, east);
        return map;
    }

    private static Piece piece(String obj, String texture) {
        return new Piece(WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, obj)).all(),
                Surface.cutout(Constants.MOD_ID + ":textures/models/" + texture + ".png"));
    }

    private void put(Block block, Style style, Map<Direction, Float> yaw, Piece... pieces) {
        configs.put(block, new Config(List.of(pieces), style, yaw, Map.of()));
    }

    @Override
    public DecoRenderState createRenderState() {
        return new DecoRenderState();
    }

    @Override
    public void extract(TileEntityDeco blockEntity, DecoRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        state.block = bs.getBlock();
        state.yaw = 0F;
        state.scaffoldOrient = null;
        state.scaffoldVariant = null;
        Config cfg = configs.get(state.block);
        if (cfg != null && bs.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state.yaw = cfg.yaw().getOrDefault(bs.getValue(BlockStateProperties.HORIZONTAL_FACING), 0F);
        }
        if (state.block == ModSteelBlocks.steel_scaffold.get()) {
            state.scaffoldOrient = bs.getValue(BlockScaffold.ORIENT);
            state.scaffoldVariant = bs.getValue(BlockScaffold.VARIANT);
        }
    }
}
