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

    /**
     * One region: its pollution keyed by {@link PollutionType} name ({@code "soot"}). 1.7 saved one float per type in
     * ordinal order; an ordinal-indexed list breaks the moment the enum changes, so it is never saved (History section
     * 19ej-ids). A pre-2026-10-03 {@code values} list is ignored with one WARN.
     */
    private record Entry(int chunkX, int chunkZ, Map<String, Float> values, boolean legacyList) {}

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("chunkX").forGetter(Entry::chunkX),
            Codec.INT.fieldOf("chunkZ").forGetter(Entry::chunkZ),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("pollution", Map.of()).forGetter(Entry::values),
            Codec.FLOAT.listOf().optionalFieldOf("values", List.of()).forGetter(e -> List.of())
    ).apply(i, (x, z, values, legacy) -> new Entry(x, z, values, !legacy.isEmpty())));

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
            if (e.legacyList()) com.hbm.util.SavedIds.warnNumeric("ordinal-indexed pollution list", "values");
            for (Map.Entry<String, Float> value : e.values().entrySet()) {
                PollutionType type = com.hbm.util.SavedIds.byName(PollutionType.class, value.getKey(), null);
                if (type != null) pd.pollution[type.ordinal()] = value.getValue();
            }
            data.pollution.put(new ChunkPos(e.chunkX(), e.chunkZ()), pd);
        }
        return data;
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>(pollution.size());
        for (Map.Entry<ChunkPos, PollutionData> e : pollution.entrySet()) {
            Map<String, Float> values = new java.util.LinkedHashMap<>();
            for (PollutionType type : PollutionType.VALUES) {
                float f = e.getValue().pollution[type.ordinal()];
                if (f != 0F) values.put(type.name().toLowerCase(java.util.Locale.ROOT), f);
            }
            entries.add(new Entry(Chunks.chunkX(e.getKey()), Chunks.chunkZ(e.getKey()), values, false));
        }
        return entries;
    }

    /** One float per {@link PollutionType}, by ordinal (1.7) - in memory only; saved by name. */
    public static final class PollutionData {
        public float[] pollution = new float[PollutionType.VALUES.length];

        public float get(PollutionType type) {
            return pollution[type.ordinal()];
        }
    }
}
