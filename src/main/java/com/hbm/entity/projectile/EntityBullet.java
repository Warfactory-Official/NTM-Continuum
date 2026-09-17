package com.hbm.entity.projectile;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.entity.mob.EntityCreeperNuclear;
import java.util.List;

import com.hbm.blocks.bomb.BlockDetonatable;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageSources;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

// 26.2: getBrightnessForRender fullbright is renderer-side - the bullet renderer must force max light for
// critical/chopper shots.
@Deprecated // ancient ass fucking bullet entity from pre ItemGunBase days
public class EntityBullet extends CompatEntity {

    // 1.7 dataWatcher ids 16/17/18 - crit / tau / chopper flag bytes
    private static final EntityDataAccessor<Byte> CRITICAL =
            SynchedEntityData.defineId(EntityBullet.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> TAU =
            SynchedEntityData.defineId(EntityBullet.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> CHOPPER =
            SynchedEntityData.defineId(EntityBullet.class, EntityDataSerializers.BYTE);

    private int tileX = -1;
    private int tileY = -1;
    private int tileZ = -1;
    public double gravity = 0.0D;
    private BlockState stuckBlock; // 1.7 field_145790_g (the inTile Block)
    private int inData;
    private boolean inGround;
    /** 1 if the player can pick up the arrow */
    public int canBePickedUp;
    /** Seems to be some sort of timer for animating an arrow. */
    public int arrowShake;
    /** The owner of this bullet. */
    public Entity shootingEntity;
    private int ticksInGround;
    private int ticksInAir;
    public double damage;
    /** The amount of knockback an arrow applies when it hits a mob. */
    private int knockbackStrength;
    private boolean instakill = false;
    private boolean rad = false;
    public boolean antidote = false;
    public boolean pip = false;
    public boolean fire = false;

    public EntityBullet(EntityType<? extends EntityBullet> type, Level level) {
        super(type, level);
    }

    public EntityBullet(EntityType<? extends EntityBullet> type, Level level, double x, double y, double z) {
        this(type, level);
        this.snapTo(x, y, z); // 1.7: setPosition + yOffset = 0 (no yOffset in 26.2)
    }

    public EntityBullet(EntityType<? extends EntityBullet> type, Level level, LivingEntity shooter, LivingEntity target,
            float velocity, float inaccuracy) {
        super(type, level);
        this.shootingEntity = shooter;

        if (shooter instanceof Player) {
            this.canBePickedUp = 1;
        }

        setPos(getX(), shooter.getY() + shooter.getEyeHeight() - 0.10000000149011612D, getZ());
        double d0 = target.getX() - shooter.getX();
        double d1 = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - this.getY();
        double d2 = target.getZ() - shooter.getZ();
        double d3 = (float) Math.sqrt(d0 * d0 + d2 * d2);

        if (d3 >= 1.0E-7D) {
            float f2 = (float) (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
            float f3 = (float) (-(Math.atan2(d1, d3) * 180.0D / Math.PI));
            double d4 = d0 / d3;
            double d5 = d2 / d3;
            this.snapTo(shooter.getX() + d4, this.getY(), shooter.getZ() + d5, f2, f3);
            float f4 = 0;//(float) d3 * 0.2F;
            this.setThrowableHeading(d0, d1 + f4, d2, velocity, inaccuracy);
        }
    }

    public EntityBullet(EntityType<? extends EntityBullet> type, Level level, LivingEntity shooter, float velocity,
            int dmgMin, int dmgMax, boolean instakill, boolean rad) {
        super(type, level);
        this.shootingEntity = shooter;

        if (shooter instanceof Player) {
            this.canBePickedUp = 1;
        }

        this.snapTo(shooter.getX(), shooter.getY() + shooter.getEyeHeight(), shooter.getZ(), shooter.getYRot(),
                shooter.getXRot());
        this.setPos(this.getX() - Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F,
                this.getY() - 0.10000000149011612D,
                this.getZ() - Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F);

        double motionX = -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionZ = Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.setThrowableHeading(motionX, motionY, motionZ, velocity * 1.5F, 1.0F);

        // this.dmgMin = dmgMin;
        // this.dmgMax = dmgMax;
        this.instakill = instakill;
        this.rad = rad;
    }

    public EntityBullet(EntityType<? extends EntityBullet> type, Level level, LivingEntity shooter, float velocity) {
        super(type, level);
        this.shootingEntity = shooter;

        if (shooter instanceof Player) {
            this.canBePickedUp = 1;
        }

        this.snapTo(shooter.getX(), shooter.getY() + shooter.getEyeHeight(), shooter.getZ(), shooter.getYRot(),
                shooter.getXRot());
        this.setPos(this.getX() - Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F,
                this.getY() - 0.10000000149011612D,
                this.getZ() - Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F);

        double motionX = -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionZ = Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.setThrowableHeading2(motionX, motionY, motionZ, velocity * 1.5F, 1.0F);
    }

    public EntityBullet(EntityType<? extends EntityBullet> type, Level level, LivingEntity shooter, float velocity,
            int dmgMin, int dmgMax, boolean instakill, String isTau) {
        super(type, level);
        this.shootingEntity = shooter;

        if (shooter instanceof Player) {
            this.canBePickedUp = 1;
        }

        if(shooter != null) this.snapTo(shooter.getX(), shooter.getY() + shooter.getEyeHeight(), shooter.getZ(),
                shooter.getYRot(), shooter.getXRot());
        this.setPos(this.getX() - Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F,
                this.getY() - 0.10000000149011612D,
                this.getZ() - Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * 0.16F);

        double motionX = -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionZ = Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.setThrowableHeading(motionX, motionY, motionZ, velocity * 1.5F, 1.0F);
        this.setTau(isTau == "tauDay"); // 1.7: identity compare against interned literals, kept verbatim
        this.setChopper(isTau == "chopper");
        this.setIsCritical(isTau != "chopper");
    }

    public EntityBullet(EntityType<? extends EntityBullet> type, Level level, int x, int y, int z, double mx,
            double my, double mz, double grav) {
        super(type, level);
        this.setPos(x + 0.5F, y + 0.5F, z + 0.5F);
        this.setDeltaMovement(new Vec3(mx, my, mz));
        this.gravity = grav;
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(CRITICAL, (byte) 0);
        builder.define(TAU, (byte) 0);
        builder.define(CHOPPER, (byte) 0);
    }

    /**
     * 1.7 setThrowableHeading: gaussian spread scaled by 0.0075, NOT vanilla shoot()'s triangular spread.
     */
    public void setThrowableHeading(double motionX, double motionY, double motionZ, float velocity, float inaccuracy) {
        this.setHeading(motionX, motionY, motionZ, velocity, inaccuracy, 0.007499999832361937D);
    }

    /**
     * 1.7 setThrowableHeading2: the wide-spread variant (0.0425).
     */
    public void setThrowableHeading2(double motionX, double motionY, double motionZ, float velocity, float inaccuracy) {
        this.setHeading(motionX, motionY, motionZ, velocity, inaccuracy, 0.042499999832361937D);
    }

    private void setHeading(double motionX, double motionY, double motionZ, float velocity, float inaccuracy,
            double spread) {
        float f2 = (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= f2;
        motionY /= f2;
        motionZ /= f2;
        motionX += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * spread * inaccuracy;
        motionY += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * spread * inaccuracy;
        motionZ += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * spread * inaccuracy;
        motionX *= velocity;
        motionY *= velocity;
        motionZ *= velocity;
        this.setDeltaMovement(motionX, motionY, motionZ);
        float f3 = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        float yaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motionY, f3) * 180.0D / Math.PI);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(pitch);
        this.xRotO = pitch;
        this.ticksInGround = 0;
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void tick() {
        super.tick();

        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            Vec3 vel = this.getDeltaMovement();
            float yaw = (float) (Math.atan2(vel.x, vel.z) * 180.0D / Math.PI);
            this.setYRot(yaw);
            this.yRotO = yaw;
            // 1.7 leaves pitch unset here
        }

        // 1.7 resolved the unrecorded (-1,-1,-1) to air, being below the world; y=-1 is valid terrain now, so
        // gate on "tile recorded" rather than letting every in-flight shot read chunk (-1,-1) each tick.
        BlockPos stuckPos = new BlockPos(this.tileX, this.tileY, this.tileZ);
        BlockState state = this.tileX != -1 || this.tileY != -1 || this.tileZ != -1
                ? level().getBlockState(stuckPos) : Blocks.AIR.defaultBlockState();

        if (!state.isAir()) {
            // 1.7: setBlockBoundsBasedOnState + getCollisionBoundingBoxFromPool + isVecInside
            VoxelShape shape = state.getCollisionShape(level(), stuckPos);
            if (!shape.isEmpty()) {
                Vec3 pos = this.position();

                for(AABB box : shape.toAabbs()) {
                    if (box.move(stuckPos).contains(pos) && !this.getIsCritical()) {
                        this.inGround = true;
                    }
                }
            }

            if (state.getBlock() instanceof BlockDetonatable detonatable) {
                detonatable.onShot(level(), stuckPos);
            }

            // 1.7: glass, stained_glass, glass_pane, stained_glass_pane
            if (state.is(Blocks.GLASS) || state.getBlock() instanceof StainedGlassBlock
                    || state.is(Blocks.GLASS_PANE) || state.getBlock() instanceof StainedGlassPaneBlock) {
                level().removeBlock(stuckPos, false);
                level().playSound(null, stuckPos.getX(), stuckPos.getY(), stuckPos.getZ(),
                        SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F); // 1.7 "dig.glass"
            }
        }

        if (this.arrowShake > 0) {
            --this.arrowShake;
        }

        if (this.inGround && !this.getIsCritical()) {
            this.discard();

        } else {
            ++this.ticksInAir;
            Vec3 vel = this.getDeltaMovement();
            Vec3 vec31 = this.position();
            Vec3 vec3 = vec31.add(vel);
            // 1.7 func_147447_a(..., stopOnLiquid=false, ignoreNoBox=true, lastUncollidable=false)
            BlockHitResult blockHit = level().clip(new ClipContext(vec31, vec3, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            HitResult mop = blockHit.getType() == HitResult.Type.MISS ? null : blockHit;
            vec31 = this.position();
            vec3 = vec31.add(vel);

            if (mop != null) {
                vec3 = mop.getLocation();
            }

            Entity entity = null;
            List<Entity> list = level().getEntities(this,
                    this.getBoundingBox().expandTowards(vel).inflate(1.0D, 1.0D, 1.0D));
            double d0 = 0.0D;
            int i;

            for (i = 0; i < list.size(); ++i) {
                Entity entity1 = list.get(i);

                if (entity1.isPickable() && (entity1 != this.shootingEntity || this.ticksInAir >= 5)) {
                    float f1 = 0.3F;
                    AABB aabb = entity1.getBoundingBox().inflate(f1, f1, f1);
                    Vec3 hitVec = aabb.clip(vec31, vec3).orElse(null);

                    if (hitVec != null) {
                        double d1 = vec31.distanceTo(hitVec);

                        if (d1 < d0 || d0 == 0.0D) {
                            entity = entity1;
                            d0 = d1;
                        }
                    }
                }
            }

            if (entity != null) {
                mop = new EntityHitResult(entity);
            }

            if (mop instanceof EntityHitResult guardHit && guardHit.getEntity() instanceof Player player) {

                if (player.getAbilities().invulnerable // 1.7 capabilities.disableDamage
                        || this.shootingEntity instanceof Player shooter && !shooter.canHarmPlayer(player)) {
                    mop = null;
                }
            }

            float f2;

            if (mop != null) {
                if (mop instanceof EntityHitResult entityHit) {
                    Entity hit = entityHit.getEntity();

                    // 1.7: everything takes the hit except an item frame displaying flame_pony
                    if (!(hit instanceof ItemFrame frame)
                            || frame.getItem().isEmpty()
                            || !frame.getItem().is(ModItems.flame_pony.get())) {

                        f2 = (float) Math.sqrt(vel.x * vel.x + vel.y * vel.y + vel.z * vel.z);
                        int k = Mth.ceil(f2 * this.damage);

                        if (this.getIsCritical()) {
                            k += this.random.nextInt(k / 2 + 2);
                        }

                        DamageSource damagesource = null;

                        //L: Crit
                        //R: Chop
                        //X: NOT
                        //O: Direct
                        //
                        //   X X   Bullet
                        //    \|
                        //   O-X   Tau
                        //   |/
                        //   X-O   Displacer

                        if (!this.getIsCritical() && !this.getIsChopper()) {
                            damagesource = this.causeBulletDamage(this.shootingEntity == null ? this : this.shootingEntity);
                        } else if(!this.getIsChopper()) {
                            damagesource = this.causeTauDamage(this.shootingEntity == null ? this : this.shootingEntity);
                        } else if(!this.getIsCritical()) {
                            damagesource = this.causeDisplacementDamage(this.shootingEntity == null ? this : this.shootingEntity);
                        }

                        if (this.fire || this.isOnFire() && !(hit instanceof EnderMan)) {
                            dev.golem.core.world.Effects.ignite(hit, 5F); // 1.7 setFire(5)
                        }

                        // 1.7 attackEntityFrom no-ops on the client - keep the whole damage subtree server-side
                        ServerLevel server = level() instanceof ServerLevel serverLevel ? serverLevel : null;
                        boolean attacked = server != null && Compat.hurt(hit, server, damagesource, (float) this.damage);

                        if (attacked) {
                            if (hit instanceof LivingEntity entitylivingbase) {

                                if (this.rad) {
                                    if (entitylivingbase instanceof Player
                                            && ArmorUtil.checkForHazmat(entitylivingbase)) {
                                        // hazmat shrugs it off
                                    } else if (entitylivingbase instanceof Creeper) {
                                        EntityCreeperNuclear creep =
                                                new EntityCreeperNuclear(ModNtmEntities.CREEPER_NUCLEAR.get(), level());
                                        creep.snapTo(entitylivingbase.getX(), entitylivingbase.getY(),
                                                entitylivingbase.getZ(), entitylivingbase.getYRot(),
                                                entitylivingbase.getXRot());
                                        entitylivingbase.discard();
                                        if (!level().isClientSide())
                                            level().addFreshEntity(creep);
                                    } else if (entitylivingbase instanceof Villager) {
                                        Zombie creep = new Zombie(level());
                                        creep.snapTo(entitylivingbase.getX(), entitylivingbase.getY(),
                                                entitylivingbase.getZ(), entitylivingbase.getYRot(),
                                                entitylivingbase.getXRot());
                                        entitylivingbase.discard();
                                        if (!level().isClientSide())
                                            level().addFreshEntity(creep);
                                    } else if (!(entitylivingbase instanceof Zombie)
                                            && !(entitylivingbase instanceof MushroomCow)
                                            && !(entitylivingbase instanceof EntityCreeperNuclear)) {
                                        entitylivingbase.addEffect(new MobEffectInstance(MobEffects.POISON, 2 * 60 * 20, 2));
                                        entitylivingbase.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 4));
                                        entitylivingbase.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 1 * 60 * 20, 1)); // 1.7 Potion.moveSlowdown
                                    }
                                }

                                if(this.antidote)
                                    entitylivingbase.removeAllEffects(); // 1.7 clearActivePotions

                                if (this.knockbackStrength > 0) {
                                    f2 = (float) Math.sqrt(vel.x * vel.x + vel.z * vel.z);

                                    if (f2 > 0.0F) {
                                        hit.push(
                                                vel.x * this.knockbackStrength * 0.6000000238418579D / f2, 0.1D,
                                                vel.z * this.knockbackStrength * 0.6000000238418579D / f2);
                                    }
                                }

                                if (this.shootingEntity instanceof LivingEntity) {
                                    // 1.7 EnchantmentHelper.func_151384_a + func_151385_b pair
                                    dev.golem.core.world.Compat.doPostAttackEffects(server, entitylivingbase, damagesource);
                                }

                                if (this.shootingEntity instanceof ServerPlayer shooterPlayer
                                        && hit != this.shootingEntity && hit instanceof Player) {
                                    shooterPlayer.connection.send(new ClientboundGameEventPacket(
                                            ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND, 0.0F)); // 1.7 S2BPacketChangeGameState(6, 0)
                                }
                            }

                            if (!(hit instanceof EnderMan)) {
                                if (!level().isClientSide()) {
                                    if (!this.instakill || hit instanceof Player) {
                                        // movingobjectposition.entityHit.attackEntityFrom(DamageSource.generic, dmgMin + rand.nextInt(dmgMax - dmgMin));
                                    } else if (hit instanceof LivingEntity living) {
                                        living.setHealth(0.0F);
                                    }
                                }
                            }
                        } else {

                            if (server != null && hit instanceof LivingEntity living) {
                                // 1.7: reflected EntityLivingBase.lastDamage - a plain public field in 26.2
                                float dmg = (float) this.damage + ((dev.golem.core.world.LivingHurtAccess) living).golem$lastHurt();

                                Compat.hurt(living, server, damagesource, dmg);
                            }

                        }

                    } else {
                        this.discard();
                    }
                } else if (!this.getIsCritical()) {
                    BlockHitResult blockResult = (BlockHitResult) mop;
                    BlockPos hitPos = blockResult.getBlockPos();
                    this.tileX = hitPos.getX();
                    this.tileY = hitPos.getY();
                    this.tileZ = hitPos.getZ();
                    this.stuckBlock = level().getBlockState(hitPos);
                    this.inData = 0; // 1.7: block metadata, gone in 26.2
                    Vec3 hitVec = blockResult.getLocation();
                    double motionX = hitVec.x - this.getX();
                    double motionY = hitVec.y - this.getY();
                    double motionZ = hitVec.z - this.getZ();
                    f2 = (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
                    this.setPos(this.getX() - motionX / f2 * 0.05000000074505806D,
                            this.getY() - motionY / f2 * 0.05000000074505806D,
                            this.getZ() - motionZ / f2 * 0.05000000074505806D);
                    this.setDeltaMovement(motionX, motionY, motionZ);
                    this.inGround = true;
                    this.arrowShake = 7;

                    // 26.2: 1.7's onEntityCollidedWithBlock on the landed shot is unreachable - entityInside is
                    // protected - and baseTick's inside-block pass already covers the contact effects.
                }
            }

            if (this.getIsCritical()) {
                Vec3 step = this.getDeltaMovement();
                for (i = 0; i < 8; ++i) {
                    if (!this.getIsTau())
                        level().addParticle(ParticleTypes.FIREWORK, // 1.7 "fireworksSpark"
                                this.getX() + step.x * i / 8.0D,
                                this.getY() + step.y * i / 8.0D,
                                this.getZ() + step.z * i / 8.0D, 0, 0,
                                0/*-motionX, -motionY + 0.2D, -motionZ*/);
                    else
                        level().addParticle(DustParticleOptions.REDSTONE, // 1.7 "reddust"
                                this.getX() + step.x * i / 8.0D,
                                this.getY() + step.y * i / 8.0D,
                                this.getZ() + step.z * i / 8.0D, 0, 0,
                                0/*-motionX, -motionY + 0.2D, -motionZ*/);
                }
            }

            Vec3 motion = this.getDeltaMovement();
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));

            float f3 = 0.99F;
            float f1 = 0.05F;

            if (this.isInWater()) {
                for (int l = 0; l < 4; ++l) {
                    float f4 = 0.25F;
                    level().addParticle(ParticleTypes.BUBBLE, this.getX() - motion.x * f4,
                            this.getY() - motion.y * f4,
                            this.getZ() - motion.z * f4, motion.x, motion.y, motion.z);
                }

                f3 = 0.8F;
            }

            if (this.isInWaterOrRain()) { // 1.7 isWet
                this.clearFire(); // 1.7 extinguish
            }

            this.setDeltaMovement(motion.scale(f3).subtract(0, this.gravity, 0));
        }

