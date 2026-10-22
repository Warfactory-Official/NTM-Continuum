import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 29. 1.7's barrels and tanks take a fluid identifier on a sneak-click ({@code onBlockActivated}, sneak
 * branch); a plain click opens the menu. Drives the single-fluid identifier as a player would on a technetium steel
 * barrel, a fluid tank and a BAT-9000, and reads the saved tank type.
 */
public final class Gh29IdentifierContainers implements Scenario {

    private static final int Y = 100;
    private static final int G = Y + 1;

    @Override
    public void run(Client c) throws Exception {
        c.useFlatWorld();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, 0, Y, 0, 14);
        c.cmd("fill -13 " + G + " -13 13 " + (Y + 10) + " 13 minecraft:air");
        c.cmd("kill @e[type=item]");
        c.markLog();

        // Technetium steel barrel at 0 G 0, approached from the south
        Fixtures.giveOnly(c, "hbm:barrel_tcalloy", 1);
        aim(c, 0.5, 2.5, 0.5, Y + 0.98, 0.5);
        c.use(1);
        c.waitTicks(4);
        Expect.that(c.block(0, G, 0).toString().contains("barrel_tcalloy"), "barrel placed: " + c.block(0, G, 0));

        Fixtures.giveOnly(c, identifier(c, "hbm:kerosene"), 1);
        c.hotbar(1);
        aim(c, 0.5, 2.5, 0.5, G + 0.5, 0.5);
        c.use(1);
        c.waitScreen("", 5000);
        c.waitTicks(5);
        Expect.that(c.screen().has("slots"), "a plain click with the identifier opens the barrel's menu");
        c.key("escape");
        c.waitTicks(5);
        Expect.that(!nbt(c, 0, G, 0).contains("\"hbm:kerosene\""), "a plain click does not retype: " + nbt(c, 0, G, 0));

        sneakUse(c, 0.5, G, 2.5, 0.5, G + 0.5, 0.5);
        String barrel = nbt(c, 0, G, 0);
        System.out.println("[Gh29] barrel after sneak-click: " + barrel);
        Expect.that(barrel.contains("\"hbm:kerosene\""), "a sneak-click with the identifier retyped the barrel: " + barrel);
        Expect.noScreen(c);
        c.screenshot("01-barrel");

        // Fluid tank south of the barrel, typed from on top
        Fixtures.giveOnly(c, "hbm:machine_fluidtank", 1);
        aim(c, 0.5, 9.5, 0.5, Y + 0.98, 5.5);
        c.use(1);
        c.waitTicks(10);
        int[] tank = findBe(c, "TileEntityMachineFluidTank", 0, 6, 3);
        Expect.that(tank != null, "the fluid tank was placed near 0 " + G + " 6");
        c.cmd("setblock " + tank[0] + " " + (G + 3) + " " + (tank[2] + 4) + " minecraft:stone");
        Fixtures.giveOnly(c, identifier(c, "hbm:diesel"), 1);
        sneakUse(c, tank[0] + 0.5, G + 4, tank[2] + 4.5, tank[0] + 0.5, G + 2.99, tank[2] + 0.5);
        String tankNbt = nbt(c, tank[0], tank[1], tank[2]);
        System.out.println("[Gh29] fluid tank after sneak-click: " + tankNbt);
        Expect.that(tankNbt.contains("\"hbm:diesel\""), "a sneak-click with the identifier retyped the fluid tank: " + tankNbt);
        Expect.noScreen(c);

        // BAT-9000 north of the barrel, typed on its north face
        Fixtures.giveOnly(c, "hbm:machine_bat9000", 1);
        aim(c, 0.5, -12.5, 0.5, Y + 0.98, -8.5);
        c.use(1);
        c.waitTicks(10);
        int[] bat = findBe(c, "TileEntityMachineBAT9000", 0, -7, 4);
        Expect.that(bat != null, "the BAT-9000 was placed near 0 " + G + " -7");
        Fixtures.giveOnly(c, identifier(c, "hbm:oil"), 1);
        sneakUse(c, bat[0] + 0.5, G, bat[2] - 4.5, bat[0] + 0.5, G + 2.5, bat[2] + 0.5);
        String batNbt = nbt(c, bat[0], bat[1], bat[2]);
        System.out.println("[Gh29] BAT-9000 after sneak-click: " + batNbt);
        Expect.that(batNbt.contains("\"hbm:oil\""), "a sneak-click with the identifier retyped the BAT-9000: " + batNbt);
        Expect.noScreen(c);
        c.screenshot("02-tanks");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]");
    }

    /** Crouches before aiming: sneaking lowers the eye, and the ray would pass under the target. */
    private static void sneakUse(Client c, double px, double py, double pz, double lx, double ly, double lz) {
        c.hotbar(1);
        c.teleport(px, py, pz);
        c.waitTicks(3);
        c.keyDown("key.sneak");
        c.waitTicks(3);
        c.lookAt(lx, ly, lz);
        c.waitTicks(1);
        c.use(1);
        c.waitTicks(5);
        c.keyUp("key.sneak");
        c.waitTicks(3);
    }

    private static void aim(Client c, double px, double pz, double lx, double ly, double lz) {
        c.teleport(px, G, pz);
        c.waitTicks(3);
        c.lookAt(lx, ly, lz);
        c.waitTicks(1);
    }

    private static String identifier(Client c, String fluid) {
        return c.node().startsWith("1.20.") ? "hbm:fluid_identifier{\"hbm:fluid\":\"" + fluid + "\"}"
                : "hbm:fluid_identifier[hbm:fluid=\"" + fluid + "\"]";
    }

    /** The core: the only cell whose block entity is of class {@code type}. */
    private static int[] findBe(Client c, String type, int x, int z, int r) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            JsonObject be = c.blockEntity(x + dx, G, z + dz);
            if (be.has("block_entity") && !be.get("block_entity").isJsonNull()
                    && be.get("block_entity").getAsString().endsWith("." + type)) {
                return new int[]{x + dx, G, z + dz};
            }
        }
        return null;
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return (be.has("nbt") ? be.get("nbt").getAsString() : be.toString()).replaceAll("\\s+", "").replace("\\\"", "\"");
    }
}
