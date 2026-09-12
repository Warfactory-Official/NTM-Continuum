package com.hbm.client.render;

import com.hbm.util.GameTime;
import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/**
 * The fusion torus — port of the 1.12.2 {@code RenderFusionTorus}. Three moving parts:
 *
 * <ul>
 *   <li>the static <b>Torus</b> vessel,</li>
 *   <li>the <b>Magnet</b> ring, which spins at a speed the block entity ramps with stored power, and</li>
 *   <li>four <b>Bolts</b> covers, drawn only on the arms that have something attached — so a connected
 *       port visibly opens up.</li>
 * </ul>
 *
 * <p>The burning plasma is <b>three additive layers</b> over the same geometry, each scrolling at its
 * own rate: a banded base sheet, a bright travelling glow, and a sparkle field that also drifts
 * sideways. That layering <i>is</i> the plasma — the base texture alone is a set of faint grey bands,
 * so drawing it once reads as a flat block of the recipe's colour and nothing more.
 *
 * <p>1.12 draws them with {@code blendFunc(SRC_ALPHA, ONE)}, {@code depthMask(false)} and culling off,
 * scrolling each with the GL texture matrix. All three parts of that state matter and no stock render
 * type has them together, so {@link CompatRenderTypes#plasmaGlow} declares the pipeline: the blend is
 * <b>alpha-weighted</b> additive (plain {@code ONE/ONE} ignores the vertex alpha and the pulse
 * disappears), depth is tested but not written (coincident layers would otherwise z-fight and flicker),
 * and culling is off so the shell is seen from inside. The scroll is baked into the vertices, giving
 * one cached type per texture instead of a fresh one every frame.
 */
public class FusionTorusRenderer implements CompatBER<TileEntityFusionTorus, FusionTorusRenderState> {

    /** OBJ part names, indexed by arm. Upstream's numbering is not in direction order. */
    private static final String[] BOLTS = {"Bolts2", "Bolts4", "Bolts3", "Bolts1"};
    /** The glow and sparkle layers are fine detail; upstream skips them past this range. */
    private static final double DETAIL_RANGE_SQ = 100 * 100;

    private final WavefrontObj model;
    private final RenderType body;
    private final RenderType plasmaLayer;
    private final RenderType glowLayer;
    private final RenderType sparkleLayer;

    public FusionTorusRenderer() {
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/fusion/torus.obj"));
        this.body = CompatRenderTypes.entityCutout(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/fusion/torus.png"));
        this.plasmaLayer = CompatRenderTypes.plasmaGlow(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma.png"));
        this.glowLayer = CompatRenderTypes.plasmaGlow(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma_glow.png"));
        this.sparkleLayer = CompatRenderTypes.plasmaGlow(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma_sparkle.png"));
    }

    @Override public FusionTorusRenderState createRenderState() { return new FusionTorusRenderState(); }

    @Override public boolean shouldRenderOffScreen() { return true; }

    @Override public boolean shouldRender(TileEntityFusionTorus be, Vec3 cameraPosition) { return true; }

    /** The model is fifteen blocks across; it has to keep drawing from well outside its own cell. */
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityFusionTorus be, FusionTorusRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.magnet = be.prevMagnet + (be.magnet - be.prevMagnet) * partialTicks;
        System.arraycopy(be.connections, 0, state.connections, 0, state.connections.length);

        state.burning = be.plasmaEnergy > 0 && be.fusionModule.getRecipe() instanceof FusionRecipe;
        if (!state.burning) return;

        FusionRecipe recipe = (FusionRecipe) be.fusionModule.getRecipe();
        state.plasmaR = recipe.r;
        state.plasmaG = recipe.g;
        state.plasmaB = recipe.b;

        // Upstream offsets each reactor's clock by a random amount so neighbouring ones do not pulse in
        // lockstep. Deriving it from the position instead keeps that without a synced field.
        long time = GameTime.now() + Math.floorMod(be.getBlockPos().hashCode(), 30_000);

        state.plasmaAlpha = 0.35F + (float) (Math.sin(time / 1000D) * 0.25D);
        state.mainScroll = (float) (sps(time / 1000D) % 1D);
        state.glowScroll = (float) ((Math.sin(time / 2000D) % 1D) + (time / 10_000D % 1D));
        state.sparkleU = (float) (time / 500D * -1D % 1D);
        state.sparkleV = (float) (Math.sin(time / 1000D) * 0.5D % 1D);

        Vec3 centre = Vec3.atCenterOf(be.getBlockPos()).add(0, 2, 0);
        state.detailed = centre.distanceToSqr(cameraPosition) < DETAIL_RANGE_SQ;
    }

    /** Upstream's {@code BobMathUtil.sps}: a sine of a cosine, giving an eased back-and-forth drift. */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    @Override
    public void submit(FusionTorusRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);

        sink.submitCustomGeometry(poseStack, body, (pose, buffer) ->
                model.renderPart("Torus", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.magnet));
        sink.submitCustomGeometry(poseStack, body, (pose, buffer) ->
                model.renderPart("Magnet", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();

        for (int i = 0; i < BOLTS.length; i++) {
            if (!state.connections[i]) continue;
            String part = BOLTS[i];
            sink.submitCustomGeometry(poseStack, body, (pose, buffer) ->
                    model.renderPart(part, pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        }

        if (state.burning) {
            // The banded base sheet, drifting upward.
            drawLayer(state, poseStack, sink, plasmaLayer, 0F, state.mainScroll,
                    state.plasmaR, state.plasmaG, state.plasmaB, state.plasmaAlpha);

            if (state.detailed) {
                // The bright travelling band, and the sparkle field drifting across it. Both are drawn
                // at double intensity, which additive blending turns into the hot core of the glow.
                drawLayer(state, poseStack, sink, glowLayer, 0F, state.glowScroll,
                        state.plasmaR * 2F, state.plasmaG * 2F, state.plasmaB * 2F, state.plasmaAlpha * 2F);
                drawLayer(state, poseStack, sink, sparkleLayer, state.sparkleU, state.sparkleV,
                        state.plasmaR * 2F, state.plasmaG * 2F, state.plasmaB * 2F, 0.75F);
            }
        }

        poseStack.popPose();
    }

    private void drawLayer(FusionTorusRenderState state, PoseStack poseStack, SubmitSink sink,
                           RenderType layer, float u, float v, float r, float g, float b, float a) {
        int color = channel(a) << 24 | channel(r) << 16 | channel(g) << 8 | channel(b);
        // Full-bright: plasma lights itself. The render type disables culling, so single-sided geometry
        // is enough for the shell to be visible from inside.
        sink.submitCustomGeometry(poseStack, layer, (pose, buffer) ->
                model.renderPart("Plasma", pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, color, u, v, false));
    }

    /** The doubled layers overshoot 1.0 deliberately; clamp on the way into the byte. */
    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255F)));
    }
}
