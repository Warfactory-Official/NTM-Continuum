import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Release audit, item processing machines: hopper automation. For each machine a hopper above it pushes items in and a
 * hopper below it pulls a pre-placed output out; a hopper at each horizontal side is tried too. The GUI is opened
 * afterwards to read which machine slots received the items. Soft checks print the observed behaviour for review.
 * Optional environment RC_PROC_ONLY=comma separated case names.
 */
public final class RcProcAuto implements Scenario {

    private static final int Y = 100;
    private final List<String> failures = new ArrayList<>();

    private record Case(String name, String block, String input, int outSlot, String outItem) {}

    private static final Case[] CASES = {
            new Case("shredder", "machine_shredder", "minecraft:iron_ingot", 9, "hbm:powder_iron"),
            new Case("centrifuge", "machine_centrifuge", "minecraft:iron_ore", 2, "minecraft:gravel"),
            new Case("epress", "machine_epress", "minecraft:iron_ingot", 3, "hbm:plate_iron"),
            new Case("efurnace", "machine_electric_furnace_off", "minecraft:iron_ore", 2, "minecraft:iron_ingot"),
            new Case("difurnace", "machine_difurnace_off", "minecraft:iron_ingot", 3, "hbm:ingot_steel"),
            new Case("press", "machine_press", "minecraft:iron_ingot", 3, "hbm:plate_iron"),
            new Case("ashpit", "machine_ashpit", "minecraft:iron_ingot", 0, "hbm:powder_ash_wood"),
            new Case("rtgfurnace", "machine_difurnace_rtg_off", "minecraft:iron_ingot", 2, "hbm:ingot_steel"),
            new Case("autocrafter", "machine_autocrafter", "minecraft:iron_ingot", 0, "minecraft:stick"),
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.key("escape");
        c.waitTicks(3);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 24);
        c.waitTicks(5);
        String only = System.getenv("RC_PROC_ONLY");
        for (Case cs : CASES) {
            if (only != null && !only.isBlank() && !List.of(only.split(",")).contains(cs.name())) continue;
            try {
                one(c, cs);
            } catch (RuntimeException e) {
                fail(cs.name() + ": " + e.getMessage());
                c.key("escape");
                c.waitTicks(3);
            }
        }
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|golem|render|screen|gui)");
        Expect.that(failures.isEmpty(), "soft checks failed: " + failures);
    }

    private void one(Client c, Case cs) {
        c.cmd("fill -10 " + (Y + 1) + " -10 10 " + (Y + 14) + " 10 minecraft:air");
        c.cmd("fill -10 " + (Y - 3) + " -10 10 " + Y + " 10 minecraft:stone");
        c.waitTicks(3);
        Fixtures.placeOnTop(c, "hbm:" + cs.block(), 0, Y, 0);
        c.waitTicks(6);
        int[] core = findCore(c);
        if (core == null) {
            fail(cs.name() + ": no block entity");
            return;
        }
        int x = core[0], y = core[1], z = core[2];
        System.out.println("[" + cs.name() + "] core " + x + "," + y + "," + z);

        // Above: input hopper pointing down.
        boolean side = cs.name().matches("centrifuge|epress|press");
        int hx = side ? x + 1 : x, hy = side ? y : y + 1;
        c.cmd("setblock " + hx + " " + hy + " " + z + " minecraft:hopper[facing=" + (side ? "west" : "down") + "]");
        c.cmd("item replace block " + hx + " " + hy + " " + z + " container.0 with " + cs.input() + " 5");
        // Below: output hopper into a chest, machine output pre-filled.
        c.cmd("setblock " + x + " " + (y - 1) + " " + z + " minecraft:hopper[facing=down]");
        c.cmd("setblock " + x + " " + (y - 2) + " " + z + " minecraft:chest");
        c.cmd("item replace block " + x + " " + y + " " + z + " container." + cs.outSlot() + " with " + cs.outItem() + " 3");
        c.waitTicks(80);

        c.teleport(x + 2.5, y, z + 0.5);
        c.waitTicks(2);
        Fixtures.interact(c, x, y, z);
        c.waitTicks(10);
        Map<Integer, String> slots = new LinkedHashMap<>();
        if (c.screenClass() != null) {
            for (JsonElement e : c.screen().getAsJsonArray("slots")) {
                JsonObject s = e.getAsJsonObject();
                if (s.get("player_inventory").getAsBoolean() || s.get("empty").getAsBoolean()) continue;
                slots.put(s.get("index").getAsInt(), s.get("id").getAsString() + "*" + s.get("count").getAsInt());
            }
            c.screenshot(cs.name() + "-auto");
            c.key("escape");
            c.waitTicks(3);
        } else {
            fail(cs.name() + ": GUI did not open");
        }
        String hopperAbove = c.blockEntity(hx, hy, z).has("nbt") ? c.blockEntity(hx, hy, z).get("nbt").getAsString().replaceAll("\\s+", " ") : "?";
        String chest = c.blockEntity(x, y - 2, z).has("nbt") ? c.blockEntity(x, y - 2, z).get("nbt").getAsString().replaceAll("\\s+", " ") : "?";
        System.out.println("[" + cs.name() + "] machine slots after 80 ticks: " + slots);
        System.out.println("[" + cs.name() + "] hopper above: " + trim(hopperAbove));
        System.out.println("[" + cs.name() + "] chest below: " + trim(chest));
    }

    private static String trim(String s) {
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    private void fail(String message) {
        System.out.println("[FAIL] " + message);
        failures.add(message);
    }

    private static int[] findCore(Client c) {
        for (int dy = 1; dy <= 5; dy++)
            for (int r = 0; r <= 6; r++)
                for (int dz = -r; dz <= r; dz++)
                    for (int dx = -r; dx <= r; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        JsonObject be = c.blockEntity(dx, Y + dy, dz);
                        if (be != null && be.has("block_entity") && !be.get("block_entity").isJsonNull()) {
                            return new int[]{dx, Y + dy, dz};
                        }
                    }
        return null;
    }
}
