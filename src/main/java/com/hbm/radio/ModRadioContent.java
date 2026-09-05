package com.hbm.radio;

/** One common bootstrap hook for all feature-local radio registries. */
public final class ModRadioContent {
    private ModRadioContent() {}

    public static void init() {
        ModRadioBlocks.init();
        ModRadioBlockEntities.init();
        ModRadioMenus.init();
    }
}
