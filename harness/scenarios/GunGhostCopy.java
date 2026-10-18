import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.List;

/**
 * A held gun rewrites its components every server tick (animation clock). Moving it with real inventory clicks, or
 * dropping it with Q, must never leave a second copy: not a client-only ghost, not a real duplicate.
 *
 * <p>Positive signal per step: the client's inventory count, the server's count ({@code /clear @s <gun> 0}, read from
 * the chat log) and the dropped item entities the server holds; client == server and server + dropped == 1.
 */
public final class GunGhostCopy implements Scenario {

    private static final int X = 380;
    private static final int Y = 100;
    private static final int Z = 380;
    private static final String GUN = "hbm:gun_light_revolver";

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        Fixtures.platform(c, X, Y, Z, 8);
        c.cmd("gamerule doMobSpawning false");
        StringBuilder bad = new StringBuilder();
        try {
            for (String mode : new String[] {"survival", "creative"}) {
                c.cmd("gamemode " + mode);
                // clicks: pick the held gun up and put it down in another slot, 20 times, at varied tick offsets
                reset(c);
                c.key("e");
                c.waitTicks(10);
                if (mode.equals("creative")) {
                    // the creative screen's Inventory tab: bottom row, last column
                    JsonObject sc = c.screen();
                    c.click(sc.get("left").getAsInt() + 169 + 13, sc.get("top").getAsInt() + 136 + 14);
                    c.waitTicks(10);
                }
                String screen = c.screenClass();
                System.out.println(c.node() + " " + mode + " screen " + screen + ": " + c.screen());
                for (int i = 0; i < 20; i++) {
                    JsonObject[] move = moveSlots(c);
                    if (move == null) {
                        System.out.println(c.node() + " " + mode + " click " + i + " slots not found: " + c.screen() + " inv " + c.inventory());
                        bad.append(mode).append(" click ").append(i).append(": slots not found; ");
                        break;
                    }
                    c.click(move[0].get("center_x").getAsDouble(), move[0].get("center_y").getAsDouble());
                    c.waitTicks(i % 3);
                    c.click(move[1].get("center_x").getAsDouble(), move[1].get("center_y").getAsDouble());
                    c.waitTicks(1 + i % 4);
                    check(c, mode + " click " + i, bad);
                }
                c.key("escape");
                c.waitTicks(5);
                check(c, mode + " after clicks", bad);

                // drops: Q with the gun held, ten times
                for (int i = 0; i < 10; i++) {
                    reset(c);
                    c.key("q");
                    c.waitTicks(1 + i % 4);
                    check(c, mode + " drop " + i, bad);
                    c.waitTicks(10);
                    check(c, mode + " drop " + i + " +10", bad);
                }
            }
        } finally {
            c.cmd("kill @e[type=item,distance=..20]");
            c.cmd("clear");
            c.cmd("gamemode creative");
        }
        Expect.that(bad.length() == 0, "no ghost or duplicate gun: " + bad);
    }

    /** Empty inventory, no dropped items, one gun selected in hotbar slot 0, drawn and ticking. */
    private static void reset(Client c) {
        c.cmd("kill @e[type=item,distance=..20]");
        c.cmd("clear");
        c.hotbar(1);
        c.cmd("give @s " + GUN);
        c.waitTicks(30);
    }

    /**
     * The gun's slot in the open screen and an empty slot in the same row (the hotbar), by screen position: the
     * creative Inventory tab wraps every slot with index 0, so indices cannot tell its slots apart.
     */
    private static JsonObject[] moveSlots(Client c) {
        JsonObject screen = c.screen();
        if (!screen.has("slots")) return null;
        JsonArray slots = screen.getAsJsonArray("slots");
        JsonObject gun = null;
        for (int i = 0; i < slots.size(); i++) {
            JsonObject s = slots.get(i).getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.toString().contains("gun_light_revolver")) gun = s;
        }
        if (gun == null) return null;
        for (int i = 0; i < slots.size(); i++) {
            JsonObject s = slots.get(i).getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("empty") && s.get("empty").getAsBoolean()
                    && s.get("center_y").getAsInt() == gun.get("center_y").getAsInt()
                    && s.get("center_x").getAsInt() != gun.get("center_x").getAsInt()) {
                return new JsonObject[] {gun, s};
            }
        }
        return null;
    }

    private static void check(Client c, String step, StringBuilder bad) {
        int client = 0;
        JsonArray slots = c.inventory().getAsJsonArray("slots");
        for (int i = 0; i < slots.size(); i++) if (slots.get(i).toString().contains("gun_light_revolver")) client++;
        JsonObject inv = c.inventory();
        boolean carried = inv.has("carried") && inv.get("carried").toString().contains("gun_light_revolver");
        c.markLog();
        c.cmd("clear @s " + GUN + " 0");
        c.waitTicks(2);
        List<String> found = c.logSinceMark("\\[CHAT\\].*(Found|No items)");
        int server = -1;
        for (String line : found) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Found (\\d+)").matcher(line);
            if (m.find()) server = Integer.parseInt(m.group(1));
            else if (line.contains("No items")) server = 0;
        }
        int dropped = 0;
        JsonObject p = new JsonObject();
        p.addProperty("radius", 20);
        p.addProperty("server", true);
        JsonArray ents = c.query("entities", p).getAsJsonArray("entities");
        for (int i = 0; i < ents.size(); i++) if (ents.get(i).getAsJsonObject().get("type").getAsString().contains("item")) dropped++;
        String line = step + ": client=" + client + (carried ? "+carried" : "") + " server=" + server + " dropped=" + dropped;
        System.out.println(c.node() + " " + line);
        if (carried) return;
        if (server < 0 || client != server || server + dropped != 1) bad.append(line).append("; ");
    }
}
