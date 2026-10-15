import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 2. A barrel typed through its GUI after the duct line exists must join the net on the next tick, as
 * 1.7's {@code trySubscribe} did: it fills and the duct grows an arm to it. A sneak-click with the multi identifier
 * retypes a whole duct line (1.7 {@code changeTypeRecursively}) and sets a barrel's or tank's type.
 */
public final class Gh2DuctsAndIdentifier implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 14);
        c.cmd("fill -10 " + G + " -10 10 " + (Y + 10) + " 10 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.markLog();

        // source barrel at x=0, ducts at x=1..2, the late barrel at x=3
        place(c, "hbm:barrel_steel", 0);
        c.cmd("clear");
        c.cmd("give @s " + identifier(c, "hbm:water") + " 1");
        c.cmd("give @s hbm:fluid_barrel_infinite 1");
        c.waitTicks(3);
        open(c, 0);
        clickFromInventory(c, "hbm:fluid_identifier", 0);
        clickFromInventory(c, "hbm:fluid_barrel_infinite", 2);
        JsonObject g = c.screen();
        int l = g.get("left").getAsInt(), t = g.get("top").getAsInt();
        c.click(l + 151 + 9, t + 35 + 9); // input -> buffer
        c.waitTicks(3);
        c.click(l + 151 + 9, t + 35 + 9); // buffer -> output
        c.waitTicks(5);
        close(c);
        Expect.that(nbt(c, 0).contains("mode:2s"), "the source barrel outputs: " + nbt(c, 0));

        place(c, "hbm:fluid_duct_neo", 1);
        place(c, "hbm:fluid_duct_neo", 2);
        Fixtures.giveOnly(c, identifier(c, "hbm:water"), 1);
        for (int x = 1; x <= 2; x++) {
            aim(c, x, G + 0.5);
            c.use(1);
            c.waitTicks(3);
            Expect.that(nbt(c, x).contains("water\""), "duct " + x + " typed water: " + nbt(c, x));
        }

        place(c, "hbm:barrel_steel", 3);
        c.waitTicks(40);
        Expect.that(intOf(nbt(c, 3), "tank") == 0, "the untyped barrel takes nothing: " + nbt(c, 3));
        c.cmd("clear");
        c.cmd("give @s " + identifier(c, "hbm:water") + " 1");
        c.waitTicks(3);
        open(c, 3);
        clickFromInventory(c, "hbm:fluid_identifier", 0);
        c.waitTicks(5);
        close(c);
        c.waitTicks(60);
        String late = nbt(c, 3);
        System.out.println("[Gh2] late barrel 60 ticks after typing: " + late);
        System.out.println("[Gh2] duct 2: " + c.block(2, G, Z));
        c.screenshot("01-late-barrel");
        Expect.that(late.contains("water\"") && intOf(late, "tank") > 0, "the barrel typed through its GUI fills from the duct: " + late);
        Expect.that(c.block(2, G, Z).toString().contains("\"east\":\"true\""), "the duct grew its arm toward the late barrel: " + c.block(2, G, Z));

        // the multi identifier, primary oil
        c.cmd("clear");
        c.cmd("give @s " + multi(c, "hbm:oil") + " 1");
        c.waitTicks(3);
        sneakUse(c, 1, G + 0.5);
        for (int x = 1; x <= 2; x++) {
            Expect.that(nbt(c, x).contains("\"hbm:oil\""), "a sneak-click with the multi identifier retyped duct " + x + ": " + nbt(c, x));
        }
        sneakUse(c, 3, G + 0.99);
        Expect.that(nbt(c, 3).contains("\"hbm:oil\""), "a sneak-click with the multi identifier retyped the barrel: " + nbt(c, 3));
        Expect.noScreen(c);
        c.screenshot("02-multi-retyped");

        // a fluid tank south of the line
        Fixtures.giveOnly(c, "hbm:machine_fluidtank", 1);
        c.teleport(0.5, G, 9.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, 5.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(10);
        int[] tank = findBe(c, "fluid_tank", 0, 5, 3);
        Expect.that(tank != null, "the fluid tank was placed near 0 " + G + " 5");
        c.cmd("clear");
        c.cmd("give @s " + multi(c, "hbm:oil") + " 1");
        c.waitTicks(3);
        c.cmd("setblock " + tank[0] + " " + (G + 3) + " " + (tank[2] + 4) + " minecraft:stone");
        c.hotbar(1);
        c.teleport(tank[0] + 0.5, G + 4, tank[2] + 4.5);
        c.waitTicks(3);
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.lookAt(tank[0] + 0.5, G + 2.99, tank[2] + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(5);
        c.keyUp("key.sneak");
        String tankNbt = nbt(c, tank[0], tank[1], tank[2]);
        System.out.println("[Gh2] tank after sneak-click: " + tankNbt);
        Expect.that(tankNbt.contains("\"hbm:oil\""), "a sneak-click with the multi identifier retyped the fluid tank: " + tankNbt);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    private static void sneakUse(Client c, int x, double y) {
        c.hotbar(1);
        // crouch before aiming: sneaking lowers the eye, and the ray would pass under a thin duct
        c.keyDown("key.sneak");
        aim(c, x, y);
        c.waitTicks(3);
        c.lookAt(x + 0.5, y, Z + 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(5);
        c.keyUp("key.sneak");
        c.waitTicks(3);
    }

    private static void aim(Client c, int x, double y) {
        c.teleport(x + 0.5, G, Z + 2.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, y, Z + 0.5);
        c.waitTicks(1);
    }

    private static void place(Client c, String item, int x) {
        Fixtures.giveOnly(c, item, 1);
        aim(c, x, Y + 0.98);
        c.use(1);
        c.waitTicks(4);
        Expect.that(!c.block(x, G, Z).toString().contains("minecraft:air"), item + " placed at " + x);
    }

    private static void open(Client c, int x) {
        c.hotbar(9);
        aim(c, x, G + 0.5);
        c.use(1);
        c.waitScreen("", 5000);
        c.waitTicks(5);
        Expect.that(c.screen().has("slots"), "a container screen opened at " + x);
    }

    private static String identifier(Client c, String fluid) {
        return c.node().startsWith("1.20.") ? "hbm:fluid_identifier{\"hbm:fluid\":\"" + fluid + "\"}"
                : "hbm:fluid_identifier[hbm:fluid=\"" + fluid + "\"]";
    }

    private static String multi(Client c, String fluid) {
        return c.node().startsWith("1.20.") ? "hbm:fluid_identifier_multi{\"hbm:fluid_multi\":{fluid1:\"" + fluid + "\"}}"
                : "hbm:fluid_identifier_multi[hbm:fluid_multi={fluid1:\"" + fluid + "\"}]";
    }

    private static int[] findBe(Client c, String id, int x, int z, int r) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            JsonObject be = c.blockEntity(x + dx, G, z + dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.toString().toLowerCase().contains(id.replace("_", ""))) {
                return new int[]{x + dx, G, z + dz};
            }
        }
        return null;
    }

    private static String nbt(Client c, int x) {
        return nbt(c, x, G, Z);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return (be.has("nbt") ? be.get("nbt").getAsString() : be.toString()).replaceAll("\\s+", "").replace("\\\"", "\"");
    }

    private static int intOf(String nbt, String key) {
        Matcher m = Pattern.compile("(?:^|[{,])\"?" + Pattern.quote(key) + "\"?:(-?\\d+)[LlSsBb]?(?=[,}])").matcher(nbt);
        Expect.that(m.find(), "no " + key + " in " + nbt);
        return Integer.parseInt(m.group(1));
    }

    private static void clickFromInventory(Client c, String id, int machineSlot) {
        JsonObject slot = findSlot(c, id);
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(machineSlot);
        c.waitTicks(2);
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

    private static void close(Client c) {
        c.key("escape");
        c.waitTicks(5);
    }
}
