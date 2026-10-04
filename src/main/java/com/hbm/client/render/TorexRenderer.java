package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.Ids;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import com.hbm.Constants;
import com.hbm.client.NukeScreenEffects;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.effect.EntityNukeTorex.Cloudlet;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;


/**
 * Renders {@link EntityNukeTorex}: 1.7's {@code RenderTorex} on the 26.1 submit API.
 *
 * <ul>
 *   <li><b>Cloudlets</b> — shared interpolated/sorted render rows feed either Core's optional persistent
 *       billboard instances or one fallback {@code submitCustomGeometry} batch on a depth-writing
 *       {@link RenderType}: 1.7's colour times brightness, fullbright.</li>
 *   <li><b>Flare</b> — for the first 100 ticks, 3 source-alpha additive gaussian billboards with no depth
 *       write, matching 1.7's flashWrapper.</li>
 * </ul>
 * 1.12.2's 300-spike ray burst is not 1.7 and is gone (History section 19ej-1d).
 *
 * It also latches the HUD {@link NukeScreenEffects} flash/shake timestamps and the local player's
 * hurt-camera-tilt when the blast is close, exactly like the original.
 */
public class TorexRenderer
        extends dev.golem.core.render.gem.GemEntityRenderer<EntityNukeTorex, TorexRenderState> {

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

    public TorexRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public TorexRenderState createRenderState() {
        return new TorexRenderState();
    }

    /**
     * Nothing here is a resident mesh — the cloudlets and flare are camera-facing billboards and the
     * flash is three hundred rays rebuilt each frame — so the declaration holds no parts. What it does
     * carry is the one property of the model rather than of a frame: the cloud reaches far outside its
     * own bounding box and must never be culled with it.
     */
    @Override
    protected void setup(Gem.Model<TorexRenderState> declaration) {
        declaration.offScreen();
    }

    @Override
    public void extract(EntityNukeTorex entity, TorexRenderState state, float partialTicks) {
        entity.prepareClientAnimation();
        state.torex = entity;
        state.partialTicks = partialTicks;
    }

    @Override
    protected void submitUndeclared(TorexRenderState state, PoseStack poseStack, SubmitSink collector,
                                    CameraInfo camera) {
        EntityNukeTorex cloud = state.torex;
        if (cloud == null || !cloud.isClientAnimationReady()) return;

        float partialTicks = state.partialTicks;
        TorexBenchmarkClient.frame(cloud);

        // Draw order via explicit buckets: the no-depth additive flare must follow the depth-writing
        // cloudlets (1.7 drew the flare after the cloudlets). Within one bucket, custom geometry is stored in
        // a HashMap keyed by RenderType (CustomFeatureRenderer.Storage), so cross-RenderType submission
        // order is NOT preserved — buckets are the only guarantee.
        //   order 1: cloudlets (depth-write on — the far-water/clouds occlusion fix)
        //   order 2: flare
        boolean instanced = com.hbm.client.render.flywheel.FlywheelCompat.isTorexBillboardsActive(cloud);
        if (!instanced) {
            TorexCloudletRenderData.sort(cloud, camera.pos);
            submitCloudlets(cloud, poseStack, collector.order(1), camera, partialTicks);
            if (cloud.tickCount < 101) {
                submitFlare(cloud, poseStack, collector.order(2), camera, partialTicks);
            }
        }

        // 1.7: latch the HUD flash for the first 10 ticks, and the shake once with a 15-tick hurt tilt.
        long now = System.currentTimeMillis();
        if (cloud.tickCount < 10 && now - NukeScreenEffects.flashTimestamp > 1_000) {
            NukeScreenEffects.flashTimestamp = now;
        }
        if (cloud.didPlaySound && !cloud.didShake && now - NukeScreenEffects.shakeTimestamp > 1_000) {
            NukeScreenEffects.shakeTimestamp = now;
            cloud.didShake = true;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.hurtTime = 15;
                player.hurtDuration = 15;
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

}
