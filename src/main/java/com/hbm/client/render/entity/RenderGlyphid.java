package com.hbm.client.render.entity;

import com.hbm.entity.mob.glyphid.EntityGlyphid;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 1.7 RenderGlyphid / ModelGlyphid: nineteen named OBJ parts posed by hand off the walk cycle, with the armour
 * plates gated by the synced armour bits and an infestation overlay pass for the infected subtype.
 */
public class RenderGlyphid<T extends EntityGlyphid> extends CompatEntityRenderer<T, RenderGlyphid.State> {

    private static final RenderType INFESTED_TYPE =
            CompatRenderTypes.entityTranslucent(ResourceManager.glyphid_infestation_tex);

    public RenderGlyphid(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T entity, State state, float partialTicks) {
        state.texture = entity.getSkin();
        state.bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.scale = entity.getGlyphidScale();
        state.armor = entity.armor();
        state.subtype = entity.subtype();
        state.walkCycle = entity.walkAnimation.position(partialTicks);
        state.swingProgress = entity.getAttackAnim(partialTicks);
        state.light = state.lightCoords;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        RenderType type = CompatRenderTypes.entityCutout(state.texture);

        pose.pushPose();
        applyBaseTransform(pose, state);
        submitLimbs(state, pose, sink, light, type, -1);
        if (state.subtype == EntityGlyphid.TYPE_INFECTED) {
            submitLimbs(state, pose, sink, light, INFESTED_TYPE, -1);
        }
        pose.popPose();
    }

    protected void applyBaseTransform(PoseStack pose, State state) {
        pose.mulPose(Axis.YP.rotationDegrees(-state.bodyYaw));
        float s = (float) state.scale;
        pose.scale(s, s, s);
    }

