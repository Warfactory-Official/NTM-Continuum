import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-release client wave (History §19es-client): the wave-3 pollution, toxin and fluid-trait changes on a real client,
 * on every node (they were GameTested only on 26.1 NeoForge and compiled elsewhere).
 * <ol>
 *   <li>Fluid-trait tooltips (1.7 FluidType.addInfo, FT_Polluting, FT_Toxin): the inventory hover tooltip of fluid icons
 *       for crude oil and phosgene, without and with physical left Shift, read through the client's own tooltip path
 *       ({@code Screen.getTooltipFromItem}) and screenshotted. 1.7: "[Polluting]" gold always; with Shift "When spilled:"
 *       green, "When burned:" red, " - <float> SOOT per mB" lines; "[Toxin]" light purple only with Shift; without
 *       Shift the italic dark-grey "Hold <LSHIFT> to display more info" closes the tooltip.</li>
 *   <li>Smoke through a duct to a chimney: a Blast Furnace (DiFurnace, 1.7 TileEntityMachinePolluting, 50 mB smoke
 *       buffer) loaded through its GUI with iron + coal, a smoke duct to a brick chimney: the chimney reports
 *       {@code onTicks} (it received smoke), the furnace's smoke tank never fills, the region gains soot (0.25 of the
 *       smoke). Without the duct the tank fills to 50 and overflows into the region with the 1.7 fizz.</li>
 *   <li>FT_Toxin at runtime: a pig in a phosgene mist ({@code hbm:entity_mist}) loses 4 x intensity on the 20-tick clock.</li>
 * </ol>
 */
public final class ReleasePollutionToxin implements Scenario {

    private static final int X = 0, Y = 100, Z = 0, L = 101;

    private static boolean legacy(Client c) {
        return c.node().startsWith("1.20.");
    }

    private static String icon(Client c, String fluid) {
        return legacy(c) ? "hbm:fluid_icon{\"hbm:fluid\":\"" + fluid + "\"}" : "hbm:fluid_icon[hbm:fluid=\"" + fluid + "\"]";
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

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        for (int i = 0; i < 3 && c.screenClass() != null; i++) { c.key("escape"); c.waitTicks(5); }
        c.useFlatWorld();
        Fixtures.platform(c, X, Y, Z, 12);
        c.markLog();

        tooltips(c);
        smokeChain(c);
        toxinMist(c);

        Expect.logCleanSinceMark(c, "(?i)(exception|unregistered hbm:|is not registered).*");
    }

    // ---------------------------------------------------------------- 1: tooltips

