package com.hbm.sound;

import com.hbm.util.TickPhase;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/** Public machine-loop vibrations, advanced independently of client audio settings and sound wrappers. */
public final class WorldSounds {
    private WorldSounds() {}

    /** A machine's public running noise at the centre of its core block. */
    public static void machineNoise(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            server.gameEvent(GameEvent.BLOCK_ACTIVATE, Vec3.atCenterOf(pos), GameEvent.Context.of((Entity) null));
        }
    }

    /** Start immediately, then once per second at the position's stable phase; return the next envelope state. */
    public static boolean machineLoop(Level level, BlockPos pos, boolean active, boolean wasActive) {
        if (active && (!wasActive || TickPhase.every(level, pos, SharedConstants.TICKS_PER_SECOND))) {
            machineNoise(level, pos);
        }
        return active;
    }
}
