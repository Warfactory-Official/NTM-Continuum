package com.hbm.entity.mob.botprime;

import dev.golem.core.data.Tags;

import dev.golem.core.world.Compat;
import dev.golem.core.data.CompatPathfinderMob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

import java.util.List;


public abstract class EntityWormBaseNT extends CompatPathfinderMob {

    public int aggroCooldown;
    public int courseChangeCooldown;
    public double waypointX;
    public double waypointY;
    public double waypointZ;

    protected @Nullable Entity targetedEntity;
    protected @Nullable LivingEntity followed;
    protected boolean canFly;
    protected int dmgCooldown;
    protected boolean wasNearGround;
    protected BlockPos spawnPoint = BlockPos.ZERO;
    protected double attackRange;
    protected double maxSpeed;
    protected double fallSpeed;
    protected double rangeForParts;
    protected int surfaceY = 60;
    protected boolean didCheck;
    protected double maxBodySpeed;
    protected double segmentDistance;
    protected double knockbackDivider;

    protected float dragInAir;
    protected float dragInGround;

    private int headID;
    /**
     * The head's UUID: what is saved. 1.7 saved {@code wormID} = the head's runtime entity id, which the next load
     * hands out to some other entity, so a reloaded worm fell apart (History section 19ej-ids). The int stays as the
     * session-only comparison key and is re-resolved from the UUID after a load.
     */
    private java.util.@Nullable UUID headUUID;
    private static final int UNRESOLVED = Integer.MIN_VALUE;
    private int partNum;

    protected EntityWormBaseNT(EntityType<? extends EntityWormBaseNT> type, Level level) {
        super(type, level);
    }

    public int getPartNumber() {
        return this.partNum;
    }

    public void setPartNumber(int num) {
        this.partNum = num;
    }

    public @Nullable Entity getHead() {
        return level().getEntity(getHeadID());
    }

    public int getHeadID() {
        if (this.headID == UNRESOLVED && this.headUUID != null && level() instanceof ServerLevel server) {
            Entity head = server.getEntity(this.headUUID);
            if (head != null) this.headID = head.getId();
        }
        return this.headID;
    }

    public void setHeadID(int id) {
        this.headID = id;
    }

    /** Joins this segment to {@code head}'s worm (the head passes itself). */
    public void setHeadEntity(Entity head) {
        this.headID = head.getId();
        this.headUUID = head.getUUID();
    }

