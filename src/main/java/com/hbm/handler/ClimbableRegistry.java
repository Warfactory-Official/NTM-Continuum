package com.hbm.handler;

import com.hbm.interfaces.IClimbable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry of {@link IClimbable} objects (climbable AABBs). Queried from
 * {@link com.hbm.mixin.MixinLivingEntity} to decide whether an entity is on a ladder.
 *
 * <p>Slim port of the 1.12.2 chunk-indexed registry: a single concurrent set (the integrated server
 * ticks on a different thread from the client, and both register into this static set), iterated and
 * filtered by level. Fine for the handful of climbable tiles in play; the chunk index can return if it
 * ever matters for perf.
 */
public final class ClimbableRegistry {

    private ClimbableRegistry() {}

    private static final Set<IClimbable> ALL = ConcurrentHashMap.newKeySet();

    public static void register(IClimbable c) { ALL.add(c); }

    public static void unregister(IClimbable c) { ALL.remove(c); }

    /** Whether {@code entity} intersects any registered climbable in its level. */
    public static boolean isEntityOnAny(Level level, LivingEntity entity) {
        if (ALL.isEmpty()) return false;
        for (IClimbable c : ALL) {
            if (c instanceof BlockEntity be && be.isRemoved()) continue;
            if (c.world() == level && c.isEntityInClimbAABB(entity)) return true;
        }
        return false;
    }
}
