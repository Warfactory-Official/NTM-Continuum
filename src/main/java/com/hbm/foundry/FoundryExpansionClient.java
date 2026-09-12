package com.hbm.foundry;

import com.hbm.client.render.ArcFurnaceRenderer;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.StrandCasterRenderer;

/** Client-side registration bridge kept with the foundry leaf instead of the central renderer table. */
public final class FoundryExpansionClient {

    private FoundryExpansionClient() {}

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, FoundryExpansion.STRAND_CASTER_BE.get(), StrandCasterRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, FoundryExpansion.ARC_FURNACE_BE.get(), ArcFurnaceRenderer::new);
    }
}
