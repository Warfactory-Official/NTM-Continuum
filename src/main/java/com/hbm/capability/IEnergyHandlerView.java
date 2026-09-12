package com.hbm.capability;

/**
 * A loader-agnostic view over a foreign energy handler, in its own unit (FE/RF) and never HE.
 *
 * <p>NeoForge and Forge both answer this from their energy capability. Fabric has no standard one,
 * so the Fabric implementation resolves nothing and every net there sees no foreign peers — which is
 * exactly what Continuum shipped before this existed.
 */
public interface IEnergyHandlerView {

    /**
     * The native handler, for dedup: one physical block exposes its capability on every face it
     * answers on, and a net must not count it once per face.
     */
    Object handlerIdentity();

    long amount();

    /**
     * Capacity, as a hint only — both loaders permit a handler to over- or under-state what an
     * insert will actually accept.
     */
    long capacity();

    /** What this handler would accept right now, simulated. */
    long insertable();

    /** What this handler would give up right now, simulated. */
    long extractable();

    /** @return FE actually accepted, between 0 and {@code maxFe}. */
    long insert(long maxFe);

    /** @return FE actually taken, between 0 and {@code maxFe}. */
    long extract(long maxFe);

    default boolean canInsert() {
        return insertable() > 0;
    }

    default boolean canExtract() {
        return extractable() > 0;
    }
}
