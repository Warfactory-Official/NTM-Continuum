package com.hbm.entity.projectile;

import dev.golem.core.data.Tags;

import com.hbm.capability.HbmLivingProps;
import com.hbm.client.ChemicalClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** The original chemical gun splash, limited to fluid facts present in this port. */
public class EntityChemical extends EntityThrowableNT {

    private static final EntityDataAccessor<Integer> FLUID_TYPE =
            SynchedEntityData.defineId(EntityChemical.class, EntityDataSerializers.INT);

    public EntityChemical(EntityType<? extends EntityChemical> type, Level level) {
        super(type, level);
    }

    public EntityChemical(Level level, LivingEntity thrower, double sideOffset, double heightOffset,
            double frontOffset) {
        this(ModEntities.CHEMICAL.get(), level);
        this.thrower = thrower;
        setPos(thrower.getX(), thrower.getEyeY(), thrower.getZ());
        var offset = new net.minecraft.world.phys.Vec3(sideOffset, heightOffset, frontOffset)
                .xRot(-thrower.getXRot() * ((float) Math.PI / 180F))
                .yRot(-thrower.getYRot() * ((float) Math.PI / 180F));
        setPos(getX() + offset.x, getY() + offset.y, getZ() + offset.z);
        double yaw = thrower.getYRot() * Math.PI / 180D;
        double pitch = thrower.getXRot() * Math.PI / 180D;
        setThrowableHeading(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch), 1F, 0F);
    }

    public EntityChemical setFluid(FluidType fluid) {
        entityData.set(FLUID_TYPE, fluid == null ? Fluids.NONE.index() : fluid.index());
        return this;
    }

    public FluidType getFluid() { return Fluids.byIndex(entityData.get(FLUID_TYPE)); }

    @Override protected void defineData(SynchedData data) {
        super.defineData(data);
        data.define(FLUID_TYPE, Fluids.NONE.index());
    }

    @Override public int selfDamageDelay() { return 2; }

    @Override public void tick() {
        if (!level().isClientSide() && tickCount > getMaxAge()) {
            discard();
            return;
        }

        if (level().isClientSide()) {
            ChemicalClientEffects.tick(this);
        } else if (FluidCombustion.isGaseous(getFluid())) {
            double intensity = 1D - (double) tickCount / (double) getMaxAge();
            List<Entity> affected = level().getEntities(this,
                    getBoundingBox().inflate(intensity * 2.5D),
                    entity -> entity != thrower && !(entity instanceof Player player
                            && (player.isSpectator() || player.isCreative())));
            for (Entity entity : affected) affect(entity, intensity);
        }
        super.tick();
    }

    @Override protected float getAirDrag() {
        return switch (style()) {
            case GAS, GASFLAME -> 0.95F;
            case AMAT, LIGHTNING -> 1F;
            default -> 0.99F;
        };
    }

    @Override protected float getWaterDrag() {
        return switch (style()) {
            case GAS, GASFLAME, AMAT, LIGHTNING -> 1F;
            default -> 0.8F;
        };
    }

    @Override public double getGravityVelocity() {
        return switch (style()) {
            case AMAT, LIGHTNING, GAS -> 0F;
            case GASFLAME -> -0.01F;
            default -> 0.03F;
        };
    }

    public int getMaxAge() {
        return switch (style()) {
            case AMAT -> 100;
            case LIGHTNING -> 5;
            case BURNING -> 600;
            case GAS -> 60;
            case GASFLAME -> 20;
            case LIQUID -> 600;
            default -> 100;
        };
    }

    public ChemicalStyle style() { return getStyleFromType(getFluid()); }

    public static ChemicalStyle getStyleFromType(FluidType type) {
        if (type == Fluids.IONGEL) return ChemicalStyle.LIGHTNING;
        if (type == Fluids.AMAT || type == Fluids.ASCHRAB) return ChemicalStyle.AMAT;
        if (FluidCombustion.isGaseous(type)) {
            return FluidCombustion.isFlammable(type) || FluidCombustion.getCombustible(type) != null
                    ? ChemicalStyle.GASFLAME : ChemicalStyle.GAS;
        }
        // The old fluid trait table marks ordinary water, diesel and acids as LIQUID even when they are
        // not viscous.  The slim port retains gaseous/combustion facts, so the non-gaseous remainder is
        // the faithful liquid branch; restricting this to the viscous helper would make water and diesel
        // silently lose the extinguisher/soaked behavior.
        if (type != Fluids.NONE) {
            return FluidCombustion.getCombustible(type) != null ? ChemicalStyle.BURNING : ChemicalStyle.LIQUID;
        }
        return ChemicalStyle.NULL;
    }

    @Override protected void onImpact(HitResult hit) {
        if (level().isClientSide()) return;
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
            affect(entityHit.getEntity(), 1D - (double) tickCount / getMaxAge());
            // The old chemical entity continues its cloud/spray lifetime after touching an entity;
            // only a block impact consumes the projectile.
            return;
        } else if (hit instanceof BlockHitResult blockHit) {
            blockImpact(blockHit);
            discard();
        }
    }

    private void affect(Entity entity, double intensity) {
        if (entity instanceof Player player && (player.isSpectator() || player.isCreative())) return;
        FluidType type = getFluid();
        LivingEntity living = entity instanceof LivingEntity value ? value : null;
        if (style() == ChemicalStyle.LIQUID || style() == ChemicalStyle.BURNING) intensity = 1D;
        switch (style()) {
            case AMAT -> {
                if (level() instanceof ServerLevel server) {
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, ModDamageSources.radiation(server), 1F);
                    if (living != null) ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                            ContaminationUtil.ContaminationType.CREATIVE, 50F * (float) intensity);
                }
                if (living != null) return;
            }
            case LIGHTNING -> {
                if (level() instanceof ServerLevel server)
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, ModDamageSources.electricity(server), 0.5F);
                if (living != null) {
                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 9));
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 9));
                }
                if (living != null) return;
            }
            default -> { }
        }

        if (type.temperature >= 500) Effects.ignite(entity, 10);
        if (style() == ChemicalStyle.LIQUID && FluidCombustion.isFlammable(type) && living != null)
            HbmLivingProps.setOil(living, 300);
        if (isExtinguishing() && level() instanceof ServerLevel) entity.clearFire();
        if (style() == ChemicalStyle.BURNING || style() == ChemicalStyle.GASFLAME) Effects.ignite(entity,
                (int) Math.ceil(5D * Math.max(intensity, 0D)));

        FT_Corrosive corrosive = type.getTrait(FT_Corrosive.class);
        if (corrosive != null && living != null && level() instanceof ServerLevel server) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(living, ModDamageSources.source(server, ModDamageSources.ACID),
                    corrosive.getRating() / 50F);
        }
    }

    private void blockImpact(BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        FluidType type = getFluid();
        if (style() == ChemicalStyle.BURNING || style() == ChemicalStyle.GASFLAME) {
            for (var dir : net.minecraft.core.Direction.values()) {
                BlockPos adjacent = pos.relative(dir);
                if (level().getBlockState(adjacent).isAir()) level().setBlockAndUpdate(adjacent,
                        net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            }
        }
        if (isExtinguishing()) {
            for (var dir : net.minecraft.core.Direction.values()) {
                BlockPos adjacent = pos.relative(dir);
                if (level().getBlockState(adjacent).is(net.minecraft.world.level.block.Blocks.FIRE))
                    level().setBlockAndUpdate(adjacent, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }
        if (type == Fluids.SEEDSLURRY) {
            var state = level().getBlockState(pos);
            if (state.is(net.minecraft.world.level.block.Blocks.DIRT))
                level().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
        }
    }

    private boolean isExtinguishing() {
        FluidType type = getFluid();
        return style() == ChemicalStyle.LIQUID && type.temperature < 50 && !FluidCombustion.isFlammable(type);
    }

    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        output.putString("fluid", Fluids.byIndex(entityData.get(FLUID_TYPE)).id().toString());
    }

    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        entityData.set(FLUID_TYPE, Fluids.byId(input.getStringOr("fluid", "")).index());
    }

    public enum ChemicalStyle { AMAT, LIGHTNING, LIQUID, GAS, GASFLAME, BURNING, NULL }
}
