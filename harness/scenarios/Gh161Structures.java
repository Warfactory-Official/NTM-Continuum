import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.HarnessException;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * GitHub issue 161: structure blocks turn with the structure, a structure's bobblehead is drawn at random (1.7
 * {@code TileEntityBobble.transformTE}), and the silo's hatch, Methusalem turret and launch pad stand whole when the
 * silo is built chunk by chunk as world generation builds it.
 *
 * <p>Flat world: six NBT structures ({@code hbm_wgc mode=nbt}) and the nine hardcoded pieces ({@code mode=build}) are
 * built in all four rotations; rotation N must hold rotation 0's blocks at the turned positions with facing, axis,
 * 16-step rotation, scaffold orientation and rail shape turned N quarters, including the blocks 1.7 left unturned.
 * The silo is built in four rotations, whole and chunk by chunk.
 */
public final class Gh161Structures implements Scenario {

    private static final String[] NBT = {"tower_base", "factory", "radio", "labolatory", "water_pump", "crane"};
    private static final String[] TURN = {"north", "east", "south", "west"};
    private static final String[] PIECES = {"NTMHouse1", "NTMHouse2", "NTMLab1", "NTMLab2", "RuralHouse1", "LargeOffice",
            "LargeOfficeCorner", "SiloComponent", "StartingHub"};
    /** 1.7 {@code MachineBoiler.onBlockAdded} turns it away from solid neighbours, which depends on the build order. */
    private static final Set<String> PLACE_ORDER = Set.of("machine_boiler_off");

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.resetWorld(); // a rebuild over an earlier run's multiblock tears down the new cells with the old core
        home(c, -40);
        c.freezeWorld();
        c.cmd("gamemode spectator");
        c.cmd("effect give @s minecraft:night_vision 100000 0 true");
        c.markLog();
        List<String> failures = new ArrayList<>();
        List<String> report = new ArrayList<>();
        int px = 6144;

        Set<String> bobbles = new LinkedHashSet<>();
        for (String id : NBT) {
            Map<Integer, Map<String, String>> builds = new HashMap<>();
            int[] size = null;
            for (int rot = 0; rot < 4; rot++) {
                JsonObject s = q(c, "hbm_wgc", "mode", "nbt", "piece", id, "x", px, "y", -61, "z", 0, "rot", rot,
                        "seed", 161L + rot).getAsJsonObject("start");
                int[] b = box(s);
                if (rot == 0) size = new int[] {b[3] - b[0] + 1, b[5] - b[2] + 1};
                Map<String, String> blocks = new HashMap<>();
                for (JsonElement e : s.getAsJsonArray("blocks")) {
                    String line = e.getAsString();
                    String[] f = line.split(" ", 5);
                    if (f[3].startsWith("wand_")) continue; // placeholders: replaced on their first tick, turned by their block entity
                    blocks.put(f[0] + " " + f[1] + " " + f[2], f[3] + " " + f[4]);
                    int type = line.indexOf(" type=");
                    if (type > 0) bobbles.add(id + "/" + rot + "=" + line.substring(type + 6));
                }
                builds.put(rot, blocks);
                for (JsonElement e : s.getAsJsonArray("multiblocks")) {
                    JsonObject m = e.getAsJsonObject();
                    if (m.get("placed").getAsInt() != m.get("cells").getAsInt()) failures.add(id + " rot " + rot + ": " + m);
                }
                if (id.equals("tower_base")) shootTowerRoom(c, b, rot, size);
                px += 96;
            }
            compare(id, builds, size, false, report, failures);
        }

        // hardcoded pieces, in their own 1.7 frame; random picks differ per rotation, so only same-block positions count
        for (String piece : PIECES) {
            Map<Integer, Map<String, String>> builds = new HashMap<>();
            for (int rot = 0; rot < 4; rot++) {
                int floor = piece.equals("SiloComponent") || piece.equals("NTMLab2") ? -62 : -60;
                JsonObject s = q(c, "hbm_wgc", "mode", "build", "piece", piece, "x", px, "z", 0, "y", floor, "seed", 161L,
                        "rot", rot).getAsJsonObject("start");
                Map<String, String> blocks = new HashMap<>();
                for (JsonElement e : s.getAsJsonArray("blocks")) {
                    String[] f = e.getAsString().split(" ", 5);
                    // 1.7 gives the builders' bobbleheads and the silo's rubble stairs a random facing
                    if (f[3].startsWith("wand_") || f[3].equals("bobblehead") || f[3].matches("brick_concrete_(\\w+_)?stairs")) continue;
                    blocks.put(f[0] + " " + f[1] + " " + f[2], f[3] + " " + f[4]);
                }
                builds.put(rot, blocks);
                px += 128;
            }
            compare(piece, builds, null, true, report, failures);
        }
        report.add("bobbleheads " + bobbles);
        Set<String> types = new LinkedHashSet<>();
        for (String b : bobbles) types.add(b.substring(b.indexOf('=') + 1));
        if (types.size() < 3 || types.contains("NONE")) failures.add("structure bobbleheads are not drawn at random: " + bobbles);