    protected final void submitLimbs(State state, PoseStack pose, SubmitSink sink, int light, RenderType type,
                                     int color) {
        byte armor = state.armor;
        double walkCycle = state.walkCycle;

        double cy0 = Math.sin(walkCycle % (Math.PI * 2));
        double cy1 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.5);
        double cy2 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI);
        double cy3 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.75);

        double bite = Mth.clamp(Math.sin(state.swingProgress * Math.PI * 2 - Math.PI * 0.5), 0D, 1D) * 20;
        double headTilt = Math.sin(state.swingProgress * Math.PI) * 30;

        part(pose, sink, light, type, color, "Body");
        if ((armor & (1 << 0)) > 0) part(pose, sink, light, type, color, "ArmorFront");
        if ((armor & (1 << 1)) > 0) part(pose, sink, light, type, color, "ArmorLeft");
        if ((armor & (1 << 2)) > 0) part(pose, sink, light, type, color, "ArmorRight");

        pose.pushPose();
        pose.translate(0.25F, 0.625F, 0.0625F);
        pose.mulPose(Axis.YP.rotationDegrees(10F));
        pose.mulPose(Axis.XP.rotationDegrees((float) (35 + cy1 * 20)));
        pose.translate(-0.25F, -0.625F, -0.0625F);
        part(pose, sink, light, type, color, "ArmLeftUpper");
        pose.translate(0.25F, 0.625F, 0.4375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-75 - cy1 * 20 + cy0 * 20)));
        pose.translate(-0.25F, -0.625F, -0.4375F);
        part(pose, sink, light, type, color, "ArmLeftMid");
        pose.translate(0.25F, 0.625F, 0.9375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 - cy0 * 45)));
        pose.translate(-0.25F, -0.625F, -0.9375F);
        part(pose, sink, light, type, color, "ArmLeftLower");
        if ((armor & (1 << 3)) > 0) part(pose, sink, light, type, color, "ArmLeftArmor");
        pose.popPose();

        pose.pushPose();
        pose.translate(-0.25F, 0.625F, 0.0625F);
        pose.mulPose(Axis.YP.rotationDegrees(-10F));
        pose.mulPose(Axis.XP.rotationDegrees((float) (35 + cy2 * 20)));
        pose.translate(0.25F, -0.625F, -0.0625F);
        part(pose, sink, light, type, color, "ArmRightUpper");
        pose.translate(-0.25F, 0.625F, 0.4375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-75 - cy2 * 20 + cy3 * 20)));
        pose.translate(0.25F, -0.625F, -0.4375F);
        part(pose, sink, light, type, color, "ArmRightMid");
        pose.translate(-0.25F, 0.625F, 0.9375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 - cy3 * 45)));
        pose.translate(0.25F, -0.625F, -0.9375F);
        part(pose, sink, light, type, color, "ArmRightLower");
        if ((armor & (1 << 4)) > 0) part(pose, sink, light, type, color, "ArmRightArmor");
        pose.popPose();

        pose.pushPose();
        pose.translate(0F, 0.5F, 0.25F);
        pose.mulPose(Axis.ZP.rotationDegrees((float) headTilt));
        pose.translate(0F, -0.5F, -0.25F);

        pose.pushPose();
        pose.translate(0F, 0.5F, 0.25F);
        pose.mulPose(Axis.XP.rotationDegrees((float) -bite));
        pose.translate(0F, -0.5F, -0.25F);
        part(pose, sink, light, type, color, "JawTop");
        pose.popPose();

        pose.pushPose();
        pose.translate(0F, 0.5F, 0.25F);
        pose.mulPose(Axis.YP.rotationDegrees((float) bite));
        pose.mulPose(Axis.XP.rotationDegrees((float) bite));
        pose.translate(0F, -0.5F, -0.25F);
        part(pose, sink, light, type, color, "JawLeft");
        pose.popPose();

        pose.pushPose();
        pose.translate(0F, 0.5F, 0.25F);
        pose.mulPose(Axis.YP.rotationDegrees((float) -bite));
        pose.mulPose(Axis.XP.rotationDegrees((float) bite));
        pose.translate(0F, -0.5F, -0.25F);
        part(pose, sink, light, type, color, "JawRight");
        pose.popPose();
        pose.popPose();

        double steppy = 15;
        double bend = 60;

        for (int i = 0; i < 3; i++) {
            double c0 = cy0 * (i == 1 ? -1 : 1);
            double c1 = cy1 * (i == 1 ? -1 : 1);

            pose.pushPose();
            pose.translate(0F, 0.25F, 0F);
            pose.mulPose(Axis.YP.rotationDegrees((float) (i * 30 - 15 + c0 * 7.5)));
            pose.mulPose(Axis.ZP.rotationDegrees((float) (steppy + c1 * steppy)));
            pose.translate(0F, -0.25F, 0F);
            part(pose, sink, light, type, color, "LegLeftUpper");
            pose.translate(0.5625F, 0.25F, 0F);
            pose.mulPose(Axis.ZP.rotationDegrees((float) (-bend - c1 * steppy)));
            pose.translate(-0.5625F, -0.25F, 0F);
            part(pose, sink, light, type, color, "LegLeftLower");
            pose.popPose();

            pose.pushPose();
            pose.translate(0F, 0.25F, 0F);
            pose.mulPose(Axis.YP.rotationDegrees((float) (i * 30 - 45 + c0 * 7.5)));
            pose.mulPose(Axis.ZP.rotationDegrees((float) (-steppy + c1 * steppy)));
            pose.translate(0F, -0.25F, 0F);
            part(pose, sink, light, type, color, "LegRightUpper");
            pose.translate(-0.5625F, 0.25F, 0F);
            pose.mulPose(Axis.ZP.rotationDegrees((float) (bend - c1 * steppy)));
            pose.translate(0.5625F, -0.25F, 0F);
            part(pose, sink, light, type, color, "LegRightLower");
            pose.popPose();
        }
    }

    private static void part(PoseStack pose, SubmitSink sink, int light, RenderType type, int color, String name) {
        sink.submitCustomGeometry(pose, type, (p, buf) ->
                NtmEntityModels.GLYPHID.renderPart(name, p, buf, light, OverlayTexture.NO_OVERLAY, color));
    }

    public static class State extends CompatEntityRenderState {
        public Identifier texture = ResourceManager.glyphid_tex;
        public float bodyYaw;
        public double scale = 1D;
        public byte armor;
        public byte subtype;
        public float walkCycle;
        public float swingProgress;
        public float deathAge;
        public int light;
    }
}
