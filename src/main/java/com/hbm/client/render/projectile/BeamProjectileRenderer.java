package com.hbm.client.render.projectile;

import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.packet.ProjectileStatePayload;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Renders the old beam entity along its measured hitscan length. */
public final class BeamProjectileRenderer extends GemEntityRenderer<EntityBulletBeamBase, BeamProjectileRenderer.State> {

    public BeamProjectileRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override protected void setup(Gem.Model<State> declaration) { declaration.offScreen(); }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(EntityBulletBeamBase entity, State state, float partialTicks) {
        // The custom payload can arrive before or after the vanilla entity spawn. Resolve the pending
        // copy here as well as in tick so the first render after spawn does not lose the beam.
        ProjectileStatePayload.applyPending(entity);
        state.origin = entity.position();
        state.heading = new Vec3(entity.headingX, entity.headingY, entity.headingZ);
        if (entity.hasRenderState()) {
            state.endpoint = new Vec3(entity.beamEndX, entity.beamEndY, entity.beamEndZ);
            state.length = entity.beamLength;
            state.direction = state.endpoint.subtract(state.origin);
            if (state.direction.lengthSqr() <= 1.0E-12D && state.heading.lengthSqr() > 1.0E-12D)
                state.direction = state.heading.normalize().scale(state.length);
        } else {
            // Entity spawning and the state payload are separate network streams. Keep one visible,
            // correctly directed frame instead of constructing a zero-length/zero-direction mesh;
            // the measured endpoint replaces this fallback as soon as the payload is applied.
            state.direction = entity.getViewVector(1F).scale(250D);
            state.endpoint = state.origin.add(state.direction);
            state.length = state.direction.length();
        }
        if (entity.config == null) entity.config = entity.getBulletConfig();
        state.config = entity.config;
        state.yaw = entity.getYRot();
        state.pitch = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.partialTicks = partialTicks;
    }

    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        if (state.config == null) return;
        state.cameraOrientation = camera.orientation;
        TracerRegistry.BeamTracer tracer = TracerRegistry.beam(state.config.id);
        if (tracer != null) tracer.draw(state, poseStack, collector);
    }

    public static final class State extends CompatEntityRenderState {
        public Vec3 heading = Vec3.ZERO;
        /** Authoritative spawn point and endpoint, not a yaw/pitch reconstruction. */
        public Vec3 origin = Vec3.ZERO;
        public Vec3 endpoint = Vec3.ZERO;
        public Vec3 direction = Vec3.ZERO;
        public double length;
        public BulletConfig config;
        public float yaw;
        public float pitch;
        public float age;
        public float partialTicks;
        public Quaternionf cameraOrientation;
    }
}
