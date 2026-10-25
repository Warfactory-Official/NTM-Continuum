package com.hbm.entity.mob;

import com.hbm.entity.mob.ai.EntityAINearestAttackableTargetNT;
import com.hbm.sound.ModSounds;
import com.hbm.util.MobUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The name comes from the "swinging" movement patterns as part of its custom path-finding. Also handles rotation.
 * The end result kind of looks like dolphins jumping out of the water just to dive back in.
 * This class assumes that all implementations are indeed hostile mobs.
 */
public abstract class EntityBurrowingSwingingBase extends EntityBurrowingBase {

    protected @Nullable Entity targetedEntity = null;
    protected boolean wasNearGround = false;
    public int courseChangeCooldown = 0;
    public int aggroCooldown = 0;
    public double waypointX;
    public double waypointY;
    public double waypointZ;
    protected BlockPos spawnPoint = BlockPos.ZERO;
    public boolean lastInGround = false;

    protected EntityBurrowingSwingingBase(EntityType<? extends EntityBurrowingSwingingBase> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new EntityAINearestAttackableTargetNT<>(this, Player.class, 0, false, false, null, 128.0D));
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = getDeltaMovement();
        // the server's rotation reaches the client with the position; recomputing it there from a stale motion would fight it
        if (!level().isClientSide()) {
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
            setXRot((float) (Math.atan2(motion.y, horizontal) * 180.0D / Math.PI));
        }

        boolean inGround = isEntityInsideOpaqueBlock();

        if (this.lastInGround != inGround) {
            if (level() instanceof ServerLevel server) {
                server.playSound(null, this, ModSounds.DEBRIS.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            } else {
                double mod = inGround ? -0.25D : 0.25D;

                for (int i = 0; i < 10; i++) {
                    double dev = 0.05D;
                    level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(),
                            motion.x * mod + this.random.nextGaussian() * dev,
                            motion.y * mod + this.random.nextGaussian() * dev,
                            motion.z * mod + this.random.nextGaussian() * dev);
                }
            }
        }

        this.lastInGround = inGround;
    }

    /** 1.7 ran these around {@code updateAITasks}; here they follow the goals in the same server AI step. */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        updateEntityActionState(level);
        if (isRemoved()) return;
        updateSwingingMovement();
    }

    protected void updateEntityActionState(ServerLevel level) {

        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        if (this.targetedEntity != null && !this.targetedEntity.isAlive()) {
            this.targetedEntity = null;
        }

        // port: 1.7's floor guards sat under y = 0 and 3, the bottom of its world
        Vec3 motion = getDeltaMovement();
        if (getY() < level.getMinY() - 10.0D) {
            setDeltaMovement(motion.x, 1D, motion.z);
        } else if (getY() < level.getMinY() + 3.0D) {
            setDeltaMovement(motion.x, 0.3D, motion.z);
        }

        if (this.tickCount % 2 == 0) {
            attackEntitiesInList(level, level.getEntities(this, getBoundingBox().inflate(0.5D)));
        }
    }

    protected void attackEntitiesInList(ServerLevel level, List<Entity> targets) {
        for (Entity target : targets) {
            if (target instanceof LivingEntity && MobUtil.canAttackClass(target)) {
                attackEntityAsMob(level, target);
            }
        }
    }

    protected boolean attackEntityAsMob(ServerLevel level, Entity target) {

        boolean hit = Compat.hurt(target, level, damageSources().mobAttack(this),
                (float) getAttributeValue(Attributes.ATTACK_DAMAGE));

        if (hit) {
            setNoActionTime(0);
            AABB box = getBoundingBox();
            double tx = (box.minX + box.maxX) / 2.0D;
            double tz = (box.minZ + box.maxZ) / 2.0D;
            double ty = (box.minY + box.maxY) / 2.0D;
            double deltaX = target.getX() - tx;
            double deltaZ = target.getZ() - tz;
            double deltaY = target.getY() - ty;
            double knockback = 0.5 * (deltaX * deltaX + deltaZ * deltaZ + deltaY * deltaY + 0.1D);
            target.push(deltaX / knockback, deltaY / knockback, deltaZ / knockback);
        }

        return hit;
    }

    protected void updateSwingingMovement() {

        double deltaX = this.waypointX - getX();
        double deltaY = this.waypointY - getY();
        double deltaZ = this.waypointZ - getZ();
        double delta = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        if (this.courseChangeCooldown-- <= 0) {

            this.courseChangeCooldown += this.random.nextInt(5) + 5;

            double speed = getAttributeBaseValue(Attributes.MOVEMENT_SPEED);

            if (!canSupportMovement()) {
                speed /= 4D;
            }

            setDeltaMovement(getDeltaMovement().add(deltaX / delta * speed, deltaY / delta * speed, deltaZ / delta * speed));
        }

        if (!canSupportMovement() && !this.wasNearGround) {
            setDeltaMovement(getDeltaMovement().subtract(0D, getSwingGravity(), 0D));
        }

        this.aggroCooldown--;

        if (getTarget() != null) {

            if (this.aggroCooldown <= 0) {
                this.targetedEntity = getTarget();
                this.aggroCooldown = getAggroCooldown();
            }

        } else if (this.targetedEntity == null) {
            this.waypointX = this.spawnPoint.getX() - 50 + this.random.nextInt(100);
            this.waypointY = this.spawnPoint.getY() - 30 + this.random.nextInt(60);
            this.waypointZ = this.spawnPoint.getZ() - 50 + this.random.nextInt(100);
        }

        Vec3 motion = getDeltaMovement();
        setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0F / Math.PI));
        setXRot((float) -(Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z)) * 180.0D / Math.PI));

        double range = 100;
        if (this.targetedEntity != null && this.targetedEntity.distanceToSqr(this) < range * range) {

            if (canSupportMovement() || this.wasNearGround) {

                this.waypointX = this.targetedEntity.getX();
                this.waypointY = this.targetedEntity.getY() + this.targetedEntity.getBbHeight() * 0.5;
                this.waypointZ = this.targetedEntity.getZ();

                int surface = level().getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(this.waypointX), Mth.floor(this.waypointZ));

                if (this.random.nextInt(80) == 0 && getY() > surface && !canSupportMovement()) {
                    this.wasNearGround = false;
                }

            } else {

                this.waypointX = this.targetedEntity.getX();
                this.waypointY = 10.0D;
                this.waypointZ = this.targetedEntity.getZ();

                if (getY() < 15.0D) {
                    this.wasNearGround = true;
                }
            }
        } else {
            this.waypointX = this.spawnPoint.getX() - 20 + this.random.nextInt(40);
            this.waypointY = this.spawnPoint.getY() - 5 + this.random.nextInt(100);
            this.waypointZ = this.spawnPoint.getZ() - 20 + this.random.nextInt(40);
        }
    }

    /** 1.7 {@code getGravity}, a name Entity now owns. */
    protected double getSwingGravity() {
        return 0.01;
    }

    protected int getAggroCooldown() {
        return 20;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("spawnX", this.spawnPoint.getX());
        output.putInt("spawnY", this.spawnPoint.getY());
        output.putInt("spawnZ", this.spawnPoint.getZ());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.spawnPoint = new BlockPos(input.getIntOr("spawnX", 0), input.getIntOr("spawnY", 0), input.getIntOr("spawnZ", 0));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        this.spawnPoint = blockPosition();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }
}
