package com.hbm.deimport.steps;

import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Dimensions;
import com.hbm.deimport.engine.Step;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Sable airships ({@code <dim>/sublevels}): their blocks live in Sable's own files, which only Sable can rewrite. While
 * Sable is missing they are moved to {@code sublevels.de-pending}, since loading them unconverted would turn their DE
 * blocks into air; once Sable is installed they are moved back and converted.
 */
public final class SubLevelsStep implements Step {
    static final String PENDING = "sublevels.de-pending";
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
        for (Path dim : Dimensions.all(ctx.world).values())
            if (Files.isDirectory(dim.resolve("sublevels")) || Files.isDirectory(dim.resolve(PENDING))) return true;
        return false;
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        aside = null;
        for (Map.Entry<String, Path> dim : Dimensions.all(ctx.world).entrySet()) {
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
                    continue;
                }
                Files.move(pending, folder);
            }
            if (Files.isDirectory(folder)) SableAirships.convert(ctx, dim.getKey(), folder);
        }
        return true;
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
}
