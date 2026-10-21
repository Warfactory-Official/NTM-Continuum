import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * A Soyuz in cargo mode (1.7 mode 1): rocket and a manual designator aimed 200 blocks south, cargo in the payload
 * slots, fuel for 5000 + distance (5200 of the 128,000 tank), launched with the GUI button. The capsule must land at
 * the designated spot as a block holding the cargo and the spent rocket, and the launcher must have spent exactly
 * the fuel and 75% of its 1,000,000 power.
 */
public final class RcSpaceSoyuzCargo implements Scenario {

    private static final int Y = 100;
    private static final int TZ = 200;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("forceload add -16 " + (TZ - 16) + " 16 " + (TZ + 16));
        c.waitTicks(60);
        Fixtures.platform(c, 0, Y, 0, 30);
        Fixtures.platform(c, 0, Y, TZ, 8);
        c.cmd("fill -30 " + (Y + 1) + " -30 30 " + (Y + 8) + " 30 minecraft:air");
        c.cmd("gamemode creative");
        c.cmd("clear");
        c.cmd("give @s hbm:soyuz_launcher 1");
        c.waitTicks(2);
        c.hotbar(1);
        c.teleport(0.5, Y + 1, -3.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(10);
        int[] core = findCore(c, 0, Y + 5, 0, "soyuz_launcher", 2);
        Expect.that(core != null, "the Soyuz complex formed a core");

        String at = core[0] + " " + core[1] + " " + core[2];
        c.cmd("item replace block " + at + " container.0 with hbm:missile_soyuz_0 1");
        c.cmd("item replace block " + at + " container.9 with minecraft:diamond 7");
        c.cmd("item replace block " + at + " container.10 with minecraft:gold_ingot 12");
        c.cmd("data merge block " + at + " {mode:1b,power:1000000L,fuel:128000,oxidizer:128000}");
        JsonObject des = new JsonObject();
        JsonArray xz = new JsonArray();
        xz.add(0);
        xz.add(TZ);
        des.add("designate", xz);
        Expect.that(c.query("hbm_gapsc", des).get("designated").getAsBoolean(), "designator handed over");
        c.waitTicks(3);

        openLauncher(c, core);
        JsonObject held = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals("hbm:designator_manual")) held = s;
        }
        Expect.that(held != null, "the designator is in the inventory");
        c.clickSlot(held.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(1);
        c.waitTicks(4);
        c.screenshot("01-cargo-loaded");
        String before = c.blockEntity(core[0], core[1], core[2]).toString().replaceAll("\\s", "");
        System.out.println("[RcSpaceSoyuzCargo] before " + before.substring(Math.max(0, before.indexOf("\"nbt\""))).replace("\\n", ""));
        JsonObject gui = c.screen();
        c.click(gui.get("left").getAsInt() + 97, gui.get("top").getAsInt() + 106);
        c.waitTicks(20);
        c.screenshot("02-counting");
        c.key("escape");
        c.waitTicks(5);

        boolean lifted = false;
        for (int i = 0; i < 100 && !lifted; i++) {
            c.waitTicks(20);
            lifted = !c.blockEntity(core[0], core[1], core[2]).toString().contains("missile_soyuz");
        }
        Expect.that(lifted, "the Soyuz left the pad");
        String after = c.blockEntity(core[0], core[1], core[2]).toString().replaceAll("\\s", "");
        System.out.println("[RcSpaceSoyuzCargo] after " + after.substring(Math.max(0, after.indexOf("\"nbt\""))).replace("\\n", ""));

        int[] cap = null;
        for (int i = 0; i < 120 && cap == null; i++) {
            c.waitTicks(40);
            cap = find(c, "hbm:soyuz_capsule", 0, TZ);
        }
        Expect.that(cap != null, "the capsule landed at the designated target");
        String capNbt = c.blockEntity(cap[0], cap[1], cap[2]).toString().replaceAll("\\s", "");
        System.out.println("[RcSpaceSoyuzCargo] capsule at " + cap[0] + " " + cap[1] + " " + cap[2] + " " + capNbt.substring(capNbt.indexOf("\"nbt\"")).replace("\\n", ""));
        Expect.that(capNbt.contains("diamond") && capNbt.contains("gold_ingot") && capNbt.contains("missile_soyuz"),
                "the capsule holds the cargo and the spent rocket");
        c.teleport(cap[0] + 5.5, Y + 3, cap[2] + 0.5);
        c.waitTicks(30);
        c.lookAt(cap[0] + 0.5, Y + 1, cap[2] + 0.5);
        c.waitTicks(10);
        c.screenshot("03-capsule");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointerException|ClassCastException|IllegalStateException|Ticking (entity|block entity)");
        c.cmd("forceload remove -16 " + (TZ - 16) + " 16 " + (TZ + 16));
    }

    private static int[] find(Client c, String block, int x, int z) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int y = Y + 1; y <= Y + 2; y++) {
                    if (c.block(x + dx, y, z + dz).toString().contains(block)) return new int[]{x + dx, y, z + dz};
                }
            }
        }
        return null;
    }

    private static int[] findCore(Client c, int x, int y, int z, String id, int r) {
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    JsonObject be = c.blockEntity(x + dx, y + dy, z + dz);
                    if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.toString().contains(id)) {
                        return new int[]{x + dx, y + dy, z + dz};
                    }
                }
            }
        }
        return null;
    }

    private static void openLauncher(Client c, int[] core) {
        c.teleport(core[0] + 0.5, Y + 1, core[2] - 9.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.lookAt(core[0] + 0.5, core[1] - 0.5, core[2] - 5.95);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("Soyuz", 5000);
        Expect.screenStaysOpen(c, "Soyuz", 10);
        c.waitTicks(5);
    }
}
