package com.hbm.client;

import com.hbm.client.particle.ParticleContrail;
import com.hbm.client.particle.ParticleExSmoke;
import com.hbm.client.particle.ParticleGasFlame;
import com.hbm.client.particle.ParticleRocketFlame;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The client-side particle entry points the copied ntm-next entities call directly (a missile or plane
 * spawns its own exhaust on each client; no packet is involved). Copied from ntm-next
 * {@code com.hbm.client.ClientEffects}, keeping the methods the entity port uses and routing each one
 * onto this port's own particle classes. The packet-borne effects ({@code GasFlame}) are registered
 * here as {@link HbmEffectNT} handlers so the server side stays a plain {@code send}.
 */
public final class ClientEffects {

    private ClientEffects() {
    }

    /** Installs the entity-effect handlers on the shared particle bus. Client only. */
    public static void install() {
        HbmEffectNT.TeslaArc.setHandler((level, x, y, z, data) -> {
            net.minecraft.world.entity.Entity entity = level.getEntity(Tags.getIntOr(data, "entityID", -1));
            java.util.List<net.minecraft.world.phys.Vec3> targets = new java.util.ArrayList<>();
            net.minecraft.nbt.ListTag list = Tags.getListOrEmpty(data, "targets");
            for (int i = 0; i < list.size(); i++) {
                net.minecraft.nbt.CompoundTag point = Tags.getCompoundOrEmpty(list, i);
                targets.add(new net.minecraft.world.phys.Vec3(Tags.getDoubleOr(point, "x", 0D),
                        Tags.getDoubleOr(point, "y", 0D), Tags.getDoubleOr(point, "z", 0D)));
            }
            if (entity instanceof com.hbm.entity.mob.EntityTeslaCrab tesla) {
                tesla.targets = targets;
            } else if (entity instanceof com.hbm.entity.mob.EntityTaintCrab taint) {
                taint.targets = targets;
            }
        });
        HbmEffectNT.GasFlame.setHandler((level, x, y, z, data) -> spawnGasFlame(level, x, y, z,
                Tags.getDoubleOr(data, "moX", 0D), Tags.getDoubleOr(data, "moY", 0D),
                Tags.getDoubleOr(data, "moZ", 0D), Tags.getFloatOr(data, "scale", GAS_FLAME_DEFAULT_SCALE)));
    }

    /** 1.7 {@code ParticleUtil.spawnGasFlame} sets no scale; the gasfire handler defaults to 6.5. */
    public static final float GAS_FLAME_DEFAULT_SCALE = 6.5F;

    public static void spawnGasFlame(Level level, double x, double y, double z,
                                     double motionX, double motionY, double motionZ, float scale) {
        if (level instanceof ClientLevel client) {
            ParticleGasFlame.spawn(client, x, y, z, motionX, motionY, motionZ, scale);
        }
    }

    private static void spawnGasFlame(Level level, double x, double y, double z, double spread) {
        RandomSource rand = level.getRandom();
        spawnGasFlame(level, x, y, z, rand.nextGaussian() * spread, 0.2D, rand.nextGaussian() * spread,
                GAS_FLAME_DEFAULT_SCALE);
    }

    public static void spawnPlaneFlame(Level level, double x, double y, double z) {
        spawnGasFlame(level, x, y, z, 0.0D, 0.1D, 0.0D, GAS_FLAME_DEFAULT_SCALE);
    }

    public static void spawnRocketFlame(Level level, double x, double y, double z, float scale, double mX, double mY,
                                        double mZ, int maxAge) {
        if (!(level instanceof ClientLevel client)) return;
        ParticleRocketFlame fx = new ParticleRocketFlame(client, x, y, z).setScale(scale).setMaxAge(maxAge)
                .setMotion(mX, mY, mZ);
        Particles.add(fx);
    }

    /**
     * 1.7 {@code ClientProxy.effectNT} "shockRand": a ring of smoke bursts, each pushed out a random fraction
     * of the strength. 1.7 hands both {@code rotateAroundY} calls a value it takes for radians, and gives the
     * second one an integer division; both preserved.
     */
    public static void spawnSmokeShockRand(Level level, double x, double y, double z, int count, double strength) {
        if (!(level instanceof ClientLevel client)) return;
        RandomSource rand = client.getRandom();
        double angle = rand.nextInt(360);
        double step = 360 / count;

        for (int i = 0; i < count; i++) {
            double r = rand.nextDouble();
            ParticleExSmoke fx = new ParticleExSmoke(client, x, y, z);
            fx.setParticleSpeed(Math.cos(angle) * strength * r, 0, Math.sin(angle) * strength * r);
            Particles.add(fx);
            angle += step;
        }
    }

    public static void spawnContrail(Level level, double x, double y, double z, Contrail type) {
        if (!(level instanceof ClientLevel client)) return;
        Particles.add(new ParticleContrail(client, x, y, z, type.r, type.g, type.b, type.scale));
    }

    /**
     * 1.7 {@code ClientProxy:880-883,897-915} - the propellant names the colour. ABM takes the no-colour ctor
     * ({@code ParticleContrail:29-36}), which is black at scale 1, so it and KEROSENE coincide. Lifetime is not
     * a parameter: 1.7 rolls {@code 100 + rand(40)} inside the particle.
     */
    public enum Contrail {
        ABM(0F, 0F, 0F, 1F),
        KEROSENE(0F, 0F, 0F, 1F),
        SOLID(0.3F, 0.2F, 0.05F, 1F),
        HYDROGEN(0.7F, 0.7F, 0.7F, 1F),
        BALEFIRE(0.2F, 0.7F, 0.2F, 1F);

        final float r;
        final float g;
        final float b;
        final float scale;

        Contrail(float r, float g, float b, float scale) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.scale = scale;
        }
    }
}
