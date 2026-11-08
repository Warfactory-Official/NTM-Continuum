import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 147: looking at an RBMK column shows 1.7's DODD readout, and a fuel rod in hand goes straight into an
 * empty fuel channel without opening its GUI. Places a fuel channel, a boiler and a control rod by hand, reads the look
 * overlay before and after a rod is clicked in, then clicks a second rod into the full channel, which opens the GUI.
 */
public final class Gh147RbmkDodd implements Scenario {

    private static final int X = 1520;
    private static final int Y = 100;
    private static final int Z = 1520;
    private static final int L = Y + 1;

    private final List<String> fails = new ArrayList<>();

    private void check(boolean ok, String message) {
        if (!ok) fails.add(message);
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("time set 6000");
        c.markLog();
        Fixtures.placeOnTop(c, "hbm:rbmk_fuel_rod", X, Y, Z);
        Fixtures.placeOnTop(c, "hbm:rbmk_boiler", X + 3, Y, Z);
        Fixtures.placeOnTop(c, "hbm:rbmk_control", X - 3, Y, Z);

        aim(c, X, Z);
        List<String> empty = look(c);
        c.screenshot("01-dodd-empty");
        check(empty.contains("Has Rod: 0") && empty.contains("Column Heat: 20.0") && empty.contains("ReaSim Water: 0"),
                "empty fuel channel DODD: " + empty);

        Fixtures.giveOnly(c, "hbm:rbmk_fuel_ueu", 1);
        c.cmd("gamemode survival");
        String capture = c.setOption("sound_capture", "true");
        c.markLog();
        aim(c, X, Z);
        c.use(2);
        c.waitTicks(10);
        List<String> plug = c.logSinceMark("harness-sound.*upgrade_plug");
        if (capture != null) c.setOption("sound_capture", capture);
        String opened = c.screenClass();
        check(opened == null, "a rod clicked into an empty channel must not open the GUI, got " + opened);
        if (opened != null) {
            c.screenshot("02a-gui-opened");
            c.key("escape");
            c.waitTicks(5);
        }
        String nbt = c.blockEntity(X, L, Z).toString();
        check(nbt.contains("rbmk_fuel_ueu"), "the rod must sit in the channel: " + nbt);
        check(c.player().getAsJsonObject("main_hand").get("empty").getAsBoolean(), "the rod must leave a survival hand");
        c.cmd("gamemode creative");
        check(plug.size() == 1, "the insert plays 1.7's upgrade plug sound once: " + plug);
        List<String> fuelled = look(c);
        c.screenshot("02-dodd-rod");
        check(fuelled.contains("Has Rod: 1") && !fuelled.isEmpty() && fuelled.get(0).startsWith("Fuel Heat: \"20.000000 / 20.000000 / 2865.00")
                        && fuelled.contains("Fuel Xenon: \"0.0%\"") && fuelled.contains("Fuel Yield: \"1.0E8 / 1.0E8 (100.0%)\""),
                "fuelled channel DODD: " + fuelled);

        Fixtures.giveOnly(c, "hbm:rbmk_fuel_meu", 1);
        aim(c, X, Z);
        c.use(2);
        c.waitTicks(10);
        String screen = c.screenClass();
        c.screenshot("03-second-rod");
        check(screen != null, "a rod clicked into a full channel opens the GUI as in 1.7");
        c.key("escape");
        c.waitTicks(5);

        aim(c, X + 3, Z);
        List<String> boiler = look(c);
        c.screenshot("04-dodd-boiler");
        check(boiler.contains("Feed Max Amount: 10000") && boiler.contains("Steam Pressure: 0"), "boiler DODD: " + boiler);

        aim(c, X - 3, Z);
        List<String> control = look(c);
        c.screenshot("05-dodd-control");
        check(control.contains("Level: 0.0") && control.contains("Target Level: 0.0"), "control rod DODD: " + control);

        List<String> bad = new ArrayList<>(c.logSinceMark("/(ERROR|FATAL)[]]"));
        check(bad.isEmpty(), "log clean since the mark: " + bad);
        if (c.screenClass() != null) c.key("escape");
        Expect.that(fails.isEmpty(), "issue 147: " + fails);
    }

    /** Stands three blocks south of the column at (x, z) and looks at its middle. */
    private static void aim(Client c, int x, int z) {
        c.hotbar(1);
        c.teleport(x + 0.5, L, z - 2.5);
        c.waitTicks(2);
        c.lookAt(x + 0.5, L + 2.5, z + 0.5);
        Fixtures.waitForTarget(c, x, L + 2, z);
        c.waitTicks(5);
    }

    /** The DODD lines of the aimed column; fails unless the overlay is the DODD. */
    private List<String> look(Client c) {
        JsonObject look = c.query("hbm_small", obj("mode", "look"));
        System.out.println("[Gh147] look " + look);
        List<String> lines = new ArrayList<>();
        if (!look.get("showing").getAsBoolean() || !look.get("diagnostic").getAsBoolean()) {
            fails.add("a DODD must show: " + look);
            return lines;
        }
        JsonArray array = look.getAsJsonArray("lines");
        array.forEach(e -> lines.add(e.getAsString()));
        return lines;
    }

    private static JsonObject obj(String key, String value) {
        JsonObject json = new JsonObject();
        json.addProperty(key, value);
        return json;
    }
}
