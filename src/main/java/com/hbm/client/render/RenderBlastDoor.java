package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.machine.BlastDoor;
import com.hbm.tileentity.machine.TileEntityBlastDoor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Port of 1.7.10 {@code RenderBlastDoor}: base, block, tooth and up to four slider segments driven by a
 * five-second wall-clock ramp off the tile's {@code sysTime}.
 */
public class RenderBlastDoor implements CompatBER<TileEntityBlastDoor, RenderBlastDoor.State> {

    public static class State extends CompatRenderState {
        public boolean turned;
        public double timer;
    }

    private static Identifier obj(String name) { return Ids.of(Constants.MOD_ID, "models/doors/" + name + ".obj"); }
    private static Identifier tex(String name) { return Ids.of(Constants.MOD_ID, "textures/models/doors/" + name + ".png"); }

    private WavefrontObj base, tooth, slider, block;
    private final RenderType baseType = CompatRenderTypes.entityCutoutNoCull(tex("blast_door_base"));
    private final RenderType toothType = CompatRenderTypes.entityCutoutNoCull(tex("blast_door_tooth"));
    private final RenderType sliderType = CompatRenderTypes.entityCutoutNoCull(tex("blast_door_slider"));
    private final RenderType blockType = CompatRenderTypes.entityCutoutNoCull(tex("blast_door_block"));

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override public int viewDistance() { return 256; }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityBlastDoor blockEntity, Vec3 cameraPosition) { return true; }

    @Override
    public void extract(TileEntityBlastDoor te, State state, float partialTicks, Vec3 cameraPosition) {
        state.turned = te.getBlockState().hasProperty(BlastDoor.AXIS) && te.getBlockState().getValue(BlastDoor.AXIS) == Direction.Axis.X;
        long now = System.currentTimeMillis();
        if (te.state == 0)
            state.timer = getAnimationFromSysTime(5000);
        else if (te.state == 2)
            state.timer = 0;
        else if (te.isOpening)
            state.timer = getAnimationFromSysTime(te.sysTime + 5000 - now);
        else
            state.timer = getAnimationFromSysTime(now - te.sysTime);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (base == null) {
            base = WavefrontObj.load(obj("blast_door_base"));
            tooth = WavefrontObj.load(obj("blast_door_tooth"));
            slider = WavefrontObj.load(obj("blast_door_slider"));
            block = WavefrontObj.load(obj("blast_door_block"));
        }
        int light = state.lightCoords;
        int overlay = OverlayTexture.NO_OVERLAY;
        double timer = state.timer;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));

        if (state.turned)
            poseStack.mulPose(Axis.YP.rotationDegrees(90));

        all(base, baseType, poseStack, sink, light, overlay);

        poseStack.translate(0, 3, 0);
        all(block, blockType, poseStack, sink, light, overlay);

        poseStack.translate(0, -timer, 0);

        poseStack.translate(0, 2, 0);
        all(tooth, toothType, poseStack, sink, light, overlay);

        if (timer > 1D) {
            all(slider, sliderType, poseStack, sink, light, overlay);
        }
        if (timer > 2D) {
            poseStack.translate(0, 1, 0);
            all(slider, sliderType, poseStack, sink, light, overlay);
        }
        if (timer > 3D) {
            poseStack.translate(0, 1, 0);
            all(slider, sliderType, poseStack, sink, light, overlay);
        }
        if (timer > 4D) {
            poseStack.translate(0, 1, 0);
            all(slider, sliderType, poseStack, sink, light, overlay);
        }

        poseStack.popPose();
    }

    private static void all(WavefrontObj model, RenderType type, PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) -> model.renderAll(pose, buffer, light, overlay));
    }

    private static double getAnimationFromSysTime(long time) {
        double duration = 5000D;
        double extend = 5.0D;
        return Math.max(Math.min(time, duration) / duration * extend, 0.0D);
    }
}
