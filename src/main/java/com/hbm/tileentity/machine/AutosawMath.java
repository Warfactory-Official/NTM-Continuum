package com.hbm.tileentity.machine;

/** Pure autosaw geometry helpers kept separate so the detector's exact legacy window is testable. */
public final class AutosawMath {

    private static final int MIN_DIST = 2;
    private static final int MAX_DIST = 9;
    private static final double CUT_ANGLE = Math.toRadians(5);

    private AutosawMath() {}

    /** The old detector's exclusive-inner/inclusive-outer annulus and wrapped five-degree cut window. */
    public static boolean isInCutWindow(int dx, int dz, float rotationYaw) {
        int squaredDistance = dx * dx + dz * dz;
        if (squaredDistance <= MIN_DIST * MIN_DIST || squaredDistance > MAX_DIST * MAX_DIST) return false;

        double angle = Math.atan2(dz, dx);
        double sawAngle = Math.toRadians((rotationYaw + 270) % 360);
        double relative = Math.abs(angle - sawAngle);
        relative = Math.abs((relative + Math.PI) % (2 * Math.PI) - Math.PI);
        return relative <= CUT_ANGLE;
    }
}
