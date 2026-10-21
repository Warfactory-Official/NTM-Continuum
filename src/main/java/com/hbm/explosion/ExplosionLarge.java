package com.hbm.explosion;

import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.api.event.HbmEvents;
import dev.golem.core.world.Explosions;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.particle.helper.HbmEffectNT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import com.hbm.config.CompatibilityConfig;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.util.ContaminationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * The "big dumb boom" helpers, 1.7's {@code ExplosionLarge}. A missile's own payload is a VNT
 * explosion or a nuclear entity; this class is the garnish around it: the vanilla blast a small
 * warhead makes, and the wreckage a destroyed missile leaves behind.
 *
 * <p><b>What is ported.</b> {@link #explode} and {@link #spawnMissileDebris}: the two that change what
 * a player ends up holding. Debris is the reason shooting a missile down is worth doing: it drops a
 * randomised share of the missile's parts plus a 1-in-10 warhead.
 *
 * <p>The smoke, rubble and shrapnel helpers keep the 1.7 effect boundaries: the
 * server names effects or spawns projectiles, while the client builds the particles.
 */
public final class ExplosionLarge {

    private ExplosionLarge() {}

    /**
     * 1.7 {@code explode(..., Entity exploder)} (:165): a vanilla smoking blast by {@code detonator}, then the
     * smoke cloud, rubble and shrapnel at the blast centre itself.
     */
    public static void explode(Level world, @Nullable Entity detonator, double x, double y, double z,
                               float strength, boolean cloud, boolean rubble, boolean shrapnel) {
        explode(world, detonator, x, y, z, strength, false, cloud, rubble, shrapnel);
    }

    /**
     * Scatters a missile's parts. Each stack in {@code debris} drops a random count from 0 to its own
     * size, and {@code rareDrop} (the warhead) lands one time in ten.
     *
     * <p>The items are given the missile's own motion plus a gaussian spread and are then advanced two
     * ticks' worth by hand, which is upstream's way of getting them clear of the fireball before the
     * blast damage pass can delete them.
     */
    public static void spawnMissileDebris(Level world, double x, double y, double z,
                                          double motionX, double motionY, double motionZ, double deviation,
                                          @Nullable List<ItemStack> debris, @Nullable ItemStack rareDrop) {

        var rand = world.getRandom();

        if (debris != null) {
            for (ItemStack stack : debris) {
                if (stack == null || stack.isEmpty()) continue;

                int count = rand.nextInt(stack.getCount() + 1);
                for (int j = 0; j < count; j++) {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);

                    ItemEntity item = new ItemEntity(world, x, y, z, copy);
                    double mx = (motionX + rand.nextGaussian() * deviation) * 0.85D;
                    double my = (motionY + rand.nextGaussian() * deviation) * 0.85D;
                    double mz = (motionZ + rand.nextGaussian() * deviation) * 0.85D;
                    item.setDeltaMovement(mx, my, mz);
                    item.setPos(item.getX() + mx * 2, item.getY() + my * 2, item.getZ() + mz * 2);

                    world.addFreshEntity(item);
                }
            }
        }

        if (rareDrop != null && !rareDrop.isEmpty() && rand.nextInt(10) == 0) {
            ItemEntity item = new ItemEntity(world, x, y, z, rareDrop.copy());
            item.setDeltaMovement(
                    motionX + rand.nextGaussian() * deviation * 0.1D,
                    motionY + rand.nextGaussian() * deviation * 0.1D,
                    motionZ + rand.nextGaussian() * deviation * 0.1D);
            world.addFreshEntity(item);
        }
    }

    // Upstream's debris counts

    /**
     * 1.7 :256-258. {@code -i/15} divides as INTs, so the exponent is a whole number: strength 10 yields 15
     * particles, strength 15 yields 552. 1.12.2 rescaled the base to 545; 1.7 is the specification.
     */
    public static int cloudFunction(int i) {
        return (int) (850 * (1 - Math.pow(Math.E, -i / 15)) + 15);
    }

    public static int rubbleFunction(int i) {
        return i / 10;
    }

    public static int shrapnelFunction(int i) {
        return i / 3;
    }

    /** Sends the legacy expanding smoke cloud to tracking clients. */
    public static void spawnParticles(Level world, double x, double y, double z, int count) {
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        HbmEffectNT.Smoke_Cloud.send(world, x, y, z, data);
    }

    /** 1.7 spawnParticlesRadial (:34): smoke mode "radial". */
    public static void spawnParticlesRadial(Level world, double x, double y, double z, int count) {
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        HbmEffectNT.Smoke_Radial.send(world, x, y, z, data);
    }

    /** 1.7 spawnFoam (:43): smoke mode "foamSplash". 1.7 sends no "range", so every bubble starts at the centre. */
    public static void spawnFoam(Level world, double x, double y, double z, int count) {
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        HbmEffectNT.Smoke_FoamSplash.send(world, x, y, z, data);
    }

    /** 1.7 spawnShock (:52): smoke mode "shock", sent half a block up. */
    public static void spawnShock(Level world, double x, double y, double z, int count, double strength) {
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        data.putDouble("strength", strength);
        HbmEffectNT.Smoke_Shock.send(world, x, y + 0.5, z, data);
    }

    /** Spawns the legacy stone fragments used by dirty blasts. */
    public static void spawnRubble(Level world, double x, double y, double z, int count) {
        if (count <= 0) return;
        var random = world.getRandom();
        for (int i = 0; i < count; i++) {
            EntityRubble rubble = new EntityRubble(ModEntities.RUBBLE.get(), world);
            rubble.setPos(x, y, z);
            double motionY = 0.75D * (1 + (count + random.nextInt(count * 5)) / 25);
            double motionX = random.nextGaussian() * 0.75D * (1 + count / 50);
            double motionZ = random.nextGaussian() * 0.75D * (1 + count / 50);
            rubble.setDeltaMovement(motionX, motionY, motionZ);
            rubble.setBlockState(Blocks.STONE.defaultBlockState());
            world.addFreshEntity(rubble);
        }
    }

    /** Spawns the old upward shrapnel burst. */
    public static void spawnShrapnels(Level world, double x, double y, double z, int count) {
        if (count <= 0) return;
        var random = world.getRandom();
        for (int i = 0; i < count; i++) {
            EntityShrapnel shrapnel =
                    new EntityShrapnel(ModEntities.SHRAPNEL.get(), world);
            shrapnel.setPos(x, y, z);
            double motionY = ((random.nextFloat() * 0.5D) + 0.5D)
                    * (1 + count / (15 + random.nextInt(21))) + random.nextFloat() / 50D * count;
            double motionX = random.nextGaussian() * (1 + count / 50);
            double motionZ = random.nextGaussian() * (1 + count / 50);
            shrapnel.setDeltaMovement(motionX, motionY, motionZ);
            shrapnel.setTrail(random.nextInt(3) == 0);
            world.addFreshEntity(shrapnel);
        }
    }

    /** Spawns the old motion-inherited shrapnel shower. */
    public static void spawnShrapnelShower(Level world, double x, double y, double z,
                                           double motionX, double motionY, double motionZ, int count, double deviation) {
        var random = world.getRandom();
        for (int i = 0; i < count; i++) {
            EntityShrapnel shrapnel =
                    new EntityShrapnel(ModEntities.SHRAPNEL.get(), world);
            shrapnel.setPos(x, y, z);
            shrapnel.setDeltaMovement(motionX + random.nextGaussian() * deviation,
                    motionY + random.nextGaussian() * deviation,
                    motionZ + random.nextGaussian() * deviation);
            shrapnel.setTrail(random.nextInt(3) == 0);
            world.addFreshEntity(shrapnel);
        }
    }

    /** Tears out the old jolt rays and launches block rubble toward their source. */
    public static void jolt(Level world, @Nullable Entity detonator, double x, double y, double z,
                            double strength, int count, double vel) {
        if (!CompatibilityConfig.isWarDim(world)) return;
        var random = world.getRandom();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int j = 0; j < count; j++) {
            double phi = random.nextDouble() * (Math.PI * 2D);
            double cosTheta = random.nextDouble() * 2D - 1D;
            double theta = Math.acos(cosTheta);
            double vx = Math.sin(theta) * Math.cos(phi);
            double vy = Math.sin(theta) * Math.sin(phi);
            double vz = Math.cos(theta);
            for (int i = 0; i < strength; i++) {
                double x0 = x + vx * i, y0 = y + vy * i, z0 = z + vz * i;
                pos.set((int) x0, (int) y0, (int) z0);
                if (world.isClientSide()) continue;
                BlockState state = world.getBlockState(pos);
                if (!state.getFluidState().isEmpty()) world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                if (state.isAir() || state.getBlock().getExplosionResistance() > 70F) continue;
                EntityRubble rubble =
                        new EntityRubble(ModEntities.RUBBLE.get(), world);
                rubble.setPos(x0 + 0.5D, y0 + 0.5D, z0 + 0.5D);
                rubble.setBlockState(state);
                Vec3 pull = new Vec3(x - rubble.getX(), y - rubble.getY(), z - rubble.getZ()).normalize();
                rubble.setDeltaMovement(pull.scale(vel));
                world.addFreshEntity(rubble);
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                break;
            }
        }
    }

    /** The old short shrapnel tracer variant. */
    public static void spawnTracers(Level world, double x, double y, double z, int count) {
        var random = world.getRandom();
        for (int i = 0; i < count; i++) {
            EntityShrapnel shrapnel =
                    new EntityShrapnel(ModEntities.SHRAPNEL.get(), world);
            shrapnel.setPos(x, y, z);
            double motionY = ((random.nextFloat() * 0.5D) + 0.5D)
                    * (1 + count / (15 + random.nextInt(21))) + random.nextFloat() / 50D * count * 0.25D;
            double motionX = random.nextGaussian() * (1 + count / 50) * 0.25D;
            double motionZ = random.nextGaussian() * (1 + count / 50) * 0.25D;
            shrapnel.setDeltaMovement(motionX, motionY, motionZ);
            shrapnel.setTrail(true);
            world.addFreshEntity(shrapnel);
        }
    }

    // ntm-next entity port (merge_explosion_helpers.py)


    public static void explode(Level level, double x, double y, double z, float strength, boolean cloud, boolean rubble,
                               boolean shrapnel) {
        explode(level, null, x, y, z, strength, false, cloud, rubble, shrapnel);
    }


    public static void explodeFire(Level level, double x, double y, double z, float strength, boolean cloud,
                                   boolean rubble, boolean shrapnel) {
        explode(level, null, x, y, z, strength, true, cloud, rubble, shrapnel);
    }


    /**
     * 1.7 explode / explodeFire (:165-193): createExplosion / newExplosion (smoking, fire for the
     * incendiary form), then the legacy smoke cloud, rubble and shrapnel counted by the 1.7 functions,
     * all at the blast centre. 1.12.2 moved the garnish two blocks up and swapped in composeEffect.
     */
    private static void explode(Level level, @Nullable Entity detonator, double x, double y, double z, float strength,
                                boolean fire, boolean cloud, boolean rubble, boolean shrapnel) {
        Explosions.explode(level, detonator, x, y, z, strength, fire, Level.ExplosionInteraction.TNT);
        if (cloud) spawnParticles(level, x, y, z, cloudFunction((int) strength));
        if (rubble) spawnRubble(level, x, y, z, rubbleFunction((int) strength));
        if (shrapnel) spawnShrapnels(level, x, y, z, shrapnelFunction((int) strength));
        HbmEvents.explosion(level, x, y, z, strength, detonator, "large");
    }

    public static void spawnBurst(Level level, double x, double y, double z, int count, double strength) {
        if (!(level instanceof ServerLevel server)) return;
        spawnShock(server, x, y, z, count, strength); // 1.7 SmokeShockPayload.MODE_BURST: gas-flame burst drawn as shock smoke
    }

