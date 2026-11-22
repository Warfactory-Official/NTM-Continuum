package fissionworks.sim;

import java.util.Arrays;

/**
 * A fixed gas-cooled reactor, ZIRNOX style, with no Minecraft code so it can be tested alone.
 * 24 fuel slots, CO2 loop, water boiled to steam, temperature, pressure, decay heat and burnup.
 * Called once per game tick. Numbers are my own, tuned so the safe CO2 window is narrow, not HBM's.
 */
public final class Zirnox {
    public enum Fuel { NONE, NATURAL_URANIUM, URANIUM, THORIUM, SPENT }

    public static final int COLS = 6, ROWS = 4, SLOTS = COLS * ROWS;
    public static final int CO2_CAP = 16000, WATER_CAP = 32000, STEAM_CAP = 8000;
    public static final int MIN_CO2 = 4000, MIN_WATER = 4000, VENT = 1000;
    public static final double MELT_TEMP = 800, MELT_PRESSURE = 30, AMBIENT = 20;

    /** Heat per tick at neighbour factor 1, and burnup life in ticks, per fuel (index = ordinal). */
    private static final double[] POWER = {0, 1.0, 1.6, 1.3, 0};
    private static final double[] LIFE = {1, 72000, 36000, 54000, 1};
    private static final double NEIGHBOUR = 0.15, THERMAL_MASS = 400, K_COOL = 0.0078, DRY = 0.15;
    private static final double PRESSURE_K = 0.64, GAS_TEMP_FLOOR = 120, STEAM_K = 1.0, LOSS = 0.002;
    private static final double DECAY_SHARE = 0.07, DECAY_KEEP = 0.9985;

    private final Fuel[] fuel = new Fuel[SLOTS];
    private final double[] burn = new double[SLOTS];
    private int co2, water, steam;
    private double temp = AMBIENT, decay, heat, steamRate;
    private boolean running, melted;

    public Zirnox() {
        Arrays.fill(fuel, Fuel.NONE);
    }

    public Fuel fuel(int slot) { return fuel[slot]; }
    public double burn(int slot) { return burn[slot]; }
    public int co2() { return co2; }
    public int water() { return water; }
    public int steam() { return steam; }
    public double temp() { return temp; }
    public boolean running() { return running; }
    public boolean melted() { return melted; }
    /** Heat made last tick, and steam made last tick in mB. */
    public double heat() { return heat; }
    public double steamRate() { return steamRate; }

    public double pressure() {
        return PRESSURE_K * (co2 / 1000.0) * (temp + 273.15) / 293.15;
    }

    public boolean load(int slot, Fuel f, double burned) {
        if (slot < 0 || slot >= SLOTS || fuel[slot] != Fuel.NONE || f == Fuel.NONE) {
            return false;
        }
        fuel[slot] = f;
        burn[slot] = f == Fuel.SPENT ? 1 : Math.max(0, Math.min(0.999, burned));
        return true;
    }

    /** Takes the rod out of a slot; returns what was there. */
    public Fuel unload(int slot) {
        if (slot < 0 || slot >= SLOTS) {
            return Fuel.NONE;
        }
        Fuel f = fuel[slot];
        fuel[slot] = Fuel.NONE;
        return f;
    }

    public int fillWater(int amount) {
        int n = Math.max(0, Math.min(amount, WATER_CAP - water));
        water += n;
        return n;
    }

    public int fillCo2(int amount) {
        int n = Math.max(0, Math.min(amount, CO2_CAP - co2));
        co2 += n;
        return n;
    }

    public int drainSteam(int amount) {
        int n = Math.max(0, Math.min(amount, steam));
        steam -= n;
        return n;
    }

    public int vent() {
        int n = Math.min(VENT, co2);
        co2 -= n;
        return n;
    }

    /** Why the reactor will not start: 0 fine, 1 no fuel, 2 low CO2, 3 low water, 4 steam tank full. */
    public int startBlock() {
        if (!hasFuel()) return 1;
        if (co2 < MIN_CO2) return 2;
        if (water < MIN_WATER) return 3;
        return steam >= STEAM_CAP ? 4 : 0;
    }

    public boolean setRunning(boolean on) {
        if (on && (melted || startBlock() != 0)) {
            return false;
        }
        running = on;
        return true;
    }

