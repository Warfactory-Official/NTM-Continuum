package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.client.particle.CasingParticles;
import com.hbm.client.particle.DebrisParticles;
import com.hbm.client.particle.ParticleSpentCasing;
import com.hbm.particle.helper.CasingCreator;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.render.Particles;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * {@code hbm_turret_fx}: what a shot leaves on the client. Vanilla particles within {@code r} of {@code x/y/z} counted by
 * class, live spent casings by name, and every flying blast chunk with its filled cells, drawn block and sky light and
 * where it was thrown from. {@code spawn_casing} [name, x, y, z, yaw, pitch] drops a still casing there first.
 */
public final class TurretHarnessQueries {

    private TurretHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_turret_fx", TurretHarnessQueries::effects);
    }

    private static JsonObject effects(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        if (mc.level == null) return json;
        if (params.has("spawn_casing")) {
            JsonArray c = params.getAsJsonArray("spawn_casing");
            CasingParticles.spawn(new CasingCreator.Event(c.get(1).getAsDouble(), c.get(2).getAsDouble(),
                    c.get(3).getAsDouble(), 0D, 0D, 0D, c.get(4).getAsFloat(), c.get(5).getAsFloat(), 0F, 0F,
                    c.get(0).getAsString(), false, 0, 0D, 0, false, false));
        }
        Vec3 at = new Vec3(number(params, "x"), number(params, "y"), number(params, "z"));
        double r = params.has("r") ? params.get("r").getAsDouble() : 8D;
        Map<String, Integer> particles = new TreeMap<>();
        Particles.forEach(mc, particle -> {
            if (Particles.position(particle).distanceTo(at) <= r) {
                particles.merge(particle.getClass().getSimpleName(), 1, Integer::sum);
            }
        });
        JsonObject classes = new JsonObject();
        particles.forEach(classes::addProperty);
        json.add("particles", classes);

        Map<String, Integer> casings = new TreeMap<>();
        for (ParticleSpentCasing casing : CasingParticles.live()) casings.merge(casing.config.getName(), 1, Integer::sum);
        JsonObject names = new JsonObject();
        casings.forEach(names::addProperty);
        json.add("casings", names);

        JsonArray debris = new JsonArray();
        for (double[] chunk : DebrisParticles.live()) {
            JsonArray row = new JsonArray();
            for (double value : chunk) row.add(value);
            debris.add(row);
        }
        json.add("debris", debris);
        return json;
    }

    private static double number(JsonObject params, String key) {
        return params.has(key) ? params.get(key).getAsDouble() : 0D;
    }
}
