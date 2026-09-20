package com.hbm.entity.train;

import com.hbm.items.ModEntityItems;
import com.hbm.entity.ModNtmEntities;
import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.blocks.rail.BlockRailNTM;
import com.hbm.blocks.rail.IRailNTM.MoveContext;
import com.hbm.blocks.rail.IRailNTM.RailCheckType;
import com.hbm.blocks.rail.IRailNTM.RailContext;
import com.hbm.blocks.rail.IRailNTM.TrackGauge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import dev.golem.core.world.EntityInterpolation;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class EntityRailCarBase extends CompatEntity {

    public LogicalTrainUnit ltu;
    public int ltuIndex = 0;
    public boolean isOnRail = true;
    /* "Actual" position with offset directly between the front and back pos, won't match the standard position on curves */
    public double lastRenderX;
    public double lastRenderY;
    public double lastRenderZ;
    public double renderX;
    public double renderY;
    public double renderZ;
    public double cachedSpeed;

    public EntityRailCarBase coupledFront;
    public EntityRailCarBase coupledBack;

    public boolean initDummies = false;
    public BoundingBoxDummyEntity[] dummies = new BoundingBoxDummyEntity[0];

    /** The rail walk's working set. Owned per entity, and an entity belongs to one thread. */
    private final RailContext railInfo = new RailContext();
    private final MoveContext railMove = new MoveContext(RailCheckType.CORE, 0);
    /** {@link #rotateOffset}'s answer */
    private double rotX, rotY, rotZ;

    private final EntityInterpolation interp = new EntityInterpolation(this);

    public EntityRailCarBase(EntityType<? extends EntityRailCarBase> type, Level level) {
        super(type, level);
    }

    @Override
    protected void loadData(DataIn input) { }

    @Override
    protected void saveData(DataOut output) { }

    @Override
    protected boolean onHurt(DamageSource source, float damage) {
        return false;
    }

    @Override
    public void tick() {

        this.interp.interpolate();

        if(this.level().isClientSide()) {

            BlockPos anchor = this.getCurrentAnchorPos();
            boolean front = walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), this.getLengthSpan(), RailCheckType.FRONT, this.getCollisionSpan() - this.getLengthSpan());
            double frontX = railInfo.x, frontY = railInfo.y, frontZ = railInfo.z;
            boolean back = walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), -this.getLengthSpan(), RailCheckType.BACK, this.getCollisionSpan() - this.getLengthSpan());

            this.lastRenderX = this.renderX;
            this.lastRenderY = this.renderY;
            this.lastRenderZ = this.renderZ;

            if(front && back) {
                this.renderX = (frontX + railInfo.x) / 2D;
                this.renderY = (frontY + railInfo.y) / 2D;
                this.renderZ = (frontZ + railInfo.z) / 2D;
            } else {
                this.renderX = getX();
                this.renderY = getY();
                this.renderZ = getZ();
            }

        } else {

            if(!this.isOnRail) {
                if(this.coupledFront != null) this.coupledFront.couple(this.coupledFront.getCouplingFrom(this), null);
                if(this.coupledBack != null) this.coupledBack.couple(this.coupledBack.getCouplingFrom(this), null);
                this.coupledFront = null;
                this.coupledBack = null;
            }

            if(this.coupledFront != null && this.coupledFront.isRemoved()) {
                this.coupledFront = null;
                if(this.ltu != null) this.ltu.dissolveTrain();
            }
            if(this.coupledBack != null && this.coupledBack.isRemoved()) {
                this.coupledBack = null;
                if(this.ltu != null) this.ltu.dissolveTrain();
            }

            if(this.ltu == null && (this.coupledFront == null || this.coupledBack == null) && this.isOnRail) {
                LogicalTrainUnit.generateTrain(this);
            }

            if(!this.isOnRail) {
                rotateOffset(0, 0, this.cachedSpeed, 0F, this.getYRot());
                this.setDeltaMovement(rotX, rotY - 0.04, rotZ);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.renderX = getX();
                this.renderY = getY();
                this.renderZ = getZ();
                this.cachedSpeed *= 0.95D;
            }

            DummyConfig[] definitions = this.getDummies();

            if(!this.initDummies) {
                this.dummies = new BoundingBoxDummyEntity[definitions.length];

                for(int i = 0; i < definitions.length; i++) {
                    DummyConfig def = definitions[i];
                    BoundingBoxDummyEntity dummy = new BoundingBoxDummyEntity(ModNtmEntities.BOUNDING_DUMMY.get(), this.level(), this, def.width, def.height);
                    rotateOffset(def.offsetX, def.offsetY, def.offsetZ, 0F, this.getYRot());
                    dummy.snapTo(getX() + rotX, getY() + rotY, getZ() + rotZ, 0F, 0F);
                    this.level().addFreshEntity(dummy);
                    this.dummies[i] = dummy;
                }

                this.initDummies = true;
            }

            if(renderY != 0) {
                for(int i = 0; i < definitions.length; i++) {
                    DummyConfig def = definitions[i];
                    rotateOffset(def.offsetX, def.offsetY, def.offsetZ, this.getXRot(), this.getYRot());
                    dummies[i].setPos(renderX + rotX, renderY + rotY, renderZ + rotZ);
                }
            }

            // 1.7: swept the level's whole entity list per tick; the cars announce themselves instead
            LogicalTrainUnit.TICKED.add(this);
        }
    }

    /**
     * {@code new Vec3(x, y, z).xRot(pitch).yRot(-yaw)} spelled out into {@link #rotX}/{@link #rotY}/{@link #rotZ}.
     * Table trig, so the result matches what the two allocating calls produced.
     */
    private void rotateOffset(double x, double y, double z, float pitchDeg, float yawDeg) {
        float pitch = (float) (pitchDeg * Math.PI / 180D);
        float yaw = (float) (-yawDeg * Math.PI / 180D);
        float cp = Mth.cos(pitch), sp = Mth.sin(pitch);
        float cy = Mth.cos(yaw), sy = Mth.sin(yaw);

        double py = y * cp + z * sp;
        double pz = z * cp - y * sp;

        this.rotX = x * cy + pz * sy;
        this.rotY = py;
        this.rotZ = pz * cy - x * sy;
    }

    /**
     * Walks {@code distanceToCover} blocks along the rail starting from the anchor block, leaving the result in
     * {@link #railInfo}'s {@code x}/{@code y}/{@code z}. False where 1.7 returned null: the train left the rail.
     */
    public boolean walkRail(int anchorX, int anchorY, int anchorZ, double distanceToCover, RailCheckType type, double collisionBogieDistance) {
        railMove.set(type, collisionBogieDistance);
        return walkRail(this.level(), this.getGauge(), railInfo, railMove, anchorX, anchorY, anchorZ,
                getX(), getY(), getZ(), this.getYRot(), distanceToCover);
    }

    /** Where the last {@link #walkRail} landed. Valid until the next walk overwrites it. */
    public RailContext railResult() {
        return this.railInfo;
    }

    /**
     * The rail walk proper. {@code info} carries both the per-step hand-off and the answer, so nothing here is
     * allocated; on success {@code info.x/y/z} is the position reached.
     */
    public static boolean walkRail(Level level, TrackGauge gauge, RailContext info, MoveContext context,
                                   int anchorX, int anchorY, int anchorZ,
                                   double startX, double startY, double startZ, float yaw, double distanceToCover) {

        if(distanceToCover < 0) {
            distanceToCover *= -1;
            yaw += 180;
        }

        double nextX = startX, nextY = startY, nextZ = startZ;
        int it = 0;

        do {

            it++;

            if(it > 30) {
                return false;
            }

            // 1.7: asked the block at the anchor directly; only the folded core is the rail block now, so resolve through the core index
            if(!BlockRailNTM.railAt(level, anchorX, anchorY, anchorZ, info.owner)) return false;
            BlockRailNTM rail = info.owner.rail;

            // new Vec3(0, 0, 1).yRot(-yaw): the unit heading, without the vector
            float rad = (float) (-yaw * Math.PI / 180D);
            double motionX = Mth.sin(rad);
            double motionZ = Mth.cos(rad);

            if(it == 1) {
                info.reset();
                rail.getTravelLocation(level, anchorX, anchorY, anchorZ, nextX, nextY, nextZ, motionX, 0, motionZ, 0, info, context);
                nextX = info.x;
                nextY = info.y;
                nextZ = info.z;
            }

            boolean flip = distanceToCover < 0;

            if(rail.getGauge(level, anchorX, anchorY, anchorZ) != gauge) return false;

            double prevX = nextX, prevY = nextY, prevZ = nextZ;
            info.reset();
            rail.getTravelLocation(level, anchorX, anchorY, anchorZ, prevX, prevY, prevZ, motionX, 0, motionZ, distanceToCover, info, context);
            nextX = info.x;
            nextY = info.y;
            nextZ = info.z;
            distanceToCover = info.overshoot;
            if(info.hasPos) {
                anchorX = info.posX;
                anchorY = info.posY;
                anchorZ = info.posZ;
            }

            yaw = generateYaw(nextX, nextZ, prevX, prevZ) * (flip ? -1 : 1);

        } while(distanceToCover != 0); //if there's still length to cover, keep going

        info.at(nextX, nextY, nextZ);
        return true;
    }

    public static float generateYaw(Vec3 front, Vec3 back) {
        return generateYaw(front.x, front.z, back.x, back.z);
    }

    public static float generateYaw(double frontX, double frontZ, double backX, double backZ) {
        double deltaX = frontX - backX;
        double deltaZ = frontZ - backZ;
        double radians = -Math.atan2(deltaX, deltaZ);
        return Mth.wrapDegrees((float) (radians * 180D / Math.PI));
    }

    /** 1.7 ModEventHandler worldTick END. A car that is not ticking - unloaded chunk - is not moved. */
    public static void updateTrains(MinecraftServer server) {
        LogicalTrainUnit.updateTicked();
    }

    /** Returns the amount of blocks that the train should move per tick */
    public abstract double getCurrentSpeed();
    public abstract double getMaxRailSpeed();
    /** Returns the gauge of this train */
    public abstract TrackGauge getGauge();
    /** Returns the length between the core and one of the bogies */
    public abstract double getLengthSpan();
    /** Returns the length between the core and the collision points */
    public abstract double getCollisionSpan();

    /** Returns the "true" position of the train, i.e. the block it wants to snap to */
    public BlockPos getCurrentAnchorPos() {
        return BlockPos.containing(getX(), getY() + 0.25, getZ());
    }

    public void derail() {
        isOnRail = false;
    }

    @Override
    protected EntityInterpolation interpolation() {
        return this.interp;
    }

    /** Invisible entities that make up the dynamic bounding structure of the train, moving as the train rotates. */
    public static class BoundingBoxDummyEntity extends CompatEntity {

        private static final EntityDataAccessor<Integer> TRAIN_ID = SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.FLOAT);
        private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.FLOAT);

        public EntityRailCarBase train;

        public BoundingBoxDummyEntity(EntityType<? extends BoundingBoxDummyEntity> type, Level level) {
            super(type, level);
        }

        public BoundingBoxDummyEntity(EntityType<? extends BoundingBoxDummyEntity> type, Level level, EntityRailCarBase train, float width, float height) {
            super(type, level);
            this.train = train;
            this.entityData.set(TRAIN_ID, train != null ? train.getId() : 0);
            setSize(width, height);
        }

        private void setSize(float width, float height) {
            if(this.entityData.get(WIDTH) == width && this.entityData.get(HEIGHT) == height) return;
            this.entityData.set(WIDTH, width);
            this.entityData.set(HEIGHT, height);
            refreshDimensions();
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(this.entityData.get(WIDTH), this.entityData.get(HEIGHT));
        }

        @Override
        protected void defineData(SynchedData builder) {
            builder.define(TRAIN_ID, 0);
            builder.define(WIDTH, 1.0F);
            builder.define(HEIGHT, 1.0F);
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
        public boolean isPushable() {
            return true;
        }

        @Override
        public boolean isPickable() {
            return !this.isRemoved();
        }

        @Override
        protected boolean onHurt(DamageSource source, float damage) {
            return train != null && level() instanceof ServerLevel server && Compat.hurt(train, server, source, damage);
        }

        @Override
        protected InteractionResult onInteract(Player player, InteractionHand hand) {
            if(train != null) return train.onInteract(player, hand);
            return super.onInteract(player, hand);
        }

        @Override
        public void tick() {
            if(!this.level().isClientSide()) {
                if(this.train == null || this.train.isRemoved()) {
                    this.discard();
                }
            } else {
                setSize(this.entityData.get(WIDTH), this.entityData.get(HEIGHT));
            }
        }
    }

    private static final DummyConfig[] NO_DUMMIES = new DummyConfig[0];

    public DummyConfig[] getDummies() {
        return NO_DUMMIES;
    }

    public record DummyConfig(float width, float height, double offsetX, double offsetY, double offsetZ) { }

    public enum TrainCoupling {
        FRONT,
        BACK
    }

    public double getCouplingDist(TrainCoupling coupling) {
        return 0D;
    }

    /** Writes the coupling point into {@code out}; false where 1.7 returned null, i.e. this end does not couple. */
    public boolean getCouplingPos(TrainCoupling coupling, double[] out) {
        double dist = this.getCouplingDist(coupling);

        if(dist <= 0) return false;

        if(coupling == TrainCoupling.BACK) dist *= -1;

        rotateOffset(0, 0, dist, 0F, this.getYRot());
        out[0] = rotX + this.renderX;
        out[1] = rotY + this.renderY;
        out[2] = rotZ + this.renderZ;
        return true;
    }

    private final double[] couplingScratch = new double[3];

    public Vec3 getCouplingPos(TrainCoupling coupling) {
        if(!getCouplingPos(coupling, couplingScratch)) return null;
        return new Vec3(couplingScratch[0], couplingScratch[1], couplingScratch[2]);
    }

    public EntityRailCarBase getCoupledTo(TrainCoupling coupling) {
        return coupling == TrainCoupling.FRONT ? this.coupledFront : coupling == TrainCoupling.BACK ? this.coupledBack : null;
    }

    /** A null argument asks which coupling is FREE; both taken answers null. */
    public TrainCoupling getCouplingFrom(EntityRailCarBase coupledTo) {
        return coupledTo == this.coupledFront ? TrainCoupling.FRONT : coupledTo == this.coupledBack ? TrainCoupling.BACK : null;
    }

    public void couple(TrainCoupling coupling, EntityRailCarBase to) {
        if(coupling == TrainCoupling.FRONT) this.coupledFront = to;
        if(coupling == TrainCoupling.BACK) this.coupledBack = to;
    }

    /**
     * The coupling tool interaction. Returns PASS so subclasses can layer their own use behaviour.
     */
    @Override
    protected InteractionResult onInteract(Player player, InteractionHand hand) {

        ItemStack held = player.getItemInHand(hand);

        if(held.is(ModEntityItems.coupling_tool.get())) {

            List<EntityRailCarBase> intersecting = this.level().getEntitiesOfClass(EntityRailCarBase.class, this.getBoundingBox().inflate(2D, 0D, 2D));

            for(EntityRailCarBase neighbor : intersecting) {
                if(neighbor == this) continue;
                if(neighbor.getGauge() != this.getGauge()) continue;

                TrainCoupling closestOwnCoupling = null;
                TrainCoupling closestNeighborCoupling = null;
                double closestDist = Double.POSITIVE_INFINITY;

                for(TrainCoupling ownCoupling : TrainCoupling.values()) {
                    for(TrainCoupling neighborCoupling : TrainCoupling.values()) {
                        Vec3 ownPos = this.getCouplingPos(ownCoupling);
                        Vec3 neighborPos = neighbor.getCouplingPos(neighborCoupling);
                        if(ownPos != null && neighborPos != null) {
                            double length = ownPos.subtract(neighborPos).length();

                            if(length < 1 && length < closestDist) {
                                closestDist = length;
                                closestOwnCoupling = ownCoupling;
                                closestNeighborCoupling = neighborCoupling;
                            }
                        }
                    }
                }

                if(closestOwnCoupling != null && closestNeighborCoupling != null) {
                    if(this.getCoupledTo(closestOwnCoupling) != null) continue;
                    if(neighbor.getCoupledTo(closestNeighborCoupling) != null) continue;
                    this.couple(closestOwnCoupling, neighbor);
                    neighbor.couple(closestNeighborCoupling, this);
                    if(this.ltu != null) this.ltu.dissolveTrain();
                    if(neighbor.ltu != null) neighbor.ltu.dissolveTrain();
                    player.swing(hand);

                    player.sendSystemMessage(Component.literal(
                            "Coupled " + this.hashCode() + " (" + closestOwnCoupling.name() + ") to " + neighbor.hashCode() + " (" + closestNeighborCoupling.name() + ")"));

                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    public static class LogicalTrainUnit {

        private static final EntityTypeTest<Entity, EntityRailCarBase> RAIL_CARS = EntityTypeTest.forClass(EntityRailCarBase.class);

        /** Cars that ticked this tick, in tick order. Server thread only, drained every tick. */
        static final List<EntityRailCarBase> TICKED = new ArrayList<>();
        /** Reused by {@link #collideTrain}; the query would otherwise mint a list per LTU per tick. */
        private static final List<EntityRailCarBase> COLLIDE_HITS = new ArrayList<>();
        /** Monotonic, so an LTU spanning several ticking cars is moved once - 1.7 deduplicated with a HashSet. */
        private static long updateStamp;

        private long stamp;
        protected double pushForce;
        protected EntityRailCarBase trains[];

        /** The two coupling points {@link #moveWagonTo} compares; long-lived so the comparison is allocation-free. */
        private final double[] couplingA = new double[3];
        private final double[] couplingB = new double[3];

        static void updateTicked() {
            if(TICKED.isEmpty()) return;
            long stamp = ++updateStamp;

            for(int i = 0; i < TICKED.size(); i++) {
                EntityRailCarBase car = TICKED.get(i);
                LogicalTrainUnit ltu = car.ltu;
                if(car.isRemoved() || ltu == null || ltu.stamp == stamp) continue;
                ltu.stamp = stamp;
                ltu.updateMotion();
            }

            TICKED.clear();
        }

        private void updateMotion() {

            double speed = this.getTotalSpeed() + this.pushForce;

            if(Math.abs(speed) < 0.001) speed = 0;

            for(EntityRailCarBase car : this.trains) car.cachedSpeed = speed;

            if(this.trains.length == 1) {

                EntityRailCarBase train = this.trains[0];

                // 1.7: the core walk anchors on the plain floor; the +0.25 anchor serves the bogeys alone
                BlockPos anchor = train.blockPosition();
                if(!train.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), speed, RailCheckType.CORE, 0)) {
                    train.derail();
                    this.dissolveTrain();
                    return;
                }
                train.setPos(train.railInfo.x, train.railInfo.y, train.railInfo.z);
                anchor = train.getCurrentAnchorPos();

                if(!train.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), train.getLengthSpan(), RailCheckType.FRONT, train.getCollisionSpan() - train.getLengthSpan())) {
                    train.derail();
                    this.dissolveTrain();
                    return;
                }
                double frontX = train.railInfo.x, frontY = train.railInfo.y, frontZ = train.railInfo.z;

                if(!train.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), -train.getLengthSpan(), RailCheckType.BACK, train.getCollisionSpan() - train.getLengthSpan())) {
                    train.derail();
                    this.dissolveTrain();
                    return;
                }

                setRenderPos(train, frontX, frontY, frontZ, train.railInfo.x, train.railInfo.y, train.railInfo.z);

                this.pushForce = 0;
                this.collideTrain(speed);

                return;
            }

            if(speed == 0) {
                this.combineWagons();
            } else {
                this.moveTrainByApproach(speed);
            }

            this.pushForce = 0;
            this.collideTrain(speed);
        }

        /** Assumes that the train is an endpoint, i.e. that only one coupling is in use */
        public static LogicalTrainUnit generateTrain(EntityRailCarBase train) {
            LogicalTrainUnit ltu = new LogicalTrainUnit();

            if(train.coupledFront == null && train.coupledBack == null) {
                ltu.trains = new EntityRailCarBase[] {train};
                train.ltu = ltu;
                train.ltuIndex = 0;
                return ltu;
            }

            List<EntityRailCarBase> links = new ArrayList<>();
            Set<EntityRailCarBase> brake = new HashSet<>();
            EntityRailCarBase current = train;
            EntityRailCarBase next = null;

            do {
                next = null;

                if(current.coupledFront != null && !brake.contains(current.coupledFront)) next = current.coupledFront;
                if(current.coupledBack != null && !brake.contains(current.coupledBack)) next = current.coupledBack;

                links.add(current);
                brake.add(current);

                current = next;

            } while(next != null);

            ltu.trains = new EntityRailCarBase[links.size()];
            for(int i = 0; i < ltu.trains.length; i++) {
                ltu.trains[i] = links.get(i);
                ltu.trains[i].ltu = ltu;
                ltu.trains[i].ltuIndex = i;
            }

            return ltu;
        }

        /** Removes the LTU from all wagons */
        public void dissolveTrain() {
            for(EntityRailCarBase train : trains) {
                train.ltu = null;
                train.ltuIndex = 0;
            }
        }

        /** Find the center of the train, then moves all wagons towards that center until the coupling points roughly touch */
        public void combineWagons() {

            if(trains.length <= 1) return;

            boolean odd = trains.length % 2 == 1;
            int centerIndex = odd ? trains.length / 2 : trains.length / 2 - 1;
            EntityRailCarBase center = trains[centerIndex];
            EntityRailCarBase prev = center;

            for(int i = centerIndex - 1; i >= 0; i--) {
                EntityRailCarBase next = trains[i];
                moveWagonTo(prev, next);
                prev = next;
            }

            prev = center;
            for(int i = centerIndex + 1; i < trains.length; i++) {
                EntityRailCarBase next = trains[i];
                moveWagonTo(prev, next);
                prev = next;
            }
        }

        /** Moves one wagon to ne next until the coupling points roughly touch */
        public void moveWagonTo(EntityRailCarBase moveTo, EntityRailCarBase moving) {
            TrainCoupling prevCouple = moveTo.getCouplingFrom(moving);
            TrainCoupling nextCouple = moving.getCouplingFrom(moveTo);
            if(!moveTo.getCouplingPos(prevCouple, couplingA)) return;
            if(!moving.getCouplingPos(nextCouple, couplingB)) return;

            double deltaX = couplingA[0] - couplingB[0];
            double deltaZ = couplingA[2] - couplingB[2];
            double len = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            len = (len / (0.5D / (len * len) + 1D)); //smart suspension

            float yaw = generateYaw(couplingA[0], couplingA[2], couplingB[0], couplingB[2]);
            moving.railMove.set(RailCheckType.CORE, 0);
            boolean moved = walkRail(moving.level(), moving.getGauge(), moving.railInfo, moving.railMove,
                    Mth.floor(moving.getX()), Mth.floor(moving.getY()), Mth.floor(moving.getZ()),
                    moving.getX(), moving.getY(), moving.getZ(), yaw, len);
            if(!moved) return;
            moving.setPos(moving.railInfo.x, moving.railInfo.y, moving.railInfo.z);

            BlockPos anchor = moving.getCurrentAnchorPos(); //reset origin to new position

            if(!moving.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), moving.getLengthSpan(), RailCheckType.FRONT, moving.getCollisionSpan() - moving.getLengthSpan())) {
                moving.derail();
                this.dissolveTrain();
                return;
            }
            double frontX = moving.railInfo.x, frontY = moving.railInfo.y, frontZ = moving.railInfo.z;

            if(!moving.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), -moving.getLengthSpan(), RailCheckType.BACK, moving.getCollisionSpan() - moving.getLengthSpan())) {
                moving.derail();
                this.dissolveTrain();
                return;
            }

            setRenderPos(moving, frontX, frontY, frontZ, moving.railInfo.x, moving.railInfo.y, moving.railInfo.z);
        }

        /** Determines the "front" wagon based on the movement and moves it, then moves all other wagons towards that */
        public void moveTrainByApproach(double speed) {
            EntityRailCarBase previous = null;
            EntityRailCarBase first = this.trains[0];
            boolean order = (speed > 0) ^ first.getCouplingFrom(null) == TrainCoupling.BACK;

            for(int i = order ? 0 : this.trains.length - 1; order ? i < this.trains.length : i >= 0; i += order ? 1 : -1) {
                EntityRailCarBase current = this.trains[i];

                if(previous == null) {

                    if(first == current) speed *= -1;

                    boolean inReverse = first.getCouplingFrom(null) == current.getCouplingFrom(null);
                    int sigNum = inReverse ? 1 : -1;
                    BlockPos anchor = current.getCurrentAnchorPos();

                    if(!current.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), (speed + current.getLengthSpan()) * -sigNum, RailCheckType.FRONT, current.getCollisionSpan() - current.getLengthSpan())) {
                        current.derail();
                        this.dissolveTrain();
                        return;
                    }
                    double frontX = current.railInfo.x, frontY = current.railInfo.y, frontZ = current.railInfo.z;

                    anchor = current.getCurrentAnchorPos(); //reset origin to new position
                    if(!current.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), speed * -sigNum, RailCheckType.CORE, 0)) {
                        current.derail();
                        this.dissolveTrain();
                        return;
                    }
                    current.setPos(current.railInfo.x, current.railInfo.y, current.railInfo.z);

                    if(!current.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), (speed - current.getLengthSpan()) * -sigNum, RailCheckType.BACK, current.getCollisionSpan() - current.getLengthSpan())) {
                        current.derail();
                        this.dissolveTrain();
                        return;
                    }
                    double backX = current.railInfo.x, backY = current.railInfo.y, backZ = current.railInfo.z;

                    if(inReverse) setRenderPos(current, backX, backY, backZ, frontX, frontY, frontZ);
                    else setRenderPos(current, frontX, frontY, frontZ, backX, backY, backZ);

                } else {
                    this.moveWagonTo(previous, current);
                }

                previous = current;
            }
        }

        /** Uses the front and back bogey positions to set the render pos and angles of a wagon */
        public void setRenderPos(EntityRailCarBase current, double frontX, double frontY, double frontZ, double backX, double backY, double backZ) {
            current.renderX = (frontX + backX) / 2D;
            current.renderY = (frontY + backY) / 2D;
            current.renderZ = (frontZ + backZ) / 2D;
            current.setYRot(generateYaw(frontX, frontZ, backX, backZ));
            double dX = frontX - backX, dY = frontY - backY, dZ = frontZ - backZ;
            current.setXRot((float) (Math.asin(dY / Math.sqrt(dX * dX + dY * dY + dZ * dZ)) * 180D / Math.PI));
        }

        /** Returns the total speed of the LTU, negative if it is backwards compared to the arbitrary "front" wagon */
        public double getTotalSpeed() {

            EntityRailCarBase prev = trains[0];
            double totalSpeed = 0;
            double maxSpeed = Double.POSITIVE_INFINITY;
            //if the first car is in reverse, flip all subsequent cars as well
            boolean reverseTheReverse = prev.getCouplingFrom(null) == TrainCoupling.BACK;

            if(trains.length == 1) {
                return prev.getCurrentSpeed();
            }

            for(EntityRailCarBase train : this.trains) {
                //if the car's linked indices are the wrong way, it is in reverse and speed applies negatively
                boolean reverse = false;

                EntityRailCarBase conFront = train.getCoupledTo(TrainCoupling.FRONT);
                EntityRailCarBase conBack = train.getCoupledTo(TrainCoupling.BACK);

                if(conFront != null && conFront.ltuIndex > train.ltuIndex) reverse = true;
                if(conBack != null && conBack.ltuIndex < train.ltuIndex) reverse = true;

                reverse ^= reverseTheReverse;

                double speed = train.getCurrentSpeed();
                if(reverse) speed *= -1;
                totalSpeed += speed;
                maxSpeed = Math.min(maxSpeed, train.getMaxRailSpeed());
            }

            if(Math.abs(totalSpeed) > maxSpeed) {
                totalSpeed = maxSpeed * Math.signum(totalSpeed);
            }

            return totalSpeed;
        }

        public void collideTrain(double speed) {
            EntityRailCarBase collidingTrain = speed > 0 ? trains[0] : trains[trains.length - 1];

            COLLIDE_HITS.clear();
            collidingTrain.level().getEntities(RAIL_CARS, collidingTrain.getBoundingBox().inflate(1, 1, 1),
                    car -> car.ltu != null && car.ltu != this, COLLIDE_HITS, 1);
            if(COLLIDE_HITS.isEmpty()) return;
            EntityRailCarBase collidesWith = COLLIDE_HITS.get(0);
            COLLIDE_HITS.clear();

            double deltaX = collidingTrain.getX() - collidesWith.getX();
            double deltaZ = collidingTrain.getZ() - collidesWith.getZ();
            double totalSpan = collidingTrain.getCollisionSpan() + collidesWith.getCollisionSpan();
            double diff = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if(diff > totalSpan) return;
            double push = (totalSpan - diff);

            pushApart(collidingTrain, collidesWith, push);
            pushApart(collidesWith, collidingTrain, push);
        }

        /** Which way {@code from}'s LTU has to be shoved to get clear of {@code into}. */
        private static void pushApart(EntityRailCarBase from, EntityRailCarBase into, double push) {
            LogicalTrainUnit ltu = from.ltu;
            if(ltu == null) return;

            if(ltu.trains.length == 1) {
                from.rotateOffset(0, 0, from.getCollisionSpan(), from.getXRot(), from.getYRot());
                double forwardX = into.getX() - (from.getX() + from.rotX);
                double forwardZ = into.getZ() - (from.getZ() + from.rotZ);
                double backwardX = into.getX() - (from.getX() - from.rotX);
                double backwardZ = into.getZ() - (from.getZ() - from.rotZ);

                if(forwardX * forwardX + forwardZ * forwardZ > backwardX * backwardX + backwardZ * backwardZ) {
                    ltu.pushForce += push;
                } else {
                    ltu.pushForce -= push;
                }
            } else {

                if(from.ltuIndex < ltu.trains.length / 2) {
                    ltu.pushForce -= push;
                } else {
                    ltu.pushForce += push;
                }
            }
        }
    }
}
