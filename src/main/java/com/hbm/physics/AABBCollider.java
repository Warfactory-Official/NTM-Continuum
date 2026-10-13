package com.hbm.physics;

/** 1.12.2 {@code physics.AABBCollider}, the static form (a world block box) the bodies collide against. */
public final class AABBCollider extends Collider {

    public final Box box;

    public AABBCollider(Box box) {
        this.box = box;
        this.localCentroid = box.center();
    }

    @Override
    public V3 support(V3 direction) {
        return new V3(direction.x() > 0 ? box.maxX() : box.minX(),
                direction.y() > 0 ? box.maxY() : box.minY(),
                direction.z() > 0 ? box.maxZ() : box.minZ());
    }
}
