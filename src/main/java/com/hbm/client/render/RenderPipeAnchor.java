package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.network.FluidPipeAnchor;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.network.TileEntityPipeAnchor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code RenderPipeAnchor}: the {@code Anchor} part of {@code pipe_anchor.obj} turned to the face it is mounted on,
 * and for every linked anchor of the same fluid a {@code Pipe} stretched from this anchor's mount to the other's,
 * tinted with the fluid colour lightened by a quarter, and a {@code Ring} at half its length less 1.5.
 * Only the "dominant" anchor of a pair (the smaller mount point, x then y then z) draws the pipe.
 */
public class RenderPipeAnchor extends GemBER<TileEntityPipeAnchor, RenderPipeAnchor.State> {

    public static class State extends CompatRenderState {
        public float rotX, rotZ;
        /** Each pipe: dx, dy, dz from this anchor's mount to the other's, and the tint. */
        public final List<double[]> pipes = new ArrayList<>();
        public final List<Integer> colors = new ArrayList<>();
    }

    private static final Identifier TEX = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/network/pipe_anchor.png");
    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/network/pipe_anchor.obj"));

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
        declaration.part(model.part("Anchor"), Surface.cutout(TEX.toString()))
                .translate(0.5F, 0.5F, 0.5F)
                .angle(Gem.Axes.X, s -> s.rotX)
                .angle(Gem.Axes.Z, s -> s.rotZ)
                .translate(0F, -0.5F, 0F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityPipeAnchor anchor, State state, float partialTicks, Vec3 cameraPosition) {
        Direction facing = anchor.getBlockState().hasProperty(FluidPipeAnchor.FACING)
                ? anchor.getBlockState().getValue(FluidPipeAnchor.FACING) : Direction.UP;
        switch (facing) {
            case DOWN -> { state.rotX = 180; state.rotZ = 0; }
            case NORTH -> { state.rotX = 90; state.rotZ = 180; }
            case SOUTH -> { state.rotX = 90; state.rotZ = 0; }
            case WEST -> { state.rotX = 90; state.rotZ = 90; }
            case EAST -> { state.rotX = 90; state.rotZ = 270; }
            default -> { state.rotX = 0; state.rotZ = 0; }
        }

        state.pipes.clear();
        state.colors.clear();
        Level level = anchor.getLevel();
        if (level == null) return;
        for (BlockPos pos : anchor.getConnected()) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (!(tile instanceof TileEntityPipeAnchor other)) continue;
            if (anchor.getFluidType() != other.getFluidType()) continue;
            Vec3 anchorPoint = anchor.getConnectionPoint();
            Vec3 connectionPoint = other.getConnectionPoint();
            if (!isDominant(anchorPoint, connectionPoint)) continue;
            state.pipes.add(new double[]{connectionPoint.x - anchorPoint.x, connectionPoint.y - anchorPoint.y,
                    connectionPoint.z - anchorPoint.z});
            state.colors.add(0xFF000000 | lighten(anchor.getFluidType().getColor(), 0.25D));
        }
    }

    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.pipes.isEmpty()) return;
        int light = state.lightCoords;
        for (int i = 0; i < state.pipes.size(); i++) {
            double[] d = state.pipes.get(i);
            int color = state.colors.get(i);
            double hyp = Math.sqrt(d[0] * d[0] + d[2] * d[2]);
            double yaw = Math.toDegrees(Math.atan2(d[0], d[2]));
            double pitch = Math.toDegrees(Math.atan2(d[1], hyp));
            double length = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);

            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (90 - pitch)));

            poseStack.pushPose();
            poseStack.scale(1F, (float) length, 1F);
            poseStack.translate(0F, -0.5F, 0F);
            sink.submitCustomGeometry(poseStack, CompatRenderTypes.entityCutout(TEX),
                    (pose, buffer) -> model.renderPart("Pipe", pose, buffer, light, OverlayTexture.NO_OVERLAY, color));
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0F, (float) (length / 2D - 1.5D), 0F);
            sink.submitCustomGeometry(poseStack, CompatRenderTypes.entityCutout(TEX),
                    (pose, buffer) -> model.renderPart("Ring", pose, buffer, light, OverlayTexture.NO_OVERLAY));
            poseStack.popPose();

            poseStack.popPose();
        }
    }

    /** 1.7: the anchor that should render the pipe (instead of two half segments). */
    public static boolean isDominant(Vec3 first, Vec3 second) {
        if (first.x < second.x) return true;
        if (first.x > second.x) return false;
        if (first.y < second.y) return true;
        if (first.y > second.y) return false;
        if (first.z < second.z) return true;
        if (first.z > second.z) return false;
        return false; // exact same pos? no need to render anything
    }

    /** 1.7 {@code ColorUtil.lightenColor}. */
    private static int lighten(int hex, double percent) {
        int r = (hex >> 16) & 0xFF, g = (hex >> 8) & 0xFF, b = hex & 0xFF;
        r = (int) (r + (255 - r) * percent);
        g = (int) (g + (255 - g) * percent);
        b = (int) (b + (255 - b) * percent);
        return (r << 16) | (g << 8) | b;
    }
}
