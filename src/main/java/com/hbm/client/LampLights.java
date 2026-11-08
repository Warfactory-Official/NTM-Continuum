package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.client.gun.JadeShotgunClient;
import com.hbm.items.ModGunItems;
import com.hbm.items.armor.ArmorNo9;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.DynamicLights;
import dev.golem.core.world.LightLevels;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
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
 * tick, and handed back to the light engine five ticks after it was last written. An unchanged spread is not redone
 * while the light engine leaves its sections alone.
 */
public final class LampLights {

    private static final DynamicLights.Cookie FLASHLIGHT = new DynamicLights.Cookie("hbm", "textures/misc/fl_cookie.png");
    private static final int HELMET_RGB = 0xFFE2B8;
    /** 1.12.2 {@code addPointLight(start, new Vec3d(1, 0.72, 0.46), 15)}, range {@code sqrt(15 / 0.15 - 1)}. */
    private static final int MUZZLE_RGB = 0xFFB875;
    private static final float MUZZLE_RANGE = (float) Math.sqrt(15F / 0.15F - 1F);

    private static final int[][] SIDES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** The cells the last spread reached, by packed position: the level it gave, 0 for a cell that stops light. */
    private static Long2IntOpenHashMap lit = new Long2IntOpenHashMap();
    private static Long2IntOpenHashMap spread = new Long2IntOpenHashMap();
    /** Cells a new spread lights less than the last one: the tick after which the light engine takes them back. */
    private static final Long2LongOpenHashMap STALE = new Long2LongOpenHashMap();
    private static final LongSet DIRTY = new LongOpenHashSet();
    /** One written cell per touched section and its value, to see whether the light engine has replaced the section. */
    private static final LongArrayList SAMPLES = new LongArrayList();
    private static final IntArrayList SAMPLE_LEVELS = new IntArrayList();
    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();
    private static long[] queue = new long[2048];
    private static long litStart = Long.MIN_VALUE;
    private static int litLevel;
    private static long litUntil;
    private static @Nullable ClientLevel checkedLevel;

