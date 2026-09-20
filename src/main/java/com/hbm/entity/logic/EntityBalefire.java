package com.hbm.entity.logic;

import dev.golem.core.data.Tags;

import com.hbm.config.GeneralConfig;
import com.hbm.explosion.ExplosionBalefire;
import com.hbm.explosion.ExplosionNukeGeneric;
import net.minecraft.world.entity.EntityType;
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

        exp = new ExplosionBalefire((int) getX(), (int) getY(), (int) getZ(), level(), this.destructionRange);
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

        if (exp != null) exp.saveToNbt(output, "exp_");
    }
}
