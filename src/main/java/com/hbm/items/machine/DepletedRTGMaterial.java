package com.hbm.items.machine;

/** The six residual materials of the legacy metadata-backed depleted RTG pellet. */
public enum DepletedRTGMaterial {
    BISMUTH("pellet_rtg_depleted_bismuth", "Decayed Bismuth RTG Pellet"),
    MERCURY("pellet_rtg_depleted_mercury", "Decayed Mercury RTG Pellet"),
    NEPTUNIUM("pellet_rtg_depleted_neptunium", "Decayed Neptunium RTG Pellet"),
    LEAD("pellet_rtg_depleted_lead", "Decayed Lead RTG Pellet"),
    ZIRCONIUM("pellet_rtg_depleted_zirconium", "Decayed Zirconium RTG Pellet"),
    NICKEL("pellet_rtg_depleted_nickel", "Decayed Nickel RTG Pellet");

    public final String id;
    public final String displayName;

    DepletedRTGMaterial(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
}
