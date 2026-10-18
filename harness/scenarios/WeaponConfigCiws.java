import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * Configurable gun damage and the radar-linked CIWS. A .357 revolver hits a still iron golem for its configured 7.5,
 * then for 40 after {@code /ntmserver set GUN_LIGHT_REVOLVER_DAMAGE 40}. A Howard in player mode (48 blocks) ignores
 * the player standing 68 blocks away on a pillar; once a powered radar holds a linker to it, the radar designates
 * the player and the Howard fires at him.
 */
public final class WeaponConfigCiws implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    private static final int HX = X - 26, HZ = Z;       // Howard
    private static final int RX = X - 20, RZ = Z - 12;  // radar
    private static final int PX = X + 30, PZ = Z;       // the player's pillar, 56 blocks east of the Howard
    private static final int PY = Y + 41;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 30);
        c.cmd("fill -30 " + (Y + 1) + " -30 30 " + (Y + 20) + " 30 minecraft:air");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.waitTicks(2);

        gunDamage(c);
        ciws(c);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private static void gunDamage(Client c) {
        expectChat(c, "ntmserver get GUN_LIGHT_REVOLVER_DAMAGE", "GUN_LIGHT_REVOLVER_DAMAGE: 7.5");
        c.cmd("summon minecraft:iron_golem 0.5 " + (Y + 1) + " 5.5 {NoAI:1b}");
        c.cmd("clear @s");
        c.cmd("give @s hbm:gun_light_revolver");
        c.cmd("give @s hbm:ammo_standard_m357_sp 12");
        c.hotbar(1);
        c.teleport(0.5, Y + 1, 0.5);
        c.waitTicks(30);
        c.key("r", 2);
        c.waitTicks(80);

        double full = health(c, "minecraft:iron_golem");
        shoot(c);
        double first = health(c, "minecraft:iron_golem");
        c.screenshot("revolver-default-damage");
        Expect.that(Math.abs(full - first - 7.5D) < 0.01D, "the revolver dealt its configured 7.5: " + full + " -> " + first);

        expectChat(c, "ntmserver set GUN_LIGHT_REVOLVER_DAMAGE 40", "GUN_LIGHT_REVOLVER_DAMAGE");
        try {
            expectChat(c, "ntmserver get GUN_LIGHT_REVOLVER_DAMAGE", "GUN_LIGHT_REVOLVER_DAMAGE: 40.0");
            shoot(c);
            double second = health(c, "minecraft:iron_golem");
            c.screenshot("revolver-configured-damage");
            Expect.that(Math.abs(first - second - 40D) < 0.01D, "the revolver dealt the edited 40: " + first + " -> " + second);
        } finally {
            c.cmd("ntmserver set GUN_LIGHT_REVOLVER_DAMAGE 7.5");
            c.waitTicks(4);
        }
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.cmd("clear @s");
    }

    private static void shoot(Client c) {
        c.lookAt(0.5, Y + 2.3, 5.5);
        c.waitTicks(25);
        c.attack();
        c.waitTicks(10);
    }

    private static void ciws(Client c) {
        // a player carrying digamma jams every radar (1.7); the shared harness world's player has some
        JsonObject clear = new JsonObject();
        clear.addProperty("op", "set");
        clear.addProperty("digamma", 0.0);
        c.query("hbm_gapsb", clear);

        // radar, powered by a creative battery clicked into slot 9
        place(c, "hbm:machine_radar", RX, Y, RZ);
        Expect.block(c, RX, Y + 1, RZ, "hbm:machine_radar");
        c.cmd("give @s hbm:battery_creative 1");
        c.waitTicks(2);
        openRadarSlots(c);
        clickFromInventory(c, "hbm:battery_creative", 9);
        c.waitTicks(5);
        close(c);

        // the Howard: belts and a battery clicked in, switched on and set to target players
        place(c, "hbm:turret_howard", HX, Y, HZ);
        int[] core = findCore(c, HX, Y + 1, HZ, "turret_howard");
        Expect.that(core != null, "the Howard formed a core");
        c.cmd("give @s hbm:ammo_dgk 4");
        c.cmd("give @s hbm:battery_creative 1");
        c.waitTicks(2);
        openAt(c, core[0] + 4.5, core[2] + 0.5, core[0] + 0.5, core[2] + 0.5, "Howard");
        JsonObject screen = c.screen();
        clickFromInventory(c, "hbm:ammo_dgk", 1);
        clickFromInventory(c, "hbm:battery_creative", 10);
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        c.click(left + 115 + 9, top + 26 + 9); // power switch
        c.waitTicks(3);
        c.click(left + 8 + 5, top + 30 + 5);   // target players
        c.waitTicks(3);
        c.screenshot("howard-gui-on-players");
        close(c);
        String nbt = c.blockEntity(core[0], core[1], core[2]).toString().replaceAll("\\s", "");
        Expect.that(nbt.contains("isOn:1b") && nbt.contains("targetPlayers:1b"), "the Howard is on in player mode: " + nbt);

        // a pillar 56 blocks east and 40 up: 68 blocks from the gun, beyond its 52.8-block sight in player mode
        Fixtures.setBlock(c, PX, PY - 1, PZ, "minecraft:stone");
        c.hotbar(9);
        c.teleport(PX + 0.5, PY, PZ + 0.5);
        c.waitTicks(100);
        int[] idle = ammo(c, core);
        c.waitTicks(100);
        int[] stillIdle = ammo(c, core);
        Expect.that(idle[0] == stillIdle[0] && idle[1] == stillIdle[1],
                "without a radar the Howard does not fire at a player 68 blocks away: " + idle[0] + "/" + idle[1]
                        + " -> " + stillIdle[0] + "/" + stillIdle[1]);
        c.lookAt(core[0] + 0.5, Y + 3, core[2] + 0.5);
        c.waitTicks(4);
        c.screenshot("howard-idle-from-pillar");

        // link the Howard to the radar
        Fixtures.giveOnly(c, "hbm:radar_linker", 1);
        linkWith(c, core[0], core[2]);
        openRadarSlots(c);
        clickFromInventory(c, "hbm:radar_linker", 0);
        c.waitTicks(3);
        c.screenshot("radar-slots-linked");
        close(c);
        String radarNbt = c.blockEntity(RX, Y + 1, RZ).toString();
        Expect.that(radarNbt.contains("radar_linker"), "the radar holds the linker: " + radarNbt);

        c.hotbar(9);
        c.teleport(PX + 0.5, PY, PZ + 0.5);
        c.waitTicks(20);
        int[] before = ammo(c, core);
        c.waitTicks(80);
        int[] after = ammo(c, core);
        c.lookAt(core[0] + 0.5, Y + 3, core[2] + 0.5);
        c.waitTicks(4);
        c.screenshot("howard-firing-at-pillar");
        Expect.that(after[0] * 100 + after[1] < before[0] * 100 + before[1],
                "the radar-designated Howard fires at the player 68 blocks away: " + before[0] + "/" + before[1]
                        + " -> " + after[0] + "/" + after[1]);

        worldShot(c, "howard-aim", core[0] + 6.5, Y + 6, core[2] + 6.5, core[0] + 0.5, Y + 3, core[2] + 0.5);
        Fixtures.setBlock(c, PX, PY - 1, PZ, "minecraft:air");
    }

    /** The Howard's belts in the slot and rounds left in the loaded belt. */
    private static int[] ammo(Client c, int[] core) {
        String nbt = c.blockEntity(core[0], core[1], core[2]).toString().replaceAll("\\s", "");
        int loaded = intAfter(nbt, "loaded:");
        int belts = 0;
        int i = nbt.indexOf("hbm:ammo_dgk");
        if (i >= 0) {
            String tail = nbt.substring(Math.max(0, i - 80), Math.min(nbt.length(), i + 80));
            belts = intAfter(tail, "count:");
        }
        return new int[]{belts, loaded};
    }

    private static int intAfter(String s, String key) {
        int i = s.indexOf(key);
        if (i < 0) return 0;
        int j = i + key.length();
        int k = j;
        while (k < s.length() && (Character.isDigit(s.charAt(k)) || s.charAt(k) == '-')) k++;
        return k == j ? 0 : Integer.parseInt(s.substring(j, k));
    }

    private static double health(Client c, String type) {
        JsonObject params = new JsonObject();
        params.addProperty("radius", 40);
        params.addProperty("type", type);
        params.addProperty("server", true);
        JsonObject result = c.query("entities", params);
        JsonObject side = result.has("entities") ? result : result.getAsJsonObject("server");
        JsonArray list = side == null ? null : side.getAsJsonArray("entities");
        Expect.that(list != null, "entities query: " + result);
        for (JsonElement e : list) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("type").getAsString().equals(type)) return o.get("health").getAsDouble();
        }
        Expect.that(false, type + " not found: " + result);
        return 0;
    }

    private static void expectChat(Client c, String command, String expected) {
        c.cmd(command);
        c.waitTicks(6);
        boolean found = false;
        for (String line : c.logSinceMark("\\[CHAT\\]")) if (line.contains(expected)) found = true;
        Expect.that(found, "/" + command + " answers \"" + expected + "\"");
    }

    private static void place(Client c, String item, int x, int y, int z) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + 4.5, y + 1, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.9, y + 0.98, z + 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
    }

    private static int[] findCore(Client c, int x, int y, int z, String id) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                JsonObject be = c.blockEntity(x + dx, y, z + dz);
                if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.toString().contains(id)) {
                    return new int[]{x + dx, y, z + dz};
                }
            }
        }
        return null;
    }

    /** Sneak + right-click the turret with the held linker. */
    private static void linkWith(Client c, int x, int z) {
        c.hotbar(1);
        c.teleport(x + 3.5, Y + 1, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, Y + 1.5, z + 0.5);
        c.waitTicks(1);
        c.keyDown("key.sneak");
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(3);
        c.keyUp("key.sneak");
        c.waitTicks(2);
    }

    private static void openRadarSlots(Client c) {
        openAt(c, RX + 2.5, RZ + 0.5, RX + 0.5, RZ + 0.5, "GUIMachineRadarNT");
        JsonObject status = c.status();
        int w = status.get("gui_width").getAsInt(), h = status.get("gui_height").getAsInt();
        int left = (w - 216) / 2, top = (h - 234) / 2;
        c.click(left - 10 + 4, top + 158 + 4);
        c.waitScreen("GUIMachineRadarNTSlots", 5000);
        c.waitTicks(3);
    }

    private static void openAt(Client c, double sx, double sz, double lx, double lz, String title) {
        c.teleport(sx, Y + 1, sz);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(lx, Y + 1.5, lz);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen(title, 5000);
        Expect.screenStaysOpen(c, title, 10);
        c.waitTicks(5);
    }

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject slot = findSlot(c, id);
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(machineSlot);
        c.waitTicks(1);
        JsonObject carried = c.screen().getAsJsonObject("carried");
        if (carried != null && carried.has("id") && !carried.get("id").getAsString().contains("air")) {
            c.clickSlot(slot.get("index").getAsInt());
            c.waitTicks(1);
        }
    }

    private static JsonObject findSlot(Client c, String id) {
        JsonObject screen = c.screen();
        if (!screen.has("slots")) return null;
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) return s;
        }
        return null;
    }

    private static void worldShot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.hotbar(9);
        c.waitTicks(1);
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(8);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }
}
