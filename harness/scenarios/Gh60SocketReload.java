import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 60: a battery socket holding a full capacitor, its chunk unloaded and loaded again, must still draw its
 * capacitor, show its charge and power the electric furnace beside it. Not for 1.20.1 (the stack NBT is 1.21 shaped).
 */
public final class Gh60SocketReload implements Scenario {

    private static final int Y = 100;
    // far from spawn; the 2x2 footprint straddles the z = 0 chunk border
    private static final int X = 5000;
    private static final String CAPACITOR = "{Slot:0b,id:\"hbm:capacitor_bismuth\",count:1,components:{\"hbm:battery_charge\":2500000L}}";

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("forceload remove all");
        c.cmd("forceload add " + (X - 16) + " -16 " + (X + 16) + " 16");
        c.teleport(X + 0.5, Y + 2, 0.5);
        c.waitTicks(20);
        Fixtures.platform(c, X, Y, 0, 8);
        c.cmd("fill " + (X - 6) + " " + (Y + 1) + " -6 " + (X + 6) + " " + (Y + 4) + " 6 minecraft:air");
        c.waitTicks(5);

        Fixtures.placeOnTop(c, "hbm:machine_battery_socket", X, Y, 0);
        c.waitTicks(8);
        int[] core = findCore(c);
        Expect.that(core != null, "the socket was placed");
        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {redLow:2s,inventory:{Items:[" + CAPACITOR + "]}}");
        int[] beside = besideFootprint(c, core);
        Expect.that(beside != null, "a free cell beside the socket");
        Fixtures.setBlock(c, beside[0], beside[1], beside[2], "hbm:machine_electric_furnace_off");
        c.waitTicks(40);
        long before = furnacePower(c, beside);
        System.out.println("[Gh60] core " + core[0] + "," + core[2] + " furnace " + beside[0] + "," + beside[2]
                + " power " + before);
        Expect.that(before > 0, "the socket feeds the furnace before the unload: " + before);
        view(c, core, "01-before-unload");

        c.cmd("forceload remove " + (X - 16) + " -16 " + (X + 16) + " 16");
        c.teleport(-3000.5, Y + 2, 0.5);
        boolean unloaded = false;
        for (int i = 0; i < 30 && !unloaded; i++) {
            c.waitTicks(20);
            JsonObject far = c.block(core[0], core[1], core[2]);
            unloaded = far.has("loaded") && !far.get("loaded").getAsBoolean();
        }
        Expect.that(unloaded, "the socket chunk must unload while the player is away");
        check(c, core, beside, "02-after-return");

        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 240_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(20);
        check(c, core, beside, "03-after-rejoin");
    }

    private static void check(Client c, int[] core, int[] beside, String name) {
        c.teleport(core[0] + 0.5, Y + 2, core[2] + 4.5);
        c.waitTicks(40);
        c.cmd("data merge block " + beside[0] + " " + beside[1] + " " + beside[2] + " {power:0L}");
        c.waitTicks(40);
        long after = furnacePower(c, beside);
        System.out.println("[Gh60] " + name + " furnace power " + after + " socket " + nbt(c, core).replaceAll("\\s+", " "));
        view(c, core, name);
        Expect.that(nbt(c, core).contains("capacitor_bismuth"), "the capacitor is still in the socket");
        Expect.that(after > 0, "the socket must feed the furnace (" + name + "): " + after);
    }

    private static void view(Client c, int[] core, String name) {
        c.teleport(core[0] + 0.5, Y + 2, core[2] + 3.5);
        c.lookAt(core[0] + 0.5, Y + 1.5, core[2] + 0.5);
        c.waitTicks(10);
        c.screenshot(name);
    }

    private static int[] findCore(Client c) {
        for (int z = -3; z <= 3; z++) {
            for (int x = X - 3; x <= X + 3; x++) {
                if (nbt(c, new int[]{x, Y + 1, z}).contains("redLow")) return new int[]{x, Y + 1, z};
            }
        }
        return null;
    }

    /** Every horizontal neighbour of the 2x2 footprint is one of 1.7's eight connection points. */
    private static int[] besideFootprint(Client c, int[] core) {
        for (int z = core[2] - 2; z <= core[2] + 2; z++) {
            for (int x = core[0] - 2; x <= core[0] + 2; x++) {
                if (!c.block(x, Y + 1, z).toString().contains("minecraft:air")) continue;
                boolean touches = false;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    touches |= c.block(x + d[0], Y + 1, z + d[1]).toString().contains("battery_socket");
                }
                if (touches) return new int[]{x, Y + 1, z};
            }
        }
        return null;
    }

    private static long furnacePower(Client c, int[] pos) {
        Matcher m = Pattern.compile("power:\\s*(-?\\d+)L").matcher(nbt(c, pos));
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }

    private static String nbt(Client c, int[] pos) {
        JsonObject be = c.blockEntity(pos[0], pos[1], pos[2]);
        return be == null || !be.has("nbt") || be.get("nbt").isJsonNull() ? "" : be.get("nbt").getAsString();
    }
}
