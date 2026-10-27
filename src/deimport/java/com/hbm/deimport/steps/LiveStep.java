package com.hbm.deimport.steps;

import com.hbm.blocks.multiblock.ImportedCells;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.target.ContinuumTarget;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.IOUtilities;
import org.jetbrains.annotations.Nullable;

/**
 * Work that needs loaded levels: multiblock cells from Continuum's live footprints (so footprint fixes carry over) with
 * the core index filled, DE's radiation set pocket by pocket, and DE's lazy loot rolled into Continuum containers that
 * keep none. Done in slices against a deadline. Every record is safe to do again, so a crash redoes them all: a rolled
 * container carries the record's id inside its chunk, and what a record parks replaces what it parked before. The
 * levels are saved before the records are dropped.
 */
public final class LiveStep implements Step {
    private static final List<String> ORDER = List.of("cores", "pads", "plotcells", "conductors", "radiation", "piles", "loot", "entities");
    /** On a rolled container, in NeoForge's block-entity data: the record that filled it. */
    static final String MARK = "hbm_de_live";
    private record Work(String kind, CompoundTag record, String id) {}

    private @Nullable Ctx ctx;
    private boolean done;
    private final Deque<Work> queue = new ArrayDeque<>();
    private final Map<String, List<CompoundTag>> waiting = new LinkedHashMap<>();
    private final TreeSet<String> missing = new TreeSet<>();
    private final Set<ServerLevel> touched = new HashSet<>();

    @Override
    public String id() {
        return "live@1";
    }

    @Override
    public Phase phase() {
        return Phase.LIVE;
    }

    @Override
    public boolean applies(Ctx ctx) {
        for (String kind : List.of("cores", "plotcells", "radiation", "conductors", "piles", "entities", "loot"))
            if (!ctx.list(kind).isEmpty()) return true;
        return false;
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        slice(ctx, server, Long.MAX_VALUE);
        return true;
    }

    @Override
    public @Nullable String waitsFor(Ctx ctx) {
        return missing.isEmpty() ? null : "dimension " + String.join(", ", missing);
    }

    /**
     * Works until {@code deadline} ({@link System#nanoTime}), one record at least; true once everything this start can
     * do is done.
     */
    public boolean slice(Ctx ctx, MinecraftServer server, long deadline) throws IOException {
        if (this.ctx != ctx) start(ctx);
        if (done) return true;
        boolean first = true;
        while (!queue.isEmpty()) {
            // at least one record per slice, so a slow tick still makes progress
            if (!first && System.nanoTime() > deadline) {
                ctx.flushGlobal();
                return false;
            }
            first = false;
            Work work = queue.poll();
            String dim = work.record.getString("dim");
            ServerLevel level = dim.isEmpty() ? server.overworld() : level(server, dim);
            if (level == null) {
                if (!work.kind.equals("pads")) waiting.computeIfAbsent(work.kind, k -> new ArrayList<>()).add(work.record);
                missing.add(dim);
                continue;
            }
            touched.add(level);
            ctx.parking.during(work.id);
            try {
                apply(ctx, level, work);
            } catch (RuntimeException e) {
                // a record Continuum's own code throws on is kept and passed over, so the phase still finishes
                String where = work.kind + " " + dim + " " + BlockPos.of(work.record.getLong("pos")).toShortString();
                ctx.parking.park("live_record", where, e.toString(), work.record);
                ctx.report.line(where + " failed (" + e + "); kept in the leftovers");
                ctx.report.count("live records failed");
            } finally {
                ctx.parking.during(null);
            }
        }
        finish(ctx);
        return true;
    }

    /** Lets go of the run, its records and the levels it touched. */
    public void reset() {
        ctx = null;
        done = false;
        queue.clear();
        waiting.clear();
        missing.clear();
        touched.clear();
    }

    private void start(Ctx ctx) {
        this.ctx = ctx;
        done = false;
        queue.clear();
        waiting.clear();
        missing.clear();
        touched.clear();
        Set<String> seen = new HashSet<>();
        for (String kind : ORDER) {
            for (CompoundTag record : ctx.list(kind.equals("pads") ? "cores" : kind)) {
                String id = id(kind, record);
                if (seen.add(id)) queue.add(new Work(kind, record, id));
            }
        }
    }

