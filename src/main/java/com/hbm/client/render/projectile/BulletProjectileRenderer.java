package com.hbm.client.render.projectile;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.mojang.math.Axis;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.joml.Quaternionf;

/**
 * Submit-API adapter for the source {@code RenderBulletMK4}; per-config geometry is client-owned by
 * {@link TracerRegistry}, not by gameplay's {@code BulletConfig}.
 */
public final class BulletProjectileRenderer extends GemEntityRenderer<EntityBulletBaseMK4, BulletProjectileRenderer.State> {

    public BulletProjectileRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen();
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(EntityBulletBaseMK4 entity, State state, float partialTicks) {
        if (entity.config == null) entity.config = entity.getBulletConfig();
        state.config = entity.config;
        state.length = entity.prevVelocity + (entity.velocity - entity.prevVelocity) * partialTicks;
        state.yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
        state.pitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;
        state.age = entity.tickCount + partialTicks;
        TracerRegistry.BulletTracer tracer = TracerRegistry.bullet(state.config == null ? null : state.config.id);
        if (tracer != null) tracer.extract(entity, state, partialTicks);
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.config == null) return;
        state.cameraOrientation = camera.orientation;
        if (state.config.renderRotations) {
            pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
            pose.mulPose(Axis.ZP.rotationDegrees(state.pitch + 180F));
        }
        TracerRegistry.BulletTracer tracer = TracerRegistry.bullet(state.config.id);
        if (tracer != null) tracer.draw(state, pose, sink);
    }

    public static final class State extends CompatEntityRenderState {
        public BulletConfig config;
        public double length;
        public float yaw;
        public float pitch;
        public float age;
        public Quaternionf cameraOrientation;
        public boolean wire;
        public double wireX, wireY, wireZ;
        public int[] wireLight;
        /** The tracer streak's half-width across the view and its length along the flight, both world axes. */
        public float sideX, sideY, sideZ, alongX, alongY, alongZ;
    }
}
