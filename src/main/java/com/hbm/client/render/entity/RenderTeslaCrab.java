package com.hbm.client.render.entity;

import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.BeamPronter;
import com.hbm.client.render.BeamPronter.EnumBeamType;
import com.hbm.client.render.BeamPronter.EnumWaveType;
import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 RenderTeslaCrab and RenderTaintCrab: an OBJ shell whose two leg banks counter-swing with the walk cycle, plus
 * the coil arcs from {@code TeslaArcPayload}. 1.7 set {@code ignoreFrustumCheck} on the entity because the arcs reach
 * well outside its hitbox; the modern renderer decides culling itself, so the equivalent is {@link #culled}.
 */
public class RenderTeslaCrab<T extends Mob> extends CompatEntityRenderer<T, RenderTeslaCrab.State> {

    private final WavefrontObj model;
    private final RenderType type;
    private final String body;
    private final String legsLeft;
    private final String legsRight;
    private final float baseYaw;
    private final double arcHeight;

    protected RenderTeslaCrab(EntityRendererProvider.Context context, WavefrontObj model, Identifier texture,
                              String left, String right, float baseYaw, double arcHeight) {
        super(context);
        this.model = model;
        this.type = CompatRenderTypes.entityCutout(texture);
        this.body = "Body";
        this.legsLeft = left;
        this.legsRight = right;
        this.baseYaw = baseYaw;
        this.arcHeight = arcHeight;
        this.shadowStrength = 0F;
    }

    public static RenderTeslaCrab<EntityTeslaCrab> tesla(EntityRendererProvider.Context context) {
        return new RenderTeslaCrab<>(context, NtmEntityModels.TESLA_CRAB, ResourceManager.teslacrab_tex,
                "Front", "Back", 0F, 1D);
    }

    public static RenderTeslaCrab<EntityTaintCrab> taint(EntityRendererProvider.Context context) {
        return new RenderTeslaCrab<>(context, NtmEntityModels.TAINT_CRAB, ResourceManager.taintcrab_tex,
                "Legs1", "Legs2", 90F, 1.25D);
    }

    private static List<Vec3> arcsOf(Mob entity) {
        if (entity instanceof EntityTeslaCrab crab) return crab.targets;
        if (entity instanceof EntityTaintCrab crab) return crab.targets;
        return List.of();
    }

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
        state.bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.walkCycle = entity.walkAnimation.position(partialTicks);
        state.walkSpeed = entity.walkAnimation.speed(partialTicks);
        state.beams.clear();
        for (Vec3 target : arcsOf(entity)) {
            state.beams.add(target.subtract(entity.getX(), entity.getY() + arcHeight, entity.getZ()));
        }
        state.start = (int) entity.level().getGameTime() % 1000 + 1;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        float swing = -(Mth.cos(state.walkCycle * 0.6662F * 2F) * 0.4F) * state.walkSpeed * 57.3F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - state.bodyYaw));
        poseStack.mulPose(Axis.YP.rotationDegrees(baseYaw));
        part(poseStack, sink, light, body);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(swing));
        part(poseStack, sink, light, legsLeft);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(swing));
        part(poseStack, sink, light, legsRight);
        poseStack.popPose();
        poseStack.popPose();

        if (!state.beams.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0D, arcHeight, 0D);
            for (Vec3 skeleton : state.beams) {
                BeamPronter.prontBeam(poseStack, sink, skeleton, EnumWaveType.RANDOM, EnumBeamType.SOLID,
                        state.start, (int) (skeleton.length() * 5D), 0.125F, 0x404040, 0x404040, 2, 0.03125F);
            }
            poseStack.popPose();
        }
    }

    private void part(PoseStack poseStack, SubmitSink sink, int light, String part) {
        sink.submitCustomGeometry(poseStack, type,
                (pose, buffer) -> model.renderPart(part, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    public static final class State extends CompatEntityRenderState {
        public final List<Vec3> beams = new ArrayList<>();
        float bodyYaw;
        float walkCycle;
        float walkSpeed;
        int start;
    }
}
