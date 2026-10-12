package com.hbm.entity.logic;

import dev.golem.core.data.Tags;

import com.hbm.config.GeneralConfig;
import com.hbm.explosion.ExplosionBalefire;
import com.hbm.explosion.ExplosionNukeGeneric;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Ticking driver for the balefire blast: owns an ExplosionBalefire spiral and steps it speed columns per tick,
 * keeping its chunk force-loaded.
 */
public class EntityBalefire extends EntityExplosionChunkloading {

    public int age = 0;
    public int destructionRange = 0;
    public ExplosionBalefire exp;
    public int speed = 1;
    public boolean did = false;
    /**
     * The crater variant ({@link ExplosionBalefire#registerVariant}); null is 1.7's balefire. Saved by name
     * ({@code variant}). NTM: Space's antimatter balefire is {@code ntmspace:antimatter}.
     */
    public @Nullable Identifier variant;

    /** A listener on every balefire tick (ADDON-API-INVENTORY H-66). */
    @FunctionalInterface
    public interface Tick {
        /**
         * Called on the server once per tick after the spiral has advanced and before 1.7's blast damage;
         * {@code finished} is true on the tick the crater completed (the entity is then already removed).
         */
        void tick(EntityBalefire balefire, boolean finished);
    }

    /** Every listener runs on each server tick of every balefire. HBM registers none (1.7 behaviour). */
    public static final dev.golem.core.hook.Hook<Tick> TICK = dev.golem.core.hook.Hook.notifying();

    public EntityBalefire(EntityType<? extends EntityBalefire> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();

        // 1.7: loadChunk(own chunk) only; ensureForced re-applies the NBT set after a reload
        if (!level().isClientSide()) {
            forceArea(0);
            ensureForced();
        }

        if (!this.did) {
            if (!level().isClientSide() && GeneralConfig.enableExtendedLogging)
                com.hbm.Constants.LOG.info("[NUKE] Initialized BF explosion at {} / {} / {} with strength {}!", getX(),
                        getY(), getZ(), destructionRange);

            exp = new ExplosionBalefire((int) getX(), (int) getY(), (int) getZ(), level(), this.destructionRange);
            exp.variant = ExplosionBalefire.variant(this.variant);

            this.did = true;
        }

        speed += 1; // increase speed to keep up with expansion

        boolean flag = false;
        for (int i = 0; i < this.speed; i++) {
            flag = exp.update();

            if (flag) {
                clearChunkLoader();
                this.discard();
            }
        }

        if (!level().isClientSide()) {
            for (Tick listener : TICK) listener.tick(this, flag);
        }

        // 1.7 does not break out on finish; damage is skipped on the finishing tick only
        if (!flag) {
            ExplosionNukeGeneric.dealDamage(level(), getX(), getY(), getZ(), this.destructionRange * 2);
        }

        age++;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        age = input.getIntOr("age", 0);
        destructionRange = input.getIntOr("destructionRange", 0);
        speed = input.getIntOr("speed", 1);
        did = input.getBooleanOr("did", false);
        String variantName = input.getStringOr("variant", "");
        variant = variantName.isEmpty() ? null : parseVariant(variantName);

        exp = new ExplosionBalefire((int) getX(), (int) getY(), (int) getZ(), level(), this.destructionRange);
        exp.variant = ExplosionBalefire.variant(this.variant);
        exp.readFromNbt(input, "exp_");

        this.did = true;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("age", age);
        output.putInt("destructionRange", destructionRange);
        output.putInt("speed", speed);
        output.putBoolean("did", did);
        if (variant != null) output.putString("variant", variant.toString());

        if (exp != null) exp.saveToNbt(output, "exp_");
    }

    private static @Nullable Identifier parseVariant(String name) {
        try {
            return dev.golem.core.Ids.parse(name);
        } catch (RuntimeException e) {
            com.hbm.Constants.LOG.warn("Balefire with an unreadable variant '{}' runs as plain balefire", name);
            return null;
        }
    }
}
