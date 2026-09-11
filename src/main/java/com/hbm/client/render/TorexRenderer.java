package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.Ids;
import dev.golem.core.render.Vertexes;
import com.hbm.Constants;
import com.hbm.client.NukeScreenEffects;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.effect.EntityNukeTorex.Cloudlet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;


/**
 * Renders {@link EntityNukeTorex}: the camera-facing cloudlet billboards (the mushroom cloud + its
 * shockwave/collar/condensation rings), the ground-flare glow, and the initial white flash. A port of
 * 1.12.2's {@code RenderTorex} to the 26.1 submit API.
 *
 * <ul>
 *   <li><b>Cloudlets</b> — shared interpolated/sorted render rows feed either Core's optional persistent
 *       billboard instances or one fallback {@code submitCustomGeometry} batch on a depth-writing
 *       {@link RenderType}. They retain the old heat/life colour and brightness-derived lightmap math.</li>
 *   <li><b>Flare</b> — 3 source-alpha additive gaussian billboards with no depth write, matching old GL state.</li>
 *   <li><b>Flash</b> — the 300-spike additive burst ({@code dragonRays} pipeline, POSITION_COLOR triangles —
 *       the 1.12.2 code was structurally the vanilla dragon-death rays), Euler-damped intensity.</li>
 * </ul>
 *
 * It also latches the HUD {@link NukeScreenEffects} flash/shake timestamps and the local player's
 * hurt-camera-tilt when the blast is close, exactly like the original.
 */
public class TorexRenderer extends dev.golem.core.render.CompatEntityRenderer<EntityNukeTorex, TorexRenderState> {

    private static final Identifier CLOUDLET_TEX = Ids.of(Constants.MOD_ID, "textures/particle/particle_base.png");
    private static final Identifier FLARE_TEX = Ids.of(Constants.MOD_ID, "textures/particle/flare.png");

    /**
     * The cloudlet layer. {@code RenderType.create} is package-private (common can't build a custom type on
     * the WEATHER pipeline), so we use the closest public factory: {@code entityTranslucent} — translucent
     * blend, no cull, and crucially <b>depth-test on / depth-write ON</b>.
     *
     * <p><b>Why depth-write ON (a deliberate divergence from the old {@code depthMask(false)}):</b> the old
     * 1.12.2 renderer drew the whole cloud <em>last</em> (in {@code RenderWorldLastEvent}), after water and
     * clouds, so ordering was never an issue. 26.1 flushes entity translucent features <em>before</em>
     * translucent terrain (water) and before the separate clouds/weather passes — and with depth‑write off
     * the cloudlets leave no depth, so far‑away water and clouds (which both depth‑test the main buffer)
     * paint straight over them. Writing depth makes the cloud occlude that farther geometry. The pipeline's
     * {@code ALPHA_CUTOUT 0.1} discards the wispiest fragments before they write depth, so the soft edges
     * survive. (An entity‑renderer can't reorder itself after the water/cloud passes; only a per‑loader
     * late world‑render hook could, which 26.1's FrameGraph makes unreliable — hence this approach.)
     *
     * <p>Non‑emissive is also what gets the brightness right: the shader multiplies the vertex colour by the
     * real lightmap texture, so the vertices carry the plain colour + the old brightness‑derived lightmap
     * {@code br} on UV2 (see {@link #submitCloudlets}) — exactly the old renderer's {@code color × lightmap(br)}.
     * (An emissive type skips the lightmap, and folding {@code br/255} into RGB by hand is a too‑dim linear
     * approximation of the lightmap curve — that was the "cloud too dark" bug.)
     */
    private static final RenderType CLOUDLET_TYPE = CompatRenderTypes.entityTranslucent(CLOUDLET_TEX);

    public static final int flashBaseDuration = 30;
    public static final int flareBaseDuration = 100;

    public TorexRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public TorexRenderState createRenderState() {
        return new TorexRenderState();
    }

    /** Always render — the cloud has no meaningful bounding box and must be visible at any distance. */
    @Override
    public boolean culled(EntityNukeTorex entity) {
        return false;
    }

    @Override
    public void extract(EntityNukeTorex entity, TorexRenderState state, float partialTicks) {
        entity.prepareClientAnimation();
        state.torex = entity;
        state.partialTicks = partialTicks;
    }

