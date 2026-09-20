package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.tileentity.ModDecoBlockEntities;
import com.hbm.client.render.DecoModelRenderer;
import com.hbm.client.render.DemonLampRenderer;
import com.hbm.client.render.FloodlightRenderer;

/** Client-side hooks of the decorative block family. */
public final class DecoClientRegistrations {
    private DecoClientRegistrations() {}

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModDecoBlockEntities.DECO_MODEL.get(), DecoModelRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, ModDecoBlockEntities.FLOODLIGHT.get(), FloodlightRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, ModDecoBlockEntities.DEMON_LAMP.get(), DemonLampRenderer::new);
    }
}