        if (this.tickCount > 250) // 1.7 ticksExisted
            this.discard();
    }

    private DamageSource causeBulletDamage(Entity shooter) {
        return this.projectileSource(ModDamageSources.REVOLVER_BULLET, shooter);
    }

    private DamageSource causeTauDamage(Entity shooter) {
        return this.projectileSource(ModDamageSources.TAU, shooter);
    }

    private DamageSource causeDisplacementDamage(Entity shooter) {
        return this.projectileSource(ModDamageSources.EMPLACER, shooter);
    }

    private DamageSource projectileSource(ResourceKey<DamageType> type, Entity shooter) {
        Holder<DamageType> holder = level().registryAccess()
                                       .lookupOrThrow(Registries.DAMAGE_TYPE)
                                       .getOrThrow(type);
        if (shooter != null) return new DamageSource(holder, this, shooter);
        return new DamageSource(holder, this);
    }

    /**
     * Called by a player entity when they collide with an entity
     */
    @Override
    public void playerTouch(Player player) { // 1.7 onCollideWithPlayer
        if (!level().isClientSide() && this.inGround && this.arrowShake <= 0) {
            boolean flag = this.canBePickedUp == 1 || this.canBePickedUp == 2 && dev.golem.core.world.Compat.hasInfiniteMaterials(player); // 1.7 capabilities.isCreativeMode

            if (flag) {
                player.take(this, 1); // 1.7 onItemPickup
                this.discard();
            }
        }
    }

    /**
     * returns if this entity triggers Block.onEntityWalking on the blocks they walk on. used for spiders and wolves to prevent them from trampling crops
     */
    @Override
    public boolean isIgnoringBlockTriggers() { // 1.7 canTriggerWalking
        return true;
    }

    @Override
    public boolean isAttackable() { // 1.7 canAttackWithItem - punching the bullet does nothing
        return false;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getDamage() {
        return this.damage;
    }

    /**
     * Sets the amount of knockback the arrow applies when it hits a mob.
     */
    public void setKnockbackStrength(int knockbackStrength) {
        this.knockbackStrength = knockbackStrength;
    }

    /**
     * Whether the arrow has a stream of critical hit particles flying behind it.
     */
    public void setIsCritical(boolean value) {
        this.setFlag(CRITICAL, value);
    }

    public void setTau(boolean value) {
        this.setFlag(TAU, value);
    }

    public void setChopper(boolean value) {
        this.setFlag(CHOPPER, value);
    }

    /**
     * Whether the arrow has a stream of critical hit particles flying behind it.
     */
    public boolean getIsCritical() {
        return (this.entityData.get(CRITICAL) & 1) != 0;
    }

    public boolean getIsTau() {
        return (this.entityData.get(TAU) & 1) != 0;
    }

    public boolean getIsChopper() {
        return (this.entityData.get(CHOPPER) & 1) != 0;
    }

    private void setFlag(EntityDataAccessor<Byte> flag, boolean value) { // 1.7 dataWatcher bit twiddling
        byte b0 = this.entityData.get(flag);

        if (value) {
            this.entityData.set(flag, (byte) (b0 | 1));
        } else {
            this.entityData.set(flag, (byte) (b0 & -2));
        }
    }

    // 1.7 renderDistanceWeight = 10.0D
    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        double perimeter = this.getBoundingBox().getSize() * 10.0D;
        perimeter *= 64.0D;
        return dist < perimeter * perimeter;
    }

    // 1.7 setSize(0.5F, 0.5F)
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(0.5F, 0.5F);
    }

    @Override
    public float getLightLevelDependentMagicValue() { // 1.7 getBrightness
        if(this.getIsCritical() || this.getIsChopper())
            return 1.0F;
        else
            return super.getLightLevelDependentMagicValue();
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void saveData(DataOut output) {
        output.putShort("xTile", (short) this.tileX);
        output.putShort("yTile", (short) this.tileY);
        output.putShort("zTile", (short) this.tileZ);
        output.putShort("life", (short) this.ticksInGround);
        output.storeNullable("inTile", BlockState.CODEC, this.stuckBlock); // 1.7: byte block id
        output.putByte("inData", (byte) 0); // 1.7: block metadata - gone in 26.2
        output.putByte("shake", (byte) this.arrowShake);
        output.putByte("inGround", (byte) (this.inGround ? 1 : 0));
        output.putByte("pickup", (byte) this.canBePickedUp);
        output.putDouble("damage", this.damage);
    }

    @Override
    protected void loadData(DataIn input) {
        this.tileX = input.getShortOr("xTile", (short) -1);
        this.tileY = input.getShortOr("yTile", (short) -1);
        this.tileZ = input.getShortOr("zTile", (short) -1);
        this.ticksInGround = input.getShortOr("life", (short) 0);
        this.stuckBlock = input.read("inTile", BlockState.CODEC).orElse(null);
        this.inData = input.getByteOr("inData", (byte) 0) & 255;
        this.arrowShake = input.getByteOr("shake", (byte) 0) & 255;
        this.inGround = input.getByteOr("inGround", (byte) 0) == 1;

        this.damage = input.getDoubleOr("damage", 0.0D);

        this.canBePickedUp = input.getByteOr("pickup",
                (byte) (input.getBooleanOr("player", false) ? 1 : 0)); // 1.7 legacy "player" fallback
    }
}
