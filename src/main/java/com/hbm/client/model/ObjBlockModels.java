package com.hbm.client.model;

import dev.golem.core.Ids;
import dev.golem.core.client.UnbakedModels;

/** Registers HBM's grouped OBJ resources with the shared native model bakers. */
public final class ObjBlockModels {
    private ObjBlockModels() {}

    public static void install() {
        UnbakedModels.registerGroups(Ids.of("hbm", "obj"), WavefrontObj::load);
    }
}
