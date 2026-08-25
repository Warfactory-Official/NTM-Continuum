package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.entity.missile.IMissilePitch;
import net.minecraft.world.entity.Entity;

/**
 * Draws a missile: one OBJ body, one texture, one scale.
 *
 * <p>Upstream writes one renderer class per missile — 24 of them — because 1.12's {@code Render<T>}
 * generics would not let one class serve several entity classes. That restriction is gone, so the
 * whole family is this class configured three ways per airframe, and
 * {@code HbmEntityRenderers} supplies the model/texture/scale triple at registration.
 *
 * <p>The orientation is upstream's exactly: yaw − 90° about Y, then pitch about <b>Z</b>. The Z axis is
 * not a typo — the missile OBJs are modelled pointing along +Y, so pitch is a roll in model space.
 * Pitch runs 0° straight up, −90° horizontal, −180° straight down, and comes from the missile's own
 * synchronized field rather than {@code xRot} — see {@link EntityMissileBaseNT#PITCH}.
 */
public class MissileRenderer<T extends Entity & IMissilePitch> extends dev.golem.core.render.CompatEntityRenderer<T, MissileRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;
    private final float scale;

    public MissileRenderer(EntityRendererProvider.Context context, String model, String texture, float scale) {
        super(context);
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/missiles/" + model));
        this.renderType = CompatRenderTypes.entityCutout(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/missiles/" + texture));
        this.scale = scale;
    }

    /**
     * The body this renderer draws, for screens that need to frame a missile against exactly what will
     * appear. Not {@link MissileModels}: that is the item/pad table and its scales are not these.
     */
    public WavefrontObj model() {
        return this.model;
    }

    /** @see #model() */
    public float modelScale() {
        return this.scale;
    }

    @Override
    public MissileRenderState createRenderState() {
        return new MissileRenderState();
    }

    @Override
    public void extract(T entity, MissileRenderState state, float partialTicks) {
        state.yaw = entity.getViewYRot(partialTicks);
        // Not getViewXRot: Entity.setXRot clamps to [-90, 90], and a missile's pitch runs to -180 as
        // it noses over. EntityMissileBaseNT carries the real value itself.
        state.pitch = entity.getMissilePitch(partialTicks);
    }

    @Override
    public void submit(MissileRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        if (scale != 1.0F) poseStack.scale(scale, scale, scale);

        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderAll(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, true));
        poseStack.popPose();
    }
}
