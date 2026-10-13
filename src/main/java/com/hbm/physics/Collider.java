package com.hbm.physics;

/** 1.12.2 {@code physics.Collider}: a convex shape in body space with its mass properties. */
public abstract class Collider {

    public float mass;
    public Mat3 localInertiaTensor;
    public V3 localCentroid;

    /** The farthest point of the shape along {@code direction}. */
    public abstract V3 support(V3 direction);
}
