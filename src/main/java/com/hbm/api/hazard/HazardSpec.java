package com.hbm.api.hazard;

import java.util.ArrayList;
import java.util.List;

/** The hazards an item, tag or block carries. {@link #override()}: replaces tag-derived hazards of the same item. */
public final class HazardSpec {

    private final List<HazardEntry> entries = new ArrayList<>();
    private boolean override;

    private HazardSpec() {}

    public static HazardSpec of() {
        return new HazardSpec();
    }

    public HazardSpec add(IHazardType type, double level) {
        entries.add(new HazardEntry(type, level));
        return this;
    }

    /** An entry with modifiers or a payload ({@link HazardType#entry}). */
    public HazardSpec add(HazardEntry entry) {
        entries.add(entry);
        return this;
    }

    public HazardSpec override() {
        override = true;
        return this;
    }

    public List<HazardEntry> entries() {
        return List.copyOf(entries);
    }

    public boolean overrides() {
        return override;
    }
}
