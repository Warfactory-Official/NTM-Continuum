package com.hbm.api.material;

/** 1.12.2 {@code SmeltingBehavior}, verbatim. */
public enum SmeltingBehavior {
    NOT_SMELTABLE,  // anything that can't be smelted or otherwise doesn't belong in a smelter, like diamond
    VAPORIZES,      // can't be smelted because the material would skadoodle
    BREAKS,         // can't be smelted because the material doesn't survive the temperatures
    SMELTABLE,      // mostly metal
    ADDITIVE        // stuff like coal which isn't smeltable but can be put in a crucible anyway
}
