package com.hbm.foundry;

import com.hbm.client.render.ArcFurnaceRenderer;
import com.hbm.client.render.StrandCasterRenderer;
import com.hbm.client.render.HbmBlockEntityRenderers;

/** Client-side registration bridge kept with the foundry leaf instead of the central renderer table. */
public final class FoundryExpansionClient {

    private FoundryExpansionClient() {}

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        sink.register(FoundryExpansion.STRAND_CASTER_BE.get(), context -> new StrandCasterRenderer());
        sink.register(FoundryExpansion.ARC_FURNACE_BE.get(), context -> new ArcFurnaceRenderer());
    }
}
