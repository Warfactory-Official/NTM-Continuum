package fissionworks.sim;

import fissionworks.sim.Zirnox.Fuel;

/** Plain main-method checks. Run: javac -d out Zirnox.java ZirnoxTest.java, then java -cp out fissionworks.sim.ZirnoxTest */
public final class ZirnoxTest {
    static int failed;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "ok   " : "FAIL ") + what);
        if (!ok) failed++;
    }

    static Zirnox full(Fuel f, int co2) {
        Zirnox z = new Zirnox();
        for (int s = 0; s < Zirnox.SLOTS; s++) z.load(s, f, 0);
        z.fillCo2(co2);
        z.fillWater(Zirnox.WATER_CAP);
        z.setRunning(true);
        return z;
    }

    /** Runs with turbines taking steam and a condenser returning water, unless told otherwise. */
    static Zirnox run(Zirnox z, int ticks, boolean loop) {
        for (int t = 0; t < ticks && !z.melted(); t++) {
            z.step();
            if (loop) {
                z.drainSteam(z.steam());
                z.fillWater(Zirnox.WATER_CAP);
            }
        }
        return z;
    }

    static void windows() {
        Zirnox nat = run(full(Fuel.NATURAL_URANIUM, 14000), 12000, true);
        check(!nat.melted() && nat.temp() > 280 && nat.temp() < 360, "natural uranium at 14000 CO2 is stable, T " + nat.temp());
        Zirnox u = run(full(Fuel.URANIUM, 14000), 12000, true);
        check(!u.melted() && u.temp() < 700 && u.pressure() < 28, "uranium at 14000 CO2 is stable, T " + u.temp() + " P " + u.pressure());
        check(u.steamRate() > nat.steamRate() * 2, "uranium makes over twice the steam of natural, " + u.steamRate());
        check(run(full(Fuel.URANIUM, 4000), 12000, true).melted(), "uranium at 4000 CO2 melts down from heat");
        check(run(full(Fuel.URANIUM, 8000), 12000, true).melted(), "uranium at 8000 CO2 still melts down");
        Zirnox high = run(full(Fuel.URANIUM, 16000), 12000, true);
        check(!high.melted() && high.pressure() > 26 && high.pressure() < 30, "uranium at 16000 CO2 sits near the pressure limit, P " + high.pressure());
    }

    static void operations() {
        check(run(full(Fuel.URANIUM, 14000), 12000, false).melted(), "no water loop: uranium melts when the water is used up");
        Zirnox blocked = full(Fuel.URANIUM, 14000);
        for (int t = 0; t < 12000 && !blocked.melted(); t++) { blocked.step(); blocked.fillWater(Zirnox.WATER_CAP); }
        check(blocked.melted(), "steam never drained: the steam tank fills and the core melts");
        Zirnox cold = new Zirnox();
        cold.load(0, Fuel.URANIUM, 0);
        check(!cold.setRunning(true) && cold.startBlock() == 2, "start refused with no CO2");
        cold.fillCo2(8000);
        check(!cold.setRunning(true) && cold.startBlock() == 3, "start refused with no water");
        cold.fillWater(8000);
        check(cold.setRunning(true), "start allowed once CO2 and water are in");
        Zirnox empty = new Zirnox();
        empty.fillCo2(8000);
        empty.fillWater(8000);
        check(!empty.setRunning(true) && empty.startBlock() == 1, "start refused with no fuel");
        Zirnox v = full(Fuel.NATURAL_URANIUM, 10000);
        run(v, 3000, true);
        double p = v.pressure();
        check(v.vent() == 1000 && v.co2() == 9000 && v.pressure() < p, "vent takes 1000 mB CO2 and lowers pressure");
        Zirnox low = run(full(Fuel.NATURAL_URANIUM, 6000), 3000, true);
        low.vent();
        low.vent();
        low.vent();
        low.step();
        check(low.co2() == 3000 && low.heat() == 0, "below 4000 CO2 the reaction stops");
    }

    static void fuelAndHeat() {
        Zirnox u = run(full(Fuel.URANIUM, 16000), 5000, true);
        check(u.burn(8) > u.burn(0) * 1.2, "centre rod burns faster than a corner rod, " + u.burn(8) + " vs " + u.burn(0));
        Zirnox n = run(full(Fuel.NATURAL_URANIUM, 16000), 5000, true);
        check(Math.abs(n.burn(8) - n.burn(0)) < 1e-9, "natural uranium breeds: no neighbour boost");
        Zirnox one = new Zirnox();
        one.load(0, Fuel.NATURAL_URANIUM, 0);
        one.fillCo2(14000);
        one.fillWater(Zirnox.WATER_CAP);
        one.setRunning(true);
        run(one, 72200, true);
        check(one.fuel(0) == Fuel.SPENT && one.heat() < 0.01, "a rod burns out after its life and then makes no heat");
        Zirnox s = run(full(Fuel.URANIUM, 14000), 6000, true);
        s.setRunning(false);
        s.step();
        double first = s.heat();
        run(s, 3000, true);
        check(first > 1 && s.heat() < first * 0.5, "decay heat after shutdown, " + first + " then " + s.heat());
    }

    static void saving() {
        Zirnox z = run(full(Fuel.THORIUM, 14000), 2000, true);
        Zirnox r = Zirnox.fromInts(z.toInts());
        check(r != null && Math.abs(r.temp() - z.temp()) < 0.01 && r.co2() == z.co2() && r.fuel(5) == Fuel.THORIUM
                && Math.abs(r.burn(5) - z.burn(5)) < 1e-4 && r.running(), "save and load keeps state");
        check(Zirnox.fromInts(new int[3]) == null, "bad data is refused");
        Zirnox melt = run(full(Fuel.URANIUM, 4000), 12000, true);
        check(melt.melted() && !melt.setRunning(true), "a melted reactor cannot restart");
    }

    public static void main(String[] args) {
        windows();
        operations();
        fuelAndHeat();
        saving();
        System.exit(failed == 0 ? 0 : 1);
    }
}
