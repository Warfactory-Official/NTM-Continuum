package com.hbm.client.render;

import com.hbm.blocks.generic.BlockWandStructure;
import com.hbm.blocks.generic.BlockWandStructure.TileEntityWandStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderWandStructure}: for a saving block (metadata 0) the box it saves - from one above the block to
 * {@code sizeX/Y/Z} further - as twelve white full-bright lines, texture and lighting off; nothing for a loading
 * block. 1.7 gave the tile an infinite render box and a 256-block render distance.
 */
public final class RenderWandStructure implements CompatBER<TileEntityWandStructure, RenderWandStructure.State> {

    public static final class State extends CompatRenderState {
        boolean save;
        int sizeX, sizeY, sizeZ;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int viewDistance() {
        return 256;
    }

    @Override
    public void extract(TileEntityWandStructure tile, State state, float partialTicks, Vec3 cameraPosition) {
        state.save = tile.getBlockState().getOptionalValue(BlockWandStructure.MODE).orElse(BlockWandStructure.Mode.SAVE)
                == BlockWandStructure.Mode.SAVE;
        state.sizeX = tile.sizeX;
        state.sizeY = tile.sizeY;
        state.sizeZ = tile.sizeZ;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (!state.save) return;

        float x1 = 0, y1 = 1, z1 = 0;
        float x2 = state.sizeX, y2 = state.sizeY + 1, z2 = state.sizeZ;
        int white = 0xFFFFFFFF;

        sink.submitCustomGeometry(poseStack, CompatRenderTypes.lines(), (pose, buffer) -> {
            // top
            line(buffer, pose, x1, y2, z1, x1, y2, z2, white);
            line(buffer, pose, x1, y2, z2, x2, y2, z2, white);
            line(buffer, pose, x2, y2, z2, x2, y2, z1, white);
            line(buffer, pose, x2, y2, z1, x1, y2, z1, white);
            // bottom
            line(buffer, pose, x1, y1, z1, x1, y1, z2, white);
            line(buffer, pose, x1, y1, z2, x2, y1, z2, white);
            line(buffer, pose, x2, y1, z2, x2, y1, z1, white);
            line(buffer, pose, x2, y1, z1, x1, y1, z1, white);
            // sides
            line(buffer, pose, x1, y1, z1, x1, y2, z1, white);
            line(buffer, pose, x2, y1, z1, x2, y2, z1, white);
            line(buffer, pose, x2, y1, z2, x2, y2, z2, white);
            line(buffer, pose, x1, y1, z2, x1, y2, z2, white);
        });
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose, float x0, float y0, float z0,
                             float x1, float y1, float z1, int color) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6F) return;
        dx /= len; dy /= len; dz /= len;
        Vertexes.line(buffer, pose, x0, y0, z0, color, dx, dy, dz);
        Vertexes.line(buffer, pose, x1, y1, z1, color, dx, dy, dz);
    }
}