    private boolean hasFuel() {
        for (Fuel f : fuel) {
            if (f != Fuel.NONE && f != Fuel.SPENT) {
                return true;
            }
        }
        return false;
    }

    private boolean active(int slot) {
        return fuel[slot] != Fuel.NONE && fuel[slot] != Fuel.SPENT;
    }

    /** More live neighbours make a rod react faster, except natural uranium, which breeds. */
    private double factor(int slot) {
        if (fuel[slot] == Fuel.NATURAL_URANIUM) {
            return 1;
        }
        int x = slot % COLS, y = slot / COLS, n = 0;
        if (x > 0 && active(slot - 1)) n++;
        if (x < COLS - 1 && active(slot + 1)) n++;
        if (y > 0 && active(slot - COLS)) n++;
        if (y < ROWS - 1 && active(slot + COLS)) n++;
        return 1 + NEIGHBOUR * n;
    }

    private double burnTick() {
        double q = 0;
        for (int s = 0; s < SLOTS; s++) {
            if (active(s)) {
                double f = factor(s);
                q += POWER[fuel[s].ordinal()] * f;
                burn[s] += f / LIFE[fuel[s].ordinal()];
                if (burn[s] >= 1) {
                    fuel[s] = Fuel.SPENT;
                    burn[s] = 1;
                }
            }
        }
        return q;
    }

    /** One game tick. */
    public void step() {
        if (melted) {
            return;
        }
        double q = 0;
        if (running && co2 >= MIN_CO2) {
            q = burnTick();
        }
        decay = running ? DECAY_SHARE * q : decay * DECAY_KEEP;
        heat = running ? q : decay;
        boolean wet = water >= 1 && steam < STEAM_CAP;
        double removal = (wet ? K_COOL : K_COOL * DRY) * (co2 / 1000.0) * Math.max(0, temp - GAS_TEMP_FLOOR);
        steamRate = 0;
        if (wet) {
            double eta = Math.max(0, (temp - 50) / (temp + 273.15));
            steamRate = Math.min(Math.min(removal * eta * STEAM_K, water), STEAM_CAP - steam);
            water -= (int) Math.round(steamRate);
            steam += (int) Math.round(steamRate);
        }
        temp += (heat - removal - LOSS * (temp - AMBIENT)) / THERMAL_MASS;
        temp = Math.max(AMBIENT, temp);
        if (temp >= MELT_TEMP || pressure() >= MELT_PRESSURE) {
            melted = true;
            running = false;
        }
    }

    /** Flat form for saving and network. */
    public int[] toInts() {
        int[] out = new int[8 + SLOTS * 2];
        out[0] = (int) Math.round(temp * 100);
        out[1] = (int) Math.round(decay * 1000);
        out[2] = co2;
        out[3] = water;
        out[4] = steam;
        out[5] = (running ? 1 : 0) | (melted ? 2 : 0);
        out[6] = (int) Math.round(heat * 100);
        out[7] = (int) Math.round(steamRate * 100);
        for (int s = 0; s < SLOTS; s++) {
            out[8 + s * 2] = fuel[s].ordinal();
            out[9 + s * 2] = (int) Math.round(burn[s] * 100000);
        }
        return out;
    }

    public static Zirnox fromInts(int[] d) {
        if (d.length != 8 + SLOTS * 2) {
            return null;
        }
        Zirnox z = new Zirnox();
        z.temp = d[0] / 100.0;
        z.decay = d[1] / 1000.0;
        z.co2 = Math.max(0, Math.min(CO2_CAP, d[2]));
        z.water = Math.max(0, Math.min(WATER_CAP, d[3]));
        z.steam = Math.max(0, Math.min(STEAM_CAP, d[4]));
        z.running = (d[5] & 1) != 0;
        z.melted = (d[5] & 2) != 0;
        z.heat = d[6] / 100.0;
        z.steamRate = d[7] / 100.0;
        Fuel[] all = Fuel.values();
        for (int s = 0; s < SLOTS; s++) {
            z.fuel[s] = all[Math.floorMod(d[8 + s * 2], all.length)];
            z.burn[s] = d[9 + s * 2] / 100000.0;
        }
        return z;
    }
}
