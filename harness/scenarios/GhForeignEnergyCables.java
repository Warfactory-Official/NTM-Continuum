import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Another mod's power cable between two HBM machines: a charged diesel generator, the foreign cable, an electric furnace.
 * The cable must draw its arms to both and carry the generator's power into the furnace. The cable is Mekanism's
 * universal cable where Mekanism is loaded (1.21.1 NeoForge with -Pmekanism.run), else Pipez's energy pipe (the
 * forge-like nodes). Fabric has no foreign cable mod on disk: there the furnace and generator faces are asked through
 * Team Reborn Energy, and a Team Reborn store beside the generator must fill.
 */
public final class GhForeignEnergyCables implements Scenario {

    private static final int Y = 100, G = Y + 1, Z = 2;
    private static final Pattern POWER = Pattern.compile("(?:^|[,{])power(?:Time)?:(\\d+)L");
    private final List<String> failures = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 8);
        c.cmd("gamemode creative");
        c.cmd("fill -6 " + G + " -6 6 " + (G + 3) + " 6 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("clear @s");
        c.waitTicks(5);

        String cable = cable(c);
        System.out.println("[Foreign] cable on " + c.node() + ": " + cable);
        // the cable is laid by hand first; the machines then arrive beside it
        if (cable != null) Fixtures.placeOnTop(c, cable, 0, Y, Z);
        else Fixtures.setBlock(c, 0, G, Z, "hbm:test_fe_cell");
        Fixtures.setBlock(c, -1, G, Z, "hbm:machine_diesel");
        Fixtures.setBlock(c, 1, G, Z, "hbm:machine_electric_furnace_off");
        c.cmd("data merge block -1 " + G + " " + Z + " {powerTime:50000L}");
        // a Pipez pipe carries nothing until one end is wrenched to extract: the generator end, as a player sets it
        if ("pipez:energy_pipe".equals(cable)) {
            Fixtures.giveOnly(c, "pipez:wrench", 1);
            // straight down onto the pipe's west arm
            c.teleport(0.15, G + 2.5, Z + 0.5);
            c.look(0, 90);
            if (!arms(c).contains("west=true")) {
                check(false, "the pipe draws no arm toward the generator, so there is no end to wrench: " + arms(c));
                c.screenshot("foreign-01-placed");
                Expect.that(false, "checks failed: " + failures);
            }
            Fixtures.waitForTarget(c, 0, G, Z);
            // Pipez answers the wrench only while sneaking
            c.keyDown("key.sneak");
            c.waitTicks(3);
            c.use(1);
            c.waitTicks(5);
            c.keyUp("key.sneak");
            c.waitTicks(5);
            c.screenshot("foreign-00-wrench");
            System.out.println("[Foreign] pipe after the wrench: " + c.block(0, G, Z));
        }
        c.teleport(0.5, G + 1.2, Z - 2.5);
        c.lookAt(0.5, G + 0.5, Z + 0.5);
        c.waitTicks(10);
        c.screenshot("foreign-01-placed");

        check(exposes(c, 1, "west"), "the furnace exposes energy on its west face");
        check(exposes(c, -1, "east"), "the generator exposes energy on its east face");
        c.waitServerTicks(100);
        c.screenshot("foreign-02-after-100-ticks");

        long generator = power(c, -1), furnace = power(c, 1);
        System.out.println("[Foreign] " + c.node() + " generator " + generator + " furnace " + furnace);
        check(generator < 50_000L, "the generator gave power into the foreign block: " + generator);
        if (cable != null) {
            check(furnace > 0L, "the foreign cable fed the electric furnace: " + furnace);
            // Mekanism keeps its connections in the block entity and draws them; Pipez states them in the block
            if (cable.startsWith("pipez")) check(arms(c).contains("east=true") && arms(c).contains("west=true"),
                    "the pipe connects to both machines: " + arms(c));
        } else {
            check(stored(c, 0) > 0L, "the Team Reborn store beside the generator filled: " + stored(c, 0));
        }
        Expect.logCleanSinceMark(c, "(?i)exception.*(hbm|power|energy|cable|pipe|mekanism)");
        Expect.that(failures.isEmpty(), "checks failed: " + failures);
    }

    /** Mekanism's basic universal cable if it is registered, else Pipez's energy pipe, else none (Fabric). */
    private static String cable(Client c) {
        for (String id : new String[]{"mekanism:basic_universal_cable", "pipez:energy_pipe"}) {
            Fixtures.setBlock(c, 0, G + 3, 0, id);
            boolean there = c.block(0, G + 3, 0).toString().contains(id);
            Fixtures.setBlock(c, 0, G + 3, 0, "minecraft:air");
            if (there) return id;
        }
        return null;
    }

    private String arms(Client c) {
        return c.block(0, G, Z).toString();
    }

    private static long power(Client c, int x) {
        JsonObject be = c.blockEntity(x, G, Z);
        String nbt = be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
        Matcher m = POWER.matcher(nbt);
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }

    private static boolean exposes(Client c, int x, String side) {
        return fe(c, "exposes", x, side).get("exposes").getAsBoolean();
    }

    private static long stored(Client c, int x) {
        return fe(c, "stored", x, null).get("stored").getAsLong();
    }

    private static JsonObject fe(Client c, String op, int x, String side) {
        JsonObject p = new JsonObject();
        p.addProperty("op", op);
        p.addProperty("x", x);
        p.addProperty("y", G);
        p.addProperty("z", Z);
        if (side != null) p.addProperty("side", side);
        return c.query("hbm_fecell", p);
    }

    private void check(boolean ok, String what) {
        if (!ok) failures.add(what);
        System.out.println((ok ? "ok   " : "FAIL ") + what);
    }
}
