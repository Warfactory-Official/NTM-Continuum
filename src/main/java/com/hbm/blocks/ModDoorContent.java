package com.hbm.blocks;

import com.hbm.sound.ModDoorSounds;
import com.hbm.tileentity.ModDoorBlockEntities;


/** One common bootstrap hook for the door family: blocks, block entities, sounds, declarations. */
public final class ModDoorContent {
    private ModDoorContent() {}

    public static void init() {
        ModDoorSounds.init();
        ModDoorBlocks.init();
        ModDoorBlockEntities.init();
        DoorDeclarations.declare();
    }
}
