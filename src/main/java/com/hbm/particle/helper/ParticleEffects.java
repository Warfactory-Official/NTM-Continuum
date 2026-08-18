package com.hbm.particle.helper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Server-side entry points for playing HBM's particle effects: one method per effect, each packing
 * that effect's parameters into the tag its client handler expects.
 *
 * <p><b>Why these are not on the creators.</b> Upstream puts each {@code composeEffect} as a static on
 * the creator class it belongs to ({@code BlackPowderCreator.composeEffect}), which works in 1.12
 * because {@code @SideOnly} strips the client half out of the server jar. There is no such stripping
 * here — a single source set means calling a static on a client class from common code loads that
 * class on a dedicated server, dragging {@code ClientLevel} and the particle engine with it. So the
 * senders live here, on the server-safe side of the same line {@link HbmEffectNT} draws, and the
 * creators keep only {@code makeParticle}.
 *
 * <p>The parameter lists are upstream's, unchanged, so old call sites port across by name.
 */
public final class ParticleEffects {

    private ParticleEffects() {}

    /**
     * Muzzle discharge: a cone of smoke and sparks thrown along {@code heading}.
     *
     * @param headingX heading of the discharge; need not be normalised
     */
    public static void blackPowder(Level level, double x, double y, double z,
            double headingX, double headingY, double headingZ,
            int cloudCount, float cloudScale, float cloudSpeedMult,
            int sparkCount, float sparkSpeedMult) {

        CompoundTag data = new CompoundTag();
        data.putInt("cloudCount", cloudCount);
        data.putFloat("cloudScale", cloudScale);
        data.putFloat("cloudSpeedMult", cloudSpeedMult);
        data.putInt("sparkCount", sparkCount);
        data.putFloat("sparkSpeedMult", sparkSpeedMult);
        data.putDouble("hX", headingX);
        data.putDouble("hY", headingY);
        data.putDouble("hZ", headingZ);
        HbmEffectNT.BlackPowder.send(level, x, y, z, data);
    }

    /** A small blast: fireballs plus debris torn from an adjacent block. */
    public static void explosionSmall(Level level, double x, double y, double z,
            int cloudCount, float cloudScale, float cloudSpeedMult) {

        CompoundTag data = new CompoundTag();
        data.putInt("cloudCount", cloudCount);
        data.putFloat("cloudScale", cloudScale);
        data.putFloat("cloudSpeedMult", cloudSpeedMult);
        data.putInt("debris", 15);
        HbmEffectNT.ExplosionSmall.send(level, x, y, z, data);
    }

    /** One tongue of flamethrower fire. Emitters call this every tick they are firing. */
    public static void flamethrower(Level level, double x, double y, double z, int meta) {
        CompoundTag data = new CompoundTag();
        data.putInt("meta", meta);
        HbmEffectNT.Flamethrower.send(level, x, y, z, data);
    }

    /** Turns an entity to ash: embers and flakes filling the volume it occupied. */
    public static void ashes(Level level, Entity entity, int ashesCount, float ashesScale) {
        CompoundTag data = new CompoundTag();
        data.putInt("entityID", entity.getId());
        data.putInt("ashesCount", ashesCount);
        data.putFloat("ashesScale", ashesScale);
        HbmEffectNT.Ashes.send(level, entity.getX(), entity.getY(), entity.getZ(), data);
    }
}
