package com.hbm.client.particle;

import com.hbm.client.model.WavefrontObj;
import com.hbm.particle.SpentCasing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.Ids;
import dev.golem.core.client.Colors;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Source {@code CasingParticleGroup}, submitted through Core's world-effects seam. */
public final class CasingParticleGroup {
    private static final Identifier MODEL_ID = Ids.of("hbm", "models/effect/casings.obj");
    private static final Identifier CASING_TEXTURE = Ids.of("hbm", "textures/particle/casings.png");
    private static final Identifier WHITE_TEXTURE = Ids.of("hbm", "textures/models/misc/white.png");
    private static final Surface CASING_SKIN = Surface.cutoutCull(CASING_TEXTURE.toString());
    private static final int MAX_PARTS = maxParts();
    private static WavefrontObj model;
    private static Gem.Model<ParticleSpentCasing> declaration;

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
    public void reload() { clear(); model = null; declaration = null; }

    public void submit(ClientLevel level, CameraInfo camera) {
        if (particles.isEmpty()) return;
        float partial = Frames.partialTick();
        float playerYaw = Minecraft.getInstance().player == null ? 0F
                : Minecraft.getInstance().player.getViewYRot(partial);
        for (ParticleSpentCasing casing : List.copyOf(particles)) {
            double px = casing.interpX(partial) - camera.pos.x;
            double py = casing.interpY(partial) - camera.pos.y;
            double pz = casing.interpZ(partial) - camera.pos.z;
            int light = casing.light(level, partial);
            PoseStack pose = new PoseStack();
            pose.translate(px, py - casing.heightQuarter() + casing.scaleLift(), pz);
            Gem.replay(declaration(), casing, pose, WorldEffects.sink(0), light);

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
            WorldEffects.submit(smokePose, CompatRenderTypes.entityTranslucent(WHITE_TEXTURE), 1,
                    (drawPose, buffer) -> drawSmoke(drawPose, buffer, nodes, smokeVector, light));
        }
    }

    private static WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(MODEL_ID);
        return model;
    }

    private static int maxParts() {
        int most = 0;
        for (SpentCasing.CasingType type : SpentCasing.CasingType.values()) {
            most = Math.max(most, type.partNames.length);
        }
        return most;
    }

    /**
     * The casing itself, stated once for every type.
     *
     * <p>A type is a slot count and a set of group names rather than a model of its own, so the
     * shotgun's second piece is the same slot the others leave hidden.
     */
    private static Gem.Model<ParticleSpentCasing> declaration() {
        if (declaration != null) return declaration;
        Gem.Model<ParticleSpentCasing> model = Gem.model();
        Gem.Part<ParticleSpentCasing> body = model.joint()
                .scale(ParticleSpentCasing.SCALE)
                .angle(Gem.Axes.Y, casing -> 180F - casing.interpYaw(Frames.partialTick()))
                .angle(Gem.Axes.X, casing -> -casing.interpPitch(Frames.partialTick()))
                .stretch(Gem.Axes.X, casing -> casing.config.getScaleX())
                .stretch(Gem.Axes.Y, casing -> casing.config.getScaleY())
                .stretch(Gem.Axes.Z, casing -> casing.config.getScaleZ());
        for (int i = 0; i < MAX_PARTS; i++) {
            final int slot = i;
            body.child(model().part(anyPartName(slot)), CASING_SKIN)
                    .mesh(casing -> model().part(partName(casing, slot)))
                    .visibleIf(casing -> slot < casing.config.getType().partNames.length)
                    .tint(casing -> tint(casing, slot));
        }
        declaration = model.seal();
        return declaration;
    }

    private static String anyPartName(int slot) {
        for (SpentCasing.CasingType type : SpentCasing.CasingType.values()) {
            if (slot < type.partNames.length) return type.partNames[slot];
        }
        throw new IllegalStateException("No casing type has a part at slot " + slot);
    }

    private static String partName(ParticleSpentCasing casing, int slot) {
        String[] names = casing.config.getType().partNames;
        return names[Math.min(slot, names.length - 1)];
    }

    private static int tint(ParticleSpentCasing casing, int slot) {
        int[] colors = casing.config.getColors();
        if (colors == null || colors.length == 0) return 0xFFFFFFFF;
        return 0xFF000000 | colors[Math.min(slot, colors.length - 1)];
    }

    private static void drawSmoke(PoseStack.Pose pose, VertexConsumer buffer,
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
