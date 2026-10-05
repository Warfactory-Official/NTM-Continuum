package com.hbm.compat.happyghast.client;

import com.hbm.client.render.HbmEntityRenderers;
import com.hbm.compat.happyghast.HbmHappyGhast;

/**
 * The happy-ghast Fat Man's client half: its renderer joins HBM's own entity renderers ({@link HbmEntityRenderers}),
 * so it is bound through the same sink and counted by the coverage audit. The loader roots call {@link #init()} on
 * the client before HBM registers renderers.
 */
public final class HappyGhastClient {

    private HappyGhastClient() {}

    private static boolean initialised;

    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        HbmEntityRenderers.addExtension(sink -> sink.register(HbmHappyGhast.RIGGED_FAT_MAN.get(), RenderRiggedFatMan::new));
    }
}
