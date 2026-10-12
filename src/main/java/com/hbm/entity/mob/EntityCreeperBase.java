package com.hbm.entity.mob;

import dev.golem.core.data.CompatCreeper;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Set {@code maxSwell} in the subclass constructor for a custom fuse. */
public abstract class EntityCreeperBase extends CompatCreeper {

    protected EntityCreeperBase(EntityType<? extends EntityCreeperBase> type, Level level) {
        super(type, level);
    }

    protected abstract void detonate(ServerLevel level);

    /**
     * 1.7 {@code func_146077_cc} called {@code setDead()} before the blast, and a dead entity refused every
     * {@code attackEntityFrom}. {@link #tick} sets {@code dead} for the blast's duration, so the creeper's own
     * explosion damage (it stands inside its kill radius) must be refused the same way: letting it through
     * kills the creeper inside the blast, which NeoForge's {@code LivingDamageEvent.Pre} check rejects with an
     * IllegalArgumentException that crashes the server.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (dead) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        // 1.7 EntityCreeper.onDeath tested EntitySkeleton, which was also the wither skeleton; AbstractSkeleton
        // additionally admits Stray and Bogged
        if (!(source.getEntity() instanceof AbstractSkeleton)) return;

        dev.golem.core.world.Compat.randomItemOf(ItemTags.CREEPER_DROP_MUSIC_DISCS, random)
             .ifPresent(disc -> spawnAtLocation(level, new ItemStack(disc)));
    }

    @Override
    public void tick() {

        if (isAlive() && level() instanceof ServerLevel server) {
            int dir = isIgnited() ? 1 : getSwellDir();

            if (dir > 0 && swell() + dir >= maxSwell()) {
                setSwell(maxSwell());
                dead = true;
                detonate(server);
                triggerOnDeathMobEffects(server, RemovalReason.KILLED);
                discard();
                return;
            }
        }

        super.tick();
    }
}
