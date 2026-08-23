package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** The stacked Rods mesh and its critical-flux Cherenkov envelope. */
public final class RBMKFuelRodRenderer<T extends BlockEntityRBMKRod> implements CompatBER<T, RBMKFuelRodRenderer.State> {
    private static final WavefrontObj RODS = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_element_rods.obj"));
    private static final WavefrontObj ELEMENT = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_element.obj"));
    private final boolean reasim;
    public RBMKFuelRodRenderer(boolean reasim) { this.reasim = reasim; }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int viewDistance() { return 256; }

    @Override public void extract(T rod, State state, float partial, Vec3 camera) {
        state.hasRod = rod.hasRod;
        state.color = rod.rodColor == 0 ? -1 : 0xFF000000 | rod.rodColor;
        state.layers = RBMKDials.getColumnHeightRuleValue(rod.getLevel());
        state.flux = Math.max(rod.fluxQuantity, rod.lastFluxOut);
        state.lid = RBMKBase.lidOf(rod.getBlockState());
        state.sheet = reasim ? (rod.isModerated() ? "rbmk_element_reasim_mod" : "rbmk_element_reasim")
                : (rod.isModerated() ? "rbmk_element_mod" : "rbmk_element");
        // Each stacked segment is lit by the block it occupies, not the lattice floor at the core. The
        // cap above the shaft gets one more — same lesson as the control rod: the mouth cell is
        // occluded by neighbouring columns and renders near-black.
        for (int i = 0; i <= state.layers + 1 && i < state.segmentLights.length; i++)
            state.segmentLights[i] = rod.getLevel() == null ? 0
                    : Renderers.lightAt(rod.getLevel(), rod.getBlockPos().above(i));
    }

    @Override public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        Identifier fuel = texture(state.sheet + "_fuel");
        Identifier inner = texture(state.sheet + "_inner");
        Identifier top = texture(state.sheet + "_top");
        for (int i = 0; i < state.layers; i++) {
            // The static JSON contributes only the four casing faces. The original model's recessed
            // channel is an OBJ Inner plus Cap at every physical column layer.
            final int light = state.segmentLights[i];
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(inner),
                    (last, buffer) -> ELEMENT.renderPart("Inner", last, buffer, light, OverlayTexture.NO_OVERLAY));
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(top),
                    (last, buffer) -> ELEMENT.renderPart("Cap", last, buffer, light, OverlayTexture.NO_OVERLAY));
            if (state.hasRod) sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(fuel),
                    (last, buffer) -> RODS.renderPart("Rods", last, buffer, light, OverlayTexture.NO_OVERLAY, state.color));
            pose.translate(0D, 1D, 0D);
        }
        int segmentLight = state.segmentLights[Math.min(state.layers + 1, state.segmentLights.length - 1)];
        pose.popPose();
        if (state.lid.present()) {
            String prefix = state.lid == RBMKBase.Lid.GLASS ? "_glass" : "_cover";
            pose.pushPose();
            // The body above was centred with (+0.5, +0.5); the popPose dropped that, and the box spans
            // [0,1] in block space — without the shift back it hangs off the column corner.
            pose.translate(0.5D, state.layers, 0.5D);
            final int lidLight = segmentLight;
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(texture(state.sheet + prefix + "_side")),
                    (last, buffer) -> RBMKColumnGeometry.box(last, buffer, 0, 0, 0, 1, .25F, 1, lidLight, 0, false));
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(texture(state.sheet + prefix + "_top")),
                    (last, buffer) -> RBMKColumnGeometry.box(last, buffer, 0, 0, 0, 1, .25F, 1, 0, lidLight, true));
            pose.popPose();
        }
        // The original emits one additive horizontal sheet every quarter block while critical.  This uses
        // the shared glow render type so all targets retain its blend/depth ordering.
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
    private static Identifier texture(String name) { return Ids.of(Constants.MOD_ID, "textures/blocks/rbmk/" + name + ".png"); }

    public static final class State extends CompatRenderState {
        boolean hasRod;
        int color;
        int layers;
        double flux;
        String sheet;
        RBMKBase.Lid lid;
        final int[] segmentLights = new int[18];
    }
}
