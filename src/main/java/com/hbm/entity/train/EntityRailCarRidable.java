package com.hbm.entity.train;

import dev.golem.core.data.CompatEntity;
import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Compat;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;


public abstract class EntityRailCarRidable extends EntityRailCarCargo {

    public double engineSpeed;
    public SeatDummyEntity[] passengerSeats;

    public EntityRailCarRidable(EntityType<? extends EntityRailCarRidable> type, Level level) {
        super(type, level);
        this.passengerSeats = new SeatDummyEntity[this.getPassengerSeats().length];
    }

    /** Returns the linear speed added per tick when using powered movement */
    public abstract double getPoweredAcceleration();
    /** A mulitplier used on the speed either is there is no player in the train or if the parking brake is active */
    public abstract double getPassivBrake();
    /** The parking brake can be toggled, assuming a player is present, otherwise it is implicitly ON */
    public abstract boolean shouldUseEngineBrake(Player player);
    /** The max speed the engine can provide in both directions */
    public abstract double getMaxPoweredSpeed();
    /** Whether the engine is turned on */
    public abstract boolean canAccelerate();
    /** Called every tick if acceleration is successful */
    public void consumeFuel() { }

    /** An additive to the engine's speed yielding the total speed, caused by uneven surfaces */
    public double getGravitySpeed() {
        return 0D;
    }

    @Override
    public double getCurrentSpeed() {

        if(this.getFirstPassenger() instanceof Player player) {

            int forward = 0;
            if(player instanceof ServerPlayer server) {
                forward = Compat.clientForwardInput(server);
            }

            if(this.canAccelerate()) {
                if(forward > 0) {
                    engineSpeed += this.getPoweredAcceleration();
                    this.consumeFuel();
                } else if(forward < 0) {
                    engineSpeed -= this.getPoweredAcceleration();
                    this.consumeFuel();
                } else {
                    if(this.shouldUseEngineBrake(player)) {
                        engineSpeed *= this.getPassivBrake();
                    } else {
                        this.consumeFuel();
                    }
                }
            } else {
                engineSpeed *= this.getPassivBrake();
            }

        } else {
            engineSpeed *= this.getPassivBrake();
        }

        double maxSpeed = this.getMaxPoweredSpeed();
        engineSpeed = Mth.clamp(engineSpeed, -maxSpeed, maxSpeed);

        return engineSpeed + this.getGravitySpeed();
    }

    @Override
    protected InteractionResult onInteract(Player player, InteractionHand hand) {

        InteractionResult coupled = super.onInteract(player, hand);
        if(coupled != InteractionResult.TRY_WITH_EMPTY_HAND) return coupled;
        if(level().isClientSide()) return InteractionResult.SUCCESS;

        int nearestSeat = this.getNearestSeat(player);

        if(nearestSeat == -1) {
            player.startRiding(this);
        } else if(nearestSeat >= 0) {
            SeatDummyEntity dummySeat = new SeatDummyEntity(ModNtmEntities.SEAT_DUMMY.get(), level(), this, nearestSeat);
            Vec3 seat = this.getPassengerSeats()[nearestSeat];
            rotate(seat, 0F, this.getYRot());
            dummySeat.setPos(renderX + rotatedX, renderY + rotatedY - 1, renderZ + rotatedZ);
            passengerSeats[nearestSeat] = dummySeat;
            level().addFreshEntity(dummySeat);
            player.startRiding(dummySeat);
        }

        return InteractionResult.SUCCESS;
    }

    public int getNearestSeat(Player player) {

        double nearestDist = Double.POSITIVE_INFINITY;
        int nearestSeat = -3;

        Vec3[] seats = getPassengerSeats();
        Vec3 look = player.getEyePosition().add(player.getViewVector(1F));

        for(int i = 0; i < seats.length; i++) {

            Vec3 seat = seats[i];
            if(seat == null) continue;
            if(passengerSeats[i] != null) continue;

            rotate(seat, 0F, this.getYRot());
            double dist = look.distanceTo(new Vec3(renderX + rotatedX, renderY + rotatedY, renderZ + rotatedZ));

            if(dist < nearestDist) {
                nearestDist = dist;
                nearestSeat = i;
            }
        }

        if(this.getFirstPassenger() == null) {
            rotate(getRiderSeatPosition(), 0F, this.getYRot());
            double dist = look.distanceTo(new Vec3(renderX + rotatedX, renderY + rotatedY, renderZ + rotatedZ));

            if(dist < nearestDist) {
                nearestDist = dist;
                nearestSeat = -1;
            }
        }

        if(nearestDist > 180) return -2;

        return nearestSeat;
    }

