import dev.golem.harness.api.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A world DE's own server wrote with what lives outside a chunk palette: lazy chest, minecart and safe loot on DE's own
 * tables, a spawner's mob, a falling block and an enderman holding DE blocks, armour modules saved as DE's count-less
 * templates, and a stat objective. Copied in as {@code de_loot} from {@code DE_WORLD_LOOT}. 1.21.1 NeoForge only.
 */
public class DeLootWorld implements Scenario {
    public void run(Client c) throws Exception {
        if (!c.node().startsWith("1.21.1-neoforge")) return;
        c.leaveWorld(60000);
        Path saves = Path.of("versions/1.21.1-neoforge/runs/harness/saves");
        Path world = saves.resolve("de_loot");
        if (Files.exists(world)) delete(world);
        copy(Path.of(System.getenv("DE_WORLD_LOOT")), world);
        int before = c.log(20000, "DE import done").size();
        c.joinWorld("de_loot", 240000);
        c.waitInWorld(240000);
        c.waitTicks(40);
        // the live phase finishes over a few server ticks after the join
        for (int i = 0; i < 120 && c.log(20000, "DE import done").size() == before; i++) c.waitTicks(10);
        List<String> doneLines = c.log(20000, "DE import done");
        Expect.that(doneLines.size() == before + 1, "this join imported the world once");
        String done = doneLines.get(doneLines.size() - 1);
        System.out.println(done);
        Expect.that(done.contains("loot tables rolled 1"), "the safe's loot was rolled: " + done);
        Expect.that(done.contains("scoreboard objectives renamed 1"), "the stat objective followed its block: " + done);

        String chest = c.blockEntity(2, -60, 2).toString();
        Expect.that(chest.contains("hbm_de:chests/ammo_10_15"), "the chest waits on the served table: " + chest);
        c.teleport(2.5, -60, 4.5);
        c.waitTicks(10);
        Fixtures.interact(c, 2, -60, 2);
        c.waitTicks(10);
        c.key("escape");
        c.waitTicks(5);
        chest = c.blockEntity(2, -60, 2).toString();
        Expect.that(chest.contains("hbm:ammo") && !chest.contains("LootTable"), "opening it rolls DE's ammo loot: " + chest);

        String safe = c.blockEntity(4, -60, 2).toString();
        Expect.that(!safe.contains("LootTable") && safe.contains("Slot"),
                "Continuum's safe holds the rolled vault loot: " + safe);
        String spawner = c.blockEntity(6, -60, 2).toString();
        Expect.that(spawner.contains("hbm:entity_missile_ab"), "the spawner's mob follows the rename: " + spawner);
        String armour = c.blockEntity(14, -60, 2).toString();
        Expect.that(armour.contains("hbm:steel_plate") && armour.contains("hbm:servo_set"), "the armour keeps its module: " + armour);

        c.cmd("execute if entity @e[type=falling_block,nbt={BlockState:{Name:\"hbm:concrete_colored\",Properties:{color:\"black\"}}}] run setblock 2 -60 6 minecraft:gold_block");
        c.cmd("execute if entity @e[type=enderman,nbt={carriedBlockState:{Name:\"hbm:concrete_colored\"}}] run setblock 4 -60 6 minecraft:gold_block");
        c.cmd("execute if entity @e[type=chest_minecart,nbt={LootTable:\"hbm_de:chests/ammo_10_15\"}] run setblock 6 -60 6 minecraft:gold_block");
        c.cmd("execute if score fixture conc matches 5 run setblock 8 -60 6 minecraft:gold_block");
        c.waitTicks(10);
        Expect.that(c.block(2, -60, 6).toString().contains("gold_block"), "the falling block holds Continuum's concrete");
        Expect.that(c.block(4, -60, 6).toString().contains("gold_block"), "the enderman carries Continuum's concrete");
        Expect.that(c.block(6, -60, 6).toString().contains("gold_block"), "the chest minecart waits on the served table");
        Expect.that(c.block(8, -60, 6).toString().contains("gold_block"), "the objective and its score survived");

        c.teleport(8.5, -57, -3.5);
        c.lookAt(8.5, -60, 2.5);
        c.screenshot("de-loot-world");
        c.markLog();
        c.leaveWorld(60000);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]].*(hbm|deimport|Skipping|invalid item|loot)");
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
