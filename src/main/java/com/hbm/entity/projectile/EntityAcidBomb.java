package com.hbm.entity.projectile;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Compat;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.lib.ModDamageSources;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class EntityAcidBomb extends EntityThrowableInterp {

    public static final RegistryObject<EntityType<EntityAcidBomb>> TYPE =
            ModNtmEntities.ENTITY_TYPES.register("entity_acid_bomb", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityAcidBomb>of(EntityAcidBomb::new, MobCategory.MISC)).sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1), key));

    public float damage = 1.5F;

    public EntityAcidBomb(EntityType<? extends EntityAcidBomb> type, Level level) {
        super(type, level);
    }

    public EntityAcidBomb(Level level, double x, double y, double z) {
        this(TYPE.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (level().isClientSide()) return;

        if (mop instanceof EntityHitResult entityHit) {
            Entity hit = entityHit.getEntity();

            if (!(hit instanceof EntityGlyphid)) {
                if (level() instanceof ServerLevel server) {
                    Compat.hurt(hit, server, acidDamageSource(), damage);
                }
                discard();
            }
        }

        if (mop instanceof BlockHitResult) {
            discard();
        }
    }

    private DamageSource acidDamageSource() {
        Holder<DamageType> holder = level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                                            .getOrThrow(ModDamageSources.ACID_PLAYER); // 1.7: s_acid = "acidPlayer"
        LivingEntity thrower = getThrower();
        return thrower != null ? new DamageSource(holder, this, thrower) : new DamageSource(holder);
    }

    @Override
    public double getGravityVelocity() {
        return 0.04D;
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putFloat("damage", damage);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.damage = input.getFloatOr("damage", 1.5F);
    }
}
