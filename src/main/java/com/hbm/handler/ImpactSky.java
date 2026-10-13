package com.hbm.handler;

import com.hbm.api.environment.Environment;
import com.hbm.config.GeneralConfig;
import com.hbm.saveddata.TomSaveData;
import dev.golem.core.world.Skies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7.10's {@code WorldProviderNTM} (registered for dimension 0 when {@code 1.32_enableImpactWorldProvider} is on):
 * after Tom's impact the dust dims the sun, the light, the stars, the clouds and the sunrise, and while the fire
 * burns the sky glows red. Applied through Golem Core's {@link Skies.Modifier}, on the server for the sky
 * darkening (1.7 {@code getSunBrightnessFactor}, which drives mob spawning and plant light) and on the client for
 * the rest, with the client reading the synced values ({@link ImpactWorldHandler}).
 *
 * <p>1.7's {@code RenderNTMSkyboxImpact} custom sky renderer ({@code enableSkyboxes}) - the faded sun, moon and
 * stars, no sunrise glow, the digamma star and satellite - is {@link com.hbm.client.render.world.RenderNTMSkyboxImpact},
 * installed by {@link com.hbm.client.render.world.NTMSkyboxes}.
 */
public final class ImpactSky implements Skies.Modifier {

    private ImpactSky() {}

    public static void register() {
        Skies.addModifier(new ImpactSky());
    }

    private static float dust(Level level) {
        if (level.isClientSide()) return ImpactWorldHandler.getDustForClient(level);
        TomSaveData data = level instanceof ServerLevel server ? TomSaveData.published(server) : null;
        return data == null ? 0F : data.dust;
    }

    private static float fire(Level level) {
        if (level.isClientSide()) return ImpactWorldHandler.getFireForClient(level);
        TomSaveData data = level instanceof ServerLevel server ? TomSaveData.published(server) : null;
        return data == null ? 0F : data.fire;
    }

    @Override
    public boolean active(Level level) {
        // the overworld's WorldProviderNTM in 1.7; any surface world an addon names (com.hbm.api.environment.Environment)
        return GeneralConfig.enableImpactWorldProvider && Environment.isSurface(level)
                && (dust(level) > 0 || fire(level) > 0);
    }

    /** 1.7 {@code getSunBrightnessFactor}: {@code sunBr * (1 - dust)}. */
    @Override
    public float daylight(Level level, float daylight) {
        return daylight * (1 - dust(level));
    }

    /** 1.7 {@code getSkyColor}. */
    @Override
    public Vec3 skyColor(Level level, Vec3 sky) {
        return skyColor17(sky, dust(level), fire(level));
    }

    public static Vec3 skyColor17(Vec3 sky, float dust, float fire) {
        float f4, f5, f6;

        if (fire > 0) {
            f4 = (float) (sky.x * 1.3f);
            f5 = (float) sky.y * (Math.max((1 - (dust * 1.4f)), 0));
            f6 = (float) sky.z * (Math.max((1 - (dust * 4)), 0));
        } else {
            f4 = (float) sky.x;
            f5 = (float) sky.y * (1 - (dust * 0.5F));
            f6 = (float) sky.z * (1 - dust);
        }

        return new Vec3((double) f4 * (fire + (1 - dust)), (double) f5 * (fire + (1 - dust)), (double) f6 * (fire + (1 - dust)));
    }

    /** 1.7 {@code getFogColor}. */
    @Override
    public Vec3 fogColor(Level level, Vec3 fog) {
        return fogColor17(fog, dust(level), fire(level));
    }

    public static Vec3 fogColor17(Vec3 fog, float dust, float fire) {
        float f3 = (float) fog.x;
        float f4 = (float) fog.y * (1 - (dust * 0.5F));
        float f5 = (float) fog.z * (1 - dust);

        if (fire > 0) {
            float m = Math.max((1 - (dust * 2)), 0);
            return new Vec3((double) f3 * m, (double) f4 * m, (double) f5 * m);
        }
        return new Vec3((double) f3 * (1 - dust), (double) f4 * (1 - dust), (double) f5 * (1 - dust));
    }

    /** 1.7 {@code drawClouds}. */
    @Override
    public Vec3 cloudColor(Level level, Vec3 clouds) {
        float dust = dust(level);
        return new Vec3(clouds.x * (1 - dust), clouds.y * (1 - dust), clouds.z * (1 - dust));
    }

    /** 1.7 {@code getStarBrightness}. */
    @Override
    public float starBrightness(Level level, float brightness) {
        return brightness * (1 - dust(level));
    }

    /** 1.7 {@code getSunBrightness}: {@code (sunBr * 0.8 + 0.2) * (1 - dust)}; vanilla's value is the bracket. */
    @Override
    public float skyLight(Level level, float factor) {
        return factor * (1 - dust(level));
    }

    /** 1.7 {@code calcSunriseSunsetColors}: colour and alpha times {@code 1 - dust}. */
    @Override
    public float[] sunriseColor(Level level, float[] rgba) {
        if (rgba == null) return null;
        float dust = dust(level);
        return new float[] {rgba[0] * (1 - dust), rgba[1] * (1 - dust), rgba[2] * (1 - dust), rgba[3] * (1 - dust)};
    }
}
