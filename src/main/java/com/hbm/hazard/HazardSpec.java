package com.hbm.hazard;

import com.hbm.Constants;
import com.hbm.config.RadiationConfig;
import com.hbm.hazard.modifier.HazardModifiers;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.type.IHazardType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One value of the {@code hbm:hazards} item data map ({@link com.hbm.api.data.HazardDataMaps#HAZARDS}): what
 * {@link HazardData} holds, in a form a JSON file can state.
 *
 * <pre>
 * "hbm:ingot_pu238": {"mutex": 1, "entries": [{"type": "hbm:radiation", "level": 10.0}, {"type": "hbm:hot", "level": 3.0}]},
 * "#c:ores/uranium": {"immune": true}
 * </pre>
 *
 * {@code override} and {@code mutex} are the 1.7 {@code HazardData} flags; {@code immune} is 1.7's hazard blacklist
 * ({@code HazardSystem.blacklist}): a stack matched by an immune value has no hazards at all. A type is named by its
 * {@link HazardRegistry#registerType} id and resolved when a stack is looked up, so an addon's type works in its own
 * datapack file as soon as the addon registered it.
 */
public record HazardSpec(boolean override, int mutex, boolean immune, List<Entry> entries) {

    /** One hazard: a registered type, the base level, and named modifiers in application order. */
    public record Entry(String type, double level, List<IHazardModifier> modifiers) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(Entry::type),
                Codec.DOUBLE.optionalFieldOf("level", 1D).forGetter(Entry::level),
                HazardModifiers.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(Entry::modifiers)
        ).apply(i, Entry::new));
    }

    public static final Codec<HazardSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("override", false).forGetter(HazardSpec::override),
            Codec.INT.optionalFieldOf("mutex", 0).forGetter(HazardSpec::mutex),
            Codec.BOOL.optionalFieldOf("immune", false).forGetter(HazardSpec::immune),
            Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(HazardSpec::entries)
    ).apply(i, HazardSpec::new));

    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    /**
     * The runtime form. CONTAMINATING entries are left out while ground contamination is off: 1.7's {@code HazardData}
     * dropped them when registered ({@code addEntry} checked {@code enableContaminationOnGround}), and every 1.7
     * registration of the type went through that gate.
     */
    public HazardData toData() {
        HazardData data = new HazardData().setMutex(mutex).setOverride(override);
        for (Entry entry : entries) {
            IHazardType type = HazardRegistry.getType(Ids.parse(entry.type()));
            if (type == null) {
                if (REPORTED.add(entry.type())) Constants.LOG.warn("Hazards data map: unknown hazard type {}", entry.type());
                continue;
            }
            if (type == HazardRegistry.CONTAMINATING && !RadiationConfig.enableContaminationOnGround) continue;
            HazardEntry runtime = new HazardEntry(type, entry.level());
            for (IHazardModifier mod : entry.modifiers()) runtime.addMod(mod);
            data.addEntry(runtime);
        }
        return data;
    }

    /** The data form of {@code data}; throws for an unnamed type or a payload, which a file cannot state. */
    public static HazardSpec of(HazardData data) {
        List<Entry> entries = new ArrayList<>();
        for (HazardEntry entry : data.entries) {
            var id = HazardRegistry.getTypeId(entry.type);
            if (id == null) throw new IllegalArgumentException("Unnamed hazard type " + entry.type);
            if (entry.payload != null) throw new IllegalArgumentException("Hazard payloads cannot be written as data: " + id);
            entries.add(new Entry(id.toString(), entry.baseLevel, List.copyOf(entry.mods)));
        }
        return new HazardSpec(data.doesOverride, data.mutexBits, false, entries);
    }
}
