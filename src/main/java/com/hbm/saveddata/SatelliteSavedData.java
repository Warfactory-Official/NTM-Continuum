package com.hbm.saveddata;

import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import dev.golem.core.world.CompatSavedData;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7 {@code SatelliteSavedData}: the orbit registry keyed by satellite-chip frequency, per dimension as 1.7's
 * {@code perWorldStorage} was.
 */
public class SatelliteSavedData extends CompatSavedData<SatelliteSavedData> {

    /**
     * 1.7 {@code SatelliteBase.writeToNBT} rides on the entry: the target and the reply register belong to
     * every satellite, so they are stated once here rather than in each type's own codec. A class whose 1.7
     * {@code writeToNBT} skipped super ({@link Satellite#savesBaseData()}) writes none of the three.
     */
    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("freq").forGetter(Entry::freq),
            SatelliteType.DISPATCH_CODEC.fieldOf("sat").forGetter(Entry::sat),
            Codec.INT.optionalFieldOf("targetX").forGetter(e -> base(e, e.sat().targetX)),
            Codec.INT.optionalFieldOf("targetZ").forGetter(e -> base(e, e.sat().targetZ)),
            Codec.STRING.optionalFieldOf("tx").forGetter(e -> base(e, e.sat().tx))
    ).apply(i, (freq, sat, targetX, targetZ, tx) -> {
        sat.setTarget(targetX.orElse(0), targetZ.orElse(0));
        sat.tx = tx.orElse("");
        return new Entry(freq, sat);
    }));

    private static <T> java.util.Optional<T> base(Entry entry, T value) {
        return entry.sat().savesBaseData() ? java.util.Optional.of(value) : java.util.Optional.empty();
    }

    public static final Codec<SatelliteSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ENTRY_CODEC.listOf().optionalFieldOf("sats", List.of()).forGetter(SatelliteSavedData::toEntries)
    ).apply(i, SatelliteSavedData::fromEntries));
    private static final String ID = com.hbm.Constants.MOD_ID + ":satellites";

    @Override
    protected Codec<SatelliteSavedData> codec() {
        return CODEC;
    }

    public final Map<Integer, Satellite> sats = new HashMap<>();

    public static SatelliteSavedData get(ServerLevel level) {
        return CompatSavedData.get(level, ID, SatelliteSavedData::new, CODEC);
    }

    private static SatelliteSavedData fromEntries(List<Entry> entries) {
        SatelliteSavedData data = new SatelliteSavedData();
        for (Entry entry : entries) data.sats.put(entry.freq(), entry.sat());
        return data;
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

    /**
     * 1.7 {@code CommandSatellites} "descend"; false where nothing held that frequency.
     */
    public boolean descend(int freq) {
        if (sats.remove(freq) == null) return false;
        setDirty();
        return true;
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>(sats.size());
        for (Map.Entry<Integer, Satellite> sat : sats.entrySet()) {
            entries.add(new Entry(sat.getKey(), sat.getValue()));
        }
        return entries;
    }

    private record Entry(int freq, Satellite sat) {
    }
}
