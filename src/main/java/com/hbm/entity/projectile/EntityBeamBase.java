package com.hbm.entity.projectile;

import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

public abstract class EntityBeamBase extends CompatEntity {

    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(EntityBeamBase.class, EntityDataSerializers.STRING);

    public EntityBeamBase(EntityType<? extends EntityBeamBase> type, Level level) {
        super(type, level);
    }

    public EntityBeamBase(EntityType<? extends EntityBeamBase> type, Level level, Player player) {
        this(type, level);

        this.setOwnerName(player.getDisplayName().getString());

        // 1.7 verbatim: the -90F angle is radians there and stays radians here (Vec3.rotateAroundY == Vec3.yRot).
        Vec3 vec = player.getLookAngle().yRot(-90F);
        float l = 0.075F;
        vec = new Vec3(vec.x * l, vec.y * l, vec.z * l);

        Vec3 vec0 = player.getLookAngle();
        float d = 0.1F;
        vec0 = new Vec3(vec0.x * d, vec0.y * d, vec0.z * d);

        this.setPos(player.getX() + vec.x + vec0.x,
                player.getY() + player.getEyeHeight() + vec0.y,
                player.getZ() + vec.z + vec0.z);
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(OWNER_NAME, "");
    }

    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    public void setOwnerName(String name) {
        this.entityData.set(OWNER_NAME, name);
    }

    // 26.2: 1.7's fullbright getBrightnessForRender/getBrightness have no common-code override left - the beam
    // renderer must emit full packed light itself.

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return true;
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void saveData(DataOut output) {
    }

    @Override
    protected void loadData(DataIn input) {
    }
}
