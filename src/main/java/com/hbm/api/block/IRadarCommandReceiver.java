package com.hbm.api.block;

import net.minecraft.world.entity.Entity;

/**
 * A block entity a radar or designator can hand a fire mission to. Ported from 1.12.2's
 * {@code tileentity.IRadarCommandReceiver}; nothing issues commands yet, because neither the radar nor
 * the artillery designator is ported — the artillery turrets implement it so that wiring is a
 * registration away rather than a refactor.
 */
public interface IRadarCommandReceiver {

    /** Queues a fixed map position. Returns whether the command was accepted. */
    boolean sendCommandPosition(int x, int y, int z);

    /** Queues wherever an entity currently is. Returns whether the command was accepted. */
    boolean sendCommandEntity(Entity target);
}
