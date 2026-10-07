package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.entity.effect.EntityQuasar;
import com.hbm.entity.effect.EntityRagingVortex;
import com.hbm.entity.effect.EntityVortex;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SceneWarp;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Renders the legacy sphere, swirl/disc and axial jets of the black-hole fields. */
public final class BlackHoleRenderer extends GemEntityRenderer<EntityBlackHole, BlackHoleRenderer.State> {
    private static final Identifier SPHERE_TEXTURE =
            Ids.of(Constants.MOD_ID, "textures/models/explosion/blackhole.png");
    private static final Identifier SWIRL_TEXTURE = Ids.of(Constants.MOD_ID, "textures/entity/bhole.png");
    private static final Identifier DISC_TEXTURE = Ids.of(Constants.MOD_ID, "textures/entity/bholedisc.png");
    /** 1.7 {@code RenderQuasar.quasar}: {@code textures/entity/bholeD.png}. */
    private static final Identifier QUASAR_DISC_TEXTURE = Ids.of(Constants.MOD_ID, "textures/entity/bholed.png");
    private static final int RAGING_COLOR = 0xE8390D;
    /** Assumed distance of the lensed background behind the hole, in blocks (no depth is sampled). */
    private static final float LENS_BACKGROUND = 32F;
    /** How far out the world is bent, as a fraction of 4 Einstein radii plus 2 shadow radii (user: halved). */
    private static final float LENS_RANGE = 0.5F;

