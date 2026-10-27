package com.hbm.deimport.source.de7;

import com.hbm.deimport.engine.ChunkBlocks;
import com.hbm.deimport.engine.Ctx;
import com.hbm.delegacy.de7.Walker;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private Airships() {}

    /** The plot's chunks go through the chunk converter; cells are rebuilt inside the plot, never through the world. */
    public static void convert(Ctx ctx, String dim, CompoundTag ship, String where) {
        CompoundTag chunks = ship.getCompound("plot").getCompound("chunks");
        int minSection = ctx.target.minSection(dim);
        Set<String> keyed = new HashSet<>();
        for (String key : chunks.getAllKeys()) {
            if (toList(chunks.getCompound(key), minSection)) keyed.add(key);
        }
        ChunkConverter converter = new ChunkConverter(ctx, dim, true);
        for (String key : chunks.getAllKeys()) {
            CompoundTag chunk = chunks.getCompound(key);
            ChunkPos pos = new ChunkPos(Long.parseLong(key));
            boolean hadPos = chunk.contains("xPos");
            chunk.putInt("xPos", pos.x);
            chunk.putInt("zPos", pos.z);
            converter.convert(chunk);
            if (!hadPos) {
                chunk.remove("xPos");
                chunk.remove("zPos");
            }
        }
        int placed = 0, blocked = 0;
        for (CompoundTag core : converter.plotCores) {
            CompoundTag props = core.getCompound("Properties");
            List<Long> cells = new ArrayList<>();
            for (Map.Entry<Long, CompoundTag> cell : ctx.target.footprint(core.getString("Name"), props, core.getLong("pos"), ctx.rbmkColumnHeight()).entrySet()) {
                BlockPos pos = BlockPos.of(cell.getKey());
                CompoundTag chunk = chunks.getCompound(Long.toString(new ChunkPos(pos).toLong()));
                CompoundTag there = chunk.isEmpty() ? null : ChunkBlocks.get(chunk, pos);
                if (there == null || !there.getString("Name").equals("minecraft:air")) {
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
        CompoundTag rest = ship.copy();
        rest.remove("plot");
        new Walker(ctx).walk(rest, where);
        for (String key : rest.getAllKeys()) ship.put(key, rest.get(key));
        ctx.report.count("airships converted");
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
