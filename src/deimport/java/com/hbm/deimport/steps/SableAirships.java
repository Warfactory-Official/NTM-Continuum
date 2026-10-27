package com.hbm.deimport.steps;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.source.de7.Airships;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelStorageFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;

/**
 * Airships in Sable's own storage ({@code r.X.Z.N.slvls}, oversized entries in {@code r.X.Z.r}), read and written
 * through Sable's {@link SubLevelStorageFile}. Only loaded while Sable is installed.
 */
final class SableAirships {
    /** Sable keeps only the keys it knows when it saves a ship, so the mark lasts until the ship is next saved. */
    static final String CONVERTED = "hbm_de_converted";
    private SableAirships() {}

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
                // a storage file Sable cannot open or write stays as it is and is tried again on the next start
                ctx.abandon(unit);
                ctx.report.line(dim + " airships in " + name + " could not be converted (" + e + ") and were left as they are");
                continue;
            }
            ctx.journal.mark("sublevels@1", ctx.world, file);
        }
        return ships;
    }
}
