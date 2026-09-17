package com.hbm.particle.helper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The server-side senders behind the ntm-next {@code MukePayload}, {@code TeslaArcPayload} and
 * {@code GasFlamePayload} calls in the copied entities. This port carries one particle bus
 * ({@link HbmEffectNT} over {@code ParticlePayload}) rather than a class per effect, so each of those
 * payloads is a tag on the bus; the radius each ntm-next site passed to its {@code TargetPoint} is kept
 * as the send range.
 */
public final class EntityEffects {

    private EntityEffects() {}

    /** 1.7 "muke"/"tinytot" AuxParticlePacketNT: the small-nuke flash, with the balefire and tiny-tot selectors. */
    public static void muke(Level level, double x, double y, double z, boolean tinytot, boolean balefire, double range) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("balefire", balefire);
        (tinytot ? HbmEffectNT.TinyTot : HbmEffectNT.Muke).send(level, x, y, z, data, range);
    }

    /** Arc endpoints for a tesla or taint crab, keyed by the crab's entity id. */
    public static void teslaArc(Entity crab, List<Vec3> targets, double range) {
        CompoundTag data = new CompoundTag();
        data.putInt("entityID", crab.getId());
        ListTag list = new ListTag();
        for (Vec3 target : targets) {
            CompoundTag point = new CompoundTag();
            point.putDouble("x", target.x);
            point.putDouble("y", target.y);
            point.putDouble("z", target.z);
            list.add(point);
        }
        data.put("targets", list);
        HbmEffectNT.TeslaArc.send(crab.level(), crab.getX(), crab.getY(), crab.getZ(), data, range);
    }

    /** 1.7 {@code ParticleUtil.spawnGasFlame}: one rising gas-fire puff with the given motion and scale. */
    public static void gasFlame(Level level, double x, double y, double z, double motionX, double motionY,
                                double motionZ, float scale, double range) {
        CompoundTag data = new CompoundTag();
        data.putDouble("moX", motionX);
        data.putDouble("moY", motionY);
        data.putDouble("moZ", motionZ);
        data.putFloat("scale", scale);
        HbmEffectNT.GasFlame.send(level, x, y, z, data, range);
    }
}
