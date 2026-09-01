package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
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

/** Animated legacy pyro oven renderer, including the exact Slider and Fan pivots. */
public final class PyroOvenRenderer implements CompatBER<TileEntityMachinePyroOven, PyroOvenRenderState> {

    private static final float BASE_YAW = 180F;

    private final WavefrontObj model;
    private final RenderType renderType;

    public PyroOvenRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/pyrooven.obj"));
        renderType = CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/models/machines/pyrooven.png"));
    }

    @Override public PyroOvenRenderState createRenderState() { return new PyroOvenRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachinePyroOven be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    /** The old {@code BobMathUtil.sps}: a sine of a cosine, not a plain sine wave. */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    @Override
    public void extract(TileEntityMachinePyroOven be, PyroOvenRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META)
                ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = BASE_YAW + switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH
        };
        state.anim = be.prevAnim + (be.anim - be.prevAnim) * partialTicks;
    }

    @Override
    public void submit(PyroOvenRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));

        part(sink, poseStack, "Oven", light);

        poseStack.pushPose();
        poseStack.translate((float) (sps(state.anim * 0.125D) / 2D - 0.5D), 0F, 0F);
        part(sink, poseStack, "Slider", light);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(1.5F, 0F, 1.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (state.anim * -15D % 360D)));
        poseStack.translate(-1.5F, 0F, -1.5F);
        part(sink, poseStack, "Fan", light);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, String name, int light) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
