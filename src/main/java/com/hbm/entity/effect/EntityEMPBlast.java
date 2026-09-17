package com.hbm.entity.effect;

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
 * The expanding EMP shockwave ring (1.7 {@code EntityEMPBlast}) - a pure visual whose geometry is a
 * function of {@link #scale} and the synced maxAge. Spawned by EMP detonations, floating bombs and
 * tier-0 missile impacts.
 */
public class EntityEMPBlast extends CompatEntity {

    // 1.7: dataWatcher slot 16
    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(EntityEMPBlast.class, EntityDataSerializers.INT);

    public int age;
    public float scale = 0;

    public EntityEMPBlast(EntityType<? extends EntityEMPBlast> type, Level level) {
        super(type, level);
        // 1.7: setSize(1.5F, 1.5F) - belongs on the EntityType registration (.sized(1.5F, 1.5F))
        this.age = 0;
        this.scale = 0;
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(MAX_AGE, 0);
    }

    @Override
    public boolean fireImmune() {
        return true; // 1.7: isImmuneToFire = true
    }

    // 1.7: getBrightnessForRender/getBrightness forced full bright - the client renderer's job now

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return true; // 1.7: ignoreFrustumCheck - the ring outgrows its bounding box immediately
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false; // Visual effect; can't be damaged.
    }

    // 1.7: onUpdate without super - no movement/gravity; 1.7 died on both sides, removal syncs now
    @Override
    public void tick() {
        this.age++;

        if (!level().isClientSide() && this.age >= this.getMaxAge()) {
            this.age = 0;
            this.discard();
        }

        this.scale++;
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    public void setMaxAge(int i) {
        this.entityData.set(MAX_AGE, i);
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
}
