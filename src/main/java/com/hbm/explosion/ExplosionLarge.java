package com.hbm.explosion;

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
 * The "big dumb boom" helpers — 1.12.2's {@code ExplosionLarge}. A missile's own payload is a VNT
 * explosion or a nuclear entity; this class is the garnish around it: the vanilla blast a small
 * warhead makes, and the wreckage a destroyed missile leaves behind.
 *
 * <p><b>What is ported.</b> {@link #explode} and {@link #spawnMissileDebris} — the two that change what
 * a player ends up holding. Debris is the reason shooting a missile down is worth doing: it drops a
 * randomised share of the missile's parts plus a 1-in-10 warhead.
 *
     * <p>The smoke, rubble and shrapnel helpers intentionally retain their old effect boundaries: the
     * server names effects or spawns projectiles, while the client builds the particles.
 */
public final class ExplosionLarge {

    private ExplosionLarge() {}

    /**
     * A vanilla blast plus optional debris. Upstream's booleans are kept even though two of the three
     * effects are stubs — a caller passing {@code (true, false, true)} is stating what the warhead is
     * supposed to look like, and that statement should survive the port.
     */
    public static void explode(Level world, @Nullable Entity detonator, double x, double y, double z,
                               float strength, boolean cloud, boolean rubble, boolean shrapnel) {
        world.explode(detonator, x, y, z, strength, Level.ExplosionInteraction.TNT);
        if (cloud) spawnParticles(world, x, y + 2, z, cloudFunction((int) strength));
        if (rubble) spawnRubble(world, x, y + 2, z, rubbleFunction((int) strength));
        if (shrapnel) spawnShrapnels(world, x, y + 2, z, shrapnelFunction((int) strength));
    }

    /** Old antimatter-cell dirty blast: a no-fallout radius-100 nuke plus the configured radiation pulse. */
    public static void explodeFire(Level world, @Nullable Entity detonator, double x, double y, double z,
                                   float strength, boolean cloud, boolean rubble, boolean shrapnel) {
        if (CompatibilityConfig.isWarDim(world)) {
            EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFacNoRad(
                    world, (int) strength, x, y, z).setDetonator(detonator);
            world.addFreshEntity(blast);
            ContaminationUtil.radiate(world, x, y, z, strength, 0F, 0F,
                    strength * 20F, strength * 5F);
        }
        if (cloud) spawnParticles(world, x, y + 2, z, cloudFunction((int) strength));
        if (rubble) spawnRubble(world, x, y + 2, z, rubbleFunction((int) strength));
        if (shrapnel) spawnShrapnels(world, x, y + 2, z, shrapnelFunction((int) strength));
    }

    /**
     * Scatters a missile's parts. Each stack in {@code debris} drops a random count from 0 to its own
     * size, and {@code rareDrop} — the warhead — lands one time in ten.
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

    // --- upstream's debris counts ---

    public static int cloudFunction(int i) {
        return (int) (545 * (1 - Math.pow(Math.E, -i / 15D)) + 15);
    }

    public static int rubbleFunction(int i) {
        return i / 10;
    }

    public static int shrapnelFunction(int i) {
        return i / 3;
    }

    // --- deferred: waiting on EntityRubble / EntityShrapnel and the smoke particle families ---

    /** Sends the legacy expanding smoke cloud to tracking clients. */
    public static void spawnParticles(Level world, double x, double y, double z, int count) {
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        data.putInt("count", count);
        com.hbm.particle.helper.HbmEffectNT.Smoke_Cloud.send(world, x, y, z, data);
    }

    /** Sends the legacy radial shock smoke to tracking clients. */
    public static void spawnShock(Level world, double x, double y, double z, int count, double strength) {
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        data.putInt("count", count);
        data.putDouble("strength", strength);
        com.hbm.particle.helper.HbmEffectNT.Smoke_Shock.send(world, x, y, z, data);
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
            rubble.setBlockState(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            world.addFreshEntity(rubble);
        }
    }

    /** Spawns the old upward shrapnel burst. */
    public static void spawnShrapnels(Level world, double x, double y, double z, int count) {
        if (count <= 0) return;
        var random = world.getRandom();
        for (int i = 0; i < count; i++) {
            com.hbm.entity.projectile.EntityShrapnel shrapnel =
                    new com.hbm.entity.projectile.EntityShrapnel(com.hbm.entity.ModEntities.SHRAPNEL.get(), world);
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
            com.hbm.entity.projectile.EntityShrapnel shrapnel =
                    new com.hbm.entity.projectile.EntityShrapnel(com.hbm.entity.ModEntities.SHRAPNEL.get(), world);
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
                net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
                if (!state.getFluidState().isEmpty()) world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                if (state.isAir() || state.getBlock().getExplosionResistance() > 70F) continue;
                com.hbm.entity.projectile.EntityRubble rubble =
                        new com.hbm.entity.projectile.EntityRubble(com.hbm.entity.ModEntities.RUBBLE.get(), world);
                rubble.setPos(x0 + 0.5D, y0 + 0.5D, z0 + 0.5D);
                rubble.setBlockState(state);
                Vec3 pull = new Vec3(x - rubble.getX(), y - rubble.getY(), z - rubble.getZ()).normalize();
                rubble.setDeltaMovement(pull.scale(vel));
                world.addFreshEntity(rubble);
                world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                break;
            }
        }
    }

    /** The old short shrapnel tracer variant. */
    public static void spawnTracers(Level world, double x, double y, double z, int count) {
        var random = world.getRandom();
        for (int i = 0; i < count; i++) {
            com.hbm.entity.projectile.EntityShrapnel shrapnel =
                    new com.hbm.entity.projectile.EntityShrapnel(com.hbm.entity.ModEntities.SHRAPNEL.get(), world);
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
}
