import dev.golem.harness.api.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A world saved by NTM: Decentralized Edition 1.0.0-beta.12 itself (its dev server, with Create, Sable and Aeronautics),
 * copied into the harness saves as {@code de_real}. Checks what came across, that a powered launch pad holds its missile
 * through a neighbour update, the airship, and that a second start does no import work. 1.21.1 NeoForge only.
 */
public class DeRealWorld implements Scenario {
    public void run(Client c) throws Exception {
        if (!c.node().startsWith("1.21.1-neoforge")) return;
        c.leaveWorld(60000);
        Path saves = Path.of("versions/1.21.1-neoforge/runs/harness/saves");
        try (Stream<Path> old = Files.list(saves)) {
            for (Path p : old.filter(p -> p.getFileName().toString().startsWith("de_real")).toList()) delete(p);
        }
        copy(Path.of(System.getenv("DE_WORLD")), saves.resolve("de_real"));
        int before = c.log(20000, "DE import done").size();
        int numericPollution = c.log(20000, "ordinal-indexed pollution").size();
        c.joinWorld("de_real", 240000);
        c.waitInWorld(240000);
        c.waitTicks(60);
        // the live phase finishes over a few server ticks after the join
        for (int i = 0; i < 120 && c.log(20000, "DE import done").size() == before; i++) c.waitTicks(10);
        List<String> doneLines = c.log(20000, "DE import done");
        Expect.that(doneLines.size() == before + 1, "this join imported the world once");
        String done = doneLines.get(doneLines.size() - 1);
        System.out.println(done);
        Expect.that(done.contains("multiblock cells blocked 0"), "every multiblock rebuilt whole: " + done);
        Expect.that(done.contains("airships converted 1"), "the airship converted: " + done);
        Expect.that(c.log(20000, "ordinal-indexed pollution").size() == numericPollution, "DE's pollution reads as Continuum's");

        Expect.that(c.block(2, -60, 2).toString().contains("hbm:machine_centrifuge"), "centrifuge");
        Expect.that(!c.block(2, -59, 2).toString().contains("minecraft:air"), "centrifuge cell");
        String centrifuge = c.blockEntity(2, -60, 2).toString();
        // The centrifuge drains its battery slot: DE's 5000 HE plus the battery's 1234.
        Expect.that(centrifuge.contains("canned_conserve_ass") && centrifuge.contains("battery_lead") && centrifuge.contains("6234"),
                "centrifuge stacks and the battery's charge: " + centrifuge);
        String tank = c.blockEntity(8, -60, 4).toString();
        Expect.that(tank.contains("16000") && tank.contains("hbm:oil"), "fluid tank oil: " + tank);
        Expect.that(c.block(18, -60, 6).toString().contains("hbm:machine_zirnox"), "ZIRNOX");
        Expect.that(c.blockEntity(18, -60, 6).toString().contains("8000"), "ZIRNOX carbon dioxide");
        Expect.that(c.block(52, -60, 12).toString().contains("hbm:machine_catalytic_cracker"), "catalytic cracker");
        Expect.that(c.blockEntity(4, -60, 14).toString().contains("steel"), "crucible molten steel");
        Expect.that(c.blockEntity(14, -60, 16).toString().contains("steel"), "arc furnace liquid steel");
        String duct = c.blockEntity(12, -60, 4).toString();
        Expect.that(duct.contains("hbm:oil"), "duct fluid: " + duct);
        Expect.that(c.blockEntity(2, -60, 30).toString().contains("777777"), "battery power");
        Expect.that(c.blockEntity(33, -60, 30).toString().contains("500"), "mass storage stockpile");
        Expect.that(c.block(18, -60, 30).toString().contains("hbm:machine_capacitor_copper"), "capacitor block");
        Expect.that(c.blockEntity(14, -60, 30).toString().contains("minecraft:iron_ingot"), "steam turbine refunded");
        Expect.that(c.blockEntity(8, -60, 30).toString().contains("machine_battery_redd"), "FEnSU refunded as a REDD");
        Expect.that(c.block(30, -60, 30).toString().contains("concrete_colored"), "concrete");
        // The fixture's lamp is lit with no signal; Continuum's lamp switches off on its first update, as in 1.7.
        Expect.that(c.block(31, -60, 30).toString().contains("lamp_tritium_blue_"), "tritium lamp");
        Expect.that(c.block(32, -60, 30).toString().contains("sellafield_3"), "sellafield");
        String chest = c.blockEntity(24, -60, 30).toString();
        Expect.that(chest.contains("canister_full") && chest.contains("hbm:diesel") && chest.contains("designator_x")
                && chest.contains("crashed_bomb_nuke") && chest.contains("hbm:machine_capacitor_copper")
                && chest.contains("hbm:ingot_metal_key") && chest.contains("hbm:gas_radon") && chest.contains("hbm:glitch")
                && !chest.contains("test_item"), "chest of DE stacks: " + chest);

        // The pad was powered before DE saved it: a neighbour update must not fire it.
        String pad = c.blockEntity(32, -60, 18).toString();
        Expect.that(pad.contains("missile_generic") && pad.contains("prevRedstonePower: 1") && pad.contains("activatedBlocks"),"pad settled: " + pad);
        c.cmd("setblock 34 -60 17 minecraft:stone");
        c.waitTicks(40);
        Expect.that(c.blockEntity(32, -60, 18).toString().contains("missile_generic"), "the pad kept its missile");

        c.teleport(20.5, -55, -6.5);
        c.lookAt(20.5, -60, 12.5);
        c.screenshot("de-world-machines");
        c.teleport(42.5, -55, 24.5);
        c.lookAt(42.5, -59, 32.5);
        c.screenshot("de-world-airship");
        Fixtures.interact(c, 8, -60, 4);
        c.waitTicks(10);
        c.screenshot("de-world-tank-gui");
        c.key("escape");

        c.leaveWorld(60000);
        c.markLog();
        c.joinWorld("de_real", 240000);
        c.waitInWorld(240000);
        c.waitTicks(40);
        Expect.that(c.log(20000, "DE import done").size() == before + 1, "a second start does no import work");
        Expect.logCleanSinceMark(c, "DE import");
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]].*(hbm|deimport|Skipping|invalid item)");
        // Later scenarios build on the harness world.
        c.leaveWorld(60000);
        c.joinWorld("harness", 180000);
        c.waitInWorld(180000);
    }

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path p : files.toList()) {
                if (p.getFileName().toString().equals("session.lock")) continue;
                Path target = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p)) Files.createDirectories(target);
                else Files.copy(p, target);
            }
        }
    }

    private static void delete(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
