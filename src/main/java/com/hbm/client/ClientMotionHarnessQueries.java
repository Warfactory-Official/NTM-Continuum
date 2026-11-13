package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.client.particle.CasingParticles;
import com.hbm.client.particle.DisintegrationParticles;
import com.hbm.client.particle.ParticleSpentCasing;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.render.WorldEffects;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only motion of short-lived effects. {@code hbm_casing_states}: each live spent casing's position and angles.
 * {@code hbm_giblets}: each live giblet's position, horizontal speed and fall this tick; {@code spawn} gibs the nearest
 * non-player living entity within 16 blocks. {@code hbm_beam_frames}: for every beam seen since {@code reset}, whether
 * its first rendered frame already had the server's measured state, and how far its angles moved after that frame.
 */
public final class ClientMotionHarnessQueries {

    private static final Map<Integer, float[]> BEAMS = new HashMap<>();
    private static boolean hooked;

    private ClientMotionHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_casing_states", ClientMotionHarnessQueries::casings);
        HarnessQueryRegistry.register("hbm_giblets", ClientMotionHarnessQueries::giblets);
        HarnessQueryRegistry.register("hbm_beam_frames", ClientMotionHarnessQueries::beams);
    }

    private static JsonObject casings(Minecraft mc, JsonObject params) {
        JsonArray rows = new JsonArray();
        for (ParticleSpentCasing casing : CasingParticles.live()) {
            JsonArray row = new JsonArray();
            row.add(casing.x);
            row.add(casing.y);
            row.add(casing.z);
            row.add(casing.rotationYaw);
            row.add(casing.rotationPitch);
            row.add(casing.age());
            rows.add(row);
        }
        JsonObject json = new JsonObject();
        json.add("casings", rows);
        return json;
    }

    private static JsonObject giblets(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        if (mc.level == null || mc.player == null) return json;
        if (params.has("spawn")) {
            LivingEntity target = null;
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity living) || entity instanceof Player) continue;
                if (entity.distanceTo(mc.player) > 16F) continue;
                if (target == null || entity.distanceTo(mc.player) < target.distanceTo(mc.player)) target = living;
            }
            if (target != null) {
                DisintegrationParticles.giblets(mc.level, mc.level.getRandom(), target.getX(),
                        target.getY() + target.getBbHeight() * 0.5D, target.getZ(), target.getId(), 0, 0);
                json.addProperty("origin", target.getX() + " " + target.getZ());
            }
        }
        JsonArray rows = new JsonArray();
        for (double[] giblet : DisintegrationParticles.gibletMotion()) {
            JsonArray row = new JsonArray();
            for (double value : giblet) row.add(value);
            rows.add(row);
        }
        json.add("giblets", rows);
        return json;
    }

    private static JsonObject beams(Minecraft mc, JsonObject params) {
        if (!hooked) WorldEffects.onBegin(camera -> recordBeams(Minecraft.getInstance()));
        hooked = true;
        if (params.has("reset")) BEAMS.clear();
        JsonArray rows = new JsonArray();
        for (Map.Entry<Integer, float[]> beam : BEAMS.entrySet()) {
            float[] v = beam.getValue();
            JsonObject row = new JsonObject();
            row.addProperty("id", beam.getKey());
            row.addProperty("first_ready", v[0] > 0F);
            row.addProperty("first_length", v[1]);
            row.addProperty("turn", Math.max(Math.abs(v[4] - v[2]), Math.abs(v[5] - v[3])));
            rows.add(row);
        }
        JsonObject json = new JsonObject();
        json.add("beams", rows);
        return json;
    }

    /** Per beam: first frame's readiness, length, yaw and pitch, then the latest yaw and pitch. */
    private static void recordBeams(Minecraft mc) {
        if (mc.level == null) return;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EntityBulletBeamBase beam)) continue;
            float[] v = BEAMS.computeIfAbsent(beam.getId(), id -> new float[] {beam.hasRenderState() ? 1F : 0F,
                    beam.hasRenderState() ? (float) beam.beamLength : -1F, beam.getYRot(), beam.getXRot(), 0F, 0F});
            v[4] = beam.getYRot();
            v[5] = beam.getXRot();
        }
    }
}
