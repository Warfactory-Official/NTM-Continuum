package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Animated OBJ renderer for the legacy turbofan. */
public final class TurbofanRenderer implements CompatBER<TileEntityMachineTurbofan, TurbofanRenderState> {
    private final WavefrontObj model;
    private final RenderType bodyType;
    private final RenderType backType;
    private final RenderType afterburnerType;

    public TurbofanRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/turbofan.obj"));
        bodyType = CompatRenderTypes.entityCutoutNoCull(dev.golem.core.Ids.of(
                Constants.MOD_ID, "textures/models/machines/turbofan.png"));
        backType = CompatRenderTypes.entityCutoutNoCull(dev.golem.core.Ids.of(
                Constants.MOD_ID, "textures/models/machines/turbofan_back.png"));
        afterburnerType = CompatRenderTypes.entityCutoutNoCull(dev.golem.core.Ids.of(
                Constants.MOD_ID, "textures/models/machines/turbofan_afterburner.png"));
    }

    @Override public TurbofanRenderState createRenderState() { return new TurbofanRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineTurbofan be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineTurbofan be, TurbofanRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = 90F + switch (ForgeDirection.getOrientation(meta - BlockDummyable.offset)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        state.spin = be.lastSpin + (be.spin - be.lastSpin) * partialTicks;
        state.afterburner = be.afterburner > 0;
    }

    @Override
    public void submit(TurbofanRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.pushPose();
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-state.spin));
        poseStack.translate(0F, -1.5F, 0F);
        part(sink, poseStack, bodyType, "Blades", state.lightCoords);
        poseStack.popPose();
        part(sink, poseStack, bodyType, "Body", state.lightCoords);
        part(sink, poseStack, state.afterburner ? afterburnerType : backType, "Afterburner", state.lightCoords);
        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, RenderType type, String name, int light) {
        sink.submitCustomGeometry(poseStack, type,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
