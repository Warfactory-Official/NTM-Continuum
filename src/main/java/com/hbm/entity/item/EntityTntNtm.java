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

    public EntityTntNtm(EntityType<? extends EntityTntNtm> type, Level level) {
        super(type, level);
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
