package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm.explosion.vanillant.interfaces.IEntityRangeMutator;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatEnchantments;
import dev.golem.core.world.Raycasts;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The entity half of a VNT explosion, and — as upstream's own comment puts it — one of the few
 * unambiguously good decisions in NTM.
 *
 * <p>Two things separate it from vanilla's version, and both matter for a missile:
 *
 * <ul>
 *   <li><b>Distance is measured to the entity's <i>box</i>, not its feet.</b> Vanilla measures centre to
 *       centre, so a wide entity standing next to a blast can read as far away. Here each axis
 *       contributes zero if the box straddles the centre on that axis, which is what stops a blast
 *       under a player's feet from under-damaging them.
 *   <li><b>Line of sight is sampled from several nodes.</b> With {@code nodeDist > 0} the exposure test
 *       runs from seven points (the centre plus the six face offsets) and keeps the <i>best</i> one, so
 *       an explosion going off flush against a wall still reaches whoever is around the corner instead
 *       of being fully absorbed by the block it touched.
 * </ul>
 *
 * <p>The damage curve {@code ((knockback² + knockback) / 2 · 8 · size + 1)} is vanilla's, unchanged.
 */
public class EntityProcessorCross implements IEntityProcessor {

    protected double nodeDist;
    protected @Nullable IEntityRangeMutator range;
    protected @Nullable ICustomDamageHandler damage;
    protected double knockbackMult = 1D;
    protected boolean allowSelfDamage = false;

    public EntityProcessorCross() {
        this(0);
    }

    public EntityProcessorCross(double nodeDist) {
        this.nodeDist = nodeDist;
    }

    public EntityProcessorCross setAllowSelfDamage() {
        this.allowSelfDamage = true;
        return this;
    }

    public EntityProcessorCross setKnockback(double mult) {
        this.knockbackMult = mult;
        return this;
    }

    public EntityProcessorCross withRangeMod(float mod) {
        this.range = (explosion, range) -> range * mod;
        return this;
    }

    public EntityProcessorCross withDamageMod(ICustomDamageHandler damage) {
        this.damage = damage;
        return this;
    }

    @Override
    public HashMap<Player, Vec3> process(ExplosionVNT explosion, Level world, double x, double y, double z, float size) {

        HashMap<Player, Vec3> affectedPlayers = new HashMap<>();

        size *= 2.0F;
        if (range != null) size = range.mutateRange(explosion, size);

        AABB area = new AABB(x - size - 1.0D, y - size - 1.0D, z - size - 1.0D,
                x + size + 1.0D, y + size + 1.0D, z + size + 1.0D);
        List<Entity> list = world.getEntities(allowSelfDamage ? null : explosion.exploder, area);

        Vec3[] nodes;
        if (this.nodeDist > 0) {
            nodes = new Vec3[7];
            for (int i = 0; i < 7; i++) {
                ForgeDirection dir = ForgeDirection.getOrientation(i);
                nodes[i] = new Vec3(x + dir.offsetX * nodeDist, y + dir.offsetY * nodeDist, z + dir.offsetZ * nodeDist);
            }
        } else {
            nodes = new Vec3[]{new Vec3(x, y, z)};
        }

        Map<Entity, Float> damageMap = new HashMap<>();

        for (Entity entity : list) {
            double distanceScaled = scaledDistance(entity, x, y, z, size);
            if (distanceScaled > 1.0D) continue;

            double deltaX = entity.getX() - x;
            double deltaY = entity.getY() + entity.getEyeHeight() - y;
            double deltaZ = entity.getZ() - z;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (distance == 0.0D) continue;

            deltaX /= distance;
            deltaY /= distance;
            deltaZ /= distance;

            double density = 0;
            for (Vec3 vec : nodes) {
                double d = seenPercent(world, vec, entity);
                if (d > density) density = d;
            }

            double knockback = (1.0D - distanceScaled) * density;
            double enchantedKnockback = knockback;
            if (entity instanceof LivingEntity living && entity.level() instanceof ServerLevel serverLevel) {
                enchantedKnockback = CompatEnchantments.blastKnockback(serverLevel, living, knockback);
            }

            float dmg = calculateDamage(distanceScaled, density, knockback, size);
            Float existing = damageMap.get(entity);
            if (existing == null || existing < dmg) damageMap.put(entity, dmg);

            if (shouldDealKnockback(entity)) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(
                        deltaX * enchantedKnockback * knockbackMult,
                        deltaY * enchantedKnockback * knockbackMult,
                        deltaZ * enchantedKnockback * knockbackMult));
            }