    @Override
    public void tick() {
        super.tick();

        if(!level().isClientSide()) {

            Vec3[] seats = this.getPassengerSeats();
            for(int i = 0; i < passengerSeats.length; i++) {
                SeatDummyEntity seat = passengerSeats[i];

                if(seat != null) {
                    if(seat.getFirstPassenger() == null) {
                        passengerSeats[i] = null;
                        seat.discard();
                    } else {
                        rotate(seats[i], this.getXRot(), this.getYRot());
                        seat.setPos(renderX + rotatedX, renderY + rotatedY - 1, renderZ + rotatedZ);
                    }
                }
            }
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        rotate(getRiderSeatPosition(), this.getXRot(), this.getYRot());
        moveFunction.accept(passenger, renderX + rotatedX, renderY + rotatedY, renderZ + rotatedZ);
    }

    /**
     * 1.7's seat vectors were mutated in place by {@code rotateAroundY}, so every accessor had to hand out a
     * fresh array. 26.1's Vec3 is immutable, which lets the seat table be a constant - the rotation lands here.
     */
    protected double rotatedX, rotatedY, rotatedZ;

    protected void rotate(Vec3 offset, float pitchDeg, float yawDeg) {
        float pitch = (float) (pitchDeg * Math.PI / 180D);
        float yaw = (float) (-yawDeg * Math.PI / 180D);
        float cp = Mth.cos(pitch), sp = Mth.sin(pitch);
        float cy = Mth.cos(yaw), sy = Mth.sin(yaw);

        double py = offset.y * cp + offset.z * sp;
        double pz = offset.z * cp - offset.y * sp;

        this.rotatedX = offset.x * cy + pz * sy;
        this.rotatedY = py;
        this.rotatedZ = pz * cy - offset.x * sy;
    }

    /** Returns a Vec3 showing the relative position from the driver to the core */
    public abstract Vec3 getRiderSeatPosition();

    /** Constant per class - see {@link #rotate}. */
    public abstract Vec3[] getPassengerSeats();

    /** Dynamic seats generated when a player clicks near a seat-spot, moves and rotates with the train as one would expect. */
    public static class SeatDummyEntity extends CompatEntity {

        private static final EntityDataAccessor<Integer> TRAIN_ID = SynchedEntityData.defineId(SeatDummyEntity.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Integer> SEAT_INDEX = SynchedEntityData.defineId(SeatDummyEntity.class, EntityDataSerializers.INT);

        public EntityRailCarRidable train;

        public SeatDummyEntity(EntityType<? extends SeatDummyEntity> type, Level level) {
            super(type, level);
        }

        public SeatDummyEntity(EntityType<? extends SeatDummyEntity> type, Level level, EntityRailCarRidable train, int index) {
            this(type, level);
            this.train = train;
            if(train != null) this.entityData.set(TRAIN_ID, train.getId());
            this.entityData.set(SEAT_INDEX, index);
        }

        @Override
        protected void defineData(SynchedData builder) {
            builder.define(TRAIN_ID, 0);
            builder.define(SEAT_INDEX, 0);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(0.5F, 0.1F);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        protected void loadData(DataIn input) {
            // 1.7 readEntityFromNBT -> setDead
            this.discard();
        }

        @Override
        protected void saveData(DataOut output) { }

        @Override
        protected boolean onHurt(DamageSource source, float damage) {
            return false;
        }

        @Override
        public void tick() {
            if(!level().isClientSide()) {
                if(this.train == null || this.train.isRemoved()) this.discard();
            }
        }

        /**
         * 1.7: "doing it like this instead of with the position directly removes any discrepancies caused by
         * entity tick order - mmhmhmhm silky smooth".
         */
        @Override
        protected void positionRider(Entity passenger, MoveFunction moveFunction) {

            if(train == null && level().getEntity(this.entityData.get(TRAIN_ID)) instanceof EntityRailCarRidable car) {
                train = car;
            }

            if(train == null) {
                moveFunction.accept(passenger, getX(), getY() + 1, getZ());
                return;
            }

            Vec3[] seats = train.getPassengerSeats();
            int index = this.entityData.get(SEAT_INDEX);
            if(index < 0 || index >= seats.length) {
                moveFunction.accept(passenger, getX(), getY() + 1, getZ());
                return;
            }

            train.rotate(seats[index], train.getXRot(), train.getYRot());
            moveFunction.accept(passenger, train.renderX + train.rotatedX, train.renderY + train.rotatedY,
                    train.renderZ + train.rotatedZ);
        }
    }
}