    public boolean getIsHead() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        // CRAMMING is modern-only: 75 overlapping segments would otherwise grind each other down
        if (isInvulnerableToBase(source) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING)) {
            return false;
        }

        if (source.getEntity() instanceof EntityWormBaseNT worm && worm.getHeadID() == getHeadID()) return false;

        markHurt();

        if (getIsHead()) return super.hurtServer(level, source, amount);

        // 1.7 forwarded a segment's damage to whatever it was following, which for part 0 is the head itself
        Entity ahead = this.targetedEntity;
        if (ahead != null) return Compat.hurt(ahead, level, source, amount);

        return super.hurtServer(level, source, amount);
    }

    /** 1.7 {@code updateEntityActionState}: the shared per-tick body of every segment. */
    protected void updateActionState() {

        if (!level().isClientSide() && level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        if (this.targetedEntity != null && !this.targetedEntity.isAlive()) this.targetedEntity = null;

        Vec3 motion = getDeltaMovement();
        if (getY() < -10D) {
            setDeltaMovement(motion.x, 1D, motion.z);
        } else if (getY() < 3D) {
            setDeltaMovement(motion.x, 0.3D, motion.z);
        }

        if (this.tickCount % 5 == 0) {
            attackEntitiesInList(level().getEntities(this, getBoundingBox().inflate(0.5D)));
        }
    }

    protected void attackEntitiesInList(List<Entity> targets) {
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity)) continue;
            if (target instanceof EntityWormBaseNT worm && worm.getHeadID() == getHeadID()) continue;
            attackAsWorm(target);
        }
    }

    /**
     * 1.7 {@code attackEntityAsMob}: mob damage plus a shove whose strength falls off with the square of the gap,
     * which is what keeps players from standing inside a segment.
     */
    protected boolean attackAsWorm(Entity target) {

        if (!(level() instanceof ServerLevel server)) return false;
        if (!Compat.hurt(target, server, damageSources().mobAttack(this), getAttackStrength(target))) return false;

        double tx = (getBoundingBox().minX + getBoundingBox().maxX) / 2D;
        double ty = (getBoundingBox().minY + getBoundingBox().maxY) / 2D;
        double tz = (getBoundingBox().minZ + getBoundingBox().maxZ) / 2D;
        double dx = target.getX() - tx;
        double dy = target.getY() - ty;
        double dz = target.getZ() - tz;
        double knockback = this.knockbackDivider * (dx * dx + dy * dy + dz * dz + 0.1D);

        target.push(dx / knockback, dy / knockback, dz / knockback);
        return true;
    }

    public abstract float getAttackStrength(Entity target);

    /** 1.7 overrode addVelocity to nothing - a worm is never pushed by anything. */
    @Override
    public void push(double x, double y, double z) {
    }

    protected boolean isCourseTraversable() {
        return this.canFly || isBuried();
    }

    protected boolean isBuried() {

        float width = getBbWidth() * 0.8F;
        AABB eyeBox = AABB.ofSize(getEyePosition(), width, 1.0E-6D, width);

        return BlockPos.betweenClosedStream(eyeBox).anyMatch(pos -> {
            BlockState state = level().getBlockState(pos);
            return !state.isAir() && state.isSuffocating(level(), pos)
                    && Shapes.joinIsNotEmpty(state.getCollisionShape(level(), pos).move(pos.getX(), pos.getY(), pos.getZ()),
                            Shapes.create(eyeBox), BooleanOp.AND);
        });
    }

    /**
     * 1.7 {@code EntityBurrowingNT.moveEntityWithHeading}: no gravity and no step assist, just a drag that
     * depends on whether the segment is currently buried.
     */
    @Override
    public void travel(Vec3 travelVector) {

        float drag = isBuried() || isInWater() || isInLava() ? this.dragInGround : this.dragInAir;
        if (!getIsHead()) drag *= 0.9F;

        moveRelative(0.02F, travelVector);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(drag));
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        // 1.7 EntityBurrowingNT.fall / updateFallState were both empty
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    /**
     * 1.7 {@code setDead}: every removal plays the death sound, not only an actual death. Chunk unloading never
     * called setDead in 1.7 (the entity was just dropped from the loaded list), so unloads stay silent; a
     * dimension change did, so it still plays.
     */
    @Override
    public void remove(RemovalReason reason) {
        SoundEvent sound = getDeathSound();
        boolean unload = reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER;

        if (sound != null && !unload && level() instanceof ServerLevel server) {
            server.playSound(null, getX(), getY(), getZ(), sound, getSoundSource(), getSoundVolume(),
                    getVoicePitch());
        }

        super.remove(reason);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        if (headUUID != null) output.store("worm", net.minecraft.core.UUIDUtil.CODEC, headUUID); // never the entity id
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.headUUID = input.read("worm", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        this.headID = this.headUUID == null ? 0 : UNRESOLVED;
    }


    protected void updateMovement() {

        double targetingRange = 128D;

        if (this.targetedEntity != null
                && this.targetedEntity.distanceToSqr(this) < targetingRange * targetingRange) {
            this.waypointX = this.targetedEntity.getX();
            this.waypointY = this.targetedEntity.getY();
            this.waypointZ = this.targetedEntity.getZ();
        }

        if ((this.tickCount % 60 == 0 || this.tickCount == 1)
                && (this.targetedEntity == null || this.followed == null)) {
            findEntityToFollow(level().getEntitiesOfClass(EntityWormBaseNT.class,
                    getBoundingBox().inflate(this.rangeForParts)));
        }

        double dx = this.waypointX - getX();
        double dy = this.waypointY - getY();
        double dz = this.waypointZ - getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double speed = Math.max(0D, Math.min(dist - this.segmentDistance, this.maxBodySpeed));

        if (dist < this.segmentDistance * 0.895D) {
            setDeltaMovement(getDeltaMovement().scale(0.8D));
        } else {
            setDeltaMovement(dx / dist * speed, dy / dist * speed, dz / dist * speed);
        }
    }


    protected void findEntityToFollow(List<EntityWormBaseNT> segments) {

        for (EntityWormBaseNT segment : segments) {

            if (segment.getHeadID() != getHeadID()) continue;

            if (segment.getIsHead()) {
                if (getPartNumber() == 0) this.targetedEntity = segment;
                this.followed = segment;
            } else if (segment.getPartNumber() == getPartNumber() - 1) {
                this.targetedEntity = segment;
            }
        }

        this.didCheck = true;
    }
}
