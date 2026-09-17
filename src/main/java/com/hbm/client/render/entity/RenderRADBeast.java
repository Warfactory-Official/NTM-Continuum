package com.hbm.client.render.entity;

import com.hbm.client.render.BeamPronter;
import com.hbm.client.render.BeamPronter.EnumBeamType;
import com.hbm.client.render.BeamPronter.EnumWaveType;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.living.CompatLivingRenderState;
import dev.golem.core.render.living.CompatMobRenderer;
import dev.golem.core.render.living.LivingModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 RenderRADBeast: blaze geometry with an NTM skin and a green drain beam to whatever it is feeding on. 1.7 also
 * hung a gas-mask pass off the render-pass hook; that overlay needs its own model layer and is not ported.
 */
public class RenderRADBeast extends CompatMobRenderer<EntityRADBeast, RenderRADBeast.State> {

    private static final int COLOR = 0x004000;
    private static final double ORIGIN = 1.25D;

    public RenderRADBeast(EntityRendererProvider.Context context) {
        super(context, LivingModel.blaze(context), 0.5F);
    }

    @Override
    protected int getBlockLightLevel(EntityRADBeast entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected void extract(EntityRADBeast entity, State state, float partialTicks) {
        state.beam = null;
        Entity victim = entity.getUnfortunateSoul();
        if (victim != null && entity.getY() > 0.1D) {
            double targetY = victim.getY() + victim.getBbHeight() * 0.5D;
            // 1.7: the local player's own eye offset would put the beam through the camera
            if (victim == Minecraft.getInstance().player) targetY -= 1.5D;
            Vec3 skeleton = new Vec3(victim.getX() - entity.getX(), targetY - (entity.getY() + ORIGIN),
                    victim.getZ() - entity.getZ());
            if (skeleton.length() < 200D) state.beam = skeleton;
        }
        state.start = (int) entity.level().getGameTime() % 1000 + 1;
    }

    @Override
    protected void submitExtra(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.beam == null) return;
        poseStack.pushPose();
        poseStack.translate(0D, ORIGIN, 0D);
        BeamPronter.prontBeam(poseStack, sink, state.beam, EnumWaveType.RANDOM, EnumBeamType.SOLID,
                state.start, (int) (state.beam.length() * 5D), 0.125F, COLOR, COLOR, 2, 0.03125F);
        poseStack.popPose();
    }

    @Override
    protected Identifier texture(State state) {
        return ResourceManager.radbeast_tex;
    }

    public static final class State extends CompatLivingRenderState {
        @Nullable Vec3 beam;
        int start;
    }
}
