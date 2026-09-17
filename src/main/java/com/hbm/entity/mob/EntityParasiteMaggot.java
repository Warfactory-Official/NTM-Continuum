package com.hbm.entity.mob;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.registry.LivingAttributes;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.registry.Entities;
import dev.golem.core.data.CompatMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EntityParasiteMaggot extends CompatMonster {

    public static final RegistryObject<EntityType<EntityParasiteMaggot>> TYPE =
            ModNtmEntities.ENTITY_TYPES.register("entity_parasite_maggot", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityParasiteMaggot>of(EntityParasiteMaggot::new, MobCategory.MONSTER)).sized(0.3F, 0.7F).clientTrackingRange(5).updateInterval(3), key));

    static {
        LivingAttributes.register(TYPE, EntityParasiteMaggot::createAttributes);
    }

    public EntityParasiteMaggot(EntityType<? extends EntityParasiteMaggot> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                      .add(Attributes.MAX_HEALTH, 8.0D)
                      .add(Attributes.MOVEMENT_SPEED, 1.0D)
                      .add(Attributes.ATTACK_DAMAGE, 2.0D)
                      .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.3F, 0.7F);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // 1.7 findPlayerToAttack -> getClosestVulnerablePlayerToEntity, which never checked sight
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // 1.7 getCreatureAttribute() == ARTHROPOD: data/minecraft/tags/entity_type/arthropod.json

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void tick() {
        super.tick();
        yBodyRot = getYRot();
        yBodyRotO = getYRot();
    }
}
