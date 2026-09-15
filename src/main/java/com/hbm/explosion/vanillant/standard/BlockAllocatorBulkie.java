package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class BlockAllocatorBulkie implements IBlockAllocator {
    private final double maximum;
    private final int resolution;

    public BlockAllocatorBulkie(double maximum) { this(maximum, 16); }
    public BlockAllocatorBulkie(double maximum, int resolution) {
        this.maximum = maximum;
        this.resolution = resolution;
    }

    @Override
    public ObjectOpenHashSet<BlockPos> allocate(ExplosionVNT explosion, Level level,
            double x, double y, double z, float size) {
        ObjectOpenHashSet<BlockPos> affected = new ObjectOpenHashSet<>();
        for (int i = 0; i < resolution; i++) for (int j = 0; j < resolution; j++) for (int k = 0; k < resolution; k++) {
            if (i != 0 && i != resolution - 1 && j != 0 && j != resolution - 1
                    && k != 0 && k != resolution - 1) continue;
            double dx = (double) i / (resolution - 1) * 2D - 1D;
            double dy = (double) j / (resolution - 1) * 2D - 1D;
            double dz = (double) k / (resolution - 1) * 2D - 1D;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= length;
            dy /= length;
            dz /= length;
            double currentX = x;
            double currentY = y;
            double currentZ = z;
            double distance = 0D;
            for (float step = 0.3F; distance <= explosion.size; ) {
                distance = Math.sqrt((currentX - x) * (currentX - x) + (currentY - y) * (currentY - y)
                        + (currentZ - z) * (currentZ - z));
                BlockPos pos = new BlockPos(Mth.floor(currentX), Mth.floor(currentY), Mth.floor(currentZ));
                var state = level.getBlockState(pos);
                if (!state.isAir() && maximum < explosion.blockResistance(level, pos, state)) break;
                if (explosion.blockExplodes(level, pos, state, explosion.size)) affected.add(pos);
                currentX += dx * step;
                currentY += dy * step;
                currentZ += dz * step;
            }
        }
        return affected;
    }
}
