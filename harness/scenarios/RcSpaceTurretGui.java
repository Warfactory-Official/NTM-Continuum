import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * The turret screen over real clicks: the power switch and a target toggle, the biometry chip in slot 0 with a name
 * typed into the box and added with 1.7's button, then removed again, rounds shift-clicked in (they must land in the
 * ammo block 1-9, not the chip slot), the state kept after leaving and rejoining.
 */
public final class RcSpaceTurretGui implements Scenario {

    private static final int Y = 100, L = Y + 1;
    private static int TX = 0, TZ = 0;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 20);
        c.cmd("fill -10 " + L + " -10 10 " + (Y + 6) + " 10 minecraft:air");
        c.cmd("gamemode creative");
        c.cmd("clear");
        placeTurret(c);
        c.cmd("data merge block " + TX + " " + L + " " + TZ + " {power:5000L}");
        c.cmd("give @s hbm:turret_chip 1");
        c.cmd("give @s hbm:ammo_standard_bmg50_sp 64");
        c.cmd("give @s hbm:ammo_standard_r556_sp 10");
        c.waitTicks(4);

        c.teleport(TX + 4.5, L, TZ + 0.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.lookAt(TX + 0.5, L + 0.5, TZ + 0.5);
        c.waitTicks(2);
        c.use(2);
        JsonObject screen = c.waitScreen("Chekhov", 8000);
        screen = c.screen();
        System.out.println("[RcSpaceTurretGui] screen " + c.screenClass() + " slots " + screen.getAsJsonArray("slots").size());
        int left = screen.get("left").getAsInt(), top = screen.get("top").getAsInt();
        c.screenshot("01-open");

        c.click(left + 115 + 9, top + 26 + 9);   // power switch
        c.waitTicks(4);
        c.click(left + 22 + 5, top + 30 + 5);    // animals
        c.waitTicks(4);
        c.screenshot("02-on-animals");
        String nbt = flags(c);
        Expect.that(nbt.contains("isOn:1b") || nbt.contains("isOn:true"), "power switch turned the turret on: " + nbt);
        Expect.that(nbt.contains("targetAnimals:1b") || nbt.contains("targetAnimals:true"), "animals toggle on: " + nbt);

        // chip into slot 0 by a pickup/place pair, then a name
        int chipSlot = invSlot(c, "hbm:turret_chip");
        Expect.that(chipSlot >= 0, "chip in the inventory");
        c.clickSlot(chipSlot);
        c.waitTicks(1);
        c.clickSlot(0);
        c.waitTicks(3);
        c.click(left + 10 + 20, top + 65 + 5);   // the name box
        c.waitTicks(2);
        c.type("Alice");
        c.waitTicks(3);
        c.click(left + 7 + 9, top + 98 + 9);      // add name
        c.waitTicks(5);
        c.screenshot("03-chip-name");
        nbt = flags(c);
        Expect.that(nbt.contains("Alice"), "the name reached the chip: " + nbt);
        c.click(left + 43 + 9, top + 98 + 9);     // delete the listed name
        c.waitTicks(5);
        nbt = flags(c);
        Expect.that(!nbt.contains("Alice"), "the name was removed again: " + nbt);
        c.click(left + 10 + 20, top + 65 + 5);
        c.type("Bob");
        c.waitTicks(2);
        c.click(left + 7 + 9, top + 98 + 9);
        c.waitTicks(5);

        // shift-click rounds: .50 SP is accepted, 5.56 is not but 1.7 takes anything in the ammo block
        int ammoSlot = invSlot(c, "hbm:ammo_standard_bmg50_sp");
        Expect.that(ammoSlot >= 0, "rounds in the inventory");
        c.slotClick(ammoSlot, 0, "QUICK_MOVE");
        c.waitTicks(4);
        nbt = flags(c);
        Expect.that(nbt.contains("ammo_standard_bmg50_sp"), "shift-click moved the rounds into the turret: " + nbt);
        Expect.that(!nbt.contains("Slot:0b,count:64") && !nbt.contains("Slot:0b,Count:64"), "the rounds did not land in the chip slot");
        c.screenshot("04-loaded");
        c.key("escape");
        c.waitTicks(5);

        String before = flags(c);
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(30);
        String after = flags(c);
        System.out.println("[RcSpaceTurretGui] after rejoin " + after);
        Expect.that(after.contains("Bob") && after.contains("ammo_standard_bmg50_sp"), "chip name and rounds survive rejoin: " + after);
        Expect.that(after.contains("isOn:1b") || after.contains("isOn:true"), "on switch survives rejoin");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|NullPointerException|ClassCastException|IllegalStateException|Ticking (entity|block entity)");
    }

    private static void placeTurret(Client c) {
        Fixtures.giveOnly(c, "hbm:turret_chekhov", 1);
        c.waitTicks(3);
        c.teleport(4.5, Y + 1, 0.5);
        c.waitTicks(3);
        c.lookAt(0.9, Y + 0.98, 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                JsonObject be = c.blockEntity(dx, L, dz);
                if (be.has("block_entity") && be.toString().contains("turret_chekhov")) {
                    TX = dx;
                    TZ = dz;
                }
            }
        }
        Expect.that(c.blockEntity(TX, L, TZ).toString().contains("turret_chekhov"), "the Chekhov formed a core");
    }

    private static String flags(Client c) {
        return c.blockEntity(TX, L, TZ).toString().replaceAll("\\s", "");
    }

    private static int invSlot(Client c, String id) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) {
                return s.get("index").getAsInt();
            }
        }
        return -1;
    }
}
