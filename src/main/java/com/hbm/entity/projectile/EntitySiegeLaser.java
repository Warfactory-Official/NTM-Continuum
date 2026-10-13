package com.hbm.entity.projectile;

import dev.golem.core.data.Tags;

import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import com.hbm.lib.ModDamageSources;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 entity/projectile/EntitySiegeLaser: the siege craft's straight-flying zero-gravity bolt; colors via synched int, breaks blocks by chance on impact.
 */
public class EntitySiegeLaser extends EntityThrowableNT {

    // 1.7 dataWatcher id 12
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(EntitySiegeLaser.class, EntityDataSerializers.INT);

    private float damage = 2;
    private float explosive = 0F;
    private float breakChance = 0F;
    private boolean incendiary = false;

    public EntitySiegeLaser(EntityType<? extends EntitySiegeLaser> type, Level level) {
        super(type, level);
    }

    /**
     * 1.7 EntityThrowable(World, EntityLivingBase); its only caller is EntitySiegeCraft:263.
     * TODO(port): 1.7 entity/mob/siege (EntitySiegeCraft, EntitySiegeTunneler, SiegeTier) is unported. 1.7
     * constructs neither mob anywhere live, its one spawn site being commented out at
     * items/tool/ItemWandD.java:142, so the family is dead upstream too. This ctor stays caller-less either way.
     */
    public EntitySiegeLaser(EntityType<? extends EntitySiegeLaser> type, Level level, LivingEntity thrower) {
        super(type, level);
        initThrower(thrower);
    }

    public EntitySiegeLaser(EntityType<? extends EntitySiegeLaser> type, Level level, double x, double y, double z) {
        super(type, level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(COLOR, 0xffffff);
    }

    public EntitySiegeLaser setDamage(float f) {
        this.damage = f;
        return this;
    }

    public EntitySiegeLaser setExplosive(float f) {
        this.explosive = f;
        return this;
    }

    public EntitySiegeLaser setBreakChance(float f) {
        this.breakChance = f;
        return this;
    }

    public EntitySiegeLaser setIncendiary() {
        this.incendiary = true;
        return this;
    }

    public EntitySiegeLaser setColor(int color) {
        this.entityData.set(COLOR, color);
        return this;
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 60)
            this.discard(); // 1.7 setDead()
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (!(level() instanceof ServerLevel server)) return;

        Vec3 hitVec = mop.getLocation();

        if (mop instanceof EntityHitResult entityHit) {
            DamageSource dmg = laserDamageSource();
            Entity hitEntity = entityHit.getEntity();

            if (Compat.hurt(hitEntity, server, dmg, this.damage)) {
                this.discard();

                if (this.incendiary)
                    Effects.ignite(hitEntity, 3); // 1.7 setFire(3)

                // 1.7 newExplosion(..., isFlaming=incendiary, isSmoking=false): no block destruction
                if (this.explosive > 0)
                    server.explode(this, hitVec.x, hitVec.y, hitVec.z, this.explosive, this.incendiary,
                            Level.ExplosionInteraction.NONE);
            }

        } else if (mop instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();

            if (this.explosive > 0) {
                server.explode(this, hitVec.x, hitVec.y, hitVec.z, this.explosive, this.incendiary,
                        Level.ExplosionInteraction.NONE);

            } else if (this.incendiary) {
                // 1.7: ForgeDirection off mop.sideHit
                BlockPos target = pos.relative(blockHit.getDirection());

                if (server.getBlockState(target).canBeReplaced()) {
                    server.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                }
            }

            if (this.random.nextFloat() < this.breakChance) {
                server.destroyBlock(pos, false); // 1.7 func_147480_a(pos, drop=false)
            }

            this.discard();
        }
    }

    // 1.7: new EntityDamageSourceIndirect(ModDamageSource.s_laser, this, thrower) / plain DamageSource without a thrower
    private DamageSource laserDamageSource() {
        Holder<DamageType> holder = level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                                            .getOrThrow(ModDamageSources.LASER);
        LivingEntity thrower = getThrower();
        return thrower != null ? new DamageSource(holder, this, thrower) : new DamageSource(holder);
    }

    @Override
    public double getGravityVelocity() {
        return 0D; // 1.7 getGravityVelocity(): 0.0F
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putFloat("damage", this.damage);
        output.putFloat("explosive", this.explosive);
        output.putFloat("breakChance", this.breakChance);
        output.putBoolean("incendiary", this.incendiary);
        output.putInt("color", this.getColor());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.damage = input.getFloatOr("damage", 2F);
        this.explosive = input.getFloatOr("explosive", 0F);
        this.breakChance = input.getFloatOr("breakChance", 0F);
        this.incendiary = input.getBooleanOr("incendiary", false);
        this.setColor(input.getIntOr("color", 0xffffff));
    }
}