    @Override
    public void submit(TorexRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        EntityNukeTorex cloud = state.torex;
        if (cloud == null || !cloud.isClientAnimationReady()) return;

        float partialTicks = state.partialTicks;
        float scale = (float) cloud.getScale();
        float flashDuration = scale * flashBaseDuration;
        float flareDuration = scale * flareBaseDuration;
        TorexBenchmarkClient.frame(cloud);

        // Draw order via explicit buckets — the depth-writing cloudlets must follow the ray burst and the
        // no-depth additive flare must follow the cloudlets. Within one bucket,
        // custom geometry is stored in a HashMap keyed by RenderType (CustomFeatureRenderer.Storage),
        // so cross-RenderType submission order is NOT preserved — buckets are the only guarantee.
        //   order 0: flash rays (drawn first, nothing can cull them; the cloud blends over them)
        //   order 1: cloudlets (depth-write on — the far-water/clouds occlusion fix)
        //   order 2: flare (no-depth additive glow over the cloud — the old drew flare after cloudlets too)
        if (cloud.tickCount < flashDuration) {
            submitFlash(cloud, poseStack, collector, flashDuration, partialTicks);
        }
        boolean instanced = com.hbm.client.render.flywheel.FlywheelCompat.isTorexBillboardsActive(cloud);
        if (!instanced) {
            TorexCloudletRenderData.sort(cloud, camera.pos);
            submitCloudlets(cloud, poseStack, collector.order(1), camera, partialTicks);
            if (cloud.tickCount < flareDuration + 1) {
                submitFlare(cloud, poseStack, collector.order(2), camera, partialTicks);
            }
        }

        // Latch the HUD flash while the cloud is very young, and the shake once (with the hurt tilt).
        long now = System.currentTimeMillis();
        if (cloud.tickCount < (flashDuration / 10) && now - NukeScreenEffects.flashTimestamp > 1_000) {
            NukeScreenEffects.flashTimestamp = now;
        }
        if (cloud.didPlaySound && !cloud.didShake && now - NukeScreenEffects.shakeTimestamp > 1_000) {
            NukeScreenEffects.shakeTimestamp = now;
            cloud.didShake = true;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                int dist = (int) player.distanceTo(cloud);
                int base = 100 - dist;
                player.hurtTime = base > 0 ? (int) (base * 1.5F) : 0;
                player.hurtDuration = base > 0 ? base : 0;
            }
        }
    }

    // ---- Cloudlets ----

    private void submitCloudlets(EntityNukeTorex cloud, PoseStack poseStack, SubmitSink collector,
                                 CameraInfo camera, float partialTicks) {
        if (cloud.cloudlets.isEmpty()) return;

        Quaternionf q = camera.orientation;
        Vector3f rightBase = q.transform(new Vector3f(1F, 0F, 0F));
        Vector3f upBase = q.transform(new Vector3f(0F, 1F, 0F));
        double ox = cloud.getX();
        double oy = cloud.getY();
        double oz = cloud.getZ();

        collector.submitCustomGeometry(poseStack, CLOUDLET_TYPE, (pose, buffer) -> {
            long benchmarkStart = TorexBenchmarkClient.fallbackStart(cloud);
            TorexCloudletRenderData.write(cloud, partialTicks,
                    (x, y, z, size, r, g, b, alpha, packedLight) -> {
                float px = (float) (x - ox), py = (float) (y - oy), pz = (float) (z - oz);
                float rx = rightBase.x * size, ry = rightBase.y * size, rz = rightBase.z * size;
                float ux = upBase.x * size, uy = upBase.y * size, uz = upBase.z * size;
                // Counter-clockwise from the camera (BL→BR→TR→TL, the DragonFireballRenderer order) so the
                // quad is a FRONT face: the PER_FACE_LIGHTING shader negates the light dots for back faces
                // (lightAccum 0.4 instead of 1.0), which rendered the whole cloud at 40% brightness when
                // these were emitted clockwise. Each corner keeps its old UV.
                cloudletVertex(buffer, pose, px - rx - ux, py - ry - uy, pz - rz - uz, 1F, 1F, r, g, b, alpha, packedLight);
                cloudletVertex(buffer, pose, px + rx - ux, py + ry - uy, pz + rz - uz, 0F, 1F, r, g, b, alpha, packedLight);
                cloudletVertex(buffer, pose, px + rx + ux, py + ry + uy, pz + rz + uz, 0F, 0F, r, g, b, alpha, packedLight);
                cloudletVertex(buffer, pose, px - rx + ux, py - ry + uy, pz - rz + uz, 1F, 0F, r, g, b, alpha, packedLight);
            });
            TorexBenchmarkClient.fallbackEnd(benchmarkStart, cloud.cloudlets.size());
        });
    }

    private static void cloudletVertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                                       float x, float y, float z, float u, float v, float r, float g, float b, float a, int packedLight) {
        Vertexes.vertex(buffer, pose, x, y, z, r, g, b, a, u, v,
                OverlayTexture.NO_OVERLAY, packedLight, 0F, 1F, 0F);
    }

    // ---- Flare ----

    private void submitFlare(EntityNukeTorex cloud, PoseStack poseStack, SubmitSink collector,
                             CameraInfo camera, float partialTicks) {
        Quaternionf q = camera.orientation;
        Vector3f rightBase = q.transform(new Vector3f(1F, 0F, 0F));
        Vector3f upBase = q.transform(new Vector3f(0F, 1F, 0F));
        double ox = cloud.getX(), oy = cloud.getY(), oz = cloud.getZ();

        SubmitSink.CustomGeometry geometry = (pose, buffer) -> {
            TorexCloudletRenderData.writeFlare(cloud, partialTicks,
                    (x, y, z, size, r, g, b, alpha, packedLight) -> {
                float px = (float) (x - ox), py = (float) (y - oy), pz = (float) (z - oz);
                float rx = rightBase.x * size, ry = rightBase.y * size, rz = rightBase.z * size;
                float ux = upBase.x * size, uy = upBase.y * size, uz = upBase.z * size;
                var vertex = CompatRenderTypes.torexFlareVertex(r, g, b, alpha, packedLight);
                flareVertex(buffer, pose, px - rx - ux, py - ry - uy, pz - rz - uz,
                        1F, 1F, vertex.red(), vertex.green(), vertex.blue(), vertex.alpha(), vertex.packedLight());
                flareVertex(buffer, pose, px - rx + ux, py - ry + uy, pz - rz + uz,
                        1F, 0F, vertex.red(), vertex.green(), vertex.blue(), vertex.alpha(), vertex.packedLight());
                flareVertex(buffer, pose, px + rx + ux, py + ry + uy, pz + rz + uz,
                        0F, 0F, vertex.red(), vertex.green(), vertex.blue(), vertex.alpha(), vertex.packedLight());
                flareVertex(buffer, pose, px + rx - ux, py + ry - uy, pz + rz - uz,
                        0F, 1F, vertex.red(), vertex.green(), vertex.blue(), vertex.alpha(), vertex.packedLight());
            });
        };
        RenderType type = CompatRenderTypes.torexFlare(FLARE_TEX);
        collector.submitCustomGeometry(poseStack, type, geometry);
    }

    private static void flareVertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                                     float x, float y, float z, float u, float v,
                                     float r, float g, float b, float alpha, int packedLight) {
        // Core preserves old white/alpha/lightmap rows on 26.1 and adapts them for legacy energySwirl.
        Vertexes.vertex(buffer, pose, x, y, z, r, g, b, alpha, u, v,
                OverlayTexture.NO_OVERLAY, packedLight, 0F, 1F, 0F);
    }

    // ---- Flash ----

    private void submitFlash(EntityNukeTorex cloud, PoseStack poseStack, SubmitSink collector,
                             float flashDuration, float partialTicks) {
        double intensity = (cloud.tickCount + partialTicks) / flashDuration;
        // Euler damping: fast rise, smooth fade-out.
        intensity = intensity * Math.pow(Math.E, -intensity) * 2.717391304D;
        float flashSize = 50F * flashDuration / flashBaseDuration;
        float inverse = (float) (1.0D - intensity);
        double coreHeight = cloud.coreHeight;
        final double fIntensity = intensity;

        collector.submitCustomGeometry(poseStack, CompatRenderTypes.dragonRays(), (pose, buffer) -> {
            dev.golem.core.render.Poses.scale(pose, 0.2F, 0.2F, 0.2F);
            dev.golem.core.render.Poses.translate(pose, 0F, (float) (coreHeight * 4), 0F);

            RandomSource random = RandomSource.create(432L);
            for (int i = 0; i < 300; i++) {
                // Accumulating random walk (the 1.12.2 GL rotations were never reset inside the loop).
                dev.golem.core.render.Poses.rotate(pose, Axis.XP.rotationDegrees(random.nextFloat() * 360F));
                dev.golem.core.render.Poses.rotate(pose, Axis.YP.rotationDegrees(random.nextFloat() * 360F));
                dev.golem.core.render.Poses.rotate(pose, Axis.ZP.rotationDegrees(random.nextFloat() * 360F));
                dev.golem.core.render.Poses.rotate(pose, Axis.XP.rotationDegrees(random.nextFloat() * 360F));
                dev.golem.core.render.Poses.rotate(pose, Axis.YP.rotationDegrees(random.nextFloat() * 360F));

                float vert1 = (random.nextFloat() * 20F + 5F + 10F) * (float) (fIntensity * flashSize);
                float vert2 = (random.nextFloat() * 2F + 1F + 2F) * (float) (fIntensity * flashSize);

                // Old triangle-fan of a 3-sided spike: centre at origin, 3 rim points at height vert1.
                float ax = -0.866F * vert2, az = -0.5F * vert2;
                float bx = 0.866F * vert2, bz = -0.5F * vert2;
                float cz = 1.0F * vert2;
                emitFlashTri(buffer, pose, inverse, ax, vert1, az, bx, vert1, bz);
                emitFlashTri(buffer, pose, inverse, bx, vert1, bz, 0F, vert1, cz);
                emitFlashTri(buffer, pose, inverse, 0F, vert1, cz, ax, vert1, az);
            }
        });
    }

    private static void emitFlashTri(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                                     float centerAlpha, float x1, float y1, float z1, float x2, float y2, float z2) {
        Vertexes.color(buffer, pose, 0F, 0F, 0F, 1F, 1F, 1F, centerAlpha);
        Vertexes.color(buffer, pose, x1, y1, z1, 1F, 1F, 1F, 0F);
        Vertexes.color(buffer, pose, x2, y2, z2, 1F, 1F, 1F, 0F);
    }
}
