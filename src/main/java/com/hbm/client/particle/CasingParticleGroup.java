package com.hbm.client.particle;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.client.Colors;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Source {@code CasingParticleGroup}, submitted through Core's world-effects seam. */
public final class CasingParticleGroup {
    private static final Identifier MODEL_ID = Ids.of("hbm", "models/effect/casings.obj");
    private static final Identifier CASING_TEXTURE = Ids.of("hbm", "textures/particle/casings.png");
    private static final Identifier WHITE_TEXTURE = Ids.of("hbm", "textures/models/misc/white.png");
    private static WavefrontObj model;

    private final List<ParticleSpentCasing> particles = new ArrayList<>();

    public void add(ParticleSpentCasing particle) { particles.add(particle); }
    public void tick(ClientLevel level) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            ParticleSpentCasing particle = particles.get(i);
            particle.tick(level);
            if (particle.expired()) particles.remove(i);
        }
    }

    public void clear() { particles.clear(); }
    public void reload() { clear(); model = null; }

    public void submit(ClientLevel level, CameraInfo camera) {
        if (particles.isEmpty()) return;
        float partial = Frames.partialTick();
        float playerYaw = net.minecraft.client.Minecraft.getInstance().player == null ? 0F
                : net.minecraft.client.Minecraft.getInstance().player.getViewYRot(partial);
        for (ParticleSpentCasing casing : List.copyOf(particles)) {
            double px = casing.interpX(partial) - camera.pos.x;
            double py = casing.interpY(partial) - camera.pos.y;
            double pz = casing.interpZ(partial) - camera.pos.z;
            int light = casing.light(level, partial);
            PoseStack pose = new PoseStack();
            pose.translate(px, py - casing.heightQuarter() + casing.scaleLift(), pz);
            // Unlike the vanilla ParticleGroup used by ntm-next, WorldEffects replays the captured
            // camera rotation around this local pose. Cancel that rotation before applying the
            // casing's authored world yaw/pitch, otherwise the casing turns with the screen.
            pose.mulPose(new org.joml.Quaternionf(camera.orientation).conjugate());
            pose.scale(ParticleSpentCasing.SCALE, ParticleSpentCasing.SCALE, ParticleSpentCasing.SCALE);
            pose.mulPose(Axis.YP.rotationDegrees(180F - casing.interpYaw(partial)));
            pose.mulPose(Axis.XP.rotationDegrees(-casing.interpPitch(partial)));
            pose.scale(casing.config.getScaleX(), casing.config.getScaleY(), casing.config.getScaleZ());
            int[] colors = casing.config.getColors();
            WorldEffects.submit(pose, CompatRenderTypes.entityCutoutCull(CASING_TEXTURE), 0,
                    (drawPose, buffer) -> {
                        for (int i = 0; i < casing.config.getType().partNames.length; i++) {
                            int color = colors == null || colors.length == 0 ? 0xFFFFFF
                                    : colors[Math.min(i, colors.length - 1)];
                            model().renderPart(casing.config.getType().partNames[i], drawPose, buffer, light,
                                    OverlayTexture.NO_OVERLAY, 0xFF000000 | color);
                        }
                    });

            if (casing.smokeNodes.size() < 2) continue;
            casing.dragSmokeNodes(casing.interpX(partial), casing.interpY(partial), casing.interpZ(partial));
            float scale = casing.config.getScaleX() * 0.5F * ParticleSpentCasing.SCALE;
            Vec3 smokeVector = new Vec3(scale, 0D, 0D).yRot((float) Math.toRadians(-playerYaw));
            float[] nodes = new float[casing.smokeNodes.size() * 4];
            double timeAlpha = 1D - casing.age() / (double) Math.max(1, casing.config.getMaxAge());
            for (int i = 0; i < casing.smokeNodes.size(); i++) {
                ParticleSpentCasing.SmokeNode node = casing.smokeNodes.get(i);
                nodes[i * 4] = (float) node.x;
                nodes[i * 4 + 1] = (float) node.y;
                nodes[i * 4 + 2] = (float) node.z;
                nodes[i * 4 + 3] = (float) (node.alpha * timeAlpha);
            }
            PoseStack smokePose = new PoseStack();
            smokePose.translate(px, py - casing.heightQuarter(), pz);
            smokePose.mulPose(new org.joml.Quaternionf(camera.orientation).conjugate());
            WorldEffects.submit(smokePose, CompatRenderTypes.entityTranslucent(WHITE_TEXTURE), 1,
                    (drawPose, buffer) -> drawSmoke(drawPose, buffer, nodes, smokeVector, light));
        }
    }

    private static WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(MODEL_ID);
        return model;
    }

    private static void drawSmoke(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
            float[] nodes, Vec3 vector, int light) {
        for (int i = 0; i + 7 < nodes.length; i += 4) {
            float nx = nodes[i], ny = nodes[i + 1], nz = nodes[i + 2], na = nodes[i + 3];
            float px = nodes[i + 4], py = nodes[i + 5], pz = nodes[i + 6], pa = nodes[i + 7];
            int near = Colors.colorFromFloat(na, 1F, 1F, 1F);
            int far = Colors.colorFromFloat(pa, 1F, 1F, 1F);
            Vertexes.vertex(buffer, pose, nx, ny, nz, near, 0F, 0F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, nx + (float) vector.x, ny, nz + (float) vector.z, near,
                    1F, 0F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, px + (float) vector.x, py, pz + (float) vector.z, far,
                    1F, 1F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, px, py, pz, far, 0F, 1F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, nx, ny, nz, near, 0F, 0F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, nx - (float) vector.x, ny, nz - (float) vector.z, near,
                    1F, 0F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, px - (float) vector.x, py, pz - (float) vector.z, far,
                    1F, 1F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
            Vertexes.vertex(buffer, pose, px, py, pz, far, 0F, 1F, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
        }
    }
}
