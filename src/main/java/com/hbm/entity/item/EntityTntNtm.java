package com.hbm.entity.item;

import dev.golem.core.data.CompatPrimedTnt;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import com.hbm.api.block.IFuckingExplode;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// 1.7: EntityTNTPrimedBase - only explode() dispatch differs
public class EntityTntNtm extends CompatPrimedTnt {

    /**
     * hbm-space {@code EntityTNTPrimedBase.doesSpin}: a primed charge tumbles, its pitch and yaw turning by a gaussian
     * 5 and 10 degrees a tick picked when it is made (ADDON-API-INVENTORY V-181; drawn by {@code RenderTNTPrimedBase}).
     * Off in plain HBM (1.7: no tumble, vanilla's renderer); NTM: Space turns it on.
     */
    public static boolean spin = false;

    private final boolean doesSpin;
    private static final EntityDataAccessor<Float> MOMENTUM_PITCH =
            SynchedEntityData.defineId(EntityTntNtm.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MOMENTUM_YAW =
            SynchedEntityData.defineId(EntityTntNtm.class, EntityDataSerializers.FLOAT);

    public EntityTntNtm(EntityType<? extends EntityTntNtm> type, Level level) {
        super(type, level);
        this.doesSpin = spin;
        // drawn only when spinning, so plain HBM's random sequence is untouched
        if (doesSpin && !level.isClientSide()) {
            entityData.set(MOMENTUM_PITCH, (float) (level.getRandom().nextGaussian() * 5F));
            entityData.set(MOMENTUM_YAW, (float) (level.getRandom().nextGaussian() * 10F));
        }
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(MOMENTUM_PITCH, 0F);
        data.define(MOMENTUM_YAW, 0F);
    }

    public float spinYaw(float partialTick) {
        return (tickCount - 1 + partialTick) * entityData.get(MOMENTUM_YAW);
    }

    public float spinPitch(float partialTick) {
        return (tickCount - 1 + partialTick) * entityData.get(MOMENTUM_PITCH);
    }

    @Override
    protected void saveData(DataOut out) {
        out.putFloat("spin_pitch", entityData.get(MOMENTUM_PITCH));
        out.putFloat("spin_yaw", entityData.get(MOMENTUM_YAW));
    }

    @Override
    protected void loadData(DataIn in) {
        entityData.set(MOMENTUM_PITCH, in.getFloatOr("spin_pitch", entityData.get(MOMENTUM_PITCH)));
        entityData.set(MOMENTUM_YAW, in.getFloatOr("spin_yaw", entityData.get(MOMENTUM_YAW)));
    }

    // 1.7: 5-arg ctor hardcodes EntityTypes.TNT; body reproduced
    // bomb required: DEFAULT_BLOCK_STATE is not IFuckingExplode
    public EntityTntNtm(EntityType<? extends EntityTntNtm> type, Level level, double x, double y, double z,
                        @Nullable LivingEntity igniter, BlockState bomb) {
        this(type, level);
        this.setPos(x, y, z);
        double rot = level.getRandom().nextDouble() * (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(rot) * 0.02, 0.2, -Math.cos(rot) * 0.02);
        this.setFuse(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        setOwnerCompat(igniter);
        this.setBlockState(bomb);
    }

    // vanilla's explode() is private on Fabric; Golem asks this at its head instead
    @Override
    protected boolean explodeCompat() {
        if (getBlockState().getBlock() instanceof IFuckingExplode bomb) {
            // 1.7 passes the raw posY, which under yOffset = height/2 is the box CENTRE, not vanilla's 0.0625 lift
            bomb.explodeEntity(level(), getX(), getY(0.5D), getZ(), this);
        }
        return true;
    }
}
