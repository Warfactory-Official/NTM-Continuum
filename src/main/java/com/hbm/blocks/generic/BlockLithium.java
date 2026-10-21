package com.hbm.blocks.generic;

import dev.golem.core.world.Explosions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockLithium} ({@code block_lithium}): a beaconable metal block that detonates (strength 15,
 * no fire, smoking) the moment water touches any of its six faces, on placement or on a neighbour
 * change, destroying itself without drops first. In the rain it smokes.
 */
public class BlockLithium extends BlockBeaconable {

    public BlockLithium(Properties properties) {
        super(properties);
    }

    private static boolean touchesWater(Level level, BlockPos pos) {
        if (level.isClientSide()) return false;
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.WATER)) return true;
        }
        return false;
    }

    private void react(Level level, BlockPos pos) {
        level.destroyBlock(pos, false);
        Explosions.explode(level, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15F, false, Level.ExplosionInteraction.BLOCK);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (touchesWater(level, pos)) react(level, pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (touchesWater(level, pos)) react(level, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (level.isRainingAt(pos.above())) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + rand.nextFloat(), pos.getY() + 1, pos.getZ() + rand.nextFloat(), 0, 0, 0);
        }
    }
}
