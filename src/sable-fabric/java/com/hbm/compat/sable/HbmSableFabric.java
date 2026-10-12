package com.hbm.compat.sable;

import dev.golem.core.platform.ModEntry;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric's entry to HBM's Sable integration ({@link HbmSableIntegration}): a {@code golemcore:entry} the build appends
 * after HBM's {@code FabricInit} in {@code fabric.mod.json} only on nodes that declare Sable.
 */
public final class HbmSableFabric implements ModEntry {

    @Override
    public void declare() {
        if (FabricLoader.getInstance().isModLoaded("sable")) HbmSableIntegration.setup();
    }
}
