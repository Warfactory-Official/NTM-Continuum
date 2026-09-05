package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.bomb.BlockTNTBase;
import com.hbm.entity.explosive.EntityPrimedExplosive;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Block-textured primed renderer with the legacy final-ten-tick swelling and white flash. */
public class PrimedExplosiveRenderer
        extends CompatEntityRenderer<EntityPrimedExplosive, PrimedExplosiveRenderState> {

    private static final Identifier WHITE =
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/misc/white.png");
    private final Map<String, RenderType[]> textures = new HashMap<>();
    private final RenderType flash = CompatRenderTypes.entityTranslucent(WHITE);

    public PrimedExplosiveRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5F;
    }

    @Override
    public PrimedExplosiveRenderState createRenderState() {
        return new PrimedExplosiveRenderState();
    }

    @Override
    public void extract(EntityPrimedExplosive entity, PrimedExplosiveRenderState state, float partialTicks) {
        state.fuse = entity.getFuse() - partialTicks + 1.0F;
        if (entity.getExplosiveState().getBlock() instanceof BlockTNTBase explosive) {
            state.textureName = explosive.textureName();
        }
    }

    @Override
    public void submit(PrimedExplosiveRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0F, 0.5F, 0F);
        if (state.fuse < 10F) {
            float swell = Mth.clamp(1F - state.fuse / 10F, 0F, 1F);
            swell *= swell;
            swell *= swell;
            float scale = 1F + swell * 0.3F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
        poseStack.translate(-0.5F, -0.5F, 0.5F);

        RenderType[] passes = textures.computeIfAbsent(state.textureName, PrimedExplosiveRenderer::textures);
        sink.submitCustomGeometry(poseStack, passes[0],
                (pose, buffer) -> sides(pose, buffer, 0xFFFFFFFF, state.lightCoords));
        sink.submitCustomGeometry(poseStack, passes[1],
                (pose, buffer) -> top(pose, buffer, 0xFFFFFFFF, state.lightCoords));
        sink.submitCustomGeometry(poseStack, passes[2],
                (pose, buffer) -> bottom(pose, buffer, 0xFFFFFFFF, state.lightCoords));

        if (((int) state.fuse) / 5 % 2 == 0) {
            float alpha = (1F - state.fuse / 100F) * 0.8F;
            int color = ((int) (Mth.clamp(alpha, 0F, 1F) * 255F) << 24) | 0xFFFFFF;
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.scale(1.006F, 1.006F, 1.006F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            sink.order(1).submitCustomGeometry(poseStack, flash,
                    (pose, buffer) -> cube(pose, buffer, color, 0xF000F0));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static RenderType[] textures(String name) {
        return new RenderType[] {
                cutout(name + "_side"), cutout(name + "_top"), cutout(name + "_bottom")
        };
    }

    private static RenderType cutout(String name) {
        return CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/blocks/" + name + ".png"));
    }

    private static void cube(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                             int color, int light) {
        sides(pose, buffer, color, light);
        top(pose, buffer, color, light);
        bottom(pose, buffer, color, light);
    }

    private static void sides(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer b,
                              int color, int light) {
        quad(b, pose, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, color, light, 0, 0, -1);
        quad(b, pose, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, color, light, 0, 0, 1);
        quad(b, pose, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0, color, light, -1, 0, 0);
        quad(b, pose, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, color, light, 1, 0, 0);
    }

    private static void top(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer b,
                            int color, int light) {
        quad(b, pose, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, color, light, 0, 1, 0);
    }

    private static void bottom(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer b,
                               int color, int light) {
        quad(b, pose, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1, color, light, 0, -1, 0);
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer b, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             int color, int light, float nx, float ny, float nz) {
        Vertexes.vertex(b, pose, x0, y0, z0, color, 0, 1, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        Vertexes.vertex(b, pose, x1, y1, z1, color, 0, 0, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        Vertexes.vertex(b, pose, x2, y2, z2, color, 1, 0, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        Vertexes.vertex(b, pose, x3, y3, z3, color, 1, 1, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
    }
}
