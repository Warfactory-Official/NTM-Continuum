package com.hbm.entity.effect;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.ModEntities;
import com.hbm.handler.ArmorUtil;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Effects;
import java.util.List;
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
 * <p>Sibling of {@link EntityFireLingering} — same stationary-area shape, and likewise it does not
 * call {@code super.tick()}: a gas cloud must not drown, suffocate or be pushed by fluids.
 *
 * <p>What a cloud does is read off the fluid, and this port can express what the fluid model carries:
 * temperature (scalds above 100 °C, freezes below −20 °C), {@link FT_Corrosive}, and flammability
 * through {@link FluidCombustion}. 1.12.2 also dispatches on poison, toxin, vent-radiation and
 * pheromone traits, and on the liquid/gaseous/viscous tags; none of those exist here yet, and the
 * fluids that carry them would be inert rather than wrong. The one consequence worth naming is that
 * <b>phosgene and mustard gas are visual</b>: upstream routes their entire lethality through
 * {@code FT_Polluting} and the pollution system, which this port defers wholesale. Chlorine, which
 * upstream makes corrosive, works.
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
        this.entityData.set(TYPE, fluid.getID());
        return this;
    }

    public FluidType getFluid() {
        return Fluids.fromID(this.entityData.get(TYPE));
    }

    public int getMaxAge() {
        return maxAge;
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

    @Override
    public void tick() {
        float width = this.entityData.get(WIDTH);
        float height = this.entityData.get(HEIGHT);

        if (this.level() instanceof ServerLevel server) spread(server, width, height);
        else drift(width, height);
    }

    private void spread(ServerLevel server, float width, float height) {
        if (this.tickCount >= getMaxAge()) {
            this.discard();
            return;
        }

        FluidType type = getFluid();
        if (type == null) return;

        // A cloud thins as it ages, and everything it does scales with what is left of it.
        double intensity = 1D - (double) this.tickCount / (double) getMaxAge();

        if (FluidCombustion.isFlammable(type) && isOnFire()) {
            server.explode(this, getX(), getY() + height / 2D, getZ(), (float) intensity * 15F,
                    Level.ExplosionInteraction.BLOCK);
            this.discard();
            return;
        }

        List<Entity> affected = this.level().getEntities(this, box(width, height),
                e -> !(e instanceof Player player && (player.isSpectator() || player.isCreative()))
                        && !(e instanceof EntityMist));

        for (Entity e : affected) affect(server, e, type, intensity);
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
            // 1.12.2: five damage at −20 °C, one more for every twenty below that.
            EntityDamageUtil.attackEntityFromIgnoreIFrame(living,
                    ModDamageSources.source(server, ModDamageSources.ICE), 0.2F + (temperature + 20) * -0.05F);
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
            living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 4));
        }

        // A flammable liquid douses whatever it lands on; a flammable gas does not stick.
        if (FluidCombustion.isFlammable(type) && !FluidCombustion.isGaseous(type) && living != null) {
            HbmLivingProps.setOil(living, 200);
        }

        if (isExtinguishing(type)) e.clearFire();

        FT_Corrosive corrosive = type.getTrait(FT_Corrosive.class);
        if (corrosive != null && living != null) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(living,
                    ModDamageSources.source(server, ModDamageSources.ACID), corrosive.getRating() / 60F);
            ArmorUtil.damageWholeSuit(living, corrosive.getRating() / 50);
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
        output.putInt("type", this.entityData.get(TYPE));
        output.putFloat("width", this.entityData.get(WIDTH));
        output.putFloat("height", this.entityData.get(HEIGHT));
        output.putInt("maxAge", this.maxAge);
    }

    @Override
    protected void loadData(DataIn input) {
        this.entityData.set(TYPE, input.getIntOr("type", 0));
        setArea(input.getFloatOr("width", 0F), input.getFloatOr("height", 0F));
        this.maxAge = input.getIntOr("maxAge", 150);
    }
}
