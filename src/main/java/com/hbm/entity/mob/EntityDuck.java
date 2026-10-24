package com.hbm.entity.mob;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.CompatChicken;
import com.hbm.sound.ModSounds;
import dev.golem.core.registry.Entities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class EntityDuck extends CompatChicken {

    public EntityDuck(EntityType<? extends EntityDuck> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_DUCC.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_DUCC.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_DUCC.get();
    }

    @Override
    public @Nullable Chicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return Entities.create(ModNtmEntities.DUCK.get(), level, EntitySpawnReason.BREEDING);
    }

    /** 1.7 overrode neither drop method, so it kept {@code EntityChicken.dropFewItems}. */

    @Override
    public void die(DamageSource source) {
        // 1.7 broadcast every duck death to the whole server.
        if (level() instanceof ServerLevel server) {
            server.getServer().getPlayerList().broadcastSystemMessage(getCombatTracker().getDeathMessage(), false);
        }

        super.die(source);
    }
}
