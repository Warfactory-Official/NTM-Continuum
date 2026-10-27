package com.hbm.deimport.steps;

import com.hbm.Constants;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Dimensions;
import com.hbm.deimport.engine.RegionIo;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.source.de7.ChunkConverter;
import com.hbm.delegacy.de7.Walker;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * Terrain ({@code region/}) and entity ({@code entities/}) files of every dimension, one file per worker. Only chunks
 * with DE's stamp are rewritten; entity files are converted whole, once, through the journal.
 */
public final class RegionsStep implements Step {
    static final String ENTITIES_DONE = "hbm_de_converted";

    @Override
    public String id() {
        return "regions@1";
    }

    @Override
    public boolean applies(Ctx ctx) {
        return true;
    }

    @Override
    public boolean run(Ctx ctx, MinecraftServer server) throws IOException {
        List<Runnable> jobs = new ArrayList<>();
        for (Map.Entry<String, Path> dim : Dimensions.all(ctx.world).entrySet()) {
            for (Path file : RegionIo.regionFiles(dim.getValue().resolve("region")))
                jobs.add(() -> terrain(ctx, dim.getKey(), file));
            for (Path file : RegionIo.regionFiles(dim.getValue().resolve("entities")))
                jobs.add(() -> entities(ctx, dim.getKey(), file));
        }
        AtomicInteger finished = new AtomicInteger();
        int total = jobs.size();
        try {
            jobs.parallelStream().forEach(job -> {
                job.run();
                int n = finished.incrementAndGet();
                if (n % 16 == 0 || n == total) Constants.LOG.info("DE import: {} of {} region files", n, total);
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return true;
    }

    private void terrain(Ctx ctx, String dim, Path file) {
        ChunkConverter converter = new ChunkConverter(ctx, dim);
        rewrite(ctx, "terrain " + dim + " " + file.getFileName(), dim + " " + file.getFileName(), file, converter::convert);
    }

    /**
     * Entity chunks have no DE stamp, so each converted one is marked: a crash between the file swap and the journal
     * line converts the file again, and the marked chunks are passed over instead of converted twice.
     */
    private void entities(Ctx ctx, String dim, Path file) {
        if (ctx.journal.done(id(), ctx.world, file)) return;
        Walker walker = new Walker(ctx);
        String where = dim + " entities " + file.getFileName();
        rewrite(ctx, "entities " + dim + " " + file.getFileName(), where, file, chunk -> {
            if (chunk.getBoolean(ENTITIES_DONE)) return null;
            walker.walkList(chunk.getList("Entities", Tag.TAG_COMPOUND), where);
            chunk.putBoolean(ENTITIES_DONE, true);
            return chunk;
        });
        try {
            ctx.journal.mark(id(), ctx.world, file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Each chunk is a part of the file's unit: a run after a crash converts only the chunks still in DE's shape, and
     * the records of those it passes over stay. A chunk whose conversion fails keeps none of what it recorded.
     */
    public static void rewrite(Ctx ctx, String unitName, String where, Path file, UnaryOperator<CompoundTag> convert) {
        Unit unit = ctx.begin(unitName);
        try {
            int changed = RegionIo.rewrite(file, new RegionIo.Hooks() {
                @Override
                public @Nullable CompoundTag convert(ChunkPos pos, CompoundTag chunk) {
                    unit.part(pos.x + "," + pos.z);
                    CompoundTag out = convert.apply(chunk);
                    unit.endPart(out != null);
                    return out;
                }

                @Override
                public void damaged(ChunkPos pos, Tag data, String why) {
                    unit.part(pos.x + "," + pos.z);
                    ctx.parking.park("chunk", where + " chunk " + pos.x + "," + pos.z, why, data);
                    unit.endPart(true);
                    ctx.report.line("chunk " + pos.x + "," + pos.z + " in " + where + " " + why + "; kept in the leftovers");
                    ctx.report.count("chunks damaged");
                }

                @Override
                public void beforeSwap() throws IOException {
                    ctx.commit(unit);
                }
            });
            // a chunk kept as it was can still have parked something, with nothing in the file to swap
            if (changed == 0 && unit.touchedAny()) ctx.commit(unit);
            else if (changed == 0) ctx.abandon(unit);
        } catch (IOException e) {
            ctx.abandon(unit);
            if (Files.isRegularFile(file) && !RegionIo.opens(file)) {
                ctx.report.line(where + " cannot be opened and was left as it is: " + e);
                ctx.report.count("region files damaged");
                return;
            }
            throw new UncheckedIOException(e);
        }
    }
}
