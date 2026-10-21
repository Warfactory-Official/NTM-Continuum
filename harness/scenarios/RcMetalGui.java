import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool and GUI interactions of the metallurgy machines: the soldering station's collision-prevention button
 * (survives a rejoin), the screwdriver cycling the electric heater and removing a mold, the hand drill flipping
 * an outlet's filter inversion, and the anvil menu opening.
 */
public final class RcMetalGui implements Scenario {

    private static final int Y = 100;
    private static final int L = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.cmd("gamemode creative");
        c.cmd("forceload add 0 0 40 40");
        c.waitTicks(40);
        c.cmd("fill 0 " + Y + " 0 40 " + Y + " 40 minecraft:stone");
        c.cmd("fill 0 " + L + " 0 40 " + (L + 10) + " 40 minecraft:air");
        c.markLog();

        // soldering station
        int sx = 10, sz = 10;
        c.cmd("setblock " + sx + " " + L + " " + sz + " hbm:machine_soldering_station[facing=north]");
        c.waitTicks(10);
        int[] core = findBe(c, sx, L, sz, "TileEntityMachineSolderingStation");
        System.out.println("[RcMetalGui] soldering core " + core[0] + "," + core[1] + "," + core[2]);
        c.teleport(core[0] + 0.5, L, core[2] + 4.5);
        c.waitTicks(5);
        Fixtures.interact(c, core[0], core[1], core[2]);
        c.waitScreen("oldering", 8000);
        JsonObject screen = c.screen();
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        Expect.that(!nbt(c, core).contains("collisionPrevention:1b") && !nbt(c, core).contains("collisionPrevention:1"), "collision prevention starts off: " + nbt(c, core));
        c.mouseMove(left + 10, top + 71);
        c.waitTicks(3);
        c.screenshot("01-soldering-hover");
        c.click(left + 10, top + 71);
        c.waitTicks(5);
        String on = nbt(c, core);
        Expect.that(on.matches(".*collisionPrevention:(1b|true).*"), "the button turned collision prevention on: " + on);
        c.screenshot("02-soldering-on");
        c.key("escape");
        c.waitTicks(3);
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 240_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(20);
        Expect.that(nbt(c, core).matches(".*collisionPrevention:(1b|true).*"), "the toggle survives a rejoin: " + nbt(c, core));
        c.teleport(core[0] + 0.5, L, core[2] + 4.5);
        c.waitTicks(5);
        Fixtures.interact(c, core[0], core[1], core[2]);
        c.waitScreen("oldering", 8000);
        screen = c.screen();
        left = screen.get("left").getAsInt();
        top = screen.get("top").getAsInt();
        c.click(left + 10, top + 71);
        c.waitTicks(5);
        Expect.that(!nbt(c, core).matches(".*collisionPrevention:(1b|true).*"), "the second click turns it off again: " + nbt(c, core));
        c.key("escape");
        c.waitTicks(3);

        // electric heater and the screwdriver
        int hx = 20, hz = 10;
        c.cmd("setblock " + hx + " " + L + " " + hz + " hbm:heater_electric[facing=north]");
        c.waitTicks(10);
        int[] heater = findBe(c, hx, L, hz, "TileEntityHeaterElectric");
        Expect.that(nbtInt(c, heater, "setting") == 0, "a fresh electric heater is at setting 0: " + nbt(c, heater));
        c.teleport(heater[0] + 0.5, L, heater[2] + 4.5);
        c.waitTicks(3);
        use(c, "hbm:screwdriver", heater[0] + 0.5, heater[1] + 0.5, heater[2] + 0.5);
        Expect.that(nbtInt(c, heater, "setting") == 1, "the screwdriver cycles the heater to 1: " + nbt(c, heater));
        use(c, "hbm:screwdriver", heater[0] + 0.5, heater[1] + 0.5, heater[2] + 0.5);
        Expect.that(nbtInt(c, heater, "setting") == 2, "and to 2: " + nbt(c, heater));

        // mold install and screwdriver removal
        int mx = 25, mz = 10;
        c.cmd("setblock " + mx + " " + L + " " + mz + " hbm:foundry_mold");
        c.waitTicks(5);
        c.teleport(mx + 0.5, L, mz + 3.5);
        c.waitTicks(3);
        use(c, "hbm:mold_ingot", mx + 0.5, L + 0.2, mz + 0.5);
        Expect.that(nbt(c, new int[]{mx, L, mz}).contains("mold_ingot"), "the mold was installed: " + nbt(c, new int[]{mx, L, mz}));
        use(c, "hbm:screwdriver", mx + 0.5, L + 0.2, mz + 0.5);
        Expect.that(!nbt(c, new int[]{mx, L, mz}).contains("mold_ingot"), "the screwdriver took the mold out: " + nbt(c, new int[]{mx, L, mz}));

        // outlet and the hand drill
        int ox = 30, oz = 10;
        c.cmd("setblock " + ox + " " + L + " " + oz + " hbm:foundry_outlet[facing=north]");
        c.waitTicks(5);
        c.teleport(ox + 0.5, L, oz + 3.5);
        c.waitTicks(3);
        use(c, "hbm:hand_drill", ox + 0.5, L + 0.2, oz + 0.5);
        Expect.that(nbt(c, new int[]{ox, L, oz}).matches(".*invertFilter:(1b|true).*"), "the hand drill flips the outlet filter: " + nbt(c, new int[]{ox, L, oz}));
        use(c, "hbm:screwdriver", ox + 0.5, L + 0.2, oz + 0.5);
        Expect.that(!nbt(c, new int[]{ox, L, oz}).matches(".*invertFilter:(1b|true).*"), "the screwdriver resets it: " + nbt(c, new int[]{ox, L, oz}));

        // anvil menu
        int ax = 35, az = 10;
        c.cmd("setblock " + ax + " " + L + " " + az + " hbm:anvil_steel");
        c.waitTicks(5);
        c.teleport(ax + 0.5, L, az + 3.5);
        c.waitTicks(3);
        Fixtures.interact(c, ax, L, az);
        c.waitScreen("nvil", 8000);
        c.screenshot("03-anvil-gui");
        c.key("escape");
        c.waitTicks(3);

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }

    private static void use(Client c, String item, double x, double y, double z) {
        Fixtures.giveOnly(c, item, 1);
        c.lookAt(x, y, z);
        c.waitTicks(2);
        c.use(2);
        c.waitTicks(5);
    }

    private static int[] findBe(Client c, int x, int y, int z, String cls) {
        for (int dy = 0; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            JsonObject be = c.blockEntity(x + dx, y + dy, z + dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.get("block_entity").getAsString().endsWith(cls)) {
                return new int[]{x + dx, y + dy, z + dz};
            }
        }
        throw new AssertionError("no " + cls + " near " + x + "," + y + "," + z);
    }

    private static String nbt(Client c, int[] p) {
        JsonObject be = c.blockEntity(p[0], p[1], p[2]);
        return be.has("nbt") ? be.get("nbt").getAsString().replaceAll("\\s+", "") : be.toString();
    }

    private static int nbtInt(Client c, int[] p, String key) {
        Matcher m = Pattern.compile("(?:^|[{,])" + key + ":(-?\\d+)").matcher(nbt(c, p));
        if (!m.find()) throw new AssertionError("no " + key + ": " + nbt(c, p));
        return Integer.parseInt(m.group(1));
    }
}
