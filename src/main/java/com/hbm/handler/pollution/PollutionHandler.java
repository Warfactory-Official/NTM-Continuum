package com.hbm.handler.pollution;

import com.hbm.Constants;
import com.hbm.config.MobConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.handler.pollution.PollutionSavedData.PollutionData;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.Chunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The per-region soot / poison / heavy-metal / fallout field, its diffusion, and what it does to the
 * world. Ported from NTM NEXT, which is 1.7's handler carried onto the modern API.
 *
 * <p>Storage is {@link PollutionSavedData} per dimension. The key is a <b>64-block region</b>, not a
 * chunk - 1.7 indexes with {@code x >> 6}, and reading it as a chunk key makes every source sixteen
 * times too local.
 *
 * <p><b>Cadence is upstream's and matters.</b> The destruction pass runs every server tick; the
 * diffusion pass runs once every 60. Running diffusion every tick multiplies the decay exponent by
 * sixty and pollution never accumulates at all.
 */
public final class PollutionHandler {

    private PollutionHandler() {}

    /** Baserate of soot generation for a furnace-equivalent machine per second. */
    public static final float SOOT_PER_SECOND = 1F / 25F;
    /** Baserate of heavy metal generation, balanced around the soot values of combustion engines. */
    public static final float HEAVY_METAL_PER_SECOND = 1F / 50F;
    /** Baserate for poison when spilled. */
    public static final float POISON_PER_SECOND = 1F / 50F;

    /** Upstream's ceiling on any one type in any one region. */
    public static final float MAX_POLLUTION = 10_000F;

    /** Server ticks between diffusion passes - 1.7's {@code eggTimer}. */
    public static final int DIFFUSION_INTERVAL = 60;

    static final float DESTRUCTION_THRESHOLD = 15F;
    static final int DESTRUCTION_COUNT = 5;

    static int eggTimer = 0;

    /**
     * Rampant-mode guidance target for the scouts; null until a player sleeps with guidance on.
     *
     * <p>A {@link BlockPos} rather than NEXT's {@code Vec3}: Continuum's scout already reads it
     * through {@code Vec3.atCenterOf}, which is the bed's centre rather than its corner.
     */
    public static @Nullable BlockPos targetCoords = null;

    // ------------------------------------------------------------------ utility

    public static void incrementPollution(Level world, BlockPos pos, PollutionType type, float amount) {
        if (!RadiationConfig.enablePollution || pos == null) return;
        if (!(world instanceof ServerLevel server)) return;

        PollutionSavedData ppw = PollutionSavedData.get(server);
        ChunkPos region = PollutionSavedData.region(pos.getX(), pos.getZ());
        PollutionData data = ppw.pollution.computeIfAbsent(region, k -> new PollutionData());
        data.pollution[type.ordinal()] = Mth.clamp(
                (float) (data.pollution[type.ordinal()] + amount * MobConfig.pollutionMult), 0F, MAX_POLLUTION);
        ppw.setDirty();
    }

    public static void decrementPollution(Level world, BlockPos pos, PollutionType type, float amount) {
        incrementPollution(world, pos, type, -amount);
    }

    /** Unlike {@link #incrementPollution} this neither scales by the config multiplier nor clamps. */
    public static void setPollution(Level world, BlockPos pos, PollutionType type, float amount) {
        if (!RadiationConfig.enablePollution) return;
        if (!(world instanceof ServerLevel server)) return;

        PollutionSavedData ppw = PollutionSavedData.get(server);
        ChunkPos region = PollutionSavedData.region(pos.getX(), pos.getZ());
        ppw.pollution.computeIfAbsent(region, k -> new PollutionData()).pollution[type.ordinal()] = amount;
        ppw.setDirty();
    }

    public static float getPollution(Level world, BlockPos pos, PollutionType type) {
        PollutionData data = getPollutionData(world, pos);
        return data == null ? 0F : data.pollution[type.ordinal()];
    }

    /** Every level of the region holding {@code pos}, or null where there are none. */
    public static @Nullable PollutionData getPollutionData(Level world, BlockPos pos) {
        if (!RadiationConfig.enablePollution) return null;
        if (!(world instanceof ServerLevel server)) return null;

        PollutionSavedData ppw = PollutionSavedData.getExisting(server);
        if (ppw == null) return null;
        return ppw.pollution.get(PollutionSavedData.region(pos.getX(), pos.getZ()));
    }

