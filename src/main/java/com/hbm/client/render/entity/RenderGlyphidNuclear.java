package com.hbm.client.render.entity;

import com.hbm.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * The nuclear glyphid: a standard glyphid carrying a mini nuke on its back that swells and flashes white
 * over its ninety-tick death countdown, the creeper way.
 */
public final class RenderGlyphidNuclear extends RenderGlyphid<EntityGlyphidNuclear> {

    private static final RenderType MINI_NUKE_TYPE = CompatRenderTypes.entityCutout(ResourceManager.mini_nuke_tex);
    private static final RenderType WHITE_FLASH_TYPE = CompatRenderTypes.entityTranslucent(ResourceManager.white_tex);

    public RenderGlyphidNuclear(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extract(EntityGlyphidNuclear entity, State state, float partialTicks) {
        super.extract(entity, state, partialTicks);
        state.deathAge = entity.deathTicks + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        RenderType type = CompatRenderTypes.entityCutout(state.texture);

        pose.pushPose();
        applySwellScale(pose, state);
        applyBaseTransform(pose, state);
        submitLimbs(state, pose, sink, light, type, -1);
        submitMiniNuke(pose, sink, light, MINI_NUKE_TYPE, -1);

        int packedAlpha = flashPackedAlpha(state.deathAge / 20F);
        if (packedAlpha > 0) {
            int flashColor = (packedAlpha << 24) | 0xFFFFFF;
            submitLimbs(state, pose, sink, light, WHITE_FLASH_TYPE, flashColor);
            submitMiniNuke(pose, sink, light, WHITE_FLASH_TYPE, flashColor);
        }
        pose.popPose();
    }

    private static void applySwellScale(PoseStack pose, State state) {
        float swell = state.deathAge / 95F;
        float flash = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;

        if (swell < 0.0F) swell = 0.0F;
        if (swell > 1.0F) swell = 1.0F;

        swell *= swell;
        swell *= swell;

        float scaleHorizontal = (1.0F + swell * 0.4F) * flash;
        float scaleVertical = (1.0F + swell * 0.1F) / flash;
        pose.scale(scaleHorizontal, scaleVertical, scaleHorizontal);
    }

    private static int flashPackedAlpha(float swell) {
        int a = (int) (swell * 0.2F * 255.0F);

        if ((int) (swell * 10.0F) % 4 < 2) return (int) (a * 0.75F);

        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return a;
    }

    private static void submitMiniNuke(PoseStack pose, SubmitSink sink, int light, RenderType type, int color) {
        pose.pushPose();
        pose.translate(0F, 1F, 0F);
        pose.mulPose(Axis.XP.rotationDegrees(90F));
        sink.submitCustomGeometry(pose, type, (p, buf) ->
                NtmEntityModels.PROJECTILES.renderPart("MiniNuke", p, buf, light, OverlayTexture.NO_OVERLAY, color));
        pose.popPose();
    }
}
