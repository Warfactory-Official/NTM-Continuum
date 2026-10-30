package com.hbm.deimport.steps;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Dimensions;
import com.hbm.deimport.engine.Step;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Sable airships ({@code <dim>/sublevels}): their blocks live in Sable's own files, which only Sable can rewrite. While
 * Sable is missing they are moved to {@code sublevels.de-pending}, since loading them unconverted would turn their DE
 * blocks into air; once Sable is installed they are moved back and converted. Each folder is converted once: Sable
 * adds its own files there afterwards. The airships of a DE addon left for later are its converter's ({@link #convert}).
 */
public final class SubLevelsStep implements Step {
    public static final String PENDING = "sublevels.de-pending";
    private static final String RETRY = "airshipretry@1";
    private @Nullable String aside;

    @Override
    public String id() {
        return "sublevels@1";
    }

    @Override
    public Set<String> requiresMods() {
        return Set.of("sable");
    }

    @Override
    public boolean applies(Ctx ctx) throws IOException {
        for (Path dim : Dimensions.all(ctx.world).values()) if (hasAirships(dim)) return true;
        return false;
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        aside = null;
        for (Map.Entry<String, Path> dim : Dimensions.without(Dimensions.all(ctx.world), ctx.deferredAddons).entrySet()) {
            Path folder = dim.getValue().resolve("sublevels");
            Path pending = dim.getValue().resolve(PENDING);
            if (Files.isDirectory(pending)) {
                if (Files.exists(folder)) {
                    // two airship stores cannot be merged safely; DE's are converted where they wait, and every start
                    // tries again to move them back
                    SableAirships.convert(ctx, dim.getKey(), pending);
                    ctx.report.line("airships in " + ctx.world.relativize(pending) + " are converted but stay aside: "
                            + ctx.world.relativize(folder) + " was created since; move one of them away to bring them back");
                    aside = ctx.world.relativize(pending).toString();
                    ctx.journal.mark(id(), ctx.world, folder);
                    ctx.journal.mark(AirshipPlotsStep.ID, ctx.world, folder);
                    continue;
                }
                Files.move(pending, folder);
            } else if (ctx.journal.done(id(), ctx.world, folder)) {
                continue;
            }
            // a folder converted, or one Sable creates later, holds Continuum's own airships from then on
            if (Files.isDirectory(folder)) {
                once(ctx, dim.getKey(), folder);
            } else {
                ctx.journal.mark(id(), ctx.world, folder);
                ctx.journal.mark(AirshipPlotsStep.ID, ctx.world, folder);
            }
        }
        return true;
    }

    /** One dimension's airships moved in after this step ran; false while Sable, which alone can rewrite them, is missing. */
    public static boolean convert(Ctx ctx, String dim, Path folder) throws IOException {
        if (!ModList.get().isLoaded("sable")) return false;
        once(ctx, dim, folder);
        return true;
    }

    /**
     * {@code folder}'s airships get back the blocks of {@code only}'s ids from DE's copy; how many, or -1 without Sable
     * or when a ship could not be written, so that the folder is merged again on the next start.
     */
    public static int merge(Ctx ctx, String dim, Path folder, Path backup, Predicate<String> only) throws IOException {
        if (!ModList.get().isLoaded("sable")) return -1;
        return SableAirships.merge(ctx, dim, folder, backup, only);
    }

    /** The ship in each plot of {@code folder} (keyed {@code ChunkPos.asLong(plot_x, plot_z)}), read through {@code scratch}; null without Sable. */
    public static @Nullable Map<Long, UUID> plots(Path folder, Path scratch) throws IOException {
        if (!ModList.get().isLoaded("sable")) return null;
        return SableAirships.plots(folder, scratch);
    }

    /** Whether work on {@code key} that could not be done now gets another start: three, then it is left as it is. */
    public static boolean retry(Ctx ctx, Path key, String what) throws IOException {
        for (int k = 1; k <= 3; k++) {
            Path attempt = key.resolveSibling(key.getFileName() + ".try" + k);
            if (ctx.journal.done(RETRY, ctx.world, attempt)) continue;
            ctx.journal.mark(RETRY, ctx.world, attempt);
            return true;
        }
        ctx.report.line(what + " could not be done in three starts and is left as it is");
        return false;
    }

    private static void once(Ctx ctx, String dim, Path folder) throws IOException {
        // ships an earlier run converted may be an older importer's, which the plot repair looks at
        boolean earlier = AirshipPlotsStep.converted(ctx, folder) || SableAirships.anyConverted(folder);
        SableAirships.convert(ctx, dim, folder);
        ctx.journal.mark("sublevels@1", ctx.world, folder);
        if (!earlier) ctx.journal.mark(AirshipPlotsStep.ID, ctx.world, folder);
    }

    @Override
    public @Nullable String waitsFor(Ctx ctx) {
        return aside == null ? null : "airships in " + aside;
    }

    @Override
    public void defer(Ctx ctx) throws IOException {
        for (Path dim : Dimensions.all(ctx.world).values()) {
            Path from = dim.resolve("sublevels");
            Path to = dim.resolve(PENDING);
            if (!Files.isDirectory(from) || Files.exists(to)) continue;
            Files.move(from, to);
            ctx.report.line("airships moved aside until Sable is installed: " + ctx.world.relativize(to));
        }
    }

    private static boolean hasAirships(Path dim) {
        return Files.isDirectory(dim.resolve("sublevels")) || Files.isDirectory(dim.resolve(PENDING));
    }
}