    // ------------------------------------------------------------------ the loop

    public static void updateSystem(MinecraftServer server) {
        if (!RadiationConfig.enablePollution) return;

        handleWorldDestruction(server);

        eggTimer++;
        if (eggTimer < DIFFUSION_INTERVAL) return;
        eggTimer = 0;

        for (ServerLevel world : server.getAllLevels()) {
            PollutionSavedData ppw = PollutionSavedData.getExisting(world);
            if (ppw == null || ppw.pollution.isEmpty()) continue;
            diffuse(ppw);
        }
    }

    /**
     * One diffusion pass: decay in place, then hand a share to the four orthogonal neighbours.
     *
     * <p>Built into a fresh table rather than edited in place, because a region that receives from
     * its neighbour must not then spread what it just received in the same pass - that would make
     * the transport rate depend on iteration order.
     */
    public static void diffuse(PollutionSavedData ppw) {
        HashMap<ChunkPos, PollutionData> newPollution = new HashMap<>();
        final int S = PollutionType.SOOT.ordinal();
        final int H = PollutionType.HEAVYMETAL.ordinal();
        final int P = PollutionType.POISON.ordinal();

        for (Map.Entry<ChunkPos, PollutionData> chunk : ppw.pollution.entrySet()) {
            int x = Chunks.chunkX(chunk.getKey());
            int z = Chunks.chunkZ(chunk.getKey());
            PollutionData data = chunk.getValue();

            float[] forNeighbours = new float[PollutionType.VALUES.length];

            if (data.pollution[S] > 10F) {
                forNeighbours[S] = data.pollution[S] * 0.05F;
                data.pollution[S] *= 0.8F;
            }

            data.pollution[S] *= 0.99F;
            data.pollution[H] *= 0.9995F;

            if (data.pollution[P] > 10F) {
                forNeighbours[P] = data.pollution[P] * 0.025F;
                data.pollution[P] *= 0.9F;
            } else {
                data.pollution[P] *= 0.995F;
            }

            accumulate(newPollution, chunk.getKey(), data.pollution);
            accumulate(newPollution, new ChunkPos(x + 1, z), forNeighbours);
            accumulate(newPollution, new ChunkPos(x - 1, z), forNeighbours);
            accumulate(newPollution, new ChunkPos(x, z + 1), forNeighbours);
            accumulate(newPollution, new ChunkPos(x, z - 1), forNeighbours);
        }

        ppw.pollution.clear();
        ppw.pollution.putAll(newPollution);
        ppw.setDirty();
    }

    /** Add {@code amounts} into {@code table}'s entry for {@code key}, dropping an all-zero result. */
    private static void accumulate(Map<ChunkPos, PollutionData> table, ChunkPos key, float[] amounts) {
        boolean any = false;
        for (float amount : amounts) {
            if (amount > 0F) {
                any = true;
                break;
            }
        }
        if (!any) return;

        PollutionData data = table.computeIfAbsent(key, k -> new PollutionData());
        for (int i = 0; i < amounts.length; i++) {
            data.pollution[i] += amounts[i];
        }
    }

