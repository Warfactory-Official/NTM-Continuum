package com.hbm.client.render.entity;

import com.hbm.entity.missile.EntityRocketSoyuz;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

/** 1.7 RenderSoyuz: the launch vehicle standing on its engines, skinned by the variant, with a flare under each nozzle. */
public class RenderSoyuz extends CompatEntityRenderer<EntityRocketSoyuz, RenderSoyuz.State> {

    private static final double[][] NOZZLES = {{0, 0}, {2.5, 0}, {-2.5, 0}, {0, 2.5}, {0, -2.5}};

    public RenderSoyuz(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public boolean culled(EntityRocketSoyuz entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityRocketSoyuz entity, State state, float partialTicks) {
        state.skin = SoyuzMesh.wrapSkin(entity.getSkin());
        state.position = entity.getPosition(partialTicks);
        state.flare = RocketFlare.scale(entity.tickCount + partialTicks, entity.getRandom().nextDouble());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        SoyuzMesh.submit(poseStack, sink, state.lightCoords, state.skin);

        poseStack.pushPose();
        RocketFlare.towardCamera(poseStack, state.position, camera, 1.5D);
        for (double[] nozzle : NOZZLES) {
            poseStack.pushPose();
            poseStack.translate(nozzle[0], 0, nozzle[1]);
            poseStack.scale(1.5F, 1.5F, 1.5F);
            RocketFlare.submit(poseStack, sink, camera, state.flare, 1F, 0.75F, 0.5F);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        int skin;
        Vec3 position = Vec3.ZERO;
        float flare;
    }
}
