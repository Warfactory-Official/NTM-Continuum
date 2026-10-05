package com.hbm.compat.sable;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric's entry to HBM's Sable integration ({@link HbmSableIntegration}): a {@code main} entrypoint the build appends to
 * {@code fabric.mod.json} only on nodes that declare Sable.
 */
public final class HbmSableFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("sable")) HbmSableIntegration.setup();
    }
}
