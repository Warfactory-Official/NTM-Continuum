package com.hbm.world.chunk;

import com.hbm.Constants;
import com.hbm.interfaces.IBomb;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatSavedData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/** Persistent detonator requests that actively load and tick their target chunks before firing. */
public final class PendingDetonations extends CompatSavedData<PendingDetonations> {

    private static final String ID = "hbm_pending_detonations";

    private record Entry(long pos, UUID detonator) {}

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
            UUIDUtil.CODEC.fieldOf("detonator").forGetter(Entry::detonator)
    ).apply(instance, Entry::new));
    private static final Codec<PendingDetonations> CODEC = ENTRY_CODEC.listOf().fieldOf("entries").codec()
            .xmap(PendingDetonations::fromEntries, PendingDetonations::toEntries);

    private final Map<Long, UUID> entries = new HashMap<>();
    private static final Map<ServerLevel, LoadingState> LOADING = new WeakHashMap<>();

    private static final class LoadingState {
        ChunkLoaderTicket ticket;
        final Map<Long, Long> releaseAt = new HashMap<>();
    }

    public static PendingDetonations get(ServerLevel level) {
        return CompatSavedData.get(level, Constants.MOD_ID + ":" + ID, PendingDetonations::new, CODEC);
    }

    public static void queue(ServerLevel level, BlockPos pos, UUID detonator) {
        PendingDetonations data = get(level);
        if (!detonator.equals(data.entries.put(pos.asLong(), detonator))) data.setDirty();
        claim(level, pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static boolean isPending(ServerLevel level, BlockPos pos) {
        return get(level).entries.containsKey(pos.asLong());
    }

    public static void cancel(ServerLevel level, BlockPos pos) {
        PendingDetonations data = get(level);
        if (data.entries.remove(pos.asLong()) != null) {
            data.setDirty();
            releaseIfUnused(level, pos.getX() >> 4, pos.getZ() >> 4);
        }
    }

    /** Removes a duplicate queued request while retaining its ticket for the spawned entity handoff. */
    public static void handoff(ServerLevel level, BlockPos pos) {
        PendingDetonations data = get(level);
        if (data.entries.remove(pos.asLong()) == null) return;
        data.setDirty();
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        if (!hasRequestInChunk(data, chunkX, chunkZ)) scheduleRelease(level, chunkX, chunkZ);
    }

    /** Ensures saved requests are loading, polls completion, and retires handoff tickets. */
    public static void tick(ServerLevel level) {
        PendingDetonations data = get(level);
        Set<Long> targetChunks = new HashSet<>();
        for (long packed : data.entries.keySet()) {
            BlockPos pos = BlockPos.of(packed);
            targetChunks.add(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
        }

        for (long packed : targetChunks) {
            int chunkX = ChunkPos.getX(packed);
            int chunkZ = ChunkPos.getZ(packed);
            claim(level, chunkX, chunkZ);
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            if (chunk != null) onChunkLoaded(level, chunk);
        }

        LoadingState state = LOADING.get(level);
        if (state == null || state.ticket == null) return;
        long now = level.getGameTime();
        state.releaseAt.entrySet().removeIf(entry -> {
            if (now < entry.getValue()) return false;
            state.ticket.unforce(ChunkPos.getX(entry.getKey()), ChunkPos.getZ(entry.getKey()));
            return true;
        });
        if (state.ticket.chunks().isEmpty()) {
            state.ticket.release();
            state.ticket = null;
        }
    }

    /** Consumes every request as soon as its forced target chunk becomes resident. */
    public static void onChunkLoaded(ServerLevel level, LevelChunk chunk) {
        PendingDetonations data = get(level);
        ChunkPos loaded = chunk.getPos();
        int loadedX = Compat.chunkX(loaded);
        int loadedZ = Compat.chunkZ(loaded);
        List<Entry> ready = new ArrayList<>();

        data.entries.forEach((packed, detonator) -> {
            BlockPos pos = BlockPos.of(packed);
            if ((pos.getX() >> 4) == loadedX && (pos.getZ() >> 4) == loadedZ) {
                ready.add(new Entry(packed, detonator));
            }
        });
        if (ready.isEmpty()) return;

        for (Entry entry : ready) data.entries.remove(entry.pos());
        data.setDirty();

        for (Entry entry : ready) {
            BlockPos pos = BlockPos.of(entry.pos());
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.detonator());
            if (!(chunk.getBlockState(pos).getBlock() instanceof IBomb bomb)) {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("chat.postoofarerror")
                            .withStyle(ChatFormatting.RED));
                }
                continue;
            }

            IBomb.BombReturnCode result = com.hbm.api.event.HbmEvents.detonate(bomb, level, pos, player, entry.detonator());
            if (player != null) {
                Component message = result.getUnlocalizedMessage().isEmpty()
                        ? Component.translatable("chat.detonated")
                        : Component.translatable(result.getUnlocalizedMessage());
                player.sendSystemMessage(Component.literal("[").append(message).append("]")
                        .withStyle(result.wasSuccessful() ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
            }
        }
        scheduleRelease(level, loadedX, loadedZ);
    }

    public static boolean isLoading(ServerLevel level, BlockPos pos) {
        LoadingState state = LOADING.get(level);
        return state != null && state.ticket != null
                && state.ticket.chunks().contains(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
    }

    public static void forget(ServerLevel level) {
        LOADING.remove(level);
    }

    private static void claim(ServerLevel level, int chunkX, int chunkZ) {
        LoadingState state = LOADING.computeIfAbsent(level, ignored -> new LoadingState());
        if (state.ticket == null || state.ticket.isReleased()) {
            state.ticket = ChunkLoadingManager.requestTicket(level, "pending detonations");
        }
        long packed = ChunkPos.pack(chunkX, chunkZ);
        state.releaseAt.remove(packed);
        state.ticket.force(chunkX, chunkZ);
    }

    private static void scheduleRelease(ServerLevel level, int chunkX, int chunkZ) {
        LoadingState state = LOADING.get(level);
        if (state != null && state.ticket != null) {
            // Keep the source ticking long enough for a spawned MK5 entity to claim it for itself.
            state.releaseAt.put(ChunkPos.pack(chunkX, chunkZ), level.getGameTime() + 2L);
        }
    }

    private static void releaseIfUnused(ServerLevel level, int chunkX, int chunkZ) {
        PendingDetonations data = get(level);
        if (hasRequestInChunk(data, chunkX, chunkZ)) return;
        long chunk = ChunkPos.pack(chunkX, chunkZ);
        LoadingState state = LOADING.get(level);
        if (state == null || state.ticket == null) return;
        state.releaseAt.remove(chunk);
        state.ticket.unforce(chunkX, chunkZ);
        if (state.ticket.chunks().isEmpty()) {
            state.ticket.release();
            state.ticket = null;
        }
    }

    private static boolean hasRequestInChunk(PendingDetonations data, int chunkX, int chunkZ) {
        long chunk = ChunkPos.pack(chunkX, chunkZ);
        for (long packed : data.entries.keySet()) {
            BlockPos pos = BlockPos.of(packed);
            if (ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4) == chunk) return true;
        }
        return false;
    }

    private static PendingDetonations fromEntries(List<Entry> entries) {
        PendingDetonations data = new PendingDetonations();
        entries.forEach(entry -> data.entries.put(entry.pos(), entry.detonator()));
        return data;
    }

    private List<Entry> toEntries() {
        List<Entry> result = new ArrayList<>(entries.size());
        entries.forEach((pos, detonator) -> result.add(new Entry(pos, detonator)));
        return result;
    }

    @Override
    protected Codec<PendingDetonations> codec() {
        return CODEC;
    }
}
