package com.hbm.entity.effect;

import com.hbm.config.CompatibilityConfig;
import com.hbm.entity.ModEntities;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/**
 * The storm a dark fusion core leaves behind when it lets go: an invisible box that calls down a bolt
 * of lightning every tick above itself until it ages out. Upstream has no renderer for it either — the
 * whole effect is the weather.
 */
public class EntityCloudFleijaRainbow extends CompatEntity {

    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(EntityCloudFleijaRainbow.class, EntityDataSerializers.INT);

    public int age;
    public float scale;

    public EntityCloudFleijaRainbow(EntityType<? extends EntityCloudFleijaRainbow> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EntityCloudFleijaRainbow(Level level) {
        this(ModEntities.CLOUD_FLEIJA_RAINBOW.get(), level);
    }

    public EntityCloudFleijaRainbow setMaxAge(int maxAge) {
        this.entityData.set(MAX_AGE, maxAge);
        return this;
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(MAX_AGE, 100);
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        if (!CompatibilityConfig.isWarDim(level())) {
            discard();
            return;
        }

        this.age++;

        if (level() instanceof ServerLevel server) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.snapTo(getX(), getY() + 200, getZ(), getYRot(), getXRot());
                bolt.setVisualOnly(true);
                server.addFreshEntity(bolt);
            }
        }

        if (this.age >= getMaxAge()) {
            this.age = 0;
            discard();
            return;
        }

        this.scale++;
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("age", age);
        output.putDouble("scale", scale);
    }

    @Override
    protected void loadData(DataIn input) {
        age = input.getIntOr("age", 0);
        scale = (float) input.getDoubleOr("scale", 0D);
    }
}
