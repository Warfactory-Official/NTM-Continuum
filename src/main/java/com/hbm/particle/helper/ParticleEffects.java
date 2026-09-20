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

    /**
     * A large blast: a smoke plume, a shockwave and scoops of the ground thrown into the air.
     *
     * <p>The parameter list is upstream's, in upstream's order, so 1.7 call sites port across by
     * name. {@code waveScale} is carried but not yet drawn — see {@code ExplosionLargeCreator}.
     *
     * @param debrisSize  edge of each scoop in blocks
     * @param debrisRetry how solid a scoop comes out, not how big
     * @param soundRange  how far the report carries; it also sets how far the effect itself is sent
     */
    public static void explosionLarge(Level level, double x, double y, double z,
            int cloudCount, float cloudScale, float cloudSpeedMult, float waveScale,
            int debrisCount, int debrisSize, int debrisRetry, float debrisVelocity,
            float debrisHorizontalDeviation, float debrisVerticalOffset, float soundRange) {

        CompoundTag data = new CompoundTag();
        data.putInt("cloudCount", cloudCount);
        data.putFloat("cloudScale", cloudScale);
        data.putFloat("cloudSpeedMult", cloudSpeedMult);
        data.putFloat("waveScale", waveScale);
        data.putInt("debrisCount", debrisCount);
        data.putInt("debrisSize", debrisSize);
        data.putInt("debrisRetry", debrisRetry);
        data.putFloat("debrisVelocity", debrisVelocity);
        data.putFloat("debrisHorizontalDeviation", debrisHorizontalDeviation);
        data.putFloat("debrisVerticalOffset", debrisVerticalOffset);
        data.putFloat("soundRange", soundRange);
        HbmEffectNT.ExplosionLarge.send(level, x, y, z, data, Math.max(300D, soundRange));
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
        HbmEffectNT.Ashes.send(level, entity.getX(), entity.getY(), entity.getZ(), data, 100D);
    }

    /** Source skeleton strip used by laser, electric, plasma, and fire death effects. */
    public static void skeleton(Level level, Entity entity, float brightness) {
        CompoundTag data = new CompoundTag();
        data.putInt("entityID", entity.getId());
        data.putFloat("brightness", brightness);
        HbmEffectNT.Skeleton.send(level, entity.getX(), entity.getY(), entity.getZ(), data, 100D);
    }

    /** Source skeleton burst used as the first half of an explosive gib effect. */
    public static void skeletonGib(Level level, Entity entity, float force) {
        CompoundTag data = new CompoundTag();
        data.putInt("entityID", entity.getId());
        data.putFloat("brightness", 1F);
        data.putFloat("force", force);
        data.putBoolean("gib", true);
        HbmEffectNT.Skeleton.send(level, entity.getX(), entity.getY(), entity.getZ(), data, 100D);
    }

    /** Source meat/slime/metal giblet burst, including the entity id used by the client hide gate. */
    public static void giblets(Level level, Entity entity, int gibType) {
        CompoundTag data = new CompoundTag();
        data.putInt("ent", entity.getId());
        data.putInt("gibType", gibType);
        HbmEffectNT.Giblets.send(level, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D,
                entity.getZ(), data, 150D);
    }

    /** RBMK's 250-block melt cloud. Scale is sent explicitly because the vanilla particle payload has no scale field. */
    public static void rbmkMush(Level level, double x, double y, double z, float scale) {
        CompoundTag data = new CompoundTag();
        data.putFloat("scale", scale);
        HbmEffectNT.RBMKMush.send(level, x, y, z, data);
    }

    /** Redstone block dust burst emitted when the sawmill blade damages a living entity. */
    public static void sawmillDamage(Level level, Entity entity, int count) {
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        HbmEffectNT.SawmillDamage.send(level, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D,
                entity.getZ(), data);
    }
}
