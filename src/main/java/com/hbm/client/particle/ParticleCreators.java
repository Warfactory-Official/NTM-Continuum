package com.hbm.client.particle;

import java.util.HashMap;
import java.util.Map;

/**
 * The client's table of particle creators, keyed by the effect name that travels on the wire.
 *
 * <p>Client only. Upstream keeps both a set of named constants and a string map; both are kept here
 * because they serve different callers — HBM code that spawns an effect directly uses the constant,
 * while the packet handler resolves by name.
 */
public final class ParticleCreators {

    private ParticleCreators() {}

    public static final IParticleCreator FLAME = new FlameCreator();
    public static final IParticleCreator EXPLOSION_SMALL = new ExplosionSmallCreator();
    public static final IParticleCreator BLACK_POWDER = new BlackPowderCreator();
    public static final IParticleCreator ASHES = new AshesCreator();
    public static final IParticleCreator TOWER = new CoolingTowerCreator();
    public static final IParticleCreator FLUID_FILL = new FluidFillCreator();

    /** Effect-name to creator. Keys match upstream's strings so old call sites port unchanged. */
    public static final Map<String, IParticleCreator> particleCreators = new HashMap<>();

    static {
        particleCreators.put("flamethrower", FLAME);
        particleCreators.put("explosionSmall", EXPLOSION_SMALL);
        particleCreators.put("blackPowder", BLACK_POWDER);
        particleCreators.put("ashes", ASHES);
        particleCreators.put("tower", TOWER);
        particleCreators.put("fluidfill", FLUID_FILL);
    }
}
