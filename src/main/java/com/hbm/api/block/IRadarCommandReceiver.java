package com.hbm.api.block;

import net.minecraft.world.entity.Entity;

/**
 * A block entity a radar or designator can hand a fire mission to. Ported from 1.12.2's
 * {@code tileentity.IRadarCommandReceiver}.
 * <p>The radar issues them: a radar linker in one of its eight link slots names the receiver, and a digit
 * typed over the radar map fires at the blip under the cursor or at the map position. Launch pads and the
 * artillery turrets implement it.
 */
public interface IRadarCommandReceiver {

    /** Queues a fixed map position. Returns whether the command was accepted. */
    boolean sendCommandPosition(int x, int y, int z);

    /** Queues wherever an entity currently is. Returns whether the command was accepted. */
    boolean sendCommandEntity(Entity target);
}