        for (int rot = 0; rot < 4; rot++) {
            for (boolean chunked : new boolean[] {false, true}) {
                JsonObject s = chunked
                        ? q(c, "hbm_wgc", "mode", "build", "piece", "SiloComponent", "x", px, "z", 0, "y", -62, "seed", 161L, "rot", rot, "chunked", true)
                        .getAsJsonObject("start")
                        : q(c, "hbm_wgc", "mode", "build", "piece", "SiloComponent", "x", px, "z", 0, "y", -62, "seed", 161L, "rot", rot)
                        .getAsJsonObject("start");
                JsonArray multi = s.getAsJsonArray("multiblocks");
                report.add("silo rot " + rot + (chunked ? " chunked " : " whole   ") + multi);
                Set<String> kinds = new LinkedHashSet<>();
                for (JsonElement e : multi) {
                    JsonObject m = e.getAsJsonObject();
                    kinds.add(m.get("block").getAsString());
                    if (m.get("placed").getAsInt() != m.get("cells").getAsInt() || m.get("indexed").getAsInt() != m.get("cells").getAsInt()) {
                        failures.add("silo rot " + rot + (chunked ? " chunked: " : ": ") + m);
                    }
                }
                for (String k : new String[] {"silo_hatch_large", "turret_howard_damaged", "launch_pad_rusted"}) {
                    if (!kinds.contains(k)) failures.add("silo rot " + rot + (chunked ? " chunked" : "") + " has no " + k + " core: " + multi);
                }
                if (chunked) shootSilo(c, box(s), rot);
                px += 128;
            }
        }

