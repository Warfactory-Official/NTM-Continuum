package com.hbm.entity.item;

import dev.golem.core.data.Tags;

import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import dev.golem.core.world.EntityInterpolation;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code entity/item/EntityDroneBase}: the shared base for the delivery network's quadcopters. Flies a
 * straight line to a waypoint, climbing when it bumps a wall, and carries its cargo appearance as synched data.
 *
 * <p>1.7's hand-rolled {@code turnProgress} client lerp is dropped in favour of 26.1's
 * {@link EntityInterpolation}, which is the same mechanism.
 */
public abstract class EntityDroneBase extends CompatEntity {

    // 1.7 dataWatcher id 10
    private static final EntityDataAccessor<Byte> APPEARANCE =
            SynchedEntityData.defineId(EntityDroneBase.class, EntityDataSerializers.BYTE);

    private final EntityInterpolation interpolation = new EntityInterpolation(this);

    public double targetX = -1;
    public double targetY = -1;
    public double targetZ = -1;

    protected EntityDroneBase(EntityType<? extends EntityDroneBase> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(APPEARANCE, (byte) 0);
    }

    @Override
    protected EntityInterpolation interpolation() {
        return this.interpolation;
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    @Override
    public boolean isPickable() {
        return true; // 1.7 canBeCollidedWith
    }

    @Override
    public boolean isAttackable() {
        return true; // 1.7 canAttackWithItem
    }

    /** 1.7 hitByEntity: a player's punch drops the drone out of the sky. Subclasses spill their cargo first. */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player) discard();
        return false;
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false; // 1.7: attackEntityFrom inherited from Entity - drones only die to a punch
    }

    /**
     * 0: empty<br>
     * 1: crate<br>
     * 2: barrel
     */
    public void setAppearance(int style) {
        entityData.set(APPEARANCE, (byte) style);
    }

    public int getAppearance() {
        return entityData.get(APPEARANCE);
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        super.tick();

        if (level().isClientSide()) {
            for (Vec3 offset : ROTOR_OFFSETS) {
                level().addParticle(ParticleTypes.SMOKE,
                        getX() + offset.x, getY() + 0.75, getZ() + offset.z, 0, -0.2, 0);
            }
            return;
        }

        setDeltaMovement(Vec3.ZERO);

        if (this.targetY != -1) {
            Vec3 delta = new Vec3(targetX - getX(), targetY - getY(), targetZ - getZ());
            double speed = Math.min(getSpeed(), delta.length());
            setDeltaMovement(delta.normalize().scale(speed));
        }

        // 1.7: bumping a wall makes the drone climb until it clears it
        if (this.horizontalCollision) {
            setDeltaMovement(getDeltaMovement().add(0, 1, 0));
        }

        loadNeighboringChunks();
        move(MoverType.SELF, getDeltaMovement());
    }

    private static final Vec3[] ROTOR_OFFSETS = {
            new Vec3(1.125, 0, 0), new Vec3(-1.125, 0, 0),
            new Vec3(0, 0, 1.125), new Vec3(0, 0, -1.125)
    };

    protected void loadNeighboringChunks() {
    }

    public double getSpeed() {
        return 0.125D;
    }

    @Override
    protected void loadData(DataIn input) {
        if (input.getDoubleOr("tY", -1) != -1) {
            this.targetX = input.getDoubleOr("tX", -1);
            this.targetY = input.getDoubleOr("tY", -1);
            this.targetZ = input.getDoubleOr("tZ", -1);
        }
        entityData.set(APPEARANCE, (byte) input.getIntOr("app", 0));
    }

    @Override
    protected void saveData(DataOut output) {
        output.putDouble("tX", targetX);
        output.putDouble("tY", targetY);
        output.putDouble("tZ", targetZ);
        output.putInt("app", getAppearance());
    }
}
