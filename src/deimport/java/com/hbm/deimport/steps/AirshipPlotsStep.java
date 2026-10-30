package com.hbm.deimport.steps;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Dimensions;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.source.de7.Airships;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * Airships an importer before this one converted at their place in their plot instead of on Sable's grid: once per
 * folder it converted, in worlds imported before, their block entities, pile links and live records are set right
 * ({@link SableAirships#repair}). Folders this importer converts are marked done as it converts them.
 */
public final class AirshipPlotsStep implements Step {
    public static final String ID = "airshipplots@1";
    private final Set<String> unfinished = new LinkedHashSet<>();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<String> requiresMods() {
        return Set.of("sable");
    }

    @Override
    public boolean applies(Ctx ctx) throws IOException {
        return !folders(ctx).isEmpty();
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        unfinished.clear();
        // an older run cut short before its live phase left its records at the plot place too
        int dropped = ctx.dropEarlier((kind, t) -> (kind.equals("plotcells") || kind.equals("conductors") && t.getBoolean("plot"))
                && !Airships.inPlotYard(new ChunkPos(BlockPos.of(t.getLong("pos")))));
        ctx.report.count("misplaced airship records of an earlier run moved to the leftovers", dropped);
        for (Map.Entry<String, Path> dim : folders(ctx).entrySet()) {
            Path folder = dim.getValue();
            int ships = SableAirships.repair(ctx, dim.getKey(), folder);
            if (ships < 0 && SubLevelsStep.retry(ctx, folder.resolveSibling("sublevels.plots"), "setting right the airships in " + ctx.world.relativize(folder))) {
                unfinished.add(ctx.world.relativize(folder).toString());
                continue;
            }
            ctx.report.count("airships checked for an older import's plot positions", Math.max(ships, 0));
            ctx.journal.mark(ID, ctx.world, folder);
        }
        return true;
    }

    @Override
    public @Nullable String waitsFor(Ctx ctx) {
        return unfinished.isEmpty() ? null : "airships that could not be read or written now: " + String.join(", ", unfinished);
    }

    private static Map<String, Path> folders(Ctx ctx) throws IOException {
        Map<String, Path> out = new LinkedHashMap<>();
        for (Map.Entry<String, Path> dim : Dimensions.all(ctx.world).entrySet()) {
            Path folder = dim.getValue().resolve("sublevels");
            if (Files.isDirectory(folder) && !ctx.journal.done(ID, ctx.world, folder) && converted(ctx, folder)) out.put(dim.getKey(), folder);
        }
        return out;
    }

    /** Importers before this one marked each storage file they converted, not the folder. */
    static boolean converted(Ctx ctx, Path folder) throws IOException {
        if (ctx.journal.done("sublevels@1", ctx.world, folder)) return true;
        try (Stream<Path> files = Files.list(folder)) {
            return files.anyMatch(f -> ctx.journal.done("sublevels@1", ctx.world, f));
        }
    }
}
