package com.hbm.entity.projectile;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Original gun payload callbacks that are independent of the gun factory declarations. */
public final class GunProjectilePayloads {

    private GunProjectilePayloads() {}

    /** The Folly/nuclear gun callback: wait two ticks against a living target, then spawn blast and cloud. */
    public static void nuclearImpact(EntityBulletBaseMK4 bullet, HitResult hit, int radius) {
        if (hit.getType() == HitResult.Type.ENTITY && bullet.tickCount < 2) return;
        if (bullet.isRemoved()) return;

        Level level = bullet.level();
        bullet.discard();
        EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFac(level, radius,
                hit.getLocation().x, hit.getLocation().y, hit.getLocation().z)
                .setDetonator(bullet.getThrower());
        if (level.addFreshEntity(blast)) blast.startChunkLoading();

        EntityNukeTorex torex = EntityNukeTorex.statFac(level, hit.getLocation().x, hit.getLocation().y,
                hit.getLocation().z, radius);
        if (level.addFreshEntity(torex)) torex.startChunkLoading();
    }

    /** Vanilla-only half of the old water extinguisher callback; foam/sand blocks remain caller-owned. */
    public static void extinguishBlock(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (!(bullet.level() instanceof ServerLevel level) || !(hit instanceof BlockHitResult blockHit)) return;
        BlockPos base = blockHit.getBlockPos();
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            BlockPos pos = base.offset(x, y, z);
            if (level.getBlockState(pos).is(Blocks.FIRE))
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        bullet.discard();
    }

    public static void extinguishEntity(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit) entityHit.getEntity().clearFire();
    }
}
