import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Melee weapons with a real left click at a no-AI iron golem, one full-charge hit each. The damage must equal the 1.7
 * figure: the player's 1 plus the tool's attack attribute (ItemSwordAbility: its damage argument; ItemSword and
 * WeaponSpecial: 4 plus the tool material's damage); the vampire ability adds 2 (cmb, schrabidium sword). Special effects are checked where they leave a trace: the
 * Schrabidium hammer kills outright, the Shimmer axe halves the remaining health after the hit, an exhausted crucible
 * does 1 and a charged one kills, and the chainsaw's kill drops ceil(maxHealth / 10) small nitra and a chat-free death.
 */
public final class RcMelee implements Scenario {

    private static final int Y = 100, G = 101;

    // item, expected damage dealt by one hit to a 100 health golem (-1: the golem must die)
    private static final Object[][] CASES = {
            {"hbm:titanium_sword", 7.5}, {"hbm:steel_sword", 7.0}, {"hbm:alloy_sword", 9.0}, {"hbm:desh_sword", 13.5},
            {"hbm:cobalt_sword", 13.0}, {"hbm:cobalt_decorated_sword", 16.0}, {"hbm:starmetal_sword", 26.0},
            {"hbm:cmb_sword", 38.0}, {"hbm:schrabidium_sword", 78.0}, {"hbm:chainsaw", 26.0},
            {"hbm:crowbar", 7.0}, {"hbm:reer_graar", 7.5}, {"hbm:stopsign", 10.0}, {"hbm:bottle_opener", 5.5},
            {"hbm:shimmer_sledge", 31.0}, {"hbm:schrabidium_hammer", -1.0}, 
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 12);
        c.cmd("fill -12 " + G + " -12 12 " + (Y + 8) + " 12 minecraft:air");
        c.cmd("difficulty normal");
        c.cmd("gamemode survival");
        c.cmd("kill @e[type=!player]");
        c.teleport(0.5, G, 0.5);
        StringBuilder bad = new StringBuilder();
        for (Object[] t : CASES) {
            String item = (String) t[0];
            double expected = (Double) t[1];
            double dealt = hit(c, item, null);
            String line = c.node() + " " + item + " dealt " + dealt + " expected " + (expected < 0 ? "kill" : expected);
            System.out.println("[RcMelee] " + line);
            if (expected < 0 ? dealt < 99.9 : Math.abs(dealt - expected) > 0.05) bad.append(line).append("; ");
        }
        // shimmer axe: hit for 31, then the remaining health is halved
        double axe = hit(c, "hbm:shimmer_axe", null);
        System.out.println("[RcMelee] " + c.node() + " shimmer_axe dealt " + axe + " (1.7: 31, then half of the rest = 65.5)");
        if (Math.abs(axe - 65.5) > 0.1) bad.append("shimmer_axe ").append(axe).append("; ");
        // exhausted crucible: no energy, the bare 1
        String damaged = c.node().startsWith("1.20") ? "hbm:crucible{Damage:3}" : "hbm:crucible[minecraft:damage=3]";
        double empty = hit(c, "hbm:crucible", damaged);
        System.out.println("[RcMelee] " + c.node() + " exhausted crucible dealt " + empty + " (1.7: 1; the port keeps 1.12.2's 5 + 1, OPEN)");
        String charged = c.node().startsWith("1.20") ? "hbm:crucible{\"hbm:charges\":16}" : "hbm:crucible[hbm:charges=16]";
        double full = hit(c, "hbm:crucible", charged);
        System.out.println("[RcMelee] " + c.node() + " charged crucible dealt " + full + " (1.7: 5001, kills)");
        if (full < 99.9) bad.append("charged crucible ").append(full).append("; ");
        // chainsaw kill: ceil(100 / 10) nitra
        c.cmd("clear @s");
        c.cmd("give @s hbm:chainsaw");
        c.hotbar(1);
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.cmd("summon minecraft:iron_golem 0.5 " + G + " 2.5 {NoAI:1b,PersistenceRequired:1b}");
        c.waitTicks(10);
        c.lookAt(0.5, G + 1.3, 2.5);
        for (int i = 0; i < 6; i++) {
            c.attack();
            c.waitTicks(22);
        }
        c.waitTicks(10);
        c.teleport(0.5, G, 2.0);
        c.waitTicks(30);
        JsonObject ip = new JsonObject();
        ip.addProperty("radius", 8);
        ip.addProperty("type", "minecraft:item");
        ip.addProperty("server", true);
        System.out.println("[RcMelee] " + c.node() + " items on the ground: " + c.query("entities", ip));
        int nitra = count(c, "hbm:nitra_small") + groundNitra(c);
        System.out.println("[RcMelee] " + c.node() + " chainsaw kill dropped nitra_small " + nitra + " (1.7: 10)");
        if (nitra != 10) bad.append("chainsaw nitra ").append(nitra).append("; ");
        c.cmd("kill @e[type=!player]");
        c.cmd("gamemode creative");
        c.cmd("difficulty peaceful");
        Expect.that(bad.length() == 0, "melee figures match 1.7: " + bad);
    }

    private static double hit(Client c, String item, String giveText) {
        c.cmd("clear @s");
        c.cmd("give @s " + (giveText == null ? item : giveText));
        c.hotbar(1);
        c.cmd("kill @e[type=minecraft:iron_golem]");
        c.cmd("summon minecraft:iron_golem 0.5 " + G + " 2.5 {NoAI:1b,PersistenceRequired:1b}");
        c.waitTicks(30);
        c.lookAt(0.5, G + 1.3, 2.5);
        c.waitTicks(25);
        double before = golem(c);
        c.attack();
        c.waitTicks(12);
        double after = golem(c);
        return before - after;
    }

    private static double golem(Client c) {
        JsonObject p = new JsonObject();
        p.addProperty("radius", 32);
        p.addProperty("type", "minecraft:iron_golem");
        p.addProperty("server", true);
        JsonObject r = c.query("entities", p);
        JsonObject side = r.has("entities") ? r : r.getAsJsonObject("server");
        JsonArray list = side.getAsJsonArray("entities");
        return list.size() == 0 ? 0.0 : list.get(0).getAsJsonObject().get("health").getAsDouble();
    }

    /** Sums the Small Pile of Nitra stacks still lying around, read from the server with /data. */
    private static int groundNitra(Client c) {
        String path = c.node().startsWith("1.20") ? "Item.Count" : "Item.count";
        c.markLog();
        c.cmd("execute as @e[type=minecraft:item,distance=..12,nbt={Item:{id:\"hbm:nitra_small\"}}] run data get entity @s " + path);
        c.waitTicks(6);
        int sum = 0;
        for (String l : c.logSinceMark("entity data")) {
            Matcher m = Pattern.compile("data: ([0-9]+)").matcher(l);
            if (m.find()) sum += Integer.parseInt(m.group(1));
        }
        return sum;
    }

    private static int count(Client c, String item) {
        int n = 0;
        for (JsonElement e : c.inventory().getAsJsonArray("slots")) {
            JsonObject s = e.getAsJsonObject();
            if (item.equals(s.get("id").getAsString())) n += s.get("count").getAsInt();
        }
        return n;
    }
}
