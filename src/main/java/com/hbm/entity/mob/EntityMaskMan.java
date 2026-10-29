package com.hbm.entity.mob;

import dev.golem.core.world.Explosions;
import com.hbm.api.item.IGasMask;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.ModGearItems;
import dev.golem.core.data.CompatMonster;
import com.hbm.advancement.BossKilledTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.mob.ai.EntityAIMaskmanCasualApproach;
import com.hbm.entity.mob.ai.EntityAIMaskmanLasergun;
import com.hbm.entity.mob.ai.EntityAIMaskmanMinigun;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;


public class EntityMaskMan extends CompatMonster implements IRadiationImmune {

    /** The authored special drop between the coin and skull. */
    public static Supplier<ItemStack> SPECIAL_DROP = () -> new ItemStack(ModArmorModItems.bottled_cloud.get());

    // 1.7's edge detector read EntityLivingBase.prevHealth, which starts at zero. Seeding this from the max health
    // instead re-armed the crossing on every load, so a mask man left below half health blew up again each time
    // its chunk came back.
    private float lastHealth;

    public EntityMaskMan(EntityType<? extends EntityMaskMan> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                      .add(Attributes.MAX_HEALTH, 1000.0D)
                      .add(Attributes.MOVEMENT_SPEED, 0.25D)
                      .add(Attributes.FOLLOW_RANGE, 100.0D)
                      .add(Attributes.ATTACK_DAMAGE, 15.0D)
                      .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new EntityAIMaskmanCasualApproach(this, Player.class, 1.0D, false));
        this.goalSelector.addGoal(2, new EntityAIMaskmanMinigun(this, 3));
        this.goalSelector.addGoal(3, new EntityAIMaskmanLasergun(this));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D)); // 1.7 EntityAIWander
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // 1.7 targetChance 0: re-acquire on every check, not one in ten
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (source.getDirectEntity() instanceof ThrownEgg && this.random.nextInt(10) == 0) {
            this.xpReward = 0;
            setHealth(0F);
            return true;
        }

        if (source.is(DamageTypeTags.IS_FIRE)) amount = 0F;
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) amount = 0F;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) amount *= 0.5F;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.5F;

        if (amount > 50F) amount = 50F + (amount - 50F) * 0.5F;

        return super.hurtServer(level, source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (level() instanceof ServerLevel server) {

            if (this.lastHealth >= getMaxHealth() / 2F && getHealth() < getMaxHealth() / 2F && isAlive()) {
                this.lastHealth = getHealth();
                // 1.7 createExplosion(..., true) broke blocks without consulting mobGriefing, so not MOB
                Explosions.explode(server, this, getX(), getY() + 4, getZ(), 2.5F, Level.ExplosionInteraction.BLOCK);
            }

            this.lastHealth = getHealth();

        }
    }

    /**
     * 1.7 turned the boss from {@code EntityAIMaskmanLasergun}, which wrote {@code rotationYaw = rotationYawHead}
     * and let {@code EntityLivingBase} drag {@code renderYawOffset} after it. 26.x renders off {@code yBodyRot},
     * and {@code BodyRotationControl} only pulls that toward the head while the mob is walking, so a boss standing
     * still and shooting would not face its target. Aiming here rather than in a goal also keeps the idle
     * look goals off the head: this runs after the goal selector, and the approach goal drops LOOK whenever it is
     * between paths, which is most of a fight.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        LivingEntity target = getTarget();
        if (target == null) return;

        getLookControl().setLookAt(target, 30F, 30F);
        setYRot(getYHeadRot());
        this.yBodyRot = getYRot();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        HbmCriteria.bossKilled(this, BossKilledTrigger.Kind.MASKMAN, 100D);
    }



    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        ItemStack mask = new ItemStack(ModGearItems.gas_mask_m65.get());
        if (mask.getItem() instanceof IGasMask gasMask) {
            gasMask.installFilter(mask, new ItemStack(ModGearItems.gas_mask_filter_combo.get()));
        }

        spawnAtLocation(level, mask);
        spawnAtLocation(level, new ItemStack(ModItems.coin_maskman.get()));
        spawnAtLocation(level, SPECIAL_DROP.get());
        spawnAtLocation(level, new ItemStack(Items.SKELETON_SKULL));
    }
}
