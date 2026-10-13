package com.hbm.saveddata.satellites;

import com.hbm.entity.logic.EntityOrbitalLaser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * 1.7 {@code SatellitePrecisionLaser}, the orbital precision laser: a five-second charge, then a small laser
 * explosion ({@link EntityOrbitalLaser}) at the target, or on an entity picked out by id.
 */
public class SatellitePrecisionLaser extends Satellite {

    // 1.7 also wrote targetedEntity (a runtime network entity id typed in over ROR) to NBT. Entity ids are reassigned
    // on every load, so the saved number could only ever hit the wrong entity; the port keeps it in memory only and
    // a reload clears it to -1 (never persist numeric ids). An old save's field is ignored.
    public static final MapCodec<SatellitePrecisionLaser> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("lastShot", 0L).forGetter(sat -> sat.lastShot)
    ).apply(i, lastShot -> new SatellitePrecisionLaser(lastShot, -1)));

    public static final String CMD_FIRE = "fire";
    public static final String CMD_CANFIRE = "canfire";
    public static final String CMD_SETENTITYTARGET = "setentitytarget";

    public static final int MAX_TARGET_RANGE = 1_000;
    public static final int CHARGE_TIME = 5 * 20;

    public long lastShot;
    public int targetedEntity = -1;

    public SatellitePrecisionLaser() {
        this(0L, -1);
    }

    private SatellitePrecisionLaser(long lastShot, int targetedEntity) {
        this.lastShot = lastShot;
        this.targetedEntity = targetedEntity;
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.PRECISION_LASER;
    }

    @Override public String getType() { return "ORBITAL_TATOO_REMOVER"; }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_FIRE)) {
            if (!(level instanceof ServerLevel server)) return;

            if (this.targetedEntity != -1) {
                Entity e = level.getEntity(this.targetedEntity);
                this.targetedEntity = -1;

                if (e == null || e.isRemoved()) return;

                int x = (int) Math.floor(e.getX());
                int z = (int) Math.floor(e.getZ());

                double dX = x - targetX;
                double dZ = z - targetZ;

                if (dX * dX + dZ * dZ <= MAX_TARGET_RANGE * MAX_TARGET_RANGE) {
                    this.deathBlast(server, e.getX(), e.getY(), e.getZ());
                    return;
                }
            }

            deathBlast(server, targetX, targetZ);
            return;
        }

        if (cmd[0].equals(CMD_CANFIRE)) {
            this.tx = ("" + (lastShot + CHARGE_TIME < level.getGameTime())).toUpperCase(Locale.US);
            return;
        }

        if (cmd[0].equals(CMD_SETENTITYTARGET)) {
            // 1.7 reads cmd[1] unchecked; a bare "setentitytarget" fails the call there and here
            this.targetedEntity = parse(cmd[1]);
        }
    }

    @Override
    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
        this.setTarget(x, z);
        this.deathBlast(level, targetX, targetZ);
    }

    public void deathBlast(ServerLevel level, int x, int z) {
        int y = Heights.heightValue(level, x, z);
        deathBlast(level, x + 0.5, y, z + 0.5);
    }

    public void deathBlast(ServerLevel level, double x, double y, double z) {
        if (lastShot + CHARGE_TIME < level.getGameTime()) {
            lastShot = level.getGameTime();

            EntityOrbitalLaser blast = new EntityOrbitalLaser(level);
            blast.setPos(x, y, z);
            blast.explode();

            level.addFreshEntity(blast);
        }
    }
}
