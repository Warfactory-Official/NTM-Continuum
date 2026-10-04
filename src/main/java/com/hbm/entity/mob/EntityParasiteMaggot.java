package com.hbm.entity.mob;

import com.hbm.entity.ModNtmEntities;
import com.hbm.registration.Reg;
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
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EntityParasiteMaggot extends CompatMonster {

    private static final Reg REG = Reg.of(ModNtmEntities.ENTITY_TYPES);

    public static final RegistryObject<EntityType<EntityParasiteMaggot>> TYPE =
            REG.entity("entity_parasite_maggot", key -> Entities.build(
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
                      // 1.7 set 1.0, but the maggot ran the pre-goal ("old") AI, which walked moveForward = 1.0
                      // times a flat getAIMoveSpeed() of 0.1. Goal AI walks speed * speed, so the same pace is
                      // sqrt(0.1) (vanilla's silverfish went 1.0 to 0.25 in the same conversion).
                      .add(Attributes.MOVEMENT_SPEED, Math.sqrt(1.0D * 0.1D))
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
        goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D)); // 1.7 old-AI wander did not avoid water
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // 1.7 findPlayerToAttack -> getClosestVulnerablePlayerToEntity, which never checked sight
        // 1.7 asked again every tick while it had no target, hence the 0 interval
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, null));
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
