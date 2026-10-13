package com.hbm.explosion;

import com.hbm.potion.ModEffects;
import dev.golem.core.world.Compat;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.ArmorUtil;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.ArmorRegistry;
import dev.golem.core.world.Effects;
import dev.golem.core.world.Fires;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.phys.AABB;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The area-effect half of a warhead, 1.7's {@code ExplosionChaos}: the fire sweeps
 * ({@link #igniteFlammableBlocks}, {@link #igniteAllBlocks}), the cluster spray, the gas clouds and
 * the effect-cloud volleys.
 */
public final class ExplosionChaos {

    private ExplosionChaos() {}

    // --- ntm-next entity port (merge_explosion_helpers.py) ---

/** 1.7 floater:224-253 - lifts every block in the sphere straight up by {@code height}, leaving air behind. */
    public static void floater(Level level, BlockPos center, int radius, int height) {
        int r2 = radius * radius;
        int threshold = r2 / 2;
        BlockPos.MutableBlockPos from = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos to = new BlockPos.MutableBlockPos();
        for (int xx = -radius; xx < radius; xx++) {
            int xxSq = xx * xx;
            for (int yy = -radius; yy < radius; yy++) {
                int yySq = xxSq + yy * yy;
                for (int zz = -radius; zz < radius; zz++) {
                    if (yySq + zz * zz >= threshold) continue;
                    from.set(center.getX() + xx, center.getY() + yy, center.getZ() + zz);
                    BlockState save = level.getBlockState(from);
                    level.setBlockAndUpdate(from, Blocks.AIR.defaultBlockState());
                    if (save.isAir()) continue;
                    to.set(from.getX(), from.getY() + height, from.getZ());
                    level.setBlockAndUpdate(to, save);
                }
            }
        }
    }

/**
     * 1.7 move:256-303 - shoves every entity by {@code (a, b, c)}. The two radii differ on purpose: a mob is
     * RENAMED anywhere within the doubled radius but only MOVED inside the plain one.
     */
    public static void move(Level level, BlockPos center, int radius, int a, int b, int c) {
        double reach = radius;
        int doubled = radius * 2;
        AABB box = new AABB(Math.floor(center.getX() - reach - 1D), Math.floor(center.getY() - reach - 1D),
                Math.floor(center.getZ() - reach - 1D), Math.floor(center.getX() + reach + 1D),
                Math.floor(center.getY() + reach + 1D), Math.floor(center.getZ() + reach + 1D));

        for (Entity entity : level.getEntities((Entity) null, box, e -> true)) {
            if (entity.distanceToSqr(center.getX(), center.getY(), center.getZ())
                    > (double) doubled * doubled) continue;

            // 1.7 asks EntityLiving, which is the mob half alone - a player is moved but never renamed
            if (entity instanceof Sheep sheep) {
                sheep.setCustomName(Component.literal("jeb_"));
            } else if (entity instanceof Mob mob) {
                mob.setCustomName(Component.literal(
                        level.getRandom().nextInt(2) == 0 ? "Dinnerbone" : "Grumm"));
            }

            double dx = entity.getX() - center.getX();
            double dy = entity.getY() + entity.getEyeHeight() - center.getY();
            double dz = entity.getZ() - center.getZ();
            if (dx * dx + dy * dy + dz * dz < reach * reach) {
                entity.setPos(entity.getX() + a, entity.getY() + b, entity.getZ() + c);
            }
        }
    }


    public static void igniteFlammableBlocks(ServerLevel level, int x, int y, int z, int bound) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int threshold = bound * bound / 2;
        for (int xx = -bound; xx < bound; xx++) {
            int xxSq = xx * xx;
            for (int yy = -bound; yy < bound; yy++) {
                int yySq = xxSq + yy * yy;
                for (int zz = -bound; zz < bound; zz++) {
                    if (yySq + zz * zz >= threshold) continue;
                    pos.set(x + xx, y + yy, z + zz);
                    // 1.7: ExplosionChaos:78 passes the squared offsets as isFlammable's coordinates; the cell's own is meant
                    if (!Fires.isFlammable(level, pos, level.getBlockState(pos), Direction.UP)) continue;
                    BlockPos above = pos.above();
                    if (level.getBlockState(above).isAir()) {
                        level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3); // 1.7 setBlock(fire)
                    }
                }
            }
        }
    }


    public static void spawnPoisonCloud(Level level, double x, double y, double z, int count, double speed, int type) {
        for (int i = 0; i < count; i++) {
            Entity fx = switch (type) {
                case 1 -> new EntityCloudFX(level, x, y, z, 0D, 0D, 0D);
                case 2 -> new EntityPinkCloudFX(level, x, y, z, 0D, 0D, 0D);
                default -> new EntityOrangeFX(level, x, y, z, 0D, 0D, 0D);
            };
            fx.setDeltaMovement(level.getRandom().nextGaussian() * speed, level.getRandom().nextGaussian() * speed,
                    level.getRandom().nextGaussian() * speed);
            level.addFreshEntity(fx);
        }
    }

/**
     * 1.7 ExplosionChaos.cluster: the cluster warhead's submunition spray, thrown along the missile's own
     * heading with a gaussian cone.
     */
    public static void cluster(Level level, double x, double y, double z, int count, float yaw, float pitch,
                               float yawRand, float pitchRand, float speed) {

        for (int i = 0; i < count; i++) {
            EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level,
                    XFactoryCatapult.cluster_submunition, 50F, 0F,
                    yaw + (float) (yawRand * level.getRandom().nextGaussian()),
                    pitch + (float) (pitchRand * level.getRandom().nextGaussian()));
            bullet.setPos(x, y, z);
            bullet.setDeltaMovement(bullet.getDeltaMovement().scale(speed));
            level.addFreshEntity(bullet);
        }
    }

