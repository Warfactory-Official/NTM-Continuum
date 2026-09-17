package com.hbm.client.render.entity;

import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.handler.MissileStruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 render/entity/rocket/RenderMissileCustom: the entity's own assembled parts under the missile flight chain.
 * 1.7 omits here the launch pad facing roll that RenderMissileGeneric applies to every other EntityMissileBaseNT,
 * and applies no tier scale. The missile's pitch is its own synchronized value - see {@code MissileRenderer}.
 */
public class RenderMissileCustom extends CompatEntityRenderer<EntityMissileCustom, RenderMissileCustom.State> {

    private final MissilePronter pronter = new MissilePronter();

    public RenderMissileCustom(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public boolean culled(EntityMissileCustom entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityMissileCustom entity, State state, float partialTicks) {
        state.yaw = entity.getViewYRot(partialTicks);
        state.pitch = entity.getMissilePitch(partialTicks);
        state.parts = entity.getStruct();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.parts == null) return;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        poseStack.mulPose(Axis.YN.rotationDegrees(state.yaw - 90F));
        pronter.pront(state.parts, poseStack, sink, state.lightCoords);
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public float yaw;
        public float pitch;
        public @Nullable MissileStruct parts;
    }
}
