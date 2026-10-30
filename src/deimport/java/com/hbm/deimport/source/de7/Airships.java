package com.hbm.deimport.source.de7;

import com.hbm.deimport.engine.ChunkBlocks;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.PackedSection;
import com.hbm.delegacy.de7.Walker;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

/**
 * One Sable airship's saved form: its plot chunks keyed by {@code ChunkPos.toLong}, then everything else it carries.
 * Sable keys a plot chunk's sections by index above the dimension's lowest section instead of vanilla's list with a
 * {@code Y}; they are put in vanilla's shape for the converter and back afterwards.
 */
public final class Airships {
    /** Where Sable's plot grid starts, in plots on both axes, in every level ({@code SubLevelContainer.DEFAULT_ORIGIN}). */
    static final int PLOT_ORIGIN = 10000;
    /** Sable's plot grid is 128 plots a side, each 1 << 7 chunks. */
    private static final int PLOT_CHUNKS = 7, PLOTS = 128;

    private Airships() {}

    /** The plot's chunks go through the chunk converter; cells are rebuilt inside the plot, never through the world. */
    public static void convert(Ctx ctx, String dim, CompoundTag ship, String where) {
        plot(ctx, dim, ship, where, new ChunkConverter(ctx, dim, true), pos -> true);
        CompoundTag rest = ship.copy();
        rest.remove("plot");
        new Walker(ctx).walk(rest, where);
        for (String key : rest.getAllKeys()) ship.put(key, rest.get(key));
        ctx.report.count("airships converted");
    }

    /** The plot alone. Sable keys its chunks by their place in the plot but its block entities by their place in the level. */
    /**
     * Sets right what an importer that converted the ship at its place in its plot, instead of on Sable's grid, left:
     * block entities and pile links near the level's origin go to the ship's place, its machines and cables are recorded
     * again, and what that importer put in the level is recorded for the live phase to take out. True when the ship
     * itself changed.
     */
    public static boolean replot(Ctx ctx, String dim, CompoundTag ship) {
        CompoundTag plot = ship.getCompound("plot"), chunks = plot.getCompound("chunks");
        int log = plot.getInt("log_size");
        int baseX = (PLOT_ORIGIN + plot.getInt("plot_x")) << log, baseZ = (PLOT_ORIGIN + plot.getInt("plot_z")) << log;
        int dx = baseX << 4, dz = baseZ << 4, span = 16 << log;
        int minSection = ctx.target.minSection(dim);
        boolean changed = false;
        for (String key : chunks.getAllKeys()) {
            ChunkPos local = new ChunkPos(Long.parseLong(key));
            CompoundTag chunk = chunks.getCompound(key);
            ListTag entities = chunk.getList("block_entities", Tag.TAG_COMPOUND), kept = new ListTag();
            Set<Long> placed = new HashSet<>();
            for (Tag t : entities) placed.add(BlockPos.asLong(((CompoundTag) t).getInt("x"), ((CompoundTag) t).getInt("y"), ((CompoundTag) t).getInt("z")));
            for (Tag t : entities) {
                CompoundTag be = (CompoundTag) t;
                int x = be.getInt("x"), z = be.getInt("z");
                if (x >> 4 == local.x && z >> 4 == local.z) {
                    changed = true;
                    // Sable made one at the block's real place since: that one is the ship's now
                    if (placed.contains(BlockPos.asLong(x + dx, be.getInt("y"), z + dz))) continue;
                    be.putInt("x", x + dx);
                    be.putInt("z", z + dz);
                }
                int cx = be.getInt("cX"), cz = be.getInt("cZ");
                if (be.contains("cX", Tag.TAG_INT) && cx >= 0 && cx < span && cz >= 0 && cz < span
                        && ctx.target.linkPileMember(be, BlockPos.asLong(cx + dx, be.getInt("cY"), cz + dz))) changed = true;
                kept.add(be);
            }
            chunk.put("block_entities", kept);
            CompoundTag sections = chunk.getCompound("sections");
            for (String index : sections.getAllKeys()) {
                PackedSection packed = new PackedSection(sections.getCompound(index).getCompound("block_states"));
                int y0 = (Integer.parseInt(index) + minSection) << 4;
                for (int p = 0; p < packed.palette.size(); p++) {
                    CompoundTag entry = (CompoundTag) packed.palette.get(p);
                    String name = entry.getString("Name");
                    boolean core = ctx.target.isCoreState(name, entry.getCompound("Properties"));
                    boolean conductor = !core && ctx.target.isPlacedConductor(name);
                    if (!core && !conductor) continue;
                    for (int i = 0; i < PackedSection.SIZE; i++) {
                        if (packed.index[i] != p) continue;
                        BlockPos pos = new BlockPos(((baseX + local.x) << 4) + (i & 15), y0 + (i >> 8), ((baseZ + local.z) << 4) + ((i >> 4) & 15));
                        CompoundTag stale = new CompoundTag();
                        stale.putString("dim", dim);
                        stale.putLong("pos", pos.offset(-dx, 0, -dz).asLong());
                        if (conductor) {
                            stale.putBoolean("conductor", true);
                            ctx.conductor(dim, pos.asLong(), true);
                        } else {
                            List<Long> cells = new ArrayList<>(), before = new ArrayList<>();
                            for (Map.Entry<Long, CompoundTag> cell : ctx.target.footprint(name, entry.getCompound("Properties"), pos.asLong(), ctx.rbmkColumnHeight()).entrySet()) {
                                BlockPos at = BlockPos.of(cell.getKey());
                                before.add(at.offset(-dx, 0, -dz).asLong());
                                CompoundTag there = stateAt(chunks, minSection, baseX, baseZ, at);
                                // the machine's cell, which for many machines is its own block, but never another core
                                if (cell.getValue().getString("Name").equals(there.getString("Name"))
                                        && !ctx.target.isCoreState(there.getString("Name"), there.getCompound("Properties"))) cells.add(cell.getKey());
                            }
                            stale.putLongArray("cells", before.stream().mapToLong(Long::longValue).toArray());
                            if (!cells.isEmpty()) {
                                CompoundTag record = new CompoundTag();
                                record.putString("dim", dim);
                                record.putLong("pos", pos.asLong());
                                record.putLongArray("cells", cells.stream().mapToLong(Long::longValue).toArray());
                                ctx.add("plotcells", record);
                            }
                        }
                        ctx.add("stale", stale);
                    }
                }
            }
        }
        return changed;
    }

