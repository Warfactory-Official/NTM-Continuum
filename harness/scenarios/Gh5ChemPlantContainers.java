import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 5: a full fluid container clicked into a chemical plant load slot whose tank has no fluid set lost
 * its fluid. In 1.7 ({@code FluidTank.loadTank}) every loader asks for the tank's own type, so such a tank takes
 * nothing and the container stays. The scenario clicks a kerosene canister into load slot 11 (tank 1, untyped) and
 * checks it stays full and the tank stays empty; then types tank 0 as kerosene and checks a canister in slot 10
 * drains into it and the empty canister lands in slot 13.
 */
public final class Gh5ChemPlantContainers implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 10);
        c.cmd("fill -6 " + G + " -6 6 " + (Y + 10) + " 6 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.markLog();

        Fixtures.giveOnly(c, "hbm:machine_chemical_plant", 1);
        c.teleport(0.5, G, -3.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(10);
        int[] core = findCore(c);
        Expect.that(core != null, "the chemical plant formed a core");
        System.out.println("[Gh5] core " + core[0] + " " + core[1] + " " + core[2]);

        // untyped tank 1: the canister must stay full in its slot
        c.cmd("clear");
        c.cmd("give @s " + canister(c, "hbm:kerosene") + " 1");
        c.waitTicks(3);
        open(c, core);
        clickFromInventory(c, "hbm:canister_full", 11);
        c.waitTicks(40);
        c.screenshot("01-untyped-tank");
        String nbt = nbt(c, core);
        System.out.println("[Gh5] after 40 ticks in slot 11: " + nbt);
        Expect.that(intOf(nbt, "i1") == 0, "the untyped tank 1 took nothing: " + nbt);
        Expect.that(slotId(c, 11).equals("hbm:canister_full") && slotId(c, 14).equals("minecraft:air"),
                "the full canister stays in load slot 11 (out slot 14 empty): " + c.screen());
        close(c);

        // typed tank 0: the canister drains in, the empty one goes to slot 13
        c.cmd("data merge block " + core[0] + " " + core[1] + " " + core[2] + " {i0_type:\"hbm:kerosene\"}");
        c.cmd("clear");
        c.cmd("give @s " + canister(c, "hbm:kerosene") + " 1");
        c.waitTicks(3);
        open(c, core);
        clickFromInventory(c, "hbm:canister_full", 10);
        c.waitTicks(40);
        c.screenshot("02-typed-tank");
        nbt = nbt(c, core);
        System.out.println("[Gh5] after 40 ticks in slot 10: " + nbt);
        Expect.that(intOf(nbt, "i0") == 1000, "tank 0 (kerosene) took the canister's 1,000 mB: " + nbt);
        // 1.7 FluidContainerRegistry: an emptied canister is canister_empty
        Expect.that(slotId(c, 10).equals("minecraft:air") && emptied(nbt, 13, "hbm:canister_empty", 1),
                "the emptied canister moved to slot 13: " + nbt);
        Expect.that(intOf(nbt, "i1") == 0 && slotId(c, 11).equals("hbm:canister_full"),
                "the untyped tank 1 still took nothing");
        close(c);

        // 1.7 unloadTank: an empty canister in fluid-out slot 16 is filled whole from output tank 0, full one to slot 19
        String at = core[0] + " " + core[1] + " " + core[2];
        c.cmd("data merge block " + at + " {o0_type:\"hbm:kerosene\",o0:2500}");
        c.cmd("clear");
        c.cmd("give @s hbm:canister_empty 3");
        c.waitTicks(3);
        open(c, core);
        clickFromInventory(c, "hbm:canister_empty", 16);
        c.waitTicks(40);
        c.screenshot("03-unload");
        nbt = nbt(c, core);
        System.out.println("[Gh5] after 40 ticks with empty canisters in slot 16: " + nbt);
        Expect.that(intOf(nbt, "o0") == 500, "two whole canisters left output tank 0 (2,500 -> 500 mB): " + nbt);
        Expect.that(savedSlot(nbt, 19).contains("hbm:canister_full") && savedSlot(nbt, 19).contains("kerosene")
                        && savedSlot(nbt, 19).matches(".*[cC]ount:2b?[,}].*"), "two full kerosene canisters in slot 19: " + nbt);
        Expect.that(emptied(nbt, 16, "hbm:canister_empty", 1), "the third canister waits in slot 16 (500 mB is not a canister): " + nbt);
        close(c);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    /** The saved stack in machine slot {@code slot}, any version's spelling; "" when the slot is empty. */
    private static String savedSlot(String nbt, int slot) {
        int at = nbt.indexOf("Slot:" + slot + "b");
        if (at < 0) return "";
        int start = nbt.lastIndexOf('{', at);
        int depth = 0;
        for (int i = start; i < nbt.length(); i++) {
            if (nbt.charAt(i) == '{') depth++;
            else if (nbt.charAt(i) == '}' && --depth == 0) return nbt.substring(start, i + 1);
        }
        return "";
    }

    /** {@code count} of {@code id} holding no fluid in machine slot {@code slot}. */
    private static boolean emptied(String nbt, int slot, String id, int count) {
        String s = savedSlot(nbt, slot);
        return s.contains("\"" + id + "\"") && s.matches(".*[cC]ount:" + count + "b?[,}].*") && !s.contains("hbm:fluid");
    }

    private static String canister(Client c, String fluid) {
        return c.node().startsWith("1.20.")
                ? "hbm:canister_full{\"hbm:fluid\":\"" + fluid + "\",\"hbm:fluid_amount\":1000}"
                : "hbm:canister_full[hbm:fluid=\"" + fluid + "\",hbm:fluid_amount=1000]";
    }

    private static void open(Client c, int[] core) {
        c.teleport(core[0] + 0.5, G, core[2] - 3.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.waitTicks(1);
        c.lookAt(core[0] + 0.5, G + 0.5, core[2] - 1.0);
        c.waitTicks(1);
        c.use(1);
        c.waitScreen("", 5000);
        c.waitTicks(5);
        Expect.that(c.screen().has("slots"), "the chemical plant menu opened");
    }

    private static int[] findCore(Client c) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -1; dz <= 3; dz++) {
            JsonObject be = c.blockEntity(dx, G, dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull() && be.toString().contains("chemical_plant")) {
                return new int[]{dx, G, dz};
            }
        }
        return null;
    }

    private static String nbt(Client c, int[] p) {
        JsonObject be = c.blockEntity(p[0], p[1], p[2]);
        return (be.has("nbt") ? be.get("nbt").getAsString() : be.toString()).replaceAll("\\s+", "").replace("\\\"", "\"");
    }

    private static int intOf(String nbt, String key) {
        Matcher m = Pattern.compile("(?:^|[{,])\"?" + Pattern.quote(key) + "\"?:(-?\\d+)[LlSsBb]?(?=[,}])").matcher(nbt);
        Expect.that(m.find(), "no " + key + " in " + nbt);
        return Integer.parseInt(m.group(1));
    }

    private static String slotId(Client c, int index) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("index").getAsInt() == index) return s.has("id") ? s.get("id").getAsString() : "minecraft:air";
        }
        return "minecraft:air";
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
