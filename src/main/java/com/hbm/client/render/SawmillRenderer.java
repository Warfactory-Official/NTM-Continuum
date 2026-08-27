package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.tileentity.machine.TileEntitySawmill;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** Renderer for the sawmill's three animated OBJ parts. */
public class SawmillRenderer implements CompatBER<TileEntitySawmill, SawmillRenderState> {

    private final com.hbm.client.model.WavefrontObj model = com.hbm.client.model.WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/sawmill.obj"));
    private final net.minecraft.client.renderer.rendertype.RenderType renderType = CompatRenderTypes.entityCutout(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/sawmill.png"));

    @Override
    public SawmillRenderState createRenderState() {
        return new SawmillRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntitySawmill blockEntity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extract(TileEntitySawmill blockEntity, SawmillRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        int meta = blockEntity.getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset;
        state.yaw = switch (meta) {
            case 3 -> 0F;
            case 5 -> 90F;
            case 2 -> 180F;
            case 4 -> 270F;
            default -> 0F;
        };
        state.rotation = blockEntity.lastSpin + (blockEntity.spin - blockEntity.lastSpin) * partialTicks;
        state.hasBlade = blockEntity.hasBlade;
    }

    @Override
    public void submit(SawmillRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart("Main", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        if (state.hasBlade) {
            poseStack.pushPose();
            poseStack.translate(0F, 1.375F, 0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-state.rotation * 2F));
            poseStack.translate(0F, -1.375F, 0F);
            collector.submitCustomGeometry(poseStack, renderType,
                    (pose, buffer) -> model.renderPart("Blade", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.5625F, 1.375F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.rotation));
        poseStack.translate(-0.5625F, -1.375F, 0F);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart("GearLeft", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.5625F, 1.375F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.rotation));
        poseStack.translate(0.5625F, -1.375F, 0F);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart("GearRight", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
        poseStack.popPose();
    }
}
