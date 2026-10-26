package com.hbm.client;

import com.hbm.api.environment.Environment;
import com.hbm.config.RadiationConfig;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Fogs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code ModEventHandlerRenderer}'s smog: {@code worldTick} eases {@code renderSoot} towards the synced soot of the
 * player's region by 0.05 a tick; above {@code POL_06_sootFogThreshold} the fog becomes a linear fog from the camera to
 * {@code farPlane / (1 + (soot - threshold) * 5 / POL_07_sootFogDivisor)} ({@code thickenFog}, radial as 1.7's
 * {@code GL_NV_fog_distance} eye-radial mode) and its colour blends towards grey 0.15 by
 * {@code min((soot - threshold) / divisor, 1)} ({@code tintFog}). Through Core {@link Fogs}.
 *
 * <p>Two 1.7 details are kept: the smog is skipped under blindness (1.7's blindness fog branch came before the
 * {@code FogDensity} event) and otherwise applies underwater and in lava too (the event came before those branches).
 * One is not: 1.7 eased {@code renderSoot} from the server's {@code WorldTickEvent}, which only fires in the integrated
 * server, so the smog never moved for a client of a dedicated server; the port eases it on the client tick instead.
 */
public final class SmogFog implements Fogs.Modifier {

    private SmogFog() {}

    /** 1.7 {@code PermaSyncHandler.pollution}: the synced pollution of the player's region, by PollutionType order. */
    public static final float[] pollution = new float[4];
    /** 1.7 {@code renderSoot}. */
    public static float renderSoot = 0F;

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ClientTicks.onEndTick(SmogFog::tick);
        Fogs.addModifier(new SmogFog());
    }

    /** 1.7 {@code worldTick} START: step 0.05 towards the synced soot. */
    public static void tick() {
        if (!RadiationConfig.enableSootFog) return;
        renderSoot = step(renderSoot, pollution[0]);
    }

    public static float step(float render, float soot) {
        float step = 0.05F;
        if (Math.abs(render - soot) < step) return soot;
        if (render < soot) return render + step;
        return render - step;
    }

    /** 1.7 {@code thickenFog}: the fog end for {@code soot} over the threshold, or NaN below it. */
    public static float fogEnd(float renderSoot, float farPlaneDistance) {
        float soot = (float) (renderSoot - RadiationConfig.sootFogThreshold);
        if (soot <= 0 || !RadiationConfig.enableSootFog) return Float.NaN;
        return farPlaneDistance / (1 + soot * 5F / (float) RadiationConfig.sootFogDivisor);
    }

    /** 1.7 {@code tintFog}: the smog's share of the fog colour. */
    public static float tint(float renderSoot) {
        float soot = (float) (renderSoot - RadiationConfig.sootFogThreshold);
        if (soot <= 0 || !RadiationConfig.enableSootFog) return 0F;
        return Math.min(soot / (float) RadiationConfig.sootFogDivisor, 1F);
    }

    @Override
    public boolean active(Entity camera) {
        return RadiationConfig.enableSootFog && renderSoot - RadiationConfig.sootFogThreshold > 0;
    }

    @Override
    public Vec3 color(Entity camera, Vec3 rgb) {
        float interp = tint(renderSoot);
        double sootColor = 0.15D;
        return new Vec3(rgb.x * (1 - interp) + sootColor * interp, rgb.y * (1 - interp) + sootColor * interp,
                rgb.z * (1 - interp) + sootColor * interp);
    }

    @Override
    public float far(Entity camera, float renderDistanceBlocks) {
        if (Environment.TERRAIN_FOG_RANGE.invoker().test(camera.level())) return Float.NaN;
        if (camera instanceof LivingEntity living && living.hasEffect(MobEffects.BLINDNESS)) return Float.NaN;
        return fogEnd(renderSoot, renderDistanceBlocks);
    }
}
