package com.hbm.physics;

import java.util.List;

/**
 * Where a body finds the world's solid boxes: 1.12 {@code world.getCollisionBoxes(null, box)} (blocks only, since no
 * entity is passed). The client supplies it from its level; a test can supply a floor.
 */
@FunctionalInterface
public interface CollisionSource {
    List<Box> collisionBoxes(Box area);
}
