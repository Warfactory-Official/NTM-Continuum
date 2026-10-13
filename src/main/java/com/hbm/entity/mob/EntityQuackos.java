package com.hbm.entity.mob;

import com.hbm.items.ModEntityItems;
import com.hbm.entity.ModNtmEntities;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.sound.ModSounds;
import com.hbm.util.DropHeight;
import dev.golem.core.registry.Entities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * BOW
 */
public class EntityQuackos extends EntityDuck {


    public EntityQuackos(EntityType<? extends EntityQuackos> type, Level level) {
        super(type, level);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.3F * 25F, 0.7F * 25F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_MEGAQUACC.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENTITY_MEGAQUACC.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENTITY_MEGAQUACC.get();
    }

    @Override
    public @Nullable Chicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return Entities.create(ModNtmEntities.QUACKOS.get(), level, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        // 1.7 setDead() was a client-only no-op on the server; prank'd. Chunk unload still has to work,
        // so only the kill paths are ignored.
        if (level().isClientSide() || (reason != RemovalReason.KILLED && reason != RemovalReason.DISCARDED)) {
            super.remove(reason);
        }
    }



    @Override
    public void setHealth(float health) {
        super.setHealth(getMaxHealth());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = super.mobInteract(player, hand);
        if (result.consumesAction()) return result;

        if (!level().isClientSide() && (getFirstPassenger() == null || getFirstPassenger() == player)) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }

        return result;
    }

    /** Called by 1.7's ItemPeas: the only thing in the game that can actually get rid of one. */
    public void despawn() {
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < 150; i++) {
                FlameCreator.composeEffect(server,
                        getX() + random.nextDouble() * 20 - 10,
                        getY() + random.nextDouble() * 25,
                        getZ() + random.nextDouble() * 20 - 10,
                        FlameCreator.META_BALEFIRE);
            }
            spawnAtLocation(server, new ItemStack(ModEntityItems.spawn_duck.get(), 3));
        }

        super.remove(RemovalReason.DISCARDED);
    }

    @Override
    protected Double compatPassengerHeight() {
        return getBbHeight() - 0.125D;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!level().isClientSide() && getY() < -30D) {
            double respawnX = getX() + random.nextGaussian() * 30;
            double respawnZ = getZ() + random.nextGaussian() * 30;
            setPos(respawnX, DropHeight.clearOfTerrain(level(), 256D, respawnX, respawnZ), respawnZ);
        }
    }

    @Override
    public void die(DamageSource source) {
    }

    @Override
    protected boolean compatCanBeLeashed() {
        return false;
    }
}
