import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub issue 154: a burning D-D torus feeding a plasma forge welding steel (west arm) and an MHD turbine (south arm), lit by a
 * creative klystron (east arm). 1.7 plays the torus hum, the klystron's FEL loop, the turbine's large-turbine loop and
 * the forge striker's boltgun hits; the client sound log must hold all four. Shots: the striker over several frames,
 * a lone breeder from above (no stray "BreederAlt" ring), the D-D tooltip in the recipe selector and the forge's
 * recipe tooltip with and without left Shift (1.7 adds "Internal: plsm.weldsteel").
 */
public class Gh154FusionSounds implements Scenario {

    private static final int Y = 100, G = 101;
    private static final int FORGE_X = -13;
    private static final int MHDT_Z = 14;
    private static final int BREEDER_X = 40;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.markLog();
        Fixtures.platform(c, 10, Y, 0, 34);
        c.cmd("fill -24 " + G + " -24 44 " + (Y + 12) + " 30 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("time set 6000");
        c.waitTicks(5);

        place(c, "hbm:fusion_torus", 0.5, G, -9.5, 0, Y, -7);
        expectCore(c, 0, G, 0, "hbm:fusion_torus");
        place(c, "hbm:fusion_klystron_creative", 18.5, G, 0.5, 15, Y, 0);
        expectCore(c, 12, G, 0, "hbm:fusion_klystron_creative");
        place(c, "hbm:fusion_plasma_forge", FORGE_X + 0.5, G, -7.5, FORGE_X, Y, -5);
        expectCore(c, FORGE_X, G, 0, "hbm:fusion_plasma_forge");
        place(c, "hbm:fusion_mhdt", 0.5, G, MHDT_Z + 9.5, 0, Y, MHDT_Z + 7);
        expectCore(c, 0, G, MHDT_Z, "hbm:fusion_mhdt");
        place(c, "hbm:fusion_breeder", BREEDER_X + 2.5, G, 0.5, BREEDER_X, Y, 0);
        expectCore(c, BREEDER_X - 2, G, 0, "hbm:fusion_breeder");

        c.cmd("data merge block " + FORGE_X + " " + G + " 0 {recipe0:\"plsm.weldsteel\",power:100000000L}");
        c.cmd("clear");
        c.teleport(FORGE_X + 0.5, G, -7.5);
        c.waitTicks(3);
        Fixtures.interact(c, FORGE_X, G, -5);
        c.waitScreen("PlasmaForge", 5_000L);
        insert(c, "hbm:plate_cast_steel", 64, 3);
        JsonObject forgeScreen = c.screen();
        c.mouseMove(forgeScreen.get("left").getAsInt() + 16, forgeScreen.get("top").getAsInt() + 89);
        c.waitTicks(4);
        c.screenshot("03-forge-recipe");
        c.keyDown("lshift");
        c.waitTicks(4);
        c.screenshot("04-forge-recipe-shift");
        c.keyUp("lshift");
        closeAll(c);

        c.teleport(BREEDER_X - 20.5, G + 11, -20.5);
        c.lookAt(BREEDER_X - 1.5, G + 1, 0.5);
        c.waitTicks(15);
        c.screenshot("01-breeder");
        c.teleport(BREEDER_X - 1.5, G + 40, -24.5);
        c.lookAt(BREEDER_X - 1.5, G, 4.5);
        c.waitTicks(10);
        c.screenshot("02-breeder-above");

        tooltip(c);

        String capture = c.setOption("sound_capture", "true");
        List<String> missing = new ArrayList<>();
        try {
            c.teleport(-2.5, G + 7, 2.5);
            c.lookAt(FORGE_X + 0.5, G + 2, 0.5);
            c.waitTicks(5);
            c.markLog();
            for (int i = 0; i < 12; i++) {
                burn(c);
                c.waitTicks(10);
            }
            c.teleport(FORGE_X + 0.5, G + 8, -4.5);
            c.lookAt(FORGE_X + 0.5, G + 2, 0.5);
            for (int i = 0; i < 6; i++) {
                burn(c);
                c.waitTicks(4);
                c.screenshot("1" + i + "-forge-striker");
            }
            String[] sounds = {"hbm:block.fusion_reactor_running", "hbm:block.fel", "hbm:block.industrial_turbine", "hbm:item.boltgun"};
            for (String sound : sounds) {
                List<String> lines = c.logSinceMark("harness-sound\\] id=" + sound.replace(".", "\\.") + " ");
                System.out.println("[Gh154] " + sound + ": " + lines.size() + (lines.isEmpty() ? "" : " e.g. " + lines.get(0)));
                if (lines.isEmpty()) missing.add(sound);
            }
            System.out.println("[Gh154] forge " + nbt(c, FORGE_X, G, 0));
            System.out.println("[Gh154] mhdt " + nbt(c, 0, G, MHDT_Z));
        } finally {
            if (capture != null) c.setOption("sound_capture", capture);
        }

        c.teleport(-24.5, G + 22, 22.5);
        c.lookAt(-3, G, 2);
        c.waitTicks(10);
        c.screenshot("20-reactor");

        c.cmd("gamerule sendCommandFeedback true");
        Expect.that(missing.isEmpty(), "1.7's fusion sounds were not played: " + missing);
        Expect.logCleanSinceMark(c, "(?i)(exception|\\[ERROR\\]).*(hbm|golem|fusion|plasma)");
        System.out.println("[Gh154] PASS");
    }

    /** The recipe selector's D-D entry, the tooltip the issue compares with 1.7. */
    private static void tooltip(Client c) {
        c.teleport(0.5, G, -9.5);
        c.waitTicks(3);
        c.lookAt(0.5, G + 0.5, -6.5);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("GUIFusionTorus", 5_000L);
        JsonObject s = c.screen();
        c.click(s.get("left").getAsInt() + 52, s.get("top").getAsInt() + 89);
        c.waitScreen("RecipeSelector", 5_000L);
        JsonObject search = null;
        for (JsonElement e : c.screen().getAsJsonArray("widgets")) {
            if (e.getAsJsonObject().get("class").getAsString().contains("EditBox")) search = e.getAsJsonObject();
        }
        Expect.that(search != null, "no search field on the recipe selector");
        int gl = search.get("x").getAsInt() - 28, gt = search.get("y").getAsInt() - 111;
        c.mouseMove(gl + 16, gt + 26);
        c.waitTicks(6);
        c.screenshot("05-dd-tooltip");
        closeAll(c);
    }

    /** Refills a D-D burn (20 mB a tick of deuterium plus coolant) and the turbine's coolant. */
    private static void burn(Client c) {
        c.cmd("data merge block 0 " + G + " 0 {recipe0:\"fus.dd\",power:10000000L,temperature:123F,t0:4000,t1:0,"
                + "ft0:4000,ft0_type:\"hbm:deuterium\",ft3:0}");
        c.cmd("data merge block 0 " + G + " " + MHDT_Z + " {t0:4000,t1:0}");
        c.cmd("data merge block " + FORGE_X + " " + G + " 0 {power:100000000L}");
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

    private static void expectCore(Client c, int x, int y, int z, String id) {
        JsonObject b = c.block(x, y, z);
        Expect.equal(id, b.get("block").getAsString(), id + " at " + x + "," + y + "," + z);
        Expect.that(b.toString().contains("\"cell\":\"false\""), id + " core at " + x + "," + y + "," + z + ": " + b);
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
        return raw.replaceAll("inventory:\\{[^}]*}]}", "");
    }
}
