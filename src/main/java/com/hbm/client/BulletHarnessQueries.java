package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.client.anim.HbmAnimations;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import dev.golem.core.client.Frames;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.WorldEffects;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The {@code hbm_bullets} harness query: nearby bullets and how far a tracer's drawn angles stray from the step taken,
 * the last frame's camera pitch, and with {@code buses} the held gun's running bus values. {@code hbm_frame_moves}
 * counts frames in which an entity of {@code type} moved or turned without a client tick: a packet written straight
 * into its position, which 1.7's interpolated projectiles never show.
 */
public final class BulletHarnessQueries {

    private static String tracked;
    private static boolean hooked;
    private static int frames;
    private static int moves;
    private static double largest;
    private static double travel;
    private static final Map<Integer, double[]> LAST = new HashMap<>();

    private BulletHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_bullets", BulletHarnessQueries::query);
        HarnessQueryRegistry.register("hbm_frame_moves", BulletHarnessQueries::frameMoves);
    }

    private static JsonObject frameMoves(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        json.addProperty("type", tracked);
        json.addProperty("frames", frames);
        json.addProperty("moves", moves);
        json.addProperty("largest", largest);
        json.addProperty("travel", travel);
        if (params.has("type")) {
            if (!hooked) WorldEffects.onBegin(camera -> recordFrame(Minecraft.getInstance()));
            hooked = true;
            tracked = params.get("type").getAsString();
            frames = 0;
            moves = 0;
            largest = 0D;
            travel = 0D;
            LAST.clear();
        }
        return json;
    }

    private static void recordFrame(Minecraft mc) {
        if (tracked == null || mc.player == null || mc.level == null) return;
        frames++;
        for (Entity entity : mc.level.getEntities(mc.player, new AABB(mc.player.blockPosition()).inflate(256D))) {
            if (!tracked.equals(String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())))) continue;
            double[] now = {entity.tickCount, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot()};
            double[] last = LAST.put(entity.getId(), now);
            if (last == null) continue;
            double jump = Math.sqrt((now[1] - last[1]) * (now[1] - last[1]) + (now[2] - last[2]) * (now[2] - last[2])
                    + (now[3] - last[3]) * (now[3] - last[3]));
            travel += jump;
            if (last[0] != now[0]) continue;
            if (jump > 1.0E-4D || Math.abs(now[4] - last[4]) > 1.0E-3D || Math.abs(now[5] - last[5]) > 1.0E-3D) {
                moves++;
                largest = Math.max(largest, jump);
            }
        }
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        if (mc.player == null || mc.level == null) return json;
        json.addProperty("game_time", mc.level.getGameTime());
        json.addProperty("pitch", mc.player.getXRot());
        // The camera's up vector leans along the facing by the pitch; forward's sign differs between versions, up's does not.
        CameraInfo camera = CameraInfo.current();
        Vector3f up = camera.orientation.transform(new Vector3f(0F, 1F, 0F));
        double yaw = Math.toRadians(camera.yRot);
        double lean = -Math.sin(yaw) * up.x() + Math.cos(yaw) * up.z();
        json.addProperty("camera_pitch", Math.toDegrees(Math.atan2(lean, up.y())));
        json.addProperty("partial", Frames.partialTick());
        double radius = params.has("radius") ? params.get("radius").getAsDouble() : 96D;
        JsonArray bullets = new JsonArray();
        for (EntityBulletBaseMK4 bullet : mc.level.getEntitiesOfClass(EntityBulletBaseMK4.class,
                new AABB(mc.player.blockPosition()).inflate(radius))) {
            JsonObject b = new JsonObject();
            Vec3 step = bullet.position().subtract(bullet.xo, bullet.yo, bullet.zo);
            b.addProperty("age", bullet.tickCount);
            b.addProperty("velocity", bullet.velocity);
            b.addProperty("yaw", bullet.getYRot());
            b.addProperty("yaw_old", bullet.yRotO);
            b.addProperty("pitch", bullet.getXRot());
            b.addProperty("pitch_old", bullet.xRotO);
            if (step.lengthSqr() > 1.0E-6D) {
                float stepYaw = (float) Math.toDegrees(Math.atan2(step.x, step.z));
                float stepPitch = (float) Math.toDegrees(Math.atan2(step.y, step.horizontalDistance()));
                // The largest yaw or pitch error the renderer's interpolation reaches within this tick.
                float stray = Math.max(Math.max(Math.abs(Mth.wrapDegrees(bullet.getYRot() - stepYaw)),
                        Math.abs(bullet.yRotO - stepYaw)), Math.max(Math.abs(bullet.getXRot() - stepPitch),
                        Math.abs(bullet.xRotO - stepPitch)));
                b.addProperty("stray", stray);
            }
            bullets.add(b);
        }
        json.add("bullets", bullets);
        if (params.has("buses")) {
            JsonObject buses = new JsonObject();
            for (var element : params.getAsJsonArray("buses")) {
                String bus = element.getAsString();
                JsonArray values = new JsonArray();
                for (double v : HbmAnimations.getRelevantTransformation(bus)) values.add(v);
                buses.add(bus, values);
            }
            json.add("buses", buses);
        }
        return json;
    }
}
