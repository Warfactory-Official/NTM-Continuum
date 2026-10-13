package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntitySolarMirror;
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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** A heliostat: the post, the mirror aimed at the tower, and the beam it throws while lit. */
public final class SolarMirrorRenderer extends GemBER<TileEntitySolarMirror, SolarMirrorRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/solar_mirror.obj"));
    private final Surface surface = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/solar_mirror.png");
    private final RenderType beamType = CompatRenderTypes.additive();

    @Override
    protected void setup(Gem.Model<SolarMirrorRenderState> declaration) {
        Gem.Part<SolarMirrorRenderState> post = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F);

        post.child(model.parts(Set.of("Base"), true), surface);
        post.joint()
                .translate(0F, 1F, 0F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .angle(Gem.Axes.Z, state -> state.pitch)
                .translate(0F, -1F, 0F)
                .child(model.parts(Set.of("Mirror"), true), surface);
    }

    @Override
    public SolarMirrorRenderState createRenderState() {
        return new SolarMirrorRenderState();
    }

    /**
     * A mirror with no target above it does not aim: upstream skips the two rotations entirely, which is
     * a zero for both angles here: the render state is reused between frames, so they have to be
     * cleared rather than left holding the last aim.
     */
    @Override
    public void extract(TileEntitySolarMirror blockEntity, SolarMirrorRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        state.targetX = blockEntity.tX;
        state.targetY = blockEntity.tY;
        state.targetZ = blockEntity.tZ;
        state.isOn = blockEntity.isOn;

        int dx = state.targetX - state.blockPos.getX();
        int dy = state.targetY - state.blockPos.getY();
        int dz = state.targetZ - state.blockPos.getZ();
        state.distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        state.targetAbove = state.targetY >= state.blockPos.getY();
        state.pitch = state.targetAbove
                ? (float) (Math.toDegrees(-Math.asin((dy + 0.5D) / state.distance)) + 90D) : 0F;
        state.yaw = state.targetAbove
                ? (float) (Math.toDegrees(-Math.atan2(dz, dx)) + 180D) : 0F;
    }

    /** The beam is a tapered box built from the distance to the tower, drawn untextured and additive. */
    @Override
    protected void submitUndeclared(SolarMirrorRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (!state.isOn) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.translate(0F, 1F, 0F);
        if (state.targetAbove) {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        }
        poseStack.translate(0F, -1F, 0F);
        sink.submitCustomGeometry(poseStack, beamType,
                (pose, buffer) -> renderBeam(pose, buffer, state.distance));
        poseStack.popPose();
    }

    private static void renderBeam(PoseStack.Pose pose, VertexConsumer buffer,
                                   float distance) {
        float min = 0.008F;
        float max = 0.008F;
        beamVertex(buffer, pose, 0.5F, 1.0625F, 0.5F, max);
        beamVertex(buffer, pose, 0.5F, 1.0625F, -0.5F, max);
        beamVertex(buffer, pose, 0.5F, distance, -0.5F, min);
        beamVertex(buffer, pose, 0.5F, distance, 0.5F, min);

        beamVertex(buffer, pose, -0.5F, 1.0625F, 0.5F, max);
        beamVertex(buffer, pose, -0.5F, 1.0625F, -0.5F, max);
        beamVertex(buffer, pose, -0.5F, distance, -0.5F, min);
        beamVertex(buffer, pose, -0.5F, distance, 0.5F, min);

        beamVertex(buffer, pose, 0.5F, 1.0625F, 0.5F, max);
        beamVertex(buffer, pose, -0.5F, 1.0625F, 0.5F, max);
        beamVertex(buffer, pose, -0.5F, distance, 0.5F, min);
        beamVertex(buffer, pose, 0.5F, distance, 0.5F, min);

        beamVertex(buffer, pose, 0.5F, 1.0625F, -0.5F, max);
        beamVertex(buffer, pose, -0.5F, 1.0625F, -0.5F, max);
        beamVertex(buffer, pose, -0.5F, distance, -0.5F, min);
        beamVertex(buffer, pose, 0.5F, distance, -0.5F, min);
    }

    private static void beamVertex(VertexConsumer buffer, PoseStack.Pose pose,
                                   float x, float y, float z, float alpha) {
        Vertexes.color(buffer, pose, x, y, z, 1F, 1F, 1F, alpha);
    }
}
