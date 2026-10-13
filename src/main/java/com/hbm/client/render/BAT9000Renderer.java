package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidHazard;
import com.hbm.tileentity.machine.TileEntityMachineBAT9000;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Exact world renderer for BAT-9000, including its four colored fluid faces. */
public final class BAT9000Renderer extends GiantTankRenderer<TileEntityMachineBAT9000> {

    private static final RenderType WHITE = CompatRenderTypes.entityTranslucentEmissive(
            Ids.of(Constants.MOD_ID, "textures/models/misc/white.png"));

    public BAT9000Renderer() {
        super("bat9000", "bat9000");
    }

    @Override
    protected void extractMachine(TileEntityMachineBAT9000 tank, GiantTankRenderState state) {
        state.yaw = 0F;
        state.tilted = false;
    }

    @Override
    protected void placeMachine(Gem.Part<GiantTankRenderState> part) {
    }

    @Override
    protected void applyMachineTransform(GiantTankRenderState state, PoseStack poseStack) {
    }

    @Override
    protected void submitHazards(GiantTankRenderState state, PoseStack poseStack, SubmitSink collector) {
        FluidHazard hazard = hazardFor(state.fluidType);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45F));
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(2.5F, 2.25F, 0F);
            poseStack.scale(1F, 0.75F, 0.75F);
            submitHazardQuad(poseStack, collector, hazard);
            poseStack.popPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        }
        poseStack.popPose();
    }

    @Override
    protected void submitFluid(GiantTankRenderState state, PoseStack poseStack, SubmitSink collector) {
        float height = fillHeight(state, 1.5F);
        if (height <= 0F) return;
        int color = 0xFF000000 | (state.fluidType.getColor() & 0xFFFFFF);
        collector.submitCustomGeometry(poseStack, WHITE,
                (pose, buffer) -> {
                    quad(buffer, pose, -2.2F, 1.5F, -0.5F, -2.2F, 1.5F + height, -0.5F,
                            -2.2F, 1.5F + height, 0.5F, -2.2F, 1.5F, 0.5F, color);
                    quad(buffer, pose, 2.2F, 1.5F, -0.5F, 2.2F, 1.5F + height, -0.5F,
                            2.2F, 1.5F + height, 0.5F, 2.2F, 1.5F, 0.5F, color);
                    quad(buffer, pose, -0.5F, 1.5F, -2.2F, -0.5F, 1.5F + height, -2.2F,
                            0.5F, 1.5F + height, -2.2F, 0.5F, 1.5F, -2.2F, color);
                    quad(buffer, pose, -0.5F, 1.5F, 2.2F, -0.5F, 1.5F + height, 2.2F,
                            0.5F, 1.5F + height, 2.2F, 0.5F, 1.5F, 2.2F, color);
                });
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, float x1, float y1,
                             float z1, float x2, float y2, float z2, float x3, float y3, float z3,
                             float x4, float y4, float z4, int color) {
        vertex(buffer, pose, x1, y1, z1, color);
        vertex(buffer, pose, x2, y2, z2, color);
        vertex(buffer, pose, x3, y3, z3, color);
        vertex(buffer, pose, x4, y4, z4, color);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, float z, int color) {
        Vertexes.vertex(buffer, pose, x, y, z, color, 0.5F, 0.5F,
                OverlayTexture.NO_OVERLAY, 0x00F000F0, 0F, 1F, 0F);
    }
}
