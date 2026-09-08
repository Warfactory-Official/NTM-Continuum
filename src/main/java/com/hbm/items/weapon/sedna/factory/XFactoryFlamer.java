package com.hbm.items.weapon.sedna.factory;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.mixin.FireBlockAccessor;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Flamethrower projectiles. Only the two gravity-free loads are built: they are what the Fritz turret
 * sprays, and it is the calibre's only consumer here — the handheld flamethrowers, their napalm and gas
 * loads and the Topaz/Daybreaker variants arrive with the guns.
 *
 * <p>A tongue of flame is a short-lived projectile, not a hitscan: it sets what it touches alight, and
 * where it lands it leaves a patch of {@link EntityFireLingering} that keeps burning after the burst.
 */
public final class XFactoryFlamer {

    public static BulletConfig flame_nograv;
    public static BulletConfig flame_nograv_bf;

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_IGNITE_FIRE = (bullet, mop) -> {
        if (mop instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            HbmLivingProps.setFireAtLeast(living, 100);
        }
    };

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_IGNITE_BALEFIRE = (bullet, mop) -> {
        if (mop instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            HbmLivingProps.setBalefireAtLeast(living, 200);
        }
    };

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_LINGER_DIESEL = (bullet, mop) -> {
        if (!igniteIfPossible(bullet, mop)) {
            spawnFire(bullet, mop, 2F, 1F, 100, EntityFireLingering.TYPE_DIESEL);
        }
    };

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_LINGER_BALEFIRE = (bullet, mop) ->
            spawnFire(bullet, mop, 3F, 1F, 300, EntityFireLingering.TYPE_BALEFIRE);

    private XFactoryFlamer() {}

    /**
     * Sets the struck face alight if it will take a flame. Flammability comes off vanilla's burn-odds
     * map rather than upstream's per-face Forge query, which has no Fabric counterpart.
     */
    public static boolean igniteIfPossible(EntityBulletBaseMK4 bullet, HitResult mop) {
        if (!(mop instanceof BlockHitResult blockHit)) return false;

        Level world = bullet.level();
        BlockPos pos = blockHit.getBlockPos();
        Direction dir = blockHit.getDirection();
        BlockState state = world.getBlockState(pos);

        if (((FireBlockAccessor) Blocks.FIRE).hbm$getBurnOdds(state) > 0) {
            BlockPos target = pos.relative(dir);
            if (world.getBlockState(target).isAir()) {
                world.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                return true;
            }
        }

        bullet.discard();
        return false;
    }

    /** One patch per spot: a burst of twenty flames must not stack twenty overlapping fires. */
    public static void spawnFire(EntityBulletBaseMK4 bullet, HitResult mop, float width, float height,
                                 int duration, int type) {
        if (!(mop instanceof BlockHitResult)) return;

        Vec3 hit = mop.getLocation();
        List<EntityFireLingering> existing = bullet.level().getEntitiesOfClass(EntityFireLingering.class,
                new AABB(hit.x, hit.y, hit.z, hit.x, hit.y, hit.z)
                        .inflate(width / 2 + 0.5, height / 2 + 0.5, width / 2 + 0.5));

        if (existing.isEmpty()) {
            EntityFireLingering fire = new EntityFireLingering(bullet.level())
                    .setArea(width, height).setDuration(duration).setFireType(type);
            fire.setPos(hit);
            bullet.level().addFreshEntity(fire);
        }

        bullet.discard();
    }

    public static void init() {
        flame_nograv = BulletConfigs.register("flame_nograv", new BulletConfig()
                .setupDamageClass(DamageClass.FIRE).setLife(100).setVel(1F).setGrav(0F)
                .setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
                .setOnImpact(LAMBDA_IGNITE_FIRE).setOnRicochet(LAMBDA_LINGER_DIESEL));

        flame_nograv_bf = BulletConfigs.register("flame_nograv_bf", new BulletConfig()
                .setupDamageClass(DamageClass.FIRE).setLife(100).setVel(1F).setGrav(0F)
                .setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
                .setOnImpact(LAMBDA_IGNITE_BALEFIRE).setOnRicochet(LAMBDA_LINGER_BALEFIRE));
    }
}