    private final com.hbm.client.model.WavefrontObj sphere;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
        sphere = com.hbm.client.model.WavefrontObj.load(
                Ids.of(Constants.MOD_ID, "models/sphere.obj"));
    }

    /**
     * The event horizon itself; the accretion field around it is built from the clock every frame. With lensing
     * the lens shader draws the horizon as its shadow instead: a sphere in the copied scene would be lensed
     * into a black Einstein ring, hiding twice what the hole hides once.
     */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen()
                .part(sphere.all(), Surface.cutout(SPHERE_TEXTURE.toString()))
                .visibleIf(state -> !state.lensed)
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
        state.lensed = SceneWarp.isConfigured();
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        EntityBlackHole entity = state.entity;
        if (entity == null) return;

        Lens lens = Lens.of(entity, pose);
        if (lens != null) {
            // the shader ray-traces the disc or swirl and the jets
            lens.submitField(entity, pose, state.partialTicks);
            return;
        }
        pose.pushPose();
        pose.scale(entity.getSize(), entity.getSize(), entity.getSize());
        // 1.7 RenderBlackHole.doRender: Vortex swirl; RagingVortex (a sibling, not a Vortex) swirl + jets;
        // everything else disc + jets.
        if (entity instanceof EntityVortex || entity instanceof EntityRagingVortex) {
            renderSwirl(entity, state.partialTicks, pose, sink);
        } else {
            renderDisc(entity, state.partialTicks, pose, sink);
        }
        if (!(entity instanceof EntityVortex)) {
            renderJets(entity, pose, sink);
        }
        pose.popPose();
    }

    /**
     * Port-only gravitational lensing (1.7 has none). One camera-parallel impostor quad in the hole's plane, whose
     * shader ({@code shaders/core/black_hole_lens.fsh}) follows each pixel's light path through the Schwarzschild
     * geometry: a path that falls in is the shadow (radius {@code size}, the 1.7 sphere's, which is hidden); each
     * crossing of the field's plane shows the 1.7 disc or swirl at that point, so the primary image, the arcs over
     * and under the shadow and the thin higher-order rings come from the paths themselves; a path that escapes
     * shows the copied scene where it lands, with the background assumed {@link #LENS_BACKGROUND} blocks behind.
     * The jets are traced too, so the shadow and the disc hide what lies behind them (as geometry drawn after the
     * impostor they were tested only against the world's depth and drew over both).
     */
    private static final class Lens {
        private final Matrix4f viewToBase;
        /** Unit vector from the camera to the hole, in view (render) space. */
        private final Vector3f axis;
        private final float distance;
        /** The shadow radius b_c in blocks. */
        private final float shadow;

        private Lens(Matrix4f baseToView, Vector3f centre, float size) {
            this.viewToBase = new Matrix4f(baseToView).invert();
            this.distance = centre.length();
            this.axis = new Vector3f(centre).div(distance);
            this.shadow = size;
        }

        private static Lens of(EntityBlackHole entity, PoseStack pose) {
            if (!WorldEffects.isCollecting() || !SceneWarp.isConfigured()) return null;
            float size = entity.getSize();
            if (size <= 0F) return null;
            Matrix4f baseToView = new Matrix4f(pose.last().pose());
            Vector3f centre = new Vector3f().mulPosition(baseToView);
            if (centre.length() <= 0.05F) return null;
            return new Lens(baseToView, centre, size);
        }

        /**
         * The impostor. Its parameters travel in the vertex (see {@code black_hole_lens.vsh}): the corner's offset
         * in blocks along the view's right/up axes (the model-view rows the shader also reads, so both agree on
         * every node), the field's 1.7 frame, type, size, the world-bending range and the clock. The offset is
         * exact floats: the shader rebuilds the hole's centre from it per vertex, and anything quantized in that
         * sum (the fixed-point size) put the quad's two triangles' centres apart, a seam along its diagonal.
         */
        private void submitField(EntityBlackHole entity, PoseStack pose, float partial) {
            Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
            Vector3f right = new Vector3f(modelView.m00(), modelView.m10(), modelView.m20()).normalize();
            Vector3f up = new Vector3f(modelView.m01(), modelView.m11(), modelView.m21()).normalize();
            // the world's Einstein radius (E = (D_LS / D_S) * D_L / b_c) and the field's projected reach
            float strength = LENS_BACKGROUND / (distance + LENS_BACKGROUND) * distance / shadow;
            // the world's bending fades out by LENS_RANGE (half of 4 Einstein radii plus 2 shadow radii, by request)
            float worldEdge = LENS_RANGE * (4F * (float) Math.sqrt(0.7698F * strength) + 2F);
            // the jets reach 10 sizes from the centre (the disc 6)
            float reach = 10.3F * shadow;
            float fieldEdge = 10.3F * distance / Math.max(distance - reach, 0.05F * distance);
            float edge = Math.min(Math.max(Math.max(worldEdge, fieldEdge), 7F), 200F);

            // the 1.7 field frame: RenderBlackHole rotates X by id % 90 - 45, then Y by id % 360
            Matrix4f frame = new Matrix4f(pose.last().pose())
                    .rotate(Axis.XP.rotationDegrees(entity.getId() % 90 - 45))
                    .rotate(Axis.YP.rotationDegrees(entity.getId() % 360));
            Vector3f normal = frame.transformDirection(new Vector3f(0F, 1F, 0F)).normalize();
            Vector3f tangent = frame.transformDirection(new Vector3f(1F, 0F, 0F)).normalize();
            int type = entity instanceof EntityRagingVortex ? 3 : entity instanceof EntityVortex ? 2
                    : entity instanceof EntityQuasar ? 1 : 0;
            Identifier texture = type >= 2 ? SWIRL_TEXTURE : type == 1 ? QUASAR_DISC_TEXTURE : DISC_TEXTURE;
            int colour = (type << 24) | (unit(tangent.x()) << 16) | (unit(tangent.y()) << 8) | unit(tangent.z());
            int size = Math.min(Math.round(shadow * 1024F), 65535);
            int range = Math.round(Math.min(worldEdge, edge) * 256F);
            long clock = Math.round((entity.tickCount + partial) * 64D) & 0xFFFFFFFFL;
            int clockLow = (int) (clock & 0xFFFF), clockHigh = (int) (clock >>> 16);

            Vector3f centre = new Vector3f(axis).mul(distance);
            float[][] signs = {{-1F, -1F}, {-1F, 1F}, {1F, 1F}, {1F, -1F}};
            Vector3f[] corners = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                corners[i] = new Vector3f(centre)
                        .add(new Vector3f(right).mul(signs[i][0] * edge * shadow))
                        .add(new Vector3f(up).mul(signs[i][1] * edge * shadow))
                        .mulPosition(viewToBase);
            }
            WorldEffects.submit(pose, SceneWarp.renderType(texture), 0, (last, buffer) -> {
                for (int i = 0; i < 4; i++) {
                    SceneWarp.vertex(buffer, last, corners[i].x(), corners[i].y(), corners[i].z(), colour,
                            signs[i][0] * edge * shadow, signs[i][1] * edge * shadow, size, range, clockLow, clockHigh,
                            normal.x(), normal.y(), normal.z());
                }
            });
        }

        private static int unit(float component) {
            return Math.round((Math.max(-1F, Math.min(1F, component)) * 0.5F + 0.5F) * 255F);
        }
    }

    private static void renderSwirl(EntityBlackHole entity, float partial, PoseStack pose, SubmitSink sink) {
        pose.pushPose();
        rotateField(entity, partial, pose, -5F);
        int full = fieldColor(entity, 1F);
        int glow = argb(entity instanceof EntityRagingVortex ? 0.25F : 0.75F, 1F, 1F, 1F);
        int black = argb(1F, 0F, 0F, 0F);
        int clearField = fieldColor(entity, 0F);
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityTranslucent(SWIRL_TEXTURE), (last, buffer) -> {
            ring(buffer, last, 0.9F, 3F, black, full);
            ring(buffer, last, 3F, 6F, full, clearField);
        });
        // 1.7 renderSwirl's additive pass ends the outer ring in setColorNone (the field colour at alpha 0)
        sink.submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(SWIRL_TEXTURE), (last, buffer) -> {
            ring(buffer, last, 0.9F, 3F, black, glow);
            ring(buffer, last, 3F, 6F, glow, clearField);
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
        int glow = argb(0.75F, 1F, 1F, 1F);
        for (int i = 0; i < 15; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partial % 360F)
                    * -(float) Math.pow(i + 1, 1.25D)));
            float inner = 3F - i * 0.175F;
            int innerColor = quasar ? quasarColor(i, 1F) : iterationColor(i, 1F);
            int outerColor = quasar ? quasarColor(i, 0F) : iterationColor(i, 0F);
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityTranslucent(discTexture),
                    (last, buffer) -> discRing(buffer, last, inner, inner * 2F, innerColor, outerColor));
            // 1.7's additive pass ends in setColorFromIteration(k, 0F), the ring colour at alpha 0
            sink.submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(discTexture),
                    (last, buffer) -> discRing(buffer, last, inner, inner * 2F, glow, outerColor));
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
            // swirl UVs: radius r maps to r / 12 around the centre (1.7's 0.25 * r / 3 and 0.5 * r / 6)
            Vertexes.vertex(buffer, pose, c0 * inner, 0F, s0 * inner, innerColor,
                    0.5F + c0 * inner / 12F, 0.5F + s0 * inner / 12F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c0 * outer, 0F, s0 * outer, outerColor,
                    0.5F + c0 * outer / 12F, 0.5F + s0 * outer / 12F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c1 * outer, 0F, s1 * outer, outerColor,
                    0.5F + c1 * outer / 12F, 0.5F + s1 * outer / 12F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c1 * inner, 0F, s1 * inner, innerColor,
                    0.5F + c1 * inner / 12F, 0.5F + s1 * inner / 12F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
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
                    0.5F + c0 * 0.25F, 0.5F + s0 * 0.25F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c0 * outer, 0F, s0 * outer, outerColor,
                    0.5F + c0 * 0.5F, 0.5F + s0 * 0.5F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c1 * outer, 0F, s1 * outer, outerColor,
                    0.5F + c1 * 0.5F, 0.5F + s1 * 0.5F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, c1 * inner, 0F, s1 * inner, innerColor,
                    0.5F + c1 * 0.25F, 0.5F + s1 * 0.25F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 1F, 0F);
        }
    }

    private static int iterationColor(int iteration, float alpha) {
        if (iteration < 5) return argb(alpha, 1F, 0.125F + iteration * 0.1F, 0F);
        if (iteration == 5) return argb(alpha, 1F, 1F, 0F);
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

    /** Clamped like 1.7's {@code setColorRGBA_F}: disc iterations 12-14 ask for blue up to 1.6. */
    private static int argb(float alpha, float red, float green, float blue) {
        return (channel(alpha) << 24) | (channel(red) << 16) | (channel(green) << 8) | channel(blue);
    }

    private static int channel(float value) {
        return Math.round(Math.max(0F, Math.min(1F, value)) * 255F);
    }

    public static final class State extends CompatEntityRenderState {
        private EntityBlackHole entity;
        private float partialTicks;
        private int entityId;
        private float size = 1F;
        private boolean lensed;
    }
}
