package com.hbm.client.render.world;

import com.hbm.capability.HbmLivingProps;
import com.hbm.config.GeneralConfig;
import com.hbm.handler.ImpactWorldHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.Sky;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * 1.7's {@code RenderNTMSkyboxImpact}: the overworld sky while (and, see {@link NTMSkyboxes}, after) Tom's impact dust
 * or fire is in the air. Ported is the part between vanilla's sky disc and its dark disc - stars, sun, moon, the
 * digamma star and the Bobmazon satellite - drawn through Golem Core's {@link Sky} in place of vanilla's sun, moon and
 * stars ({@link NTMSkyboxes#celestial}). The sky disc, the dark disc below the horizon and the sky colour stay vanilla's
 * (the sky colour carries {@link com.hbm.handler.ImpactSky}'s 1.7 {@code WorldProviderNTM} change, as 1.7's
 * {@code world.getSkyColor} did); 1.7's renderer has no sunrise glow, which {@link NTMSkyboxes} removes.
 *
 * <p>Every value, rotation, size and blend below is 1.7's, line for line. Three odd things are ported as written:
 * the stars are alpha-blended (not additive as vanilla's) and turned a further 19 degrees about Y; the black
 * "blanking" quad meant to hide the stars behind the sun is drawn with the additive blend already set, so it adds
 * black and conceals nothing; and the digamma star and the satellite are drawn inside the sun's push, so the digamma
 * star's celestial rotation is applied twice (it crosses the sky at double speed) and the satellite's orbit is
 * carried round with the sun - unlike {@link RenderNTMSkyboxChainloader}'s copies, which start from the sky pose.
 * The digamma star's alpha is {@code dust} = {@code max(1 - 2 * atmosphericDust, 0)}, the factor without the rain
 * term, while the satellite's is {@code rain}; 1.7 wrote it that way.
 */
public final class RenderNTMSkyboxImpact {

    static final Sky.Texture DIGAMMA_STAR = Sky.Texture.of("hbm", "textures/misc/star_digamma.png");
    static final Sky.Texture BOBMAZON_SAT = Sky.Texture.of("hbm", "textures/misc/sat_bobmazon.png");

    private static float[] stars;

    private RenderNTMSkyboxImpact() {}

    public static void render(Sky sky) {
        ClientLevel world = sky.level();
        float partialTicks = sky.partialTick();
        float celestial = sky.celestialAngle();
        PoseStack pose = sky.pose();

        float atmosphericDust = ImpactWorldHandler.getDustForClient(world);
        float dust = Math.max((1.0F - (atmosphericDust * 2)), 0);
        float rain = dust * (1.0F - world.getRainLevel(partialTicks));
        // 1.7's anaglyph colour remap is gone: vanilla removed the 3D-anaglyph option in 1.13.

        float f18 = starBrightness(world, celestial);

        if (f18 > 0.0F) {
            // Blend (770, 771) and texturing off, as set before the stars in 1.7.
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.XP.rotationDegrees(celestial * 360.0F));
            pose.mulPose(Axis.YP.rotationDegrees(-19.0F));
            sky.quads(Sky.Blend.ALPHA, f18, f18, f18, f18 * rain, stars());
            pose.popPose();
        }

        // OpenGlHelper.glBlendFunc(SRC_ALPHA, ONE, ONE, ZERO) from here on.
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(celestial * 360.0F));

        // Render sun. "Some blanking to conceal the stars" - additive black, so it conceals nothing (see class comment).
        float f10 = 30.0F;
        sky.quads(Sky.Blend.ADDITIVE, 0.0F, 0.0F, 0.0F, 1.0F,
                -f10, 99.9F, -f10,
                f10, 99.9F, -f10,
                f10, 99.9F, f10,
                -f10, 99.9F, f10);
        sky.texturedQuads(Sky.Texture.sun(), Sky.Blend.ADDITIVE, 1.0F, 1.0F, 1.0F, rain,
                -f10, 100.0F, -f10, 0.0F, 0.0F,
                f10, 100.0F, -f10, 1.0F, 0.0F,
                f10, 100.0F, f10, 1.0F, 1.0F,
                -f10, 100.0F, f10, 0.0F, 1.0F);

        // Moon: UV 0..1 is the phase's cell (zz/rand9 = 1, yy/rand7 = 0 in 1.7's names).
        f10 = 20.0F;
        sky.texturedQuads(Sky.Texture.moon(sky.moonPhase()), Sky.Blend.ADDITIVE, 1.0F, 1.0F, 1.0F, rain,
                -f10, -100.0F, f10, 1.0F, 1.0F,
                f10, -100.0F, f10, 0.0F, 1.0F,
                f10, -100.0F, -f10, 0.0F, 0.0F,
                -f10, -100.0F, -f10, 1.0F, 0.0F);

        float brightness = (float) Math.sin(celestial * Math.PI);
        brightness *= brightness;

        // Digamma star - still inside the sun's push, so this rotation stacks on the sun's (1.7 as written).
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(celestial * 360.0F));
        pose.mulPose(Axis.XP.rotationDegrees(140.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-40.0F));
        float digamma = digamma();
        float var12 = 1F * (1 + digamma * 0.25F);
        double dist = 100D - digamma * 2.5;
        sky.texturedQuads(DIGAMMA_STAR, Sky.Blend.ADDITIVE, brightness, brightness, brightness, dust,
                quad(var12, (float) dist));
        pose.popPose();

        // Bobmazon satellite.
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(-40.0F));
        pose.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() % (360 * 1000) / 1000F)));
        pose.mulPose(Axis.XP.rotationDegrees((System.currentTimeMillis() % (360 * 100) / 100F)));
        sky.texturedQuads(BOBMAZON_SAT, Sky.Blend.ADDITIVE, brightness, brightness, brightness, rain, quad(0.5F, 100.0F));
        pose.popPose();

        pose.popPose();
    }

    /**
     * 1.7 {@code world.getStarBrightness(partialTicks)}: {@code getStarBrightnessBody} from the celestial angle, times
     * {@code 1 - dust} when 1.7 registered {@code WorldProviderNTM} ({@code 1.32_enableImpactWorldProvider}). Computed
     * here because 26.1's {@code visual/star_brightness} is a keyframed approximation with the rain already folded in.
     */
    static float starBrightness(ClientLevel world, float celestialAngle) {
        float f2 = 1.0F - (Mth.cos(celestialAngle * (float) Math.PI * 2.0F) * 2.0F + 0.25F);
        if (f2 < 0.0F) f2 = 0.0F;
        if (f2 > 1.0F) f2 = 1.0F;
        float starBr = f2 * f2 * 0.5F;
        if (GeneralConfig.enableImpactWorldProvider) starBr *= 1 - ImpactWorldHandler.getDustForClient(world);
        return starBr;
    }

    /** {@code HbmLivingProps.getDigamma(Minecraft.getMinecraft().thePlayer)}. */
    static float digamma() {
        var player = Minecraft.getInstance().player;
        return player == null ? 0F : (float) HbmLivingProps.getDigamma(player);
    }

    /** The star/satellite quad both 1.7 sky renderers draw: half-size {@code s} at height {@code dist}. */
    static float[] quad(float s, float dist) {
        return new float[] {
                -s, dist, -s, 0.0F, 0.0F,
                s, dist, -s, 0.0F, 1.0F,
                s, dist, s, 1.0F, 1.0F,
                -s, dist, s, 1.0F, 0.0F};
    }

    /** 1.7 {@code renderStars}: 1500 candidates from seed 10842, in its own double arithmetic. */
    private static float[] stars() {
        if (stars != null) return stars;
        Random random = new Random(10842L);
        float[] out = new float[1500 * 12];
        int n = 0;

        for (int i = 0; i < 1500; ++i) {
            double d0 = (double) (random.nextFloat() * 2.0F - 1.0F);
            double d1 = (double) (random.nextFloat() * 2.0F - 1.0F);
            double d2 = (double) (random.nextFloat() * 2.0F - 1.0F);
            double d3 = (double) (0.15F + random.nextFloat() * 0.1F);
            double d4 = d0 * d0 + d1 * d1 + d2 * d2;

            if (d4 < 1.0D && d4 > 0.01D) {
                d4 = 1.0D / Math.sqrt(d4);
                d0 *= d4;
                d1 *= d4;
                d2 *= d4;
                double d5 = d0 * 100.0D;
                double d6 = d1 * 100.0D;
                double d7 = d2 * 100.0D;
                double d8 = Math.atan2(d0, d2);
                double d9 = Math.sin(d8);
                double d10 = Math.cos(d8);
                double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
                double d12 = Math.sin(d11);
                double d13 = Math.cos(d11);
                double d14 = random.nextDouble() * Math.PI * 2.0D;
                double d15 = Math.sin(d14);
                double d16 = Math.cos(d14);

                for (int j = 0; j < 4; ++j) {
                    double d17 = 0.0D;
                    double d18 = (double) ((j & 2) - 1) * d3;
                    double d19 = (double) ((j + 1 & 2) - 1) * d3;
                    double d20 = d18 * d16 - d19 * d15;
                    double d21 = d19 * d16 + d18 * d15;
                    double d22 = d20 * d12 + d17 * d13;
                    double d23 = d17 * d12 - d20 * d13;
                    double d24 = d23 * d9 - d21 * d10;
                    double d25 = d21 * d9 + d23 * d10;
                    out[n++] = (float) (d5 + d24);
                    out[n++] = (float) (d6 + d22);
                    out[n++] = (float) (d7 + d25);
                }
            }
        }
        stars = java.util.Arrays.copyOf(out, n);
        return stars;
    }
}