    private void tooltips(Client c) {
        c.cmd("clear");
        c.cmd("give @s " + icon(c, "hbm:oil") + " 1");
        c.cmd("give @s " + icon(c, "hbm:phosgene") + " 1");
        c.waitTicks(5);
        c.cmd("gamemode survival");
        c.waitTicks(5);
        c.key("e");
        c.waitScreen("Inventory", 5_000L);
        c.waitTicks(10);

        int[] hotbar = {0, 1};
        String[] names = {"oil", "phosgene"};
        for (int k = 0; k < 2; k++) {
            JsonObject slot = playerSlot(c, hotbar[k]);
            c.mouseMove(slot.get("center_x").getAsDouble(), slot.get("center_y").getAsDouble());
            c.waitTicks(5);
            JsonObject plain = release(c, "mode", "tooltip", "slot", hotbar[k]);
            c.screenshot("1" + k + "-tooltip-" + names[k]);
            c.keyDown("lshift");
            c.waitTicks(5);
            JsonObject shift = release(c, "mode", "tooltip", "slot", hotbar[k]);
            c.screenshot("1" + k + "-tooltip-" + names[k] + "-shift");
            c.keyUp("lshift");
            c.waitTicks(3);

            List<String> p = strings(plain.getAsJsonArray("lines"));
            List<String> s = strings(shift.getAsJsonArray("lines"));
            List<String> ps = strings(plain.getAsJsonArray("styled"));
            List<String> ss = strings(shift.getAsJsonArray("styled"));
            System.out.println(c.node() + " " + names[k] + " plain=" + ps + "\n shift=" + ss);
            Expect.that(p.stream().anyMatch(l -> l.equals("[Polluting]")), names[k] + ": [Polluting] always shown: " + p);
            Expect.that(runs(p, ps, "[Polluting]", "gold"), names[k] + ": [Polluting] is gold: " + ps);
            // 1.7 ModEventHandlerClient appends the QMAW "[ Press F1 for help ]" line after addInformation
            int last = p.size() - 1;
            if (p.get(last).startsWith("[ Press ")) last--;
            final int hint = last;
            Expect.that(p.get(hint).equals("Hold <LSHIFT> to display more info"),
                    names[k] + ": the Shift hint closes the fluid lines: " + p);
            Expect.that(ps.get(hint).startsWith("dark_gray/i:Hold <|yellow/i:LSHIFT|dark_gray/i:>"),
                    names[k] + ": the hint is italic dark grey with a yellow LSHIFT: " + ps.get(hint));
            Expect.that(s.stream().noneMatch(l -> l.startsWith("Hold <")), names[k] + ": no hint with Shift: " + s);
            Expect.that(p.stream().noneMatch(l -> l.equals("When spilled:")), names[k] + ": spill table hidden without Shift: " + p);
            Expect.that(runs(s, ss, "When spilled:", "green"), names[k] + ": green spill heading with Shift: " + ss);
            Expect.that(s.stream().anyMatch(l -> l.matches(" - [0-9.E-]+ (SOOT|POISON|HEAVY METAL) per mB")),
                    names[k] + ": 1.7 per-mB lines with Shift: " + s);
            if (names[k].equals("oil")) {
                Expect.that(runs(s, ss, "When burned:", "red"), "oil: red burn heading with Shift: " + ss);
            } else {
                Expect.that(s.stream().noneMatch(l -> l.equals("When burned:")), "phosgene has no burn table in 1.7: " + s);
                Expect.that(p.stream().noneMatch(l -> l.equals("[Toxin]")), "[Toxin] is hidden without Shift: " + p);
                Expect.that(runs(s, ss, "[Toxin]", "light_purple"), "light purple [Toxin] with Shift: " + ss);
                Expect.that(s.stream().anyMatch(l -> l.startsWith("- ") && l.contains(": 4.0 ")),
                        "phosgene 4.0 DPS line with Shift: " + s);
            }
        }
        c.key("escape");
        c.waitTicks(5);
        c.cmd("gamemode creative");
    }

    private static JsonObject playerSlot(Client c, int inventoryIndex) {
        // the survival inventory menu: hotbar slots are menu indexes 36..44
        JsonObject slot = c.slot(36 + inventoryIndex);
        Expect.that(slot != null, "inventory hotbar slot " + inventoryIndex + " on the screen");
        return slot;
    }

    /** The line whose text is {@code text} exists and every styled run of it has colour {@code colour}. */
    private static boolean runs(List<String> plain, List<String> styled, String text, String colour) {
        int i = plain.indexOf(text);
        if (i < 0) return false;
        for (String run : styled.get(i).split("[|]")) {
            if (!run.startsWith(colour + ":") && !run.startsWith(colour + "/i:")) return false;
        }
        return true;
    }

    private static List<String> strings(JsonArray a) {
        List<String> out = new ArrayList<>();
        if (a != null) for (JsonElement e : a) out.add(e.getAsString());
        return out;
    }

    // ---------------------------------------------------------------- 2: smoke through a duct to a chimney

