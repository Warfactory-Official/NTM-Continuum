package com.hbm.deimport.steps;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.source.de7.Airships;
import com.hbm.deimport.source.de7.BackupMerge;
import dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelRegionFile;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelStorageFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * Airships in Sable's own storage ({@code r.X.Z.N.slvls}, oversized entries in {@code r.X.Z.r}), read and written
 * through Sable's {@link SubLevelStorageFile}. Only loaded while Sable is installed.
 */
final class SableAirships {
    /** Sable keeps only the keys it knows when it saves a ship, so the mark lasts until the ship is next saved. */
    static final String CONVERTED = "hbm_de_converted";
    /** The journal's mark of a ship that got the backup's blocks, under its storage folder by id. */
    static final String MERGED = "airshipmerge@1";
    private SableAirships() {}

    /** Whether a ship in {@code folder} carries the mark of a conversion Sable has not saved over yet. */
    static boolean anyConverted(Path folder) throws IOException {
        for (Path file : storage(folder)) {
            try (SubLevelStorageFile storage = open(file)) {
                for (int i = 0; i < storage.getTotalIndexCapacity(); i++) {
                    try {
                        CompoundTag ship = storage.read(i);
                        if (ship != null && ship.getBoolean(CONVERTED)) return true;
                    } catch (IOException | RuntimeException e) {
                        // a ship Sable cannot read shows no mark
                    }
                }
            } catch (IOException | RuntimeException e) {
                // the same for a file it cannot open
            }
        }
        return false;
    }

    /** Converts every airship in {@code folder}; returns how many. */
    static int convert(Ctx ctx, String dim, Path folder) throws IOException {
        List<Path> files;
        try (Stream<Path> list = Files.list(folder)) {
            files = list.filter(p -> p.getFileName().toString().endsWith(".slvls")).sorted().toList();
        }
        int ships = 0;
        for (Path file : files) {
            if (ctx.journal.done("sublevels@1", ctx.world, file)) continue;
            String name = file.getFileName().toString();
            String region = name.substring(0, name.lastIndexOf('.', name.length() - ".slvls".length() - 1));
            // converted in memory, committed, then written: Sable writes in place, so a crash mid-file must find each
            // written ship marked and the rest still DE's. Each ship is a part, so the records of the ships already
            // written stay when the rest are converted again.
            Unit unit = ctx.begin("airships " + dim + " " + name);
            try (SubLevelStorageFile storage = new SubLevelStorageFile(file, folder.resolve(region + ".r"))) {
                Map<Integer, CompoundTag> converted = new TreeMap<>();
                for (int i = 0; i < storage.getTotalIndexCapacity(); i++) {
                    String where = dim + " airship " + name + "#" + i;
                    unit.part(Integer.toString(i));
                    CompoundTag ship = null;
                    try {
                        ship = storage.read(i);
                        if (ship == null || ship.getBoolean(CONVERTED)) {
                            unit.endPart(false);
                            continue;
                        }
                        CompoundTag copy = ship.copy();
                        Airships.convert(ctx, dim, copy, where);
                        copy.putBoolean(CONVERTED, true);
                        converted.put(i, copy);
                    } catch (IOException | RuntimeException e) {
                        // one ship Sable cannot read or the converter cannot take stays as it is, with a copy kept
                        unit.part(Integer.toString(i));
                        ctx.parking.park("airship", where, "not converted: " + e, ship);
                        ctx.report.line(where + " was not converted (" + e + "); kept in the leftovers");
                    }
                    unit.endPart(true);
                }
                if (converted.isEmpty() && unit.touchedAny()) {
                    ctx.commit(unit);
                } else if (converted.isEmpty()) {
                    ctx.abandon(unit);
                } else {
                    ctx.commit(unit);
                    for (Map.Entry<Integer, CompoundTag> e : converted.entrySet()) storage.write(e.getKey(), e.getValue());
                    storage.flush();
                    ships += converted.size();
                }
            } catch (IOException | RuntimeException e) {
                // a storage file Sable cannot open or write stays as it is; Sable will not load it either
                ctx.abandon(unit);
                ctx.report.line(dim + " airships in " + name + " could not be converted (" + e + ") and were left as they are");
                continue;
            }
            ctx.journal.mark("sublevels@1", ctx.world, file);
        }
        return ships;
    }

