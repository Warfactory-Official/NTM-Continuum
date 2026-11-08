import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 142. A conveyor carries coal and charcoal into conveyor inserters that face a firebox and a heating oven
 * through a ring cell, as 1.7's inventory proxies allow. A Fluid Burner heats a crucible and a Stirling engine set on
 * top of it through its heat cell.
 */
public final class Gh142HeaterAutomation implements Scenario {

    private static final int Y = 100, G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 30);
        c.cmd("fill -30 " + G + " -12 30 " + (Y + 8) + " 12 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("gamemode creative");
        c.cmd("gamerule sendCommandFeedback false");
        c.cmd("gamerule send_command_feedback false");

        int[] fireboxCore = place(c, "hbm:heater_firebox", 0, Y, 0);
        int fx = fireboxCore[0], fz = fireboxCore[2];
        Fixtures.setBlock(c, fx + 2, G, fz, "hbm:crane_inserter[input=east,output=west]");
        for (int x = fx + 3; x <= fx + 5; x++) Fixtures.setBlock(c, x, G, fz, "hbm:conveyor[facing=west]");

        int[] ovenCore = place(c, "hbm:heater_oven", -8, Y, 0);
        int ox = ovenCore[0], oz = ovenCore[2];
        Fixtures.setBlock(c, ox - 2, G, oz, "hbm:crane_inserter[input=west,output=east]");
        for (int x = ox - 5; x <= ox - 3; x++) Fixtures.setBlock(c, x, G, oz, "hbm:conveyor[facing=east]");

        int[] crucibleCore = consumerOnBurner(c, "hbm:machine_crucible", 10);
        int[] stirlingBurner = burnerUnder(consumerOnBurner(c, "hbm:machine_stirling", 20));
        int[] control = place(c, "hbm:heater_oilburner", -20, Y, 0);

        c.cmd("summon minecraft:item " + (fx + 5.5) + " " + (G + 0.3) + " " + (fz + 0.5) + " {Item:{id:\"minecraft:coal\",count:4,Count:4b},PickupDelay:32767s}");
        c.cmd("summon minecraft:item " + (ox - 4.5) + " " + (G + 0.3) + " " + (oz + 0.5) + " {Item:{id:\"minecraft:charcoal\",count:4,Count:4b},PickupDelay:32767s}");

        for (int[] core : new int[][]{crucibleCore, stirlingBurner, control}) {
            int y = core == crucibleCore ? core[1] - 2 : core[1];
            c.cmd("data merge block " + core[0] + " " + y + " " + core[2] + " {heatEnergy:1000}");
        }
        boolean firebox = false, oven = false;
        for (int i = 0; i < 40 && !(firebox && oven); i++) {
            c.waitTicks(5);
            firebox = fuelled(c, fx, G, fz, "minecraft:coal");
            oven = fuelled(c, ox, G, oz, "minecraft:charcoal");
        }
        System.out.println("[Gh142] firebox " + nbt(c, fx, G, fz));
        System.out.println("[Gh142] oven " + nbt(c, ox, G, oz));

        int crucible = intTag(nbt(c, crucibleCore[0], crucibleCore[1], crucibleCore[2]), "heat");
        int left = intTag(nbt(c, stirlingBurner[0], stirlingBurner[1], stirlingBurner[2]), "heatEnergy");
        int alone = intTag(nbt(c, control[0], control[1], control[2]), "heatEnergy");
        System.out.println("[Gh142] crucible heat " + crucible + ", burner under the Stirling engine " + left + " TU, alone " + alone + " TU");
        c.cmd("clear @s");
        c.teleport(0.5, G + 6, 14.5);
        c.waitTicks(2);
        c.lookAt(0.5, G, 0.5);
        c.waitTicks(10);
        c.screenshot("gh142-heaters");

        Expect.that(firebox, "coal on the conveyor reached the firebox through a ring cell");
        Expect.that(oven, "charcoal on the conveyor reached the heating oven through a ring cell");
        Expect.that(crucible > 0, "the crucible on a Fluid Burner took no heat");
        // alone a burner cools by 1 TU a tick; the engine takes a tenth of it a tick
        Expect.that(left < alone - 100, "the Stirling engine on a Fluid Burner took no heat: " + left + " TU left");

        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointer|ClassCast|IllegalState");
    }

    /** Places a machine by hand, sneaking, on the top of a block, facing north, and returns its core. */
    private static int[] place(Client c, String item, int x, int y, int z) {
        Fixtures.giveOnly(c, item, 1);
        // a step to stand on, so the eye is above the clicked top face
        if (y > Y) Fixtures.setBlock(c, x, y, z + 4, "minecraft:glass");
        c.teleport(x + 0.5, y + 1, z + 4.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, y + 0.98, z + 0.5);
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.use(2);
        c.waitTicks(2);
        c.keyUp("key.sneak");
        c.waitTicks(4);
        if (y > Y) Fixtures.setBlock(c, x, y, z + 4, "minecraft:air");
        for (int dy = 1; dy <= 3; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -3; dz <= 2; dz++) {
                    JsonObject b = c.block(x + dx, y + dy, z + dz);
                    if (b.get("block").getAsString().equals(item) && b.toString().contains("\"cell\":\"false\"")) {
                        return new int[]{x + dx, y + dy, z + dz};
                    }
                }
            }
        }
        throw new AssertionError(item + " placed by hand left no core near " + x + "," + y + "," + z);
    }

    /** A Fluid Burner, and the consumer placed by hand so its core sits on the burner's heat cell. */
    private static int[] consumerOnBurner(Client c, String item, int x) {
        int[] burner = place(c, "hbm:heater_oilburner", x, Y, 0);
        int[] probe = place(c, item, x + 6, Y, 0);
        int dx = probe[0] - (x + 6), dz = probe[2];
        c.cmd("setblock " + probe[0] + " " + probe[1] + " " + probe[2] + " minecraft:air");
        c.waitTicks(4);
        int cx = burner[0] - dx, cy = burner[1] + 1, cz = burner[2] - dz;
        int[] consumer = place(c, item, cx, cy, cz);
        Expect.that(consumer[0] == burner[0] && consumer[1] == burner[1] + 2 && consumer[2] == burner[2],
                item + " core at " + consumer[0] + "," + consumer[1] + "," + consumer[2] + " is not on the burner's heat cell");
        return consumer;
    }

    private static int[] burnerUnder(int[] consumer) {
        return new int[]{consumer[0], consumer[1] - 2, consumer[2]};
    }

    /** The fuel sits in a slot, or the first item already burns. */
    private static boolean fuelled(Client c, int x, int y, int z, String item) {
        String raw = nbt(c, x, y, z);
        return raw.contains(item) || intTag(raw, "burnTime") > 0;
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") && !be.get("nbt").isJsonNull() ? be.get("nbt").getAsString().replaceAll("\\s+", "") : be.toString();
    }

    private static int intTag(String raw, String key) {
        Matcher m = Pattern.compile("[{,]" + key + ":(-?\\d+)").matcher(raw);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
