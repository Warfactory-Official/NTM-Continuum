import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 183, as 1.12.2, through the barrel GUI: a portable barrel takes 15,950 mB and moves out; a canister and a
 * gas tank fill only whole ({@code FluidLoaderStandard}) and are left alone.
 */
public final class Gh183BarrelPartFill implements Scenario {

    private static final int Y = 100, G = Y + 1;
    /** The barrel menu's fill-in slot (1.7 {@code unloadTank(4, 5)}). */
    private static final int FILL_IN = 4, FILL_OUT = 5;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(180_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 6);
        c.cmd("gamemode creative");
        c.cmd("fill -4 " + G + " -4 4 " + (G + 3) + " 4 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.cmd("clear @s");

        fill(c, "hbm:fluid_barrel", "hbm:gas", 15_950, "barrel");
        String block = squash(c.blockEntity(0, G, 2).toString());
        String out = squash(slotAfter(c, FILL_OUT));
        Expect.that(block.contains("tank:0") && block.contains("15950"), "barrel: the item did not take all 15,950 mB: " + block);
        Expect.that(out.contains("hbm:fluid_barrel"), "barrel: the filled item is not in the output slot: " + out);

        whole(c, "hbm:canister_empty", "hbm:diesel", 999, "canister");
        whole(c, "hbm:gas_empty", "hbm:gas", 400, "gas-tank");

        Expect.logCleanSinceMark(c, "(?i)exception.*(hbm|barrel|fluid)");
    }

    private static void whole(Client c, String item, String fluid, int amount, String tag) {
        String slot = squash(fill(c, item, fluid, amount, tag));
        String block = squash(c.blockEntity(0, G, 2).toString());
        Expect.that(block.contains("tank:" + amount), tag + ": part of " + amount + " mB left the barrel: " + block);
        Expect.that(slot.contains(item), tag + ": the empty container did not stay in the fill slot: " + slot);
    }

    private static String slotAfter(Client c, int index) {
        Fixtures.interact(c, 0, G, 2);
        c.waitScreen("Barrel", 5_000L);
        String slot = slot(c, index).toString();
        c.key("escape");
        c.waitTicks(5);
        return slot;
    }

    private static String fill(Client c, String item, String fluid, int amount, String tag) {
        Fixtures.setBlock(c, 0, G, 2, "minecraft:air");
        Fixtures.placeOnTop(c, "hbm:barrel_steel", 0, Y, 2);
        c.cmd("data merge block 0 " + G + " 2 {tank:" + amount + ",tank_type:\"" + fluid + "\"}");
        Fixtures.giveOnly(c, item, 1);
        c.hotbar(2);
        c.waitTicks(5);
        Fixtures.interact(c, 0, G, 2);
        c.waitScreen("Barrel", 5_000L);
        c.clickSlot(slotHolding(c, item));
        c.clickSlot(FILL_IN);
        c.waitTicks(20);
        c.screenshot("gh183-" + tag + "-gui");
        JsonObject slot = slot(c, FILL_IN);
        System.out.println("[Gh183] " + tag + " fill slot " + slot);
        c.key("escape");
        c.waitTicks(5);
        return slot.toString();
    }

    private static String squash(String text) {
        return text.replaceAll("\\s+", "");
    }

    private static int slotHolding(Client c, String item) {
        for (JsonElement e : slots(c)) {
            JsonObject s = e.getAsJsonObject();
            if (s.get("player_inventory").getAsBoolean() && s.toString().contains(item)) return s.get("index").getAsInt();
        }
        throw new IllegalStateException("no " + item + " in the player's inventory: " + c.screen());
    }

    private static JsonObject slot(Client c, int index) {
        for (JsonElement e : slots(c)) {
            if (e.getAsJsonObject().get("index").getAsInt() == index) return e.getAsJsonObject();
        }
        return new JsonObject();
    }

    private static JsonArray slots(Client c) {
        return c.screen().getAsJsonArray("slots");
    }
}
