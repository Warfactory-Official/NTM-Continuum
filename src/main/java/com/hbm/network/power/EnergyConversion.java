package com.hbm.network.power;

import com.hbm.config.GeneralConfig;

/**
 * The ONE HE<->FE exchange, shared by every boundary that crosses it; symmetric, and A:B with a per-boundary {@link Carry} converts exactly in BOTH directions.
 */
public final class EnergyConversion {

    private EnergyConversion() {
    }

    /**
     * The HE side of the ratio: {@code heRatio()} HE is worth {@code feRatio()} FE. At least 1.
     */
    public static long heRatio() {
        return Math.max(1, GeneralConfig.energyRatioHe);
    }

    /**
     * The FE side of the ratio. At least 1.
     */
    public static long feRatio() {
        return Math.max(1, GeneralConfig.energyRatioFe);
    }

    /**
     * Whole FE that he buys with NO carry, rounding down - for advertising only.
     */
    public static long feFromHe(long he) {
        return he <= 0 ? 0 : scale(he, feRatio(), heRatio());
    }

    /**
     * Whole HE that {@code fe} is worth with NO carry, rounding down. Advertising only, as above.
     */
    public static long heFromFe(long fe) {
        return fe <= 0 ? 0 : scale(fe, heRatio(), feRatio());
    }

    /**
     * The smallest FE that converts to a WHOLE HE - for a boundary that cannot carry a remainder (a transactional loader face, where an aborted transaction would leave the carry). No-op at the default 5:1.
     */
    public static long feQuantum() {
        return feQuantum(heRatio(), feRatio());
    }

    /**
     * {@link #feQuantum()} as a pure function, so it can be exercised at ratios no config default reaches.
     */
    static long feQuantum(long heRatio, long feRatio) {
        return feRatio / gcd(heRatio, feRatio);
    }

    private static long gcd(long x, long y) {
        while (y != 0) {
            long t = x % y;
            x = y;
            y = t;
        }
        return x;
    }

    /**
     * {@code value * mul / div}, floored, saturating instead of wrapping.
     */
    private static long scale(long value, long mul, long div) {
        if (value > Long.MAX_VALUE / mul) return Long.MAX_VALUE;
        return value * mul / div;
    }

    /**
     * amount * mul / div with the floored remainder BANKED in carry[0] and spent next call - the exact-ratio arithmetic as a pure function; the banked numerator bounded below div IS round-trip conservation.
     */
    static long convert(long amount, long mul, long div, long[] carry) {
        if (amount <= 0) return 0;
        long banked = carry[0];
        if (amount > (Long.MAX_VALUE - banked) / mul) return Long.MAX_VALUE;
        long numerator = amount * mul + banked;
        carry[0] = numerator % div;
        return numerator / div;
    }

    /**
     * One boundary's sub-unit remainder in BOTH directions - the NUMERATOR each floor discarded, bounded, and a saturated conversion does not carry (a creative source has no exact remainder to bank).
     */
    public static final class Carry {

        private long heToFe;
        private long feToHe;

        /**
         * FE that {@code he} buys, spending and updating the HE&rarr;FE carry.
         */
        public long feFromHe(long he) {
            long[] cell = {heToFe};
            long out = convert(he, feRatio(), heRatio(), cell);
            heToFe = cell[0];
            return out;
        }

        /**
         * HE that {@code fe} is worth, spending and updating the FE&rarr;HE carry.
         */
        public long heFromFe(long fe) {
            long[] cell = {feToHe};
            long out = convert(fe, heRatio(), feRatio(), cell);
            feToHe = cell[0];
            return out;
        }

        /**
         * Un-spend fe the far side REFUSED, and answer the whole HE it was worth - a refusal is not bounded like a floor, so only the sub-unit stays banked (a parked quantity would die with the face cache while the net was debited). Capped at maxHe, the caller's offer remainder.
         */
        public long refundFe(long fe, long maxHe) {
            if (fe <= 0 || maxHe <= 0) return 0;
            long a = heRatio();
            long b = feRatio();
            if (fe > (Long.MAX_VALUE - heToFe) / a) return 0;
            heToFe += fe * a;
            long he = Math.min(heToFe / b, maxHe);
            heToFe -= he * b;
            return he;
        }

        /**
         * The banked HE&rarr;FE numerator remainder.
         */
        public long heToFeRemainder() {
            return heToFe;
        }

        /**
         * The banked FE&rarr;HE numerator remainder.
         */
        public long feToHeRemainder() {
            return feToHe;
        }

        /**
         * Fold another boundary's banks into this one - numerator remainders are exactly additive; an over-full bank materialises a whole unit on its next conversion.
         */
        public void absorb(Carry other) {
            heToFe += other.heToFe;
            feToHe += other.feToHe;
        }

        /**
         * Re-seat both banks from an earlier reading - for a boundary a transaction can roll back; a carry mutated by a conversion whose FE never moved would break the conservation identity.
         */
        public void restore(long heToFe, long feToHe) {
            this.heToFe = heToFe;
            this.feToHe = feToHe;
        }
    }
}
