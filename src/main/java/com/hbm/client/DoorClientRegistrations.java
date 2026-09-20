package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.ModDoorBlockEntities;
import com.hbm.client.render.RenderBlastDoor;
import com.hbm.client.render.RenderDoorGeneric;

/** Client registration bridge for the door family: two block-entity renderers, no screens. */
public final class DoorClientRegistrations {
    private DoorClientRegistrations() {}

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        sink.register(ModDoorBlockEntities.DOOR_GENERIC.get(), context -> new RenderDoorGeneric());
        sink.register(ModDoorBlockEntities.BLAST_DOOR.get(), context -> new RenderBlastDoor());
    }
}
