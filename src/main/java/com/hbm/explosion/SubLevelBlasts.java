package com.hbm.explosion;

import dev.golem.core.world.SubLevels;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * port: blasts on moving sub-levels (Create Aeronautics). A sphere looks the same from any frame, so a blast reaches a
 * sub-level by running again around its centre as that sub-level sees it.
 */
public final class SubLevelBlasts {

    /** Iron-block weights a blast pushes a sub-level with, per block it breaks there, for one tick. */
    static final double PUSH_PER_BLOCK = 2D;

    private SubLevelBlasts() {}

    /** Pushes a sub-level away from grid point {@code centre} for each of its {@code broken} blocks. */
    public static void push(Level level, Vec3 centre, Collection<BlockPos> broken) {
        Vec3 from = SubLevels.toWorld(level, centre);
        for (BlockPos pos : broken) {
            Vec3 at = Vec3.atCenterOf(pos);
            Vec3 away = SubLevels.toWorld(level, at).subtract(from);
            if (away.lengthSqr() < 1.0E-6D) continue;
            SubLevels.push(level, at, away.normalize().scale(PUSH_PER_BLOCK));
        }
    }

    /** A nuclear crater on sub-levels: every breakable block within {@code radius} of world point {@code centre}. */
    public static void crater(Level level, Vec3 centre, double radius) {
        if (level.isClientSide()) return;
        double radiusSq = radius * radius;
        for (SubLevels.Local local : SubLevels.intoSubLevels(level, centre, radius)) {
            Vec3 point = local.point();
            AABB sphere = new AABB(point, point).inflate(radius);
            if (!local.blocks().intersects(sphere)) continue;
            AABB reach = local.blocks().intersect(sphere);
            List<BlockPos> broken = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(reach.minX, reach.minY, reach.minZ),
                    BlockPos.containing(reach.maxX, reach.maxY, reach.maxZ))) {
                if (Vec3.atCenterOf(pos).distanceToSqr(point) > radiusSq) continue;
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(level, pos) < 0F) continue;
                broken.add(pos.immutable());
            }
            for (BlockPos pos : broken) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            push(level, point, broken);
        }
    }
}
