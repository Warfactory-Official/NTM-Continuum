package com.hbm.entity.mob.siege;

import com.hbm.entity.mob.EntityBurrowingSwingingBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Attributes.Op;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * A no-clip drill that swims through terrain toward players and rams them; it breaks no blocks. Like 1.7, neither
 * siege mob is registered.
 */
public class EntitySiegeTunneler extends EntityBurrowingSwingingBase {

    // 1.7 dataWatcher 12: the tier, synced by runtime index and saved by name
    private static final EntityDataAccessor<Integer> TIER =
            SynchedEntityData.defineId(EntitySiegeTunneler.class, EntityDataSerializers.INT);

    private static final String TIER_MODIFIER = "hbm:siege_tier_damage";

    public EntitySiegeTunneler(EntityType<? extends EntitySiegeTunneler> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        super.registerGoals();
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
            //worldObj.playSoundAtEntity(this, "random.break", 5F, 1.0F + rand.nextFloat() * 0.5F);
            return false;
        }

        damage *= (1F - tier.dr);

        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(TIER, 0);
    }

    /**
     * port: 1.7 added a fresh randomly-named damage modifier on every call, so each reload stacked another; one
     * transient modifier, re-applied with the tier on load, keeps the first application's value.
     */
    public void setTier(SiegeTier tier) {
        this.entityData.set(TIER, tier.id);

        dev.golem.core.world.Attributes.setTransient(getAttribute(Attributes.ATTACK_DAMAGE), TIER_MODIFIER, tier.damageMod, Op.MULTIPLY_BASE);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(tier.health * 0.5);
        setHealth(getMaxHealth());
    }

    public SiegeTier getTier() {
        int id = this.entityData.get(TIER);
        SiegeTier tier = id >= 0 && id < SiegeTier.getLength() ? SiegeTier.tiers[id] : null;
        return tier != null ? tier : SiegeTier.CLAY;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putString("siegeTier", getTier().name);
    }

    /** 1.7 re-applies the tier here, which also refills the health on every load. A missing key read as tier 0. */
    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        SiegeTier tier = SiegeTier.byName(input.getStringOr("siegeTier", ""));
        setTier(tier != null ? tier : SiegeTier.DEFAULT_BUFF);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        setTier(SiegeTier.tiers[this.random.nextInt(SiegeTier.getLength())]);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }
}