    /** The same id in every run: the record's kind and content, without the shard bookkeeping. */
    static String id(String kind, CompoundTag record) {
        CompoundTag content = record.copy();
        content.remove("unit");
        content.remove(Unit.PART);
        return kind + "|" + content.getLong("pos") + "|" + Integer.toHexString(content.toString().hashCode());
    }

    private static void apply(Ctx ctx, ServerLevel level, Work work) {
        CompoundTag record = work.record;
        BlockPos pos = BlockPos.of(record.getLong("pos"));
        switch (work.kind) {
            case "cores" -> {
                ImportedCells.Result result = ImportedCells.rebuild(level, pos);
                ctx.report.count("multiblocks rebuilt");
                ctx.report.count("multiblock cells placed", result.placed());
                ctx.report.count("multiblock cells blocked", result.blocked());
                if (result.blocked() > 0)
                    ctx.report.line("multiblock " + record.getString("block") + " at " + pos.toShortString() + ": "
                            + result.blocked() + " cell(s) blocked by other blocks, left out");
            }
            case "plotcells" -> ctx.report.count("airship cells indexed", ContinuumTarget.indexCells(level, pos, record.getLongArray("cells")));
            case "pads" -> {
                if (ContinuumTarget.settleRedstone(level, pos)) ctx.report.count("powered launch pads settled");
            }
            case "conductors" -> {
                if (record.getBoolean("plot")) ContinuumTarget.joinPlotConductor(level, pos);
                else ContinuumTarget.announceConductor(level, pos);
                ctx.report.count("conductors joined");
            }
            case "radiation" -> ContinuumTarget.setRadiation(level, pos, record.getDouble("density"));
            case "piles" -> {
                int[] box = record.getIntArray("box");
                if (box.length == 6) ctx.report.count("pile blocks linked", ContinuumTarget.linkPile(level, pos, box));
            }
            case "loot" -> loot(ctx, level, record, pos, work.id);
            case "entities" -> entity(ctx, level, record);
            default -> throw new IllegalStateException(work.kind);
        }
    }

    private static void loot(Ctx ctx, ServerLevel level, CompoundTag record, BlockPos pos, String id) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(record.getString("table"))));
        BlockEntity be = level.getBlockEntity(pos);
        if (table == LootTable.EMPTY || !(be instanceof Container container)) {
            ctx.parking.park("loot_table", record.getString("dim") + " " + pos.toShortString(), record.getString("table"), record);
            return;
        }
        if (id.equals(be.getPersistentData().getString(MARK))) return;
        table.fill(container, new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST), record.getLong("seed"));
        be.getPersistentData().putString(MARK, id);
        container.setChanged();
        ctx.report.count("loot tables rolled");
    }

    private static void entity(Ctx ctx, ServerLevel overworld, CompoundTag record) {
        CompoundTag nbt = record.getCompound("nbt");
        Map<String, Tag> lost = ContinuumTarget.entityLost(overworld, nbt, record.getBoolean("player"));
        if (lost == null) {
            ctx.report.count("entities unchecked");
            return;
        }
        for (Map.Entry<String, Tag> field : lost.entrySet())
            ctx.parking.park("entity_field", record.getString("where") + " " + field.getKey(), nbt.getString("id"), field.getValue());
        ctx.report.count("entity fields parked", lost.size());
        ctx.report.count("entities checked");
    }

    /**
     * Everything this start could reach is done. Its levels are saved first: until then what the records placed exists
     * only in memory, and the records are about to be dropped. Waiting records are kept for a later start.
     */
    private void finish(Ctx ctx) throws IOException {
        for (ServerLevel level : touched) level.save(null, true, false);
        // NeoForge writes saved data (the core index, the power graph) on its own IO thread
        IOUtilities.waitUntilIOWorkerComplete();
        touched.clear();
        ctx.finishLive(waiting);
        if (!waiting.isEmpty()) ctx.report.line("live work for dimension(s) " + missing + " waits until they exist");
        done = true;
    }

    private static @Nullable ServerLevel level(MinecraftServer server, String dim) {
        ResourceLocation id = ResourceLocation.tryParse(dim);
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }
}
