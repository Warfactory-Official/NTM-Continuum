package com.hbm.saveddata.satellites;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.CompatSavedData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything in orbit over one level, keyed by frequency. Satellites have no position and no block entity,
 * so this is the only place they exist; a sat link finds one by asking for its frequency.
 *
 * <p>Each row keeps the satellite's registry slot beside its own {@link CompoundTag}, so a satellite whose
 * slot is not ported yet survives a save/load round trip as an unrecognised row rather than being dropped.
 */
public final class SatelliteSavedData extends CompatSavedData<SatelliteSavedData> {

    private static final String ID = "satellites";

    private final Int2ObjectMap<Satellite> sats = new Int2ObjectOpenHashMap<>();
    /** Rows whose registry slot is empty, kept verbatim so an unported satellite is not lost on load. */
    private final List<Row> unknown = new ArrayList<>();

    private record Row(int freq, int id, CompoundTag data) {}

    private static final Codec<Row> ROW_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("freq").forGetter(Row::freq),
            Codec.INT.fieldOf("id").forGetter(Row::id),
            CompoundTag.CODEC.fieldOf("data").forGetter(Row::data)
    ).apply(i, Row::new));

    private static final Codec<SatelliteSavedData> CODEC = ROW_CODEC.listOf().fieldOf("sats").codec()
            .xmap(SatelliteSavedData::fromRows, SatelliteSavedData::toRows);

    public SatelliteSavedData() {}

    public static SatelliteSavedData get(ServerLevel level) {
        return CompatSavedData.get(level, Constants.MOD_ID + ":" + ID, SatelliteSavedData::new, CODEC);
    }

    @Override
    protected Codec<SatelliteSavedData> codec() {
        return CODEC;
    }

    public boolean isFreqTaken(int freq) {
        return getSatFromFreq(freq) != null;
    }

    public @Nullable Satellite getSatFromFreq(int freq) {
        return sats.get(freq);
    }

    public void put(int freq, Satellite sat) {
        sats.put(freq, sat);
        setDirty();
    }

    public void remove(int freq) {
        if (sats.remove(freq) != null) setDirty();
    }

    public Int2ObjectMap<Satellite> all() {
        return sats;
    }

    private static SatelliteSavedData fromRows(List<Row> rows) {
        SatelliteSavedData data = new SatelliteSavedData();
        for (Row row : rows) {
            Satellite sat = SatelliteRegistry.create(row.id());
            if (sat == null) {
                data.unknown.add(row);
                continue;
            }
            sat.load(row.data());
            data.sats.put(row.freq(), sat);
        }
        return data;
    }

    private static List<Row> toRows(SatelliteSavedData data) {
        List<Row> rows = new ArrayList<>(data.unknown);
        for (Int2ObjectMap.Entry<Satellite> entry : data.sats.int2ObjectEntrySet()) {
            CompoundTag tag = new CompoundTag();
            entry.getValue().save(tag);
            rows.add(new Row(entry.getIntKey(), entry.getValue().getID(), tag));
        }
        return rows;
    }
}
