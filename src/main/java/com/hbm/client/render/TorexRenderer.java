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

import java.util.Comparator;

/**
 * Renders {@link EntityNukeTorex}: the camera-facing cloudlet billboards (the mushroom cloud + its
 * shockwave/collar/condensation rings), the ground-flare glow, and the initial white flash. A port of
 * 1.12.2's {@code RenderTorex} to the 26.1 submit API.
 *
 * <ul>
 *   <li><b>Cloudlets</b> — one {@code submitCustomGeometry} batch on a custom translucent, no-depth-write
 *       PARTICLE-format {@link RenderType} (vanilla {@code WEATHER_NO_DEPTH_WRITE} pipeline). Distance-sorted
 *       once per client tick, coloured/lit by the old heat-and-life math (lightmap = colour brightness).</li>
 *   <li><b>Flare</b> — 3 additive gaussian billboards ({@code energySwirl} pipeline), fade baked into RGB.</li>
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

    private static final Comparator<Cloudlet> CLOUD_SORTER =
            (a, b) -> Double.compare(b.renderSortDistanceSq, a.renderSortDistanceSq);

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
        state.torex = entity;
        state.partialTicks = partialTicks;
        state.entityId = entity.getId();
    }

    @Override
    public void submit(TorexRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        EntityNukeTorex cloud = state.torex;
        if (cloud == null) return;

        float partialTicks = state.partialTicks;
        float scale = (float) cloud.getScale();
        float flashDuration = scale * flashBaseDuration;
        float flareDuration = scale * flareBaseDuration;

        // Draw order via explicit buckets — needed because BOTH the cloudlets (entityTranslucent) and
        // the flare (energySwirl, DepthStencilState.DEFAULT) WRITE DEPTH, and the ray burst originates
        // INSIDE the cloud core: drawn after either of them, its fragments fail the depth test and the
        // burst disappears (the "white png just sits there, no rays" playtest bug). Within one bucket,
        // custom geometry is stored in a HashMap keyed by RenderType (CustomFeatureRenderer.Storage),
        // so cross-RenderType submission order is NOT preserved — buckets are the only guarantee.
        //   order 0: flash rays (drawn first, nothing can cull them; the cloud blends over them)
        //   order 1: cloudlets (depth-write on — the far-water/clouds occlusion fix)
        //   order 2: flare (additive glow blended over the cloud — the old drew flare after cloudlets too)
        if (cloud.tickCount < flashDuration) {
            submitFlash(cloud, poseStack, collector, flashDuration, partialTicks);
        }
        sortCloudlets(cloud, camera);
        submitCloudlets(cloud, poseStack, collector.order(1), camera, partialTicks);
        if (cloud.tickCount < flareDuration + 1) {
            submitFlare(cloud, state.entityId, poseStack, collector.order(2), camera, partialTicks, flareDuration);
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

    private void sortCloudlets(EntityNukeTorex cloud, CameraInfo camera) {
        if (cloud.lastRenderSortTick == cloud.tickCount) return;
        Vec3 cam = camera.pos;
        for (Cloudlet cloudlet : cloud.cloudlets) {
            double dx = cam.x - cloudlet.posX;
            double dy = cam.y - cloudlet.posY;
            double dz = cam.z - cloudlet.posZ;
            cloudlet.renderSortDistanceSq = dx * dx + dy * dy + dz * dz;
        }
        cloud.cloudlets.sort(CLOUD_SORTER);
        cloud.lastRenderSortTick = cloud.tickCount;
    }

    private void submitCloudlets(EntityNukeTorex cloud, PoseStack poseStack, SubmitSink collector,
                                 CameraInfo camera, float partialTicks) {
        if (cloud.cloudlets.isEmpty()) return;

        Quaternionf q = camera.orientation;
        Vector3f rightBase = q.transform(new Vector3f(1F, 0F, 0F));
        Vector3f upBase = q.transform(new Vector3f(0F, 1F, 0F));
        float cloudAlphaBase = cloud.getAlpha();
        double ox = cloud.getX();
        double oy = cloud.getY();
        double oz = cloud.getZ();

        collector.submitCustomGeometry(poseStack, CLOUDLET_TYPE, (pose, buffer) -> {
            for (Cloudlet c : cloud.cloudlets) {
                float lifeFrac = (float) c.age / (float) c.cloudletLife;
                float alpha = (1F - lifeFrac) * cloudAlphaBase;
                if (c.type == EntityNukeTorex.TorexType.CONDENSATION) alpha *= 0.25F;
                alpha = Mth.clamp(alpha, 0.0001F, 1F);

                float cs = c.startingScale + lifeFrac * c.growingScale;
                float brightness = c.type == EntityNukeTorex.TorexType.CONDENSATION ? 0.9F : 0.75F * c.colorMod;
                double greying = c.type == EntityNukeTorex.TorexType.RING ? 0.05D : 0D;

                float r = clampColor(color(c, c.prevColorR, c.colorR, partialTicks, greying), brightness);
                float g = clampColor(color(c, c.prevColorG, c.colorG, partialTicks, greying), brightness);
                float b = clampColor(color(c, c.prevColorB, c.colorB, partialTicks, greying), brightness);
                // Faithful to the old renderer: vertex colour = the plain (r,g,b), and the brightness-derived
                // lightmap `br` rides UV2. The non-emissive pipeline then does colour × lightmapColor(br) —
                // the real (non-linear, brighter) lightmap curve, NOT a dim linear br/255 fold. Up-facing
                // normals get a per-face factor of 1.0 (0.808·2·0.6+0.4 clamps to 1), so no extra dimming.
                int br = lightmap(r, g, b);
                int packedLight = br | (br << 16);

                float px = (float) (c.prevPosX + (c.posX - c.prevPosX) * partialTicks - ox);
                float py = (float) (c.prevPosY + (c.posY - c.prevPosY) * partialTicks - oy);
                float pz = (float) (c.prevPosZ + (c.posZ - c.prevPosZ) * partialTicks - oz);

                float rx = rightBase.x * cs, ry = rightBase.y * cs, rz = rightBase.z * cs;
                float ux = upBase.x * cs, uy = upBase.y * cs, uz = upBase.z * cs;

                // Counter-clockwise from the camera (BL→BR→TR→TL, the DragonFireballRenderer order) so the
                // quad is a FRONT face: the PER_FACE_LIGHTING shader negates the light dots for back faces
                // (lightAccum 0.4 instead of 1.0), which rendered the whole cloud at 40% brightness when
                // these were emitted clockwise. Each corner keeps its old UV.
                cloudletVertex(buffer, pose, px - rx - ux, py - ry - uy, pz - rz - uz, 1F, 1F, r, g, b, alpha, packedLight);
                cloudletVertex(buffer, pose, px + rx - ux, py + ry - uy, pz + rz - uz, 0F, 1F, r, g, b, alpha, packedLight);
                cloudletVertex(buffer, pose, px + rx + ux, py + ry + uy, pz + rz + uz, 0F, 0F, r, g, b, alpha, packedLight);
                cloudletVertex(buffer, pose, px - rx + ux, py - ry + uy, pz - rz + uz, 1F, 0F, r, g, b, alpha, packedLight);
            }
        });
    }

    private static void cloudletVertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                                       float x, float y, float z, float u, float v, float r, float g, float b, float a, int packedLight) {
        Vertexes.vertex(buffer, pose, x, y, z, r, g, b, a, u, v,
                OverlayTexture.NO_OVERLAY, packedLight, 0F, 1F, 0F);
    }

    private static double color(Cloudlet c, double prev, double cur, float partialTicks, double greying) {
        if (c.type == EntityNukeTorex.TorexType.CONDENSATION) return 1D;
        return (prev + (cur - prev) * partialTicks) + greying;
    }

    private static float clampColor(double color, float brightness) {
        float channel = (float) color * brightness;
        if (channel < 0.15F) return 0.15F;
        return channel > 1F ? 1F : channel;
    }

    private static int lightmap(float r, float g, float b) {
        float avg = (r + g + b) / 3F;
        if (avg > 1F) avg = 1F;
        int br = (int) (avg * 240F);
        return br < 48 ? 48 : br;
    }

    // ---- Flare ----

    private void submitFlare(EntityNukeTorex cloud, int entityId, PoseStack poseStack, SubmitSink collector,
                             CameraInfo camera, float partialTicks, float flareDuration) {
        double age = Math.min(cloud.tickCount + partialTicks, flareDuration);
        float alpha = (float) Math.min(1, (flareDuration - age) / flareDuration);
        if (alpha <= 0F) return;

        Quaternionf q = camera.orientation;
        Vector3f rightBase = q.transform(new Vector3f(1F, 0F, 0F));
        Vector3f upBase = q.transform(new Vector3f(0F, 1F, 0F));
        float size = (float) (10 * cloud.rollerSize);
        double coreHeight = cloud.coreHeight;
        RandomSource rand = RandomSource.create(entityId);

        float[][] offsets = new float[3][3];
        for (int i = 0; i < 3; i++) {
            offsets[i][0] = (float) (rand.nextGaussian() * 0.5F * cloud.rollerSize);
            offsets[i][1] = (float) (rand.nextGaussian() * 0.5F * cloud.rollerSize) + (float) coreHeight;
            offsets[i][2] = (float) (rand.nextGaussian() * 0.5F * cloud.rollerSize);
        }

        collector.submitCustomGeometry(poseStack, CompatRenderTypes.energySwirl(FLARE_TEX, 0F, 0F), (pose, buffer) -> {
            float rx = rightBase.x * size, ry = rightBase.y * size, rz = rightBase.z * size;
            float ux = upBase.x * size, uy = upBase.y * size, uz = upBase.z * size;
            for (float[] o : offsets) {
                float px = o[0], py = o[1], pz = o[2];
                flareVertex(buffer, pose, px - rx - ux, py - ry - uy, pz - rz - uz, 1F, 1F, alpha);
                flareVertex(buffer, pose, px - rx + ux, py - ry + uy, pz - rz + uz, 1F, 0F, alpha);
                flareVertex(buffer, pose, px + rx + ux, py + ry + uy, pz + rz + uz, 0F, 0F, alpha);
                flareVertex(buffer, pose, px + rx - ux, py + ry - uy, pz + rz - uz, 0F, 1F, alpha);
            }
        });
    }

    private static void flareVertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                                    float x, float y, float z, float u, float v, float alpha) {
        // Additive pipeline ignores alpha in the blend, so the fade is baked into RGB.
        Vertexes.vertex(buffer, pose, x, y, z, alpha, alpha, alpha, 1F, u, v,
                OverlayTexture.NO_OVERLAY, 0x00F000F0, 0F, 1F, 0F);
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