            if (entity instanceof Player player) {
                // The packet path deliberately carries the raw value; old-hbm applies blast protection
                // only to server motion, while the client receives this unmodified knockback map.
                affectedPlayers.put(player, new Vec3(deltaX * knockback * knockbackMult,
                        deltaY * knockback * knockbackMult, deltaZ * knockback * knockbackMult));
            }
        }

        for (Map.Entry<Entity, Float> entry : damageMap.entrySet()) {
            Entity entity = entry.getKey();
            attackEntity(entity, explosion, entry.getValue());

            if (damage != null) {
                damage.handleAttack(explosion, entity, scaledDistance(entity, x, y, z, size));
            }
        }

        return affectedPlayers;
    }

    /** Box-aware distance, normalised so 1.0 is the edge of the blast. */
    private static double scaledDistance(Entity entity, double x, double y, double z, float size) {
        AABB box = entity.getBoundingBox();
        double xDist = (box.minX <= x && box.maxX >= x) ? 0 : Math.min(Math.abs(box.minX - x), Math.abs(box.maxX - x));
        double yDist = (box.minY <= y && box.maxY >= y) ? 0 : Math.min(Math.abs(box.minY - y), Math.abs(box.maxY - y));
        double zDist = (box.minZ <= z && box.maxZ >= z) ? 0 : Math.min(Math.abs(box.minZ - z), Math.abs(box.maxZ - z));
        return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) / size;
    }

    /**
     * The fraction of rays from {@code centre} to the entity's box that reach it unobstructed —
     * 1.12.2's {@code World.getBlockDensity}, which modern Minecraft has as
     * {@code ServerExplosion.getSeenPercent}. That method is on a different class in each era, so the
     * (unchanged) algorithm is spelled out here rather than reached for through a version guard.
     */
    public static double seenPercent(Level level, Vec3 centre, Entity entity) {
        AABB box = entity.getBoundingBox();
        double xs = 1.0D / ((box.maxX - box.minX) * 2.0D + 1.0D);
        double ys = 1.0D / ((box.maxY - box.minY) * 2.0D + 1.0D);
        double zs = 1.0D / ((box.maxZ - box.minZ) * 2.0D + 1.0D);
        if (xs < 0.0D || ys < 0.0D || zs < 0.0D) return 0.0D;

        double xOffset = (1.0D - Math.floor(1.0D / xs) * xs) / 2.0D;
        double zOffset = (1.0D - Math.floor(1.0D / zs) * zs) / 2.0D;

        int hits = 0;
        int count = 0;
        for (double xx = 0.0D; xx <= 1.0D; xx += xs) {
            for (double yy = 0.0D; yy <= 1.0D; yy += ys) {
                for (double zz = 0.0D; zz <= 1.0D; zz += zs) {
                    Vec3 sample = new Vec3(
                            net.minecraft.util.Mth.lerp(xx, box.minX, box.maxX) + xOffset,
                            net.minecraft.util.Mth.lerp(yy, box.minY, box.maxY),
                            net.minecraft.util.Mth.lerp(zz, box.minZ, box.maxZ) + zOffset);
                    if (!Raycasts.isObstructed(level, sample, centre, entity)) hits++;
                    count++;
                }
            }
        }
        return count == 0 ? 0.0D : (double) hits / count;
    }

    /**
     * Projectiles are exempt from knockback: upstream's list names bullets and grenades, both of which
     * carry their own flight model that a shove would corrupt. Only the bullet is ported so far.
     */
    /** ntm-next: explosion knockback resistance damping (1.7 had none; the attribute is modern). */
    public static double knockbackDamping(Entity entity) {
        return entity instanceof LivingEntity living
                ? 1.0D - dev.golem.core.world.Compat.explosionKnockbackResistance(living)
                : 1.0D;
    }

    public static boolean shouldDealKnockback(Entity entity) {
        return !(entity instanceof com.hbm.entity.projectile.EntityBulletBaseMK4);
    }

    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        Compat.hurt(entity, level, damageSource(entity.level(), source.exploder), amount);
    }

    public float calculateDamage(double distanceScaled, double density, double knockback, float size) {
        return (int) ((knockback * knockback + knockback) / 2.0D * 8.0D * size + 1.0D);
    }

    public static DamageSource damageSource(Level level, @Nullable Entity exploder) {
        return level.damageSources().explosion(exploder, exploder);
    }
}
