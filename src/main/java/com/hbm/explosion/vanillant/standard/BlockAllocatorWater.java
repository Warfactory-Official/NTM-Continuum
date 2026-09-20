package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockAllocatorWater}: the standard ray allocator, except that liquids neither attenuate
 * the ray nor get collected. Used by the naval mine so its 25-power blast carves the sea floor rather
 * than being spent on the water column above it.
 */
public class BlockAllocatorWater implements IBlockAllocator {

    protected int resolution;

    public BlockAllocatorWater(int resolution) {
        this.resolution = resolution;
    }

    @Override
    public ObjectOpenHashSet<BlockPos> allocate(ExplosionVNT explosion, Level world, double x, double y, double z, float size) {

        ObjectOpenHashSet<BlockPos> affectedBlocks = new ObjectOpenHashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int i = 0; i < this.resolution; ++i) {
            for (int j = 0; j < this.resolution; ++j) {
                for (int k = 0; k < this.resolution; ++k) {

                    if (i != 0 && i != this.resolution - 1 && j != 0 && j != this.resolution - 1
                            && k != 0 && k != this.resolution - 1) {
                        continue;
                    }

                    double d0 = (float) i / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                    double d1 = (float) j / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                    double d2 = (float) k / ((float) this.resolution - 1.0F) * 2.0F - 1.0F;
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

                    d0 /= d3;
                    d1 /= d3;
                    d2 /= d3;

                    float powerRemaining = size * (0.7F + world.getRandom().nextFloat() * 0.6F);
                    double currentX = x;
                    double currentY = y;
                    double currentZ = z;

                    for (float stepSize = 0.3F; powerRemaining > 0.0F; powerRemaining -= stepSize * 0.75F) {

                        cursor.set((int) Math.floor(currentX), (int) Math.floor(currentY), (int) Math.floor(currentZ));
                        BlockState state = world.getBlockState(cursor);
                        boolean liquid = !world.getFluidState(cursor).isEmpty() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock;

                        // "im braindead and copy code" - upstream; the one difference from the standard
                        // allocator is that a liquid is free to pass through and never collected.
                        if (!state.isAir() && !liquid) {
                            float blockResistance = state.getBlock().getExplosionResistance();
                            powerRemaining -= (blockResistance + 0.3F) * stepSize;
                        }

                        if (powerRemaining > 0.0F && !liquid) {
                            affectedBlocks.add(cursor.immutable());
                        }

                        currentX += d0 * stepSize;
                        currentY += d1 * stepSize;
                        currentZ += d2 * stepSize;
                    }
                }
            }
        }

        return affectedBlocks;
    }
}
