import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Another mod's pipes move HBM items and fluids: Pipez (dev runs on NeoForge and Forge) pipes, each extracting from its
 * west neighbour into its east neighbour. Items chest to crate, crate to chest, chest to the alloy furnace's fuel slot and its
 * output to a chest; diesel from one steel barrel to another. Every case is reported, then checked.
 */
public final class PipezInterop implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;
    /** Fluid ports on cells: 1.20.1 Forge exposes them only through cell block entities. */
    private static final String[] MULTIBLOCKS = {"machine_turbofan", "machine_wood_burner", "heater_heatex"};

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -12 " + G + " -12 12 " + (Y + 6) + " 12 minecraft:air");
        c.cmd("gamemode creative");
        c.markLog();
        c.teleport(0.5, G + 4, -6.5);
        c.lookAt(1.5, G, 4.5);
        c.waitTicks(10);

        pipe(c, "item", 1, -3);
        pipe(c, "item", 1, 0);
        pipe(c, "item", 1, 3);
        pipe(c, "item", 1, 6);
        pipe(c, "item", 3, 6);
        pipe(c, "fluid", 1, 9);
        // 1 chest -> crate
        c.cmd("setblock 0 " + G + " 0 minecraft:chest");
        c.cmd("item replace block 0 " + G + " 0 container.0 with minecraft:cobblestone 32");
        c.cmd("setblock 2 " + G + " 0 hbm:crate_iron");
        // 2 crate -> chest
        c.cmd("setblock 0 " + G + " 3 hbm:crate_iron");
        c.cmd("item replace block 0 " + G + " 3 container.0 with minecraft:cobblestone 32");
        c.cmd("setblock 2 " + G + " 3 minecraft:chest");
        // 3 chest -> alloy furnace, 4 alloy furnace output -> chest
        c.cmd("setblock 0 " + G + " 6 minecraft:chest");
        c.cmd("item replace block 0 " + G + " 6 container.0 with minecraft:coal 16");
        // 1.7 takes each input only from its configured side; fuel set to west (4), as in the furnace's GUI
        c.cmd("setblock 2 " + G + " 6 hbm:machine_difurnace_off{sideFuel:4b}");
        c.cmd("setblock 4 " + G + " 6 minecraft:chest");
        c.cmd("item replace block 2 " + G + " 6 container.3 with hbm:ingot_steel 8");
        // 5 barrel -> barrel
        c.cmd("setblock 0 " + G + " 9 hbm:barrel_steel{tank:5000,tank_type:\"hbm:diesel\",mode:2s}");
        c.cmd("setblock 2 " + G + " 9 hbm:barrel_steel{tank:0,tank_type:\"hbm:diesel\",mode:0s}");

        // 0 control: chest -> chest
        c.cmd("setblock 0 " + G + " -3 minecraft:chest");
        c.cmd("item replace block 0 " + G + " -3 container.0 with minecraft:cobblestone 32");
        c.cmd("setblock 2 " + G + " -3 minecraft:chest");

        extract(c, 1, -3);
        extract(c, 1, 0);
        extract(c, 1, 3);
        extract(c, 1, 6);
        extract(c, 3, 6);
        extract(c, 1, 9);
        c.waitTicks(200);
        System.out.println("[PipezInterop] pipe " + nbt(c, 1, -3) + " state " + c.block(1, G, -3));
        c.screenshot("pipes");
        List<String> failures = new ArrayList<>();
        report(failures, "control chest to chest", nbt(c, 2, -3).contains("cobblestone"), nbt(c, 2, -3));
        report(failures, "chest to crate", nbt(c, 2, 0).contains("cobblestone"), nbt(c, 2, 0));
        report(failures, "crate to chest", nbt(c, 2, 3).contains("cobblestone"), nbt(c, 2, 3));
        // the furnace burns fuel into powerTime at once, 200 per coal
        report(failures, "chest to alloy furnace fuel (side set to west)", nbt(c, 2, 6).contains("powerTime:3200"), nbt(c, 2, 6));
        report(failures, "alloy furnace output to chest", nbt(c, 4, 6).contains("ingot_steel"), nbt(c, 4, 6));
        report(failures, "barrel to barrel", !nbt(c, 2, 9).contains("tank:0,") && nbt(c, 2, 9).contains("tank:"), nbt(c, 2, 9));
        for (String machine : MULTIBLOCKS) {
            int connected = multiblock(c, machine);
            report(failures, machine + " pipe connections " + connected, connected > 0, "");
        }
        Expect.that(failures.isEmpty(), "Pipez could not move: " + failures);
        Expect.logCleanSinceMark(c, "(?i)(exception|/(ERROR|FATAL)[]]).*");
    }

    /** Places {@code id} by hand, surrounds it with Pipez fluid pipes and counts their connections into it. */
    private static int multiblock(Client c, String id) {
        int oz = -10;
        c.cmd("fill -10 " + G + " -12 10 " + (Y + 8) + " -4 minecraft:air");
        Fixtures.giveOnly(c, "hbm:" + id, 1);
        c.teleport(0.5, G, oz + 4.5);
        c.waitTicks(5);
        c.lookAt(0.5, Y + 0.98, oz + 1.5);
        c.waitTicks(5);
        c.use(2);
        c.waitTicks(10);
        c.cmd("clear");
        c.teleport(0.5, G + 6, oz + 8.5);
        Set<Long> footprint = new HashSet<>();
        for (int x = -6; x <= 6; x++) for (int z = oz - 6; z <= oz + 6; z++) for (int y = G; y <= G + 5; y++) {
            if (c.block(x, y, z).get("block").getAsString().equals("hbm:" + id)) footprint.add(key(x, y, z));
        }
        Expect.that(!footprint.isEmpty(), id + " placed by hand");
        Set<Long> pipes = new HashSet<>();
        int[][] sides = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}};
        for (long cell : footprint) {
            for (int[] d : sides) {
                int x = kx(cell) + d[0], y = ky(cell) + d[1], z = kz(cell) + d[2];
                long at = key(x, y, z);
                if (footprint.contains(at) || pipes.contains(at)) continue;
                pipes.add(at);
                c.cmd("setblock " + x + " " + y + " " + z + " pipez:fluid_pipe");
            }
        }
        c.waitTicks(20);
        int connected = 0;
        String[][] dirs = {{"east", "1", "0", "0"}, {"west", "-1", "0", "0"}, {"south", "0", "0", "1"},
                {"north", "0", "0", "-1"}, {"up", "0", "1", "0"}, {"down", "0", "-1", "0"}};
        for (long at : pipes) {
            JsonObject props = c.block(kx(at), ky(at), kz(at)).getAsJsonObject("properties");
            for (String[] d : dirs) {
                long toward = key(kx(at) + Integer.parseInt(d[1]), ky(at) + Integer.parseInt(d[2]), kz(at) + Integer.parseInt(d[3]));
                if (footprint.contains(toward) && props != null && "true".equals(props.get(d[0]).getAsString())) connected++;
            }
        }
        c.screenshot("pipes-" + id);
        for (long at : pipes) c.cmd("setblock " + kx(at) + " " + ky(at) + " " + kz(at) + " minecraft:air");
        c.cmd("fill -10 " + G + " -12 10 " + (Y + 8) + " -4 minecraft:air");
        return connected;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + 512) << 40) | ((long) (y + 512) << 20) | (z + 512);
    }

    private static int kx(long k) { return (int) (k >> 40) - 512; }
    private static int ky(long k) { return (int) ((k >> 20) & 0xFFFFF) - 512; }
    private static int kz(long k) { return (int) (k & 0xFFFFF) - 512; }

    private static void pipe(Client c, String kind, int x, int z) {
        c.cmd("setblock " + x + " " + G + " " + z + " pipez:" + kind + "_pipe");
    }

    /** After the neighbours are placed: a sneaking wrench click on the pipe's west arm makes it extract from that side. */
    private static void extract(Client c, int x, int z) {
        Fixtures.giveOnly(c, "pipez:wrench", 1);
        c.teleport(x + 0.15, G, z - 1.5);
        c.keyDown("key.sneak");
        c.waitTicks(5);
        c.lookAt(x + 0.15, G + 0.5, z + 0.3125);
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(5);
        c.keyUp("key.sneak");
        System.out.println("[PipezInterop] extracting " + x + "," + z + ": " + c.block(x, G, z).get("state"));
    }

    private static void report(List<String> failures, String what, boolean ok, String nbt) {
        System.out.println("[PipezInterop] " + what + ": " + (ok ? "OK" : "FAILED") + " | " + nbt);
        if (!ok) failures.add(what);
    }

    private static String nbt(Client c, int x, int z) {
        JsonObject be = c.blockEntity(x, G, z);
        return (be.has("nbt") && be.get("nbt").isJsonPrimitive() ? be.get("nbt").getAsString() : be.toString()).replaceAll("\\s+", "");
    }
}
