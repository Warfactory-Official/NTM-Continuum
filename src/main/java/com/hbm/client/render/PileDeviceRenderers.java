package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import com.hbm.tileentity.machine.pile.TileEntityPileVent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The three Chicago Pile device renderers, which differ only in which part of the model moves. */
public final class PileDeviceRenderers {

    private PileDeviceRenderers() {}

    /**
     * Shared chrome: load the model, face the device the way its meta says, and draw. The devices sit
     * flush against a pile face, so the only orientation that matters is which of the four horizontals
     * the low two bits of the meta name.
     */
    private abstract static class Base<T extends BlockEntity> implements CompatBER<T, PileDeviceRenderState> {

        private final WavefrontObj model;
        private final RenderType renderType;

        Base(String name) {
            this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/pile/" + name + ".obj"));
            this.renderType = CompatRenderTypes.entityCutout(
                    dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/pile/" + name + ".png"));
        }

        @Override public PileDeviceRenderState createRenderState() { return new PileDeviceRenderState(); }

        protected void extractFacing(BlockEntity be, PileDeviceRenderState state) {
            BlockState blockState = be.getBlockState();
            int meta = blockState.hasProperty(BlockPileDevice.META) ? blockState.getValue(BlockPileDevice.META) : 0;
            state.yaw = switch (ForgeDirection.getOrientation(meta % 4 + 2)) {
                case NORTH -> 90F;
                case SOUTH -> 270F;
                case WEST -> 180F;
                default -> 0F;
            };
        }

        protected void part(SubmitSink sink, PoseStack poseStack, String name, int light) {
            sink.submitCustomGeometry(poseStack, renderType,
                    (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
        }
    }

    /** The control rod: the whole rod slides up out of its channel as it is withdrawn. */
    public static final class Control extends Base<TileEntityPileControl> {

        public Control() { super("pile_control"); }

        @Override
        public void extract(TileEntityPileControl be, PileDeviceRenderState state, float partialTicks, Vec3 camera) {
            extractFacing(be, state);
            state.motion = (float) (be.lastExtraction + (be.extraction - be.lastExtraction) * partialTicks);
        }

        @Override
        public void submit(PileDeviceRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            part(sink, poseStack, "Base", state.lightCoords);
            poseStack.translate(0F, state.motion * 0.75F, 0F);
            part(sink, poseStack, "Rod", state.lightCoords);
            poseStack.popPose();
        }
    }

    /** The fuel loader: a lever swings over as the ram slides in, carrying the rod with it. */
    public static final class Loader extends Base<TileEntityPileLoader> {

        public Loader() { super("pile_loader"); }

        @Override
        public void extract(TileEntityPileLoader be, PileDeviceRenderState state, float partialTicks, Vec3 camera) {
            extractFacing(be, state);
            state.motion = (float) (be.lastTravel + (be.travel - be.lastTravel) * partialTicks);
            state.loaded = !be.syncStack.isEmpty();
        }

        @Override
        public void submit(PileDeviceRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            part(sink, poseStack, "Loader", state.lightCoords);

            poseStack.pushPose();
            poseStack.translate(-0.1875F, 0.5F, 0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.motion * 90F));
            poseStack.translate(0.1875F, -0.5F, 0F);
            part(sink, poseStack, "Lever", state.lightCoords);
            poseStack.popPose();

            poseStack.translate(state.motion * -0.5F, 0F, 0F);
            part(sink, poseStack, "Slider", state.lightCoords);
            if (state.loaded) part(sink, poseStack, "Rod", state.lightCoords);
            poseStack.popPose();
        }
    }

    /** The vent: the fan spins whenever air is actually moving into the channel. */
    public static final class Vent extends Base<TileEntityPileVent> {

        public Vent() { super("pile_vent"); }

        @Override
        public void extract(TileEntityPileVent be, PileDeviceRenderState state, float partialTicks, Vec3 camera) {
            extractFacing(be, state);
            state.motion = be.lastFan + (be.fan - be.lastFan) * partialTicks;
        }

        @Override
        public void submit(PileDeviceRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            part(sink, poseStack, "Pipe", state.lightCoords);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.motion));
            part(sink, poseStack, "Fan", state.lightCoords);
            poseStack.popPose();
        }
    }
}
