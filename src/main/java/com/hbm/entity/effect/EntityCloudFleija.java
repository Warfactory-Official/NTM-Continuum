package com.hbm.entity.effect;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * The Schrabidium blast's cyan shockwave sphere - a pure visual whose entire geometry is a function of age and the synced maxAge; frustum culling is the renderer's call.
 */
public class EntityCloudFleija extends CompatEntity {

    // 1.7: dataWatcher slot 16
    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(EntityCloudFleija.class, EntityDataSerializers.INT);

    public int age;
    public float scale = 0;

    public EntityCloudFleija(EntityType<? extends EntityCloudFleija> type, Level level) {
        super(type, level);
        this.age = 0;
        this.scale = 0;
    }

    public static EntityCloudFleija statFac(Level level, int maxAge, double x, double y, double z) {
        EntityCloudFleija cloud = new EntityCloudFleija(ModNtmEntities.CLOUD_FLEIJA.get(), level);
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
        // 1.7: lastLightningBolt = 2 - sky flashed white
        level().setSkyFlashTime(2);

        if (!level().isClientSide() && this.age >= this.getMaxAge()) {
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
