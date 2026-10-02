package com.hbm.physics;

/** 1.12 {@code AxisAlignedBB} as the physics code used it. {@link #intersects} is strict, as both 1.12 and modern are. */
public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

    public static Box of(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public boolean intersects(Box o) {
        return minX < o.maxX && maxX > o.minX && minY < o.maxY && maxY > o.minY && minZ < o.maxZ && maxZ > o.minZ;
    }

    public V3 center() {
        return new V3(minX + (maxX - minX) * 0.5D, minY + (maxY - minY) * 0.5D, minZ + (maxZ - minZ) * 0.5D);
    }
}
