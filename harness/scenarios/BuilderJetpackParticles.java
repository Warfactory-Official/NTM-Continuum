import dev.golem.harness.api.Client;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** The Builder's Jetpack hovering over HBM building blocks, looking down at its exhaust (Discord: black/violet particles). */
public final class BuilderJetpackParticles implements Scenario {

    private static final int X = 360;
    private static final int Y = 100;
    private static final int Z = 360;
    private static final String[] FLOORS = {"hbm:brick_concrete", "hbm:concrete_smooth", "hbm:reinforced_brick",
            "hbm:brick_light", "hbm:concrete_colored_ext", "minecraft:stone"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("weather clear");
        c.cmd("time set 6000");
        String jetpack = c.node().startsWith("1.20") ? "hbm:jetpack_break{\"hbm:jetpack_fuel\":1200}"
                : "hbm:jetpack_break[hbm:jetpack_fuel=1200]";
        try {
            for (String floor : FLOORS) {
                c.cmd("fill " + (X - 4) + " " + Y + " " + (Z - 4) + " " + (X + 4) + " " + Y + " " + (Z + 4) + " " + floor);
                c.cmd("gamemode survival");
                c.cmd("item replace entity @s armor.chest with " + jetpack);
                c.teleport(X + 0.5, Y + 4, Z + 0.5);
                c.setOption("camera", "third_person_back");
                c.look(0, 60);
                c.waitTicks(30);
                c.screenshot(floor.replaceAll("[^a-z_]", "_") + "-hover");
            }
        } finally {
            c.cmd("gamemode creative");
            c.setOption("camera", "first_person");
            c.cmd("clear");
        }
    }
}
