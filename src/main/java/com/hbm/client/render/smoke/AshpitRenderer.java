package com.hbm.client.render.smoke;

import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityAshpit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** The heating-oven model used by the old RenderAshpit, including its sliding door. */
public final class AshpitRenderer implements CompatBER<TileEntityAshpit, AshpitRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of("hbm", "models/machines/heating_oven.obj"));
    private final RenderType renderType = CompatRenderTypes.entityCutout(
            dev.golem.core.Ids.of("hbm", "textures/models/machines/ashpit.png"));

    @Override public AshpitRenderState createRenderState() { return new AshpitRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityAshpit blockEntity, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityAshpit ashpit, AshpitRenderState state, float partialTicks, Vec3 cameraPosition) {
        int meta = ashpit.getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset;
        // The old renderer applied its facing switch and then a final -90 degree rotation.
        state.yaw = -90F + switch (meta) {
            case 5 -> 90F;
            case 2 -> 180F;
            case 4 -> 270F;
            default -> 0F;
        };
        state.doorAngle = ashpit.prevDoorAngle
                + (ashpit.doorAngle - ashpit.prevDoorAngle) * partialTicks;
        state.full = ashpit.isFull;
    }

    @Override
    public void submit(AshpitRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));

        part(poseStack, collector, "Main", light);

        poseStack.pushPose();
        poseStack.translate(0F, 0F, state.doorAngle * 0.75F / 135F);
        part(poseStack, collector, "Door", light);
        poseStack.popPose();

        part(poseStack, collector, state.full ? "InnerBurning" : "Inner", light);
        poseStack.popPose();
    }

    private void part(PoseStack poseStack, SubmitSink collector, String name, int light) {
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
