package com.hbm.entity.mob.siege;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityUFOBase;
import com.hbm.entity.projectile.EntitySiegeLaser;
import com.hbm.lib.ModDamageSources;
import com.hbm.packet.PlasmaBlastPayload;
import com.hbm.particle.helper.EntityEffects;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.MobUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The siege saucer: a fan of seven siege lasers every 30..39 ticks and a 300-tick lock-on cycle whose last 40 ticks
 * beam the lock-on point through any blocks. Its boss bar call is commented out in 1.7, so it shows none.
 */
public class EntitySiegeCraft extends EntityUFOBase {

    // 1.7 dataWatcher 12 (tier, runtime index, saved by name), 13..15 (lock-on), 16 (beam)
    private static final EntityDataAccessor<Integer> TIER =
            SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LOCK_X =
            SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOCK_Y =
            SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOCK_Z =
            SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> BEAM =
            SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.BYTE);

    private int attackCooldown;
    private int beamCountdown;

    public EntitySiegeCraft(EntityType<? extends EntitySiegeCraft> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {

        if (isInvulnerableToBase(source)) return false;

        SiegeTier tier = getTier();

        if (tier.fireProof && source.is(DamageTypeTags.IS_FIRE)) {
            clearFire();
            return false;
        }

        //noFF can't be harmed by other mobs
        if (tier.noFriendlyFire && source.getEntity() != null && !(source.getEntity() instanceof Player)) return false;

        damage -= tier.dt;

        if (damage < 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 5F, 1.0F + this.random.nextFloat() * 0.5F);
            return false;
        }

        damage *= (1F - tier.dr);

        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void tickDeath() {

        this.beamCountdown = 200;
        setBeam(false);

        setDeltaMovement(getDeltaMovement().subtract(0D, 0.05D, 0D));

        if (this.deathTime == 19 && level() instanceof ServerLevel server) {
            EntityEffects.muke(server, getX(), getY() + 0.5, getZ(), true, false, 250D);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.MUKE_EXPLOSION.get(), SoundSource.HOSTILE, 15.0F, 1.0F);
        }

        super.tickDeath();
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(TIER, 0);
        builder.define(LOCK_X, 0F);
        builder.define(LOCK_Y, 0F);
        builder.define(LOCK_Z, 0F);
        builder.define(BEAM, (byte) 0);
    }

    public void setTier(SiegeTier tier) {
        this.entityData.set(TIER, tier.id);

        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(tier.speedMod);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(tier.health * 25);
        setHealth(getMaxHealth());
    }

    public SiegeTier getTier() {
        int id = this.entityData.get(TIER);
        SiegeTier tier = id >= 0 && id < SiegeTier.getLength() ? SiegeTier.tiers[id] : null;
        return tier != null ? tier : SiegeTier.CLAY;
    }

    public void setBeam(boolean beam) {
        this.entityData.set(BEAM, beam ? (byte) 1 : (byte) 0);
    }

    public boolean getBeam() {
        return this.entityData.get(BEAM) == 1;
    }

    public void setLockon(double x, double y, double z) {
        this.entityData.set(LOCK_X, (float) x);
        this.entityData.set(LOCK_Y, (float) y);
        this.entityData.set(LOCK_Z, (float) z);
    }

    public Vec3 getLockon() {
        return new Vec3(this.entityData.get(LOCK_X), this.entityData.get(LOCK_Y), this.entityData.get(LOCK_Z));
    }

    public @Nullable Entity target() {
        return this.target;
    }

    @Override
    protected int getScanRange() {
        return 100;
    }

    @Override
    protected int targetHeightOffset() {
        return 7 + this.random.nextInt(5);
    }

    @Override
    protected int wanderHeightOffset() {
        return 10 + this.random.nextInt(2);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (isRemoved()) return;

        if (this.courseChangeCooldown > 0) {
            this.courseChangeCooldown--;
        }
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.beamCountdown > 0) {
            this.beamCountdown--;
        }

        if (this.random.nextInt(50) == 0) {
            CompoundTag dPart = new CompoundTag();
            dPart.putByte("count", (byte) (2 + this.random.nextInt(3)));
            HbmEffectNT.Tau.send(level, getX() + this.random.nextGaussian() * 2, getY() + this.random.nextGaussian(),
                    getZ() + this.random.nextGaussian() * 2, dPart, 50D);
        }

        boolean beam = false;

        if (this.target == null || this.beamCountdown <= 0) {
            this.beamCountdown = 300; //200 - 100: nothing, 100 - 40: update lockon, 40 - 20: fix lockon, 20 - 0: beam
        } else {

            if (this.beamCountdown >= 60 && this.beamCountdown < 120) {
                double x = this.target.getX();
                double y = this.target.getY() + this.target.getBbHeight() * 0.5;
                double z = this.target.getZ();
                setLockon(x, y, z);

                if (this.beamCountdown == 110) {
                    level.playSound(null, this.target.getX(), this.target.getY(), this.target.getZ(),
                            ModSounds.STINGER_LOCK_ON.get(), SoundSource.HOSTILE, 2F, 0.75F);
                }
            }

            if (this.beamCountdown >= 40 && this.beamCountdown < 100) {
                Vec3 lockon = getLockon();
                CompoundTag fx = new CompoundTag();
                fx.putDouble("motion", 0.2D);
                fx.putInt("count", 5);
                HbmEffectNT.VanillaBurst_RedDust.send(level, lockon.x, lockon.y, lockon.z, fx, 100D);
            }

            if (this.beamCountdown < 40) {

                Vec3 lockon = getLockon();

                if (this.beamCountdown == 39) {
                    level.playSound(null, lockon.x, lockon.y, lockon.z, ModSounds.UFO_BLAST.get(), SoundSource.HOSTILE, 5.0F,
                            0.9F + level.getRandom().nextFloat() * 0.2F);
                }

                for (Entity e : level.getEntities(this, new AABB(lockon, lockon).inflate(2, 2, 2))) {
                    if (MobUtil.canAttackClass(e)) {
                        Compat.hurt(e, level, ModDamageSources.source(level, ModDamageSources.COMBINE, this, e), 1000F);
                        Effects.ignite(e, 5);

                        if (e instanceof LivingEntity living) {
                            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
                        }
                    }
                }

                PlasmaBlastPayload.send(level, lockon.x, lockon.y, lockon.z, 0.0F, 0.75F, 1.0F,
                        -90 + this.random.nextFloat() * 180, this.random.nextFloat() * 180F, 5F, 150D);
                beam = true;
            }
        }

        setBeam(beam);

        if (this.attackCooldown == 0 && this.target != null) {
            this.attackCooldown = 30 + this.random.nextInt(10);

            double x = getX();
            double y = getY();
            double z = getZ();

            Vec3 vec = new Vec3(this.target.getX() - x, this.target.getY() + this.target.getBbHeight() * 0.5 - y,
                    this.target.getZ() - z).normalize();
            SiegeTier tier = getTier();

            float health = getHealth() / getMaxHealth();

            int r = (int) (0xff * (1 - health));
            int g = (int) (0xff * health);
            int b = 0;
            int color = (r << 16) | (g << 8) | b;

            for (int i = 0; i < 7; i++) {

                Vec3 copy = vec.yRot((float) Math.PI / 180F * (i - 3) * 5F);

                EntitySiegeLaser laser = new EntitySiegeLaser(ModNtmEntities.SIEGE_LASER.get(), level, this);
                laser.setPos(x, y, z);
                laser.setThrowableHeading(copy.x, copy.y, copy.z, 1F, 0.0F);
                laser.setColor(color);
                laser.setDamage(tier.damageMod);
                laser.setBreakChance(tier.laserBreak * 2);
                if (tier.laserIncendiary) laser.setIncendiary();
                level.addFreshEntity(laser);
            }

            playSound(ModSounds.BALLS_LASER.get(), 2.0F, 1.0F);
        }

        if (this.courseChangeCooldown > 0) {
            approachPosition(this.target == null ? 0.25D : 0.5D + getAttributeValue(Attributes.MOVEMENT_SPEED) * 1);
        }
    }

    @Override
    protected void setCourseWithoutTarget() {
        int x = (int) Math.floor(getX() + this.random.nextGaussian() * 15);
        int z = (int) Math.floor(getZ() + this.random.nextGaussian() * 15);
        setWaypoint(x, surfaceAt(x, z) + 5 + this.random.nextInt(6), z);
    }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        out.putString("siegeTier", getTier().name);
    }

    /** 1.7 re-applies the tier here, which also refills the health on every load. A missing key read as tier 0. */
    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        SiegeTier tier = SiegeTier.byName(in.getStringOr("siegeTier", ""));
        setTier(tier != null ? tier : SiegeTier.DEFAULT_BUFF);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        setTier(SiegeTier.tiers[this.random.nextInt(SiegeTier.getLength())]);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        if (killedByPlayer) {
            for (ItemStack drop : getTier().dropItems()) {
                spawnAtLocation(level, drop, 0F);
            }
        }
    }
}