/**
     * 1.7 ExplosionChaos.pc: the pink cloud. Eats the victim's whole suit whether or not it protects, then
     * hits for a flat 5 through armour.
     */
    public static void pinkCloud(Level level, double x, double y, double z, double range) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(x - range, y - range, z - range, x + range, y + range, z + range))) {
            if (entity.distanceToSqr(x, y, z) > range * range) continue;

            ArmorUtil.damageWholeSuit(entity, 25);

            if (level instanceof ServerLevel server) {
                Compat.hurt(entity, server, ModDamageSources.source(level, ModDamageSources.PINK_CLOUD), 5F);
            }
        }
    }

/**
     * 1.7 ExplosionChaos.c: the chlorine cloud. Same suit damage, but a hazmat wearer is spared the hit -
     * and any taint already on them is converted into the (inert) mutation marker.
     */
    public static void chlorine(Level level, double x, double y, double z, double range) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(x - range, y - range, z - range, x + range, y + range, z + range))) {
            if (entity.distanceToSqr(x, y, z) > range * range) continue;

            ArmorUtil.damageWholeSuit(entity, 25);

            if (ArmorUtil.checkForHazmat(entity)) continue;

            if (Effects.hasRegistered(entity, ModEffects.TAINT.get())) {
                Effects.removeRegistered(entity, ModEffects.TAINT.get());
                entity.addEffect(Effects.instanceRegistered(
                        ModEffects.MUTATION.get(), 60 * 60 * 20, 0, false, false));
            }

            if (level instanceof ServerLevel server) {
                Compat.hurt(entity, server, ModDamageSources.source(level, ModDamageSources.CLOUD), 5F);
            }
        }
    }

/**
     * 1.7 ExplosionChaos.poison, invoked by EntityOrangeFX once per 50 server ticks.
     */
    public static void poison(Level level, double x, double y, double z, double range) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(x - range, y - range, z - range, x + range, y + range, z + range))) {
            if (entity.distanceToSqr(x, y, z) > range * range) continue;
            boolean protectedFromGas = ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.GAS_LUNG)
                    || ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.GAS_BLISTERING);
            // 1.7: protection turns the hit into filter wear instead
            if (protectedFromGas) {
                ArmorUtil.damageGasMaskFilter(entity, 1);
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 2));
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30 * 20, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 30 * 20, 2));
        }
    }

    public static void igniteAllBlocks(ServerLevel level, int x, int y, int z, int bound) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int threshold = bound * bound / 2;
        for (int xx = -bound; xx < bound; xx++) {
            int xxSq = xx * xx;
            for (int yy = -bound; yy < bound; yy++) {
                int yySq = xxSq + yy * yy;
                for (int zz = -bound; zz < bound; zz++) {
                    if (yySq + zz * zz >= threshold) continue;
                    pos.set(x + xx, y + yy, z + zz);
                    if (level.getBlockState(pos).isAir()) continue;
                    BlockPos above = pos.above();
                    BlockState aboveState = level.getBlockState(above);
                    if (aboveState.isAir() || aboveState.is(Blocks.SNOW)) {
                        level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3); // 1.7 setBlock(fire)
                    }
                }
            }
        }
    }
}
