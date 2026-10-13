package com.hbm.hazard;

import java.util.ArrayList;
import java.util.List;

import com.hbm.config.RadiationConfig;
import com.hbm.hazard.type.HazardType;
import com.hbm.hazard.type.IHazardType;
import org.jspecify.annotations.Nullable;

/** A set of {@link HazardEntry} with override/mutex semantics, ported 1:1 from 1.12.2. */
public class HazardData {

    /** Purges all fetched data when read, for items that fully override tag data. */
    boolean doesOverride = false;

    /**
     * MUTEX: makes specific entries mutually exclusive (e.g. tag aliases). If a previous entry
     * collides with this one, this one yields.
     * RESERVED BITS: 1 = material tags ("c:ingots/x").
     */
    int mutexBits = 0;

    public List<HazardEntry> entries = new ArrayList<>();

    public HazardData addEntry(final IHazardType hazard) {
        return this.addEntry(hazard, 1D, false);
    }

    public HazardData addEntry(final IHazardType hazard, final double level) {
        if (gated(hazard)) return this;
        return this.addEntry(hazard, level, false);
    }

    public HazardData addEntry(final IHazardType hazard, final double level, final boolean override) {
        this.entries.add(new HazardEntry(hazard, level));
        this.doesOverride = override;
        return this;
    }

    /** Registers {@code hazard} with the payload its lambdas will be handed on every application. */
    public <P> HazardData addEntry(final HazardType<P> hazard, final double level, final @Nullable P payload) {
        if (gated(hazard)) return this;
        return this.addEntry(hazard, level, payload, false);
    }

    public <P> HazardData addEntry(final HazardType<P> hazard, final double level, final @Nullable P payload,
            final boolean override) {
        this.entries.add(hazard.entry(level, payload));
        this.doesOverride = override;
        return this;
    }

    public HazardData addEntry(final HazardEntry entry) {
        this.entries.add(entry);
        return this;
    }

    /** The fork's one registration-time gate, kept on exactly the overloads that carried it. */
    private static boolean gated(final IHazardType hazard) {
        return hazard == HazardRegistry.CONTAMINATING && !RadiationConfig.enableContaminationOnGround;
    }

    public HazardData setMutex(final int mutex) {
        this.mutexBits = mutex;
        return this;
    }

    public HazardData setOverride(final boolean override) {
        this.doesOverride = override;
        return this;
    }

    public int getMutex() {
        return mutexBits;
    }
}
