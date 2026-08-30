package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** OBJ renderer for the plasma forge, preserving the legacy machine, plasma, plume, beam and item passes. */
public class PlasmaForgeRenderer implements CompatBER<TileEntityFusionPlasmaForge, PlasmaForgeRenderState> {

    private static final int FULLBRIGHT = 0xF000F0;
    private static final int BLACK = 0xFF000000;
    private static final float PLUME_SIDE = 0.125F;
    private static final float PLUME_NEAR = 1.375F;
    private static final float PLUME_FAR = 1.625F;
    private static final float PLUME_NARROW = 0.01F;
    private static final float BEAM_IN = 0.4375F;

    private final WavefrontObj model;
    private final WavefrontObj torusModel;
    private final RenderType body;
    private final RenderType inactivePlasma;
    private final RenderType plasma;
    private final RenderType glow;
    private final RenderType beam;
    private final RenderType plume;

    public PlasmaForgeRenderer() {
        model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/fusion/plasma_forge.obj"));
        torusModel = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/fusion/torus.obj"));
        body = CompatRenderTypes.entityCutout(Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma_forge.png"));
        inactivePlasma = CompatRenderTypes.entitySolid(Ids.of(Constants.MOD_ID, "textures/models/misc/white.png"));
        plasma = CompatRenderTypes.plasmaGlow(Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma.png"));
        glow = CompatRenderTypes.plasmaGlow(Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma_glow.png"));
        beam = CompatRenderTypes.plasmaGlow(Fluids.STELLAR_FLUX.getTexture());
        plume = CompatRenderTypes.lightning();
    }

    @Override public PlasmaForgeRenderState createRenderState() { return new PlasmaForgeRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityFusionPlasmaForge be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityFusionPlasmaForge be, PlasmaForgeRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.connected = be.connected;
        state.ring = be.prevRing + (be.ring - be.prevRing) * partialTicks;
        state.striker = be.armStriker.getPositions(partialTicks);
        state.jet = be.armJet.getPositions(partialTicks);
        state.burning = be.plasmaEnergySync > 0;
        state.plasmaR = be.plasmaRed;
        state.plasmaG = be.plasmaGreen;
        state.plasmaB = be.plasmaBlue;

        if (state.burning) {
            long time = System.currentTimeMillis() + be.timeOffset;
            state.plasmaAlpha = PlasmaForgeRenderMath.plasmaAlpha(time);
            state.mainScroll = PlasmaForgeRenderMath.mainScroll(time);
            state.glowScroll = PlasmaForgeRenderMath.firstGlowScroll(time);
            state.secondGlowScroll = PlasmaForgeRenderMath.secondGlowScroll(time);
        }

        state.jetPlume = PlasmaForgeRenderMath.shouldRenderJetPlume(
                be.didProcess, be.armJet.angles[2], be.armJet.prevAngles[2]);
        state.jetPlumeLength = 1F;
        Minecraft minecraft = Minecraft.getInstance();
        if (state.jetPlume && minecraft.player != null) {
            state.jetPlumeLength += minecraft.player.getRandom().nextFloat() * 0.125F;
        }

        state.showItem = false;
        state.showBeam = false;
        state.itemIsBlock = false;
        state.itemIsGui3d = false;
        state.itemBob = 0F;
        var recipe = be.plasmaModule.getRecipe();
        if (recipe == null) {
            state.outputItem.clear();
            return;
        }

        Vec3 viewer = minecraft.player == null ? cameraPosition : minecraft.player.position();
        BlockPosLike display = new BlockPosLike(be.getBlockPos().getX() + 0.5D,
                be.getBlockPos().getY() + 1D, be.getBlockPos().getZ() + 0.5D);
        double distanceSq = viewer.distanceToSqr(display.x, display.y, display.z);
        state.showBeam = PlasmaForgeRenderMath.within(distanceSq, 50D);
        if (PlasmaForgeRenderMath.within(distanceSq, 35D)) {
            ItemStack icon = recipe.getIcon();
            if (!icon.isEmpty()) {
                ItemStack one = icon.copy();
                one.setCount(1);
                state.outputItem.extract(be.getLevel(), one, ItemDisplayContext.GROUND);
                state.itemIsBlock = one.getItem() instanceof BlockItem;
                state.itemIsGui3d = state.itemIsBlock && state.outputItem.isGui3d();
                state.showItem = !state.outputItem.isEmpty();
                if (minecraft.player != null) {
                    state.itemBob = PlasmaForgeRenderMath.itemBob(minecraft.player.tickCount, partialTicks);
                }
            }
        }
        if (minecraft.player != null) {
            state.beamScroll = PlasmaForgeRenderMath.beamScroll(minecraft.player.tickCount, partialTicks);
        }
        if (!state.showItem) state.outputItem.clear();
    }

    @Override
    public void submit(PlasmaForgeRenderState state, PoseStack ps, SubmitSink sink, CameraInfo camera) {
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(90F + state.yaw));

        if (state.connected) {
            ps.pushPose();
            ps.translate(-2F, 0F, 0F);
            sink.submitCustomGeometry(ps, CompatRenderTypes.entityCutout(
                            Ids.of(Constants.MOD_ID, "textures/models/fusion/torus.png")),
                    (pose, buffer) -> torusModel.renderPart("Bolts1", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
            ps.popPose();
        }
        part(sink, ps, "Body", state.lightCoords);
        renderPlasma(state, ps, sink);
        renderRecipeItem(state, ps, sink);
        if (state.showBeam) renderBeam(state, ps, sink);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) state.ring));
        renderStriker(state, ps, sink);
        renderJet(state, ps, sink);
        ps.popPose();
        ps.popPose();
    }

    private void renderPlasma(PlasmaForgeRenderState state, PoseStack ps, SubmitSink sink) {
        if (!state.burning) {
            sink.submitCustomGeometry(ps, inactivePlasma, (pose, buffer) ->
                    model.renderPart("Plasma", pose, buffer, FULLBRIGHT, OverlayTexture.NO_OVERLAY, BLACK));
            return;
        }
        plasmaLayer(state, ps, sink, plasma, state.mainScroll, state.plasmaAlpha);
        plasmaLayer(state, ps, sink, glow, state.glowScroll, PlasmaForgeRenderMath.GLOW_COLOR_SCALE);
        plasmaLayer(state, ps, sink, glow, state.secondGlowScroll, PlasmaForgeRenderMath.GLOW_COLOR_SCALE);
    }

    private void renderStriker(PlasmaForgeRenderState s, PoseStack ps, SubmitSink sink) {
        part(sink, ps, "SliderStriker", s.lightCoords);
        ps.pushPose();
        ps.translate(-2.75F, 2.5F, 0F); ps.mulPose(Axis.ZP.rotationDegrees((float) -s.striker[0])); ps.translate(2.75F, -2.5F, 0F);
        part(sink, ps, "ArmLowerStriker", s.lightCoords);
        ps.translate(-2.75F, 3.75F, 0F); ps.mulPose(Axis.ZP.rotationDegrees((float) -s.striker[1])); ps.translate(2.75F, -3.75F, 0F);
        part(sink, ps, "ArmUpperStriker", s.lightCoords);
        ps.translate(-1.5F, 3.75F, 0F); ps.mulPose(Axis.ZP.rotationDegrees((float) -s.striker[2])); ps.translate(1.5F, -3.75F, 0F);
        part(sink, ps, "StrikerMount", s.lightCoords);
        ps.pushPose();
        ps.translate(0F, 3.375F, 0.5F); ps.mulPose(Axis.XP.rotationDegrees((float) s.striker[3])); ps.translate(0F, -3.375F, -0.5F);
        part(sink, ps, "StrikerRight", s.lightCoords); ps.translate(0F, -s.striker[4], 0F); part(sink, ps, "PistonRight", s.lightCoords); ps.popPose();
        ps.pushPose();
        ps.translate(0F, 3.375F, -0.5F); ps.mulPose(Axis.XP.rotationDegrees((float) -s.striker[3])); ps.translate(0F, -3.375F, 0.5F);
        part(sink, ps, "StrikerLeft", s.lightCoords); ps.translate(0F, -s.striker[5], 0F); part(sink, ps, "PistonLeft", s.lightCoords); ps.popPose();
        ps.popPose();
    }

    private void renderJet(PlasmaForgeRenderState s, PoseStack ps, SubmitSink sink) {
        part(sink, ps, "SliderJet", s.lightCoords);
        ps.pushPose();
        ps.translate(2.75F, 2.5F, 0F); ps.mulPose(Axis.ZP.rotationDegrees((float) s.jet[0])); ps.translate(-2.75F, -2.5F, 0F);
        part(sink, ps, "ArmLowerJet", s.lightCoords);
        ps.translate(2.75F, 3.75F, 0F); ps.mulPose(Axis.ZP.rotationDegrees((float) s.jet[1])); ps.translate(-2.75F, -3.75F, 0F);
        part(sink, ps, "ArmUpperJet", s.lightCoords);
        ps.translate(1.5F, 3.75F, 0F); ps.mulPose(Axis.ZP.rotationDegrees((float) s.jet[2])); ps.translate(-1.5F, -3.75F, 0F);
        part(sink, ps, "Jet", s.lightCoords);
        if (s.jetPlume) renderJetPlume(s, ps, sink);
        ps.popPose();
    }

    private void renderJetPlume(PlasmaForgeRenderState s, PoseStack ps, SubmitSink sink) {
        int solid = argb(1F, s.plasmaR, s.plasmaG, s.plasmaB);
        int fade = argb(0F, s.plasmaR, s.plasmaG, s.plasmaB);
        sink.submitCustomGeometry(ps, plume, (pose, buffer) -> {
            float narrow = PLUME_NARROW;
            float outerLength = s.jetPlumeLength;
            emitPlumeQuad(pose, buffer, PLUME_NEAR, 3F, PLUME_SIDE, PLUME_FAR, 3F, PLUME_SIDE,
                    PLUME_FAR - narrow, 3F - outerLength, PLUME_SIDE - narrow,
                    PLUME_NEAR + narrow, 3F - outerLength, PLUME_SIDE - narrow, solid, fade);
            emitPlumeQuad(pose, buffer, PLUME_NEAR, 3F, -PLUME_SIDE, PLUME_FAR, 3F, -PLUME_SIDE,
                    PLUME_FAR - narrow, 3F - outerLength, -PLUME_SIDE + narrow,
                    PLUME_NEAR + narrow, 3F - outerLength, -PLUME_SIDE + narrow, solid, fade);
            emitPlumeQuad(pose, buffer, PLUME_NEAR, 3F, PLUME_SIDE, PLUME_NEAR, 3F, -PLUME_SIDE,
                    PLUME_NEAR + narrow, 3F - outerLength, -PLUME_SIDE + narrow,
                    PLUME_NEAR + narrow, 3F - outerLength, PLUME_SIDE - narrow, solid, fade);
            emitPlumeQuad(pose, buffer, PLUME_FAR, 3F, PLUME_SIDE, PLUME_FAR, 3F, -PLUME_SIDE,
                    PLUME_FAR - narrow, 3F - outerLength, -PLUME_SIDE + narrow,
                    PLUME_FAR - narrow, 3F - outerLength, PLUME_SIDE - narrow, solid, fade);

            narrow = PlasmaForgeRenderMath.OUTER_PLUME_NARROW;
            outerLength *= 1.5F;
            emitPlumeQuad(pose, buffer, PLUME_NEAR, 3F, PLUME_SIDE, PLUME_FAR, 3F, PLUME_SIDE,
                    PLUME_FAR - narrow, 3F - outerLength, PLUME_SIDE - narrow,
                    PLUME_NEAR + narrow, 3F - outerLength, PLUME_SIDE - narrow, solid, fade);
            emitPlumeQuad(pose, buffer, PLUME_NEAR, 3F, -PLUME_SIDE, PLUME_FAR, 3F, -PLUME_SIDE,
                    PLUME_FAR - narrow, 3F - outerLength, -PLUME_SIDE + narrow,
                    PLUME_NEAR + narrow, 3F - outerLength, -PLUME_SIDE + narrow, solid, fade);
            emitPlumeQuad(pose, buffer, PLUME_NEAR, 3F, PLUME_SIDE, PLUME_NEAR, 3F, -PLUME_SIDE,
                    PLUME_NEAR + narrow, 3F - outerLength, -PLUME_SIDE + narrow,
                    PLUME_NEAR + narrow, 3F - outerLength, PLUME_SIDE - narrow, solid, fade);
            emitPlumeQuad(pose, buffer, PLUME_FAR, 3F, PLUME_SIDE, PLUME_FAR, 3F, -PLUME_SIDE,
                    PLUME_FAR - narrow, 3F - outerLength, -PLUME_SIDE + narrow,
                    PLUME_FAR - narrow, 3F - outerLength, PLUME_SIDE - narrow, solid, fade);
        });
    }

    private void renderRecipeItem(PlasmaForgeRenderState s, PoseStack ps, SubmitSink sink) {
        if (!s.showItem) return;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(90F));
        ps.translate(0F, 1.75F, 0F);
        if (s.itemIsBlock) {
            if (s.itemIsGui3d) ps.translate(0F, -0.0625F, 0F);
            else ps.scale(0.5F, 0.5F, 0.5F);
        } else {
            ps.mulPose(Axis.YP.rotationDegrees(90F));
        }
        ps.translate(0F, s.itemBob, 0F);
        ps.scale(1.5F, 1.5F, 1.5F);
        ps.translate(0F, s.outputItem.droppedItemYOffset(), 0F);
        sink.submitItem(ps, s.outputItem, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }

    private void renderBeam(PlasmaForgeRenderState s, PoseStack ps, SubmitSink sink) {
        float offset = s.beamScroll;
        float top = PlasmaForgeRenderMath.BEAM_TOP;
        float bottom = PlasmaForgeRenderMath.BEAM_BOTTOM;
        float bottomUv = PlasmaForgeRenderMath.beamBottomUv(offset);
        float topUv = PlasmaForgeRenderMath.beamTopUv(offset);
        float height = top;
        int solid = argb(1F, 1F, 1F, 1F);
        int fade = argb(0F, 1F, 1F, 1F);
        sink.submitCustomGeometry(ps, beam, (pose, buffer) -> {
            emitBeamQuad(pose, buffer, -BEAM_IN, bottom, BEAM_IN, bottomUv, 0F, solid,
                    -BEAM_IN, height, BEAM_IN, topUv, 0F, fade,
                    -BEAM_IN, height, -BEAM_IN, topUv, 1F, fade,
                    -BEAM_IN, bottom, -BEAM_IN, bottomUv, 1F, solid);
            emitBeamQuad(pose, buffer, BEAM_IN, height, BEAM_IN, topUv, 0F, fade,
                    BEAM_IN, bottom, BEAM_IN, bottomUv, 0F, solid,
                    BEAM_IN, bottom, -BEAM_IN, bottomUv, 1F, solid,
                    BEAM_IN, height, -BEAM_IN, topUv, 1F, fade);
            emitBeamQuad(pose, buffer, BEAM_IN, bottom, BEAM_IN, bottomUv, 0F, solid,
                    BEAM_IN, height, BEAM_IN, topUv, 0F, fade,
                    -BEAM_IN, height, BEAM_IN, topUv, 1F, fade,
                    -BEAM_IN, bottom, BEAM_IN, bottomUv, 1F, solid);
            emitBeamQuad(pose, buffer, BEAM_IN, height, -BEAM_IN, topUv, 0F, fade,
                    BEAM_IN, bottom, -BEAM_IN, bottomUv, 0F, solid,
                    -BEAM_IN, bottom, -BEAM_IN, bottomUv, 1F, solid,
                    -BEAM_IN, height, -BEAM_IN, topUv, 1F, fade);
        });
    }

    private void part(SubmitSink sink, PoseStack ps, String name, int light) {
        sink.submitCustomGeometry(ps, body,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    private void plasmaLayer(PlasmaForgeRenderState s, PoseStack ps, SubmitSink sink, RenderType type,
                             float scroll, float colorScale) {
        int color = argb(1F, s.plasmaR * colorScale, s.plasmaG * colorScale, s.plasmaB * colorScale);
        sink.submitCustomGeometry(ps, type, (pose, buffer) ->
                model.renderPart("Plasma", pose, buffer, FULLBRIGHT, OverlayTexture.NO_OVERLAY,
                        color, 0F, scroll, false));
    }

    private static void emitPlumeQuad(PoseStack.Pose pose, VertexConsumer buffer,
                                      float x1, float y1, float z1, float x2, float y2, float z2,
                                      float x3, float y3, float z3, float x4, float y4, float z4,
                                      int solid, int fade) {
        Vertexes.color(buffer, pose, x1, y1, z1, solid);
        Vertexes.color(buffer, pose, x2, y2, z2, solid);
        Vertexes.color(buffer, pose, x3, y3, z3, fade);
        Vertexes.color(buffer, pose, x4, y4, z4, fade);
    }

    private static void emitBeamQuad(PoseStack.Pose pose, VertexConsumer buffer,
                                     float x1, float y1, float z1, float u1, float v1, int c1,
                                     float x2, float y2, float z2, float u2, float v2, int c2,
                                     float x3, float y3, float z3, float u3, float v3, int c3,
                                     float x4, float y4, float z4, float u4, float v4, int c4) {
        beamVertex(pose, buffer, x1, y1, z1, u1, v1, c1);
        beamVertex(pose, buffer, x2, y2, z2, u2, v2, c2);
        beamVertex(pose, buffer, x3, y3, z3, u3, v3, c3);
        beamVertex(pose, buffer, x4, y4, z4, u4, v4, c4);
    }

    private static void beamVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                                   float u, float v, int color) {
        Vertexes.vertex(buffer, pose, x, y, z, color, u, v, OverlayTexture.NO_OVERLAY,
                FULLBRIGHT, 0F, 1F, 0F);
    }

    private static int argb(float alpha, float red, float green, float blue) {
        return channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, (int) (value * 255F)));
    }

    private record BlockPosLike(double x, double y, double z) {}
}
