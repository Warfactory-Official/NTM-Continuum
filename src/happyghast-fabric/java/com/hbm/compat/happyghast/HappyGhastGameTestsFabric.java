package com.hbm.compat.happyghast;

import dev.golem.core.platform.ModEntry;

/** Declares the happy-ghast GameTests on Fabric, before the registries fill; release jars drop the class and its entry. */
public final class HappyGhastGameTestsFabric implements ModEntry {

    @Override
    public void declare() {
        HappyGhastGameTests.init();
    }
}
