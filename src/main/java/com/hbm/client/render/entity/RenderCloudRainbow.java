package com.hbm.client.render.entity;

import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;

/**
 * 1.7 RenderCloudRainbow: six concentric spheres, each re-coloured at random every frame so the blast strobes;
 * the mesh is models/Sphere.obj, the same OBJ the black hole draws.
 */
public class RenderCloudRainbow extends CompatEntityRenderer<EntityCloudFleijaRainbow, RenderCloudRainbow.State> {

    // renderer-local RandomSource - the level's is off-thread
    private final RandomSource random = RandomSource.create();

    public RenderCloudRainbow(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    // 1.7: frustum skipped, distance kept
    @Override
    public boolean culled(EntityCloudFleijaRainbow entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityCloudFleijaRainbow entity, State state, float partialTicks) {
        state.age = entity.age; // 1.7: raw tick count - no interpolation
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.scale(state.age, state.age, state.age);

        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        submitSphere(poseStack, sink, CloudRenderTypes.OPAQUE);
        poseStack.popPose();

        for (float i = 0.6F; i <= 1F; i += 0.1F) {
            poseStack.pushPose();
            poseStack.scale(i, i, i);
            submitSphere(poseStack, sink, CloudRenderTypes.ADDITIVE);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private void submitSphere(PoseStack poseStack, SubmitSink sink, RenderType type) {
        // glColor3ub(rand x3) - unsigned bytes, alpha 1
        int color = 0xFF000000 | random.nextInt(0x100) << 16 | random.nextInt(0x100) << 8 | random.nextInt(0x100);
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) -> NtmEntityModels.SPHERE.renderAll(
                pose, buffer, CloudRenderTypes.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color));
    }

    public static final class State extends CompatEntityRenderState {
        float age;
    }
}