    /**
     * Poison past {@link #DESTRUCTION_THRESHOLD} kills the ground cover, five rolls per region per tick.
     *
     * <p>Only ever touches a region whose chunk is already resident: the roll lands anywhere in a
     * 64-block square, and loading a chunk from inside the server tick to kill a fern would let one
     * polluted region drag its whole neighbourhood into memory.
     */
    static void handleWorldDestruction(MinecraftServer server) {
        final int span = 1 << PollutionSavedData.REGION_BITS;

        for (ServerLevel world : server.getAllLevels()) {
            PollutionSavedData ppw = PollutionSavedData.getExisting(world);
            if (ppw == null || ppw.pollution.isEmpty()) continue;

            RandomSource rand = world.getRandom();

            for (Map.Entry<ChunkPos, PollutionData> pollution : ppw.pollution.entrySet()) {
                if (pollution.getValue().pollution[PollutionType.POISON.ordinal()] < DESTRUCTION_THRESHOLD) continue;

                ChunkPos entryPos = pollution.getKey();

                for (int i = 0; i < DESTRUCTION_COUNT; i++) {
                    int x = (Chunks.chunkX(entryPos) << PollutionSavedData.REGION_BITS) + rand.nextInt(span);
                    int z = (Chunks.chunkZ(entryPos) << PollutionSavedData.REGION_BITS) + rand.nextInt(span);

                    if (world.hasChunk(x >> 4, z >> 4)) {
                        int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - rand.nextInt(3) + 1;
                        PollutionWorldHandler.destroyBlock(world, new BlockPos(x, y, z));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ rampant mode

    /*
     * NOT YET TRIGGERED. Both entry points below are ported and correct, and nothing calls them:
     * 1.7 drives them from PlayerSleepInBedEvent and WorldEvent.PotentialSpawns, and Continuum has
     * no hook for either - NeoForge publishes no sleep event at 26.1, and no loader-neutral spawn
     * hook exists in Core. Every gate they read (rampantGlyphidGuidance, rampantNaturalScoutSpawn)
     * defaults to false, so nothing is silently degraded today; wiring them is what turns rampant
     * mode on. Recorded here rather than left to be rediscovered from a feature that does nothing.
     */

    public static void rampantTargetSetter(BlockPos bedPos) {
        if (MobConfig.rampantGlyphidGuidance) targetCoords = bedPos.immutable();
    }

    public static void rampantScoutPopulator(ServerLevel level, BlockPos pos) {
        if (!MobConfig.rampantNaturalScoutSpawn) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (!level.canSeeSky(pos)) return;
        if (level.getRandom().nextInt(MobConfig.rampantScoutSpawnChance) != 0) return;
        if (getPollution(level, pos, PollutionType.SOOT) < MobConfig.rampantScoutSpawnThresh) return;

        EntityGlyphidScout scout = new EntityGlyphidScout(ModNtmEntities.GLYPHID_SCOUT.get(), level);
        scout.snapTo(pos.getX(), pos.getY(), pos.getZ(), level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!scout.isValidLightLevel()) return;

        // The escort digs the scout out of whatever it spawned inside.
        EntityGlyphidDigger digger = new EntityGlyphidDigger(EntityGlyphidDigger.TYPE.get(), level);
        digger.snapTo(pos.getX(), pos.getY(), pos.getZ(), level.getRandom().nextFloat() * 360.0F, 0.0F);

        if (canSpawnHere(level, scout)) level.addFreshEntity(scout);
        if (canSpawnHere(level, digger)) level.addFreshEntity(digger);
    }

    private static boolean canSpawnHere(ServerLevel level, EntityGlyphid glyphid) {
        // 1.7 EntityGlyphid.getCanSpawnHere, including checkNoEntityCollision
        return level.getDifficulty() != Difficulty.PEACEFUL && level.isUnobstructed(glyphid)
                && level.noCollision(glyphid, glyphid.getBoundingBox())
                && !level.containsAnyLiquid(glyphid.getBoundingBox());
    }

    // ------------------------------------------------------------------ mob effects

    /**
     * 1.7 {@code decorateMob}: a hostile spawning in soot comes out tougher.
     *
     * <p>NEXT leaves this a TODO and ships {@code buffMobThreshold} with no consumer; the threshold
     * is read here instead. Glyphids are excluded - pollution is their own resource, and buffing
     * them with it would let the hive compound on itself.
     *
     * <p>Upstream applies operation {@code 1}, which scales the attribute's base rather than its
     * running total; {@link Attributes#replacePermanentBase} is that operation on every node.
     */
    public static void decorateMob(Entity entity) {
        if (!RadiationConfig.enablePollution) return;
        if (!(entity instanceof Mob mob) || !(entity instanceof Enemy) || entity instanceof EntityGlyphid) return;
        if (mob.level().isClientSide()) return;

        PollutionData data = getPollutionData(mob.level(), mob.blockPosition());
        if (data == null || data.pollution[PollutionType.SOOT.ordinal()] <= RadiationConfig.buffMobThreshold) return;

        Attributes.replacePermanentBase(mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH),
                Constants.MOD_ID + ":soot_anger_health", 1D);
        Attributes.replacePermanentBase(mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                Constants.MOD_ID + ":soot_anger_damage", 1.5D);
        mob.heal(mob.getMaxHealth());
    }
}
