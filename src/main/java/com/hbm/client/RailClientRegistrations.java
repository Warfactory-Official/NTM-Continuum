package com.hbm.client;

import com.hbm.blocks.ModRailBlocks;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.RailRenderer;

/** Client-side binding for the folded rail family's source-faithful OBJ renderer. */
public final class RailClientRegistrations {

    private RailClientRegistrations() {}

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModRailBlocks.RAIL_ENTITY.get(), RailRenderer::new);
    }
}
