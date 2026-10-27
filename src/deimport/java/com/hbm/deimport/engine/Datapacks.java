package com.hbm.deimport.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** The world's own datapacks ({@code datapacks/}) as vanilla loads them, and reading files out of one. */
public final class Datapacks {
    private Datapacks() {}

    /**
     * The packs vanilla loads, lowest priority first: those {@code level.dat} enables in its order, then the ones it has
     * not seen yet, which vanilla adds after them. Disabled packs are left out.
     */
    public static List<Path> ordered(Path world) throws IOException {
        Path folder = world.resolve("datapacks");
        if (!Files.isDirectory(folder)) return List.of();
        Map<String, Path> available = new TreeMap<>();
        try (Stream<Path> list = Files.list(folder)) {
            for (Path pack : list.toList()) {
                String name = pack.getFileName().toString();
                if (Files.isDirectory(pack) || name.endsWith(".zip")) available.put("file/" + name, pack);
            }
        }
        CompoundTag packs = new CompoundTag();
        Path level = world.resolve("level.dat");
        try {
            if (Files.isRegularFile(level))
                packs = NbtIo.readCompressed(level, NbtAccounter.unlimitedHeap()).getCompound("Data").getCompound("DataPacks");
        } catch (IOException | RuntimeException e) {
            // without a readable level.dat every pack counts as new, as vanilla would treat it
        }
        List<Path> out = new ArrayList<>();
        for (Tag t : packs.getList("Enabled", Tag.TAG_STRING)) {
            Path pack = available.remove(t.getAsString());
            if (pack != null) out.add(pack);
        }
        for (Tag t : packs.getList("Disabled", Tag.TAG_STRING)) available.remove(t.getAsString());
        out.addAll(available.values());
        return out;
    }

    /** The text of {@code entry} (a path from the pack's root) in a pack folder or zip, or null. */
    public static @Nullable String read(Path pack, String entry) throws IOException {
        if (Files.isDirectory(pack)) {
            Path file = pack.resolve(entry);
            return Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8) : null;
        }
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            ZipEntry e = zip.getEntry(entry);
            if (e == null) return null;
            try (InputStream in = zip.getInputStream(e)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    /** The file names directly inside {@code folder} (a path from the pack's root, ending in a slash). */
    public static List<String> list(Path pack, String folder) throws IOException {
        List<String> out = new ArrayList<>();
        if (Files.isDirectory(pack)) {
            Path dir = pack.resolve(folder);
            if (!Files.isDirectory(dir)) return out;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(Files::isRegularFile).forEach(p -> out.add(p.getFileName().toString()));
            }
            return out;
        }
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                String name = e.nextElement().getName();
                if (name.startsWith(folder) && name.length() > folder.length() && name.indexOf('/', folder.length()) < 0)
                    out.add(name.substring(folder.length()));
            }
        }
        return out;
    }
}
