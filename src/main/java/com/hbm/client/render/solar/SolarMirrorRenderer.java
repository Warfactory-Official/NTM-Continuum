package com.hbm.client.render.solar;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.solar.TileEntitySolarMirror;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Aimed mirror model plus the textureless additive sunlight beam from the legacy renderer. */
public final class SolarMirrorRenderer implements CompatBER<TileEntitySolarMirror, SolarMirrorRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/solar_mirror.obj"));
    private final RenderType modelType = CompatRenderTypes.entityCutoutNoCull(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/solar_mirror.png"));
    private final RenderType beamType = CompatRenderTypes.lightning();

    @Override
    public SolarMirrorRenderState createRenderState() {
        return new SolarMirrorRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntitySolarMirror blockEntity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

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
        if (state.targetAbove) {
            state.pitch = (float) (Math.toDegrees(-Math.asin((dy + 0.5D) / state.distance)) + 90D);
            state.yaw = (float) (Math.toDegrees(-Math.atan2(dz, dx)) + 180D);
        }
    }

    @Override
    public void submit(SolarMirrorRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        sink.submitCustomGeometry(poseStack, modelType,
                (pose, buffer) -> model.renderPart("Base", pose, buffer, state.lightCoords,
                        OverlayTexture.NO_OVERLAY, -1, 0F, 0F, true));

        poseStack.translate(0F, 1F, 0F);
        if (state.targetAbove) {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        }
        poseStack.translate(0F, -1F, 0F);
        sink.submitCustomGeometry(poseStack, modelType,
                (pose, buffer) -> model.renderPart("Mirror", pose, buffer, state.lightCoords,
                        OverlayTexture.NO_OVERLAY, -1, 0F, 0F, true));

        if (state.isOn) {
            sink.submitCustomGeometry(poseStack, beamType,
                    (pose, buffer) -> renderBeam(pose, buffer, state.distance));
        }
        poseStack.popPose();
    }

    private static void renderBeam(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
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

    private static void beamVertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                                   float x, float y, float z, float alpha) {
        Vertexes.color(buffer, pose, x, y, z, 1F, 1F, 1F, alpha);
    }
}
