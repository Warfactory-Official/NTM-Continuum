package com.hbm.blocks.bomb;

import com.hbm.blocks.ModBlocks;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code CrystalPulsar} ({@code crystal_pulsar}, "Pulsating Crystal"): on any neighbour change it
 * hardens every dark crystal within a radius-10 sphere ({@code ExplosionChaos.hardenVirus}). The old
 * neighbour test was OR-ed with {@code !world.isRemote}, so on the server it always fired; ported as is.
 */
public class CrystalPulsar extends CompatBlock {

    public CrystalPulsar(Properties properties) {
        super(properties);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (level.isClientSide()) return;
        hardenVirus(level, pos, 10);
    }

    /** {@code ExplosionChaos.hardenVirus}: every virus block within {@code r²/2} of the centre hardens. */
    public static void hardenVirus(Level world, BlockPos centre, int r) {
        int r2 = r * r;
        int r22 = r2 / 2;
        BlockState hardened = ModBlocks.crystal_hardened.get().defaultBlockState();
        for (int xx = -r; xx < r; xx++) {
            int xx2 = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int yy2 = xx2 + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    if (yy2 + zz * zz < r22) {
                        BlockPos at = centre.offset(xx, yy, zz);
                        if (world.getBlockState(at).getBlock() instanceof CrystalVirus) world.setBlockAndUpdate(at, hardened);
                    }
                }
            }
        }
    }
}
