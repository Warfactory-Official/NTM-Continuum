import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gap batch B (§19ed-gaps-b), every item exercised in game and checked by value, with screenshots:
 * <ol>
 *   <li>BossSpawnHandler through its real tick: the mask man (crystallizer placed, 100 RAD, underground, warning then
 *       spawn), an FBI raid (15 agents + 5 drones) and the FBI mark from using the missile assembly, the radiation beast
 *       pack after a meltdown mark (1 leader of 10), a digamma ghost;</li>
 *   <li>reputation: the old lantern repaired with a blowtorch (+1, horns, Bobmazon supplies with alexandrite at 10+),
 *       destroyed (-1); the GOOD_KARMA pedestal ritual refused at 0 and performed at 12;</li>
 *   <li>a napalm bomblet on a gasoline tank and on a refinery: blown open, burning, exploded models, inferno granted,
 *       no GUI while torn, blowtorch repair;</li>
 *   <li>/ntmserver keys: CRATE_OPEN_HELD (a crate opened from the hand, contents on the stack, sneak places it with them),
 *       CRATE_KEEP_CONTENTS off (contents spill), TAINT_TRAILS, ITEM_HAZARD_DROP_TICKRATE, ENABLE_MKU,
 *       DAMAGE_COMPATIBILITY_MODE (1.7 knockback);</li>
 *   <li>1.7 tile data in generated structures: radio house (armed mines, bobblehead, potted plants), oil rig (torn tank),
 *       lighthouse (the logbook in the skeleton's hands); no "not migrated" line for those ids;</li>
 *   <li>pipe wrench linking two anchors: chat, links, one network, the pipe drawn;</li>
 *   <li>the radar map over chunks that are saved but unloaded (seeded world), equal to their saved height maps;</li>
 *   <li>the RF converter seen by the foreign energy system (Team Reborn Energy on Fabric).</li>
 * </ol>
 * Each section records PASS/FAIL in items.txt next to the screenshots; the scenario fails if any section failed.
 */
public final class FinalGapsB implements Scenario {

    private static final int X = 0;
    private static final int Y = 100;
    private static final int Z = 0;

    private interface Step { void run() throws Exception; }

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.resetWorld();
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 30);
        List<String> results = new ArrayList<>();

        step(c, results, "4 /ntmserver keys", () -> serverKeys(c));
        step(c, results, "4b CRATE_OPEN_HELD held crate", () -> heldCrate(c));
        step(c, results, "3 tank + refinery explosions, inferno", () -> explosions(c));
        step(c, results, "2 reputation: lantern + karma pedestal", () -> reputation(c));
        step(c, results, "6 pipe wrench linking", () -> pipes(c));
        step(c, results, "8 RF converter foreign energy", () -> energy(c));
        step(c, results, "5 structure tile data", () -> structures(c));
        step(c, results, "1 BossSpawnHandler spawns", () -> bossSpawns(c));
        step(c, results, "7 radar over saved unloaded chunks (seeded)", () -> radar(c));

        Path out = c.screenshotDir().resolve("items.txt");
        Files.createDirectories(out.getParent());
        Files.write(out, results);
        for (String r : results) System.out.println("[FinalGapsB " + c.node() + "] " + r);
        List<String> errors = c.logSinceMark("/(ERROR|FATAL)[]]|Missing model|Failed to load texture|Unable to load model");
        c.useFlatWorld();
        c.resetWorld();
        long failed = results.stream().filter(r -> r.startsWith("FAIL")).count();
        Expect.that(failed == 0, failed + " section(s) failed: " + results);
        Expect.that(errors.isEmpty(), "log errors: " + errors);
    }

    private static void step(Client c, List<String> results, String name, Step body) {
        try {
            body.run();
            results.add("PASS " + name);
        } catch (Throwable t) {
            results.add("FAIL " + name + ": " + t.getMessage());
            try {
                c.screenshot("fail-" + name.substring(0, 2).trim());
                if (c.screenClass() != null) c.key("escape");
                c.releaseAll();
                c.cmd("gamemode creative");
                c.waitTicks(5);
            } catch (Throwable ignored) { }
        }
    }

    // ============================================================== helpers

    private static JsonObject q(Client c, String op, Object... kv) {
        JsonObject p = new JsonObject();
        p.addProperty("op", op);
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else if (v instanceof Boolean b) p.addProperty((String) kv[i], b);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        JsonObject r = c.query("hbm_gapsb", p);
        Expect.that(!r.has("error"), "query " + op + " failed: " + r);
        return r;
    }

    private static boolean chatSince(Client c, String text) {
        return !c.logSinceMark(java.util.regex.Pattern.quote(text)).isEmpty();
    }

    /** Places a (multiblock) item on top of (x, y, z) by hand from {@code dist} blocks south-east, outside its footprint. */
    private static void placeFar(Client c, String item, int x, int y, int z, int dist) {
        Fixtures.giveOnly(c, item, 1);
        c.teleport(x + dist + 0.5, y + 1, z + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, y + 0.98, z + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
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

    // ============================================================== 4: /ntmserver keys

    private static void serverKeys(Client c) {
        // ITEM_HAZARD_DROP_TICKRATE drives the dropped-item hazard cadence
        c.cmd("ntmserver set ITEM_HAZARD_DROP_TICKRATE 5");
        c.waitTicks(2);
        Expect.that(q(c, "config").get("hazardTickRate").getAsInt() == 5, "ITEM_HAZARD_DROP_TICKRATE bound to the hazard tick");
        c.cmd("ntmserver set ITEM_HAZARD_DROP_TICKRATE 2");

        // ENABLE_MKU: an infected player is healthy while the key is off
        c.cmd("ntmserver set ENABLE_MKU false");
        c.waitTicks(2);
        Expect.that(!q(c, "config").get("mku").getAsBoolean() && q(c, "props").get("contagion").getAsInt() == 0,
                "ENABLE_MKU off: no contagion");
        c.cmd("ntmserver set ENABLE_MKU true");

        // TAINT_TRAILS: a tainted cow on stone leaves taint below it
        int tx = X + 8, tz = Z - 8;
        c.cmd("fill " + (tx - 1) + " " + Y + " " + (tz - 1) + " " + (tx + 1) + " " + Y + " " + (tz + 1) + " minecraft:stone");
        c.cmd("ntmserver set TAINT_TRAILS true");
        c.cmd("summon minecraft:cow " + tx + ".5 " + (Y + 1) + " " + tz + ".5");
        c.waitTicks(4);
        c.cmd("effect give @e[type=minecraft:cow,distance=..40] hbm:taint 30 0");
        int taint = 0;
        for (int i = 0; i < 20 && taint == 0; i++) {
            c.waitTicks(5);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (c.block(tx + dx, Y, tz + dz).get("block").getAsString().equals("hbm:taint")) taint++;
            }
        }
        worldShot(c, "40-taint-trail", tx + 3.5, Y + 3, tz + 3.5, tx + 0.5, Y, tz + 0.5);
        Expect.that(taint > 0, "TAINT_TRAILS: the tainted cow left taint on the stone");
        c.cmd("ntmserver set TAINT_TRAILS false");
        c.cmd("kill @e[type=minecraft:cow]");

        // DAMAGE_COMPATIBILITY_MODE: 1.7's super-compatibility knockback (0.4 x multiplier)
        c.cmd("difficulty easy"); // a zombie does not survive peaceful
        c.cmd("summon minecraft:zombie " + (X + 3) + " " + (Y + 1) + " " + Z + " {NoAI:1b}");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(5);
        c.cmd("ntmserver set DAMAGE_COMPATIBILITY_MODE false");
        JsonObject plain = q(c, "damage", "kb", 2.0);
        c.cmd("ntmserver set DAMAGE_COMPATIBILITY_MODE true");
        c.waitTicks(2);
        JsonObject compat = q(c, "damage", "kb", 2.0);
        c.cmd("ntmserver set DAMAGE_COMPATIBILITY_MODE false");
        c.cmd("kill @e[type=minecraft:zombie]");
        c.cmd("difficulty peaceful");
        double hp = Math.hypot(plain.get("vx").getAsDouble(), plain.get("vz").getAsDouble());
        double hc = Math.hypot(compat.get("vx").getAsDouble(), compat.get("vz").getAsDouble());
        Expect.that(hc > 0.7 && hc < 0.9, "compat mode applies 1.7 knockback 0.4 x 2 horizontally: " + compat);
        Expect.that(hp < 0.6, "default mode keeps vanilla knockback: " + plain);

        // CRATE_KEEP_CONTENTS off: the broken crate spills its contents
        int cx = X - 8, cz = Z - 8;
        c.cmd("ntmserver set CRATE_KEEP_CONTENTS false");
        c.cmd("clear");
        Fixtures.setBlock(c, cx, Y + 1, cz, "hbm:crate_iron");
        c.cmd("item replace block " + cx + " " + (Y + 1) + " " + cz + " container.0 with minecraft:diamond 5");
        c.teleport(cx + 2.5, Y + 1, cz + 0.5);
        c.waitTicks(3);
        c.lookAt(cx + 0.5, Y + 1.5, cz + 0.5);
        c.waitTicks(2);
        c.attack(2);
        c.waitTicks(10);
        boolean diamonds = false;
        for (JsonElement e : c.entities(8)) {
            JsonObject o = e.getAsJsonObject();
            // the entity list names an item entity by its stack ("Diamond"), not by the item id
            if (o.get("type").getAsString().equals("minecraft:item") && o.get("name").getAsString().contains("Diamond")) diamonds = true;
        }
        // the player stands two blocks away and may already have picked the spilled stacks up; in creative the crate
        // itself drops nothing, so a diamond in the inventory also came out of the broken crate
        if (c.inventory().toString().contains("minecraft:diamond")) diamonds = true;
        Expect.that(c.block(cx, Y + 1, cz).get("block").getAsString().equals("minecraft:air"), "crate broken");
        c.screenshot("41-crate-spilled");
        Expect.that(diamonds, "CRATE_KEEP_CONTENTS off: the diamonds spilled as items");
        c.cmd("ntmserver set CRATE_KEEP_CONTENTS true");
        c.cmd("kill @e[type=minecraft:item]");
    }

    // ============================================================== 4b: crate opened from the hand

    private static void heldCrate(Client c) {
        c.cmd("ntmserver set CRATE_OPEN_HELD true");
        c.cmd("clear");
        c.cmd("give @s minecraft:diamond 3");
        c.cmd("give @s hbm:crate_iron 1");
        c.waitTicks(3);
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, -60); // at the sky: nothing to click
        c.hotbar(2);
        c.waitTicks(2);
        c.use(2);
        c.waitScreen("Crate", 5000);
        c.waitTicks(5);
        c.screenshot("42-held-crate-open");
        JsonObject screen = c.screen();
        int diamondSlot = -1;
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && s.get("id").getAsString().equals("minecraft:diamond")) {
                diamondSlot = s.get("index").getAsInt();
            }
        }
        Expect.that(diamondSlot >= 0, "diamonds visible in the held crate's player inventory");
        c.slotClick(diamondSlot, 0, "QUICK_MOVE");
        c.waitTicks(5);
        c.screenshot("43-held-crate-filled");
        c.key("escape");
        c.waitTicks(5);
        JsonObject crate = q(c, "crate");
        Expect.that(crate.get("count").getAsInt() == 1 && crate.toString().contains("minecraft:diamond x3"),
                "the diamonds are on the crate stack: " + crate);
        // sneaking places it, with its contents
        Fixtures.setBlock(c, X + 2, Y, Z, "minecraft:stone");
        c.teleport(X + 4.5, Y + 1, Z + 0.5);
        c.waitTicks(3);
        c.lookAt(X + 2.5, Y + 0.98, Z + 0.5);
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(4);
        c.keyUp("key.sneak");
        c.waitTicks(3);
        String placed = c.block(X + 2, Y + 1, Z).get("block").getAsString();
        Expect.that(placed.equals("hbm:crate_iron"), "sneak places the crate: " + placed);
        String nbt = c.blockEntity(X + 2, Y + 1, Z).toString();
        Expect.that(nbt.contains("diamond"), "and it holds the diamonds: " + nbt);
    }

    // ============================================================== 3: explosions

    private static void explosions(Client c) {
        int tx = X + 14, tz = Z + 10;
        c.cmd("advancement revoke @s only hbm:inferno");
        placeFar(c, "hbm:machine_fluidtank", tx, Y, tz, 4);
        c.waitTicks(5);
        JsonObject tank = q(c, "machine", "x", tx, "y", Y + 1, "z", tz, "fillWith", "GASOLINE", "amount", 200000);
        Expect.that(tank.get("present").getAsBoolean(), "tank placed: " + tank);
        c.teleport(tx + 0.5, Y + 1, tz - 12.5);
        c.waitTicks(3);
        // released inside the tank's bottom cell: the 1.7 bomblet centres its blast 1.5 above the block it enters, so one
        // dropped onto the roof explodes in the air above it and only its luckiest rays reach the resistance-12 shell
        q(c, "bomblet", "x", tx, "y", Y + 1, "z", tz, "t", 1);
        JsonObject after = null;
        for (int i = 0; i < 30; i++) {
            c.waitTicks(2);
            after = q(c, "machine", "x", tx, "y", Y + 1, "z", tz);
            if (after.has("exploded") && after.get("exploded").getAsBoolean()) break;
        }
        Expect.that(after.get("exploded").getAsBoolean() && after.get("onFire").getAsBoolean(),
                "the napalm bomblet blew the gasoline tank open and set it on fire: " + after);
        Expect.that(q(c, "advancement", "id", "hbm:inferno").get("done").getAsBoolean(), "inferno granted (tank)");
        c.waitTicks(40);
        int fill = q(c, "machine", "x", tx, "y", Y + 1, "z", tz).get("fill").getAsInt();
        Expect.that(fill < 200000, "the torn tank leaks: " + fill);
        worldShot(c, "30-tank-burning", tx + 7.5, Y + 5, tz - 6.5, tx + 0.5, Y + 1.5, tz + 0.5);
        // no GUI while torn
        // the blast tore the platform beside the tank open and left fire on it: floor and air again where the player stands
        c.cmd("fill " + (tx + 2) + " " + Y + " " + (tz - 3) + " " + (tx + 5) + " " + Y + " " + (tz + 3) + " minecraft:stone");
        c.cmd("fill " + (tx + 2) + " " + (Y + 1) + " " + (tz - 3) + " " + (tx + 5) + " " + (Y + 3) + " " + (tz + 3) + " minecraft:air");
        c.teleport(tx + 3.5, Y + 1, tz + 0.5);
        Fixtures.interact(c, tx + 1, Y + 1, tz);
        c.waitTicks(5);
        Expect.that(c.screenClass() == null || !c.screenClass().contains("Tank"), "a torn tank opens no GUI: " + c.screenClass());
        // blowtorch repair (six steel plates)
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:blowtorch 1");
        c.cmd("give @s hbm:plate_steel 6");
        c.waitTicks(3);
        c.hotbar(1);
        c.lookAt(tx + 1.5, Y + 1.5, tz + 0.5);
        c.waitTicks(3);
        c.screenshot("31-tank-repair-overlay");
        c.use(2);
        c.waitTicks(5);
        c.cmd("gamemode creative");
        JsonObject repaired = q(c, "machine", "x", tx, "y", Y + 1, "z", tz);
        Expect.that(!repaired.get("exploded").getAsBoolean(), "the blowtorch repaired the tank: " + repaired);

        // the refinery
        int rx = X - 14, rz = Z + 10;
        c.cmd("advancement revoke @s only hbm:inferno");
        placeFar(c, "hbm:machine_refinery", rx, Y, rz, 3);
        c.waitTicks(5);
        q(c, "machine", "x", rx, "y", Y + 1, "z", rz, "amount", 20000);
        c.teleport(rx + 0.5, Y + 1, rz - 14.5);
        c.waitTicks(3);
        q(c, "bomblet", "x", rx, "y", Y + 1, "z", rz, "t", 1);
        JsonObject ref = null;
        for (int i = 0; i < 40; i++) {
            c.waitTicks(2);
            ref = q(c, "machine", "x", rx, "y", Y + 1, "z", rz);
            if (ref.has("exploded") && ref.get("exploded").getAsBoolean()) break;
        }
        Expect.that(ref.get("exploded").getAsBoolean() && ref.get("onFire").getAsBoolean(), "the refinery blew open: " + ref);
        Expect.that(q(c, "advancement", "id", "hbm:inferno").get("done").getAsBoolean(), "inferno granted (refinery)");
        c.waitTicks(20);
        worldShot(c, "32-refinery-burning", rx + 8.5, Y + 6, rz - 8.5, rx + 0.5, Y + 4, rz + 0.5);
        c.cmd("advancement grant @s only hbm:inferno");
        c.key("l");
        c.waitTicks(20);
        c.screenshot("33-advancements");
        c.key("escape");
        c.waitTicks(3);
    }

    // ============================================================== 2: reputation

    private static void reputation(Client c) {
        int lx = X + 6, lz = Z + 20;
        q(c, "set", "reputation", 0);
        Fixtures.placeOnTop(c, "hbm:lantern_behemoth", lx, Y, lz);
        c.waitTicks(5);
        JsonObject lantern = q(c, "lantern", "x", lx, "y", Y + 1, "z", lz, "broken", true);
        Expect.that(lantern.has("broken") && lantern.get("broken").getAsBoolean(), "lantern placed and broken: " + lantern);
        c.cmd("gamemode survival");
        c.cmd("clear");
        c.cmd("give @s hbm:blowtorch 1");
        c.cmd("give @s hbm:plate_steel 2");
        c.cmd("give @s hbm:circuit_basic 1");
        c.waitTicks(3);
        c.teleport(lx + 2.5, Y + 1, lz + 0.5);
        c.hotbar(1);
        c.lookAt(lx + 0.5, Y + 2.5, lz + 0.5);
        c.waitTicks(3);
        c.screenshot("20-lantern-broken-overlay");
        c.use(2);
        c.waitTicks(5);
        c.cmd("gamemode creative");
        JsonObject props = q(c, "props");
        Expect.that(props.get("reputation").getAsInt() == 1, "a repair earns one reputation: " + props);
        JsonObject after = q(c, "lantern", "x", lx, "y", Y + 1, "z", lz);
        Expect.that(!after.get("broken").getAsBoolean() && after.get("comTimer").getAsInt() > 0, "repaired and calling: " + after);
        worldShot(c, "21-lantern-repaired", lx + 4.5, Y + 3, lz - 3.5, lx + 0.5, Y + 3, lz + 0.5);

        // good reputation: the supplies bring alexandrite
        q(c, "set", "reputation", 12);
        q(c, "lantern", "x", lx, "y", Y + 1, "z", lz, "broken", true);
        c.cmd("give @s hbm:plate_steel 2");
        c.cmd("give @s hbm:circuit_basic 1");
        c.cmd("gamemode survival");
        c.teleport(lx + 2.5, Y + 1, lz + 0.5);
        c.hotbar(1);
        c.lookAt(lx + 0.5, Y + 2.5, lz + 0.5);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(5);
        c.cmd("gamemode creative");
        boolean alexandrite = false;
        for (int i = 0; i < 30 && !alexandrite; i++) {
            c.waitTicks(20);
            alexandrite = q(c, "shuttle").get("alexandrite").getAsBoolean();
        }
        Expect.that(alexandrite, "at reputation >= 10 the Bobmazon supplies carry alexandrite");
        Expect.that(q(c, "props").get("reputation").getAsInt() == 13, "second repair: 13");

        // destroying the lantern costs everyone near one reputation
        c.cmd("setblock " + lx + " " + (Y + 1) + " " + lz + " minecraft:air");
        c.waitTicks(5);
        Expect.that(q(c, "props").get("reputation").getAsInt() == 12, "a destroyed lantern costs one: " + q(c, "props"));

        // GOOD_KARMA pedestal ritual
        int px = X - 6, pz = Z + 22;
        int[][] ring = {{0, 0}, {-2, -2}, {0, -3}, {2, -2}, {-3, 0}, {3, 0}, {-2, 2}, {0, 3}, {2, 2}};
        for (int[] r : ring) Fixtures.setBlock(c, px + r[0], Y + 1, pz + r[1], "hbm:pedestal");
        q(c, "set", "reputation", 0);
        q(c, "pedestal", "x", px, "y", Y + 1, "z", pz, "fill", true);
        c.teleport(px + 0.5, Y + 1, pz + 5.5);
        c.waitTicks(3);
        Fixtures.setBlock(c, px, Y + 2, pz, "minecraft:redstone_block");
        c.waitTicks(5);
        String before = q(c, "pedestal", "x", px, "y", Y + 1, "z", pz).get("center").getAsString();
        Expect.that(!before.contains("gun_aberrator_eott"), "reputation 0: the karma ritual refuses: " + before);
        Fixtures.setBlock(c, px, Y + 2, pz, "minecraft:air");
        q(c, "set", "reputation", 12);
        Fixtures.setBlock(c, px, Y + 2, pz, "minecraft:redstone_block");
        c.waitTicks(5);
        String center = q(c, "pedestal", "x", px, "y", Y + 1, "z", pz).get("center").getAsString();
        Fixtures.setBlock(c, px, Y + 2, pz, "minecraft:air");
        worldShot(c, "22-karma-pedestal", px + 5.5, Y + 4, pz + 6.5, px + 0.5, Y + 1.5, pz + 0.5);
        Expect.that(center.contains("gun_aberrator_eott"), "reputation 12: the GOOD_KARMA ritual forges the EOTT: " + center);
    }

    // ============================================================== 6: pipes

    private static void pipes(Client c) {
        int ax = X - 20, az = Z - 20, bx = ax + 7;
        c.cmd("gamemode creative");
        Fixtures.setBlock(c, ax, Y + 1, az, "hbm:pipe_anchor[facing=up]");
        Fixtures.setBlock(c, bx, Y + 1, az, "minecraft:stone");
        Fixtures.setBlock(c, bx, Y + 2, az, "minecraft:stone");
        Fixtures.setBlock(c, bx, Y + 3, az, "hbm:pipe_anchor[facing=up]");
        q(c, "anchorFluid", "x", ax, "y", Y + 1, "z", az, "fluid", "WATER");
        Fixtures.giveOnly(c, "hbm:wrench", 1);
        c.markLog();
        c.teleport(ax + 3.5, Y + 1, az + 2.5);
        c.waitTicks(3);
        c.lookAt(ax + 0.5, Y + 1.4, az + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        c.lookAt(bx + 0.5, Y + 3.4, az + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(6);
        Expect.that(chatSince(c, "Pipe start") && chatSince(c, "Pipe end"), "wrench chat: start, end");
        JsonObject a = q(c, "anchor", "x", ax, "y", Y + 1, "z", az);
        JsonObject b = q(c, "anchor", "x", bx, "y", Y + 3, "z", az);
        Expect.that(a.getAsJsonArray("links").size() == 1 && b.getAsJsonArray("links").size() == 1, "linked both ways: " + a + b);
        Expect.that(b.get("fluid").getAsString().equals("WATER"), "the fluid-less anchor took water: " + b);
        Expect.that(a.get("network").getAsInt() != 0 && a.get("network").getAsInt() == b.get("network").getAsInt(),
                "one fluid network over the pipe: " + a + b);
        worldShot(c, "50-pipeline", ax + 3.5, Y + 3, az - 5.5, ax + 3.5, Y + 2, az + 0.5);
        // a second anchor 12 m away is out of reach
        Fixtures.setBlock(c, ax, Y + 1, az + 12, "hbm:pipe_anchor[facing=up]");
        // worldShot left the empty slot 9 selected, and a slot change made while the switch back from spectator is still
        // in flight is dropped: wait for creative, then select the wrench
        c.waitTicks(5);
        c.hotbar(1);
        c.waitTicks(3);
        c.markLog();
        c.teleport(ax + 2.5, Y + 1, az + 0.5);
        c.waitTicks(3);
        c.lookAt(ax + 0.5, Y + 1.4, az + 0.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(3);
        c.teleport(ax + 2.5, Y + 1, az + 12.5);
        c.waitTicks(3);
        c.lookAt(ax + 0.5, Y + 1.4, az + 12.5);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(4);
        Expect.that(chatSince(c, "Pipe anchor is too far away"), "12.0 m: too far for 10 m pipes");
    }

    // ============================================================== 8: RF converter

    private static void energy(Client c) {
        int ex = X + 20, ez = Z - 20;
        Fixtures.setBlock(c, ex, Y + 1, ez, "hbm:machine_converter_he_rf");
        c.waitTicks(3);
        JsonObject e = q(c, "energy", "x", ex, "y", Y + 1, "z", ez);
        boolean fabric = c.node().contains("fabric");
        if (fabric) {
            Expect.that(e.get("reborn").getAsBoolean(), "Team Reborn Energy loaded in the Fabric dev game: " + e);
        }
        Expect.that(e.get("supported").getAsBoolean() && e.get("found").getAsBoolean(),
                "the HE->RF converter is a foreign energy handler (" + (fabric ? "TR Energy" : "FE") + "): " + e);
    }

    // ============================================================== 5: structures

    private static void structures(Client c) {
        c.markLog();
        JsonObject radio = placeAndScan(c, "hbm:radio", X + 200, "60-radio-house", 40);
        Expect.that(radio.get("mines").getAsInt() > 0 && radio.get("primed").getAsInt() > 0,
                "radio house: 1.7 landmines arrive armed: " + radio);
        Expect.that(radio.get("bobbles").getAsInt() > 0, "radio house: bobblehead placed: " + radio);
        Expect.that(radio.get("pots").getAsInt() > 0, "radio house: the 1.7 flower pots hold their plants: " + radio);
        JsonObject rig = placeAndScan(c, "hbm:oil_rig", X + 400, "61-oil-rig", 40);
        Expect.that(rig.get("explodedTanks").getAsInt() > 0, "oil rig: the 1.7 torn fluid tank stays torn: " + rig);
        JsonObject light = placeAndScan(c, "hbm:lighthouse", X + 600, "62-lighthouse", 80);
        Expect.that(light.get("books").getAsInt() > 0, "lighthouse: the skeleton holds the logbook: " + light);
        List<String> unmigrated = c.logSinceMark("tile entity data for .* is not migrated");
        for (String line : unmigrated) {
            Expect.that(!line.contains("tileentity_landmine") && !line.contains("tileentity_ntm_bobblehead")
                    && !line.contains("tileentity_ntm_skeleton") && !line.contains("tileentity_fluid_tank")
                    && !line.contains("FlowerPot"), "migrated ids must not be reported: " + line);
        }
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(10);
    }

    /** /place structure in the flat world (it lands on the flat surface), then a scan of its column and a screenshot. */
    /**
     * {@code r}: how far the scan reads block entities. The lighthouse's box starts at the corner of the target's chunk
     * and spans 66 x 65 blocks, and its rotation follows the world seed, which the superflat worlds do not share (each
     * node's was created with its own random seed), so the skeleton sits at a different spot on every node. Same seed,
     * same box on all nodes: seeded world, /place 1000 64 1000 gives chunk 62,62, rotation 3, 992,29,992..1057,100,1056 on
     * 26.1 NeoForge, 1.21.1 NeoForge and 1.20.1 Forge (WorldgenModdedBiomes, History §19eg-worldgen).
     */
    private static JsonObject placeAndScan(Client c, String id, int sx, String shot, int r) {
        c.cmd("gamemode spectator");
        c.teleport(sx, -40, 0);
        c.waitTicks(40);
        c.cmd("place structure " + id + " " + sx + " -60 0");
        c.waitTicks(60);
        JsonObject scan = q(c, "scan", "x", sx, "y", -60, "z", 0, "r", r, "y0", -64, "y1", 60);
        int ty = -58, tx = sx, tz = 0;
        if (scan.has("at")) {
            String[] at = scan.get("at").getAsString().split(",");
            tx = Integer.parseInt(at[0]);
            ty = Integer.parseInt(at[1]);
            tz = Integer.parseInt(at[2]);
        }
        c.teleport(tx - 14.5, ty + 12, tz - 14.5);
        c.waitTicks(20);
        c.lookAt(tx + 0.5, ty, tz + 0.5);
        c.waitTicks(10);
        c.screenshot(shot);
        return scan;
    }

    // ============================================================== 1: boss spawns

    private static void bossSpawns(Client c) {
        try {
            bossSpawnsBody(c);
        } finally {
            // 1.7 MobConfig defaults again, even after a failed check, so no raid follows the player into section 7
            q(c, "set", "enableRaids", false, "raidDelay", 30 * 60 * 60, "raidChance", 3, "raidAttackDistance", 32,
                    "elementalDelay", 30 * 60 * 60, "elementalChance", 2, "maskmanDelay", 1200, "digamma", 0.0);
            q(c, "mobs", "clear", true);
            c.cmd("ntmrad clear");
            q(c, "set", "rads", 0.0);
            c.cmd("difficulty peaceful");
        }
    }

    private static void bossSpawnsBody(Client c) {
        // 1.7's rolls ignore the game mode; creative keeps the player alive next to ten radiation beasts and 100 RAD
        c.cmd("difficulty easy");
        c.cmd("gamemode creative");
        q(c, "mobs", "clear", true);
        c.cmd("clear");
        // the mask man: crystallizer placed, 100 RAD, three blocks under the height map
        placeFar(c, "hbm:machine_crystallizer", X + 10, Y, Z - 14, 4);
        c.cmd("gamemode creative");
        int px = X - 12, pz = Z - 12;
        Fixtures.setBlock(c, px, Y - 6, pz, "minecraft:stone");
        c.cmd("setblock " + px + " " + Y + " " + pz + " minecraft:stone");
        c.teleport(px + 0.5, Y - 5, pz + 0.5);
        // 100 RAD on the player (1.7 getRads), not on the chunk: /ntmrad's chunk radiation spreads and kills the player
        q(c, "set", "rads", 100.0);
        // the roll resets the timer on any second the three conditions do not hold: start counting only once they do
        boolean ready = false;
        for (int i = 0; i < 20 && !ready; i++) {
            c.waitTicks(5);
            ready = q(c, "maskman").get("conditions").getAsBoolean();
        }
        Expect.that(ready, "mask man conditions (acidizer placed, 50+ RAD, underground): " + q(c, "maskman"));
        c.markLog();
        q(c, "set", "maskmanDelay", 70, "maskManTimer", 5);
        c.waitTicks(160);
        Expect.that(chatSince(c, "The mask man draws near."), "mask man warning at delay - 60 s: " + q(c, "maskman"));
        q(c, "set", "maskManTimer", 67);
        c.waitTicks(80);
        Expect.that(chatSince(c, "The mask man is about to claim another victim.")
                || chatSince(c, "Seems like mask man couldn't come today."), "mask man spawn roll at maskmanDelay");
        // gaussian-20 around the player on the height map: off the 30-block platform that is the flat ground far below
        JsonObject mobs = q(c, "mobs", "r", 400);
        Expect.that(mobs.get("maskmen").getAsInt() >= 1 || chatSince(c, "couldn't come"), "a mask man: " + mobs);
        // /ntmrad sets the chunk's radiation, which spreads: clear the whole system and the player's dose
        c.cmd("ntmrad clear");
        q(c, "set", "maskmanDelay", 1200, "rads", 0.0);
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(5);
        c.screenshot("10-mask-man");
        q(c, "mobs", "clear", true);

        // FBI raid
        c.markLog();
        q(c, "set", "fbiMark", 0, "enableRaids", true, "raidDelay", 20, "raidChance", 1, "raidAttackDistance", 8);
        c.cmd("gamemode creative");
        // raidDelay 20 rolls every second: stop the raids as soon as the first one is announced, then count it
        boolean raided = false;
        for (int i = 0; i < 12 && !raided; i++) {
            c.waitTicks(5);
            raided = chatSince(c, "FBI, OPEN UP!");
        }
        q(c, "set", "enableRaids", false);
        Expect.that(raided, "FBI raid announced");
        mobs = q(c, "mobs", "r", 400);
        Expect.that(mobs.get("fbi").getAsInt() == 15 && mobs.get("drones").getAsInt() == 5, "15 agents + 5 drones: " + mobs);
        c.cmd("gamemode spectator");
        c.teleport(X + 0.5, Y + 12, Z - 16.5);
        c.lookAt(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(10);
        c.screenshot("11-fbi-raid");
        c.cmd("gamemode creative");
        q(c, "mobs", "clear", true);
        // using the missile assembly marks the player for 20 minutes: no raid then
        Fixtures.placeOnTop(c, "hbm:machine_missile_assembly", X + 5, Y, Z + 5);
        Fixtures.interact(c, X + 5, Y + 1, Z + 5);
        c.waitTicks(5);
        if (c.screenClass() != null) c.key("escape");
        JsonObject marked = q(c, "props");
        Expect.that(marked.get("fbiMark").getAsLong() > marked.get("time").getAsLong() + 20 * 60 * 20 - 200,
                "the missile assembly marked the player: " + marked);
        q(c, "set", "enableRaids", true);
        c.markLog();
        c.cmd("gamemode creative");
        c.waitTicks(50);
        Expect.that(!chatSince(c, "FBI, OPEN UP!"), "no raid on a marked player");
        q(c, "set", "enableRaids", false, "raidDelay", 30 * 60 * 60, "raidChance", 3, "raidAttackDistance", 32);
        c.cmd("gamemode creative");
        q(c, "mobs", "clear", true);

        // radiation beasts after a meltdown mark
        c.markLog();
        q(c, "set", "radMark", true, "elementalDelay", 20, "elementalChance", 1, "raidAttackDistance", 8);
        c.cmd("gamemode creative");
        c.waitTicks(30);
        Expect.that(chatSince(c, "You hear a faint clicking..."), "the beasts' warning");
        mobs = q(c, "mobs", "r", 400);
        Expect.that(mobs.get("beasts").getAsInt() == 10 && mobs.get("leaders").getAsInt() == 1, "10 beasts, one leader: " + mobs);
        Expect.that(!q(c, "props").get("radMark").getAsBoolean(), "the meltdown mark is spent");
        c.cmd("gamemode spectator");
        c.teleport(X + 0.5, Y + 12, Z - 16.5);
        c.lookAt(X + 0.5, Y + 1, Z + 0.5);
        c.waitTicks(10);
        c.screenshot("12-rad-beasts");
        c.cmd("gamemode creative");
        q(c, "set", "elementalDelay", 30 * 60 * 60, "elementalChance", 2, "raidAttackDistance", 32);
        q(c, "mobs", "clear", true);

        // digamma ghosts
        // spawns do not look at the game mode; creative keeps the digamma-poisoned player alive for the wait
        // on the flat ground: a ghost lands on the height map 75 blocks out, and seen from the platform 160 blocks up it is
        // beyond vanilla's 128-block instant despawn distance before anyone can count it
        c.cmd("ntmrad clear");
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, -60, Z + 0.5);
        c.waitTicks(60);
        q(c, "set", "digamma", 0.5, "rads", 0.0);
        int ghosts = 0;
        for (int i = 0; i < 30 && ghosts == 0; i++) {
            c.waitTicks(20);
            ghosts = q(c, "mobs", "r", 400).get("ghosts").getAsInt();
        }
        String probe = "";
        if (ghosts == 0) {
            probe = q(c, "ghostProbe").toString();
            c.waitTicks(20);
            probe += " one second later: " + q(c, "ghostCheck");
        }
        q(c, "set", "digamma", 0.0);
        c.cmd("gamemode creative");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        Expect.that(ghosts > 0, "a digamma ghost spawned 75 blocks out: " + probe);
        q(c, "mobs", "clear", true);
        c.cmd("difficulty peaceful");
    }

    // ============================================================== 7: radar in the seeded world

    private static void radar(Client c) {
        c.useSeededWorld();
        c.waitInWorld(120_000L);
        // the seeded world's snapshot, not whatever an earlier run left in it (a raid left on once filled it with agents)
        c.resetWorld();
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        // visit a place 600 blocks east so its chunks are generated and saved, then come back
        c.teleport(600.5, 200, 0.5);
        // a fresh region of the seeded world generates slowly on a loaded machine: give the view distance time to become
        // full chunks (only full chunks carry a final height map)
        c.waitTicks(400);
        c.teleport(0.5, 200, 0.5);
        c.waitTicks(200);
        // a fresh snapshot has no other saved terrain: write the chunks the visit generated (unloaded ones included)
        c.cmd("save-all flush");
        c.waitTicks(40);
        JsonObject far = q(c, "saved", "cx", 600 >> 4, "cz", 0);
        Expect.that(!far.get("loaded").getAsBoolean() && far.get("saved").getAsBoolean(),
                "chunk at x 600 is saved and unloaded: " + far);
        JsonObject never = q(c, "saved", "cx", 3000, "cz", 3000);
        Expect.that(!never.get("saved").getAsBoolean(), "a never generated chunk has no saved height map: " + never);
        c.cmd("fill -2 180 -2 2 180 2 minecraft:stone");
        Fixtures.setBlock(c, 0, 181, 0, "hbm:machine_radar");
        c.teleport(2.5, 181, 2.5); // on the 5x5 stone pad: beside it the player falls into the ocean below
        c.waitTicks(5);
        q(c, "radar", "x", 0, "y", 181, "z", 0);
        JsonObject r = null;
        for (int i = 0; i < 12; i++) {
            c.waitTicks(100);
            r = q(c, "radar", "x", 0, "y", 181, "z", 0);
            if (r.get("filledUnloaded").getAsInt() > 50 && r.get("compared").getAsInt() > 5) break;
        }
        Expect.that(r.get("filledUnloaded").getAsInt() > 50, "the radar map covers saved unloaded chunks: " + r);
        Expect.that(r.get("compared").getAsInt() > 0 && r.get("matching").getAsInt() == r.get("compared").getAsInt(),
                "and each such cell is the chunk's saved height: " + r);
        q(c, "radar", "x", 0, "y", 181, "z", 0, "power", true);
        c.teleport(2.5, 181, 2.5);
        c.waitTicks(5);
        Fixtures.interact(c, 0, 181, 0);
        c.waitTicks(60); // the map reaches the client 100 cells a tick
        c.screenshot("70-radar-map");
        if (c.screenClass() != null) c.key("escape");
        c.waitTicks(3);
    }
}
