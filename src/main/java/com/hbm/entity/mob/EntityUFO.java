package com.hbm.entity.mob;

import dev.golem.core.world.Explosions;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatMob;
import com.hbm.advancement.BossKilledTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.XFactoryNPC;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EntityUFO extends CompatMob implements Enemy, IRadiationImmune {

    // 1.7 dataWatcher ids 16 / 17 / 18 / 19
    private static final EntityDataAccessor<Boolean> BEAM =
            SynchedEntityData.defineId(EntityUFO.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> WAYPOINT_X =
            SynchedEntityData.defineId(EntityUFO.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Y =
            SynchedEntityData.defineId(EntityUFO.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WAYPOINT_Z =
            SynchedEntityData.defineId(EntityUFO.class, EntityDataSerializers.INT);


    private final List<Entity> secondaries = new ArrayList<>();

    public int courseChangeCooldown;
    public int scanCooldown;
    public int hurtCooldown;
    public int beamTimer;

    private @Nullable Entity target;

    public EntityUFO(EntityType<? extends EntityUFO> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 500;
        // 1.7 started deathTime at -30 so the saucer spends a second and a half falling before it detonates
        this.deathTime = -30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20000.0D).add(Attributes.FOLLOW_RANGE, 150.0D);
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(BEAM, false);
        builder.define(WAYPOINT_X, 0);
        builder.define(WAYPOINT_Y, 0);
        builder.define(WAYPOINT_Z, 0);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        // 1.7 EntityFlying.fall() was empty
    }

    /**
     * 1.7 {@code EntityFlying.moveEntityWithHeading}: no gravity, move, then a flat 0.91 drag (0.546 on the
     * ground). The AI rewrites motion every live tick, so the drag only shapes the death fall.
     */
    @Override
    public void travel(Vec3 input) {
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(onGround() ? 0.6D * 0.91D : 0.91D));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000D; // 1.7 ignoreFrustumCheck + isInRangeToRenderDist
    }

    /** 1.7 threw away every hit for five ticks after one landed, which is what makes it survivable to fight. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (this.hurtCooldown > 0) return false;

        boolean hit = super.hurtServer(level, source, amount);
        if (hit) this.hurtCooldown = 5;
        return hit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {

        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        if (this.hurtCooldown > 0) this.hurtCooldown--;
        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;
        if (this.scanCooldown > 0) this.scanCooldown--;

        if (this.target != null && !this.target.isAlive()) this.target = null;

        if (this.scanCooldown <= 0) scanForTargets();
        if (this.target != null && this.courseChangeCooldown <= 0) setCourse();

        tickBeam(level);
        tickWeapons();
        approachWaypoint();
    }

    /**
     * 1.7: players are the primary target and anything else living within a hundred blocks and line of sight is
     * a secondary. With no player around the saucer picks a secondary at random and hunts that instead.
     */
    private void scanForTargets() {

        this.secondaries.clear();
        this.target = null;

        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(100D, 50D, 100D))) {

            if (!entity.isAlive() || !canTarget(entity)) continue;

            if (entity instanceof Player player) {
                if (player.isCreative()) continue;
                if (player.hasEffect(MobEffects.INVISIBILITY)) continue;

                if (this.target == null || distanceToSqr(player) < distanceToSqr(this.target)) this.target = player;
            }

            if (entity instanceof LivingEntity living && distanceToSqr(living) < 100D * 100D
                    && getSensing().hasLineOfSight(living) && living != this.target) {
                this.secondaries.add(living);
            }
        }

        if (this.target == null && !this.secondaries.isEmpty()) {
            this.target = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
        }

        this.scanCooldown = 50;
    }

    /** 1.7: aim thirty-five blocks past the target on a randomly rotated bearing, well above the terrain. */
    private void setCourse() {

        Vec3 vec = new Vec3(getX() - this.target.getX(), 0D, getZ() - this.target.getZ());

        if (this.random.nextInt(3) > 0) vec = vec.yRot((float) Math.PI * 2F * this.random.nextFloat());

        double length = vec.length();
        double overshoot = 35D;

        int wX = (int) Math.floor(this.target.getX() - vec.x / length * overshoot);
        int wZ = (int) Math.floor(this.target.getZ() - vec.z / length * overshoot);

        int surface = level().getHeight(Heightmap.Types.WORLD_SURFACE, wX, wZ) + 20 + this.random.nextInt(15);
        setWaypoint(wX, Math.max(surface, (int) this.target.getY() + 15), wZ);

        this.courseChangeCooldown = 40 + this.random.nextInt(20);
    }

    /**
     * 1.7's tractor beam: armed whenever the saucer passes within twenty-five blocks of its target, it kills
     * anything standing in the column between the saucer and the ground.
     */
    private void tickBeam(ServerLevel level) {

        if (this.beamTimer <= 0 && getBeam()) setBeam(false);

        if (this.target != null) {
            double dist = Math.abs(this.target.getX() - getX()) + Math.abs(this.target.getZ() - getZ());
            if (dist < 25D) this.beamTimer = 30;
        }

        if (this.beamTimer <= 0) return;
        this.beamTimer--;

        if (!getBeam()) {
            level.playSound(null, this, ModSounds.UFO_BEAM.get(), SoundSource.HOSTILE, 10F, 1F);
            setBeam(true);
        }

        int groundY = groundBelow();
        if (groundY >= getY()) return;

        AABB column = new AABB(getX(), groundY, getZ(), getX(), getY(), getZ()).inflate(5D, 0D, 5D);

        for (Entity entity : level.getEntities(this, column)) {

            if (!canTarget(entity)) continue;

            Compat.hurt(entity, level, ModDamageSources.source(level, ModDamageSources.COMBINE, this, entity), 1000F);
            Effects.ignite(entity, 5);

            if (entity instanceof LivingEntity living) {
                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            }
        }

        // 1.7: two still muke clouds a tick at the beam's foot, one half a tick ahead along the motion (150 blocks)
        Vec3 motion = getDeltaMovement();
        HbmEffectNT.Ufo.send(level, getX(), groundY + 0.5, getZ(), null, 150D);
        HbmEffectNT.Ufo.send(level, getX() + motion.x * 0.5, groundY + 0.5,
                getZ() + motion.z * 0.5, null, 150D);
    }

    /**
     * 1.7's phase clock: two hundred ticks of lasers, then a hundred of rockets, both firing at the secondaries
     * when it has any and at the primary otherwise.
     */
    private void tickWeapons() {

        boolean laserPhase = this.tickCount % 300 < 200;
        int period = laserPhase ? 4 : 20;

        if (this.tickCount % period == 0) {

            if (!this.secondaries.isEmpty()) {
                Entity victim = this.secondaries.get(this.random.nextInt(this.secondaries.size()));
                if (!victim.isAlive()) this.secondaries.remove(victim);
                else shoot(victim, laserPhase);

            } else if (this.target != null) {
                shoot(this.target, laserPhase);
            }

        } else if (this.tickCount % period == period / 2 && this.target != null) {
            shoot(this.target, laserPhase);
        }
    }

    private void shoot(Entity victim, boolean laser) {
        if (laser) laserAttack(victim);
        else rocketAttack(victim);
    }

    /** 1.7: the shot comes off the saucer's rim, ten blocks out on a random bearing, not off its centre. */
    private void laserAttack(Entity victim) {

        Vec3 vec = new Vec3(getX() - victim.getX(), 0D, getZ() - victim.getZ())
                .yRot((float) Math.toRadians(-80 + this.random.nextInt(160)))
                .normalize();

        double pivotX = getX() - vec.x * 10D;
        double pivotY = getY() + 0.5D;
        double pivotZ = getZ() - vec.z * 10D;

        Vec3 heading = new Vec3(victim.getX() - pivotX, victim.getY() + victim.getBbHeight() / 2D - pivotY,
                victim.getZ() - pivotZ).normalize();

        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level(), this, XFactoryNPC.worm_laser,
                XFactoryNPC.WORM_LASER_DAMAGE, 0.02F, pivotX, pivotY, pivotZ, heading.x, heading.y, heading.z);
        // the head fires this config at 1, the saucer at 2 - see XFactoryNPC
        bullet.accel = 1F;
        level().addFreshEntity(bullet);

        level().playSound(null, getX(), getY(), getZ(), ModSounds.BALLS_LASER.get(), SoundSource.HOSTILE, 5F, 1F);
    }

    private void rocketAttack(Entity victim) {

        Vec3 heading = new Vec3(victim.getX() - getX(), victim.getY() + victim.getBbHeight() / 2D - getY() - 0.5D,
                victim.getZ() - getZ()).normalize();

        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level(), this, XFactoryNPC.ufo_rocket,
                XFactoryNPC.UFO_ROCKET_DAMAGE, 0.02F, getX(), getY() - 0.5D, getZ(),
                heading.x, heading.y, heading.z);
        bullet.lockonTarget = victim;
        level().addFreshEntity(bullet);

        level().playSound(null, getX(), getY(), getZ(), ModSounds.TURRET_RICHARD_FIRE.get(), SoundSource.HOSTILE, 5F, 1F);
    }


    private void approachWaypoint() {

        setDeltaMovement(Vec3.ZERO);

        if (this.courseChangeCooldown <= 0) return;

        Vec3 delta = new Vec3(getWaypointX() - getX(), getWaypointY() - getY(), getWaypointZ() - getZ());
        double len = delta.length();

        if (len <= 5D) return;

        double speed = this.target instanceof Player ? 5D : 2D;

        if (isCourseTraversable(len)) {
            setDeltaMovement(delta.scale(speed / len));
        } else {
            this.courseChangeCooldown = 0;
        }
    }

    private boolean isCourseTraversable(double distance) {

        Vec3 step = new Vec3(getWaypointX() - getX(), getWaypointY() - getY(), getWaypointZ() - getZ())
                .scale(1D / distance);
        AABB box = getBoundingBox();

        for (int i = 1; i < distance; i++) {
            box = box.move(step);
            if (!level().noCollision(this, box)) return false;
        }

        return true;
    }

    private int groundBelow() {

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = Mth.floor(getX());
        int z = Mth.floor(getZ());

        for (int y = (int) Math.ceil(getY()); y >= level().getMinY(); y--) {
            if (!level().getBlockState(pos.set(x, y, z)).isAir()) return y;
        }

        return level().getMinY();
    }


    private boolean canTarget(Entity entity) {
        return !(entity instanceof EntityUFO) && !(entity instanceof EntityBulletBaseMK4);
    }

    @Override
    public void tick() {
        super.tick();

    }




    @Override
    protected void tickDeath() {

        if (getBeam()) setBeam(false);

        setDeltaMovement(getDeltaMovement().subtract(0D, 0.05D, 0D));

        if (this.deathTime == -10) {
            level().playSound(null, this, ModSounds.CHOPPER_DAMAGE.get(), SoundSource.HOSTILE, 10F, 1F);
        }

        if (this.deathTime == 19 && level() instanceof ServerLevel server) {

            // 1.7 newExplosion(isFlaming = true, isSmoking = true)
            Explosions.explode(server, this, getX(), getY(), getZ(), 10F, true, Level.ExplosionInteraction.MOB);
            ExplosionNukeSmall.explode(server, getX(), getY(), getZ(), ExplosionNukeSmall.PARAMS_MEDIUM);

            HbmCriteria.bossKilled(this, BossKilledTrigger.Kind.UFO, 200D);

            // 1.7: boundingBox.expand(200) box, and addItemStackToInventory - a full inventory loses the coin
            for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(200D))) {
                player.getInventory().add(new ItemStack(ModItems.coin_ufo.get()));
            }
        }

        super.tickDeath();
    }

    @Override
    protected float getSoundVolume() {
        return 10F;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    public void setBeam(boolean beam) {
        this.entityData.set(BEAM, beam);
    }

    public boolean getBeam() {
        return this.entityData.get(BEAM);
    }

    public void setWaypoint(int x, int y, int z) {
        this.entityData.set(WAYPOINT_X, x);
        this.entityData.set(WAYPOINT_Y, y);
        this.entityData.set(WAYPOINT_Z, z);
    }

    public int getWaypointX() {
        return this.entityData.get(WAYPOINT_X);
    }

    public int getWaypointY() {
        return this.entityData.get(WAYPOINT_Y);
    }

    public int getWaypointZ() {
        return this.entityData.get(WAYPOINT_Z);
    }
}
