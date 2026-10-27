package com.hbm.deimport.steps;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Journal;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.gate.ImportState;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Copies the whole world next to itself before anything is written, and refuses when the disk cannot hold the copy.
 * {@code -Dhbm.deimport.backup=none} skips it for operators who made their own.
 */
public final class BackupStep implements Step {
    private static final Set<String> SKIPPED = Set.of("session.lock", ImportState.FILE, ImportState.PREV, Journal.FILE);
    @Override
    public String id() {
        return "backup@1";
    }

    @Override
    public boolean applies(Ctx ctx) {
        return !"none".equals(System.getProperty("hbm.deimport.backup"));
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        Path world = ctx.world;
        long size = size(world);
        if (Files.getFileStore(world).getUsableSpace() < size + (64L << 20))
            throw new IOException("not enough disk space for a backup of " + (size >> 20) + " MB; free some or start with -Dhbm.deimport.backup=none");
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path target = world.resolveSibling(world.getFileName() + "_DE_backup_" + stamp);
        Files.walkFileTree(world, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(world.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // the importer's own files stay behind, or opening the backup would carry on converting it
                if (!SKIPPED.contains(file.getFileName().toString()))
                    Files.copy(file, target.resolve(world.relativize(file).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
        ctx.report.line("backup: " + target);
        ctx.report.count("backup MB", size >> 20);
        return true;
    }

    private static long size(Path world) throws IOException {
        long[] total = new long[1];
        Files.walkFileTree(world, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                total[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }
        });
        return total[0];
    }
}
