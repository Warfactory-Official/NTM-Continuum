package com.hbm.client.render;

/** Pure legacy plasma-forge render predicates and clock formulas, kept testable without client bootstrap. */
public final class PlasmaForgeRenderMath {

    public static final float OUTER_PLUME_NARROW = 0.09375F;
    public static final float BEAM_BOTTOM = 1F;
    public static final float BEAM_LENGTH = 1.5F;
    public static final float BEAM_TOP = BEAM_BOTTOM + BEAM_LENGTH;
    public static final float GLOW_COLOR_SCALE = 2F;
    public static final String ITEM_BLACK_PART = "Plasma";

    private PlasmaForgeRenderMath() {}

    /** The old plume appears only while processing and while the jet arm is stationary away from home. */
    public static boolean shouldRenderJetPlume(boolean processing, double angle, double previousAngle) {
        return processing && angle == previousAngle && angle != 0D;
    }

    public static float plasmaAlpha(long time) {
        return 0.5F + (float) (Math.sin(time / 500D) * 0.25D);
    }

    /** The old renderer pulses only the main plasma colour; both glow colours stay at 2x. */
    public static float plasmaColorScale(boolean glow, long time) {
        return glow ? GLOW_COLOR_SCALE : plasmaAlpha(time);
    }

    public static float mainScroll(long time) {
        return (float) (Math.sin(Math.PI / 2D * Math.cos(time / 750D)) % 1D);
    }

    public static float firstGlowScroll(long time) {
        return (float) (Math.sin(time / 1000D) % 1D + time / 10_000D % 1D);
    }

    public static float secondGlowScroll(long time) {
        return (float) (Math.sin(time / 600D + 2D) % 1D + time / 5_000D % 1D);
    }

    public static float itemBob(float ticks, float partialTicks) {
        return (float) (Math.sin((ticks + partialTicks) * 0.1D) * 0.0625D);
    }

    public static float beamScroll(float ticks, float partialTicks) {
        return (ticks + partialTicks) / 15F % 1F;
    }

    public static float beamBottomUv(float offset) {
        return offset + BEAM_LENGTH;
    }

    public static float beamTopUv(float offset) {
        return offset;
    }

    /** The old renderer kept the boundary itself visible; only values greater than the radius were culled. */
    public static boolean within(double distanceSq, double radius) {
        return distanceSq <= radius * radius;
    }
}