    private static CompoundTag stateAt(CompoundTag chunks, int minSection, int baseX, int baseZ, BlockPos pos) {
        CompoundTag section = chunks.getCompound(Long.toString(ChunkPos.asLong((pos.getX() >> 4) - baseX, (pos.getZ() >> 4) - baseZ)))
                .getCompound("sections").getCompound(Integer.toString((pos.getY() >> 4) - minSection));
        if (!section.contains("block_states")) return new CompoundTag();
        PackedSection packed = new PackedSection(section.getCompound("block_states"));
        return (CompoundTag) packed.palette.get(packed.index[PackedSection.indexOf(pos.getX(), pos.getY(), pos.getZ())]);
    }

    /** A chunk of Sable's plot grid, where its ships keep their blocks and entities. */
    public static boolean inPlotYard(ChunkPos chunk) {
        int from = PLOT_ORIGIN << PLOT_CHUNKS, to = (PLOT_ORIGIN + PLOTS) << PLOT_CHUNKS;
        return chunk.x >= from && chunk.x < to && chunk.z >= from && chunk.z < to;
    }

    /** The plot of the grid holding {@code chunk}, keyed as {@code ChunkPos.asLong(plot_x, plot_z)}. */
    public static long plotOf(ChunkPos chunk) {
        return ChunkPos.asLong((chunk.x >> PLOT_CHUNKS) - PLOT_ORIGIN, (chunk.z >> PLOT_CHUNKS) - PLOT_ORIGIN);
    }

