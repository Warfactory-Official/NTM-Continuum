package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * The research reactor: the fixed base (turned 180 like the old TESR), the control-rod bundle riding
 * the interpolated level, and the Cherenkov glow shells drawn while it runs submerged.
 */
public final class RenderSmallReactor extends GemBER<TileEntityReactorResearch, RenderSmallReactor.State> {

    private final WavefrontObj base = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/reactors/reactor_small_base.obj"));
    private final WavefrontObj rods = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/reactors/reactor_small_rods.obj"));
    private final Surface baseSurface = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/reactors/reactor_small_base.png");
    private final Surface rodSurface = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/reactors/reactor_small_rods.png");

    public static final class State extends CompatRenderState {
        public float level;
        public int totalFlux;
        public boolean submerged;
        public float random;
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 180F);
        machine.child(base.all(), baseSurface);
        machine.child(rods.all(), rodSurface).slide(Gem.Axes.Y, state -> state.level);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityReactorResearch reactor, State state, float partialTicks, Vec3 cameraPosition) {
        state.level = (float) (reactor.lastRodLevel + (reactor.rodLevel - reactor.lastRodLevel) * partialTicks);
        state.totalFlux = reactor.totalFlux;
        state.submerged = reactor.isSubmerged();
        state.random = (float) Math.random();
    }

    /** The old additive, untextured, unlit glow cubes: alpha grows with flux, the shells step outward. */
    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.totalFlux <= 10 || !state.submerged) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        float alpha = 0.025F + state.random * 0.015F + (0.125F * state.totalFlux / 1000F);
        int color = argb(alpha, 0.4F, 0.9F, 1.0F);
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.lightning(), (pose, buffer) -> {
            for (double dd = 0.285; dd < 0.7; dd += 0.025) {
                float d = (float) dd;
                float top = 1.375F, bottom = 1.375F;
                quad(buffer, pose, d, bottom - d, -d, d, top + d, -d, d, top + d, d, d, bottom - d, d, color);
                quad(buffer, pose, -d, bottom - d, -d, -d, top + d, -d, -d, top + d, d, -d, bottom - d, d, color);
                quad(buffer, pose, -d, bottom - d, d, -d, top + d, d, d, top + d, d, d, bottom - d, d, color);
                quad(buffer, pose, -d, bottom - d, -d, -d, top + d, -d, d, top + d, -d, d, bottom - d, -d, color);
                quad(buffer, pose, -d, top + d, -d, -d, top + d, d, d, top + d, d, d, top + d, -d, color);
                quad(buffer, pose, -d, bottom - d, -d, -d, bottom - d, d, d, bottom - d, d, d, bottom - d, -d, color);
            }
        });
        poseStack.popPose();
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int color) {
        Vertexes.color(buffer, pose, x0, y0, z0, color);
        Vertexes.color(buffer, pose, x1, y1, z1, color);
        Vertexes.color(buffer, pose, x2, y2, z2, color);
        Vertexes.color(buffer, pose, x3, y3, z3, color);
    }

    private static int argb(float alpha, float red, float green, float blue) {
        return ((int) (alpha * 255F) & 0xFF) << 24 | ((int) (red * 255F) & 0xFF) << 16
                | ((int) (green * 255F) & 0xFF) << 8 | ((int) (blue * 255F) & 0xFF);
    }
}
