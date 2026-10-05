package com.hbm.client.render.entity;

import com.hbm.client.render.BeamPronter;
import com.hbm.client.render.BeamPronter.EnumBeamType;
import com.hbm.client.render.BeamPronter.EnumWaveType;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.ModelParts;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.living.CompatLivingRenderState;
import dev.golem.core.render.living.CompatMobRenderer;
import dev.golem.core.render.living.LivingModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 RenderRADBeast: blaze geometry with an NTM skin, the {@link ModelM65Blaze} gas mask from its render pass 0
 * ({@code textures/models/ModelM65Blaze.png}, here {@code textures/models/armor/m65_blaze.png}) and a green drain
 * beam to whatever it is feeding on.
 */
public class RenderRADBeast extends CompatMobRenderer<EntityRADBeast, RenderRADBeast.State> {

    private static final int COLOR = 0x004000;
    private static final double ORIGIN = 1.25D;
    private static final Identifier MASK = Ids.of("hbm", "textures/models/armor/m65_blaze.png");
    /** {@code ModelM65Blaze.render}: {@code 1/16 * 18}, then {@code 1.01} against z-fighting with the head. */
    private static final float MASK_SCALE = 18F / 16F * 1.01F;

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
        submitMask(state, poseStack, sink);
        if (state.beam == null) return;
        poseStack.pushPose();
        poseStack.translate(0D, ORIGIN, 0D);
        BeamPronter.prontBeam(poseStack, sink, state.beam, EnumWaveType.RANDOM, EnumBeamType.SOLID,
                state.start, (int) (state.beam.length() * 5D), 0.125F, COLOR, COLOR, 2, 0.03125F);
        poseStack.popPose();
    }

    /**
     * 1.7 render pass 0: {@code ModelM65Blaze} drawn in the body's model space with the body's head angles. The
     * living renderer's own frame is restated here (yaw, death flip, the -1/-1/1 flip and the -1.501 lift) because
     * {@link #submitExtra} runs at the entity origin. {@code ModelBiped.setRotationAngles} gives the head pivot 0,0
     * and yaw/pitch straight from the head angles; the mask copies those.
     */
    private void submitMask(State state, PoseStack poseStack, SubmitSink sink) {
        if (state.isInvisible) return;
        poseStack.pushPose();
        poseStack.scale(state.scale, state.scale, state.scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - state.bodyRot));
        if (state.deathTime > 0F) {
            float flip = (float) Math.sqrt((state.deathTime - 1F) / 20F * 1.6F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(Math.min(flip, 1F) * flipDegrees()));
        }
        poseStack.scale(-1F, -1F, 1F);
        poseStack.translate(0F, -1.501F, 0F);
        poseStack.scale(MASK_SCALE, MASK_SCALE, MASK_SCALE);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        int overlay = OverlayTexture.pack(OverlayTexture.u(0F), OverlayTexture.v(state.hasRedOverlay));
        ModelParts.submit(sink, poseStack, CompatRenderTypes.entityCutoutNoCull(MASK), ModelM65Blaze.MASK,
                state.lightCoords, overlay);
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
