package com.hbm.entity.grenade;

import com.hbm.Constants;
import com.hbm.config.GeneralConfig;
import dev.golem.core.world.Compat;
import com.hbm.entity.projectile.EntityThrowableNT;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1.7 EntityGrenadeBase (marked @Deprecated there too, kept because EntityDisperserCanister still rides it): detonates on impact rather than on a fuse.
 */
@Deprecated
public abstract class EntityGrenadeBase extends EntityThrowableNT {

    public EntityGrenadeBase(EntityType<? extends EntityGrenadeBase> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();

        xRotO = getXRot();
        // 26.1: setXRot clamps; tumble saturates, nothing observes
        setXRot(getXRot() - (float) (getDeltaMovement().length() * 25));
        // 1.7: the yaw normalise+lerp after super is idempotent - same motion, same tick-start yRotO, so it
        // recomputes what EntityThrowableNT just wrote. Only the pitch tumble above it is a real edit.
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (mop instanceof EntityHitResult entityHit && level() instanceof ServerLevel server) {
            Entity hit = entityHit.getEntity();
            // 1.7: zero-damage hit, for the hurt flash
            Compat.hurt(hit, server, damageSources().thrown(this, getOwner()), 0F);
        }

        if (!level().isClientSide() && GeneralConfig.enableExtendedLogging) {
            Entity thrower = getOwner();
            Constants.LOG.info("[GREN] Set off grenade at {} / {} / {} by {}!", (int) getX(), (int) getY(),
                    (int) getZ(), thrower instanceof Player player ? player.getDisplayName().getString() : "null");
        }

        explode();
    }

    public abstract void explode();
}
