package com.hbm.explosion;

import dev.golem.core.world.Compat;
import com.hbm.blocks.ModFalloutBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;

/**
 * Shared nuclear-explosion helpers, ported from 1.12.2. Only the blast-damage path used by the Fat Man
 * is ported here; the radiation/contamination, confetti gibbing, EMP and silk-block conversion paths
 * are deferred until the hazard system lands (see IMPLEMENTATION.md §18).
 */
public final class ExplosionNukeGeneric {

    private ExplosionNukeGeneric() {}

    public static void dealDamage(Level world, List<Entity> list, double x, double y, double z, double radius) {
        dealDamage(world, list, x, y, z, radius, 250F);
    }

    /**
     * Damages, ignites and knocks back every entity within {@code radius} of the centre, scaled linearly
     * by distance and blocked by line-of-sight obstruction. Server-side only ({@code hurtServer}).
     */
    public static void dealDamage(Level world, List<Entity> list, double x, double y, double z, double radius, float maxDamage) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        DamageSource source = world.damageSources().explosion(null, null);

        for (Entity e : list) {
            double dist = Math.sqrt(e.distanceToSqr(x, y, z));
            if (dist > radius) continue;

            double entX = e.getX();
            double entY = e.getY() + e.getEyeHeight();
            double entZ = e.getZ();

            if (isObstructed(world, x, y, z, entX, entY, entZ)) continue;

            double damage = maxDamage * (radius - dist) / radius;
            boolean doKnockback = Compat.hurt(e, serverLevel, source, (float) damage);

            e.igniteForSeconds(5);

            if (doKnockback) {
                Vec3 knock = new Vec3(entX - x, entY - y, entZ - z).normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(knock.x * 0.2D, knock.y * 0.2D, knock.z * 0.2D));
            }
        }
    }

    /** True if a solid block lies between the two points (line-of-sight blocked). */
    private static boolean isObstructed(Level world, double x1, double y1, double z1, double x2, double y2, double z2) {
        Vec3 from = new Vec3(x1, y1, z1);
        Vec3 to = new Vec3(x2, y2, z2);
        // Use the CollisionContext overload with CollisionContext.empty(): the (…, Entity) ctor calls
        // CollisionContext.of(entity) which NPEs on a null entity.
        return world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()))
                .getType() != HitResult.Type.MISS;
    }

    /**
     * Small, server-only remnant of the old nuclear wasteland conversion. It deliberately changes only
     * natural terrain and never creates fallout layers, weather, nuclear entities, or blast effects.
     */
    public static void transformWaste(Level level, BlockPos center, int radius) {
        if (!(level instanceof ServerLevel) || radius <= 0) return;

        int radiusSquared = radius * radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        RandomSource random = level.getRandom();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSquared) continue;
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(pos, ModFalloutBlocks.waste_earth.get().defaultBlockState(), 2);
                    } else if (state.is(Blocks.MYCELIUM)) {
                        level.setBlock(pos, ModFalloutBlocks.waste_mycelium.get().defaultBlockState(), 2);
                    } else if (state.is(Blocks.SAND) && random.nextInt(20) == 0) {
                        level.setBlock(pos, ModFalloutBlocks.waste_trinitite.get().defaultBlockState(), 2);
                    } else if (state.is(Blocks.RED_SAND) && random.nextInt(20) == 0) {
                        level.setBlock(pos, ModFalloutBlocks.waste_trinitite_red.get().defaultBlockState(), 2);
                    } else if (state.is(Blocks.CLAY)) {
                        level.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}
