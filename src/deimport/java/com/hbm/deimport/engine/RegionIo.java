package com.hbm.deimport.engine;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.jetbrains.annotations.Nullable;

/** Reads and rewrites region files through vanilla {@link RegionFile}, so compression and oversized chunks stay vanilla's. */
public final class RegionIo {
    private static final RegionStorageInfo INFO = new RegionStorageInfo("deimport", Level.OVERWORLD, "chunk");

    private RegionIo() {}

    public static List<Path> regionFiles(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) return List.of();
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(p -> p.getFileName().toString().matches("r\\.-?\\d+\\.-?\\d+\\.mca")).sorted().toList();
        }
    }

    /** Every chunk position stored in one region file, from its name and header. */
    public static List<ChunkPos> chunks(RegionFile region, Path file) {
        String[] parts = file.getFileName().toString().split("\\.");
        int rx = Integer.parseInt(parts[1]), rz = Integer.parseInt(parts[2]);
        List<ChunkPos> out = new ArrayList<>();
        for (int i = 0; i < 1024; i++) {
            ChunkPos pos = new ChunkPos(rx * 32 + (i & 31), rz * 32 + (i >> 5));
            if (region.hasChunk(pos)) out.add(pos);
        }
        return out;
    }

    public static RegionFile open(Path file) throws IOException {
        return open(file, RegionFileVersion.VERSION_DEFLATE);
    }

    private static RegionFile open(Path file, RegionFileVersion version) throws IOException {
        return new RegionFile(INFO, file, file.getParent(), version, false);
    }

    /** False for a region file whose header vanilla cannot read. */
    public static boolean opens(Path file) {
        try (RegionFile region = open(file)) {
            return region != null;
        } catch (IOException | RuntimeException e) {
            // the answer the caller asked for: this file is damaged
            return false;
        }
    }

    public static @Nullable CompoundTag read(RegionFile region, ChunkPos pos) throws IOException {
        try (DataInputStream in = region.getChunkDataInputStream(pos)) {
            return in == null ? null : NbtIo.read(in);
        }
    }

    /** Each readable chunk until {@code each} returns false, the file only read: vanilla's {@link RegionFile} opens for writing. */
    public static void readOnly(Path file, BiPredicate<ChunkPos, CompoundTag> each) throws IOException {
        String[] parts = file.getFileName().toString().split("\\.");
        int rx = Integer.parseInt(parts[1]), rz = Integer.parseInt(parts[2]);
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            if (raf.length() < 4096) return;
            byte[] header = new byte[4096];
            raf.readFully(header);
            for (int i = 0; i < 1024; i++) {
                int entry = ((header[i * 4] & 0xFF) << 24) | ((header[i * 4 + 1] & 0xFF) << 16) | ((header[i * 4 + 2] & 0xFF) << 8) | (header[i * 4 + 3] & 0xFF);
                long start = (long) (entry >>> 8) * 4096;
                if (entry == 0 || start + 5 > raf.length()) continue;
                ChunkPos pos = new ChunkPos(rx * 32 + (i & 31), rz * 32 + (i >> 5));
                raf.seek(start);
                int length = raf.readInt(), type = raf.readUnsignedByte();
                RegionFileVersion version = RegionFileVersion.fromId(type & 0x7F);
                boolean external = (type & 0x80) != 0;
                if (version == null || !external && (length < 1 || start + 4 + length > raf.length())) continue;
                byte[] bytes = new byte[external ? 0 : length - 1];
                raf.readFully(bytes);
                CompoundTag tag;
                try (InputStream data = external ? Files.newInputStream(file.resolveSibling("c." + pos.x + "." + pos.z + ".mcc"))
                        : new ByteArrayInputStream(bytes); DataInputStream in = new DataInputStream(version.wrap(data))) {
                    tag = NbtIo.read(in);
                } catch (IOException | RuntimeException e) {
                    // a chunk vanilla could not read either says nothing; the next one may
                    continue;
                }
                if (!each.test(pos, tag)) return;
            }
        }
    }

    /** What one rewrite does with each chunk. */
    public interface Hooks {
        /** The converted chunk, or null to keep it as it is. */
        @Nullable CompoundTag convert(ChunkPos pos, CompoundTag chunk);

        /** A chunk that could not be read (its raw sector bytes) or converted (its NBT, kept unchanged in the file). */
        void damaged(ChunkPos pos, Tag data, String why);

        /** After the new file is written and before it replaces the old one. */
        default void beforeSwap() throws IOException {}
    }

    /**
     * Rewrites one region file: each chunk goes through {@code hooks} into a fresh file in a scratch folder, which then
     * replaces the original. A chunk that cannot be read, or that vanilla's header check refuses, is left out, as vanilla
     * would fail to load it too, and handed to {@link Hooks#damaged} with its raw bytes. Returns how many chunks changed;
     * nothing is replaced when none did. Each file has its own scratch folder, since files convert in parallel and an
     * oversized chunk's external file must move in only with its own region file, in that file's compression.
     */
    public static int rewrite(Path file, Hooks hooks) throws IOException {
        Path scratch = file.resolveSibling(".deimport").resolve(file.getFileName().toString());
        clear(scratch);
        Files.createDirectories(scratch);
        Path out = scratch.resolve(file.getFileName());
        int changed = 0;
        byte[] raw = null;
        try (RegionFile in = open(file); RegionFile target = open(out, version(file))) {
            List<ChunkPos> listed = chunks(in, file);
            Set<ChunkPos> readable = new HashSet<>(listed);
            for (ChunkPos pos : allocated(file)) {
                if (readable.contains(pos)) continue;
                if (raw == null) raw = Files.readAllBytes(file);
                hooks.damaged(pos, new ByteArrayTag(sector(raw, pos)), "unreadable: vanilla's header check refuses it");
                changed++;
            }
            for (ChunkPos pos : listed) {
                CompoundTag tag;
                try {
                    tag = read(in, pos);
                } catch (IOException | RuntimeException e) {
                    if (raw == null) raw = Files.readAllBytes(file);
                    hooks.damaged(pos, new ByteArrayTag(sector(raw, pos)), "unreadable: " + e);
                    changed++;
                    continue;
                }
                if (tag == null) {
                    // vanilla reads no data here either: an unknown compression or an external file that is gone
                    if (raw == null) raw = Files.readAllBytes(file);
                    hooks.damaged(pos, new ByteArrayTag(sector(raw, pos)), "unreadable: no data");
                    changed++;
                    continue;
                }
                CompoundTag next;
                try {
                    next = hooks.convert(pos, tag.copy());
                } catch (RuntimeException e) {
                    hooks.damaged(pos, tag, "not converted: " + e);
                    next = null;
                }
                if (next != null) changed++;
                try (DataOutputStream stream = target.getChunkDataOutputStream(pos)) {
                    NbtIo.write(next != null ? next : tag, stream);
                }
            }
        }
        if (changed > 0) {
            hooks.beforeSwap();
            try (Stream<Path> extra = Files.list(scratch)) {
                for (Path p : extra.filter(p -> p.getFileName().toString().endsWith(".mcc")).toList())
                    Files.move(p, file.resolveSibling(p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(out, file, StandardCopyOption.REPLACE_EXISTING);
        }
        clear(scratch);
        return changed;
    }

    /** Deletes a scratch folder and what an earlier, interrupted rewrite left in it. */
    private static void clear(Path scratch) throws IOException {
        if (!Files.isDirectory(scratch)) return;
        try (Stream<Path> files = Files.list(scratch)) {
            for (Path p : files.toList()) Files.delete(p);
        }
        Files.delete(scratch);
    }

    /** The compression of the file's first stored chunk, or deflate, vanilla's default. */
    private static RegionFileVersion version(Path file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            for (int i = 0; i < 1024 && i * 4L + 4 <= raf.length(); i++) {
                raf.seek(i * 4L);
                int entry = raf.readInt();
                long start = (long) (entry >>> 8) * 4096;
                if (entry == 0 || start + 5 > raf.length()) continue;
                raf.seek(start + 4);
                RegionFileVersion version = RegionFileVersion.fromId(raf.readUnsignedByte() & 0x7F);
                if (version != null) return version;
            }
        }
        return RegionFileVersion.VERSION_DEFLATE;
    }

    /** Every chunk the region file's header allocates, readable or not. */
    private static List<ChunkPos> allocated(Path file) throws IOException {
        String[] parts = file.getFileName().toString().split("\\.");
        int rx = Integer.parseInt(parts[1]), rz = Integer.parseInt(parts[2]);
        byte[] header;
        try (InputStream in = Files.newInputStream(file)) {
            header = in.readNBytes(4096);
        }
        List<ChunkPos> out = new ArrayList<>();
        for (int i = 0; i < 1024 && i * 4 + 3 < header.length; i++) {
            int entry = ((header[i * 4] & 0xFF) << 24) | ((header[i * 4 + 1] & 0xFF) << 16) | ((header[i * 4 + 2] & 0xFF) << 8) | (header[i * 4 + 3] & 0xFF);
            if (entry != 0) out.add(new ChunkPos(rx * 32 + (i & 31), rz * 32 + (i >> 5)));
        }
        return out;
    }

    /** A chunk's stored bytes (length, compression type, data) straight from the region file. */
    private static byte[] sector(byte[] raw, ChunkPos pos) {
        int index = (pos.x & 31) + (pos.z & 31) * 32;
        if (raw.length < 4096) return new byte[0];
        int entry = ((raw[index * 4] & 0xFF) << 24) | ((raw[index * 4 + 1] & 0xFF) << 16) | ((raw[index * 4 + 2] & 0xFF) << 8) | (raw[index * 4 + 3] & 0xFF);
        long start = (long) (entry >>> 8) * 4096;
        if (start + 5 > raw.length) return new byte[0];
        int length = ((raw[(int) start] & 0xFF) << 24) | ((raw[(int) start + 1] & 0xFF) << 16) | ((raw[(int) start + 2] & 0xFF) << 8) | (raw[(int) start + 3] & 0xFF);
        long end = Math.min(raw.length, start + 4 + Math.max(0, length));
        return Arrays.copyOfRange(raw, (int) start, (int) end);
    }
}
