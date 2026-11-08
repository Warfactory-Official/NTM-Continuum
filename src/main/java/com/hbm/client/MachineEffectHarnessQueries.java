package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.client.particle.DisintegrationParticles;
import com.hbm.client.particle.ParticleCoolingTower;
import com.hbm.client.particle.RBMKFlameParticles;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.render.Particles;
import dev.golem.core.world.SubLevels;
import net.minecraft.world.phys.Vec3;

/**
 * The {@code hbm_effects} harness query: live effects that draw outside the particle engine, by count, and each live
 * cooling-tower puff's position and age; {@code hbm_sublevel}: a world point's position in each sub-level grid it lies in.
 */
public final class MachineEffectHarnessQueries {

    private MachineEffectHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_effects", (mc, params) -> {
            JsonObject json = new JsonObject();
            json.addProperty("flames", RBMKFlameParticles.active());
            json.addProperty("giblets", DisintegrationParticles.gibletCount());
            JsonArray towers = new JsonArray();
            Particles.forEach(mc, particle -> {
                if (!(particle instanceof ParticleCoolingTower tower)) return;
                Vec3 at = Particles.position(tower);
                JsonObject puff = new JsonObject();
                puff.addProperty("x", at.x);
                puff.addProperty("y", at.y);
                puff.addProperty("z", at.z);
                puff.addProperty("age", tower.ageTicks());
                towers.add(puff);
            });
            json.add("towers", towers);
            return json;
        });
        HarnessQueryRegistry.register("hbm_sublevel", (mc, params) -> {
            Vec3 world = new Vec3(params.get("x").getAsDouble(), params.get("y").getAsDouble(), params.get("z").getAsDouble());
            JsonArray grids = new JsonArray();
            for (SubLevels.Local local : SubLevels.intoSubLevels(mc.level, world, 0.5)) {
                JsonObject grid = new JsonObject();
                grid.addProperty("x", local.point().x);
                grid.addProperty("y", local.point().y);
                grid.addProperty("z", local.point().z);
                grids.add(grid);
            }
            JsonObject json = new JsonObject();
            json.add("grids", grids);
            return json;
        });
    }
}
