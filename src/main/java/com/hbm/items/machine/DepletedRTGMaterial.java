package com.hbm.items.machine;

/** The six residual materials of the legacy metadata-backed depleted RTG pellet. */
public enum DepletedRTGMaterial {
    BISMUTH("pellet_rtg_depleted_bismuth"),
    MERCURY("pellet_rtg_depleted_mercury"),
    NEPTUNIUM("pellet_rtg_depleted_neptunium"),
    LEAD("pellet_rtg_depleted_lead"),
    ZIRCONIUM("pellet_rtg_depleted_zirconium"),
    NICKEL("pellet_rtg_depleted_nickel");

    public final String id;

    DepletedRTGMaterial(String id) {
        this.id = id;
    }
}
