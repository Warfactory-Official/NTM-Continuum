package com.hbm.gametest;

import dev.golem.core.platform.ModEntry;

/** Fabric's {@code golemcore:entry} for the GameTest suites; release jars drop the class and its line in fabric.mod.json. */
public final class GameTestEntry implements ModEntry {

    @Override
    public void declare() {
        HbmGameTestSuites.all();
    }
}
