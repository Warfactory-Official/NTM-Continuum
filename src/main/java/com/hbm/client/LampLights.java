package com.hbm.client;

import com.google.gson.JsonObject;
import com.hbm.client.gun.JadeShotgunClient;
import com.hbm.items.ModGunItems;
import com.hbm.items.armor.ArmorNo9;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.DynamicLights;
import dev.golem.core.world.LightLevels;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The mining helmet's lamp (1.7 {@code ArmorNo9.onArmorTick}, client half) and the jade shotgun's flashlight (1.12.2
 * {@code ItemGunJShotty.playerWorldRender}). With GemRender both are spot lights on every player. Without it the local
 * player's lamp is 1.7's: block light written into the client's light data around the block looked at, every other
 * tick, and handed back to the light engine five ticks after it was last written.
 */
public final class LampLights {

    private static final DynamicLights.Cookie FLASHLIGHT = new DynamicLights.Cookie("hbm", "textures/misc/fl_cookie.png");
    private static final int HELMET_RGB = 0xFFE2B8;
    /** 1.12.2 {@code addPointLight(start, new Vec3d(1, 0.72, 0.46), 15)}, range {@code sqrt(15 / 0.15 - 1)}. */
    private static final int MUZZLE_RGB = 0xFFB875;
    private static final float MUZZLE_RANGE = (float) Math.sqrt(15F / 0.15F - 1F);

    private static final Set<BlockPos> BREADCRUMB = new HashSet<>();
    private static final Map<BlockPos, Long> LIGHT_CHECK = new HashMap<>();
    private static final LongSet DIRTY = new LongOpenHashSet();
    private static @Nullable ClientLevel checkedLevel;

    /** For the harness: lights handed to GemRender last frame, block-light writes made by the fallback. */
    private static int lightsLastFrame;
    private static int writes;

    private LampLights() {}

    public static void register() {
        DynamicLights.register(LampLights::collect);
        ClientTicks.onEndTick(LampLights::tick);
    }

    private static boolean helmetLit(Player player) {
        var head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.getItem() instanceof ArmorNo9 && ArmorNo9.isOn(head);
    }

    private static boolean flashlightOn(Player player) {
        var held = player.getMainHandItem();
        return held.is(ModGunItems.gun_jshotgun.get()) && XFactory10ga.getFlashlightActive(held);
    }

    private static void collect(DynamicLights.Sink sink, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        int lights = 0;
        if (mc.level != null) {
            for (Player player : mc.level.players()) {
                Vector3f look = player.getViewVector(partialTick).toVector3f();
                Vector3f up = Vec3.directionFromRotation(player.getViewXRot(partialTick) - 90F,
                        player.getViewYRot(partialTick)).toVector3f();
                if (helmetLit(player)) {
                    Vec3 eye = player.getEyePosition(partialTick);
                    sink.spot(eye.x, eye.y, eye.z, look, up, 50F, 10F, 40F, HELMET_RGB, 1400F, null, true, 30F);
                    lights++;
                }
                boolean jade = player.getMainHandItem().is(ModGunItems.gun_jshotgun.get());
                boolean fired = jade && player == mc.player && mc.level.getGameTime() == JadeShotgunClient.lastShotTime;
                if (!flashlightOn(player) && !fired) continue;
                Vec3 start = flashlightStart(mc, player, partialTick);
                if (flashlightOn(player)) {
                    // port: 1.12.2's 20 degree cone widened to 30; softness sqrt(500 / 2) = its falloff clamp at 2
                    sink.spot(start.x, start.y, start.z, look, up, 30F, 29F, 30F, 0xFFFFFF, 500F, FLASHLIGHT, true,
                            15.8F);
                    lights++;
                }
                if (fired) {
                    sink.point(start.x, start.y, start.z, MUZZLE_RANGE, MUZZLE_RGB, 15F, false);
                    lights++;
                }
            }
        }
        lightsLastFrame = lights;
    }

