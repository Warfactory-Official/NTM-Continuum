package com.hbm.entity.effect;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * The Tsar-Bomba missile's expanding blast sphere - a pure visual whose geometry is a function of age and the synced
 * maxAge; frustum culling is the renderer's call.
 *
 * 1.7 was ignoreFrustumCheck + isImmuneToFire on both ctors, so register with .fireImmune() and a renderer that
 * never culls (1.7 id "entity_moonstone_blast", tracking range 1000).
 */
public class EntityCloudTom extends CompatEntity {

    // 1.7: dataWatcher slot 16
    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(EntityCloudTom.class, EntityDataSerializers.INT);

    /** 1.7's registered ctor sized(1, 4); only the missile-spawned one sized(20, 40), and it never synced. */
    private static final EntityDimensions MISSILE_SIZE = EntityDimensions.scalable(20.0F, 40.0F);

    public int maxAge = 100;
    public int age;
    private boolean missileSpawned;

    public EntityCloudTom(EntityType<? extends EntityCloudTom> type, Level level) {
        super(type, level);
        this.age = 0;
    }

    public static EntityCloudTom statFac(Level level, int maxAge, double x, double y, double z) {
        EntityCloudTom cloud = new EntityCloudTom(ModNtmEntities.MOONSTONE_BLAST.get(), level);
        cloud.setPos(x, y, z);
        cloud.setMaxAge(maxAge);
        cloud.missileSpawned = true;
        cloud.refreshDimensions();
        return cloud;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.missileSpawned ? MISSILE_SIZE : super.getDimensions(pose);
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
            this.discard();
        }
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    // 1.7: getBrightnessForRender/getBrightness fullbright moved into RenderCloudTom.

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return true; // 1.7: isInRangeToRenderDist
    }

    @Override
    protected void loadData(DataIn input) {
        age = input.getShortOr("age", (short) 0);
    }

    @Override
    protected void saveData(DataOut output) {
        output.putShort("age", (short) age);
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    public void setMaxAge(int i) {
        this.maxAge = i;
        this.entityData.set(MAX_AGE, i);
    }
}
