package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Large Arc Furnace BER: dynamic contents, lid, electrode states, cables, and molten pour stream. */
public class ArcFurnaceRenderer implements CompatBER<TileEntityMachineArcFurnaceLarge, ArcFurnaceRenderer.State> {

    public static class State extends CompatRenderState {
        public ForgeDirection direction = ForgeDirection.NORTH;
        public float lid;
        public boolean progressing;
        public boolean hasMaterial;
        public long gameTime;
        public int meltAmount;
        public int pourColor = -1;
        public float pourLength;
        public final byte[] electrodes = new byte[3];
    }

    private final WavefrontObj model;
    private final RenderType bodyType;
    private final RenderType hotType;
    private final RenderType streamType;

    public ArcFurnaceRenderer() {
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/arc_furnace.obj"));
        var texture = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/arc_furnace.png");
        this.bodyType = CompatRenderTypes.entityCutout(texture);
        this.hotType = CompatRenderTypes.entityTranslucentEmissive(texture);
        this.streamType = CompatRenderTypes.entitySolid(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/lava_gray.png"));
    }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineArcFurnaceLarge be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineArcFurnaceLarge be, State state, float partialTicks, Vec3 cameraPosition) {
        int meta = be.getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset;
        state.direction = ForgeDirection.getOrientation(meta);
        state.lid = be.prevLid + (be.lid - be.prevLid) * partialTicks;
        state.progressing = be.isProgressing;
        state.hasMaterial = be.hasMaterial;
        state.gameTime = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        state.meltAmount = TileEntityMachineArcFurnaceLarge.getStackAmount(be.liquids);
        state.pourColor = be.pourColor;
        state.pourLength = be.pourLen;
        System.arraycopy(be.electrodes, 0, state.electrodes, 0, state.electrodes.length);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        float yaw = 90F + yaw(state.direction);
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        collector.submitCustomGeometry(poseStack, bodyType,
                (pose, buffer) -> model.renderPart("Furnace", pose, buffer, light, OverlayTexture.NO_OVERLAY));

        if (state.meltAmount > 0) {
            poseStack.pushPose();
            float y = -1.75F + state.meltAmount * 1.75F / TileEntityMachineArcFurnaceLarge.MAX_LIQUID;
            poseStack.translate(0F, y, 0F);
            collector.submitCustomGeometry(poseStack, hotType,
                    (pose, buffer) -> model.renderPart("ContentsHot", pose, buffer, light, OverlayTexture.NO_OVERLAY));
            poseStack.popPose();
        } else if (state.hasMaterial) {
            collector.submitCustomGeometry(poseStack, bodyType,
                    (pose, buffer) -> model.renderPart("ContentsCold", pose, buffer, light, OverlayTexture.NO_OVERLAY));
        }

        poseStack.pushPose();
        poseStack.translate(0F, 2F * state.lid, 0F);
        if (state.progressing) poseStack.translate(0F, 0F,
                (float) Math.sin((state.gameTime + 0.5F) * 1D) * 0.005F);
        collector.submitCustomGeometry(poseStack, bodyType,
                (pose, buffer) -> model.renderPart("Lid", pose, buffer, light, OverlayTexture.NO_OVERLAY));
        for (int i = 0; i < 3; i++) {
            int index = i + 1;
            if (state.electrodes[i] != TileEntityMachineArcFurnaceLarge.ELECTRODE_NONE) {
                collector.submitCustomGeometry(poseStack, bodyType,
                        (pose, buffer) -> model.renderPart("Ring" + index, pose, buffer, light, OverlayTexture.NO_OVERLAY));
            }
            if (state.electrodes[i] == TileEntityMachineArcFurnaceLarge.ELECTRODE_FRESH) {
                collector.submitCustomGeometry(poseStack, bodyType,
                        (pose, buffer) -> model.renderPart("Electrode" + index, pose, buffer, light, OverlayTexture.NO_OVERLAY));
            }
        }
        for (int i = 0; i < 3; i++) {
            int index = i + 1;
            String part = state.electrodes[i] == TileEntityMachineArcFurnaceLarge.ELECTRODE_USED
                    ? "Electrode" + index + "Hot" : "Electrode" + index + "Short";
            if (state.electrodes[i] != TileEntityMachineArcFurnaceLarge.ELECTRODE_NONE
                    && state.electrodes[i] != TileEntityMachineArcFurnaceLarge.ELECTRODE_FRESH) {
                collector.submitCustomGeometry(poseStack, hotType,
                        (pose, buffer) -> model.renderPart(part, pose, buffer, light, OverlayTexture.NO_OVERLAY));
            }
        }
        for (int i = 0; i < 3; i++) {
            int index = i + 1;
            if (state.electrodes[i] == TileEntityMachineArcFurnaceLarge.ELECTRODE_NONE) continue;
            poseStack.pushPose();
            poseStack.translate(0F, 5.5F, i == 0 ? 0.5F : i == 2 ? -0.5F : 0F);
            if (state.progressing) poseStack.mulPose(Axis.XP.rotationDegrees(
                    (float) Math.sin((state.gameTime + 0.5F) / 2D) * 30F));
            poseStack.translate(0F, -5.5F, i == 0 ? -0.5F : i == 2 ? 0.5F : 0F);
            collector.submitCustomGeometry(poseStack, bodyType,
                    (pose, buffer) -> model.renderPart("Cable" + index, pose, buffer, light, OverlayTexture.NO_OVERLAY));
            poseStack.popPose();
        }
        poseStack.popPose();

        if (state.pourColor >= 0 && state.pourLength > 0F) {
            poseStack.pushPose();
            poseStack.translate(state.direction.offsetX * 2.875F, 1F,
                    state.direction.offsetZ * 2.875F);
            int color = state.pourColor;
            collector.submitCustomGeometry(poseStack, streamType,
                    (pose, buffer) -> FoundryRenderHelper.pourStream(pose, buffer, state.direction,
                            state.pourLength, 0.625F, 0.625F, color));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static float yaw(ForgeDirection direction) {
        return switch (direction) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
    }
}
