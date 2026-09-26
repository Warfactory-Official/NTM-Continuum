package com.hbm.api.hazard;

public enum HazardClass {
    GAS_LUNG("hazard.gasChlorine"),          // also attacks eyes -> no half mask
    GAS_MONOXIDE("hazard.gasMonoxide"),      // only affects lungs
    GAS_INERT("hazard.gasInert"),            // SA
    PARTICLE_COARSE("hazard.particleCoarse"),// only affects lungs
    PARTICLE_FINE("hazard.particleFine"),    // only affects lungs
    BACTERIA("hazard.bacteria"),             // no half masks
    NERVE_AGENT("hazard.nerveAgent"),        // aggressive nerve agent, also attacks skin, NOT USED UPSTREAM
    GAS_BLISTERING("hazard.corrosive"),      // corrosive substance, also attacks skin
    SAND("hazard.sand"),                     // blinding sand particles
    LIGHT("hazard.light");                   // blinding light

    public final String lang;

    HazardClass(String lang) {
        this.lang = lang;
    }
}
