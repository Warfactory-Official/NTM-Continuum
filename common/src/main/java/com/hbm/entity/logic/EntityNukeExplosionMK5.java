package com.hbm.entity.logic;

import com.hbm.config.BombConfig;
import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionNukeRayBatched;
import com.hbm.interfaces.IExplosionRay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class EntityNukeExplosionMK5 extends Entity {

    private int strength;
    private int radius;
    private boolean fallout = true;

    private IExplosionRay explosion;
    private boolean initialized = false;

    public UUID detonator;

    public EntityNukeExplosionMK5(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static EntityNukeExplosionMK5 statFac(Level level, int r, double x, double y, double z) {
        if (r == 0) r = 25;

        EntityNukeExplosionMK5 mk5 = new EntityNukeExplosionMK5(ModEntities.NUKE_EXPLOSION_MK5.get(), level);
        mk5.strength = 2 * r;
        mk5.radius = r;
        mk5.setPos(x, y, z);
        if (BombConfig.disableNuclear) mk5.fallout = false;
        return mk5;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {

        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        if (strength == 0) {
            discard();
            return;
        }

        double r2 = this.radius * 2.0D;
        List<Entity> list = level().getEntities(this,
                new AABB(getX() - r2, getY() - r2, getZ() - r2, getX() + r2, getY() + r2, getZ() + r2));
        ExplosionNukeGeneric.dealDamage(level(), list, getX(), getY(), getZ(), r2);

        if (!initialized) {
            explosion = new ExplosionNukeRayBatched(level(), (int) getX(), (int) getY(), (int) getZ(), strength, radius);
            explosion.setDetonator(detonator);
            initialized = true;
        }

        if (!explosion.isComplete()) {
            explosion.update(BombConfig.mk5);
        } else {

            discard();
        }
    }

    public EntityNukeExplosionMK5 setDetonator(Entity detonator) {
        if (detonator instanceof Player) {
            this.detonator = detonator.getUUID();
        }
        return this;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (explosion != null) explosion.cancel();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        radius = input.getIntOr("radius", 0);
        strength = input.getIntOr("strength", 0);
        fallout = input.getBooleanOr("fallout", true);
        detonator = input.read("detonator", net.minecraft.core.UUIDUtil.CODEC).orElse(null);

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("radius", radius);
        output.putInt("strength", strength);
        output.putBoolean("fallout", fallout);
        if (detonator != null) output.store("detonator", net.minecraft.core.UUIDUtil.CODEC, detonator);
    }
}