    /**
     * 1.12.2's lamp: 2 blocks ahead, 0.28 to the right and 0.1 below the eye. In first person it sat 1 block from the
     * camera toward the lamp on the hand model, so it never starts inside a wall the player faces.
     */
    private static Vec3 flashlightStart(Minecraft mc, Player player, float partialTick) {
        Vec3 offset = new Vec3(-0.28, -0.1, 2);
        if (player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) offset = offset.normalize();
        return offset.xRot(-(float) Math.toRadians(player.getViewXRot(partialTick)))
                .yRot(-(float) Math.toRadians(player.getViewYRot(partialTick)))
                .add(player.getEyePosition(partialTick));
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != checkedLevel) {
            LIGHT_CHECK.clear();
            checkedLevel = level;
        }
        if (level == null || mc.player == null) return;
        checkLights(level);
        if (level.getGameTime() % 2 != 0 || DynamicLights.shading()) return;
        if (helmetLit(mc.player)) lightAlong(level, mc.player, 50F);
        else if (flashlightOn(mc.player)) lightAlong(level, mc.player, 30F);
    }

    /** 1.7: the brightest light, up to 15, at the face looked at, dimming with distance until {@code range}. */
    private static void lightAlong(ClientLevel level, Player player, float range) {
        HitResult hit = player.pick(range, 1F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return;
        double distance = player.getEyePosition(1F).distanceTo(hit.getLocation());
        int light = Math.min(15, (int) (25 - (distance * 25 / range)));
        lightUpRecursively(level, block.getBlockPos().relative(block.getDirection()), light);
        BREADCRUMB.clear();
        for (long section : DIRTY) {
            level.setSectionDirtyWithNeighbors(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section));
        }
        DIRTY.clear();
    }

    private static void lightUpRecursively(ClientLevel level, BlockPos pos, int light) {
        if (light <= 0) return;
        if (!BREADCRUMB.add(pos)) return;
        // Only fully blocking blocks stop it: light goes through semi-translucent blocks like it's air.
        if (LightLevels.opacity(level, pos) >= 15) return;
        SectionPos section = SectionPos.of(pos);
        DataLayer layer = level.getLightEngine().getLayerListener(LightLayer.BLOCK).getDataLayerData(section);
        if (layer == null) return;

        int x = pos.getX() & 15, y = pos.getY() & 15, z = pos.getZ() & 15;
        int existing = layer.get(x, y, z);
        int newLight = Math.min(15, Math.max(existing, light));
        if (newLight != existing) {
            layer.set(x, y, z, newLight);
            DIRTY.add(section.asLong());
            writes++;
        }
        LIGHT_CHECK.put(pos, level.getGameTime() + 5);

        lightUpRecursively(level, pos.east(), light - 1);
        lightUpRecursively(level, pos.west(), light - 1);
        lightUpRecursively(level, pos.above(), light - 1);
        lightUpRecursively(level, pos.below(), light - 1);
        lightUpRecursively(level, pos.south(), light - 1);
        lightUpRecursively(level, pos.north(), light - 1);
    }

    /** 1.7 {@code checkLights}: the light engine recomputes what the lamp stopped lighting. */
    private static void checkLights(ClientLevel level) {
        if (LIGHT_CHECK.isEmpty()) return;
        LevelLightEngine engine = level.getLightEngine();
        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> it = LIGHT_CHECK.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            if (now > entry.getValue()) {
                engine.checkBlock(entry.getKey());
                it.remove();
            }
        }
    }

    static JsonObject query() {
        JsonObject json = new JsonObject();
        json.addProperty("shading", DynamicLights.shading());
        json.addProperty("lights_last_frame", lightsLastFrame);
        json.addProperty("fallback_writes", writes);
        json.addProperty("fallback_lit", LIGHT_CHECK.size());
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            json.addProperty("helmet_lit", helmetLit(mc.player));
            json.addProperty("flashlight_on", flashlightOn(mc.player));
            HitResult hit = mc.player.pick(50F, 1F, false);
            if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos face = block.getBlockPos().relative(block.getDirection());
                json.addProperty("face_block_light", mc.level.getBrightness(LightLayer.BLOCK, face));
            }
        }
        return json;
    }
}
