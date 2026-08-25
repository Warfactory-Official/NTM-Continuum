package com.hbm.explosion;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The area-effect half of a warhead — 1.12.2's {@code ExplosionChaos}. Only the two fire sweeps the
 * ported missiles use are here: {@link #flameDeath} (incendiary) and {@link #burn} (inferno).
 *
 * <p>Deferred with their subsystems (IMPLEMENTATION.md §20): {@code cluster} and {@code miniMirv} need
 * {@code EntityRocket}/{@code EntityMiniNuke}, and the gas sweeps need the shadow-particle family.
 * That is why the three cluster missiles are not registered yet.
 */
public final class ExplosionChaos {

    private ExplosionChaos() {}

    /**
     * Upstream's sphere walk, kept exactly — including the detail that it uses <b>half</b> the squared
     * radius as its bound, so a "radius 25" sweep really reaches about 17 blocks. That looks like a
     * bug and is load-bearing: the radii the missiles pass were tuned against it.
     */
    private static void forEachBlockInSphere(Level world, int x, int y, int z, int radius,
                                             Consumer<BlockPos.MutableBlockPos> action) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int radiusSqHalf = (radius * radius) / 2;

        for (int yy = -radius; yy < radius; yy++) {
            int currentY = y + yy;
            if (currentY < world.getMinY() || currentY > world.getMaxY()) continue;

            int YY = yy * yy;
            if (YY >= radiusSqHalf) continue;

            int xzRadius = (int) Math.sqrt(radiusSqHalf - YY);

            for (int xx = -xzRadius; xx <= xzRadius; xx++) {
                int XX = xx * xx;
                int YY_XX = YY + XX;
                if (YY_XX >= radiusSqHalf) continue;

                int zRadius = (int) Math.sqrt(radiusSqHalf - YY_XX);

                for (int zz = -zRadius; zz <= zRadius; zz++) {
                    action.accept(pos.set(x + xx, currentY, z + zz));
                }
            }
        }
    }

    /** Sets every <em>flammable</em> block in range alight. */
    public static void flameDeath(Level world, BlockPos centre, int bound) {
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

        forEachBlockInSphere(world, centre.getX(), centre.getY(), centre.getZ(), bound, pos -> {
            above.set(pos.getX(), pos.getY() + 1, pos.getZ());
            if (world.getBlockState(pos).ignitedByLava() && world.getBlockState(above).isAir()) {
                world.setBlockAndUpdate(above, Blocks.FIRE.defaultBlockState());
            }
        });
    }

    /** Sets <em>everything</em> in range alight — anything solid with air or snow above it. */
    public static void burn(Level world, BlockPos centre, int bound) {
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

        forEachBlockInSphere(world, centre.getX(), centre.getY(), centre.getZ(), bound, pos -> {
            above.set(pos.getX(), pos.getY() + 1, pos.getZ());
            BlockState upState = world.getBlockState(above);
            if ((upState.isAir() || upState.is(Blocks.SNOW)) && !world.getBlockState(pos).isAir()) {
                world.setBlockAndUpdate(above, Blocks.FIRE.defaultBlockState());
            }
        });
    }
}
