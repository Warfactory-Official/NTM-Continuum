package com.hbm.handler.pollution;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.Chunks;
import dev.golem.core.world.CompatSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-dimension pollution store - 1.7's {@code hbmpollution.dat}, in 64x64 buckets.
 *
 * <p>Shape follows NTM NEXT: a {@link ChunkPos} key holding a <b>region</b> coordinate rather than a
 * chunk one (1.7 indexes with {@code x >> 6}, which is four chunks a side), the four levels as one
 * float list per entry, and a non-creating {@link #getExisting} so the per-tick sweep allocates
 * nothing for a dimension nobody has polluted. The one deviation is the base class: NEXT is a
 * 26.2-only tree and names {@code SavedDataType} directly, which does not exist below 26.1, so this
 * sits on Core's {@link CompatSavedData}.
 */
public final class PollutionSavedData extends CompatSavedData<PollutionSavedData> {

    private static final String ID = Constants.MOD_ID + ":pollution";

    /** 1.7's {@code x >> 6}: a pollution region is 64 blocks, four chunks, on a side. */
    public static final int REGION_BITS = 6;

    private record Entry(int chunkX, int chunkZ, List<Float> values) {}

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("chunkX").forGetter(Entry::chunkX),
            Codec.INT.fieldOf("chunkZ").forGetter(Entry::chunkZ),
            Codec.FLOAT.listOf().fieldOf("values").forGetter(Entry::values)
    ).apply(i, Entry::new));

    public static final Codec<PollutionSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ENTRY_CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(PollutionSavedData::toEntries)
    ).apply(i, PollutionSavedData::fromEntries));

    public final HashMap<ChunkPos, PollutionData> pollution = new HashMap<>();

    @Override
    protected Codec<PollutionSavedData> codec() {
        return CODEC;
    }

    public static PollutionSavedData get(ServerLevel level) {
        return CompatSavedData.get(level, ID, PollutionSavedData::new, CODEC);
    }

    /** Non-creating lookup; the sweep allocates nothing. */
    public static @Nullable PollutionSavedData getExisting(ServerLevel level) {
        return CompatSavedData.getIfPresent(level, ID, PollutionSavedData::new, CODEC);
    }

    /** The region holding a block position. */
    public static ChunkPos region(int blockX, int blockZ) {
        return new ChunkPos(blockX >> REGION_BITS, blockZ >> REGION_BITS);
    }

    private static PollutionSavedData fromEntries(List<Entry> entries) {
        PollutionSavedData data = new PollutionSavedData();
        for (Entry e : entries) {
            PollutionData pd = new PollutionData();
            int n = Math.min(e.values().size(), pd.pollution.length);
            for (int i = 0; i < n; i++) pd.pollution[i] = e.values().get(i);
            data.pollution.put(new ChunkPos(e.chunkX(), e.chunkZ()), pd);
        }
        return data;
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>(pollution.size());
        for (Map.Entry<ChunkPos, PollutionData> e : pollution.entrySet()) {
            List<Float> values = new ArrayList<>(PollutionType.VALUES.length);
            for (float f : e.getValue().pollution) values.add(f);
            entries.add(new Entry(Chunks.chunkX(e.getKey()), Chunks.chunkZ(e.getKey()), values));
        }
        return entries;
    }

    /** One float per {@link PollutionType}, by ordinal (1.7). */
    public static final class PollutionData {
        public float[] pollution = new float[PollutionType.VALUES.length];

        public float get(PollutionType type) {
            return pollution[type.ordinal()];
        }
    }
}
