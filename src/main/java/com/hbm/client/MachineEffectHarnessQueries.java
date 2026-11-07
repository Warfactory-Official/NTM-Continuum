package com.hbm.client;

import com.google.gson.JsonObject;
import com.hbm.client.particle.DisintegrationParticles;
import com.hbm.client.particle.RBMKFlameParticles;
import dev.golem.core.harness.HarnessQueryRegistry;

/** The {@code hbm_effects} harness query: live effects that draw outside the particle engine, by count. */
public final class MachineEffectHarnessQueries {

    private MachineEffectHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_effects", (mc, params) -> {
            JsonObject json = new JsonObject();
            json.addProperty("flames", RBMKFlameParticles.active());
            json.addProperty("giblets", DisintegrationParticles.gibletCount());
            return json;
        });
    }
}
