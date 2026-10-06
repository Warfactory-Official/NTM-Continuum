import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * Pre-release client wave (History §19es-client): the launch pad GUI click path for the nine missiles §19eg-missiles
 * made launchable (schrabidium, bhole, EMP, cluster, cluster_strong, rain, drill, volcano, shuttle), on a real client.
 *
 * <p>Per missile, by hand: the pad's GUI is opened with a right-click, the missile, a designator (target 400 blocks
 * east), a creative battery and the fuel are clicked from the inventory into their slots (the cluster's ethanol and
 * peroxide as eight 1000 mB fluid tanks, the others' liquid fuels from the infinite barrel; the three micro missiles
 * are pre-fuelled), the GUI is closed, and a redstone block next to the pad fires it (1.7: a rising redstone edge).
 * Expected (1.7 TileEntityLaunchPadBase): the missile entity spawns above the pad and climbs, the takeoff sound plays
 * ({@code hbm:weapon.missile_takeoff}, 1.7 "hbm:weapon.missileTakeOff"), the missile item is consumed and both tanks
 * lose exactly the missile's fuelCap. The missile is removed after it has climbed (its impact is ReleaseMissileImpacts).
 */
public final class ReleaseMissilePad implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101;

    /** item id, entity id, fuel fluid, oxidiser fluid, fuel cap (0 = solid) */
    private static final String[][] MISSILES = {
            {"hbm:missile_schrabidium", "hbm:missile_schrabidium", "", "", "0"},
            {"hbm:missile_bhole", "hbm:missile_bhole", "", "", "0"},
            {"hbm:missile_emp", "hbm:missile_emp", "", "", "0"},
            {"hbm:missile_cluster", "hbm:missile_cluster", "hbm:ethanol", "hbm:peroxide", "4000"},
            {"hbm:missile_cluster_strong", "hbm:missile_cluster_strong", "hbm:kerosene", "hbm:peroxide", "8000"},
            {"hbm:missile_rain", "hbm:missile_rain", "hbm:kerosene", "hbm:oxygen", "12000"},
            {"hbm:missile_drill", "hbm:missile_drill", "hbm:kerosene", "hbm:oxygen", "12000"},
            {"hbm:missile_volcano", "hbm:missile_volcano", "hbm:kerosene_reform", "hbm:oxygen", "16000"},
            {"hbm:missile_shuttle", "hbm:entity_missile_shuttle", "hbm:kerosene", "hbm:peroxide", "8000"},
    };

    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    private static String designator(Client c, int x, int z) {
        return legacy(c) ? "hbm:designator{\"hbm:designator_x\":" + x + ",\"hbm:designator_z\":" + z + "}"
                : "hbm:designator[hbm:designator_x=" + x + ",hbm:designator_z=" + z + "]";
    }

    private static String tank(Client c, String fluid) {
        return legacy(c) ? "hbm:fluid_tank{\"hbm:fluid\":\"" + fluid + "\",\"hbm:fluid_amount\":1000}"
                : "hbm:fluid_tank[hbm:fluid=\"" + fluid + "\",hbm:fluid_amount=1000]";
    }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("gamemode survival");
        c.markLog();

        Fixtures.placeOnTop(c, "hbm:launch_pad", X, Y, Z + 5);
        c.waitTicks(10);
        JsonObject pad = server(c, "mode", "pad", "x", X, "y", L, "z", Z + 5);
        Expect.that(pad.has("found") && pad.get("found").getAsBoolean(), "the launch pad was placed by hand: " + pad);
        String[] core = pad.get("core").getAsString().split(" ");
        int px = Integer.parseInt(core[0]), py = Integer.parseInt(core[1]), pz = Integer.parseInt(core[2]);

        c.setOption("sound_capture", "true");
        StringBuilder summary = new StringBuilder();
        try {
            for (String[] m : MISSILES) summary.append(launch(c, px, py, pz, m)).append('\n');
        } finally {
            c.setOption("sound_capture", "false");
            c.cmd("gamemode creative");
        }
        System.out.println(c.node() + " missile pad:\n" + summary);
        Expect.logCleanSinceMark(c, "(?i)(exception|unregistered hbm:|is not registered).*");
    }

    private String launch(Client c, int px, int py, int pz, String[] m) {
        String item = m[0], entity = m[1], fuel = m[2], oxid = m[3];
        int cap = Integer.parseInt(m[4]);
        String name = item.substring(4);

        c.cmd("setblock " + (px + 2) + " " + py + " " + pz + " minecraft:air");
        c.cmd("clear");
        c.cmd("give @s " + item + " 1");
        c.cmd("give @s " + designator(c, px + 400, pz) + " 1");
        c.cmd("give @s hbm:battery_creative 1");
        boolean tanks = name.equals("missile_cluster");
        if (cap > 0) {
            if (tanks) {
                c.cmd("give @s " + tank(c, fuel) + " " + (cap / 1000));
                c.cmd("give @s " + tank(c, oxid) + " " + (cap / 1000));
            } else {
                c.cmd("give @s hbm:fluid_barrel_infinite 1");
                c.cmd("give @s hbm:fluid_barrel_infinite 1");
            }
        }
        c.waitTicks(5);

        c.teleport(px + 0.5, py, pz - 2.5);
        c.waitTicks(3);
        Fixtures.interact(c, px, py, pz);
        c.waitTicks(10);
        Expect.that(c.screenClass() != null, name + ": the pad GUI opened by right-click");
        moveTo(c, item, 0);
        moveTo(c, "hbm:designator", 1);
        moveTo(c, "hbm:battery_creative", 2);
        if (cap > 0) {
            String in = tanks ? "hbm:fluid_tank" : "hbm:fluid_barrel_infinite";
            moveTo(c, in, 3, false); // the pad drains a container in the tick it lands, so the slot may be empty already
            moveTo(c, in, 5, false);
        }
        c.waitTicks(30);
        JsonObject before = server(c, "mode", "pad", "x", px, "y", py, "z", pz);
        c.screenshot(name + "-1-gui-loaded");
        JsonArray t = before.getAsJsonArray("tanks");
        int fuel0 = t.get(0).getAsJsonObject().get("fill").getAsInt(), oxid0 = t.get(1).getAsJsonObject().get("fill").getAsInt();
        if (cap > 0) {
            Expect.that(fuel0 >= cap && oxid0 >= cap, name + ": both tanks hold the fuel cap " + cap + " after GUI loading: " + before);
            Expect.that(t.get(0).getAsJsonObject().get("type").getAsString().equalsIgnoreCase(fuel.substring(4))
                    && t.get(1).getAsJsonObject().get("type").getAsString().equalsIgnoreCase(oxid.substring(4)),
                    name + ": the tanks took the missile's fuel pair: " + t);
        }
        // pull the fuel containers back out so the launch's drain is measurable (the barrels would refill)
        if (cap > 0 && !tanks) {
            c.clickSlot(3, 0);
            c.waitTicks(2);
            c.clickSlot(findEmptyPlayerSlot(c), 0);
            c.waitTicks(2);
            c.clickSlot(5, 0);
            c.waitTicks(2);
            c.clickSlot(findEmptyPlayerSlot(c), 0);
            c.waitTicks(2);
            before = server(c, "mode", "pad", "x", px, "y", py, "z", pz);
            t = before.getAsJsonArray("tanks");
            fuel0 = t.get(0).getAsJsonObject().get("fill").getAsInt();
            oxid0 = t.get(1).getAsJsonObject().get("fill").getAsInt();
        }
        c.key("escape");
        c.waitTicks(5);

        // the small pad loads for 100 ticks (STATE_LOADING) before it is ready
        JsonObject ready = before;
        for (int i = 0; i < 12; i++) {
            ready = server(c, "mode", "pad", "x", px, "y", py, "z", pz);
            if (ready.get("state").getAsInt() == 2) break;
            c.waitTicks(20);
        }
        Expect.that(ready.get("state").getAsInt() == 2, name + ": the pad reports READY: " + ready);

        c.teleport(px + 0.5, py + 1, pz - 9.5);
        c.lookAt(px + 0.5, py + 6.0, pz + 0.5);
        c.waitTicks(5);
        c.markLog();
        c.cmd("setblock " + (px + 2) + " " + py + " " + pz + " minecraft:redstone_block");
        double firstY = Double.NaN, lastY = Double.NaN;
        for (int i = 0; i < 8; i++) {
            c.waitTicks(4);
            JsonObject e = find(c, entity);
            if (e != null) {
                double y = e.get("y").getAsDouble();
                if (Double.isNaN(firstY)) firstY = y;
                lastY = y;
            }
            if (i == 2) c.screenshot(name + "-2-launched");
        }
        Expect.that(!Double.isNaN(firstY), name + ": a " + entity + " entity spawned");
        Expect.that(lastY > firstY + 2, name + ": the missile climbs: " + firstY + " -> " + lastY);
        List<String> takeoff = c.logSinceMark("harness-sound.*id=hbm:weapon.missile_takeoff");
        Expect.that(!takeoff.isEmpty(), name + ": the takeoff sound played");

        JsonObject after = server(c, "mode", "pad", "x", px, "y", py, "z", pz);
        String slot0 = after.getAsJsonArray("slots").get(0).getAsString();
        Expect.that(slot0.isEmpty(), name + ": the missile item was consumed: " + after);
        JsonArray ta = after.getAsJsonArray("tanks");
        int fuel1 = ta.get(0).getAsJsonObject().get("fill").getAsInt(), oxid1 = ta.get(1).getAsJsonObject().get("fill").getAsInt();
        if (cap > 0) {
            Expect.that(fuel0 - fuel1 == cap && oxid0 - oxid1 == cap,
                    name + ": the launch drained exactly the fuel cap " + cap + ": " + fuel0 + "->" + fuel1 + ", " + oxid0 + "->" + oxid1);
        }
        c.cmd("kill @e[type=" + entity + "]");
        c.waitTicks(5);
        return name + ": y " + String.format("%.1f -> %.1f", firstY, lastY) + ", takeoff=" + takeoff.size()
                + ", fuel " + fuel0 + "->" + fuel1 + ", oxid " + oxid0 + "->" + oxid1;
    }

    private static JsonObject find(Client c, String entity) {
        for (JsonElement e : c.entities(64)) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("type") && o.get("type").getAsString().equals(entity)) return o;
        }
        return null;
    }

    private static void moveTo(Client c, String id, int target) {
        moveTo(c, id, target, true);
    }

    private static void moveTo(Client c, String id, int target, boolean check) {
        int from = findPlayerSlot(c, id);
        Expect.that(from >= 0, "player holds " + id);
        c.clickSlot(from);
        c.waitTicks(3);
        c.clickSlot(target);
        c.waitTicks(3);
        if (!check) return;
        JsonObject s = c.slot(target);
        Expect.that(s != null && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id),
                id + " sits in pad slot " + target + ": " + s);
    }

    private static int findEmptyPlayerSlot(Client c) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.get("empty").getAsBoolean()) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no empty player slot");
    }

    static JsonObject release(Client c, Object... kv) {
        JsonObject p = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        return c.query("hbm_release", p);
    }

    /** Polls a server-side hbm_release query until it is no longer pending (a nonce defeats the answer cache). */
    static JsonObject server(Client c, Object... kv) {
        JsonObject r = null;
        for (int i = 0; i < 20; i++) {
            Object[] withNonce = java.util.Arrays.copyOf(kv, kv.length + 2);
            withNonce[kv.length] = "nonce";
            withNonce[kv.length + 1] = System.nanoTime();
            r = release(c, withNonce);
            if (!r.has("pending")) return r;
            c.waitTicks(5);
        }
        return r;
    }

    static int findPlayerSlot(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(id)) {
                return s.get("index").getAsInt();
            }
        }
        return -1;
    }
}
