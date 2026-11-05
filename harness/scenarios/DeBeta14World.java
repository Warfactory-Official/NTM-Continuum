import com.google.gson.JsonObject;
import dev.golem.harness.api.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * A world DE beta.14's own server wrote with every DE block, item and entity plus the shapes beta.14 added, copied in as
 * {@code de_beta14} from {@code DE_WORLD_14}: throttled and classic turbofans at (1000|1030, 64, 1000), a chest of fuses
 * and Create motors at (1060, 64, 1000), placed motors at (1070|1072, 64, 1000). 1.21.1 NeoForge only.
 */
public class DeBeta14World implements Scenario {
    public void run(Client c) throws Exception {
        if (!c.node().startsWith("1.21.1-neoforge")) return;
        c.leaveWorld(60000);
        Path saves = Path.of("versions/1.21.1-neoforge/runs/harness/saves");
        Path world = saves.resolve("de_beta14");
        if (Files.exists(world)) delete(world);
        copy(Path.of(System.getenv("DE_WORLD_14")), world);
        int before = c.log(20000, "DE import done").size();
        c.markLog();
        c.joinWorld("de_beta14", 600000);
        c.waitInWorld(600000);
        c.waitTicks(100);
        // the live phase finishes over a few server ticks after the join
        for (int i = 0; i < 120 && c.log(20000, "DE import done").size() == before; i++) c.waitTicks(10);
        List<String> done = c.log(20000, "DE import done");
        Expect.that(done.size() == before + 1, "this join imported the world once");
        System.out.println(done.get(done.size() - 1));
        Expect.logCleanSinceMark(c, "/(ERROR|WARN)[]].*(Failed to load|Skipping BlockEntity|Unknown structure|invalid item|Tried to load)");

        c.cmd("gamemode creative");
        c.cmd("tp @s 1040 66 1000");
        c.waitTicks(40);
        Expect.that(c.block(1000, 64, 1000).toString().contains("hbm:machine_turbofan"), "the throttled fan is a Continuum turbofan");
        Expect.that(c.block(1030, 64, 1000).toString().contains("hbm:machine_turbofan"), "the classic fan is a Continuum turbofan");
        Expect.that(c.block(1070, 64, 1000).toString().contains("minecraft:air"), "the Create motor is gone from the world");
        Expect.that(c.block(1072, 64, 1000).toString().contains("minecraft:air"), "the desh Create motor is gone from the world");

        String chest = nbt(c, 1060, 64, 1000);
        Expect.that(chest.contains("\"hbm:unstable_timer\": 42"), "the running fuse keeps its tick count: " + chest);
        Expect.that(!chest.contains("unstable_at"), "the old fuse deadline converted: " + chest);
        Expect.that(!chest.contains("create_motor"), "motor stacks left the chest: " + chest);

        String leftovers = gunzipped(world.resolve("data/hbm_de_leftovers.dat"));
        for (String parked : List.of("hbm:create_motor", "hbm:create_motor_desh", "controlMode"))
            Expect.that(leftovers.contains(parked), parked + " is parked, not deleted");
        c.leaveWorld(60000);
        c.joinWorld("harness", 180000);
        c.waitInWorld(180000);
    }

    private static String nbt(Client c, int x, int y, int z) {
        JsonObject be = c.blockEntity(x, y, z);
        return be.has("nbt") ? be.get("nbt").getAsString() : "";
    }

    private static String gunzipped(Path file) throws IOException {
        try (InputStream in = new GZIPInputStream(Files.newInputStream(file))) {
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
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
