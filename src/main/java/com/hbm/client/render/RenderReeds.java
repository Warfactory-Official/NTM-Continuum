package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockReeds;
import com.hbm.config.ClientConfig;
import com.hbm.tileentity.deco.TileEntityReeds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderReeds}, below the water line. The old renderer measured the column under the reed each
 * frame ({@code depth} = water blocks + 1, stopping at the first non-water block) and drew
 * {@code drawCrossedSquares} once per block: {@code reeds_top} at the reed itself, {@code reeds_bottom} in the
 * lowest water block and {@code reeds_mid} in between, each lit by the block it stands in and coloured
 * {@code colorMultiplier * 0.75} (white, so 0.75 grey). With {@code RENDER_REEDS} off only the top is drawn.
 *
 * <p>The top segment stays in the chunk-rendered block model (tinted the same 0.75 grey, see
 * {@code TerrainClientRegistrations}) so a reed is still visible past block-entity render distance; this
 * renderer adds segments 1..depth-1. Terrain in 1.7 had no directional shade on crossed squares, hence
 * {@link CompatRenderTypes#flatCutout} (vertex colour times lightmap, both windings drawn as 1.7 did).
 */
public final class RenderReeds extends GemBER<TileEntityReeds, RenderReeds.State> {

    private static final Identifier MID = Ids.of(Constants.MOD_ID, "textures/blocks/reeds_mid.png");
    private static final Identifier BOTTOM = Ids.of(Constants.MOD_ID, "textures/blocks/reeds_bottom.png");
    /** 1.7 {@code setColorOpaque_F(r * 0.75, g * 0.75, b * 0.75)} with the default white multiplier. */
    public static final int COLOR = 0xFFBFBFBF;

    @Override
    protected void setup(Gem.Model<State> model) {
        // Nothing declared: every segment is built from the measured column in submitUndeclared.
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityReeds be, State state, float partialTicks, Vec3 cameraPosition) {
        Level level = be.getLevel();
        state.depth = 1;
        if (level == null || !ClientConfig.RENDER_REEDS) return;
        BlockPos pos = be.getBlockPos();
        int depth = BlockReeds.columnDepth(level, pos);
        if (state.light.length < depth) state.light = new int[Math.max(depth, state.light.length * 2)];
        for (int i = 1; i < depth; i++) state.light[i] = Renderers.lightAt(level, pos.below(i));
        state.depth = depth;
    }

    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int depth = state.depth;
        if (depth <= 1) return;
        if (depth > 2) {
            sink.submitCustomGeometry(poseStack, CompatRenderTypes.flatCutout(MID), (pose, buffer) -> {
                for (int i = 1; i < depth - 1; i++) crossedSquares(pose, buffer, -i, state.light[i]);
            });
        }
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.flatCutout(BOTTOM),
                (pose, buffer) -> crossedSquares(pose, buffer, -(depth - 1), state.light[depth - 1]));
    }

    /** 1.7 {@code RenderBlocks.drawCrossedSquares} at scale 1: two diagonal sheets, each drawn front and back. */
    private static void crossedSquares(PoseStack.Pose pose, VertexConsumer buffer, float y, int light) {
        float d = 0.45F;
        float x0 = 0.5F - d, x1 = 0.5F + d, z0 = 0.5F - d, z1 = 0.5F + d, y1 = y + 1F;
        vertex(buffer, pose, x0, y1, z0, 0, 0, light);
        vertex(buffer, pose, x0, y, z0, 0, 1, light);
        vertex(buffer, pose, x1, y, z1, 1, 1, light);
        vertex(buffer, pose, x1, y1, z1, 1, 0, light);
        vertex(buffer, pose, x1, y1, z1, 0, 0, light);
        vertex(buffer, pose, x1, y, z1, 0, 1, light);
        vertex(buffer, pose, x0, y, z0, 1, 1, light);
        vertex(buffer, pose, x0, y1, z0, 1, 0, light);
        vertex(buffer, pose, x0, y1, z1, 0, 0, light);
        vertex(buffer, pose, x0, y, z1, 0, 1, light);
        vertex(buffer, pose, x1, y, z0, 1, 1, light);
        vertex(buffer, pose, x1, y1, z0, 1, 0, light);
        vertex(buffer, pose, x1, y1, z0, 0, 0, light);
        vertex(buffer, pose, x1, y, z0, 0, 1, light);
        vertex(buffer, pose, x0, y, z1, 1, 1, light);
        vertex(buffer, pose, x0, y1, z1, 1, 0, light);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        // Flat: the render type ignores the normal on 26.1, and straight up evaluates to 1.0 below it.
        Vertexes.vertex(buffer, pose, x, y, z, COLOR, u, v, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
    }

    public static final class State extends CompatRenderState {
        int depth = 1;
        int[] light = new int[8];
    }
}
