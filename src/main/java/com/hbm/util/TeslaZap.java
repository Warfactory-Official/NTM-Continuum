package com.hbm.util;

import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.handler.ArmorUtil;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.Compat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * ntm-next {@code BlockEntityTesla.zap}, lifted out of the (unported) tesla coil block entity so the tesla
 * and taint crabs share the 1.7 arc logic. When the coil is ported it should call this rather than carry
 * its own copy.
 */
public final class TeslaZap {

    private TeslaZap() {}

    public static boolean isObstructed(ServerLevel level, double x, double y, double z, double ex, double ey,
                                       double ez) {
        // 1.7's 2-arg rayTraceBlocks runs collisionRayTrace on bounding-box-less blocks too, so torches and
        // dust block the zap; OUTLINE is that shape here.
        return Compat.clip(level, new Vec3(x, y, z), new Vec3(ex, ey, ez), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE).getType() != HitResult.Type.MISS;
    }

    /** Returns the beam endpoints, so callers that render arcs (the coil, the tesla crabs) can reuse them. */
    public static List<Vec3> zap(ServerLevel level, double x, double y, double z, double range,
                                 @Nullable Entity source) {
        List<Vec3> beams = new ArrayList<>();
        AABB box = new AABB(x - range, y - range, z - range, x + range, y + range, z + range);
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box);
        DamageSource damage = ModDamageSources.source(level, ModDamageSources.ELECTRICITY);

        for (LivingEntity e : found) {
            if (e instanceof Ocelot || e == source) continue;

            double eyeY = e.getY() + e.getBbHeight() / 2D;
            double len = Math.sqrt(Math.pow(e.getX() - x, 2) + Math.pow(eyeY - y, 2) + Math.pow(e.getZ() - z, 2));
            if (len > range) continue;
            if (isObstructed(level, x, y, z, e.getX(), eyeY, e.getZ())) continue;

            if (e instanceof EntityTaintCrab) {
                beams.add(new Vec3(e.getX(), e.getY() + 1.25D, e.getZ()));
                e.heal(15F);
                continue;
            }

            if (e instanceof EntityTeslaCrab) {
                beams.add(new Vec3(e.getX(), e.getY() + 1D, e.getZ()));
                e.heal(10F);
                continue;
            }

            if (e instanceof EntityCyberCrab) {
                beams.add(new Vec3(e.getX(), eyeY, e.getZ()));
                continue;
            }

            if (e instanceof Creeper creeper) {
                // 1.7:119 watcher 17 = powered
                ((dev.golem.core.world.CreeperFuse) creeper).golem$setPowered(true);
                beams.add(new Vec3(e.getX(), eyeY, e.getZ()));
                continue;
            }

            float amount = Mth.clamp(e.getMaxHealth() * 0.5F, 3F, 20F) / found.size();
            // 1.7: immune targets stay silent
            if (!(e instanceof Player player && ArmorUtil.checkForFaraday(player))
                    && Compat.hurt(e, level, damage, amount)) {
                level.playSound(null, e.getX(), e.getY(), e.getZ(), ModSounds.TESLA_ZAP.get(), SoundSource.BLOCKS,
                        1.0F, 1.0F);
            }

            beams.add(new Vec3(e.getX(), eyeY, e.getZ()));
        }

        return beams;
    }
}
