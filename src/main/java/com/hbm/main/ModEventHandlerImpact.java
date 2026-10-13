package com.hbm.main;

import com.hbm.api.environment.Environment;
import dev.golem.core.world.Effects;
import dev.golem.core.world.Spawns;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hbm.handler.ImpactWorldHandler;
import com.hbm.packet.ImpactSyncPayload;
import com.hbm.saveddata.TomSaveData;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * 1.7.10's {@code ModEventHandlerImpact}: the server half of Tom's aftermath.
 *
 * <ul>
 *   <li>every tick and level: {@link ImpactWorldHandler#impactEffects}, then the fire burns down over one day
 *       (1/24,000 a tick) while the dust thickens by the same amount, and without fire the dust settles over 600
 *       days (1/14,400,000 a tick); in the overworld, while there is fire and the dust is below 0.75, every living
 *       entity under sky light above 7 is set on fire (5 s) and takes 2 fire damage;</li>
 *   <li>{@link #denySpawn}: after an impact, natural spawns of anything 0.85 tall (or wide) in the overworld and
 *       four in five water mobs anywhere are refused (1.7 {@code extinction}), and world-generation animals are
 *       refused outright (1.7 {@code onPopulate ANIMALS});</li>
 *   <li>the client sync (1.7 {@code PermaSyncHandler}).</li>
 * </ul>
 *
 * <p>The worldgen half (grass to impact dirt, decoration denial, the post-populate wipe) is
 * {@link com.hbm.world.feature.ImpactAftermathFeature}; the darkened sky is {@link com.hbm.client.ImpactSky}.
 * Not ported: 1.7's {@code BiomeEvent.GetVillageBlockID} village rewrite (wood, glass, doors, chests and crops to
 * air, cobble and stone brick a third gravel): modern villages are jigsaw structures with no per-block hook, and the
 * aftermath feature already removes their logs.
 */
public final class ModEventHandlerImpact {

    private ModEventHandlerImpact() {}

    /** 600 days to completely clear all dust. */
    public static final float SETTLE = 1F / 14400000F;
    /** One MC day between initial impact and total darkness. */
    public static final float COOL = 1F / 24000F;

    /** 1.7's {@code theChunkProviderServer.loadedChunks}, per level: a list, so a random pick is uniform. */
    private static final Map<ServerLevel, LoadedChunks> LOADED = new IdentityHashMap<>();
    /** What each player was last sent, with the level it was for. */
    private static final Map<UUID, Object[]> SENT = new HashMap<>();

    public static void init() {
        Lifecycle.onChunkLoad((level, chunk) -> LOADED.computeIfAbsent(level, l -> new LoadedChunks()).add(chunk));
        Lifecycle.onChunkUnload((level, chunk) -> {
            LoadedChunks chunks = LOADED.get(level);
            if (chunks != null) chunks.remove(chunk);
        });
        Lifecycle.onLevelUnload((server, level) -> LOADED.remove(level));
        Lifecycle.onServerStopping(server -> {
            LOADED.clear();
            SENT.clear();
        });
        Lifecycle.onServerTick(ModEventHandlerImpact::tick);
        Spawns.onSpawnCheck(ModEventHandlerImpact::denySpawn);
    }

    private static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) worldTick(level);
        sync(server);
    }

    /** 1.7 {@code worldTick}. */
    public static void worldTick(ServerLevel world) {
        LoadedChunks chunks = LOADED.get(world);
        ImpactWorldHandler.impactEffects(world, chunks == null ? List.of() : chunks.list);
        TomSaveData data = TomSaveData.get(world);
        decay(data);

        // 1.7 dimensionId == 0; hbm-space dropped the check for its impacted planets (com.hbm.api.environment.Environment.SURFACE)
        if (Environment.isSurface(world) && data.fire > 0 && data.dust < 0.75F) {
            List<LivingEntity> burning = new ArrayList<>();
            for (Entity e : world.getAllEntities()) {
                // 1.7 casts the position to int (towards zero), not floor
                if (e instanceof LivingEntity living && world.getBrightness(LightLayer.SKY,
                        new BlockPos((int) e.getX(), (int) e.getY(), (int) e.getZ())) > 7) {
                    burning.add(living);
                }
            }
            for (LivingEntity entity : burning) {
                // 1.7 setFire(5): Fire Protection applies, never shortens a longer burn
                Effects.ignite(entity, 5);
                Compat.hurt(entity, world, world.damageSources().onFire(), 2);
            }
        }
    }

    /** One tick of 1.7's dust and fire: the dust settles only once the fire is out; the fire turns into dust. */
    public static void decay(TomSaveData data) {
        if (data.dust > 0 && data.fire == 0) {
            data.dust = Math.max(0, data.dust - SETTLE);
            data.setDirty();
        }

        if (data.fire > 0) {
            data.fire = Math.max(0, (data.fire - COOL));
            data.dust = Math.min(1, (data.dust + COOL));
            data.setDirty();
        }
    }

    /**
     * 1.7 {@code extinction} (CheckSpawn) and {@code onPopulate} (ANIMALS). True refuses the spawn.
     * The size test keeps 1.7's precedence: {@code height >= 0.85 || (width >= 0.85 && !water && !child)}.
     * {@code WORLD_SPAWNER} spawns (pillager patrols, phantoms, wandering traders - modern periodic spawners 1.7 did
     * not have, standing where its natural spawner was) get the natural rule, so the impact winter refuses them too;
     * raids and village sieges are not asked (1.7's siege never fired CheckSpawn).
     */
    /** Spawns this hook refused since start (harness evidence only). */
    public static int denied;

    public static boolean denySpawn(Mob mob, ServerLevel level, Spawns.Reason reason) {
        boolean deny = denySpawn17(mob, level, reason);
        if (deny) denied++;
        return deny;
    }

    private static boolean denySpawn17(Mob mob, ServerLevel level, Spawns.Reason reason) {
        if (!TomSaveData.impact(level)) return false;

        if (reason == Spawns.Reason.CHUNK_GENERATION) return true;

        boolean water = mob.getType().getCategory() == MobCategory.WATER_CREATURE
                || mob.getType().getCategory() == MobCategory.WATER_AMBIENT
                || mob.getType().getCategory() == MobCategory.UNDERGROUND_WATER_CREATURE
                || mob.getType().getCategory() == MobCategory.AXOLOTLS;

        // (1.7 also excluded players, which never reach a mob spawn check here)
        if (Environment.isSurface(level)) { // 1.7 dimensionId == 0; hbm-space: any impacted body
            if (mob.getBbHeight() >= 0.85F || mob.getBbWidth() >= 0.85F && !water && !mob.isBaby()) {
                return true;
            }
        }
        if (water) {
            return level.getRandom().nextInt(5) != 0;
        }
        return false;
    }

    /** 1.7 {@code PermaSyncHandler}'s impact block, sent when it changed for the player. */
    private static void sync(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) continue;
            TomSaveData data = TomSaveData.published(level);
            float fire = data == null ? 0F : data.fire;
            float dust = data == null ? 0F : data.dust;
            boolean impact = data != null && data.impact;
            Object[] last = SENT.get(player.getUUID());
            if (last != null && last[0] == level && (float) last[1] == fire && (float) last[2] == dust
                    && (boolean) last[3] == impact) continue;
            SENT.put(player.getUUID(), new Object[] {level, fire, dust, impact});
            GolemNetwork.sendToPlayer(player, ImpactSyncPayload.CODEC, new ImpactSyncPayload(fire, dust, impact));
        }
    }

    /** A list of the loaded chunks with O(1) removal. */
    private static final class LoadedChunks {
        final List<LevelChunk> list = new ArrayList<>();
        final Map<LevelChunk, Integer> index = new IdentityHashMap<>();

        void add(LevelChunk chunk) {
            if (index.containsKey(chunk)) return;
            index.put(chunk, list.size());
            list.add(chunk);
        }

        void remove(LevelChunk chunk) {
            Integer i = index.remove(chunk);
            if (i == null) return;
            LevelChunk last = list.remove(list.size() - 1);
            if (last != chunk) {
                list.set(i, last);
                index.put(last, i);
            }
        }
    }
}