/**
     * Bunker-buster 'drill' - a chained line of vanilla explosions along the flight vector.
     */
    public static void buster(Level level, double x, double y, double z, Vec3 vector, float strength, int depth) {
        Vec3 dir = vector.normalize();
        for (int i = 0; i < depth; i += 2) {
            Explosions.explode(level, null, x + dir.x * i, y + dir.y * i, z + dir.z * i, strength, Level.ExplosionInteraction.TNT);
        }
    }

/**
     * Uniform-sphere-sampled rays that yank the first low-resistance block on each ray into a flying EntityRubble.
     */
    public static void jolt(Level level, double x, double y, double z, double strength, int count, double vel) {
        RandomSource rand = level.getRandom();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int n = 0; n < count; n++) {
            double phi = rand.nextDouble() * Math.PI * 2;
            double cosTheta = rand.nextDouble() * 2 - 1;
            double sinTheta = Math.sqrt(1 - cosTheta * cosTheta);
            double dx = sinTheta * Math.cos(phi);
            double dy = cosTheta;
            double dz = sinTheta * Math.sin(phi);
            for (int i = 0; i < strength; i++) {
                // 1.7 :224 casts with (int): truncation toward zero, not floor
                int bx = (int) (x + dx * i);
                int by = (int) (y + dy * i);
                int bz = (int) (z + dz * i);
                pos.set(bx, by, bz);
                BlockState state = level.getBlockState(pos);
                if (state.liquid()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    continue;
                }
                if (state.isAir()) continue;
                if (state.getBlock().getExplosionResistance() > 70F) continue;
                EntityRubble rubble = new EntityRubble(level, bx + 0.5, by + 0.5, bz + 0.5);
                rubble.setBlockState(state);
                // 1.7 :240 discards Vec3.normalize()'s return value, so the velocity scales with the ray length
                Vec3 back = new Vec3(x - (bx + 0.5), y - (by + 0.5), z - (bz + 0.5)).scale(vel);
                rubble.setDeltaMovement(back);
                level.addFreshEntity(rubble);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                break;
            }
        }
    }
}
