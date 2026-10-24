package com.hbm.entity.mob;

import dev.golem.core.data.CompatCreeper;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
