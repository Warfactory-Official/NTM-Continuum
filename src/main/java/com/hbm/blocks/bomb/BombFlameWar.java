package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IBomb;
import com.hbm.particle.helper.ExplosionCreator;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Flame War in a Box ({@code flame_war}): 150 tiny block-less VNT bursts scattered in a 51x11x51 box (entity
 * damage only), then one strength-15 no-drop blast at the block with the small composed effect.
 */
public class BombFlameWar extends CompatBlock implements IBomb {

    public BombFlameWar(Properties properties) {
        super(properties);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block block, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) explode(level, pos, null);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!level.isClientSide()) {
            level.destroyBlock(pos, false);
            RandomSource rand = level.getRandom();

            for (int i = 0; i < 150; i++) {
                ExplosionVNT vnt = new ExplosionVNT(level, pos.getX() + rand.nextInt(51) - 25,
                        pos.getY() + rand.nextInt(11) - 5, pos.getZ() + rand.nextInt(51) - 25, 4, null);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 25));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectTiny());
                vnt.explode();
            }

            ExplosionVNT xnt = new ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15F);
            xnt.setBlockAllocator(new BlockAllocatorStandard(32));
            xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
            xnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, 200));
            xnt.setPlayerProcessor(new PlayerProcessorStandard());
            xnt.explode();
            ExplosionCreator.composeEffectSmall(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
        return BombReturnCode.DETONATED;
    }
}
