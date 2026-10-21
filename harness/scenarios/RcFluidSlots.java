import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Scenario;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Container slots of the tank machines through the menu: a full canister put in the load slot is
 * emptied into the tank and leaves an empty canister in the result slot; an empty canister put in the fill slot
 * comes out full. Checked on the barrel, the fluid tank, the derrick and the reformer.
 */
public final class RcFluidSlots implements Scenario {

    private static final int X = 700;
    private static final int Y = 100;
    private static final int L = Y + 1;
    private static final int Z = 0;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        c.cmd("gamemode creative");
        c.cmd("forceload add " + (X - 16) + " " + (Z - 16) + " " + (X + 80) + " " + (Z + 16));
        c.waitTicks(40);
        c.cmd("fill " + (X - 8) + " " + Y + " " + (Z - 8) + " " + (X + 70) + " " + Y + " " + (Z + 8) + " minecraft:stone");
        c.cmd("fill " + (X - 8) + " " + L + " " + (Z - 8) + " " + (X + 70) + " " + (L + 12) + " " + (Z + 8) + " minecraft:air");

        // barrel: tank slots 2 load / 3 result, 4 fill / 5 result
        machine(c, 0, "hbm:barrel_steel{tank:3000,tank_max:16000,tank_type:\"hbm:oil\",tank_p:0s,mode:0s}", "tank", 2, 3, 4, 5);
        // fluid tank: the same four slots
        machine(c, 10, "hbm:machine_fluidtank[facing=north]{tank:3000,tank_type:\"hbm:oil\"}", "tank", 2, 3, 4, 5);
        // derrick: oil tank unloaded through slots 1 and 2 (1.7 unloadTank(1, 2)): an empty canister is filled
        machine(c, 20, "hbm:machine_well[facing=north]{t0:3000,t0_type:\"hbm:oil\"}", "t0", -1, -1, 1, 2);
        // reformer: naphtha input loaded through 1 and 2 (loadTank(1, 2)): a full canister is emptied
        machine(c, 30, "hbm:machine_catalytic_reformer[facing=north]{input:0,input_type:\"hbm:naphtha\"}", "input", 1, 2, -1, -1);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Exception");
    }

    private void machine(Client c, int dx, String state, String key, int loadIn, int loadOut, int fillIn, int fillOut) {
        int x = X + dx;
        c.cmd("setblock " + x + " " + L + " " + Z + " " + state);
        c.waitTicks(10);
        String fluid = state.contains("naphtha") ? "hbm:naphtha" : "hbm:oil";
        String name = state.substring(0, state.indexOf('[') > 0 ? state.indexOf('[') : state.indexOf('{'));
        int start = nbtInt(c, x, key);
        System.out.println("[RcSlots] " + name + " start " + key + "=" + start);
        c.teleport(x + 0.5, L, Z + 4.5);
        c.waitTicks(3);

        if (loadIn >= 0) {
            c.cmd("clear");
            c.cmd("give @s " + full(c, fluid) + " 2");
            open(c, x);
            int inv = findInventory(c, "hbm:canister_full");
            Expect.that(inv >= 0, name + ": the full canister is in the player's inventory");
            c.slotClick(inv, 0, "PICKUP");
            c.slotClick(loadIn, 0, "PICKUP");
            c.waitTicks(40);
            JsonObject screen = c.screen();
            System.out.println("[RcSlots] " + name + " after placing full canister: " + machineSlots(screen));
            int moved = nbtInt(c, x, key);
            Expect.that(moved > start, name + ": the full canisters must be emptied into the tank: " + start + " -> " + moved);
            Expect.that(machineSlotHolds(screen, loadOut, "hbm:canister_empty"),
                    name + ": the emptied canister must come out in slot " + loadOut + ": " + machineSlots(screen));
            c.screenshot(name.replace(':', '_') + "-load");
            c.key("escape");
            c.waitTicks(5);
            start = moved;
        }
        if (fillIn >= 0) {
            c.cmd("clear");
            c.cmd("give @s hbm:canister_empty 2");
            open(c, x);
            int inv = findInventory(c, "hbm:canister_empty");
            Expect.that(inv >= 0, name + ": the empty canister is in the player's inventory");
            c.slotClick(inv, 0, "PICKUP");
            c.slotClick(fillIn, 0, "PICKUP");
            c.waitTicks(60);
            JsonObject screen = c.screen();
            System.out.println("[RcSlots] " + name + " after placing empty canister: " + machineSlots(screen));
            int left = nbtInt(c, x, key);
            Expect.that(left < start, name + ": the tank must fill the empty canisters: " + start + " -> " + left);
            Expect.that(machineSlotHolds(screen, fillOut, "hbm:canister_full"),
                    name + ": the filled canister must come out in slot " + fillOut + ": " + machineSlots(screen));
            c.screenshot(name.replace(':', '_') + "-fill");
            // shift-click the result back out into the inventory
            c.slotClick(fillOut, 0, "QUICK_MOVE");
            c.waitTicks(5);
            Expect.that(!machineSlotHolds(c.screen(), fillOut, "hbm:canister_full"), name + ": the result slot must empty by shift-click");
            c.key("escape");
            c.waitTicks(5);
        }
    }

    private static String full(Client c, String fluid) {
        return c.node().startsWith("1.20.") ? "hbm:canister_full{\"hbm:fluid\":\"" + fluid + "\",\"hbm:fluid_amount\":1000}"
                : "hbm:canister_full[hbm:fluid=\"" + fluid + "\",hbm:fluid_amount=1000]";
    }

    private static void open(Client c, int x) {
        c.hotbar(9);
        c.lookAt(x + 0.5, L + 0.5, Z + 0.5);
        c.waitTicks(2);
        c.use(1);
        c.waitScreen("", 5000);
        c.waitTicks(5);
    }

    private static int findInventory(Client c, String id) {
        JsonObject screen = c.screen();
        if (!screen.has("slots")) return -1;
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.has("id") && id.equals(s.get("id").getAsString())) return s.get("index").getAsInt();
        }
        return -1;
    }

    private static boolean machineSlotHolds(JsonObject screen, int index, String id) {
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("player_inventory").getAsBoolean() && s.get("index").getAsInt() == index) {
                return s.has("id") && id.equals(s.get("id").getAsString());
            }
        }
        return false;
    }

    private static String machineSlots(JsonObject screen) {
        StringBuilder out = new StringBuilder();
        for (JsonElement e : screen.getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (!s.get("player_inventory").getAsBoolean() && s.has("id")) {
                out.append(s.get("index").getAsInt()).append('=').append(s.get("id").getAsString())
                        .append(s.has("count") ? "x" + s.get("count").getAsInt() : "").append(' ');
            }
        }
        return out.toString();
    }

    private static int nbtInt(Client c, int x, String key) {
        JsonObject be = c.blockEntity(x, L, Z);
        String nbt = (be.has("nbt") ? be.get("nbt").getAsString() : be.toString()).replaceAll("\\s+", "").replace("\\\"", "\"");
        Matcher m = Pattern.compile("(?:^|[{,])\"?" + Pattern.quote(key) + "\"?:(-?\\d+)[LlSsBb]?(?=[,}])").matcher(nbt);
        Expect.that(m.find(), "no " + key + " in " + nbt);
        return Integer.parseInt(m.group(1));
    }
}
