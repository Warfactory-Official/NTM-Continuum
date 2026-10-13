package com.hbm.entity.missile;

import com.hbm.blocks.machine.MachineSatDock;
import com.hbm.client.ClientEffects;
import com.hbm.particle.helper.EntityEffects;
import dev.golem.core.data.Tags;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionLarge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

/**
 * The satellite-launch miner rocket: descends onto a satellite dock, mines for ~5 seconds, lifts back off.
 * Flight is a fixed vertical column - no guidance, no impact damage; delta-port keeps the 1.7 tick order
 * bit-for-bit (move first, then dock/crash check, then unload/FX).
 */
public class EntityMinerRocket extends CompatEntity {

    //0 landing, 1 unloading, 2 lifting - 1.7 dataWatcher ids 16/17
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(EntityMinerRocket.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SAT =
            SynchedEntityData.defineId(EntityMinerRocket.class, EntityDataSerializers.INT);

    public int timer = 0;

    public EntityMinerRocket(Level level) {
        this(ModNtmEntities.MINER_LANDER.get(), level);
    }

    public EntityMinerRocket(EntityType<? extends EntityMinerRocket> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(MODE, 0);
        builder.define(SAT, 0);
    }

    // 1.7: setSize(1F, 3F)
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(1F, 3F);
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        // 1.7: no attackEntityFrom override - base Entity ignores all damage
        return false;
    }

    public int getMode() {
        return entityData.get(MODE);
    }

    public void setMode(int mode) {
        entityData.set(MODE, mode);
    }

    public int getSatellite() {
        return entityData.get(SAT);
    }

    public void setSatellite(int sat) {
        entityData.set(SAT, sat);
    }

    @Override
    public void tick() {

        if(getMode() == 0)
            this.setDeltaMovement(0, -0.75, 0);
        if(getMode() == 1)
            this.setDeltaMovement(0, 0, 0);
        if(getMode() == 2)
            this.setDeltaMovement(0, 1, 0);

        Vec3 motion = this.getDeltaMovement();
        this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        BlockPos below = new BlockPos((int)(getX() - 0.5), (int)(getY() - 0.5), (int)(getZ() - 0.5));
        if(getMode() == 0 && isSatDock(below)) {
            setMode(1);
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(getX(), (int) getY(), getZ());
        } else {
            BlockPos above = new BlockPos((int)(getX() - 0.5), (int)(getY() + 1), (int)(getZ() - 0.5));
            if(!level().getBlockState(above).isAir() && !level().isClientSide() && getMode() != 1) {
                this.discard();
                ExplosionLarge.explodeFire(level(), getX() - 0.5, getY(), getZ() - 0.5, 10F, true, false, true);
            }
        }

        if(getMode() == 1) {
            if(!level().isClientSide() && this.tickCount % 4 == 0)
                ExplosionLarge.spawnShock(level(), getX(), getY(), getZ(),
                        1 + random.nextInt(3), 1 + random.nextGaussian());

            this.timer++;

            if(this.timer > 100) {
                setMode(2);
            }
        }

        if(getMode() != 1 && level() instanceof ServerLevel server && this.tickCount % 2 == 0) {
            EntityEffects.gasFlame(server, getX(), getY() - 0.5, getZ(), 0.0D, -1.0D, 0.0D, ClientEffects.GAS_FLAME_DEFAULT_SCALE, 150);
        }

        if(getMode() == 2 && getY() > 300)
            this.discard();
    }

    // 1.7 tests the CORE block alone; the 3x3's cells were dummy_plate_cargo and do not stop the descent
    private boolean isSatDock(BlockPos pos) {
        return MachineSatDock.isCore(level().getBlockState(pos));
    }

    @Override
    protected void loadData(DataIn input) {
        setMode(input.getIntOr("mode", 0));
        setSatellite(input.getIntOr("sat", 0));
        this.timer = input.getIntOr("timer", this.timer);
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("mode", getMode());
        output.putInt("sat", getSatellite());
        output.putInt("timer", this.timer);
    }
}