    /**
     * Gives each airship of {@code live} back the blocks its copy in {@code backup} had of {@code only}'s ids; returns
     * how many, or -1 when a ship could not be read or written now and the next start merges again.
     */
    static int merge(Ctx ctx, String dim, Path live, Path backup, Predicate<String> only) throws IOException {
        // Sable opens its files for writing, so the backup's are read from a copy
        Path copy = live.resolveSibling(live.getFileName() + ".de-backup");
        try {
            Map<UUID, CompoundTag> before = new HashMap<>();
            // a crash in Sable's move of a ship can leave two copies of it: the one its holding chunk names is the one
            // that loads, and when those cannot be read this start merges every copy it meets
            Set<UUID> merging = new HashSet<>();
            Set<String> then = referenced(copyOf(backup, copy)), now = referenced(live);
            for (Path file : storage(copy)) {
                try (SubLevelStorageFile storage = open(file)) {
                    for (int i = 0; i < storage.getTotalIndexCapacity(); i++) {
                        if (then != null && !then.contains(file.getFileName() + "#" + i)) continue;
                        try {
                            CompoundTag ship = storage.read(i);
                            if (ship != null && BackupMerge.holds(ship.getCompound("plot"), only)) before.put(ship.getUUID("uuid"), ship);
                        } catch (IOException | RuntimeException e) {
                            // the first import found the same ship and reported it
                        }
                    }
                } catch (IOException | RuntimeException e) {
                    ctx.report.line(dim + " backup airships in " + file.getFileName() + " cannot be read (" + e + ")");
                }
            }
            int merged = 0;
            boolean again = false;
            for (Path file : before.isEmpty() ? List.<Path>of() : storage(live)) {
                SubLevelStorageFile storage;
                try {
                    storage = open(file);
                } catch (IOException | RuntimeException e) {
                    // only an I/O fault, such as a lock held by another program: Sable reads bad content and logs it
                    again = true;
                    ctx.report.line(dim + " airships in " + file.getFileName() + " cannot be opened (" + e + "); the next start tries again");
                    continue;
                }
                try (storage) {
                    for (int i = 0; i < storage.getTotalIndexCapacity(); i++) {
                        CompoundTag ship;
                        try {
                            ship = storage.read(i);
                        } catch (FileSystemException e) {
                            // an oversized ship's own file, held by another program
                            again = true;
                            ctx.report.line(dim + " airship " + i + " in " + file.getFileName() + " cannot be read now (" + e + "); the next start tries again");
                            continue;
                        } catch (IOException | RuntimeException e) {
                            // the first import reported a ship Sable cannot read
                            continue;
                        }
                        CompoundTag de = ship == null || !ship.hasUUID("uuid") ? null : before.get(ship.getUUID("uuid"));
                        if (de == null || now != null && !now.contains(file.getFileName() + "#" + i)) continue;
                        // a start that could not finish merges again only the ships it did not finish, as the others were played since
                        UUID id = ship.getUUID("uuid");
                        Path done = live.resolve(id.toString());
                        if (ctx.journal.done(MERGED, ctx.world, done) && !merging.contains(id)) continue;
                        merging.add(id);
                        String where = dim + " airship " + id;
                        CompoundTag original = ship.copy();
                        Unit unit = ctx.begin("airship from the backup " + where + " #" + file.getFileName() + "." + i);
                        int back;
                        try {
                            back = BackupMerge.ship(ctx, dim, ship, de, only, where);
                        } catch (RuntimeException e) {
                            back = 0;
                            ctx.report.line(where + " could not get its blocks back (" + e + ")");
                        }
                        if (back == 0) {
                            ctx.abandon(unit);
                            mark(ctx, done);
                            continue;
                        }
                        // recorded before the ship changes, so a crash merges it again into the same records
                        record(() -> ctx.commit(unit));
                        try {
                            storage.write(i, ship);
                        } catch (IOException | RuntimeException e) {
                            // Sable puts the ship in its header before it removes an oversized ship's old file
                            if (!written(storage, i, ship)) {
                                putBack(ctx, storage, i, original, where);
                                record(() -> ctx.retract(unit));
                                throw e;
                            }
                        }
                        mark(ctx, done);
                        merged++;
                    }
                    storage.flush();
                } catch (UncheckedIOException e) {
                    throw e;
                } catch (IOException | RuntimeException e) {
                    again = true;
                    ctx.report.line(dim + " airships in " + file.getFileName() + " could not be written (" + e + "); the next start tries again");
                }
            }
            return again ? -1 : merged;
        } catch (UncheckedIOException e) {
            // the importer's own records failed: the start stops before anything is played, and the next merges again
            throw e.getCause();
        } finally {
            delete(copy);
        }
    }

