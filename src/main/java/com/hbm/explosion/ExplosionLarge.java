package com.hbm.explosion;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
 * <p><b>What is deferred, and why (see IMPLEMENTATION.md §20).</b> {@code spawnRubble},
 * {@code spawnShrapnels}, {@code spawnShrapnelShower}, {@code spawnTracers} and {@code jolt} all spawn
 * {@code EntityRubble}/{@code EntityShrapnel}, and {@code spawnParticles}/{@code spawnShock} name
 * particle families ({@code Smoke_Cloud}, {@code Smoke_Shock}) that the particle wave has not reached.
 * They are kept here as no-op stubs with their real signatures rather than deleted, so the call sites
 * in the missiles stay identical to upstream and light up as soon as those entities land.
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

    // --- upstream's debris counts, kept exact so the stubs below light up correctly later ---

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

    /** Deferred: needs the {@code Smoke_Cloud} particle family. */
    public static void spawnParticles(Level world, double x, double y, double z, int count) {}

    /** Deferred: needs the {@code Smoke_Shock} particle family. */
    public static void spawnShock(Level world, double x, double y, double z, int count, double strength) {}

    /** Deferred: needs {@code EntityRubble}. */
    public static void spawnRubble(Level world, double x, double y, double z, int count) {}

    /** Deferred: needs {@code EntityShrapnel}. */
    public static void spawnShrapnels(Level world, double x, double y, double z, int count) {}

    /** Deferred: needs {@code EntityShrapnel}. */
    public static void spawnShrapnelShower(Level world, double x, double y, double z,
                                           double motionX, double motionY, double motionZ, int count, double deviation) {}

    /** Deferred: needs {@code EntityRubble} — this is the one that tears blocks out along each ray. */
    public static void jolt(Level world, @Nullable Entity detonator, double x, double y, double z,
                            double strength, int count, double vel) {}
}
