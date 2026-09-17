package com.hbm.entity.mob;

import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatMonster;
import com.hbm.advancement.BossKilledTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.ModItems;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.sound.ModSounds;
import com.hbm.lib.ModDamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

public class EntityRADBeast extends CompatMonster implements IRadiationImmune {

    /** 1.7 dataWatcher id 16: the current victim's entity id, so the renderer can draw the beam. */
    private static final EntityDataAccessor<Integer> VICTIM =
            SynchedEntityData.defineId(EntityRADBeast.class, EntityDataSerializers.INT);

    private static final double LEADER_HEALTH = 360.0D;

    private float heightOffset = 0.5F;
    private int heightOffsetUpdateTime;
    /** 1.7 {@code EntityLiving.attackTime}: 20 after every hit, and the beam's own visibility window. */
    private int attackDelay;

    public EntityRADBeast(EntityType<? extends EntityRADBeast> type, Level level) {
        super(type, level);
        dev.golem.core.world.Compat.avoidWater(this); // 1.7 drowns itself in water
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                      .add(Attributes.MAX_HEALTH, 120.0D)
                      .add(Attributes.ATTACK_DAMAGE, 16.0D)
                      // 1.7 hardcoded getTotalArmorValue() = 8
                      .add(Attributes.ARMOR, 8.0D);
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(VICTIM, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(4, new RadiationAttackGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 1.7 makeLeader: the tripled-health variant that carries the boss coin. */
    public EntityRADBeast makeLeader() {
        setDropChance(EquipmentSlot.MAINHAND, 1F);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.coin_radiation.get()));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(LEADER_HEALTH);
        heal(getMaxHealth());
        return this;
    }

    public boolean isLeader() {
        return getMaxHealth() > 150;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false; // 1.7 canDespawn() = false
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F; // 1.7 getBrightness: always fully lit
    }

    public @Nullable Entity getUnfortunateSoul() {
        int id = entityData.get(VICTIM);
        return id == 0 ? null : level().getEntity(id);
    }

    @Override
    public void aiStep() {
        if (level() instanceof ServerLevel server) {
            if (isInWaterOrRain()) hurtServer(server, damageSources().drown(), 1.0F);

            if (--this.heightOffsetUpdateTime <= 0) {
                this.heightOffsetUpdateTime = 100;
                this.heightOffset = 0.5F + (float) random.nextGaussian() * 3.0F;
            }

            LivingEntity target = getTarget();

            // 1.7: hop upward while the target is higher than the drifting offset
            if (target != null && target.getY() + target.getEyeHeight() > getY() + getEyeHeight() + this.heightOffset) {
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x, motion.y + (0.3D - motion.y) * 0.3D, motion.z);
            }

            // 1.7 wrote the id only while attackTime < 10, so the beam draws over the wind-up and not after
            entityData.set(VICTIM, target != null && this.attackDelay < 10 ? target.getId() : 0);
        }

        // 1.7 slowed the descent so it hangs in the air
        if (!onGround() && getDeltaMovement().y < 0) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0.6D, 1));
        }

        spawnHeatParticles();
        super.aiStep();
    }

    /** 1.7 rendered the leader as molten and the rank-and-file as merely hot. */
    private void spawnHeatParticles() {
        if (isLeader()) {
            level().addParticle(ParticleTypes.LAVA,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight() * 0.75,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(), 0, 0, 0);
            return;
        }

        for (int i = 0; i < 6; i++) {
            level().addParticle(ParticleTypes.MYCELIUM,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.5,
                    getY() + random.nextDouble() * getBbHeight(),
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.5, 0, 0, 0);
        }

        if (random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.FLAME,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight() * 0.75,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(), 0, 0, 0);
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false; // 1.7 fall() was empty
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GEIGER_SOUNDS[random.nextInt(ModSounds.GEIGER_SOUNDS.length)].get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.METAL_STEP.get(); // 1.7 "hbm:step.iron"
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (isLeader()) HbmCriteria.bossKilled(this, BossKilledTrigger.Kind.MELTDOWN, 50D);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (!killedByPlayer) return;

        int looting = 0;
        if (source.getEntity() instanceof LivingEntity attacker) {
            looting = dev.golem.core.world.Compat.lootingLevel(level, attacker);
        }

        // 1.7: a guaranteed polonium nugget per looting level
        if (looting > 0) {
            spawnAtLocation(level, new ItemStack(ModItems.nugget_polonium.get(), looting));
        }

        boolean wet = isInWaterOrRain();
        int count = random.nextInt(3) + 1;

        for (int i = 0; i < count; i++) {
            ZirnoxFuel rod = switch (random.nextInt(3)) {
                case 0 -> ZirnoxFuel.URANIUM;
                case 1 -> ZirnoxFuel.MOX;
                default -> ZirnoxFuel.PLUTONIUM;
            };

            // 1.7: a wet beast has already slagged its rods down to raw waste
            spawnAtLocation(level, wet
                    ? new ItemStack(wasteFor(rod), 2)
                    : new ItemStack(ModZirnoxItems.depleted(rod)));
        }
    }

    private static Item wasteFor(ZirnoxFuel rod) {
        return ModZirnoxItems.waste(rod);
    }

    /**
     * 1.7 {@code attackEntity}, plus the pursuit {@code EntityCreature.updateEntityActionState} ran around it -
     * this claims {@link Goal.Flag#MOVE}, so the stroll goal cannot supply that movement while a target exists.
     */
    private static class RadiationAttackGoal extends Goal {

        private static final double PULSE_RANGE = 30.0D;
        private static final double MELEE_RANGE = 2.0D;

        private final EntityRADBeast beast;

        RadiationAttackGoal(EntityRADBeast beast) {
            this.beast = beast;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.beast.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.beast.getTarget();
            if (target == null) return;

            if (this.beast.attackDelay > 0) this.beast.attackDelay--;

            this.beast.getLookControl().setLookAt(target, 30F, 30F);

            // 1.7 repathed on a finished path or one tick in twenty
            if (this.beast.getNavigation().isDone() || this.beast.getRandom().nextInt(20) == 0) {
                dev.golem.core.world.Navigations.navigateTo(this.beast.getNavigation(), target, 1.0D);
            }

            if (this.beast.attackDelay > 0) return;
            // 1.7 EntityCreature only called attackEntity when canEntityBeSeen(target): no melee or pulse through walls
            if (!this.beast.getSensing().hasLineOfSight(target)) return;

            double distance = this.beast.distanceTo(target);
            AABB self = this.beast.getBoundingBox();
            AABB other = target.getBoundingBox();

            // 1.7 required vertical overlap for the melee, else the radiation branch runs at any height
            if (distance < MELEE_RANGE && other.maxY > self.minY && other.minY < self.maxY) {
                if (this.beast.level() instanceof ServerLevel server) this.beast.doHurtTarget(server, target);
                this.beast.attackDelay = 20;
                return;
            }

            if (distance >= PULSE_RANGE || !(this.beast.level() instanceof ServerLevel server)) return;

            // 1.7 ChunkRadiationManager.incrementRad(x, y, z, 100): an unbounded add, so no cap here
            RadiationSystemNT.incrementRad(server, this.beast.blockPosition(), 100, Double.MAX_VALUE);
            Compat.hurt(target, server, ModDamageSources.source(server, ModDamageSources.RADIATION), 16.0F);
            this.beast.swing(InteractionHand.MAIN_HAND);
            this.beast.playAmbientSound();
            this.beast.attackDelay = 20;
        }
    }
}
