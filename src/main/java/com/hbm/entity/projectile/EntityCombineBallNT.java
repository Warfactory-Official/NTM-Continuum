package com.hbm.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class EntityCombineBallNT extends EntityThrowableInterp {

    // 1.7: EntityBulletBaseNT.overrideDamage, folded into the impact damage roll; unread until a bullet-config port exists
    public float overrideDamage;

    public EntityCombineBallNT(EntityType<? extends EntityCombineBallNT> type, Level level) {
        super(type, level);
        this.overrideDamage = 1000;
    }

    // 1.7 EntityBulletBaseNT: gaussian-spread scale 1 (vs the throwable default of 0.0075)
    @Override
    protected double headingForceMult() {
        return 1D;
    }

    // 1.7 EntityBulletBaseNT: drag 1 in both media (no slowdown)
    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected float getWaterDrag() {
        return 1F;
    }

    @Override
    protected void onImpact(HitResult mop) {
        // 1.7 resolves the hit through EntityBulletBaseNT's BulletConfiguration. Unported: nothing
        // in 1.7 constructs this class - no EntityMappings entry, no BulletConfigSyncingUtil id, no call site -
        // so a sedna BulletConfig here would be an invented weapon. Wire one if a shooter ever lands.
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    // 26.1: 1.7's setDead() explosion rides remove(), destroy-gated like EntityBulletBaseMK4.
    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);

        if (!reason.shouldDestroy()) return;
        if (!(this.level() instanceof ServerLevel server)) return;
        server.explode(this.getThrower(), null, null, this.getX(), this.getY(), this.getZ(), 2F, false,
                Level.ExplosionInteraction.NONE);
    }

    // 1.7: writeToNBTOptional()==false - transient
    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
