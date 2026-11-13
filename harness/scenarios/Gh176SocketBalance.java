import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 176. Two battery sockets placed by hand, joined by red copper cable, a full and an empty lead-acid battery
 * clicked in, both set to input/output ("balance") mode with the GUI's mode button. 1.7 {@code PowerNetMK2} moves charge
 * only while one socket's priority is higher: at the same priority the empty one stops at 10,000 HE (the full one's
 * headroom then equals its charge rate), with the empty one at High it takes 1,000 HE a tick.
 */
public final class Gh176SocketBalance implements Scenario {

    private static final int Y = 100, G = Y + 1;
    private static final int A = 0, B = 6;
    private static final Pattern CHARGE = Pattern.compile("battery_charge\"?:(\\d+)L");

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 2, 10);
        c.cmd("gamemode creative");
        c.cmd("fill -6 " + G + " -6 6 " + (G + 3) + " 12 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("clear @s");
        c.waitTicks(5);

        Fixtures.placeOnTop(c, "hbm:machine_battery_socket", 0, Y, A);
        Fixtures.placeOnTop(c, "hbm:machine_battery_socket", 0, Y, B);
        c.cmd("fill -2 " + G + " " + (A + 1) + " 2 " + G + " " + (B - 1) + " hbm:red_cable replace minecraft:air");
        c.waitTicks(10);

        setUp(c, A, true, 0);
        setUp(c, B, false, 0);
        c.teleport(7.5, G + 3, 3.5);
        c.lookAt(0.5, G + 0.5, 3.5);
        c.waitTicks(10);
        c.screenshot("gh176-world");

        long b0 = charge(c, B);
        c.waitServerTicks(200);
        long a1 = charge(c, A), b1 = charge(c, B);
        System.out.println("[Gh176] same priority: full " + a1 + " empty " + b1 + " (empty started at " + b0 + ")");
        Expect.that(a1 + b1 == 18_000_000L, "same priority: charge was not conserved: " + a1 + " + " + b1);
        Expect.that(b1 > 9_000L && b1 <= 10_000L, "same priority: the empty socket holds " + b1 + " HE, 1.7 stops at 10,000");
        gui(c, B, "gh176-same-priority-empty");

        // two clicks on the priority button: Low, Normal, High
        setUp(c, B, false, 2);
        long t0 = gameTime(c);
        long b2 = charge(c, B);
        c.waitServerTicks(100);
        long b3 = charge(c, B);
        long ticks = gameTime(c) - t0;
        System.out.println("[Gh176] empty at High: " + b2 + " then " + b3 + " within " + ticks + " ticks");
        // the reads sit inside [t0, t0 + ticks], so the gain is at most 1,000 a tick of that window
        Expect.that(b3 - b2 <= 1_000L * ticks && b3 - b2 >= 850L * ticks,
                "the high-priority socket took " + (b3 - b2) + " HE in at most " + ticks + " ticks, 1.7 moves 1,000 a tick");
        gui(c, B, "gh176-high-priority-empty");
        Expect.logCleanSinceMark(c, "(?i)exception.*(hbm|power|battery|socket)");
    }

    /** Opens the socket at z, puts a lead-acid battery in by hand, clicks the low-signal mode to input/output. */
    private static void setUp(Client c, int z, boolean full, int priorityClicks) {
        boolean insert = priorityClicks == 0;
        if (insert) {
            String battery = c.node().startsWith("1.20.1")
                    ? "hbm:battery_lead{\"hbm:battery_charge\":" + (full ? 18_000_000L : 0L) + "L}"
                    : "hbm:battery_lead[hbm:battery_charge=" + (full ? 18_000_000L : 0L) + "L]";
            c.cmd("give @s " + battery);
            c.waitTicks(5);
        }
        c.teleport(3.5, G, z + 0.5);
        Fixtures.interact(c, 0, G, z);
        c.waitScreen("Battery Socket", 5_000L);
        if (insert) {
            c.clickSlot(slotHolding(c, "hbm:battery_lead"));
            c.clickSlot(0);
            c.waitTicks(5);
        }
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        if (insert) c.click(left + 115, top + 25);
        for (int i = 0; i < priorityClicks; i++) {
            c.click(left + 133, top + 43);
            c.waitTicks(3);
        }
        c.waitTicks(5);
        c.key("escape");
        c.waitTicks(5);
    }

    private static void gui(Client c, int z, String name) {
        c.teleport(3.5, G, z + 0.5);
        Fixtures.interact(c, 0, G, z);
        c.waitScreen("Battery Socket", 5_000L);
        c.waitTicks(10);
        c.screenshot(name);
        c.key("escape");
        c.waitTicks(5);
    }

    private static int slotHolding(Client c, String item) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.toString().contains(item)) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no " + item + " in the player's inventory: " + c.screen());
    }

    private static long gameTime(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("server", true);
        p.addProperty("radius", 0);
        return c.query("entities", p).get("game_time").getAsLong();
    }

    /** The battery charge in the socket cored at (0, G, z), from the server's block entity. */
    private static long charge(Client c, int z) {
        JsonObject be = c.blockEntity(0, G, z);
        String nbt = be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : "";
        Matcher m = CHARGE.matcher(nbt);
        return m.find() ? Long.parseLong(m.group(1)) : 0L;
    }
}
