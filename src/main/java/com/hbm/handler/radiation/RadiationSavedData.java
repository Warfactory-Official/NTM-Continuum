package com.hbm.handler.radiation;

import java.util.ArrayList;
import java.util.List;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import dev.golem.core.world.CompatSavedData;

/**
 * The world blob radiation used to live in, kept only to empty it. Radiation is now stored per chunk in
 * its own region files ({@link RadiationPayload}, through {@code ChunkSidecar}); a world saved before
 * that move still has its parked densities here, and {@link RadiationSystemNT} drains them across on the
 * first load. Nothing writes to it any more, so once drained it saves empty and stays that way.
 */
public final class RadiationSavedData extends CompatSavedData<RadiationSavedData> {

    private static final String ID = "hbm_radiation";

    private final Long2ObjectMap<double[][]> data = new Long2ObjectOpenHashMap<>();

    // ---- codec ----
    private record SecRow(int section, List<Double> d) {}
    private record ChunkRow(long chunk, List<SecRow> secs) {}

    private static final Codec<SecRow> SEC_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("s").forGetter(SecRow::section),
            Codec.DOUBLE.listOf().fieldOf("d").forGetter(SecRow::d)
    ).apply(i, SecRow::new));

    private static final Codec<ChunkRow> CHUNK_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("c").forGetter(ChunkRow::chunk),
            SEC_CODEC.listOf().fieldOf("s").forGetter(ChunkRow::secs)
    ).apply(i, ChunkRow::new));

    /** A named field keeps the legacy SavedData representation a CompoundTag root. */
    private static final Codec<RadiationSavedData> CODEC = CHUNK_CODEC.listOf().fieldOf("chunks").codec().xmap(
            RadiationSavedData::fromRows, RadiationSavedData::toRows);

    public RadiationSavedData() {}

    public static RadiationSavedData get(ServerLevel level) {
        return CompatSavedData.get(level, Constants.MOD_ID + ":" + ID, RadiationSavedData::new, CODEC);
    }

    @Override
    protected Codec<RadiationSavedData> codec() {
        return CODEC;
    }

    /** One parked chunk, with the packing this blob used already undone. */
    @FunctionalInterface
    public interface ParkedChunk {
        void accept(int chunkX, int chunkZ, double[][] densities);
    }

    /** Hands every parked chunk to {@code sink} and empties the blob. */
    public void drainTo(ParkedChunk sink) {
        if (data.isEmpty()) return;
        for (Long2ObjectMap.Entry<double[][]> e : data.long2ObjectEntrySet()) {
            long key = e.getLongKey();
            sink.accept(ChunkPos.getX(key), ChunkPos.getZ(key), e.getValue());
        }
        data.clear();
        setDirty();
    }

    // ---- codec conversion ----
    private List<ChunkRow> toRows() {
        List<ChunkRow> rows = new ArrayList<>(data.size());
        for (Long2ObjectMap.Entry<double[][]> e : data.long2ObjectEntrySet()) {
            double[][] secs = e.getValue();
            List<SecRow> secRows = new ArrayList<>();
            for (int i = 0; i < secs.length; i++) {
                if (secs[i] == null) continue;
                List<Double> d = new ArrayList<>(secs[i].length);
                for (double v : secs[i]) d.add(v);
                secRows.add(new SecRow(i, d));
            }
            if (!secRows.isEmpty()) rows.add(new ChunkRow(e.getLongKey(), secRows));
        }
        return rows;
    }

    private static RadiationSavedData fromRows(List<ChunkRow> rows) {
        RadiationSavedData rsd = new RadiationSavedData();
        for (ChunkRow row : rows) {
            int maxSection = 0;
            for (SecRow sr : row.secs()) maxSection = Math.max(maxSection, sr.section());
            double[][] secs = new double[maxSection + 1][];
            for (SecRow sr : row.secs()) {
                double[] d = new double[sr.d().size()];
                for (int i = 0; i < d.length; i++) d[i] = sr.d().get(i);
                secs[sr.section()] = d;
            }
            rsd.data.put(row.chunk(), secs);
        }
        return rsd;
    }
}
