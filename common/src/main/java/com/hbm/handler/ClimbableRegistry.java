package com.hbm.handler;

import com.hbm.interfaces.IClimbable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClimbableRegistry {

    private ClimbableRegistry() {}

    private static final Set<IClimbable> ALL = ConcurrentHashMap.newKeySet();

    public static void register(IClimbable c) { ALL.add(c); }

    public static void unregister(IClimbable c) { ALL.remove(c); }

    public static boolean isEntityOnAny(Level level, LivingEntity entity) {
        if (ALL.isEmpty()) return false;
        for (IClimbable c : ALL) {
            if (c instanceof BlockEntity be && be.isRemoved()) continue;
            if (c.world() == level && c.isEntityInClimbAABB(entity)) return true;
        }
        return false;
    }
}
