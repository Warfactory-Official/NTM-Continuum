package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.TextureStrip;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.generic.DecoModelBlock;
import com.hbm.blocks.ModDecoContent;
import com.hbm.tileentity.deco.TileEntityDecoModel;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * The CRTs (fullbright, animated screens) and the pole satellite receiver (a 1.7 TESR whose ModelBase mesh faces inward,
 * so it needs culling off); the rest of the family is chunk models ({@link StaticObjModels}).
 */
public class DecoModelRenderer extends GemBER<TileEntityDecoModel, DecoModelRenderState> {

    private record Skin(WavefrontObj model, Surface surface) {}

    private Skin satellite;
    private final Map<Block, Skin> variants = new HashMap<>();
    private Geometry crtMonitor, crtScreen;
    /** {@code crt_blinking.png} is an animated strip; 1.7 drew its atlas sprite, so each frame is its own mesh. */
    private TextureStrip blinking;
    private Geometry[] blinkingMonitor, blinkingScreen;

    private static Skin skin(String obj, String texture) {
        return new Skin(WavefrontObj.load(Ids.of(Constants.MOD_ID, obj)), Surface.cutoutNoCull(Constants.MOD_ID + ":" + texture));
    }

    private void build() {
        satellite = skin("models/deco/pole_satellite_receiver.obj", "textures/models/deco/polesatellitereceiver.png");
        Skin crtSkin = skin("models/blocks/crt.obj", "textures/blocks/crt_clean.png");
        crtMonitor = crtSkin.model().part("Monitor");
        crtScreen = crtSkin.model().part("Screen");
        variants.put(ModDecoContent.deco_crt_clean.get(), crtSkin);
        variants.put(ModDecoContent.deco_crt_broken.get(), skin("models/blocks/crt.obj", "textures/blocks/crt_broken.png"));
        Skin blinkingSkin = skin("models/blocks/crt.obj", "textures/blocks/crt_blinking.png");
        variants.put(ModDecoContent.deco_crt_blinking.get(), blinkingSkin);
        blinking = TextureStrip.of(Ids.parse(blinkingSkin.surface().texture()));
        blinkingMonitor = new Geometry[blinking.frames()];
        blinkingScreen = new Geometry[blinking.frames()];
        for (int i = 0; i < blinking.frames(); i++) {
            WavefrontObj frame = blinkingSkin.model().stripFrame(i, blinking.frames());
            blinkingMonitor[i] = frame.part("Monitor");
            blinkingScreen[i] = frame.part("Screen");
        }
        variants.put(ModDecoContent.deco_crt_bsod.get(), skin("models/blocks/crt.obj", "textures/blocks/crt_bsod.png"));
    }

    private static boolean is(DecoModelRenderState state, DecoModelBlock.Kind kind) {
        return state.block instanceof DecoModelBlock model && model.kind == kind;
    }

    @Override
    protected void setup(Gem.Model<DecoModelRenderState> declaration) {
        build();
        declaration.viewDistance(256);

        // RenderPoleSatelliteReceiver: TESR basis translate(0.5,1.5,0.5), rotateZ(180), rotateY(meta), scale(1/16).
        declaration.joint()
                .visibleIf(state -> state.block == ModDecoContent.pole_satellite_receiver.get())
                .translate(0.5F, 1.5F, 0.5F)
                .rotate(Gem.Axes.Z, 180F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .scale(0.0625F)
                .child(satellite.model().all(), satellite.surface());

        // RenderCRT: translate(x+0.5, y, z+0.5), rotateAroundY(quadrant); Monitor lit normally, Screen fullbright for the blinking/BSOD subtypes (old meta >= 8).
        Gem.Part<DecoModelRenderState> crt = declaration.joint()
                .visibleIf(state -> state.block instanceof DecoModelBlock model && model.kind.crt())
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        crt.child(crtMonitor, variants.get(ModDecoContent.deco_crt_clean.get()).surface())
                .mesh(state -> is(state, DecoModelBlock.Kind.CRT_BLINKING) ? blinkingMonitor[state.frame] : crtMonitor)
                .skin(state -> variants.getOrDefault(state.block, variants.get(ModDecoContent.deco_crt_clean.get())).surface());
        crt.child(crtScreen, variants.get(ModDecoContent.deco_crt_clean.get()).surface())
                .mesh(state -> is(state, DecoModelBlock.Kind.CRT_BLINKING) ? blinkingScreen[state.frame] : crtScreen)
                .skin(state -> variants.getOrDefault(state.block, variants.get(ModDecoContent.deco_crt_clean.get())).surface())
                .light(state -> is(state, DecoModelBlock.Kind.CRT_BLINKING) || is(state, DecoModelBlock.Kind.CRT_BSOD)
                        ? 0xF000F0 : state.lightCoords);
    }

    @Override
    public DecoModelRenderState createRenderState() {
        return new DecoModelRenderState();
    }

    @Override
    public void extract(TileEntityDecoModel blockEntity, DecoModelRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        state.block = bs.getBlock();
        state.frame = 0;
        if (!(state.block instanceof DecoModelBlock model)) return;
        if (model.kind == DecoModelBlock.Kind.CRT_BLINKING) state.frame = blinking.frameAt(blockEntity.getLevel().getGameTime());
        Direction look = bs.getValue(DecoModelBlock.FACING);
        state.yaw = model.kind == DecoModelBlock.Kind.SATELLITE
                // the stored ForgeDirection is the opposite of the look: look west 90, north 180, east 270
                ? switch (look) {
                    case WEST -> 90F;
                    case NORTH -> 180F;
                    case EAST -> 270F;
                    default -> 0F;
                }
                // the quadrant table of RenderCRT: looking south gives 90, west 0, north 270, east 180
                : switch (look) {
                    case SOUTH -> 90F;
                    case WEST -> 0F;
                    case NORTH -> 270F;
                    default -> 180F;
                };
    }
}
