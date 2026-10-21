package com.hbm.entity.effect;

import dev.golem.core.world.Explosions;
import dev.golem.core.data.Tags;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.ModEntities;
import com.hbm.handler.ArmorUtil;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Pheromone;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_Toxin;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.util.ContaminationUtil;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A cloud of fluid hanging in the air: an invisible box that applies its fluid's contact effects to
 * everything inside it, thinning as it ages, and never moving after it is placed.
 *
 * <p>Sibling of {@link EntityFireLingering}, same stationary-area shape, and likewise it does not
 * call {@code super.tick()}: a gas cloud must not drown, suffocate or be pushed by fluids.
 *
 * <p>What a cloud does is read off the fluid, in 1.7's order: temperature (scalds above 100 °C, freezes
 * below −20 °C), {@code FT_Delicious} healing, oil-soaking for a flammable {@code FT_Liquid},
 * extinguishing, {@link FT_Corrosive}, {@link FT_VentRadiation} contamination, {@link FT_Poison},
 * {@link FT_Toxin}, then {@link FT_Pheromone}. A radioactive cloud also adds {@code radPerMB * 2} to its
 * chunk every tick. Ender juice teleports a living thing at random, as 1.7 (between toxin and pheromone).
 */
public class EntityMist extends CompatEntity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
            SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.FLOAT);

    public int maxAge = 150;

    public EntityMist(EntityType<? extends EntityMist> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EntityMist(Level level) {
        this(ModEntities.MIST.get(), level);
    }

    public EntityMist setArea(float width, float height) {
        this.entityData.set(WIDTH, width);
        this.entityData.set(HEIGHT, height);
        return this;
    }

    public EntityMist setDuration(int duration) {
        this.maxAge = duration;
        return this;
    }

    public EntityMist setType(FluidType fluid) {
        this.entityData.set(TYPE, Fluids.networkId(fluid));
        return this;
    }

    public FluidType getFluid() {
        return Fluids.byNetworkId(this.entityData.get(TYPE));
    }

    public int getMaxAge() {
        return maxAge;
    }

    /** Re-seeds this cloud's own random (ender juice's teleport target), for GameTests. */
    public void seedRandom(long seed) {
        this.random.setSeed(seed);
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(TYPE, 0);
        data.define(WIDTH, 0F);
        data.define(HEIGHT, 0F);
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    /**
     * 1.7's cloud is not fire-immune: {@code isBurning()} makes a flammable cloud explode, and a hot cloud or a
     * flame sets it. The entity type is registered {@code fireImmune()}, which would make {@link #isOnFire}
     * always false, so the cloud overrides it. The cloud never runs the vanilla base tick (no fire countdown,
     * lava or fire-block contact), as with 1.7's overridden {@code onEntityUpdate}.
     */
    @Override
    public boolean fireImmune() {
        return false;
    }

    @Override
    public void tick() {
        this.tickCount++;
        float width = this.entityData.get(WIDTH);
        float height = this.entityData.get(HEIGHT);

        if (this.level() instanceof ServerLevel server) {
            // 1.7 calls setDead() at maxAge and still runs that tick's effects (at intensity 0): boil and acid
            // damage are not scaled by intensity, so the last tick still lands one full hit.
            boolean expired = this.tickCount >= getMaxAge();
            spread(server, width, height);
            if (expired) this.discard();
        } else {
            drift(width, height);
        }
    }

    private void spread(ServerLevel server, float width, float height) {
        FluidType type = getFluid();
        if (type == null) return;

        // 1.7: a radioactive cloud irradiates the block it stands in every tick, before anything else.
        FT_VentRadiation vent = type.getTrait(FT_VentRadiation.class);
        if (vent != null) {
            ChunkRadiationManager.proxy.incrementRad(server, BlockPos.containing(getX(), getY(), getZ()),
                    vent.getRadPerMB() * 2);
        }

        // A cloud thins as it ages, and everything it does scales with what is left of it.
        double intensity = 1D - (double) this.tickCount / (double) getMaxAge();

        if (FluidCombustion.isFlammable(type) && isOnFire()) {
            Explosions.explode(server, this, getX(), getY() + height / 2D, getZ(), (float) intensity * 15F,
                    Level.ExplosionInteraction.BLOCK);
            this.discard();
            return;
        }

        List<Entity> affected = this.level().getEntities(this, box(width, height),
                EntityMist::isAffected);

        for (Entity e : affected) affect(server, e, type, intensity);
    }

    /**
     * Whether a cloud acts on {@code e}. 1.7's scan excludes only the cloud itself: creative players get its
     * effects, and so do other clouds. A 500 °C+ cloud sets an overlapping cloud on fire (a flammable one then
     * explodes on its next tick) and a cool non-flammable cloud puts a burning one out. Only spectators, which
     * 1.7 lacks, are skipped.
     */
    public static boolean isAffected(Entity e) {
        return !e.isSpectator();
    }

    private AABB box(float width, float height) {
        return new AABB(getX() - width / 2D, getY(), getZ() - width / 2D,
                getX() + width / 2D, getY() + height, getZ() + width / 2D);
    }

    private void affect(ServerLevel server, Entity e, FluidType type, double intensity) {
        LivingEntity living = e instanceof LivingEntity ? (LivingEntity) e : null;
        int temperature = type.temperature;

        if (temperature >= 100) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(e, ModDamageSources.source(server, ModDamageSources.BOIL),
                    0.2F + (temperature - 100) * 0.02F);
            if (temperature >= 500) Effects.ignite(e, 10);
        }

        if (temperature < -20 && living != null) {
            // The old formula is 0.2 + (temperature + 20) * -0.05; the strict branch starts below -20.
            EntityDamageUtil.attackEntityFromIgnoreIFrame(living,
                    ModDamageSources.source(server, ModDamageSources.ICE), 0.2F + (temperature + 20) * -0.05F);
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
            living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 4));
        }

        if (type.hasTrait(FluidTraitSimple.FT_Delicious.class) && living != null && living.isAlive()) {
            living.heal(2F * (float) intensity);
        }

        // 1.7 FT_Flammable && FT_Liquid: doused in oil for 10 seconds.
        if (FluidCombustion.isFlammable(type) && type.hasTrait(FluidTraitSimple.FT_Liquid.class) && living != null) {
            HbmLivingProps.setOil(living, 200);
        }

        if (isExtinguishing(type)) e.clearFire();

        FT_Corrosive corrosive = type.getTrait(FT_Corrosive.class);
        if (corrosive != null && living != null) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(living,
                    ModDamageSources.source(server, ModDamageSources.ACID), corrosive.getRating() / 60F);
            ArmorUtil.damageWholeSuit(living, corrosive.getRating() / 50); // every living thing, as 1.7
        }

        FT_VentRadiation vent = type.getTrait(FT_VentRadiation.class);
        if (vent != null && living != null) {
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                    ContaminationUtil.ContaminationType.CREATIVE, vent.getRadPerMB() * 5);
        }

        FT_Poison poison = type.getTrait(FT_Poison.class);
        if (poison != null && living != null) {
            living.addEffect(new MobEffectInstance(poison.isWithering() ? MobEffects.WITHER : MobEffects.POISON,
                    (int) (5 * 20 * intensity)));
        }

        FT_Toxin toxin = type.getTrait(FT_Toxin.class);
        if (toxin != null && living != null) toxin.affect(living, intensity);

        // 1.7: ender juice throws a living thing to a random spot, EntityChemical's teleport.
        if (type == Fluids.ENDERJUICE && living != null) EntityChemical.teleportRandomly(this, this.random, living);

        FT_Pheromone pheromone = type.getTrait(FT_Pheromone.class);
        if (pheromone != null && living != null) applyPheromone(living, pheromone);
    }

    /**
     * 1.7 {@code EntityMist}'s pheromone table: glyphid pheromone (type 1) buffs glyphids, the modified one
     * (type 2) buffs players, both scaled by the type number. Not {@code EntityChemical}'s table, which differs.
     */
    public static void applyPheromone(LivingEntity living, FT_Pheromone pheromone) {
        if ((living instanceof EntityGlyphid && pheromone.getType() == 1)
                || (living instanceof Player && pheromone.getType() == 2)) {
            int mult = pheromone.getType();
            Effects.add(living, Effects.speed(), mult * 60 * 20, 1);
            Effects.add(living, Effects.haste(), mult * 60 * 20, 1);
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, mult * 2 * 20, 0));
            Effects.add(living, Effects.resistance(), mult * 60 * 20, 0);
            Effects.add(living, Effects.strength(), mult * 60 * 20, 1);
            living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, mult * 60 * 20, 0));
        }
    }

    /** Anything cool and non-flammable puts a burning entity out. */
    protected boolean isExtinguishing(FluidType type) {
        return type.temperature < 50 && !FluidCombustion.isFlammable(type);
    }

    /** The cloud itself, drawn as drifting tower smoke tinted with the fluid's own colour. */
    private void drift(float width, float height) {
        FluidType type = getFluid();
        if (type == null) return;

        AABB box = box(width, height);
        for (int i = 0; i < 2; i++) {
            CompoundTag data = new CompoundTag();
            data.putFloat("lift", 0.5F);
            data.putFloat("base", 0.75F);
            data.putFloat("max", 2F);
            data.putInt("life", 50 + this.random.nextInt(10));
            data.putInt("color", type.getColor());
            HbmEffectNT.Tower.summonParticle(this.level(),
                    box.minX + this.random.nextDouble() * (box.maxX - box.minX),
                    box.minY + this.random.nextDouble() * (box.maxY - box.minY),
                    box.minZ + this.random.nextDouble() * (box.maxZ - box.minZ), data);
        }
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void saveData(DataOut output) {
        Fluids.write(output, "type", getFluid()); // registry name; the synched int is session-only
        output.putFloat("width", this.entityData.get(WIDTH));
        output.putFloat("height", this.entityData.get(HEIGHT));
    }

    @Override
    protected void loadData(DataIn input) {
        this.entityData.set(TYPE, Fluids.networkId(Fluids.read(input, "type")));
        setArea(input.getFloatOr("width", 0F), input.getFloatOr("height", 0F));
    }
}
