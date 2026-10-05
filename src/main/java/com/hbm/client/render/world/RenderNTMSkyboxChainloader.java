package com.hbm.client.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.Sky;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * 1.7's {@code RenderNTMSkyboxChainloader}: in the overworld, after the sky (vanilla's, or
 * {@link RenderNTMSkyboxImpact}'s when that was the chained parent), the lode star when
 * {@link NTMSkyboxes#renderLodeStar}, the digamma star (bigger and closer the more digamma the player has) and the
 * orbiting Bobmazon satellite, all with {@code glBlendFunc(SRC_ALPHA, ONE)}, no fog and no depth write. Drawn from
 * {@link NTMSkyboxes#afterSky}. 1.7's parent chaining and recursion brake are {@link NTMSkyboxes}' slot state.
 *
 * <p>The lode star's size is re-rolled from the world's random every frame, so it twinkles; 1.7 draws one random
 * number per frame whether or not the lode star is shown, and so does this.
 */
public final class RenderNTMSkyboxChainloader {

    static final Sky.Texture LODE_STAR = Sky.Texture.of("hbm", "textures/misc/star_lode.png");

    private RenderNTMSkyboxChainloader() {}

    public static void render(Sky sky) {
        ClientLevel world = sky.level();
        float celestial = sky.celestialAngle();
        PoseStack pose = sky.pose();
        pose.pushPose();

        float var12 = 0.5F + world.getRandom().nextFloat() * 0.25F;
        float dist = 100.0F;

        if (NTMSkyboxes.renderLodeStar) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-75.0F));
            pose.mulPose(Axis.YP.rotationDegrees(10.0F));
            sky.texturedQuads(LODE_STAR, Sky.Blend.ADDITIVE, 1F, 1F, 1F, 1.0F, RenderNTMSkyboxImpact.quad(var12, dist));
            pose.popPose();
        }

        float brightness = (float) Math.sin(celestial * Math.PI);
        brightness *= brightness;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(celestial * 360.0F));
        pose.mulPose(Axis.XP.rotationDegrees(140.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-40.0F));
        float digamma = RenderNTMSkyboxImpact.digamma();
        var12 = 1F * (1 + digamma * 0.25F);
        double digammaDist = 100D - digamma * 2.5;
        sky.texturedQuads(RenderNTMSkyboxImpact.DIGAMMA_STAR, Sky.Blend.ADDITIVE, brightness, brightness, brightness, 1.0F,
                RenderNTMSkyboxImpact.quad(var12, (float) digammaDist));
        pose.popPose();

        // Same colour as the digamma star: 1.7 sets glColor4f(brightness, ..., 1) once for both.
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(-40.0F));
        pose.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() % (360 * 1000) / 1000F)));
        pose.mulPose(Axis.XP.rotationDegrees((System.currentTimeMillis() % (360 * 100) / 100F)));
        sky.texturedQuads(RenderNTMSkyboxImpact.BOBMAZON_SAT, Sky.Blend.ADDITIVE, brightness, brightness, brightness, 1.0F,
                RenderNTMSkyboxImpact.quad(0.5F, 100.0F));
        pose.popPose();

        pose.popPose();
    }
}
