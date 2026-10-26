import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issues 75 to 77. A supply crate broken in survival drops itself with its contents and places back full. The
 * B92 in first person, third person and on the ground (shots to compare with 1.7). The radiation-powered engine
 * takes 1.7's fuels by hand and from a hopper, and refuses a waste barrel as 1.7 does.
 */
public final class Gh75to77 implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        Fixtures.platform(c, 0, Y, 0, 12);
        clear(c);
        c.cmd("gamemode creative");
        c.markLog();
        supplyCrate(c);
        b92(c);
        radgen(c);
        Expect.logCleanSinceMark(c, "(?i)(exception|/(ERROR|FATAL)[]]).*");
    }

    private static void supplyCrate(Client c) {
        int x = 0, z = 3;
        Fixtures.setBlock(c, x, G, z, "hbm:crate_supply");
        c.cmd("item replace block " + x + " " + G + " " + z + " container.0 with minecraft:diamond 7");
        c.waitTicks(3);
        c.cmd("clear");
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:haste infinite 120 true");
        Fixtures.give(c, "minecraft:diamond_axe", 1);
        c.hotbar(1);
        c.teleport(x + 0.5, G, z - 2 + 0.5);
        c.waitTicks(5);
        c.lookAt(x + 0.5, G + 0.5, z + 0.5);
        c.waitTicks(3);
        c.attack(1);
        c.waitTicks(10);
        c.teleport(x + 0.5, G, z + 0.5);
        c.waitTicks(40);
        Expect.that(c.block(x, G, z).get("block").getAsString().equals("minecraft:air"), "the supply crate broke");
        int slot = hotbarSlot(c, "hbm:crate_supply");
        System.out.println("[Gh75] inventory after breaking: " + c.inventory());
        Expect.that(slot >= 0, "the broken supply crate dropped itself");
        c.cmd("effect clear @s");

        c.hotbar(slot + 1);
        c.teleport(x + 0.5, G, z - 2 + 0.5);
        c.waitTicks(3);
        c.lookAt(x + 0.5, Y + 0.99, z + 0.5);
        c.waitTicks(3);
        c.use(1);
        c.waitTicks(10);
        String nbt = c.blockEntity(x, G, z).toString();
        System.out.println("[Gh75] placed back: " + nbt);
        Expect.that(nbt.contains("diamond"), "the placed-back supply crate holds its diamonds: " + nbt);
        c.cmd("gamemode creative");
        clear(c);
    }

    private static void b92(Client c) {
        c.teleport(0.5, G, -4.5);
        c.look(0, 10);
        Fixtures.giveOnly(c, "hbm:gun_b92", 1);
        c.waitTicks(20);
        c.screenshot("b92-first-person");
        c.key("f5");
        c.waitTicks(10);
        c.screenshot("b92-third-person-back");
        c.key("f5");
        c.waitTicks(10);
        c.screenshot("b92-third-person-front");
        c.key("f5");
        c.waitTicks(5);
        c.cmd("summon item 0.5 " + G + " -2.5 {Item:{id:\"hbm:gun_b92\",count:1,Count:1b},PickupDelay:32767,NoGravity:1b}");
        c.lookAt(0.5, G + 0.2, -2.5);
        c.waitTicks(10);
        c.screenshot("b92-dropped");
        c.cmd("kill @e[type=item]");
    }

    private static void radgen(Client c) {
        int oz = -10;
        c.cmd("clear");
        Fixtures.giveOnly(c, "hbm:machine_radgen", 1);
        c.teleport(0.5, G, oz + 4.5);
        c.waitTicks(5);
        c.lookAt(0.5, Y + 0.98, oz + 1.5);
        c.waitTicks(5);
        c.use(2);
        c.waitTicks(10);
        int[] core = null;
        for (int x = -6; x <= 6 && core == null; x++) for (int z = oz - 6; z <= oz + 6 && core == null; z++) {
            for (int y = G; y <= G + 4; y++) {
                if (c.block(x, y, z).get("block").getAsString().equals("hbm:machine_radgen")
                        && c.blockEntity(x, y, z).toString().contains("maxProgress")) {
                    core = new int[] {x, y, z};
                    break;
                }
            }
        }
        Expect.that(core != null, "the radiation-powered engine was placed by hand");
        System.out.println("[Gh77] core at " + core[0] + "," + core[1] + "," + core[2]);

        c.cmd("clear");
        c.cmd("give @s hbm:nuclear_waste_short_uranium235 4");
        c.cmd("give @s hbm:scrap_nuclear 4");
        c.cmd("give @s hbm:yellow_barrel 4");
        c.teleport(core[0] + 0.5, G, oz + 4.5);
        c.waitTicks(3);
        c.lookAt(core[0] + 0.5, G + 0.5, core[2] + 0.5);
        c.waitTicks(3);
        c.use(1);
        c.waitTicks(10);
        Expect.screenOpen(c, "Radiation");
        c.slotClick(playerSlotWith(c, "hbm:nuclear_waste_short_uranium235"), 0, "QUICK_MOVE");
        c.slotClick(playerSlotWith(c, "hbm:yellow_barrel"), 0, "QUICK_MOVE");
        c.waitTicks(5);
        c.screenshot("radgen-menu");
        c.key("escape");
        c.waitTicks(10);
        String nbt = c.blockEntity(core[0], core[1], core[2]).toString();
        System.out.println("[Gh77] after shift-clicks: " + nbt);
        Expect.that(nbt.contains("nuclear_waste_short"), "the engine took nuclear waste by hand: " + nbt);
        Expect.that(!nbt.contains("yellow_barrel"), "the engine refused the waste barrel: " + nbt);

        // a hopper on top of the core, fed scrap
        int hy = core[1] + 1;
        while (!c.block(core[0], hy, core[2]).get("block").getAsString().equals("minecraft:air")) hy++;
        c.cmd("setblock " + core[0] + " " + hy + " " + core[2] + " minecraft:hopper[facing=down]");
        c.waitTicks(2);
        c.cmd("item replace block " + core[0] + " " + hy + " " + core[2] + " container.0 with hbm:scrap_nuclear 4");
        String top = c.block(core[0], hy - 1, core[2]).get("block").getAsString();
        System.out.println("[Gh77] block under the hopper: " + top);
        c.waitTicks(80);
        String hopper = c.blockEntity(core[0], hy, core[2]).toString();
        nbt = c.blockEntity(core[0], core[1], core[2]).toString();
        System.out.println("[Gh77] hopper after: " + hopper + "\n[Gh77] engine after: " + nbt);
        Expect.that(nbt.contains("scrap_nuclear"), "the engine took scrap from a hopper: " + nbt);
        c.screenshot("radgen-hopper");
        clear(c);
    }

    private static int hotbarSlot(Client c, String item) {
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("id").getAsString().equals(item) && s.get("slot").getAsInt() < 9) return s.get("slot").getAsInt();
        }
        return -1;
    }

    private static int playerSlotWith(Client c, String item) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && !s.get("empty").getAsBoolean()
                    && s.get("id").getAsString().equals(item)) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no player slot holding " + item);
    }

    private static void clear(Client c) {
        c.cmd("fill -12 " + G + " -16 12 " + (Y + 10) + " 12 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.waitTicks(3);
    }
}
