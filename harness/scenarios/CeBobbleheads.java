import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * old-hbm's Community Edition bobbleheads (2026-10-06): movblock, Leafia, Hacker6329 and EOS, types 25..28. Three wear
 * their person's Mojang skin, fetched at client start by Core {@code ProfileSkins} and voxelized (old-hbm
 * {@code SkinBoxVoxelizer}); Leafia is her own model. Checks: each places and keeps its type, and the three skins
 * load ({@code hbm_special.bobble_skins}, a texture id each). Shots: the row from the front and close-ups.
 */
public final class CeBobbleheads implements Scenario {

    private static final int X = 900;
    private static final int Y = 100;
    private static final int Z = 900;
    private static final String[] NAMES = {"MOVBLOCK", "LEAFIA", "HACKER6329", "EOS", "BOB"};
    /** The four CE figures, then 1.7's BOB as the orientation reference. */
    private static final int[] TYPES = {25, 26, 27, 28, 8};
    /** BlockBobble.BobbleType names by ordinal (the item ids are bobblehead_<name>). */
    private static final String[] BOBBLE_NAMES = {"none", "strength", "perception", "endurance", "charisma", "intelligence", "agility", "luck", "bob", "frizzle", "pu238", "vt", "doc", "bluehat", "pheo", "adam29", "uffr", "vaer", "nos", "drillgon", "cirno", "microwave", "peep", "mellow", "abel", "movblock", "leafia", "hacker6329", "eos"};

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        String forceload = (X - 16) + " " + (Z - 16) + " " + (X + 16) + " " + (Z + 16);
        String hideGui = null;
        try {
            c.cmd("forceload add " + forceload);
            Fixtures.platform(c, X, Y, Z, 8);
            c.cmd("time set 6000");
            c.cmd("gamerule doDaylightCycle false");
            c.cmd("gamerule advance_time false");
            c.cmd("weather clear");
            c.cmd("gamemode creative");
            for (int i = 0; i < TYPES.length; i++) Fixtures.setBlock(c, X + i * 2, Y + 1, Z, "minecraft:air");
            c.waitTicks(3);

            for (int i = 0; i < TYPES.length; i++) {
                c.cmd("clear");
                c.waitTicks(1);
                // one item per bobblehead type since History §19es-itemvariant
                String item = "hbm:bobblehead_" + BOBBLE_NAMES[TYPES[i]];
                c.cmd("give @s " + item + " 1");
                c.hotbar(1);
                c.waitTicks(2);
                int x = X + i * 2;
                c.teleport(x + 0.5, Y + 1, Z + 3.5);
                c.waitTicks(3);
                c.lookAt(x + 0.5, Y + 0.98, Z + 0.9);
                c.waitTicks(1);
                c.use(2);
                c.waitTicks(5);
                Expect.block(c, x, Y + 1, Z, "hbm:bobblehead");
                // the figure is saved by name since History §19es-itemvariant
                String nbt = c.blockEntity(x, Y + 1, Z).toString().replace(" ", "").replace("\\", "");
                Expect.that(nbt.contains("type:\"" + BOBBLE_NAMES[TYPES[i]] + "\""),
                        NAMES[i] + " keeps type " + TYPES[i] + ": " + nbt);
            }
            c.cmd("clear");

            JsonObject skins = new JsonObject();
            for (int wait = 0; wait < 30; wait++) {
                skins = c.query("hbm_special", new JsonObject()).getAsJsonObject("bobble_skins");
                if (!skins.get("MOVBLOCK").getAsString().isEmpty() && !skins.get("HACKER6329").getAsString().isEmpty()
                        && !skins.get("EOS").getAsString().isEmpty()) break;
                c.waitTicks(20);
            }
            System.out.println("[CeBobbleheads] " + c.node() + " skins=" + skins);
            for (String name : new String[] {"MOVBLOCK", "HACKER6329", "EOS"}) {
                Expect.that(skins.get(name).getAsString().contains("profile_skins/"), name + "'s skin loaded: " + skins);
            }

            hideGui = c.setOption("hide_gui", "true");
            c.cmd("gamemode spectator");
            c.waitTicks(2);
            c.teleport(X + 4.5, Y + 1.8, Z + 3.5);
            c.lookAt(X + 4.5, Y + 1.25, Z + 0.5);
            c.waitTicks(15);
            c.screenshot("1-row");
            for (int i = 0; i < TYPES.length; i++) {
                double x = X + i * 2 + 0.5;
                c.teleport(x, Y - 0.35, Z + 1.3); // spectator: the eye is 1.62 above, level with the figure
                c.lookAt(x, Y + 1.3, Z + 0.5);
                c.waitTicks(12);
                c.screenshot((2 + i) + "-" + NAMES[i].toLowerCase());
            }
        } finally {
            c.cmd("gamemode creative");
            if (hideGui != null) c.setOption("hide_gui", hideGui);
            c.cmd("forceload remove " + forceload);
        }
    }
}
