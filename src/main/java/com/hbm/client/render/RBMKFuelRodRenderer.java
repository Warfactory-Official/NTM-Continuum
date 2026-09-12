package com.hbm.client.render;

import com.hbm.Constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** The stacked Rods mesh and its critical-flux Cherenkov envelope. */
public final class RBMKFuelRodRenderer<T extends BlockEntityRBMKRod> extends GemBER<T, RBMKFuelRodRenderer.State> {

    private static final WavefrontObj RODS = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_element_rods.obj"));
    private static final WavefrontObj ELEMENT = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_element.obj"));
    /** {@code RBMKDials} clamps the column to this many blocks, so the stack cannot be taller. */
    private static final int MAX_LAYERS = 16;
    private static final Map<String, Surface> SKINS = new ConcurrentHashMap<>();

    private final boolean reasim;

    public RBMKFuelRodRenderer(boolean reasim) {
        this.reasim = reasim;
    }

    /**
     * The static JSON contributes only the four casing faces; the recessed channel is an OBJ Inner plus
     * Cap at every column layer, with the fuel rods inside them. The height is a dial, so every layer
     * it could reach is declared and the surplus hidden — a sealed declaration cannot grow when the
     * player changes the dial mid-session.
     */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
        for (int i = 0; i < MAX_LAYERS; i++) {
            final int layer = i;
            Gem.Part<State> segment = declaration.joint()
                    .visibleIf(state -> layer < state.layers)
                    .translate(0.5F, layer, 0.5F);

            segment.child(ELEMENT.part("Inner"), skin("rbmk_element_inner"))
                    .skin(state -> skin(state.sheet + "_inner"))
                    .light(state -> state.segmentLights[layer]);
            segment.child(ELEMENT.part("Cap"), skin("rbmk_element_top"))
                    .skin(state -> skin(state.sheet + "_top"))
                    .light(state -> state.segmentLights[layer]);
            segment.child(RODS.part("Rods"), skin("rbmk_element_fuel"))
                    .skin(state -> skin(state.sheet + "_fuel"))
                    .visibleIf(state -> state.hasRod)
                    .light(state -> state.segmentLights[layer])
                    .tint(state -> state.color);
        }

        Gem.Part<State> lid = declaration.joint()
                .visibleIf(state -> state.lid.present())
                .slide(Gem.Axes.Y, state -> state.layers);
        lid.child(RBMKColumnGeometry.LID_SIDES, skin("rbmk_element_cover_side"))
                .skin(state -> skin(state.sheet + lidPrefix(state) + "_side"))
                .light(RBMKFuelRodRenderer::lidLight);
        lid.child(RBMKColumnGeometry.LID_CAPS, skin("rbmk_element_cover_top"))
                .skin(state -> skin(state.sheet + lidPrefix(state) + "_top"))
                .light(RBMKFuelRodRenderer::lidLight);
    }

    private static String lidPrefix(State state) {
        return state.lid == RBMKBase.Lid.GLASS ? "_glass" : "_cover";
    }

    /** The cap sits above the shaft, so it takes the light of the cell one past the top layer. */
    private static int lidLight(State state) {
        return state.segmentLights[Math.min(state.layers + 1, state.segmentLights.length - 1)];
    }

    private static Surface skin(String name) {
        return SKINS.computeIfAbsent(name, id -> Surface.cutout(texture(id).toString()));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /**
     * Each stacked segment is lit by the block it occupies, not the lattice floor at the core. The cap
     * above the shaft gets one more — same lesson as the control rod: the mouth cell is occluded by
     * neighbouring columns and renders near-black.
     */
    @Override
    public void extract(T rod, State state, float partial, Vec3 camera) {
        state.hasRod = rod.hasRod;
        state.color = rod.rodColor == 0 ? -1 : 0xFF000000 | rod.rodColor;
        state.layers = RBMKDials.getColumnHeightRuleValue(rod.getLevel());
        state.flux = Math.max(rod.fluxQuantity, rod.lastFluxOut);
        state.lid = RBMKBase.lidOf(rod.getBlockState());
        state.sheet = reasim ? (rod.isModerated() ? "rbmk_element_reasim_mod" : "rbmk_element_reasim")
                : (rod.isModerated() ? "rbmk_element_mod" : "rbmk_element");
        for (int i = 0; i <= state.layers + 1 && i < state.segmentLights.length; i++) {
            state.segmentLights[i] = rod.getLevel() == null ? 0
                    : Renderers.lightAt(rod.getLevel(), rod.getBlockPos().above(i));
        }
    }

    /**
     * The Cherenkov envelope is an additive sheet every quarter block up a stack whose height is a
     * dial — a count rather than a shape, on a textureless pipeline no {@code Surface} can name.
     */
    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.flux > 5D) {
            pose.pushPose();
            pose.translate(0.5D, 0.75D, 0.5D);
            sink.submitCustomGeometry(pose, CompatRenderTypes.lightning(), (last, buffer) -> {
                for (float y = 0; y <= state.layers - 1; y += .25F) {
                    dev.golem.core.render.Vertexes.vertex(buffer, last, -.5F, y, -.5F, 0x1A66E6FF, 0, 0, OverlayTexture.NO_OVERLAY, 0, 0, 1, 0);
                    dev.golem.core.render.Vertexes.vertex(buffer, last, -.5F, y,  .5F, 0x1A66E6FF, 0, 0, OverlayTexture.NO_OVERLAY, 0, 0, 1, 0);
                    dev.golem.core.render.Vertexes.vertex(buffer, last,  .5F, y,  .5F, 0x1A66E6FF, 0, 0, OverlayTexture.NO_OVERLAY, 0, 0, 1, 0);
                    dev.golem.core.render.Vertexes.vertex(buffer, last,  .5F, y, -.5F, 0x1A66E6FF, 0, 0, OverlayTexture.NO_OVERLAY, 0, 0, 1, 0);
                }
            });
            pose.popPose();
        }
    }

    private static Identifier texture(String name) {
        return Ids.of(Constants.MOD_ID, "textures/blocks/rbmk/" + name + ".png");
    }

    public static final class State extends CompatRenderState {
        boolean hasRod;
        int color;
        int layers;
        double flux;
        String sheet = "rbmk_element";
        RBMKBase.Lid lid = RBMKBase.Lid.NONE;
        final int[] segmentLights = new int[18];
    }
}
