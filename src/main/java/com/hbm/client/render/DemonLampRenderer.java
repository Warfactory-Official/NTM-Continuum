package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.machine.DemonLamp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Poses;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 1.7 {@code RenderDemonLamp}: the lamp housing (OBJ) turned onto its mounting face, then two rings of
 * sixteen additive blue sheets fanning out from the core to 15 blocks, alpha 0.25 at the core, 0 at
 * the rim, drawn textureless with additive blending and no depth writes, which is the lightning
 * pipeline here.
 */
public class DemonLampRenderer extends GemBER<DemonLamp.TileEntityDemonLamp, DemonLampRenderer.State> {

    public static class State extends CompatRenderState {
        public Direction facing = Direction.UP;
    }

    private WavefrontObj model;

    /** The old metadata switch: translate to the centre, orient by face, drop half a block. */
    private static Matrix4f orient(Direction facing) {
        Matrix4f m = new Matrix4f().translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case DOWN -> m.rotateX((float) Math.PI);
            case UP -> { }
            case NORTH -> m.rotateX((float) Math.PI / 2).rotateZ((float) Math.PI);
            case SOUTH -> m.rotateX((float) Math.PI / 2);
            case WEST -> m.rotateX((float) Math.PI / 2).rotateZ((float) Math.PI / 2);
            case EAST -> m.rotateX((float) Math.PI / 2).rotateZ((float) Math.PI * 1.5F);
        }
        return m.translate(0F, -0.5F, 0F);
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/blocks/demon_lamp.obj"));
        declaration.offScreen().viewDistance(256);
        declaration.joint().pose(state -> orient(state.facing))
                .child(model.all(), Surface.solid(Constants.MOD_ID + ":textures/models/machines/demon_lamp.png"));
    }

    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        Poses.mulPose(poseStack, orient(state.facing));
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.additive(), (pose, buffer) -> {
            Vec3 vec = new Vec3(1, 0, 0);
            double near = 0.375D;
            double far = 15D;
            int nearColor = argb(0.25F, 0F, 0.75F, 1F);
            int farColor = argb(0F, 0F, 0.75F, 1F);
            //whereeeeeeever you are
            for (int j = 0; j < 2; j++) {
                double h = 0.5;
                double height = j == 0 ? -h : h;
                for (int i = 0; i < 16; i++) {
                    vertex(buffer, pose, vec.x * near, 0.5D + j * 0.125D, vec.z * near, nearColor);
                    vertex(buffer, pose, vec.x * far, 0.5D + j * 0.125D + height, vec.z * far, farColor);
                    vec = vec.yRot((float) (Math.PI * 2D / 16D));
                    vertex(buffer, pose, vec.x * far, 0.5D + j * 0.125D + height, vec.z * far, farColor);
                    vertex(buffer, pose, vec.x * near, 0.5D + j * 0.125D, vec.z * near, nearColor);
                }
            }
        });
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z, int color) {
        Vertexes.color(buffer, pose, (float) x, (float) y, (float) z, color);
    }

    private static int argb(float a, float r, float g, float b) {
        return ((int) (a * 255F) << 24) | ((int) (r * 255F) << 16) | ((int) (g * 255F) << 8) | (int) (b * 255F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(DemonLamp.TileEntityDemonLamp blockEntity, State state, float partialTicks, Vec3 cameraPosition) {
        state.facing = blockEntity.getBlockState().getValue(DemonLamp.FACING);
    }
}
