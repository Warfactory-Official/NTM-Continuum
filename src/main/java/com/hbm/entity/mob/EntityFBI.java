package com.hbm.entity.mob;

import com.hbm.api.environment.Environment;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModGearItems;
import com.hbm.items.ModArmorItems;
import com.hbm.config.MobConfig;
import dev.golem.core.world.Compat;
import dev.golem.core.data.CompatMonster;
import com.hbm.entity.mob.ai.EntityAIBreaking;
import com.hbm.lib.HbmBlockTags;
import dev.golem.core.world.Effects;
import dev.golem.core.world.Navigations;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import dev.golem.core.world.CompatMeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class EntityFBI extends CompatMonster implements RangedAttackMob {

    public EntityFBI(EntityType<? extends EntityFBI> type, Level level) {
        super(type, level);
        Navigations.setCanOpenDoors(getNavigation(), true); // 1.7 setBreakDoors
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                      .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                      .add(Attributes.MOVEMENT_SPEED, 0.3D)
                      // 1.7 hardcoded getTotalArmorValue() = 20, "combat vest = full diamond set"
                      .add(Attributes.ARMOR, 20D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new EntityAIBreaking(this));
        // 1.7's ranged goal outranks the melee one and shares its MOVE|LOOK mutex, so the melee goal can never
        // start while a target exists - and performRangedAttack is empty. The FBI closes and does nothing.
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 20, 25, 15.0F));
        goalSelector.addGoal(3, new CompatMeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        // 1.7 targetChance 0: re-acquire on every check, not one in ten
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, null));
    }


    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // 1.7: friendly fire from another FBI's projectile is ignored
        if (source.getDirectEntity() != null && source.getEntity() instanceof EntityFBI
                && source.getDirectEntity() != source.getEntity()) {
            return false;
        }

        // 1.7: a glass-helmeted FBI was immune to the "oxygenSuffocation" and "thermal" damage types,
        // matched by string. Neither is an NTM type - they are Galacticraft's (cf. 1.7 Compat.MOD_GCC), matched
        // that way so NTM needed no hard dependency. The port carries no Galacticraft integration, so minting
        // hbm: types for them would be inventing content, not porting it.
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false; // 1.7 canDespawn() = false
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        equipRaidLoadout();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    public void equipRaidLoadout() {
        setItemSlot(EquipmentSlot.MAINHAND, random.nextBoolean()
                ? ModGunItems.gun_heavy_revolver.get().getDefaultInstance()
                : ModGunItems.gun_spas12.get().getDefaultInstance());

        if (random.nextInt(5) == 0) {
            setItemSlot(EquipmentSlot.HEAD, ModArmorItems.security_helmet.get().getDefaultInstance());
            setItemSlot(EquipmentSlot.CHEST, ModArmorItems.security_plate.get().getDefaultInstance());
            setItemSlot(EquipmentSlot.LEGS, ModArmorItems.security_legs.get().getDefaultInstance());
            setItemSlot(EquipmentSlot.FEET, ModArmorItems.security_boots.get().getDefaultInstance());
        }

        // 1.7 swapped to the vacuum loadout outside the overworld, glass helmet included
        // 1.7: the space suit outside the overworld; hbm-space: wherever the agent cannot breathe. An addon answers
        // com.hbm.api.environment.Environment.BREATHING; unanswered, 1.7's rule decides.
        if (!Environment.breatheAir(level(), blockPosition(), 0, level().dimension() == Level.OVERWORLD)) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GLASS));
            setItemSlot(EquipmentSlot.CHEST, ModArmorItems.paa_plate.get().getDefaultInstance());
            setItemSlot(EquipmentSlot.LEGS, ModArmorItems.paa_legs.get().getDefaultInstance());
            setItemSlot(EquipmentSlot.FEET, ModArmorItems.paa_boots.get().getDefaultInstance());
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        // 1.7 attackEntityWithRangedAttack: empty
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            setItemSlot(EquipmentSlot.HEAD, ModGearItems.gas_mask_m65.get().getDefaultInstance());
        }
        return false; // 1.7: isPotionApplicable always false
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // 1.7 updateAITasks: no goal sees past followRange, so a targetless FBI reacquires the nearest player
        // within 128 blocks and walks a partial path 16 blocks along the bearing, recomputed every tick
        LivingEntity target = getTarget();

        if (target == null) {
            target = level.getNearestPlayer(getX(), getY(), getZ(), 128.0D,
                    entity -> !((Player) entity).getAbilities().invulnerable);
            if (target != null) setTarget(target);
        }

        if (target != null) {
            Vec3 bearing = new Vec3(target.getX() - getX(), target.getY() - getY(), target.getZ() - getZ())
                    .normalize().scale(16.0D);
            int x = Mth.floor(getX() + bearing.x);
            int y = Mth.floor(getY() + bearing.y);
            int z = Mth.floor(getZ() + bearing.z);

            // 1.7 PathFinderUtils ground-snap: an unreachable endpoint slows the walk to a crawl
            y = snapToGround(level, x, y, z, true);
            if (y == Integer.MIN_VALUE) y = snapToGround(level, x, Mth.floor(getY() + bearing.y), z, false);
            if (y != Integer.MIN_VALUE) Navigations.navigateTo(getNavigation(), x + 0.5D, y, z + 0.5D, 1.0D);
        }
    }

    /** First spot, scanning 10 blocks down (or up), that is passable over solid ground; MIN_VALUE if none. */
    private static int snapToGround(ServerLevel level, int x, int y, int z, boolean down) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int start = down ? y : y + 10;
        int end = down ? y - 10 : y;

        for (int i = start; i > end; i--) {
            pos.set(x, i, z);
            if (!Compat.isSolidRender(level.getBlockState(pos)) && Compat.isSolidRender(level.getBlockState(pos.move(0, -1, 0)))) {
                return i;
            }
        }

        return Integer.MIN_VALUE;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide() || getHealth() <= 0) return;

        if (tickCount % MobConfig.raidAttackDelay == 0) {
            breakNearbyMachine();
        }

        // 1.7 expanded a zero-size box at the feet, not the hitbox
        double range = 1.5;
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class,
                new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(range, range, range));

        for (ItemEntity item : items) Effects.ignite(item, 10);
    }

    private void breakNearbyMachine() {
        Vec3 reach = new Vec3(MobConfig.raidAttackReach, 0, 0).yRot((float) (Math.PI * 2) * random.nextFloat());
        Vec3 from = new Vec3(getX(), getY() + 0.5 + random.nextFloat(), getZ());
        Vec3 to = from.add(reach);

        BlockHitResult hit = level().clip(
                new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (hit.getType() != HitResult.Type.BLOCK || !(level() instanceof ServerLevel server)) return;
        BlockState state = server.getBlockState(hit.getBlockPos());
        if (state.is(HbmBlockTags.FBI_BREAKABLE)
                && Compat.canEntityDestroyBlock(server, hit.getBlockPos(), state, this)) {
            level().destroyBlock(hit.getBlockPos(), false);
        }
    }
}
