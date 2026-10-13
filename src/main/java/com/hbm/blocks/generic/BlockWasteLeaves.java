package com.hbm.blocks.generic;

import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dead leaves, from 1.7 {@code WasteLeaves}. A random tick has a 1 in 30 chance to remove the
 * block; when the space below is air the leaves come down as a falling single {@code leaves_layer} that
 * places itself where it lands and never drops as an item (1.7 {@code field_145813_c = false}). One client
 * display tick in seven sheds a {@code deadleaf} particle from the underside when it is open.
 *
 * <p>It drops nothing ({@code getItemDropped} null) and 1.7 {@code canSilkHarvest()} is false, so the loot
 * table stays empty. The registration asks for random ticks.
 */
public class BlockWasteLeaves extends CompatBlock {

    public BlockWasteLeaves(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(30) != 0) return;
        if (level.getBlockState(pos.below()).isAir()) {
            // fall() replaces the leaves with air itself, as 1.7 setBlockToAir did before spawning the entity
            FallingBlockEntity leaves = FallingBlockEntity.fall(level, pos,
                    ModTerrainBlocks.leaves_layer.get().defaultBlockState());
            leaves.dropItem = false;
        } else {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(7) == 0 && level.getBlockState(pos.below()).isAir()) {
            HbmEffectNT.DeadLeaf.summonParticle(level, pos.getX() + random.nextDouble(), pos.getY() - 0.05D,
                    pos.getZ() + random.nextDouble(), null);
        }
    }
}
