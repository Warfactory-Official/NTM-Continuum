package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityStirling;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Faithful split-part renderer for the legacy Stirling engine OBJ. */
public final class StirlingRenderer implements CompatBER<TileEntityStirling, StirlingRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/stirling.obj"));
    private final RenderType normal = CompatRenderTypes.entityCutout(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/stirling.png"));
    private final RenderType steel = CompatRenderTypes.entityCutout(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/stirling_steel.png"));
    private final RenderType creative = CompatRenderTypes.entityCutout(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/stirling_creative.png"));

    @Override public StirlingRenderState createRenderState() { return new StirlingRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityStirling be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityStirling be, StirlingRenderState state, float partialTicks, Vec3 cameraPosition) {
        int meta = be.getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset;
        state.yaw = switch (ForgeDirection.getOrientation(meta)) {
            case SOUTH -> 0F;
            case EAST -> 90F;
            case NORTH -> 180F;
            case WEST -> 270F;
            default -> 0F;
        };
        state.rotation = be.lastSpin + (be.spin - be.lastSpin) * partialTicks;
        state.type = be.getGearVariant();
        state.hasCog = be.hasCog;
    }

    @Override
    public void submit(StirlingRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        RenderType renderType = state.type == 0 ? normal : state.type == 2 ? creative : steel;

        part(sink, poseStack, renderType, "Base", state.lightCoords);
        if (state.hasCog) {
            poseStack.pushPose();
            poseStack.translate(0F, 1.375F, 0F);
            poseStack.mulPose(Axis.ZN.rotationDegrees(state.rotation));
            poseStack.translate(0F, -1.375F, 0F);
            part(sink, poseStack, renderType, "Cog", state.lightCoords);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(0F, 1.375F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.rotation * 2F + 3F));
        poseStack.translate(0F, -1.375F, -0.25F);
        part(sink, poseStack, renderType, "CogSmall", state.lightCoords);
        poseStack.popPose();

        poseStack.translate(Math.sin(state.rotation * Math.PI / 90D) * 0.25D + 0.125D, 0F, 0F);
        part(sink, poseStack, renderType, "Piston", state.lightCoords);
        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, RenderType renderType, String name, int light) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
