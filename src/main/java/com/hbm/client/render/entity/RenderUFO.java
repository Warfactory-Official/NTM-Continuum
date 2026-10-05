package com.hbm.client.render.entity;

import com.hbm.client.render.BeamPronter;
import com.hbm.client.render.BeamPronter.EnumBeamType;
import com.hbm.client.render.BeamPronter.EnumWaveType;
import com.hbm.entity.mob.EntityUFO;
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
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * 1.7 {@code RenderUFO}: the saucer spins five degrees a tick regardless of what it is doing, tips over once it
 * is dead, and drops three stacked beams to the ground while the tractor beam is up.
 */
public class RenderUFO extends CompatEntityRenderer<EntityUFO, RenderUFO.State> {

    private static final RenderType TYPE = CompatRenderTypes.entityCutout(ResourceManager.ufo_tex);
    private static final double SCALE = 2D;

    public RenderUFO(EntityRendererProvider.Context context) {
        super(context);
        this.shadowStrength = 0F;
    }

    @Override
    public boolean culled(EntityUFO entity) {
        return false; // 1.7 ignoreFrustumCheck
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityUFO entity, State state, float partialTicks) {
        com.hbm.client.BossStatus.set(entity, net.minecraft.world.BossEvent.BossBarColor.PINK); // 1.7 render-pushed BossStatus
        state.spin = (float) ((entity.tickCount + partialTicks) * 5D % 360D);
        state.alive = entity.isAlive();
        state.deathTilt = entity.deathTime + 30 + partialTicks;
        state.beam = entity.getBeam();
        state.beamLength = state.beam ? entity.getY() - groundBelow(entity) : 0D;
        state.age = entity.tickCount;
    }

    /** 1.7 walked down from the saucer looking for the first non-air block; the beam stops there. */
    private static int groundBelow(EntityUFO entity) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = Mth.floor(entity.getX());
        int z = Mth.floor(entity.getZ());

        for (int y = (int) Math.ceil(entity.getY()); y >= entity.level().getMinY(); y--) {
            if (!entity.level().getBlockState(pos.set(x, y, z)).isAir()) return y;
        }

        return entity.level().getMinY();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0F, 1F, 0F);

        if (!state.alive) poseStack.mulPose(new Quaternionf().rotateAxis(
                state.deathTilt * Mth.DEG_TO_RAD, 0.7071068F, 0F, 0.7071068F));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        poseStack.scale((float) SCALE, (float) SCALE, (float) SCALE);
        sink.submitCustomGeometry(poseStack, TYPE, (pose, buffer) ->
                NtmEntityModels.UFO.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();

        if (state.beam && state.beamLength > 0D) {
            Vec3 skeleton = new Vec3(0D, -state.beamLength, 0D);
            BeamPronter.prontBeam(poseStack, sink, skeleton, EnumWaveType.SPIRAL, EnumBeamType.SOLID,
                    0, (int) (state.beamLength + 1), 0F, 0x101020, 0x101020, 6, (float) SCALE * 0.75F);
            BeamPronter.prontBeam(poseStack, sink, skeleton, EnumWaveType.RANDOM, EnumBeamType.SOLID,
                    state.age / 2, (int) (state.beamLength / 2 + 1), (float) SCALE * 1.5F, 0x202060, 0x202060, 2,
                    0.0625F);
            BeamPronter.prontBeam(poseStack, sink, skeleton, EnumWaveType.RANDOM, EnumBeamType.SOLID,
                    state.age / 4, (int) (state.beamLength / 2 + 1), (float) SCALE * 1.5F, 0x202060, 0x202060, 2,
                    0.0625F);
        }

        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float spin;
        boolean alive;
        float deathTilt;
        boolean beam;
        double beamLength;
        int age;
    }
}
