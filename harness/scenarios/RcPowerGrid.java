import com.google.gson.JsonObject;
import dev.golem.harness.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Release audit, power transmission: creative generator, cable run, switch, detector cable, pylon pair with a wire and
 * a consumer, each step read from the furnace's saved power. Soft checks, so one failure does not hide the rest.
 */
public final class RcPowerGrid implements Scenario {

    private static final int Y = 100;
    private static final int B = Y + 1;
    private final List<String> failures = new ArrayList<>();

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, 0, Y, 0, 20);
        c.waitTicks(5);

        straightRun(c);
        switchStep(c);
        pylonStep(c);

        c.screenshot("99-final");
        Expect.logCleanSinceMark(c, "(?i)(exception|error).*(hbm|power|energy|cable|pylon)");
        Expect.that(failures.isEmpty(), "soft checks failed: " + failures);
    }

    private void straightRun(Client c) {
        c.cmd("fill -10 " + B + " -2 10 " + (B + 4) + " 2 minecraft:air");
        Fixtures.setBlock(c, -9, B, 0, "hbm:creative_generator");
        c.cmd("fill -8 " + B + " 0 -1 " + B + " 0 hbm:red_cable");
        Fixtures.setBlock(c, 0, B, 0, "hbm:machine_electric_furnace_off");
        c.waitTicks(40);
        c.screenshot("01-run");
        check(furnacePower(c, 0) > 0, "furnace gets power over 8 cables: " + furnacePower(c, 0));

        // break the run, empty the furnace, it must stay empty
        Fixtures.setBlock(c, -4, B, 0, "minecraft:air");
        c.cmd("data merge block 0 " + B + " 0 {power:0L}");
        c.waitTicks(40);
        check(furnacePower(c, 0) == 0, "no power across a broken run: " + furnacePower(c, 0));

        // rejoin: power flows again
        Fixtures.setBlock(c, -4, B, 0, "hbm:red_cable");
        c.waitTicks(40);
        check(furnacePower(c, 0) > 0, "power flows again after the run is repaired: " + furnacePower(c, 0));
        c.screenshot("02-rejoined");
    }

    private void switchStep(Client c) {
        Fixtures.setBlock(c, -4, B, 0, "hbm:cable_switch");
        c.cmd("data merge block 0 " + B + " 0 {power:0L}");
        c.waitTicks(40);
        check(furnacePower(c, 0) == 0, "an open switch blocks power: " + furnacePower(c, 0));
        c.screenshot("03-switch-off");

        c.teleport(-4.5, B, 2.5);
        c.waitTicks(2);
        Fixtures.interact(c, -4, B, 0);
        c.waitTicks(40);
        check(blockOn(c, -4), "right-click closes the switch: " + c.block(-4, B, 0));
        check(furnacePower(c, 0) > 0, "a closed switch conducts: " + furnacePower(c, 0));
        c.screenshot("04-switch-on");

        Fixtures.interact(c, -4, B, 0);
        c.cmd("data merge block 0 " + B + " 0 {power:0L}");
        c.waitTicks(40);
        check(!blockOn(c, -4), "right-click opens the switch again");
        check(furnacePower(c, 0) == 0, "an opened switch blocks power again: " + furnacePower(c, 0));

        // detector cable: closed by redstone power
        Fixtures.setBlock(c, -4, B, 0, "hbm:cable_detector");
        c.cmd("data merge block 0 " + B + " 0 {power:0L}");
        c.waitTicks(30);
        check(furnacePower(c, 0) == 0, "an unpowered detector cable blocks: " + furnacePower(c, 0));
        Fixtures.setBlock(c, -4, B, 1, "minecraft:redstone_block");
        c.waitTicks(40);
        check(blockOn(c, -4), "a powered detector cable closes: " + c.block(-4, B, 0));
        check(furnacePower(c, 0) > 0, "a powered detector cable conducts: " + furnacePower(c, 0));
        c.screenshot("05-detector-on");
        Fixtures.setBlock(c, -4, B, 1, "minecraft:air");
        Fixtures.setBlock(c, -4, B, 0, "hbm:red_cable");
    }

    private void pylonStep(Client c) {
        c.cmd("fill -20 " + B + " -3 20 " + (B + 6) + " 3 minecraft:air");
        c.waitTicks(3);
        Fixtures.setBlock(c, -17, B, 0, "hbm:creative_generator");
        c.cmd("fill -16 " + B + " 0 -14 " + B + " 0 hbm:red_cable");
        Fixtures.placeOnTop(c, "hbm:red_pylon", -13, Y, 0);
        Fixtures.placeOnTop(c, "hbm:red_pylon", 9, Y, 0);
        c.cmd("fill 10 " + B + " 0 13 " + B + " 0 hbm:red_cable");
        Fixtures.setBlock(c, 14, B, 0, "hbm:machine_electric_furnace_off");
        c.waitTicks(20);
        c.screenshot("06-pylons-unwired");
        check(furnacePower(c, 14) == 0, "unwired pylons carry nothing: " + furnacePower(c, 14));

        Fixtures.giveOnly(c, "hbm:wiring_red_copper", 1);
        c.hotbar(1);
        c.waitTicks(1);
        c.teleport(-11.5, B, 2.5);
        c.waitTicks(3);
        c.lookAt(-12.5, B + 1, 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(5);
        c.teleport(7.5, B, 2.5);
        c.waitTicks(3);
        c.lookAt(9.5, B + 1, 0.5);
        c.waitTicks(1);
        c.use(2);
        c.waitTicks(40);
        c.screenshot("07-pylons-wired");
        check(furnacePower(c, 14) > 0, "power crosses a pylon wire: " + furnacePower(c, 14));

        // save and rejoin: the wired line must rebuild itself
        c.cmd("save-all flush");
        c.waitTicks(40);
        String world = c.currentWorld();
        c.leaveWorld(60_000L);
        c.joinWorld(world, 120_000L);
        c.waitInWorld(120_000L);
        c.waitTicks(60);
        c.cmd("data merge block 14 " + B + " 0 {power:0L}");
        c.waitTicks(60);
        check(furnacePower(c, 14) > 0, "the wired line works again after leaving and rejoining: " + furnacePower(c, 14));
        c.screenshot("07b-after-rejoin");

        // cut one pylon: the far side goes dark
        c.cmd("setblock 9 " + B + " 0 minecraft:air");
        c.waitTicks(10);
        c.cmd("data merge block 14 " + B + " 0 {power:0L}");
        c.waitTicks(40);
        check(furnacePower(c, 14) == 0, "a removed pylon cuts the line: " + furnacePower(c, 14));
        c.screenshot("08-pylon-removed");
    }

    private void check(boolean ok, String message) {
        System.out.println((ok ? "[ok] " : "[FAIL] ") + message);
        if (!ok) failures.add(message);
    }

    private static boolean blockOn(Client c, int x) {
        JsonObject b = c.block(x, B, 0);
        return b != null && b.toString().replace(" ", "").contains("\"on\":\"true\"");
    }

    private static long furnacePower(Client c, int x) {
        JsonObject be = c.blockEntity(x, B, 0);
        if (be == null || !be.has("nbt")) return -1;
        Matcher m = Pattern.compile("[{,]power:(-?\\d+)").matcher(be.get("nbt").getAsString().replaceAll("\\s+", ""));
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }
}
