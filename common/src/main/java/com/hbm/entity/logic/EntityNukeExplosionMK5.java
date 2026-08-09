package com.hbm.entity.logic;

import com.hbm.config.BombConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionNukeRayBatched;
import com.hbm.interfaces.IExplosionRay;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/**
 * The invisible logic entity that drives a nuclear blast over many ticks: each tick it damages nearby
 * entities and advances the {@link ExplosionNukeRayBatched} crater, removing itself when the crater
 * completes. Ported from 1.12.2's {@code EntityNukeExplosionMK5}.
 *
 * <p>It carries no model — a vanilla {@code NoopRenderer} is registered for it (see
 * {@code HbmEntityRenderers}). The strength is {@code 2 * radius}; for the Fat Man that is
 * {@code 2 * 175}.
 *
 * <p>While the blast is young (first 10 ticks, strength ≥ 75) it also pulses radiation into every
 * living entity in range ({@link #radiate}), and when the crater completes it spawns the
 * {@link EntityFalloutRain} sweep (scale {@code radius·2.5 + falloutAdd}, scaled by
 * {@code BombConfig.falloutRange}%).
 *
 * <p><b>Deferred vs 1.12.2:</b> the Torex mushroom cloud, the glowing-one conversion (tick-42 seam
 * below — the mob isn't ported), the Manhattan advancement, the forced chunk-loading
 * ({@code EntityExplosionChunkloading}), and the threaded explosion algorithms. The detonator is
 * tracked for those future hooks but otherwise unused.
 */
public class EntityNukeExplosionMK5 extends Entity {

    private int strength;
    private int radius;
    private boolean fallout = true;
    private int falloutAdd = 0;

    private IExplosionRay explosion;
    private boolean initialized = false;

    public UUID detonator;

    public EntityNukeExplosionMK5(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Builds (but does not spawn) a blast of the given radius centred at the coordinates. */
    public static EntityNukeExplosionMK5 statFac(Level level, int r, double x, double y, double z) {
        if (r == 0) r = 25;

        EntityNukeExplosionMK5 mk5 = new EntityNukeExplosionMK5(ModEntities.NUKE_EXPLOSION_MK5.get(), level);
        mk5.strength = 2 * r;
        mk5.radius = r;
        mk5.setPos(x, y, z);
        if (BombConfig.disableNuclear) mk5.fallout = false;
        return mk5;
    }

    /** Like {@link #statFac} but with the radiation pulse + fallout disabled. */
    public static EntityNukeExplosionMK5 statFacNoRad(Level level, int r, double x, double y, double z) {
        EntityNukeExplosionMK5 mk5 = statFac(level, r, x, y, z);
        mk5.fallout = false;
        return mk5;
    }

    /** Extends the fallout scale by the given number of blocks (dirty-bomb style additions). */
    public EntityNukeExplosionMK5 moreFallout(int falloutAdd) {
        this.falloutAdd = falloutAdd;
        return this;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No synced data — the entity is invisible and purely server-driven.
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // The blast cannot be damaged/killed by anything.
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        if (strength == 0) {
            discard();
            return;
        }

        double r2 = this.radius * 2.0D;
        List<Entity> list = level().getEntities(this,
                new AABB(getX() - r2, getY() - r2, getZ() - r2, getX() + r2, getY() + r2, getZ() + r2));

        // The initial radiation pulse: enormous at detonation, falling off hard over the first 10
        // ticks (the explosion != null gate means it starts on tick 2, like the original).
        if (fallout && explosion != null && tickCount < 10 && strength >= 75) {
            radiate(list, 2_500_000F / (tickCount * 5 + 1));
        }

        ExplosionNukeGeneric.dealDamage(level(), list, getX(), getY(), getZ(), r2);

        if (fallout && tickCount == 42) {
            // TODO(mobs): EntityGlowingOne.convertInRadiusToGlow(level, x, y, z, radius * 1.5) once the mob lands
        }

        // Create the crater raytracer once.
        if (!initialized) {
            explosion = new ExplosionNukeRayBatched(level(), (int) getX(), (int) getY(), (int) getZ(), strength, radius);
            explosion.setDetonator(detonator);
            initialized = true;
        }

        if (!explosion.isComplete()) {
            explosion.update(BombConfig.mk5);
        } else {
            if (fallout) {
                EntityFalloutRain rain = new EntityFalloutRain(ModEntities.FALLOUT_RAIN.get(), level());
                rain.setPos(getX(), getY(), getZ());
                rain.setScale((int) (this.radius * 2.5 + falloutAdd) * BombConfig.falloutRange / 100);
                level().addFreshEntity(rain);
            }
            discard();
        }
    }

    /**
     * The early-blast radiation pulse: per living entity, a ray from the blast centre to its eyes
     * accumulates the explosion resistance of every block on the way; the dose falls off with the
     * summed resistance and the squared distance, then applies as RAD_BYPASS (no hazmat reduction).
     */
    private void radiate(List<Entity> entities, float rads) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living)) continue;

            double dx = living.getX() - getX();
            double dy = (living.getY() + living.getEyeHeight()) - getY();
            double dz = living.getZ() - getZ();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len <= 0.0001D) continue;
            dx /= len; dy /= len; dz /= len;

            double res = 0D;
            int steps = (int) Math.floor(len);
            for (int i = 1; i < steps; i++) {
                int ix = (int) Math.floor(getX() + dx * i);
                int iy = (int) Math.floor(getY() + dy * i);
                int iz = (int) Math.floor(getZ() + dz * i);
                res += level().getBlockState(pos.set(ix, iy, iz)).getBlock().getExplosionResistance();
            }
            if (res < 1.0) res = 1.0;

            double eRads = rads;
            eRads /= res;
            eRads /= len * len;
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.RAD_BYPASS, eRads);
        }
    }

    public EntityNukeExplosionMK5 setDetonator(Entity detonator) {
        if (detonator instanceof Player) {
            this.detonator = detonator.getUUID();
        }
        return this;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (explosion != null) explosion.cancel();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        radius = input.getIntOr("radius", 0);
        strength = input.getIntOr("strength", 0);
        fallout = input.getBooleanOr("fallout", true);
        falloutAdd = input.getIntOr("falloutAdd", 0);
        detonator = input.read("detonator", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        // The in-flight crater bitsets are not persisted — a blast interrupted by a save restarts.
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("radius", radius);
        output.putInt("strength", strength);
        output.putBoolean("fallout", fallout);
        output.putInt("falloutAdd", falloutAdd);
        if (detonator != null) output.store("detonator", net.minecraft.core.UUIDUtil.CODEC, detonator);
    }
}
