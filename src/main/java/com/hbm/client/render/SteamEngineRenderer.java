package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineSteamEngine;
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

/** Faithful split-part renderer for the five-part steam engine OBJ. */
public final class SteamEngineRenderer
        implements CompatBER<TileEntityMachineSteamEngine, SteamEngineRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/steam_engine.obj"));
    // old-hbm disabled GL face culling around every steam-engine part, including the hollow backs.
    private final RenderType renderType = CompatRenderTypes.entityCutoutNoCull(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/steam_engine.png"));

    @Override public SteamEngineRenderState createRenderState() { return new SteamEngineRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineSteamEngine be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineSteamEngine be, SteamEngineRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (ForgeDirection.getOrientation(meta - BlockDummyable.offset)) {
            case SOUTH -> 90F;
            case EAST -> 180F;
            case NORTH -> 270F;
            default -> 0F;
        };
        state.rotor = be.getRotor(partialTicks);
    }

    @Override
    public void submit(SteamEngineRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(2F, 0F, 0F);

        part(sink, poseStack, "Base", state.lightCoords);

        poseStack.pushPose();
        poseStack.translate(2F, 1.375F, 0F);
        poseStack.mulPose(Axis.ZN.rotationDegrees(state.rotor));
        poseStack.translate(-2F, -1.375F, 0F);
        part(sink, poseStack, "Flywheel", state.lightCoords);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0F, 1.375F, -0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.rotor * 2F));
        poseStack.translate(0F, -1.375F, 0.5F);
        part(sink, poseStack, "Shaft", state.lightCoords);
        poseStack.popPose();

        double sin = Math.sin(state.rotor * Math.PI / 180D) * 0.25D - 0.25D;
        double cos = Math.cos(state.rotor * Math.PI / 180D) * 0.25D;
        double angle = Math.acos(cos / 1.875D) * 180D / Math.PI - 90D;
        poseStack.pushPose();
        poseStack.translate(sin + 2.25D, cos + 1.375D, 0D);
        poseStack.mulPose(Axis.ZN.rotationDegrees((float) angle));
        poseStack.translate(-2.25D, -1.375D, 0D);
        part(sink, poseStack, "Transmission", state.lightCoords);
        poseStack.popPose();

        double cath = Math.sqrt(3.515625D - (cos * cos) / 2D);
        poseStack.pushPose();
        poseStack.translate(1.875D - cath + sin, 0D, 0D);
        part(sink, poseStack, "Piston", state.lightCoords);
        poseStack.popPose();
        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, String name, int light) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
