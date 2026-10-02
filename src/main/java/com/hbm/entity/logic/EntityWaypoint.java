package com.hbm.entity.logic;

import dev.golem.core.data.Tags;

import com.hbm.entity.ModNtmEntities;
import com.hbm.registration.Reg;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.registry.Entities;
import com.hbm.config.MobConfig;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class EntityWaypoint extends CompatEntity {

    private static final Reg REG = Reg.of(ModNtmEntities.ENTITY_TYPES);

    public static final RegistryObject<EntityType<EntityWaypoint>> TYPE =
            REG.entity("entity_waypoint", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityWaypoint>of(EntityWaypoint::new, MobCategory.MISC)).fireImmune().sized(0.1F, 0.1F).clientTrackingRange(16).updateInterval(1), key));

    private static final EntityDataAccessor<Integer> WAYPOINT_TYPE =
            SynchedEntityData.defineId(EntityWaypoint.class, EntityDataSerializers.INT);

    public int maxAge = 2400;
    public int radius = 3;
    public boolean highPriority = false;
    protected EntityWaypoint additional;
    private boolean hasSpawned = false;

    public EntityWaypoint(EntityType<? extends EntityWaypoint> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(WAYPOINT_TYPE, 0);
    }

    public int getWaypointType() {
        return entityData.get(WAYPOINT_TYPE);
    }

    public void setWaypointType(int waypointType) {
        entityData.set(WAYPOINT_TYPE, waypointType);
    }

    public void setHighPriority() {
        highPriority = true;
    }

    public void setAdditionalWaypoint(EntityWaypoint waypoint) {
        additional = waypoint;
    }

    public int getColor() {
        int type = getWaypointType();
        if (type == EntityGlyphid.TASK_RETREAT_FOR_REINFORCEMENTS) return 0x5FA6E8;
        if (type == EntityGlyphid.TASK_BUILD_HIVE || type == EntityGlyphid.TASK_INITIATE_RETREAT) return 0x127766;
        return 0x566573;
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount >= maxAge) {
            discard();
        }

        AABB bb = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(radius);

        if (!level().isClientSide()) {

            if (tickCount % 40 == 0) {

                List<Entity> targets = level().getEntities(this, bb);

                for (Entity e : targets) {
                    if (e instanceof EntityGlyphid bug) {

                        if (additional != null && !hasSpawned) {
                            level().addFreshEntity(additional);
                            hasSpawned = true;
                        }

                        boolean exceptions = bug.getWaypoint() != this || bug.isScoutType() || bug.isNuclearType();

                        if (!exceptions) bug.setCurrentTask(getWaypointType(), additional);

                        if (getWaypointType() == EntityGlyphid.TASK_BUILD_HIVE) {
                            if (bug.isScoutType()) discard();
                        } else {
                            discard();
                        }
                    }
                }
            }
        } else if (MobConfig.waypointDebug) {

            double x = bb.minX + (random.nextDouble() - 0.5) * (bb.maxX - bb.minX);
            double y = bb.minY + random.nextDouble() * (bb.maxY - bb.minY);
            double z = bb.minZ + (random.nextDouble() - 0.5) * (bb.maxZ - bb.minZ);

            level().addParticle(dev.golem.core.world.Compat.dust(getColor(), 1.0F), x, y, z, 0D, 0D, 0D);
        }
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void loadData(DataIn input) {
        setWaypointType(input.getIntOr("type", 0));
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("type", getWaypointType());
    }
}