    static void plot(Ctx ctx, String dim, CompoundTag ship, String where, ChunkConverter converter, Predicate<BlockPos> room) {
        CompoundTag plot = ship.getCompound("plot");
        int baseX = (PLOT_ORIGIN + plot.getInt("plot_x")) << plot.getInt("log_size");
        int baseZ = (PLOT_ORIGIN + plot.getInt("plot_z")) << plot.getInt("log_size");
        // Sable loads the whole plot under one biome, which it looks up strictly
        if (plot.contains("biome", Tag.TAG_STRING)) plot.putString("biome", ChunkConverter.biome(plot.getString("biome")));
        CompoundTag chunks = plot.getCompound("chunks");
        int minSection = ctx.target.minSection(dim);
        Set<String> keyed = new HashSet<>();
        for (String key : chunks.getAllKeys()) {
            if (toList(chunks.getCompound(key), minSection)) keyed.add(key);
        }
        for (String key : chunks.getAllKeys()) {
            CompoundTag chunk = chunks.getCompound(key);
            ChunkPos pos = new ChunkPos(Long.parseLong(key));
            Tag x = chunk.get("xPos"), z = chunk.get("zPos");
            chunk.putInt("xPos", pos.x + baseX);
            chunk.putInt("zPos", pos.z + baseZ);
            converter.convert(chunk);
            chunk.remove("xPos");
            chunk.remove("zPos");
            if (x != null) chunk.put("xPos", x);
            if (z != null) chunk.put("zPos", z);
        }
        int placed = 0, blocked = 0;
        for (CompoundTag core : converter.plotCores) {
            CompoundTag props = core.getCompound("Properties");
            List<Long> cells = new ArrayList<>();
            for (Map.Entry<Long, CompoundTag> cell : ctx.target.footprint(core.getString("Name"), props, core.getLong("pos"), ctx.rbmkColumnHeight()).entrySet()) {
                BlockPos pos = BlockPos.of(cell.getKey());
                CompoundTag chunk = chunks.getCompound(Long.toString(ChunkPos.asLong((pos.getX() >> 4) - baseX, (pos.getZ() >> 4) - baseZ)));
                CompoundTag there = chunk.isEmpty() ? null : ChunkBlocks.get(chunk, pos);
                if (there == null || !there.getString("Name").equals("minecraft:air") || !room.test(pos)) {
                    blocked++;
                    continue;
                }
                ChunkBlocks.set(chunk, pos, cell.getValue());
                cells.add(cell.getKey());
                placed++;
            }
            // a cell finds its machine through the level's core index, which nothing fills for a ship's plot
            if (!cells.isEmpty()) {
                CompoundTag record = new CompoundTag();
                record.putString("dim", dim);
                record.putLong("pos", core.getLong("pos"));
                record.putLongArray("cells", cells.stream().mapToLong(Long::longValue).toArray());
                ctx.add("plotcells", record);
            }
        }
        int linked = 0;
        for (CompoundTag pile : converter.plotPiles) {
            int[] box = pile.getIntArray("box");
            if (box.length != 6) continue;
            for (String key : chunks.getAllKeys()) {
                for (Tag t : chunks.getCompound(key).getList("block_entities", Tag.TAG_COMPOUND)) {
                    CompoundTag be = (CompoundTag) t;
                    if (inside(box, be.getInt("x"), be.getInt("y"), be.getInt("z")) && ctx.target.linkPileMember(be, pile.getLong("pos"))) linked++;
                }
            }
        }
        ctx.report.count("pile blocks linked", linked);
        for (String key : keyed) toKeyed(chunks.getCompound(key), minSection);
        ctx.report.count("airship cells placed", placed);
        if (blocked > 0) ctx.report.line(where + ": " + blocked + " multiblock cell(s) blocked, left out");
    }

    private static boolean inside(int[] box, int x, int y, int z) {
        return x >= Math.min(box[0], box[3]) && x <= Math.max(box[0], box[3]) && y >= Math.min(box[1], box[4])
                && y <= Math.max(box[1], box[4]) && z >= Math.min(box[2], box[5]) && z <= Math.max(box[2], box[5]);
    }

    /** Sable's index-keyed sections to vanilla's list; false when the chunk already has vanilla's shape. */
    static boolean toList(CompoundTag chunk, int minSection) {
        if (!(chunk.get("sections") instanceof CompoundTag keyed)) return false;
        ListTag list = new ListTag();
        for (String index : keyed.getAllKeys()) {
            CompoundTag section = keyed.getCompound(index).copy();
            section.putByte("Y", (byte) (Integer.parseInt(index) + minSection));
            list.add(section);
        }
        chunk.put("sections", list);
        return true;
    }

    static void toKeyed(CompoundTag chunk, int minSection) {
        CompoundTag keyed = new CompoundTag();
        for (Tag t : chunk.getList("sections", Tag.TAG_COMPOUND)) {
            CompoundTag section = (CompoundTag) t;
            int index = section.getByte("Y") - minSection;
            section.remove("Y");
            keyed.put(Integer.toString(index), section);
        }
        chunk.put("sections", keyed);
    }
}
