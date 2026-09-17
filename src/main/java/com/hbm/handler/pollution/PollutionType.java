package com.hbm.handler.pollution;

// Extracted standalone from CE's PollutionHandler for fluid traits.
public enum PollutionType {
    SOOT("trait.ptype.soot"),
    POISON("trait.ptype.poison"),
    HEAVYMETAL("trait.ptype.heavymetal"),
    FALLOUT("trait.ptype.fallout");

    public static final PollutionType[] VALUES = values();

    public final String name;

    PollutionType(String name) {
        this.name = name;
    }
}
