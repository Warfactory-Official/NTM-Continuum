package com.hbm.uninos.graph;

/**
 * Per-node edge-face register: the node's open faces aimed at node-less positions - the CANDIDATE set a distributor may probe; maintained incrementally, derived lazily per net.
 */
public final class EdgeFaces {

    // 1.7: buffer barrel endpoint at member
    public static final int SELF = 6;
    public final Object[] attachments = new Object[7];
    // bit per Direction set = node-less aim; SELF = selfEndpoint
    public int mask;
    /**
     * Faces the distributor has evaluated since they last changed; the kernel only ever CLEARS bits here - "is there an endpoint" is a capability question.
     */
    public int probedMask;
    /**
     * Faces a probe actually found an endpoint at - what the gather iterates, so per-tick cost tracks endpoints, not conductors. Staleness is one-directional: a stale SET bit is harmless, a stale CLEAR bit is a machine silently never served.
     */
    public int activeMask;
    /**
     * Which endpoint ROLE the probe found behind each face - deletes half the gather's work, one of the two per-tick lookups being guaranteed null for a single-role machine. Only meaningful where probedMask is set; the gather MUST test that first, or an invalidation clears the mask and the face gathers against stale roles for one tick.
     */
    public int roleA;
    public int roleB;
}
