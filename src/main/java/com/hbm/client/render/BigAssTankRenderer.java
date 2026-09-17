package com.hbm.client.render;

import com.hbm.tileentity.machine.TileEntityMachineBigAssTank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Exact world renderer for the 9x9 Big Ass Tank and its moving fluid sheet. */
public final class BigAssTankRenderer extends GiantTankRenderer<TileEntityMachineBigAssTank> {

    public BigAssTankRenderer() {
        super("bigasstank", "bigasstank");
    }

    @Override
    protected void extractMachine(TileEntityMachineBigAssTank tank, GiantTankRenderState state) {
        state.yaw = switch (tank.getFacing()) {
            case NORTH -> 270F;
            case SOUTH -> 90F;
            case WEST -> 0F;
            case EAST -> 180F;
            default -> 0F;
        };
        state.tilted = tank.tilted;
    }

    @Override
    protected void placeMachine(Gem.Part<GiantTankRenderState> part) {
        part.slide(Gem.Axes.Y, state -> state.tilted ? -1F : 0F)
                .angle(Gem.Axes.Z, state -> state.tilted ? 10F : 0F)
                .angle(Gem.Axes.Y, state -> state.tilted ? 5F : 0F)
                .angle(Gem.Axes.Y, state -> state.yaw);
    }

    @Override
    protected void applyMachineTransform(GiantTankRenderState state, PoseStack poseStack) {
        if (state.tilted) {
            poseStack.translate(0F, -1F, 0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(10F));
            poseStack.mulPose(Axis.YP.rotationDegrees(5F));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
    }

    @Override
    protected void submitHazards(GiantTankRenderState state, PoseStack poseStack, SubmitSink collector) {
        Hazard hazard = hazardFor(state.fluidType);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(22.5F));
        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            poseStack.translate(5.5F, 2F, 0F);
            submitHazardQuad(poseStack, collector, hazard);
            poseStack.popPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        }
        poseStack.popPose();
    }

    @Override
    protected void submitFluid(GiantTankRenderState state, PoseStack poseStack, SubmitSink collector) {
        float height = fillHeight(state, 1.5F);
        if (height <= 0F) return;
        RenderType fluidType = CompatRenderTypes.entityTranslucentEmissive(state.fluidType.getTexture());
        float minU = -((state.gameTime % 250L + state.partialTick) / 250F) % 1F;
        float maxU = minU + 0.5F;
        collector.submitCustomGeometry(poseStack, fluidType, (pose, buffer) -> {
            fluidVertex(buffer, pose, -5.9375F, 1.75F, -0.25F, minU, 0F);
            fluidVertex(buffer, pose, -5.9375F, 1.75F + height, -0.25F, minU, -height);
            fluidVertex(buffer, pose, -5.9375F, 1.75F + height, 0.25F, maxU, -height);
            fluidVertex(buffer, pose, -5.9375F, 1.75F, 0.25F, maxU, 0F);
            fluidVertex(buffer, pose, 5.9375F, 1.75F, -0.25F, maxU, 0F);
            fluidVertex(buffer, pose, 5.9375F, 1.75F + height, -0.25F, maxU, -height);
            fluidVertex(buffer, pose, 5.9375F, 1.75F + height, 0.25F, minU, -height);
            fluidVertex(buffer, pose, 5.9375F, 1.75F, 0.25F, minU, 0F);
        });
    }

    private static void fluidVertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                                    float x, float y, float z, float u, float v) {
        Vertexes.vertex(buffer, pose, x, y, z, 0xFFFFFFFF, u, v,
                OverlayTexture.NO_OVERLAY, 0x00F000F0, 0F, 1F, 0F);
    }
}
