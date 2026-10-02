package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla's own ray allocator, with the ray count made a parameter.
 *
 * <p>Vanilla fixes the resolution at 16 rays per cube edge; missiles ask for 24, 32 or 48 so that a
 * 50-power blast still carves a smooth sphere instead of a starburst. Everything else — the 0.3 step,
 * the {@code 0.7 + rand·0.6} power roll, the {@code (resistance + 0.3) · step} attenuation — is
 * vanilla's, and is what makes a VNT explosion eat exactly as much stone as a TNT of the same size.
 */
public class BlockAllocatorStandard implements IBlockAllocator {

    protected int resolution;

    /**
     * 1.7 {@code ExplosionNT} {@code ExAttrib.ERRODE}: blocks in this table are affected even when the ray's power
     * ran out inside them, so a blast too weak to break concrete still gets to erode it.
     */
    protected @Nullable Map<Block, Block> erosion;

    public BlockAllocatorStandard() {
        this(16);
    }

    public BlockAllocatorStandard(int resolution) {
        this.resolution = resolution;
    }

    public BlockAllocatorStandard withErosion(Map<Block, Block> erosion) {
        this.erosion = erosion;
        return this;
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
                        continue; // only the shell of the cube produces rays
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

                        if (!state.isAir()) {
                            // Vanilla's default damage calculator: the greater of the block's and the
                            // fluid's resistance. The entity-based variant is not reproduced — no HBM
                            // exploder overrides it.
                            float blockResistance = Math.max(state.getBlock().getExplosionResistance(),
                                    world.getFluidState(cursor).getExplosionResistance());
                            powerRemaining -= (blockResistance + 0.3F) * stepSize;
                        }

                        if (powerRemaining > 0.0F) {
                            affectedBlocks.add(cursor.immutable());
                        } else if (erosion != null && erosion.containsKey(state.getBlock())) {
                            // 1.7 ExplosionNT.doExplosionA: "else if(has(ERRODE) && errosion.containsKey(block))"
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
