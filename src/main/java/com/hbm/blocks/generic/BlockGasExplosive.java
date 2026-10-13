package com.hbm.blocks.generic;

import com.hbm.config.GeneralConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Explosive gas pocket, the 1.12.2 {@code BlockGasExplosive}. Identical drift and ignition triggers to
 * {@link BlockGasFlammable}; only {@link #combust} differs: instead of quietly turning into fire, the
 * whole connected pocket detonates in one pass.
 *
 * <p>The old flood fill is ported exactly: a BFS from the ignition point over same-block neighbours,
 * <b>capped at 128 cells</b>, each turned to fire and given a 3.0-power block-breaking explosion. The
 * {@code ThreadLocal} re-entry guard is kept, because the explosions destroy neighbouring gas cells, which
 * would otherwise re-enter {@code combust} through {@code neighborChanged} mid-fill.
 */
public class BlockGasExplosive extends BlockGasFlammable {

    private static final ThreadLocal<Boolean> COMBUSTING = ThreadLocal.withInitial(() -> false);
    private static final int MAX_CELLS = 128;

    public BlockGasExplosive(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean enabled() {
        return GeneralConfig.enableExplosiveGas;
    }

    @Override
    protected void combust(Level level, BlockPos start) {
        if (COMBUSTING.get() || !GeneralConfig.enableExplosiveGas) return;
        COMBUSTING.set(true);
        try {
            Queue<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            if (level.getBlockState(start).is(this)) {
                queue.offer(start.immutable());
                visited.add(start.immutable());
            }
            int processed = 0;
            while (!queue.isEmpty() && processed < MAX_CELLS) {
                BlockPos pos = queue.poll();
                processed++;
                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 2);
                level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 3.0F,
                        Level.ExplosionInteraction.BLOCK);
                for (Direction dir : Direction.values()) {
                    BlockPos neighbour = pos.relative(dir);
                    if (visited.contains(neighbour)) continue;
                    BlockState state = level.getBlockState(neighbour);
                    if (state.is(this)) {
                        visited.add(neighbour);
                        queue.offer(neighbour);
                    }
                }
            }
        } finally {
            COMBUSTING.set(false);
        }
    }
}
