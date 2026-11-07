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
        // Entity spawning and the state payload are separate streams: until the payload lands, keep the source's range.
        state.length = entity.hasRenderState() ? entity.beamLength : 250D;
        if (entity.config == null) entity.config = entity.getBulletConfig();
        state.config = entity.config;
        state.yaw = entity.getYRot();
        state.pitch = entity.getXRot();
        state.age = entity.tickCount + partialTicks;
        state.entityId = entity.getId();
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
        public double length;
        public BulletConfig config;
        public float yaw;
        public float pitch;
        public float age;
        public float partialTicks;
        public int entityId;
        public Quaternionf cameraOrientation;
    }
}
