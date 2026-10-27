import dev.golem.harness.api.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A world DE's own server wrote with every DE block that has a block entity placed once ({@code /defixture all} in the
 * DE checkout), copied in as {@code de_all} from {@code DE_WORLD_ALL}. Every family's real DE save goes through the
 * importer and Continuum's own load; what Continuum would not keep must be parked, never logged as a load error.
 */
public class DeAllBlocks implements Scenario {
    public void run(Client c) throws Exception {
        if (!c.node().startsWith("1.21.1-neoforge")) return;
        c.leaveWorld(60000);
        Path saves = Path.of("versions/1.21.1-neoforge/runs/harness/saves");
        try (Stream<Path> old = Files.list(saves)) {
            for (Path p : old.filter(p -> p.getFileName().toString().startsWith("de_all")).toList()) delete(p);
        }
        copy(Path.of(System.getenv("DE_WORLD_ALL")), saves.resolve("de_all"));
        int before = c.log(20000, "DE import done").size();
        c.markLog();
        c.joinWorld("de_all", 600000);
        c.waitInWorld(600000);
        c.waitTicks(100);
        // the live phase finishes over a few server ticks after the join
        for (int i = 0; i < 120 && c.log(20000, "DE import done").size() == before; i++) c.waitTicks(10);
        List<String> done = c.log(20000, "DE import done");
        Expect.that(done.size() == before + 1, "this join imported the world once");
        System.out.println(done.get(done.size() - 1));
        Expect.logCleanSinceMark(c, "/(ERROR|WARN)[]].*(Failed to load|Skipping BlockEntity|Unknown structure|invalid item|Tried to load)");
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
