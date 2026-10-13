package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.effect.EntitySpear;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.LightningRenderType;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.Random;

/** The 1.12.2 digamma spear: full-bright lance blade, settling ghost and stable 64-spike flash. */
public final class RenderSpear extends GemEntityRenderer<EntitySpear, RenderSpear.State> {
    private static final WavefrontObj LANCE = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/weapons/lance.obj"));
    private static final Identifier LANCE_TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/weapons/lance.png");
    private static final Identifier WHITE_TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/misc/white.png");

    public RenderSpear(EntityRendererProvider.Context context) { super(context); }

    /** The lance, and the ghost of it that fades in while it stands in the ground. */
    @Override protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> lance = declaration.offScreen()
                .joint()
                .translate(0F, 15F, 0F)
                .rotate(Gem.Axes.X, 180F)
                .scale(2F);
        lance.child(LANCE.part("Spear"), Surface.cutout(LANCE_TEXTURE.toString()))
                .light(state -> 0xF000F0);
        lance.child(LANCE.part("Spear"), Surface.translucent(WHITE_TEXTURE.toString()))
                .visibleIf(state -> state.ticks > 0F)
                .light(state -> 0xF000F0)
                .tint(state -> argb(Math.min(state.ticks / 100F, 1F), 1F, 1F, 1F));
    }

    @Override public State createRenderState() { return new State(); }
    @Override public void extract(EntitySpear spear, State state, float partial) {
        state.ticks = spear.ticksInGround > 0 ? spear.ticksInGround + partial : 0F;
    }
    /** The charge spikes are 64 randomly oriented triangles rebuilt every frame. */
    @Override protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0D, 15D, 0D);
        pose.mulPose(Axis.XP.rotationDegrees(180F));
        pose.scale(2F, 2F, 2F);
        if (state.ticks > 0F) {
            float intensity = state.ticks / 200F, squared = intensity * intensity;
            int tip = argb(Math.min(1F, squared * 2F), 1F, .6F, .6F);
            int edge = argb(0F, 1F, .6F, .6F);
            pose.pushPose(); pose.scale(.2F, .2F, .2F);
            Random random = new Random(432L);
            for (int i = 0; i < 64; i++) {
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360F));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360F));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360F));
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360F));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360F));
                float v1 = (random.nextFloat() * 30F + 15F) * (squared * 25F);
                float v2 = (random.nextFloat() * 4F + 3F) * (squared * 25F);
                sink.submitCustomGeometry(pose, LightningRenderType.get(),
                        (last, buffer) -> spike(last, buffer, tip, edge, v1, v2));
            }
            pose.popPose();
        }
        pose.popPose();
    }
    private static void spike(PoseStack.Pose pose, VertexConsumer buffer, int tip, int edge, float v1, float v2) {
        triangle(pose, buffer, tip, edge, 0, 0, 0, -.866F * v2, v1, -.5F * v2, .866F * v2, v1, -.5F * v2);
        triangle(pose, buffer, tip, edge, 0, 0, 0, .866F * v2, v1, -.5F * v2, 0, v1, v2);
        triangle(pose, buffer, tip, edge, 0, 0, 0, 0, v1, v2, -.866F * v2, v1, -.5F * v2);
    }
    private static void triangle(PoseStack.Pose pose, VertexConsumer buffer, int tip, int edge,
                                 float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2) {
        Vertexes.color(buffer, pose, x0, y0, z0, tip); Vertexes.color(buffer, pose, x1, y1, z1, edge);
        Vertexes.color(buffer, pose, x2, y2, z2, edge); Vertexes.color(buffer, pose, x2, y2, z2, edge);
    }
    private static int argb(float a, float r, float g, float b) {
        return (Math.round(a * 255F) << 24) | (Math.round(r * 255F) << 16) | (Math.round(g * 255F) << 8) | Math.round(b * 255F);
    }
    public static final class State extends CompatEntityRenderState { float ticks; }
}
