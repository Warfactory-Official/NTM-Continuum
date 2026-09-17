package com.hbm.entity.logic;

import com.hbm.config.GeneralConfig;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionTom;
import com.hbm.saveddata.TomSaveData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

public class EntityTomBlast extends EntityExplosionChunkloading {

    public int age = 0;
    public int destructionRange = 0;
    public ExplosionTom exp;
    public int speed = 1;
    public boolean did = false;

    public EntityTomBlast(EntityType<? extends EntityTomBlast> type, Level level) {
        super(type, level);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        age = input.getIntOr("age", 0);
        destructionRange = input.getIntOr("destructionRange", 0);
        speed = input.getIntOr("speed", 0);
        did = input.getBooleanOr("did", false);

        exp = new ExplosionTom((int) this.getX(), (int) this.getY(), (int) this.getZ(), level(),
                this.destructionRange);
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

    @Override
    public void tick() {
        super.tick();

        // 1.7: own chunk only; ensureForced re-applies NBT set
        if (!level().isClientSide()) {
            forceArea(0);
            ensureForced();
        }

        if (!this.did) {

            if (!level().isClientSide() && GeneralConfig.enableExtendedLogging)
                com.hbm.Constants.LOG.info("[NUKE] Initialized TOM explosion at {} / {} / {} with strength {}!",
                        getX(), getY(), getZ(), destructionRange);

            exp = new ExplosionTom((int) this.getX(), (int) this.getY(), (int) this.getZ(), level(),
                    this.destructionRange);

            this.did = true;
        }

        speed += 1; // increase speed to keep up with expansion

        boolean flag = false;

        // 26.2: 1.7 drove the explosion on both sides - TomSaveData.forWorld took either. TomSaveData.get is
        // ServerLevel-only, and ExplosionTom writes blocks, so the tracked client copy must not drive it.
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < this.speed; i++) {
                flag = exp.update();

                if (flag) {
                    this.clearChunkLoader();
                    this.discard();
                    TomSaveData data = TomSaveData.get(server);
                    data.impact = true;
                    data.fire = 1F;
                    data.setDirty();
                }
            }
        }

        if (!level().isClientSide()) {

            if (this.random.nextInt(5) == 0)
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                        10000.0F, 0.8F + this.random.nextFloat() * 0.2F);

            if (!flag) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.WEATHER, 10000.0F, 0.8F + this.random.nextFloat() * 0.2F);
                ExplosionNukeGeneric.dealDamage(level(), getX(), getY(), getZ(), this.destructionRange * 2);
            }
        }

        age++;
    }
}
