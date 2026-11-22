package fissionworks.sim;

/**
 * Flux model of a W x H grid of columns, no Minecraft code so it can be tested alone.
 * Fuel emits flux in the four cardinal directions. Flux walks until something takes it:
 * a rod passes (100 - insertion)%, a moderator boosts what reaches fuel, a reflector sends it
 * back once, an absorber eats it, fuel takes it in. Fuel then emits BASE + GAIN * received.
 * Simplified from HBM's RBMK; not a copy of its numbers.
 */
public final class Reactor {
    public enum Kind { EMPTY, FUEL, ROD, MODERATOR, REFLECTOR, ABSORBER }

    public static final int GROUPS = 5;
    public static final double CAP = 100, BASE = 4, GAIN = 1.1, MOD_BOOST = 1.5, REFLECT = 0.8;
    /** Self-limiting: reactivity drops as flux rises (Doppler-like). 0 gives a harsh HBM-style reactor. */
    public static final double DOPPLER = 0.5;
    /** Rod travel per step in percent: normal and AZ-5. */
    public static final int STEP = 1, SCRAM_STEP = 4;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public final int w, h;
    private final Kind[] kind;
    private final int[] level, target, group;
    private final double[] flux;
    private boolean scramming;

    public Reactor(int w, int h) {
        this.w = w;
        this.h = h;
        int n = w * h;
        kind = new Kind[n];
        level = new int[n];
        target = new int[n];
        group = new int[n];
        flux = new double[n];
        java.util.Arrays.fill(kind, Kind.EMPTY);
    }

    private int i(int x, int z) { return z * w + x; }
    private boolean in(int x, int z) { return x >= 0 && z >= 0 && x < w && z < h; }

    public Kind kind(int x, int z) { return kind[i(x, z)]; }
    public int level(int x, int z) { return level[i(x, z)]; }
    public int target(int x, int z) { return target[i(x, z)]; }
    public int group(int x, int z) { return group[i(x, z)]; }
    public double flux(int x, int z) { return flux[i(x, z)]; }
    public boolean scramming() { return scramming; }

    /** Rods start fully inserted, the safe way to begin. */
    public void set(int x, int z, Kind k) {
        int n = i(x, z);
        kind[n] = k;
        level[n] = target[n] = k == Kind.ROD ? 100 : 0;
        flux[n] = 0;
    }

    public void setTarget(int x, int z, int percent) {
        if (kind(x, z) == Kind.ROD) {
            target[i(x, z)] = Math.max(0, Math.min(100, percent));
            scramming = false;
        }
    }

    public void setGroupTarget(int g, int percent) {
        for (int n = 0; n < kind.length; n++) {
            if (kind[n] == Kind.ROD && group[n] == g) {
                target[n] = Math.max(0, Math.min(100, percent));
            }
        }
        scramming = false;
    }

    public void cycleGroup(int x, int z) {
        if (kind(x, z) == Kind.ROD) {
            group[i(x, z)] = (group[i(x, z)] + 1) % GROUPS;
        }
    }

    /** AZ-5: every rod starts moving all the way in, faster than normal. */
    public void scram() {
        scramming = true;
        for (int n = 0; n < kind.length; n++) {
            if (kind[n] == Kind.ROD) {
                target[n] = 100;
            }
        }
    }

    /** One step: rods move, then flux relaxes toward what the received flux allows. */
    public void step() {
        int speed = scramming ? SCRAM_STEP : STEP;
        for (int n = 0; n < kind.length; n++) {
            if (kind[n] == Kind.ROD) {
                int d = target[n] - level[n];
                level[n] += Math.max(-speed, Math.min(speed, d));
            }
        }
        double[] received = new double[kind.length];
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                if (kind(x, z) == Kind.FUEL) {
                    for (int[] d : DIRS) {
                        trace(x, z, d[0], d[1], flux(x, z), received, 0);
                    }
                }
            }
        }
        for (int n = 0; n < kind.length; n++) {
            if (kind[n] == Kind.FUEL) {
                double goal = Math.min(CAP, BASE + GAIN * received[n] * (1 - DOPPLER * flux[n] / CAP));
                flux[n] += (goal - flux[n]) * 0.2;
            }
        }
        if (scramming && allIn()) {
            scramming = false;
        }
    }

    private void trace(int sx, int sz, int dx, int dz, double amount, double[] received, int bounces) {
        int x = sx + dx, z = sz + dz;
        boolean moderated = false;
        while (in(x, z) && amount > 0.01) {
            int n = i(x, z);
            switch (kind[n]) {
                case ROD -> amount *= (100 - level[n]) / 100.0;
                case MODERATOR -> moderated = true;
                case ABSORBER -> { return; }
                case FUEL -> {
                    received[n] += amount * (moderated ? MOD_BOOST : 1);
                    return;
                }
                case REFLECTOR -> {
                    if (bounces == 0) {
                        trace(x, z, -dx, -dz, amount * REFLECT, received, 1);
                    }
                    return;
                }
                default -> { }
            }
            x += dx;
            z += dz;
        }
    }

    private boolean allIn() {
        for (int n = 0; n < kind.length; n++) {
            if (kind[n] == Kind.ROD && level[n] < 100) {
                return false;
            }
        }
        return true;
    }

    /** Average fuel flux as a percent of CAP. */
    public double power() {
        double sum = 0;
        int fuel = 0;
        for (int n = 0; n < kind.length; n++) {
            if (kind[n] == Kind.FUEL) {
                sum += flux[n];
                fuel++;
            }
        }
        return fuel == 0 ? 0 : sum / fuel / CAP * 100;
    }

    /** True when any fuel sits at the cap: the reactor is supercritical without rods to hold it. */
    public boolean runaway() {
        for (int n = 0; n < kind.length; n++) {
            if (kind[n] == Kind.FUEL && flux[n] >= CAP - 1) {
                return true;
            }
        }
        return false;
    }

    public int count(Kind k) {
        int c = 0;
        for (Kind x : kind) {
            if (x == k) {
                c++;
            }
        }
        return c;
    }

    /** Flat form for saving and network: w, h, scramming, then kind, level, target, group, flux*100 per cell. */
    public int[] toInts() {
        int[] out = new int[3 + kind.length * 5];
        out[0] = w;
        out[1] = h;
        out[2] = scramming ? 1 : 0;
        for (int n = 0; n < kind.length; n++) {
            int o = 3 + n * 5;
            out[o] = kind[n].ordinal();
            out[o + 1] = level[n];
            out[o + 2] = target[n];
            out[o + 3] = group[n];
            out[o + 4] = (int) Math.round(flux[n] * 100);
        }
        return out;
    }

    public static Reactor fromInts(int[] d) {
        if (d.length < 3 || d[0] <= 0 || d[1] <= 0 || d.length != 3 + d[0] * d[1] * 5) {
            return null;
        }
        Reactor r = new Reactor(d[0], d[1]);
        r.scramming = d[2] == 1;
        Kind[] kinds = Kind.values();
        for (int n = 0; n < r.kind.length; n++) {
            int o = 3 + n * 5;
            r.kind[n] = kinds[Math.floorMod(d[o], kinds.length)];
            r.level[n] = d[o + 1];
            r.target[n] = d[o + 2];
            r.group[n] = Math.floorMod(d[o + 3], GROUPS);
            r.flux[n] = d[o + 4] / 100.0;
        }
        return r;
    }
}