        System.out.println("[Gh161] " + c.node() + "\n  " + String.join("\n  ", report));
        List<String> bad = c.logSinceMark("Detected setBlock in a far chunk|NTM structure gen failed|Exception.*(Structure|Component)");
        if (!bad.isEmpty()) failures.add("structure errors in the log: " + bad);
        Expect.that(failures.isEmpty(), "issue 161: " + failures);
    }

    /**
     * Rotation N must hold rotation 0's turning blocks turned N quarters, at rotation 0's positions turned N quarters
     * ({@code size} for NBT pieces, whose positions are box-relative; null for hardcoded pieces, already in their frame).
     */
    private static void compare(String id, Map<Integer, Map<String, String>> builds, int[] size, boolean sameBlockOnly,
                                List<String> report, List<String> failures) {
        int wrong = 0;
        List<String> examples = new ArrayList<>();
        Map<String, Integer> wrongById = new TreeMap<>();
        for (int rot = 1; rot < 4; rot++) {
            for (var entry : builds.get(0).entrySet()) {
                String[] p = entry.getKey().split(" ");
                int bx = Integer.parseInt(p[0]), by = Integer.parseInt(p[1]), bz = Integer.parseInt(p[2]);
                int rx = size == null ? bx : rotateX(bx, bz, rot, size), rz = size == null ? bz : rotateZ(bx, bz, rot, size);
                String block = entry.getValue().split(" ")[0];
                if (PLACE_ORDER.contains(block)) continue;
                String found = builds.get(rot).get(rx + " " + by + " " + rz);
                if (sameBlockOnly && (found == null || !found.startsWith(block + " "))) continue;
                String expected = relevant(turn(entry.getValue(), rot));
                if (!expected.equals(relevant(found))) {
                    wrong++;
                    wrongById.merge(block, 1, Integer::sum);
                    if (examples.size() < 12) examples.add("rot " + rot + " at " + entry.getKey() + " " + entry.getValue()
                            + ": expected " + expected + ", found " + found);
                }
            }
        }
        report.add(String.format("%-18s %d turning blocks, %d wrong after rotation %s %s", id, builds.get(0).size(), wrong, wrongById, examples));
        if (wrong > 0) failures.add(id + ": " + wrong + " blocks do not turn with the structure " + wrongById + " " + examples);
    }

    /** The room with the bobblehead, tape recorders and telex: from template (9, 22, 9) towards (5, 22, 8). */
    private void shootTowerRoom(Client c, int[] b, int rot, int[] size) {
        int ex = b[0] + rotateX(9, 9, rot, size), ez = b[2] + rotateZ(9, 9, rot, size);
        int tx = b[0] + rotateX(5, 8, rot, size), tz = b[2] + rotateZ(5, 8, rot, size);
        c.cmd("forceload add " + b[0] + " " + b[2] + " " + b[3] + " " + b[5]);
        c.teleport(ex + 0.5, b[1] + 22.4, ez + 0.5);
        c.lookAt(tx + 0.5, b[1] + 22.0, tz + 0.5);
        c.waitTicks(80);
        c.screenshot("tower_base-rot" + rot);
    }

    /** The silo's surface hatch and turret, from above the hatch's south-west. */
    private void shootSilo(Client c, int[] b, int rot) {
        c.teleport((b[0] + b[3]) / 2 + 0.5, b[4] + 10, (b[2] + b[5]) / 2 + 0.5);
        c.lookAt((b[0] + b[3]) / 2 + 0.5, b[4] - 4, (b[2] + b[5]) / 2 + 2.5);
        c.waitTicks(100);
        c.screenshot("silo-rot" + rot);
    }

    /** 1.7 {@code NBTStructure.rotateX/rotateZ}; {@code size} is the rotation-0 footprint. */
    private static int rotateX(int x, int z, int rot, int[] size) {
        return switch (rot) {
            case 1 -> size[1] - 1 - z;
            case 2 -> size[0] - 1 - x;
            case 3 -> z;
            default -> x;
        };
    }

    private static int rotateZ(int x, int z, int rot, int[] size) {
        return switch (rot) {
            case 1 -> x;
            case 2 -> size[1] - 1 - z;
            case 3 -> size[0] - 1 - x;
            default -> z;
        };
    }

    /** The block's turning properties as they must read after the structure turned {@code rot} quarters clockwise. */
    private static String turn(String block, int rot) {
        StringBuilder out = new StringBuilder();
        for (String part : stripType(block).split(" ")) {
            if (!out.isEmpty()) out.append(' ');
            int eq = part.indexOf('=');
            if (eq < 0) {
                out.append(part);
                continue;
            }
            String key = part.substring(0, eq), value = part.substring(eq + 1);
            switch (key) {
                case "facing", "gauge" -> {
                    int i = List.of(TURN).indexOf(value);
                    value = i < 0 ? value : TURN[(i + rot) % 4];
                }
                case "axis" -> value = rot % 2 == 0 ? value : value.equals("x") ? "z" : value.equals("z") ? "x" : value;
                case "rotation" -> value = String.valueOf((Integer.parseInt(value) + 4 * rot) % 16);
                case "shape" -> value = turnShape(value, rot);
                case "orient" -> value = rot % 2 == 0 ? value
                        : value.contains("north_south") ? value.replace("north_south", "east_west") : value.replace("east_west", "north_south");
                default -> { }
            }
            out.append(key).append('=').append(value);
        }
        return out.toString();
    }

    /** A rail shape turned {@code rot} quarters clockwise; stair shapes (straight, inner_left, ...) do not change. */
    private static String turnShape(String value, int rot) {
        List<String> turn = List.of(TURN);
        String[] words = value.split("_");
        List<String> dirs = new ArrayList<>();
        for (String w : words) if (turn.contains(w)) dirs.add(TURN[(turn.indexOf(w) + rot) % 4]);
        if (dirs.isEmpty()) return value;
        if (value.startsWith("ascending_")) return "ascending_" + dirs.get(0);
        Set<String> set = Set.copyOf(dirs);
        for (String name : List.of("north_south", "east_west", "south_east", "south_west", "north_west", "north_east")) {
            if (Set.of(name.split("_")).equals(set)) return name;
        }
        return value;
    }

    /** The properties that decide the look: a hanging chain's and a duct gauge's {@code facing} mean nothing. */
    private static String relevant(String block) {
        String s = stripType(block);
        if (s.contains(" wall=false")) s = s.replaceAll(" facing=\\w+", "");
        if (s.startsWith("fluid_duct_gauge ")) s = s.replaceAll(" facing=\\w+", "");
        return s;
    }

    private static String stripType(String block) {
        if (block == null) return "";
        int type = block.indexOf(" type=");
        return type < 0 ? block : block.substring(0, type);
    }

    static JsonObject q(Client c, String kind, Object... kv) {
        JsonObject p = new JsonObject();
        p.addProperty("kind", kind);
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) p.addProperty((String) kv[i], n);
            else if (v instanceof Boolean b) p.addProperty((String) kv[i], b);
            else p.addProperty((String) kv[i], String.valueOf(v));
        }
        JsonObject r = c.send("query", p, 240_000L);
        if (r.has("error")) throw new HarnessException(kind + " " + p + ": " + r.get("error"));
        return r;
    }

    static int[] box(JsonObject s) {
        JsonArray b = s.getAsJsonArray("box");
        int[] out = new int[6];
        for (int i = 0; i < 6; i++) out[i] = b.get(i).getAsInt();
        return out;
    }

    static void home(Client c, int y) {
        for (int i = 0; i < 10; i++) {
            if (String.valueOf(c.screenClass()).contains("Death")) {
                c.clickWidget("Respawn");
                c.waitTicks(40);
            }
            c.cmd("gamemode spectator");
            c.cmd("execute in minecraft:overworld run tp @s 0 " + y + " 0");
            c.waitTicks(40);
            if (!String.valueOf(c.screenClass()).contains("Death") && String.valueOf(c.status().get("dimension")).contains("overworld")) return;
        }
        throw new HarnessException("could not bring the player to the overworld: " + c.status().get("dimension"));
    }
}