    private void smokeChain(Client c) {
        c.cmd("clear");
        c.cmd("fill " + (X - 12) + " " + (Y + 1) + " " + (Z - 4) + " " + (X + 6) + " " + (Y + 20) + " " + (Z + 12) + " minecraft:air");
        c.waitTicks(5);
        Fixtures.placeOnTop(c, "hbm:chimney_brick", X - 4, Y, Z + 4);
        c.waitTicks(10);
        JsonObject found = server(c, "mode", "machine", "x", X - 4, "y", L, "z", Z + 4, "radius", 4);
        JsonArray chimneys = found.getAsJsonArray("chimneys");
        Expect.that(chimneys != null && chimneys.size() == 1, "one brick chimney core placed: " + found);
        JsonObject core = chimneys.get(0).getAsJsonObject();
        int cx = core.get("x").getAsInt(), cy = core.get("y").getAsInt(), cz = core.get("z").getAsInt();

        // furnace three blocks east of the core, a smoke duct between (the chimney's east port is core + 2)
        int fx = cx + 3, fz = cz;
        Fixtures.setBlock(c, fx, cy, fz, "hbm:machine_difurnace_off");
        // the duct is typed by hand, as a player does: right-click with a fluid identifier set to smoke. (A duct typed by
        // /setblock NBT keeps an untyped graph cell: the NBT is applied after the block entity joined the graph.)
        Fixtures.setBlock(c, cx + 2, cy, cz, "hbm:fluid_duct_neo");
        c.cmd("clear");
        c.cmd("give @s " + (legacy(c) ? "hbm:fluid_identifier{\"hbm:fluid\":\"hbm:smoke\"}" : "hbm:fluid_identifier[hbm:fluid=\"hbm:smoke\"]") + " 1");
        c.hotbar(1);
        c.teleport(cx + 2.5, cy, cz - 2.5);
        c.waitTicks(5);
        c.lookAt(cx + 2.5, cy + 0.5, cz + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(10);
        JsonObject duct = c.blockEntity(cx + 2, cy, cz);
        Expect.that(duct.toString().contains("hbm:smoke"), "the identifier typed the duct smoke: " + duct);
        JsonObject zero = new JsonObject();
        zero.addProperty("set_soot", 0);
        c.query("hbm_hooks", zero); // earlier runs left soot in this region; start the measurement at zero
        c.waitTicks(2);
        float sootBefore = server(c, "mode", "machine", "x", fx, "y", cy, "z", fz, "radius", 0).get("soot").getAsFloat();

        loadFurnace(c, fx, cy, fz);
        c.screenshot("20-smoke-chain-loaded");
        int maxSmoke = 0;
        int onTicksSeen = 0;
        for (int i = 0; i < 12; i++) {
            c.waitTicks(40);
            JsonObject m = server(c, "mode", "machine", "x", fx, "y", cy, "z", fz, "radius", 4);
            int smoke = m.getAsJsonArray("smoke").get(0).getAsJsonObject().get("fill").getAsInt();
            maxSmoke = Math.max(maxSmoke, smoke);
            for (JsonElement e : m.getAsJsonArray("chimneys")) {
                onTicksSeen = Math.max(onTicksSeen, e.getAsJsonObject().get("on_ticks").getAsInt());
            }
        }
        c.teleport(fx + 6.5, L + 3, fz - 6.5);
        c.lookAt(cx + 0.5, cy + 8.0, cz + 0.5);
        c.waitTicks(20);
        c.screenshot("21-smoke-chain-chimney");
        float sootAfter = server(c, "mode", "machine", "x", fx, "y", cy, "z", fz, "radius", 0).get("soot").getAsFloat();
        System.out.println(c.node() + " smoke chain: maxSmoke=" + maxSmoke + " onTicks=" + onTicksSeen
                + " soot " + sootBefore + " -> " + sootAfter);
        Expect.that(onTicksSeen > 0, "the chimney received smoke through the duct (onTicks > 0)");
        Expect.that(maxSmoke < 50, "the furnace's 50 mB smoke buffer is drained by the duct, max seen " + maxSmoke);
        Expect.that(sootAfter > sootBefore, "the chimney releases soot into the region: " + sootBefore + " -> " + sootAfter);

        // break the duct: the buffer fills and overflows with the 1.7 fizz
        c.cmd("setblock " + (cx + 2) + " " + cy + " " + cz + " minecraft:air");
        c.setOption("sound_capture", "true");
        c.markLog();
        int full = 0;
        for (int i = 0; i < 20 && full < 50; i++) {
            c.waitTicks(40);
            full = server(c, "mode", "machine", "x", fx, "y", cy, "z", fz, "radius", 0)
                    .getAsJsonArray("smoke").get(0).getAsJsonObject().get("fill").getAsInt();
        }
        Expect.that(full == 50, "without a duct the smoke buffer fills to 50 mB: " + full);
        List<String> fizz = new ArrayList<>();
        for (int i = 0; i < 15 && fizz.isEmpty(); i++) {
            c.waitTicks(20);
            fizz = c.logSinceMark("harness-sound.*id=minecraft:block.fire.extinguish");
        }
        c.setOption("sound_capture", "false");
        Expect.that(!fizz.isEmpty(), "the overflow plays 1.7's random.fizz (block.fire.extinguish)");
        System.out.println(c.node() + " fizz: " + fizz.get(0));
        c.cmd("setblock " + fx + " " + cy + " " + fz + " minecraft:air");
        c.teleport(X + 0.5, L, Z + 0.5);
    }

    private void loadFurnace(Client c, int x, int y, int z) {
        c.cmd("clear");
        c.cmd("give @s minecraft:iron_ingot 16");
        c.cmd("give @s minecraft:coal 16");
        c.cmd("give @s minecraft:coal_block 4");
        c.waitTicks(5);
        c.teleport(x + 0.5, y, z - 2.5);
        c.waitTicks(3);
        Fixtures.interact(c, x, y, z);
        c.waitTicks(10);
        Expect.that(c.screenClass() != null, "the furnace GUI opened");
        int[] targets = {0, 1, 2};
        String[] items = {"minecraft:iron_ingot", "minecraft:coal", "minecraft:coal_block"};
        for (int k = 0; k < 3; k++) {
            int from = findPlayerSlot(c, items[k]);
            Expect.that(from >= 0, "player holds " + items[k]);
            c.clickSlot(from);
            c.waitTicks(3);
            c.clickSlot(targets[k]);
            c.waitTicks(3);
        }
        c.waitTicks(10);
        c.screenshot("19-difurnace-gui");
        for (int k = 0; k < 2; k++) { // the fuel slot empties at once into the furnace's internal fuel (1.7 loadFuel)
            JsonObject slot = c.slot(targets[k]);
            Expect.that(slot != null && !slot.get("empty").getAsBoolean(), "furnace slot " + targets[k] + " loaded: " + slot);
        }
        c.key("escape");
        c.waitTicks(5);
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

    // ---------------------------------------------------------------- 3: phosgene mist

    private void toxinMist(Client c) {
        c.teleport(X + 0.5, L, Z + 0.5);
        c.cmd("kill @e[type=minecraft:pig]");
        c.cmd("summon minecraft:pig " + (X + 6) + " " + L + " " + Z + " {NoAI:1b,Health:10f,Tags:[\"hbm_toxin\"]}");
        c.waitTicks(5);
        c.cmd("summon hbm:entity_mist " + (X + 6) + " " + L + " " + Z + " {type:\"hbm:phosgene\",width:4f,height:2f}");
        c.lookAt(X + 6.5, L + 0.5, Z + 0.5);
        float health = 10F;
        for (int i = 0; i < 6; i++) {
            c.waitTicks(10);
            JsonObject living = c.query("hbm_living", new JsonObject());
            for (JsonElement e : living.getAsJsonArray("living")) {
                JsonObject o = e.getAsJsonObject();
                if (o.get("type").getAsString().equals("minecraft:pig")) {
                    health = o.get("alive").getAsBoolean() ? o.get("health").getAsFloat() : 0F;
                }
            }
            if (i == 2) c.screenshot("30-phosgene-mist");
        }
        System.out.println(c.node() + " pig in phosgene: health " + health);
        Expect.that(health < 10F, "the phosgene mist (FT_Toxin, 4 per 20 ticks x intensity) hurt the pig: " + health);
        c.cmd("kill @e[type=hbm:entity_mist]");
        c.cmd("kill @e[type=minecraft:pig]");
    }
}
