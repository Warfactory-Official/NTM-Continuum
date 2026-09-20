package com.hbm.blocks.bomb;

import com.hbm.explosion.ExplosionThermo;
import com.hbm.interfaces.IBomb;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The thermobaric pair ({@code therm_endo} / {@code therm_exo}). No tile: redstone or a detonator removes
 * the block, runs the freeze or scorch sweep (radius 15 -> 30-block sphere) plus the entity pass (20), and
 * finishes with a vanilla strength-5 fire explosion.
 */
public class BombThermo extends CompatBlock implements IBomb {

    private final boolean endothermic;

    public BombThermo(Properties properties, boolean endothermic) {
        super(properties);
        this.endothermic = endothermic;
    }

    public boolean isEndothermic() {
        return endothermic;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block block, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) explode(level, pos, null);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        // 1.7 ran the sweep on both sides; the block writes are the server's alone here.
        if (level.isClientSide()) return BombReturnCode.DETONATED;
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        if (endothermic) {
            ExplosionThermo.freeze(level, pos, 15);
            ExplosionThermo.freezer(level, pos, 20);
        } else {
            ExplosionThermo.scorch(level, pos, 15);
            ExplosionThermo.setEntitiesOnFire(level, pos.getX(), pos.getY(), pos.getZ(), 20);
        }
        level.explode(null, pos.getX(), pos.getY(), pos.getZ(), 5.0F, true, Level.ExplosionInteraction.TNT);
        return BombReturnCode.DETONATED;
    }
}
