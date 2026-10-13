import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub issue 4: fluid containers clicked into the Soyuz launcher's GUI did nothing. 1.7 {@code tanks[i].loadTank}
 * drains a kerosene container in slot 4 (empty to slot 5) and an oxygen container in slot 6 (empty to slot 7). The
 * launcher is placed by hand and opened from a stone ledge west of its rim, as in {@code Pt1012Soyuz}.
 */
public final class Gh4SoyuzContainers implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;
    private static final int CY = G + 4;
    private static final int LEDGE = CY - 2;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 20);
        c.cmd("fill -15 " + G + " -15 15 " + (Y + 30) + " 15 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.markLog();

        Fixtures.giveOnly(c, "hbm:soyuz_launcher", 1);
        c.teleport(0.5, G, -3.5);
        c.waitTicks(3);
        c.lookAt(0.5, Y + 0.98, 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(10);
        Expect.that(c.blockEntity(0, CY, 0).toString().contains("soyuz_launcher") || c.blockEntity(0, CY, 0).toString().contains("SoyuzLauncher"),
                "the launch complex formed its core at 0 " + CY + " 0: " + c.blockEntity(0, CY, 0));
        c.cmd("fill -13 " + LEDGE + " -6 -7 " + LEDGE + " 6 minecraft:stone");
        c.cmd("clear");
        c.cmd("give @s " + container(c, "hbm:canister_full", "hbm:kerosene") + " 2");
        c.cmd("give @s " + container(c, "hbm:gas_full", "hbm:oxygen") + " 1");
        c.waitTicks(3);

        c.teleport(-9.5, CY - 1, 3.5);
        c.waitTicks(3);
        c.hotbar(9);
        c.lookAt(-5.95, CY - 0.5, 3.5);
        c.waitTicks(1);
        c.use(2);
        c.waitScreen("Soyuz", 5000);
        c.waitTicks(5);
        clickFromInventory(c, "hbm:canister_full", 4);
        clickFromInventory(c, "hbm:gas_full", 6);
        c.waitTicks(20);
        c.screenshot("01-gui-containers");
        String pad = nbt(c, 0, CY, 0);
        System.out.println("[Gh4] launcher after the containers: " + pad);
        Expect.that(intOf(pad, "fuel") == 2000, "two kerosene canisters drained into the fuel tank: " + pad);
        Expect.that(intOf(pad, "oxidizer") == 1000, "the oxygen tank drained into the oxidizer tank: " + pad);
        // 1.7 FluidContainerRegistry: an emptied canister / gas tank is canister_empty / gas_empty
        Expect.that(slot(c, 4).isEmpty() && emptied(pad, 5, "hbm:canister_empty", 2), "empty canisters in slot 5: " + pad);
        Expect.that(slot(c, 6).isEmpty() && emptied(pad, 7, "hbm:gas_empty", 1), "empty gas tank in slot 7: " + pad);
        c.key("escape");
        c.waitTicks(5);
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

    private static String container(Client c, String item, String fluid) {
        return c.node().startsWith("1.20.")
                ? item + "{\"hbm:fluid\":\"" + fluid + "\",\"hbm:fluid_amount\":1000}"
                : item + "[hbm:fluid=\"" + fluid + "\",hbm:fluid_amount=1000]";
    }

    /** "id xN" for a filled slot, "" for an empty one. */
    private static String slot(Client c, int index) {
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("index").getAsInt() != index) continue;
            if (!s.has("id") || s.get("id").getAsString().equals("minecraft:air")) return "";
            return s.get("id").getAsString() + " x" + (s.has("count") ? s.get("count").getAsInt() : 1);
        }
        return "";
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
        JsonObject slot = null;
        for (JsonElement e : c.screen().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) slot = s;
        }
        Expect.that(slot != null, "inventory slot holding " + id);
        c.clickSlot(slot.get("index").getAsInt());
        c.waitTicks(1);
        c.clickSlot(machineSlot);
        c.waitTicks(2);
    }
}
