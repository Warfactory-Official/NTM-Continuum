package com.hbm.client.render.entity;

import com.hbm.client.model.WavefrontObj;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;

/**
 * 1.7 RenderBoxcar: the one renderer for all four equestrian falling props, branched per type (the 1.7 branch as the Kind).
 */
public class RenderBoxcar<T extends Entity> extends CompatEntityRenderer<T, RenderBoxcar.State> {

    private final Kind kind;
    private final RenderType body;
    private final WavefrontObj model;

    public RenderBoxcar(EntityRendererProvider.Context context, Kind kind) {
        super(context);
        this.shadowRadius = 0F;
        this.kind = kind;
        // 1.7: the BUILDING arm alone disables culling
        this.body = switch (kind) {
            case BOXCAR -> CompatRenderTypes.entityCutoutCull(ResourceManager.boxcar_tex);
            case DUCHESS_GAMBIT -> CompatRenderTypes.entityCutoutCull(ResourceManager.duchessgambit_tex);
            case BUILDING -> CompatRenderTypes.entityCutoutNoCull(ResourceManager.building_tex);
            case TORPEDO -> CompatRenderTypes.entityCutoutCull(ResourceManager.torpedo_tex);
        };
        this.model = switch (kind) {
            case BOXCAR -> NtmEntityModels.BOXCAR;
            case DUCHESS_GAMBIT -> NtmEntityModels.DUCHESS_GAMBIT;
            case BUILDING -> NtmEntityModels.BUILDING;
            case TORPEDO -> NtmEntityModels.TORPEDO;
        };
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(T entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T entity, State state, float partialTicks) {
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        pose.pushPose();

        switch (this.kind) {
            case BOXCAR -> {
                pose.translate(0, 0, -1.5F);
                pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.mulPose(Axis.XP.rotationDegrees(90));
            }
            case DUCHESS_GAMBIT -> pose.translate(0, 0, -1.0F);
            case BUILDING -> {
            }
            case TORPEDO -> pose.mulPose(Axis.XP.rotationDegrees(Math.min(85, state.age * 3)));
        }

        sink.submitCustomGeometry(pose, body, (p, buf) -> model.renderAll(p, buf, light, OverlayTexture.NO_OVERLAY));

        pose.popPose();
    }

    public enum Kind {BOXCAR, DUCHESS_GAMBIT, BUILDING, TORPEDO}

    public static final class State extends CompatEntityRenderState {
        public float age;
    }
}
