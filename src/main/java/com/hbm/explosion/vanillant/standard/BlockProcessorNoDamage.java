package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockProcessorNoDamage}: breaks nothing. The optional mutator still sees every allocated
 * non-air block ({@code mutatePre}) and every allocated air position ({@code mutatePost}), and the set is
 * cleared afterwards so the standard SFX draws no block-damage particles.
 */
public class BlockProcessorNoDamage implements IBlockProcessor {

    protected @Nullable IBlockMutator convert;

    public BlockProcessorNoDamage() {}

    public BlockProcessorNoDamage withBlockEffect(@Nullable IBlockMutator convert) {
        this.convert = convert;
        return this;
    }

    @Override
    public void process(ExplosionVNT explosion, Level world, double x, double y, double z, ObjectOpenHashSet<BlockPos> affectedBlocks) {
        if (this.convert != null) {
            for (BlockPos pos : affectedBlocks) {
                BlockState state = world.getBlockState(pos);
                if (!state.isAir()) this.convert.mutatePre(explosion, state, pos);
            }
            for (BlockPos pos : affectedBlocks) {
                if (world.getBlockState(pos).isAir()) this.convert.mutatePost(explosion, pos);
            }
        }
        affectedBlocks.clear(); // 1.7: tricks the standard SFX into skipping the block-damage particles
    }
}
