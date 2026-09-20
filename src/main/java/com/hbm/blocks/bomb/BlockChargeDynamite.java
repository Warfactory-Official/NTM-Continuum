package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.particle.helper.ExplosionSmallCreator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockChargeDynamite}: time bomb. Upstream fires {@code ExplosionNT(4F)} with no
 * attributes, which is the vanilla 16-ray algorithm with drops; {@code ExplosionNT} is not ported, so
 * the equivalent VNT (standard allocator/processor, standard entity damage) stands in.
 */
public class BlockChargeDynamite extends BlockChargeBase {

    public BlockChargeDynamite(Properties properties) {
        super(properties);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        removeSafely(level, pos);
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();

        ExplosionVNT exp = new ExplosionVNT(level, x + 0.5, y + 0.5, z + 0.5, 4F);
        exp.setBlockAllocator(new BlockAllocatorStandard());
        exp.setBlockProcessor(new BlockProcessorStandard());
        exp.setEntityProcessor(new EntityProcessorStandard());
        exp.setPlayerProcessor(new PlayerProcessorStandard());
        exp.explode();
        ExplosionSmallCreator.composeEffect(level, x + 0.5, y + 0.5, z + 0.5, 15, 3F, 1.25F);
        return BombReturnCode.DETONATED;
    }
}
