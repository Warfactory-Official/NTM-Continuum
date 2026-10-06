package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.entity.effect.EntityQuasar;
import com.hbm.entity.effect.EntityRagingVortex;
import com.hbm.entity.effect.EntityVortex;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Renders the legacy sphere, swirl/disc and axial jets of the black-hole fields. */
public final class BlackHoleRenderer extends GemEntityRenderer<EntityBlackHole, BlackHoleRenderer.State> {
    private static final Identifier SPHERE_TEXTURE =
            Ids.of(Constants.MOD_ID, "textures/models/explosion/blackhole.png");
    private static final Identifier SWIRL_TEXTURE = Ids.of(Constants.MOD_ID, "textures/entity/bhole.png");
    private static final Identifier DISC_TEXTURE = Ids.of(Constants.MOD_ID, "textures/entity/bholedisc.png");
    /** 1.7 {@code RenderQuasar.quasar}: {@code textures/entity/bholeD.png}. */
    private static final Identifier QUASAR_DISC_TEXTURE = Ids.of(Constants.MOD_ID, "textures/entity/bholed.png");
    private static final int RAGING_COLOR = 0xE8390D;

    private final com.hbm.client.model.WavefrontObj sphere;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
        sphere = com.hbm.client.model.WavefrontObj.load(
                Ids.of(Constants.MOD_ID, "models/sphere.obj"));
    }

    /** The event horizon itself; the accretion field around it is built from the clock every frame. */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen()
                .part(sphere.all(), Surface.cutout(SPHERE_TEXTURE.toString()))
                .light(state -> 0xF000F0)
                .stretch(state -> state.size);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityBlackHole entity, State state, float partialTicks) {
        state.entity = entity;
        state.partialTicks = partialTicks;
        state.entityId = entity.getId();
        state.size = entity.getSize();
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        EntityBlackHole entity = state.entity;
        if (entity == null) return;

        pose.pushPose();
        pose.scale(entity.getSize(), entity.getSize(), entity.getSize());
        if (entity instanceof EntityVortex) {
            renderSwirl(entity, state.partialTicks, pose, sink);
        } else {
            renderDisc(entity, state.partialTicks, pose, sink);
        }
        if (!(entity instanceof EntityVortex) || entity instanceof EntityRagingVortex) {
            renderJets(entity, pose, sink);
        }
        pose.popPose();
    }

    private static void renderSwirl(EntityBlackHole entity, float partial, PoseStack pose, SubmitSink sink) {
        pose.pushPose();
        rotateField(entity, partial, pose, -5F);
        int full = fieldColor(entity, 1F);
        int glow = argb(entity instanceof EntityRagingVortex ? 0.25F : 0.75F, 1F, 1F, 1F);
        int black = argb(1F, 0F, 0F, 0F);
        int clearField = fieldColor(entity, 0F);
        int clearWhite = argb(0F, 1F, 1F, 1F);

        sink.submitCustomGeometry(pose, CompatRenderTypes.entityTranslucent(SWIRL_TEXTURE),
                (last, buffer) -> {
                    ring(buffer, last, 0.9F, 3F, black, full);
                    ring(buffer, last, 3F, 6F, full, clearField);
                });
        sink.submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(SWIRL_TEXTURE),
                (last, buffer) -> {
                    ring(buffer, last, 0.9F, 3F, black, glow);
                    ring(buffer, last, 3F, 6F, glow, clearWhite);
                });
        pose.popPose();
    }

    private static void renderDisc(EntityBlackHole entity, float partial, PoseStack pose, SubmitSink sink) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        pose.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        // 1.7 RenderQuasar overrides discTex() and setColorFromIteration(); steps() stays 15.
        boolean quasar = entity instanceof EntityQuasar;
        Identifier discTexture = quasar ? QUASAR_DISC_TEXTURE : DISC_TEXTURE;
        for (int i = 0; i < 15; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partial % 360F)
                    * -(float) Math.pow(i + 1, 1.25D)));
            float inner = 3F - i * 0.175F;
            int innerColor = quasar ? quasarColor(i, 1F) : iterationColor(i, 1F);
            int outerColor = quasar ? quasarColor(i, 0F) : iterationColor(i, 0F);
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityTranslucent(discTexture),
                    (last, buffer) -> discRing(buffer, last, inner, inner * 2F, innerColor, outerColor));
            sink.submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(discTexture),
                    (last, buffer) -> discRing(buffer, last, inner, inner * 2F,
                            argb(0.75F, 1F, 1F, 1F), argb(0F, 1F, 1F, 1F)));
            pose.popPose();
        }
        pose.popPose();
    }

    private static void renderJets(EntityBlackHole entity, PoseStack pose, SubmitSink sink) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        pose.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        // 1.7 draws each jet as a GL_TRIANGLE_FAN with culling off; a fan is expanded into its triangles here.
        sink.submitCustomGeometry(pose, CompatRenderTypes.additiveTriangles(), (last, buffer) -> {
            int centre = argb(0.35F, 1F, 1F, 1F);
            int edge = argb(0F, 1F, 1F, 1F);
            for (int direction : new int[] {-1, 1}) {
                for (int i = 0; i < 12; i++) {
                    double a0 = i * Math.PI / 6D * -direction;
                    double a1 = (i + 1) * Math.PI / 6D * -direction;
                    Vertexes.color(buffer, last, 0F, 0F, 0F, centre);
                    Vertexes.color(buffer, last, (float) (Math.cos(a0) * 0.5D),
                            10F * direction, (float) (Math.sin(a0) * 0.5D), edge);
                    Vertexes.color(buffer, last, (float) (Math.cos(a1) * 0.5D),
                            10F * direction, (float) (Math.sin(a1) * 0.5D), edge);
                }
            }
        });
        pose.popPose();
    }

    private static void rotateField(EntityBlackHole entity, float partial, PoseStack pose, float speed) {
        pose.mulPose(Axis.XP.rotationDegrees(entity.getId() % 90 - 45));
        pose.mulPose(Axis.YP.rotationDegrees(entity.getId() % 360));
        pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partial) * speed));
    }

    private static void ring(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                             float inner, float outer, int innerColor, int outerColor) {
        for (int i = 0; i < 16; i++) {
            double a0 = i * Math.PI * 2D / 16D;
            double a1 = (i + 1) * Math.PI * 2D / 16D;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float innerU0 = 0.5F + c0 * 0.25F * inner / 3F;
            float innerV0 = 0.5F + s0 * 0.25F * inner / 3F;
            float outerU0 = 0.5F + c0 * 0.5F * outer / 6F;
            float outerV0 = 0.5F + s0 * 0.5F * outer / 6F;
            float innerU1 = 0.5F + c1 * 0.25F * inner / 3F;
            float innerV1 = 0.5F + s1 * 0.25F * inner / 3F;
            float outerU1 = 0.5F + c1 * 0.5F * outer / 6F;
            float outerV1 = 0.5F + s1 * 0.5F * outer / 6F;
            Vertexes.vertex(buffer, pose, c0 * inner, 0F, s0 * inner,
                    innerColor, innerU0, innerV0, 0, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c0 * outer, 0F, s0 * outer,
                    outerColor, outerU0, outerV0, 0, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c1 * outer, 0F, s1 * outer,
                    outerColor, outerU1, outerV1, 0, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c1 * inner, 0F, s1 * inner,
                    innerColor, innerU1, innerV1, 0, 0xF000F0, 0F, 1F, 0F);
        }
    }

    /** Disc UVs are fixed to the old .25/.5 radii; only the geometry radius shrinks per iteration. */
    private static void discRing(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                                 float inner, float outer, int innerColor, int outerColor) {
        for (int i = 0; i < 16; i++) {
            double a0 = i * Math.PI * 2D / 16D;
            double a1 = (i + 1) * Math.PI * 2D / 16D;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            Vertexes.vertex(buffer, pose, c0 * inner, 0F, s0 * inner, innerColor,
                    0.5F + c0 * 0.25F, 0.5F + s0 * 0.25F, 0, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c0 * outer, 0F, s0 * outer, outerColor,
                    0.5F + c0 * 0.5F, 0.5F + s0 * 0.5F, 0, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c1 * outer, 0F, s1 * outer, outerColor,
                    0.5F + c1 * 0.5F, 0.5F + s1 * 0.5F, 0, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c1 * inner, 0F, s1 * inner, innerColor,
                    0.5F + c1 * 0.25F, 0.5F + s1 * 0.25F, 0, 0xF000F0, 0F, 1F, 0F);
        }
    }

    private static int iterationColor(int iteration, float alpha) {
        if (iteration < 5) return argb(alpha, 1F, 0.125F + iteration * 0.1F, 0F);
        if (iteration == 5) return argb(alpha, 1F, 1F, 1F);
        int i = iteration - 6;
        return argb(alpha, 1F - i / 9F, 1F - i / 9F, i / 5F);
    }

    /** 1.7 {@code RenderQuasar.setColorFromIteration}: red fading to white, {@code (1, (i/15)^2, (i/15)^2)}. */
    private static int quasarColor(int iteration, float alpha) {
        float gb = (float) Math.pow(iteration / 15F, 2);
        return argb(alpha, 1F, gb, gb);
    }

    private static int fieldColor(EntityBlackHole entity, float alpha) {
        if (entity instanceof EntityRagingVortex) return (Math.round(alpha * 255F) << 24) | RAGING_COLOR;
        return argb(alpha, 0.2196F, 0.5961F, 0.702F);
    }

    private static int argb(float alpha, float red, float green, float blue) {
        return (Math.round(alpha * 255F) << 24) | (Math.round(red * 255F) << 16)
                | (Math.round(green * 255F) << 8) | Math.round(blue * 255F);
    }

    public static final class State extends CompatEntityRenderState {
        private EntityBlackHole entity;
        private float partialTicks;
        private int entityId;
        private float size = 1F;
    }
}