    private interface Records {
        void run() throws IOException;
    }

    private static void record(Records records) {
        try {
            records.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void mark(Ctx ctx, Path done) {
        record(() -> ctx.journal.mark(MERGED, ctx.world, done));
    }

    /** A ship Sable could not write may point at a file it never made: written back as it was, it loads again. */
    private static void putBack(Ctx ctx, SubLevelStorageFile storage, int i, CompoundTag original, String where) {
        try {
            storage.write(i, original);
        } catch (IOException | RuntimeException e) {
            ctx.report.line(where + " could not be written back as it was (" + e + "); its copy in DE's backup is the one left");
        }
    }

    /** The ship in each plot of {@code folder}, keyed {@code ChunkPos.asLong(plot_x, plot_z)}, read from a copy in {@code scratch}. */
    static Map<Long, UUID> plots(Path folder, Path scratch) throws IOException {
        Map<Long, UUID> out = new HashMap<>();
        Set<Long> unclear = new HashSet<>();
        if (!Files.isDirectory(folder)) return out;
        try {
            Set<String> loads = referenced(copyOf(folder, scratch));
            for (Path file : storage(scratch)) {
                try (SubLevelStorageFile storage = open(file)) {
                    for (int i = 0; i < storage.getTotalIndexCapacity(); i++) {
                        if (loads != null && !loads.contains(file.getFileName() + "#" + i)) continue;
                        try {
                            CompoundTag ship = storage.read(i);
                            if (ship == null || !ship.hasUUID("uuid")) continue;
                            CompoundTag plot = ship.getCompound("plot");
                            long key = ChunkPos.asLong(plot.getInt("plot_x"), plot.getInt("plot_z"));
                            UUID other = out.put(key, ship.getUUID("uuid"));
                            // a copy Sable left behind in a plot another ship took since: which ship it is stays unknown
                            if (other != null && !other.equals(ship.getUUID("uuid"))) unclear.add(key);
                        } catch (IOException | RuntimeException e) {
                            // a ship Sable cannot read shows nothing about its plot
                        }
                    }
                } catch (IOException | RuntimeException e) {
                    // the same: its plots stay unknown
                }
            }
        } finally {
            delete(scratch);
        }
        unclear.forEach(out::remove);
        return out;
    }

    /**
     * Airships an older importer converted at their place in their plot (see {@link Airships#replot}); returns how many
     * it looked at, or -1 when a storage file could not be read or written now.
     */
    static int repair(Ctx ctx, String dim, Path folder) throws IOException {
        int ships = 0;
        boolean again = false;
        Set<String> loads = referenced(folder);
        for (Path file : storage(folder)) {
            try (SubLevelStorageFile storage = open(file)) {
                for (int i = 0; i < storage.getTotalIndexCapacity(); i++) {
                    if (loads != null && !loads.contains(file.getFileName() + "#" + i)) continue;
                    CompoundTag ship;
                    try {
                        ship = storage.read(i);
                    } catch (FileSystemException e) {
                        throw e;
                    } catch (IOException | RuntimeException e) {
                        // the first import reported a ship Sable cannot read
                        continue;
                    }
                    if (ship == null) continue;
                    String where = dim + " airship " + (ship.hasUUID("uuid") ? ship.getUUID("uuid") : file.getFileName() + "#" + i);
                    Unit unit = ctx.begin("airship plot " + where + " #" + file.getFileName() + "." + i);
                    boolean changed;
                    try {
                        changed = Airships.replot(ctx, dim, ship);
                    } catch (RuntimeException e) {
                        ctx.abandon(unit);
                        ctx.report.line(where + " could not be checked for an older import's plot positions (" + e + ")");
                        continue;
                    }
                    // the records stand whether or not the ship is written: they follow its blocks, which stay
                    record(() -> ctx.commit(unit));
                    if (changed) storage.write(i, ship);
                    ships++;
                }
                storage.flush();
            } catch (UncheckedIOException e) {
                throw e.getCause();
            } catch (IOException | RuntimeException e) {
                again = true;
                ctx.report.line(dim + " airships in " + file.getFileName() + " could not be set right (" + e + "); the next start tries again");
            }
        }
        return again ? -1 : ships;
    }

    /**
     * The storage slots Sable loads ships from, {@code r.X.Z.N.slvls#i}, as its holding chunks name them; null when they
     * cannot all be read.
     */
    private static @Nullable Set<String> referenced(Path folder) {
        List<Path> regions;
        try (Stream<Path> list = Files.list(folder)) {
            regions = list.filter(p -> p.getFileName().toString().endsWith(SubLevelRegionFile.FILE_EXTENSION)).sorted().toList();
        } catch (IOException e) {
            return null;
        }
        if (regions.isEmpty()) return null;
        Set<String> out = new HashSet<>();
        for (Path region : regions) {
            String name = region.getFileName().toString();
            String base = name.substring(0, name.length() - SubLevelRegionFile.FILE_EXTENSION.length());
            try (SubLevelRegionFile holding = new SubLevelRegionFile(region, region.resolveSibling(base + ".r"))) {
                for (int i = 0; i < holding.getTotalIndexCapacity(); i++) {
                    CompoundTag chunk = holding.read(i);
                    if (chunk == null) continue;
                    for (int packed : chunk.getIntArray("pointers")) {
                        SavedSubLevelPointer pointer = SavedSubLevelPointer.unpack(packed);
                        out.add(base + "." + pointer.storageIndex() + ".slvls#" + pointer.subLevelIndex());
                    }
                }
            } catch (IOException | RuntimeException e) {
                // without every holding chunk, which copy loads stays unknown
                return null;
            }
        }
        return out;
    }

    private static boolean written(SubLevelStorageFile storage, int i, CompoundTag ship) {
        try {
            return ship.equals(storage.read(i));
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** {@code folder} copied into {@code copy}, oversized ships' folders with it. */
    private static Path copyOf(Path folder, Path copy) throws IOException {
        delete(copy);
        Files.createDirectories(copy);
        try (Stream<Path> files = Files.walk(folder)) {
            for (Path file : files.toList()) {
                Path to = copy.resolve(folder.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(to);
                    continue;
                }
                try (InputStream in = Files.newInputStream(file)) {
                    Files.copy(in, to);
                }
            }
        }
        return copy;
    }

    private static List<Path> storage(Path folder) throws IOException {
        try (Stream<Path> list = Files.list(folder)) {
            return list.filter(p -> p.getFileName().toString().endsWith(".slvls")).sorted().toList();
        }
    }

    private static SubLevelStorageFile open(Path file) throws IOException {
        String name = file.getFileName().toString();
        // Sable writes an oversized ship into this folder, which it makes whenever it opens the file
        Path external = file.resolveSibling(name.substring(0, name.lastIndexOf('.', name.length() - ".slvls".length() - 1)) + ".r");
        Files.createDirectories(external);
        return new SubLevelStorageFile(file, external);
    }

    private static void delete(Path folder) throws IOException {
        if (!Files.exists(folder)) return;
        try (Stream<Path> files = Files.walk(folder)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
