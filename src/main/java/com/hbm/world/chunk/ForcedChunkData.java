package com.hbm.world.chunk;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import dev.golem.core.world.CompatSavedData;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

/**
 * The chunks HBM currently holds forced in one level, persisted so a restart can reconcile them.
 *
 * <p>1.12.2 had Forge's ticket system: a ticket was persisted by the loader, and on world load Forge
 * handed it back to the mod through {@code LoadingCallback} so the entity could rebind (see the
 * contract in {@link com.hbm.entity.logic.IChunkLoader}). Modern Minecraft has no ticket ownership at
 * all — {@code ServerLevel.setChunkForced} is a flat boolean set with no notion of who asked — so the
 * ownership half has to be kept here. This is that half: a plain record of "these chunks are forced
 * because HBM asked", which {@link ChunkLoadingManager} replays at level load and expires if nothing
 * claims them (the analogue of Forge dropping an unclaimed ticket).
 */
public final class ForcedChunkData extends CompatSavedData<ForcedChunkData> {

    private static final String ID = "hbm_forced_chunks";
    /** A named field keeps the pre-26.1 SavedData encoding a CompoundTag root. */
    private static final Codec<ForcedChunkData> CODEC = Codec.LONG.listOf().fieldOf("chunks").codec()
            .xmap(ForcedChunkData::fromList, ForcedChunkData::toList);

    private final LongOpenHashSet chunks = new LongOpenHashSet();

    public static ForcedChunkData get(ServerLevel level) {
        return CompatSavedData.get(level, Constants.MOD_ID + ":" + ID, ForcedChunkData::new, CODEC);
    }

    public LongOpenHashSet chunks() {
        return chunks;
    }

    public void add(long packed) {
        if (chunks.add(packed)) setDirty();
    }

    public void remove(long packed) {
        if (chunks.remove(packed)) setDirty();
    }

    private static ForcedChunkData fromList(List<Long> rows) {
        ForcedChunkData data = new ForcedChunkData();
        rows.forEach(data.chunks::add);
        return data;
    }

    private List<Long> toList() {
        List<Long> rows = new ArrayList<>(chunks.size());
        chunks.forEach((long packed) -> rows.add(packed));
        return rows;
    }

    @Override
    protected Codec<ForcedChunkData> codec() {
        return CODEC;
    }
}
