package com.hbm.entity.item;

import dev.golem.core.data.CompatPrimedTnt;
import com.hbm.api.block.IFuckingExplode;
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
    private final float momentumPitch;
    private final float momentumYaw;

    public EntityTntNtm(EntityType<? extends EntityTntNtm> type, Level level) {
        super(type, level);
        this.doesSpin = spin;
        // drawn only when spinning, so plain HBM's random sequence is untouched
        this.momentumPitch = doesSpin ? (float) (level.getRandom().nextGaussian() * 5F) : 0F;
        this.momentumYaw = doesSpin ? (float) (level.getRandom().nextGaussian() * 10F) : 0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (doesSpin && !isRemoved()) {
            setXRot(getXRot() + momentumPitch);
            setYRot(getYRot() + momentumYaw);
        }
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
