package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.network.TileEntityPistonInserter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code RenderPistonInserter}: the frame, the piston sliding out by the interpolated extension, and the held item on it. */
public class PistonInserterRenderer extends GemBER<TileEntityPistonInserter, PistonInserterRenderer.State> {

    public static class State extends CompatRenderState {
        public Direction facing = Direction.UP;
        public float extend;
        public final CompatItemRenderState item = new CompatItemRenderState();
        public boolean showItem;
        public boolean blockItem;
    }

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/piston_inserter.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/piston_inserter.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
        for (Direction facing : Direction.values()) {
            Gem.Part<State> joint = declaration.joint().visibleIf(state -> state.facing == facing)
                    .translate(0.5F, 0.5F, 0.5F);
            orient(joint, facing);
            joint.translate(0F, -0.5F, 0F);
            joint.child(model.part("Frame"), surface);
            joint.child(model.part("Piston"), surface).slide(Gem.Axes.Y, state -> state.extend * 0.9375F);
        }
    }

    /** The 1.7 per-metadata GL rotation sequence. */
    private static void orient(Gem.Part<State> part, Direction facing) {
        switch (facing) {
            case DOWN -> part.rotate(Gem.Axes.X, 180F);
            case UP -> {}
            case NORTH -> part.rotate(Gem.Axes.X, -90F).rotate(Gem.Axes.Y, 180F);
            case WEST -> part.rotate(Gem.Axes.Z, 90F).rotate(Gem.Axes.Y, -90F);
            case SOUTH -> part.rotate(Gem.Axes.X, 90F);
            case EAST -> part.rotate(Gem.Axes.Z, -90F).rotate(Gem.Axes.Y, 90F);
        }
    }

    private static void orient(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180F));
            case UP -> {}
            case NORTH -> { poseStack.mulPose(Axis.XP.rotationDegrees(-90F)); poseStack.mulPose(Axis.YP.rotationDegrees(180F)); }
            case WEST -> { poseStack.mulPose(Axis.ZP.rotationDegrees(90F)); poseStack.mulPose(Axis.YP.rotationDegrees(-90F)); }
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90F));
            case EAST -> { poseStack.mulPose(Axis.ZP.rotationDegrees(-90F)); poseStack.mulPose(Axis.YP.rotationDegrees(90F)); }
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityPistonInserter be, State state, float partialTicks, Vec3 cameraPosition) {
        state.facing = be.getFacing();
        state.extend = (float) ((be.lastExtend + (be.renderExtend - be.lastExtend) * partialTicks) / (double) TileEntityPistonInserter.MAX_EXTEND);

        ItemStack held = be.slot;
        state.showItem = false;
        if (held != null && !held.isEmpty() && be.getLevel() != null) {
            state.blockItem = held.getItem() instanceof BlockItem;
            state.item.extract(be.getLevel(), held.copyWithCount(1), ItemDisplayContext.FIXED);
            state.showItem = !state.item.isEmpty();
        }
        if (!state.showItem) state.item.clear();
    }

    /** The item rides the piston head; 1.7 drew it "in frame" (flat, half size), which is the FIXED display context. */
    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (!state.showItem) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        orient(poseStack, state.facing);
        poseStack.translate(0F, -0.5F, 0F);
        poseStack.translate(0F, state.extend * 0.9375F, 0F);

        if (state.blockItem) {
            poseStack.translate(0D, 1.125D, 0D);
        } else {
            poseStack.translate(0D, 1.0625D, 0.1D);
            poseStack.mulPose(Axis.XN.rotationDegrees(90F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        sink.submitItem(poseStack, state.item, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
