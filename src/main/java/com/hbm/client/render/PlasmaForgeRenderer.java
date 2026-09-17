package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** OBJ renderer for the plasma forge, preserving the legacy machine, plasma, plume, beam and item passes. */
public class PlasmaForgeRenderer extends GemBER<TileEntityFusionPlasmaForge, PlasmaForgeRenderState> {

    private static final int FULLBRIGHT = 0xF000F0;
    private static final int BLACK = 0xFF000000;
    private static final float PLUME_SIDE = 0.125F;
    private static final float PLUME_NEAR = 1.375F;
    private static final float PLUME_FAR = 1.625F;
    private static final float PLUME_NARROW = 0.01F;
    private static final float BEAM_IN = 0.4375F;

    private final WavefrontObj model;
    private final WavefrontObj torusModel;
    private final Surface shell;
    private final Surface torusShell;
    private final Surface coldPlasma;
    private final RenderType body;
    private final RenderType inactivePlasma;
    private final RenderType plasma;
    private final RenderType glow;
    private final RenderType beam;
    private final RenderType plume;

    public PlasmaForgeRenderer() {
        model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/fusion/plasma_forge.obj"));
        torusModel = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/fusion/torus.obj"));
        shell = Surface.cutout(Constants.MOD_ID + ":textures/models/fusion/plasma_forge.png");
        torusShell = Surface.cutout(Constants.MOD_ID + ":textures/models/fusion/torus.png");
        coldPlasma = Surface.solid(Constants.MOD_ID + ":textures/models/misc/white.png");
        body = CompatRenderTypes.entityCutout(Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma_forge.png"));
        inactivePlasma = CompatRenderTypes.entitySolid(Ids.of(Constants.MOD_ID, "textures/models/misc/white.png"));
        plasma = CompatRenderTypes.plasmaGlow(Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma.png"));
        glow = CompatRenderTypes.plasmaGlow(Ids.of(Constants.MOD_ID, "textures/models/fusion/plasma_glow.png"));
        beam = CompatRenderTypes.plasmaGlow(Fluids.STELLAR_FLUX.getTexture());
        plume = CompatRenderTypes.lightning();
    }

    /**
     * The shell, the neighbouring torus bolt when one is coupled, the cold plasma bulb, and the two
     * arms riding the ring. Everything that glows is built from the clock and stays drawn.
     */
    @Override
    protected void setup(Gem.Model<PlasmaForgeRenderState> declaration) {
        Gem.Part<PlasmaForgeRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> 90F + state.yaw);

        machine.child(torusModel.part("Bolts1"), torusShell)
                .visibleIf(state -> state.connected)
                .translate(-2F, 0F, 0F);
        machine.child(model.part("Body"), shell);
        machine.child(model.part("Plasma"), coldPlasma)
                .visibleIf(state -> !state.burning)
                .light(state -> FULLBRIGHT)
                .tint(state -> BLACK);

        Gem.Part<PlasmaForgeRenderState> ring = machine.joint()
                .angle(Gem.Axes.Y, state -> (float) state.ring);
        striker(ring);
        jet(ring);
    }

    private void striker(Gem.Part<PlasmaForgeRenderState> ring) {
        ring.child(model.part("SliderStriker"), shell);

        Gem.Part<PlasmaForgeRenderState> lower = ring.joint()
                .translate(-2.75F, 2.5F, 0F)
                .angle(Gem.Axes.Z, state -> (float) -state.striker[0])
                .translate(2.75F, -2.5F, 0F);
        lower.child(model.part("ArmLowerStriker"), shell);

        Gem.Part<PlasmaForgeRenderState> upper = lower.joint()
                .translate(-2.75F, 3.75F, 0F)
                .angle(Gem.Axes.Z, state -> (float) -state.striker[1])
                .translate(2.75F, -3.75F, 0F);
        upper.child(model.part("ArmUpperStriker"), shell);

        Gem.Part<PlasmaForgeRenderState> mount = upper.joint()
                .translate(-1.5F, 3.75F, 0F)
                .angle(Gem.Axes.Z, state -> (float) -state.striker[2])
                .translate(1.5F, -3.75F, 0F);
        mount.child(model.part("StrikerMount"), shell);

        hammer(mount, "Right", 0.5F, 1F, 4);
        hammer(mount, "Left", -0.5F, -1F, 5);
    }

    private void hammer(Gem.Part<PlasmaForgeRenderState> mount, String side, float z, float sign, int piston) {
        Gem.Part<PlasmaForgeRenderState> arm = mount.joint()
                .translate(0F, 3.375F, z)
                .angle(Gem.Axes.X, state -> sign * (float) state.striker[3])
                .translate(0F, -3.375F, -z);
        arm.child(model.part("Striker" + side), shell);
        arm.child(model.part("Piston" + side), shell)
                .slide(Gem.Axes.Y, state -> (float) -state.striker[piston]);
    }

    private void jet(Gem.Part<PlasmaForgeRenderState> ring) {
        ring.child(model.part("SliderJet"), shell);

        Gem.Part<PlasmaForgeRenderState> lower = ring.joint()
                .translate(2.75F, 2.5F, 0F)
                .angle(Gem.Axes.Z, state -> (float) state.jet[0])
                .translate(-2.75F, -2.5F, 0F);
        lower.child(model.part("ArmLowerJet"), shell);

        Gem.Part<PlasmaForgeRenderState> upper = lower.joint()
                .translate(2.75F, 3.75F, 0F)
                .angle(Gem.Axes.Z, state -> (float) state.jet[1])
                .translate(-2.75F, -3.75F, 0F);
        upper.child(model.part("ArmUpperJet"), shell);

        upper.joint()
                .translate(1.5F, 3.75F, 0F)
                .angle(Gem.Axes.Z, state -> (float) state.jet[2])
                .translate(-1.5F, -3.75F, 0F)
                .child(model.part("Jet"), shell);
    }

    @Override public PlasmaForgeRenderState createRenderState() { return new PlasmaForgeRenderState(); }

    @Override
    public void extract(TileEntityFusionPlasmaForge be, PlasmaForgeRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = switch (meta) {
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
            long time = GameTime.now() + be.timeOffset;
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
    protected void submitUndeclared(PlasmaForgeRenderState state, PoseStack ps, SubmitSink sink, CameraInfo camera) {
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(90F + state.yaw));

        renderPlasma(state, ps, sink);
        renderRecipeItem(state, ps, sink);
        if (state.showBeam) renderBeam(state, ps, sink);

        if (state.jetPlume) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float) state.ring));
            ps.translate(2.75F, 2.5F, 0F);
            ps.mulPose(Axis.ZP.rotationDegrees((float) state.jet[0]));
            ps.translate(-2.75F, -2.5F, 0F);
            ps.translate(2.75F, 3.75F, 0F);
            ps.mulPose(Axis.ZP.rotationDegrees((float) state.jet[1]));
            ps.translate(-2.75F, -3.75F, 0F);
            ps.translate(1.5F, 3.75F, 0F);
            ps.mulPose(Axis.ZP.rotationDegrees((float) state.jet[2]));
            ps.translate(-1.5F, -3.75F, 0F);
            renderJetPlume(state, ps, sink);
            ps.popPose();
        }
        ps.popPose();
    }

    private void renderPlasma(PlasmaForgeRenderState state, PoseStack ps, SubmitSink sink) {
        if (!state.burning) return;
        plasmaLayer(state, ps, sink, plasma, state.mainScroll, state.plasmaAlpha);
        plasmaLayer(state, ps, sink, glow, state.glowScroll, PlasmaForgeRenderMath.GLOW_COLOR_SCALE);
        plasmaLayer(state, ps, sink, glow, state.secondGlowScroll, PlasmaForgeRenderMath.GLOW_COLOR_SCALE);
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
