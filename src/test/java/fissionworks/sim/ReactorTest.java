package fissionworks.sim;

import fissionworks.sim.Reactor.Kind;

/** Plain main-method checks. Run: javac -d out the two files, then java -cp out fissionworks.sim.ReactorTest */
public final class ReactorTest {
    static int failed;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "ok   " : "FAIL ") + what);
        if (!ok) failed++;
    }

    static Reactor run(Reactor r, int steps) {
        for (int s = 0; s < steps; s++) r.step();
        return r;
    }

    /** F R F in a row, rod held at the given insertion. */
    static Reactor line(int rodLevel) {
        Reactor r = new Reactor(3, 1);
        r.set(0, 0, Kind.FUEL);
        r.set(1, 0, Kind.ROD);
        r.set(2, 0, Kind.FUEL);
        r.setTarget(1, 0, rodLevel);
        return run(r, 120);
    }

    static Reactor row(int w, int[] fuel, int[] other, Kind kind) {
        Reactor r = new Reactor(w, 1);
        for (int x : fuel) r.set(x, 0, Kind.FUEL);
        for (int x : other) r.set(x, 0, kind);
        return r;
    }

    static void rodWorth() {
        Reactor out = line(0), half = line(50), in = line(100);
        check(out.power() > 20, "rod fully out: pair of fuel makes real power, got " + out.power());
        check(!in.runaway() && in.power() < 6, "rod fully in: power near the base, got " + in.power());
        check(half.power() > in.power() && half.power() < out.power(), "half in sits between, got " + half.power());
        Reactor dense = run(row(5, new int[] {0, 1, 2, 3, 4}, new int[] {}, Kind.EMPTY), 300);
        check(dense.runaway(), "five fuel side by side with no rods run away to the cap");
    }

    static void flux() {
        Reactor lone = run(row(5, new int[] {2}, new int[] {}, Kind.EMPTY), 200);
        check(Math.abs(lone.power() - Reactor.BASE) < 0.5, "lone fuel only makes base flux, got " + lone.power());
        Reactor refl = run(row(5, new int[] {1}, new int[] {3}, Kind.REFLECTOR), 200);
        check(refl.power() > Reactor.BASE + 0.5, "reflector sends flux back, got " + refl.power());
        Reactor abs = run(row(5, new int[] {1, 3}, new int[] {2}, Kind.ABSORBER), 200);
        check(abs.power() < Reactor.BASE + 0.5, "absorber between two fuel keeps them apart, got " + abs.power());
        Reactor mod = run(row(4, new int[] {0, 3}, new int[] {1}, Kind.MODERATOR), 40);
        Reactor bare = run(row(4, new int[] {0, 3}, new int[] {}, Kind.EMPTY), 40);
        check(mod.power() > bare.power(), "moderator raises reactivity, " + mod.power() + " > " + bare.power());
    }

    static void rodMotion() {
        Reactor r = line(100);
        r.setTarget(1, 0, 0);
        run(r, 100);
        check(r.level(1, 0) == 0, "rod reaches target 0 after 100 steps at 1 percent per step");
        r.scram();
        run(r, 10);
        check(r.level(1, 0) == 40, "scram moves 4 percent per step, got " + r.level(1, 0));
        run(r, 25);
        check(r.level(1, 0) == 100 && !r.scramming(), "scram ends with rod fully in, flag cleared");
        run(r, 150);
        check(!r.runaway() && r.power() < 6, "quiet again after scram, got " + r.power());
        Reactor g = new Reactor(3, 1);
        g.set(1, 0, Kind.ROD);
        g.cycleGroup(1, 0);
        g.setGroupTarget(1, 70);
        run(g, 80);
        check(g.group(1, 0) == 1 && g.level(1, 0) == 70, "group target moves rods in that group only");
    }

    static void saving() {
        Reactor rt = Reactor.fromInts(line(30).toInts());
        check(rt != null && rt.level(1, 0) == 30 && rt.kind(0, 0) == Kind.FUEL, "save and load keeps rod and fuel");
        check(Reactor.fromInts(new int[] {3, 1, 0}) == null, "bad data is refused");
    }

    static void starterHall() {
        Reactor hall = new Reactor(5, 5);
        for (int[] p : new int[][] {{1, 1}, {3, 1}, {1, 3}, {3, 3}, {2, 2}}) hall.set(p[0], p[1], Kind.FUEL);
        for (int[] p : new int[][] {{2, 1}, {1, 2}, {3, 2}, {2, 3}}) hall.set(p[0], p[1], Kind.ROD);
        run(hall, 200);
        check(hall.power() < 6, "starter hall with rods in is quiet, got " + hall.power());
        hall.setGroupTarget(0, 40);
        run(hall, 300);
        check(hall.power() > 30 && !hall.runaway(), "starter hall at 40 percent runs at real power, got " + hall.power());
        hall.setGroupTarget(0, 0);
        run(hall, 400);
        check(hall.runaway(), "starter hall with rods fully out runs away");
        hall.scram();
        run(hall, 120);
        check(hall.power() < 6 && !hall.scramming(), "AZ-5 brings the runaway hall back, got " + hall.power());
    }

    public static void main(String[] args) {
        rodWorth();
        flux();
        rodMotion();
        saving();
        starterHall();
        System.exit(failed == 0 ? 0 : 1);
    }
}
