package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineStrandCaster;
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

/** Strand Caster BER: old caster/plate parts plus a fullbright tinted molten surface. */
public class StrandCasterRenderer implements CompatBER<TileEntityMachineStrandCaster, StrandCasterRenderer.State> {

    public static class State extends CompatRenderState {
        public ForgeDirection direction = ForgeDirection.NORTH;
        public boolean hasMelt;
        public int color;
        public float level;
        public float plateOffset;
    }

    private final WavefrontObj model;
    private final RenderType bodyType;
    private final RenderType lavaType;

    public StrandCasterRenderer() {
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/strand_caster.obj"));
        this.bodyType = CompatRenderTypes.entityCutout(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/strand_caster.png"));
        this.lavaType = CompatRenderTypes.entityTranslucentEmissive(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/lava_gray.png"));
    }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineStrandCaster be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineStrandCaster be, State state, float partialTicks, Vec3 cameraPosition) {
        int meta = be.getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset;
        state.direction = ForgeDirection.getOrientation(meta);
        state.hasMelt = be.amount > 0 && be.type != null && be.getInstalledMold() != null;
        if (state.hasMelt) {
            state.color = 0xFF000000 | (be.type.moltenColor & 0xFFFFFF);
            state.level = (float) (be.amount * 0.675D / Math.max(be.getCapacity(), 1));
            state.plateOffset = (float) (be.amount * 0.375D / Math.max(be.getInstalledMold().getCost(), 1));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw(state.direction)));
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));

        collector.submitCustomGeometry(poseStack, bodyType,
                (pose, buffer) -> model.renderPart("caster", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));

        if (state.hasMelt) {
            poseStack.pushPose();
            poseStack.translate(0F, 0F, Math.max(-state.plateOffset + 3.4F, 0F));
            collector.submitCustomGeometry(poseStack, bodyType,
                    (pose, buffer) -> model.renderPart("plate", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
            poseStack.popPose();

            float y = 2.3F + state.level;
            collector.submitCustomGeometry(poseStack, lavaType,
                    (pose, buffer) -> FoundryRenderHelper.flatQuad(pose, buffer, -0.9F, -0.999F,
                            0.9F, 0.999F, y, state.color));
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
