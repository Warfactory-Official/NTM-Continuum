package com.hbm.handler.radiation;

import java.util.ArrayList;
import java.util.List;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=26.1 {
import net.minecraft.world.level.saveddata.SavedDataType;
//?}
//? if >=26.1 {
//?}

/**
 * Persists chunk radiation across save/load — the cross-loader replacement for the fork's per-chunk NBT
 * payload. Holds, per <b>unloaded</b> chunk, its per-section pocket densities (loaded chunks live in
 * {@link RadiationSystemNT.WorldRadiationData}; they're flushed here on unload and before a level save).
 * On chunk load the entry is popped and re-applied by pocket index (deterministic flood-fill order → the
 * indices line up as long as the blocks didn't change, which they can't while a chunk is unloaded).
 */
public final class RadiationSavedData extends SavedData {

    private static final String ID = "hbm_radiation";

    // chunkKey (ChunkPos.asLong) -> [sectionIndex] -> densities[pocket]  (rows may be null)
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

    private static final Codec<RadiationSavedData> CODEC = CHUNK_CODEC.listOf().xmap(
            RadiationSavedData::fromRows, RadiationSavedData::toRows);

    //? if >=26.1 {
    public static final SavedDataType<RadiationSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, ID),
            RadiationSavedData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);
    //?}

    public RadiationSavedData() {}

    public static RadiationSavedData get(ServerLevel level) {
        //? if >=26.1 {
        return level.getDataStorage().computeIfAbsent(TYPE);
        //?} else {
        /*// Pre-26.1 SavedData is CompoundTag-based and keyed by a plain string; the Codec above is
        // reused by hand so both versions persist an identical on-disk shape.
        return level.getDataStorage().computeIfAbsent(
                new net.minecraft.world.level.saveddata.SavedData.Factory<>(
                        RadiationSavedData::new,
                        (tag, registries) -> CODEC
                                .parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("rad"))
                                .result().orElseGet(RadiationSavedData::new),
                        DataFixTypes.SAVED_DATA_MAP_DATA),
                ID);
        *///?}
    }

    //? if <26.1 {
    /*@Override
    public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag,
                                              net.minecraft.core.HolderLookup.Provider registries) {
        CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, this).result()
                .ifPresent(encoded -> tag.put("rad", encoded));
        return tag;
    }
    *///?}

    /** Pops (removes + returns) the stored densities for a chunk, or null. */
    public double[][] take(long chunkKey) {
        double[][] v = data.remove(chunkKey);
        if (v != null) setDirty();
        return v;
    }

    public void put(long chunkKey, double[][] densities) {
        data.put(chunkKey, densities);
        setDirty();
    }

    public void remove(long chunkKey) {
        if (data.remove(chunkKey) != null) setDirty();
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