    /** For the harness: lights handed to GemRender last frame, fallback block-light writes, spread cost. */
    private static int lightsLastFrame;
    private static int writes;
    private static int spreads;
    private static int skips;
    private static long spreadNanos;
    private static long maxSpreadNanos;

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
            lit.clear();
            STALE.clear();
            litStart = Long.MIN_VALUE;
            checkedLevel = level;
        }
        if (level == null || mc.player == null) return;
        boolean redo = checkLights(level);
        if (level.getGameTime() % 2 != 0 && !redo || DynamicLights.shading()) return;
        if (helmetLit(mc.player)) lightAlong(level, mc.player, 50F);
        else if (flashlightOn(mc.player)) lightAlong(level, mc.player, 30F);
    }

    /** 1.7: the brightest light, up to 15, at the face looked at, dimming with distance until {@code range}. */
    private static void lightAlong(ClientLevel level, Player player, float range) {
        HitResult hit = player.pick(range, 1F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return;
        double distance = player.getEyePosition(1F).distanceTo(hit.getLocation());
        int light = Math.min(15, (int) (25 - (distance * 25 / range)));
        long start = block.getBlockPos().relative(block.getDirection()).asLong();
        long now = level.getGameTime();
        if (start == litStart && light == litLevel && samplesHold(level)) {
            litUntil = now + 5;
            skips++;
            return;
        }
        long began = System.nanoTime();
        spreadFrom(level, start, light);
        for (Long2IntMap.Entry entry : lit.long2IntEntrySet()) {
            if (entry.getIntValue() > spread.get(entry.getLongKey())) STALE.put(entry.getLongKey(), litUntil);
        }
        Long2IntOpenHashMap previous = lit;
        lit = spread;
        spread = previous;
        spread.clear();
        litStart = start;
        litLevel = light;
        litUntil = now + 5;
        for (long section : DIRTY) {
            level.setSectionDirtyWithNeighbors(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section));
        }
        DIRTY.clear();
        long spent = System.nanoTime() - began;
        spreads++;
        spreadNanos += spent;
        maxSpreadNanos = Math.max(maxSpreadNanos, spent);
    }

    /**
     * Light from {@code start} at {@code light}, one less per block, breadth-first so each cell gets its highest level.
     * Only fully blocking blocks stop it: semi-translucent blocks pass light like air (1.7).
     */
    private static void spreadFrom(ClientLevel level, long start, int light) {
        // port: spreads like a placed light; 1.7's lightUpRecursively walked depth-first and kept each cell's first value
        SAMPLES.clear();
        SAMPLE_LEVELS.clear();
        if (light <= 0) return;
        int head = 0, tail = 0;
        if (light(level, start, light)) queue[tail++] = start;
        while (head < tail) {
            long pos = queue[head++];
            int next = spread.get(pos) - 1;
            if (next <= 0) continue;
            int x = BlockPos.getX(pos), y = BlockPos.getY(pos), z = BlockPos.getZ(pos);
            for (int[] side : SIDES) {
                long neighbour = BlockPos.asLong(x + side[0], y + side[1], z + side[2]);
                if (spread.containsKey(neighbour) || !light(level, neighbour, next)) continue;
                if (tail == queue.length) queue = Arrays.copyOf(queue, tail * 2);
                queue[tail++] = neighbour;
            }
        }
    }

    /** Lights one cell; false, recorded as 0, for a cell that stops light. */
    private static boolean light(ClientLevel level, long pos, int light) {
        CURSOR.set(pos);
        if (LightLevels.opacity(level, CURSOR) >= 15) {
            spread.put(pos, 0);
            return false;
        }
        spread.put(pos, light);
        SectionPos section = SectionPos.of(CURSOR);
        DataLayer layer = level.getLightEngine().getLayerListener(LightLayer.BLOCK).getDataLayerData(section);
        if (layer == null) return true;
        int x = CURSOR.getX() & 15, y = CURSOR.getY() & 15, z = CURSOR.getZ() & 15;
        int existing = layer.get(x, y, z);
        int newLight = Math.max(existing, light);
        if (newLight != existing) {
            layer.set(x, y, z, newLight);
            writes++;
        }
        if (DIRTY.add(section.asLong())) {
            SAMPLES.add(pos);
            SAMPLE_LEVELS.add(newLight);
        }
        return true;
    }

    /** Whether the light engine has left every section of the last spread as it was written. */
    private static boolean samplesHold(ClientLevel level) {
        var engine = level.getLightEngine().getLayerListener(LightLayer.BLOCK);
        for (int i = 0; i < SAMPLES.size(); i++) {
            CURSOR.set(SAMPLES.getLong(i));
            if (engine.getLightValue(CURSOR) < SAMPLE_LEVELS.getInt(i)) return false;
        }
        return true;
    }

    /**
     * 1.7 {@code checkLights}: five ticks after a cell was last lit, the light engine recomputes it. The engine's
     * decrease also clears the lamp's light around it, so its updates run at once; true when it took cells back.
     */
    private static boolean checkLights(ClientLevel level) {
        long now = level.getGameTime();
        LevelLightEngine engine = level.getLightEngine();
        boolean checked = false;
        if (!lit.isEmpty() && now > litUntil) {
            for (Long2IntMap.Entry entry : lit.long2IntEntrySet()) {
                if (entry.getIntValue() > 0) engine.checkBlock(CURSOR.set(entry.getLongKey()));
            }
            lit.clear();
            checked = true;
        }
        ObjectIterator<Long2LongMap.Entry> it = STALE.long2LongEntrySet().iterator();
        while (it.hasNext()) {
            Long2LongMap.Entry entry = it.next();
            if (now <= entry.getLongValue()) continue;
            engine.checkBlock(CURSOR.set(entry.getLongKey()));
            it.remove();
            checked = true;
        }
        if (!checked) return false;
        engine.runLightUpdates();
        litStart = Long.MIN_VALUE;
        return true;
    }

    /** With {@code x, y0, y1, z0, z1}: the client's block light on that x plane, one row per y from the top. */
    static JsonObject query(JsonObject params) {
        JsonObject json = new JsonObject();
        if (params.has("x") && Minecraft.getInstance().level != null) {
            ClientLevel level = Minecraft.getInstance().level;
            int x = params.get("x").getAsInt();
            JsonArray rows = new JsonArray();
            for (int y = params.get("y1").getAsInt(); y >= params.get("y0").getAsInt(); y--) {
                StringBuilder row = new StringBuilder();
                for (int z = params.get("z0").getAsInt(); z <= params.get("z1").getAsInt(); z++) {
                    row.append(String.format("%3d", level.getBrightness(LightLayer.BLOCK, new BlockPos(x, y, z))));
                }
                rows.add(row.toString());
            }
            json.add("plane", rows);
        }
        json.addProperty("shading", DynamicLights.shading());
        json.addProperty("lights_last_frame", lightsLastFrame);
        json.addProperty("fallback_writes", writes);
        int held = STALE.size();
        for (int value : lit.values()) if (value > 0) held++;
        json.addProperty("fallback_lit", held);
        json.addProperty("spreads", spreads);
        json.addProperty("skips", skips);
        json.addProperty("spread_cells", lit.size());
        json.addProperty("spread_micros_avg", spreads == 0 ? 0 : spreadNanos / spreads / 1000);
        json.addProperty("spread_micros_max", maxSpreadNanos / 1000);
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
