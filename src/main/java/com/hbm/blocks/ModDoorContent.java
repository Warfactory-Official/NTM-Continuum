package com.hbm.blocks;

import com.hbm.sound.ModDoorSounds;
import com.hbm.tileentity.ModDoorBlockEntities;


/** One common bootstrap hook for the door family: blocks (with their declarations), block entities, sounds. */
public final class ModDoorContent {
    private ModDoorContent() {}

    public static void init() {
        ModDoorSounds.init();
        ModDoorBlocks.init();
        ModDoorBlockEntities.init();
    }
}
