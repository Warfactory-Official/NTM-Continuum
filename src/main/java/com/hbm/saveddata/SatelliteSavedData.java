package com.hbm.saveddata;

import com.hbm.Constants;
import com.hbm.api.environment.Environment;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import dev.golem.core.world.CompatSavedData;
import dev.golem.core.world.ItemStacks;
import net.minecraft.world.item.ItemStack;
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
    private static final Codec<EnumDriveType> DRIVE_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                for (EnumDriveType type : EnumDriveType.values()) if (type.name().equals(name)) return DataResult.success(type);
                return DataResult.error(() -> "Unknown drive type " + name);
            }, EnumDriveType::name);

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("freq").forGetter(Entry::freq),
            SatelliteType.DISPATCH_CODEC.fieldOf("sat").forGetter(Entry::sat),
            Codec.INT.optionalFieldOf("targetX").forGetter(e -> base(e, e.sat().targetX)),
            Codec.INT.optionalFieldOf("targetZ").forGetter(e -> base(e, e.sat().targetZ)),
            Codec.STRING.optionalFieldOf("tx").forGetter(e -> base(e, e.sat().tx)),
            CompoundTag.CODEC.optionalFieldOf("extra")
                    .forGetter(e -> e.sat().extra.isEmpty() ? Optional.empty() : Optional.of(e.sat().extra)),
            DRIVE_CODEC.optionalFieldOf("driveInput")
                    .forGetter(e -> e.sat().savesBaseData() ? Optional.ofNullable(e.sat().driveInput) : Optional.empty()),
            DRIVE_CODEC.optionalFieldOf("driveOutput")
                    .forGetter(e -> e.sat().savesBaseData() ? Optional.ofNullable(e.sat().driveOutput) : Optional.empty()),
            ItemStacks.optionalCodec().listOf().optionalFieldOf("requestableSlots")
                    .forGetter(e -> e.sat().savesBaseData() && e.sat().requestableSlots.length > 0
                            ? Optional.of(slots(e.sat().requestableSlots)) : Optional.empty())
    ).apply(i, (freq, sat, targetX, targetZ, tx, extra, driveInput, driveOutput, requestable) -> {
        sat.setTarget(targetX.orElse(0), targetZ.orElse(0));
        sat.tx = tx.orElse("");
        sat.extra = extra.map(CompoundTag::copy).orElseGet(CompoundTag::new);
        sat.driveInput = driveInput.orElse(null);
        sat.driveOutput = driveOutput.orElse(null);
        sat.requestableSlots = requestable.map(list -> list.toArray(new ItemStack[0])).orElseGet(() -> new ItemStack[0]);
        return new Entry(freq, sat);
    }));

    private static List<ItemStack> slots(ItemStack[] stacks) {
        List<ItemStack> list = new ArrayList<>(stacks.length);
        for (ItemStack stack : stacks) list.add(stack == null ? ItemStack.EMPTY : stack);
        return list;
    }

    private static <T> Optional<T> base(Entry entry, T value) {
        return entry.sat().savesBaseData() ? Optional.of(value) : Optional.empty();
    }

    public static final Codec<SatelliteSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ENTRY_CODEC.listOf().optionalFieldOf("sats", List.of()).forGetter(SatelliteSavedData::toEntries)
    ).apply(i, SatelliteSavedData::fromEntries));
    private static final String ID = Constants.MOD_ID + ":satellites";

    @Override
    protected Codec<SatelliteSavedData> codec() {
        return CODEC;
    }

    public final Map<Integer, Satellite> sats = new HashMap<>();

    public static SatelliteSavedData get(ServerLevel level) {
        return CompatSavedData.get(level, ID, SatelliteSavedData::new, CODEC);
    }

    /**
     * hbm-space {@code getData(world, x, z)}: the satellites that serve {@code pos}, which an addon may keep in
     * another level (an orbital station's are the body's below; com.hbm.api.environment.Environment.SATELLITES).
     * Plain HBM: {@link #get(ServerLevel)} of {@code level}.
     */
    public static SatelliteSavedData get(ServerLevel level, BlockPos pos) {
        return get(Environment.satelliteLevel(level, pos, ItemStack.EMPTY));
    }

    /** The local namespace wins a frequency collision before other addon-visible registries are searched. */
    public static SatelliteSavedData get(ServerLevel level, BlockPos pos, int frequency) {
        var local = get(level, pos);
        if (local.isFreqTaken(frequency)) return local;
        for (var candidate : Environment.satelliteLevels(level, pos)) {
            var data = get(candidate);
            if (data != local && data.isFreqTaken(frequency)) return data;
        }
        return local;
    }

    private static SatelliteSavedData fromEntries(List<Entry> entries) {
        SatelliteSavedData data = new SatelliteSavedData();
        for (Entry entry : entries) {
            data.attach(entry.freq(), entry.sat());
            entry.sat().isDirty = false;
        }
        return data;
    }

    /** 1.7 {@code ModEventHandler} world tick: every satellite ticks, and a dirty one saves the registry. */
    public void tick(ServerLevel level) {
        boolean dirty = false;
        for (Satellite sat : sats.values()) {
            sat.onUpdateTick(level);
            if (sat.isDirty) {
                sat.isDirty = false;
                dirty = true;
            }
        }
        if (dirty) setDirty();
    }

    public boolean isFreqTaken(int freq) {
        return getSatFromFreq(freq) != null;
    }

    public @Nullable Satellite getSatFromFreq(int freq) {
        return sats.get(freq);
    }

    public void put(int freq, Satellite sat) {
        attach(freq, sat);
        setDirty();
    }

    private void attach(int freq, Satellite sat) {
        var previous = sats.put(freq, sat);
        if (previous != null && previous != sat) previous.attachOwner(null);
        sat.attachOwner(this);
    }

    /**
     * 1.7 {@code CommandSatellites} "descend"; false where nothing held that frequency.
     */
    public boolean descend(int freq) {
        var satellite = sats.remove(freq);
        if (satellite == null) return false;
        satellite.attachOwner(null);
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
