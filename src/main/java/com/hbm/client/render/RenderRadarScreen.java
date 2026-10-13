package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.api.entity.RadarEntry;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.gui.GUIMachineRadarNT;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineRadarScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code RenderRadarScreen}: the 2x2 panel turned to its facing, and on its face (the plane x=0.38) either
 * the linked radar's picture - a translucent green sweep line falling down the glass every 56 ticks and the
 * blips from the radar map texture - or, unlinked, a random 40-pixel strip of the map's static.
 */
public final class RenderRadarScreen extends GemBER<TileEntityMachineRadarScreen, RenderRadarScreen.State> {

    public static final class State extends CompatRenderState {
        public float yaw;
        public boolean linked;
        public double sweep;
        public int noise;
        public final List<float[]> blips = new ArrayList<>();
    }

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/radar_screen.obj"));
    private final Surface skin = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/machines/radar_screen.png");
    private final RandomSource rand = RandomSource.create();

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .child(model.all(), skin);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityMachineRadarScreen screen, State state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = switch (MachineCore.facingOrdinal(screen.getBlockState(), 5)) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.linked = screen.linked;
        long time = screen.getLevel() == null ? 0L : screen.getLevel().getGameTime();
        state.sweep = ((time % 56) + partialTicks) / 30D;
        state.noise = 118 + rand.nextInt(81);
        state.blips.clear();
        for (RadarEntry entry : screen.entries) {
            float sX = (float) ((entry.posX - screen.refX) / ((double) screen.range + 1) * 0.875D);
            float sZ = (float) ((entry.posZ - screen.refZ) / ((double) screen.range + 1) * 0.875D);
            state.blips.add(new float[]{sX, sZ, entry.blipLevel});
        }
    }

    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));

        int light = state.lightCoords;
        if (state.linked) {
            float o = (float) state.sweep;
            // glDepthMask(false), blend, no texture: a smooth green fade, clear at the top edge
            sink.submitCustomGeometry(poseStack, CompatRenderTypes.weaponSmoke(), (pose, buffer) -> {
                Vertexes.color(buffer, pose, 0.38F, 2F - o, 1.375F, 0x0000FF00);
                Vertexes.color(buffer, pose, 0.38F, 2F - o, -0.375F, 0x0000FF00);
                Vertexes.color(buffer, pose, 0.38F, 2F - o - 0.125F, -0.375F, 0x3200FF00);
                Vertexes.color(buffer, pose, 0.38F, 2F - o - 0.125F, 1.375F, 0x3200FF00);
            });

            if (!state.blips.isEmpty()) {
                List<float[]> blips = List.copyOf(state.blips);
                sink.submitCustomGeometry(poseStack, CompatRenderTypes.flatCutout(GUIMachineRadarNT.TEXTURE), (pose, buffer) -> {
                    float size = 0.0625F;
                    for (float[] b : blips) {
                        float sX = b[0], sZ = b[1];
                        float v0 = b[2] * 8F / 256F, v1 = (b[2] * 8F + 8F) / 256F;
                        vertex(buffer, pose, 0.38F, 1 - sZ + size, 0.5F - sX + size, 216F / 256F, v1, light);
                        vertex(buffer, pose, 0.38F, 1 - sZ + size, 0.5F - sX - size, 224F / 256F, v1, light);
                        vertex(buffer, pose, 0.38F, 1 - sZ - size, 0.5F - sX - size, 224F / 256F, v0, light);
                        vertex(buffer, pose, 0.38F, 1 - sZ - size, 0.5F - sX + size, 216F / 256F, v0, light);
                    }
                });
            }
        } else {
            float offset = state.noise;
            sink.submitCustomGeometry(poseStack, CompatRenderTypes.flatCutout(GUIMachineRadarNT.TEXTURE), (pose, buffer) -> {
                vertex(buffer, pose, 0.38F, 1.875F, 1.375F, 216F / 256F, (offset + 40F) / 256F, light);
                vertex(buffer, pose, 0.38F, 1.875F, -0.375F, 256F / 256F, (offset + 40F) / 256F, light);
                vertex(buffer, pose, 0.38F, 0.125F, -0.375F, 256F / 256F, offset / 256F, light);
                vertex(buffer, pose, 0.38F, 0.125F, 1.375F, 216F / 256F, offset / 256F, light);
            });
        }

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        Vertexes.vertex(buffer, pose, x, y, z, 0xFFFFFFFF, u, v, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
    }
}
