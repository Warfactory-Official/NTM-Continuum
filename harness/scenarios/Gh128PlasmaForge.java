import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 128: a Plasma Forge next to a burning torus read 0 TU. In 1.7 the forge takes the beam on its sides
 * ({@code getRotation(UP)} of its facing, five out at +2), which meets a torus arm with the two cores 13 apart.
 * A torus on D-D (1 MTU, one receiver, so 1,000,000 TU) feeds a forge facing north on its west arm; the forge must
 * read that and finish a 1 MTU recipe set up through its menu.
 */
public class Gh128PlasmaForge implements Scenario {

    private static final int Y = 100, G = 101;
    private static final int FORGE_X = -13;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 26);
        c.cmd("fill -26 " + G + " -26 26 " + (Y + 10) + " 26 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("gamerule sendCommandFeedback false");
        c.waitTicks(5);

        place(c, "hbm:fusion_torus", 0.5, G, -9.5, 0, Y, -7);
        expectCore(c, 0, G, 0, "hbm:fusion_torus", null);
        place(c, "hbm:fusion_klystron_creative", 18.5, G, 0.5, 15, Y, 0);
        expectCore(c, 12, G, 0, "hbm:fusion_klystron_creative", "east");
        place(c, "hbm:fusion_plasma_forge", FORGE_X + 0.5, G, -7.5, FORGE_X, Y, -5);
        expectCore(c, FORGE_X, G, 0, "hbm:fusion_plasma_forge", "north");

        c.cmd("data merge block " + FORGE_X + " " + G + " 0 {recipe0:\"plsm.exsteelplating\"}");
        c.cmd("clear");
        c.teleport(FORGE_X + 0.5, G, -7.5);
        c.waitTicks(3);
        Fixtures.interact(c, FORGE_X, G, -5);
        c.waitScreen("PlasmaForge", 5_000L);
        Expect.screenStaysOpen(c, "PlasmaForge", 5);
        insert(c, "hbm:battery_creative", 1, 0);
        insert(c, "hbm:plate_cast_steel", 8, 3);
        insert(c, "hbm:plate_titanium", 8, 4);
        insert(c, "hbm:bolt_steel", 32, 5);
        closeAll(c);

        long idle = forgeTu(c);
        Expect.equal(0L, idle, "the forge reads 0 TU while the torus is cold");

        burn(c);
        long tu = 0;
        for (int i = 0; i < 20 && tu == 0; i++) {
            c.waitTicks(10);
            tu = forgeTu(c);
        }
        System.out.println("[Gh128] forge reads " + tu + " TU, torus " + nbt(c, 0, G, 0));
        Expect.equal(1_000_000L, tu, "the forge reads the D-D plasma of one receiver");

        c.teleport(FORGE_X + 0.5, G, -7.5);
        c.waitTicks(3);
        Fixtures.interact(c, FORGE_X, G, -5);
        c.waitScreen("PlasmaForge", 5_000L);
        JsonObject s = c.screen();
        // 1.7 drawCustomInfoStat(guiLeft + 25, guiTop + 115, 18, 18): the plasma gauge
        c.mouseMove(s.get("left").getAsInt() + 34, s.get("top").getAsInt() + 124);
        c.waitTicks(6);
        c.screenshot("10-forge-gauge");

        boolean made = false;
        for (int i = 0; i < 30 && !made; i++) {
            burn(c);
            c.waitTicks(20);
            made = !c.screen().getAsJsonArray("slots").get(15).getAsJsonObject().get("empty").getAsBoolean();
        }
        c.screenshot("11-forge-output");
        Expect.that(made, "the forge finished its 40 tick recipe: " + nbt(c, FORGE_X, G, 0));
        closeAll(c);

        c.teleport(FORGE_X + 0.5, G + 9, -9.5);
        c.lookAt(FORGE_X + 5, G + 2, 0.5);
        c.waitTicks(10);
        c.screenshot("20-forge-beside-torus");

        c.cmd("gamerule sendCommandFeedback true");
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|golem|fusion|plasma)");
        System.out.println("[Gh128] PASS");
    }

    /** Refills what a D-D burn spends: 20 mB a tick of deuterium, plus coolant to stay at 123 K. */
    private static void burn(Client c) {
        c.cmd("data merge block 0 " + G + " 0 {recipe0:\"fus.dd\",power:10000000L,temperature:123F,t0:4000,t1:0,"
                + "ft0:4000,ft0_type:\"hbm:deuterium\",ft3:0}");
    }

    private static long forgeTu(Client c) {
        JsonObject params = new JsonObject();
        params.addProperty("x", FORGE_X);
        params.addProperty("y", G);
        params.addProperty("z", 0);
        for (int attempt = 0; attempt < 20; attempt++) {
            JsonObject result = c.query("hbm_block_info", params);
            if (result.has("gauges")) {
                for (JsonElement gauge : result.getAsJsonArray("gauges")) {
                    JsonObject g = gauge.getAsJsonObject();
                    if ("TU".equals(g.get("unit").getAsString())) return g.get("value").getAsLong();
                }
            }
            c.waitTicks(5);
        }
        Expect.that(false, "no plasma gauge on the forge");
        return -1;
    }

    /** Stand at (px, py, pz), aim at the top face of block (bx, by, bz), right-click with one {@code item}. */
    private static void place(Client c, String item, double px, double py, double pz, int bx, int by, int bz) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(px, py, pz);
        c.waitTicks(3);
        c.lookAt(bx + 0.5, by + 0.98, bz + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(4);
        Expect.equal(item, c.block(bx, by + 1, bz).get("block").getAsString(), item + " placed by hand");
    }

    private static void expectCore(Client c, int x, int y, int z, String id, String facing) {
        JsonObject b = c.block(x, y, z);
        String st = b.toString();
        Expect.equal(id, b.get("block").getAsString(), id + " at " + x + "," + y + "," + z);
        Expect.that(st.contains("\"cell\":\"false\""), id + " core at " + x + "," + y + "," + z + ": " + st);
        if (facing != null) Expect.that(st.contains("\"facing\":\"" + facing + "\""), id + " faces " + facing + ": " + st);
    }

    private static void insert(Client c, String item, int count, int slot) {
        c.cmd("give @s " + item + " " + count);
        c.waitTicks(4);
        int source = -1;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean() && s.get("id").getAsString().equals(item)) {
                source = s.get("index").getAsInt();
                break;
            }
        }
        Expect.that(source >= 0, "no player slot holding " + item);
        c.clickSlot(source);
        c.waitTicks(2);
        c.clickSlot(slot);
        c.waitTicks(5);
        Expect.slotHolds(c, slot, item, count);
    }

    private static void closeAll(Client c) {
        for (int i = 0; i < 4 && c.screenClass() != null && !"none".equals(c.screenClass()); i++) {
            c.key("escape");
            c.waitTicks(4);
        }
        c.waitTicks(2);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        String raw = be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : be.toString();
        Matcher progress = Pattern.compile("progress0:[^,}]*").matcher(raw);
        return progress.find() ? progress.group() + " " + raw : raw;
    }
}
