package com.hbm.entity.effect;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.registry.Entities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * The B92 / weaponised-cell blast sphere: same tick body as EntityCloudSolinium, drawn in per-frame random colours.
 */
public class EntityCloudFleijaRainbow extends CompatEntity {

    // 1.7: dataWatcher slot 16
    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(EntityCloudFleijaRainbow.class, EntityDataSerializers.INT);

    public int age;
    public float scale = 0;

    public EntityCloudFleijaRainbow(EntityType<? extends EntityCloudFleijaRainbow> type, Level level) {
        super(type, level);
        this.age = 0;
        this.scale = 0;
    }

    public static EntityCloudFleijaRainbow statFac(Level level, int maxAge, double x, double y, double z) {
        EntityCloudFleijaRainbow cloud = new EntityCloudFleijaRainbow(ModNtmEntities.CLOUD_RAINBOW.get(), level);
        cloud.setPos(x, y, z);
        cloud.setMaxAge(maxAge);
        return cloud;
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(MAX_AGE, 0);
    }

    // 1.7: onUpdate without super - no movement/gravity
    @Override
    public void tick() {
        this.age++;

        // 26.1: server-only spawn - see EntityCloudSolinium.
        if (level() instanceof ServerLevel server) {
            LightningBolt bolt = Entities.createTriggered(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, server);
            if (bolt != null) {
                bolt.snapTo(getX(), getY() + 200, getZ());
                server.addFreshEntity(bolt);
            }
        }

        if (this.age >= this.getMaxAge()) {
            this.age = 0;
            this.discard();
        }

        this.scale++;
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < 25000; // 1.7: isInRangeToRenderDist
    }

    @Override
    protected void loadData(DataIn input) {
        age = input.getShortOr("age", (short) 0);
        scale = input.getShortOr("scale", (short) 0);
    }

    @Override
    protected void saveData(DataOut output) {
        output.putShort("age", (short) age);
        output.putShort("scale", (short) scale);
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    public void setMaxAge(int i) {
        this.entityData.set(MAX_AGE, i);
    }
}
