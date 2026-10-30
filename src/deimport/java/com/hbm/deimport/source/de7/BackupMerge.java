package com.hbm.deimport.source.de7;

import com.hbm.deimport.engine.ChunkBlocks;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.PackedSection;
import com.hbm.delegacy.de7.Walker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * What a start without a DE addon's converter lost outside the addon's dimensions, put back from DE's backup: the
 * addon's blocks the game loaded as air, in terrain and on airships, and the stacks it dropped from containers and
 * entities. Only an empty place, or one still holding the raw addon block, is filled, so nothing built since goes.
 */
public final class BackupMerge {
    private static final Set<String> AIR = Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air");

    private BackupMerge() {}

    /** An id or block name {@code only} accepts, anywhere in {@code tag}. */
    public static boolean holds(Tag tag, Predicate<String> only) {
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                Tag value = compound.get(key);
                if ((key.equals("id") || key.equals("Name")) && value instanceof StringTag text && only.test(text.getAsString())) return true;
                if (holds(value, only)) return true;
            }
        } else if (tag instanceof ListTag list) {
            for (Tag t : list) if (holds(t, only)) return true;
        }
        return false;
    }

    /** The addon's blocks of {@code backed}, DE's copy of {@code live}'s chunk, where {@code live} has room; returns how many. */
    public static int blocks(Ctx ctx, String dim, CompoundTag live, CompoundTag backed, Predicate<String> only) {
        CompoundTag masked = backed.copy();
        Map<Integer, CompoundTag> now = new HashMap<>();
        for (Tag t : live.getList("sections", Tag.TAG_COMPOUND)) now.put((int) ((CompoundTag) t).getByte("Y"), (CompoundTag) t);
        Set<Long> chosen = new HashSet<>();
        for (Tag t : masked.getList("sections", Tag.TAG_COMPOUND)) {
            int y = ((CompoundTag) t).getByte("Y");
            CompoundTag there = now.get(y);
            for (int i : choose(((CompoundTag) t).getCompound("block_states"), there == null ? null : there.getCompound("block_states"), only)) chosen.add(key(y, i));
        }
        if (chosen.isEmpty()) return 0;
        keepEntities(masked, chosen);
        new ChunkConverter(ctx, dim, false, only, true).convert(masked);
        Set<Long> copied = new HashSet<>();
        for (Tag t : masked.getList("sections", Tag.TAG_COMPOUND)) {
            int y = ((CompoundTag) t).getByte("Y");
            CompoundTag there = now.get(y);
            if (there != null) copy(((CompoundTag) t).getCompound("block_states"), there.getCompound("block_states"), y, chosen, only, copied);
        }
        moveEntities(masked, live, copied);
        if (!copied.isEmpty()) {
            // what the game saved since was lit and measured around the empty places
            live.remove("Heightmaps");
            live.putBoolean("isLightOn", false);
        }
        return copied.size();
    }

    /**
     * The addon's stacks of {@code backed}'s block entities, DE's copy of {@code live}'s chunk, back into the same
     * containers: only where the same block stands, so a container replaced since takes nothing.
     */
    public static int containers(Ctx ctx, CompoundTag live, CompoundTag backed, Walker walker, Predicate<String> only, String where) {
        Map<Long, CompoundTag> now = new HashMap<>();
        for (Tag t : live.getList("block_entities", Tag.TAG_COMPOUND)) now.put(key((CompoundTag) t), (CompoundTag) t);
        int restored = 0;
        for (Tag t : backed.getList("block_entities", Tag.TAG_COMPOUND)) {
            CompoundTag be = (CompoundTag) t, there = now.get(key(be));
            BlockPos pos = new BlockPos(be.getInt("x"), be.getInt("y"), be.getInt("z"));
            if (there == null || !holds(be, only) || !same(ctx, ChunkBlocks.get(live, pos), ChunkBlocks.get(backed, pos))) continue;
            restored += stacks(there, BlockEntities.layout(be), walker, only, where);
        }
        return restored;
    }

    /** The addon's blocks and stacks of {@code backed}, DE's copy of the airship {@code live}, where it has room; returns how many. */
    public static int ship(Ctx ctx, String dim, CompoundTag live, CompoundTag backed, Predicate<String> only, String where) {
        CompoundTag livePlot = live.getCompound("plot"), backedPlot = backed.getCompound("plot");
        // Sable loads a ship back into the plot it saved, so a ship with the same id has the same one
        for (String key : List.of("log_size", "plot_x", "plot_z")) if (livePlot.getInt(key) != backedPlot.getInt(key)) return 0;
        CompoundTag masked = backed.copy();
        CompoundTag liveChunks = livePlot.getCompound("chunks"), maskedChunks = masked.getCompound("plot").getCompound("chunks");
        int minSection = ctx.target.minSection(dim);
        int stacks = 0;
        Walker walker = new Walker(ctx);
        for (String key : liveChunks.getAllKeys()) {
            CompoundTag before = backedPlot.getCompound("chunks").getCompound(key), now = liveChunks.getCompound(key);
            Map<Long, CompoundTag> held = new HashMap<>();
            for (Tag t : now.getList("block_entities", Tag.TAG_COMPOUND)) held.put(key((CompoundTag) t), (CompoundTag) t);
            for (Tag t : before.getList("block_entities", Tag.TAG_COMPOUND)) {
                CompoundTag be = (CompoundTag) t, there = held.get(key(be));
                int y = be.getInt("y"), i = PackedSection.indexOf(be.getInt("x"), y, be.getInt("z"));
                String index = Integer.toString((y >> 4) - minSection);
                if (there != null && holds(be, only) && same(ctx, entry(now.getCompound("sections").getCompound(index), i), entry(before.getCompound("sections").getCompound(index), i)))
                    stacks += stacks(there, BlockEntities.layout(be), walker, only, where);
            }
        }
        boolean any = false;
        for (String key : maskedChunks.getAllKeys()) {
            CompoundTag sections = maskedChunks.getCompound(key).getCompound("sections");
            CompoundTag there = liveChunks.getCompound(key).getCompound("sections");
            Set<Long> chosen = new HashSet<>();
            for (String index : sections.getAllKeys()) {
                CompoundTag states = there.contains(index) ? there.getCompound(index).getCompound("block_states") : null;
                int y = Integer.parseInt(index) + minSection;
                for (int i : choose(sections.getCompound(index).getCompound("block_states"), states, only)) chosen.add(key(y, i));
            }
            keepEntities(maskedChunks.getCompound(key), chosen);
            any |= !chosen.isEmpty();
        }
        if (!any) return stacks;
        int baseX = (Airships.PLOT_ORIGIN + livePlot.getInt("plot_x")) << livePlot.getInt("log_size");
        int baseZ = (Airships.PLOT_ORIGIN + livePlot.getInt("plot_z")) << livePlot.getInt("log_size");
        Airships.plot(ctx, dim, masked, where, new ChunkConverter(ctx, dim, true, only, true), pos -> {
            CompoundTag sections = liveChunks.getCompound(Long.toString(ChunkPos.asLong((pos.getX() >> 4) - baseX, (pos.getZ() >> 4) - baseZ))).getCompound("sections");
            String index = Integer.toString((pos.getY() >> 4) - minSection);
            return !sections.contains(index) || room(new PackedSection(sections.getCompound(index).getCompound("block_states")),
                    PackedSection.indexOf(pos.getX(), pos.getY(), pos.getZ()), only);
        });
        int restored = stacks;
        for (String key : maskedChunks.getAllKeys()) {
            CompoundTag from = maskedChunks.getCompound(key), to = liveChunks.getCompound(key);
            CompoundTag fromSections = from.getCompound("sections"), toSections = to.getCompound("sections");
            Set<Long> copied = new HashSet<>();
            for (String index : fromSections.getAllKeys()) {
                // Sable leaves out a section, or a whole chunk, that is all air
                boolean fresh = !toSections.contains(index);
                if (fresh) {
                    CompoundTag air = new CompoundTag();
                    air.put("palette", new ListTag());
                    ((ListTag) air.get("palette")).add(airState());
                    CompoundTag section = new CompoundTag();
                    section.put("block_states", air);
                    toSections.put(index, section);
                }
                int before = copied.size();
                copy(fromSections.getCompound(index).getCompound("block_states"), toSections.getCompound(index).getCompound("block_states"),
                        Integer.parseInt(index) + minSection, null, only, copied);
                if (fresh && copied.size() == before) toSections.remove(index);
            }
            if (copied.isEmpty()) continue;
            // Sable keeps a plot's heightmaps and stored light, and only adds light from sources on load
            to.remove("heightmaps");
            for (String index : toSections.getAllKeys()) {
                toSections.getCompound(index).remove("BlockLight");
                toSections.getCompound(index).remove("SkyLight");
            }
            to.put("sections", toSections);
            moveEntities(from, to, copied);
            liveChunks.put(key, to);
            restored += copied.size();
        }
        livePlot.put("chunks", liveChunks);
        return restored;
    }

    /**
     * The addon's stacks of {@code backed}, DE's copy of a container or entity in Continuum's layout, whose place in
     * {@code live} is empty: a slot (a vanilla container's, or the inventory of HBM's crates and machines), an armor or
     * hand slot, an item frame's item, a one-stack block's. Only a stack of the addon's own item: the game dropped
     * those whole. A stack it kept, such as a shulker box, gets back what it held of the addon's.
     */
    public static int stacks(CompoundTag live, CompoundTag backed, Walker walker, Predicate<String> only, String where) {
        int restored = slots(live, backed, walker, only, where);
        if (live.contains("inventory", Tag.TAG_COMPOUND)) {
            CompoundTag inventory = live.getCompound("inventory");
            boolean typeless = !filled(inventory, 1);
            restored += slots(inventory, backed.getCompound("inventory"), walker, only, where);
            // a mass storage counts what it holds beside its type in slot 1, and zeroes the count without it
            if (typeless && filled(inventory, 1) && backed.get("stack") instanceof IntTag held && live.get("stack") instanceof IntTag now
                    && now.getAsInt() == 0 && live.contains("capacity", Tag.TAG_INT))
                live.putInt("stack", Math.min(held.getAsInt(), live.getInt("capacity")));
        }
        for (String key : List.of("ArmorItems", "HandItems")) {
            ListTag before = backed.getList(key, Tag.TAG_COMPOUND), after = live.getList(key, Tag.TAG_COMPOUND);
            boolean changed = false;
            for (int i = 0; i < before.size(); i++) {
                CompoundTag stack = before.getCompound(i).copy();
                while (after.size() <= i) after.add(new CompoundTag());
                if (!after.getCompound(i).isEmpty()) {
                    restored += held(after.getCompound(i), stack, walker, only, where);
                    continue;
                }
                if (!only.test(stack.getString("id")) || !walker.stacks().convert(stack, where)) continue;
                after.set(i, stack);
                changed = true;
                restored++;
            }
            if (changed) live.put(key, after);
        }
        for (String key : List.of("Item", "item", "stack")) {
            CompoundTag item = backed.getCompound(key).copy();
            if (live.get(key) instanceof CompoundTag kept && !kept.isEmpty()) {
                restored += held(kept, item, walker, only, where);
                continue;
            }
            if (!only.test(item.getString("id")) || live.contains(key) || !walker.stacks().convert(item, where)) continue;
            live.put(key, item);
            restored++;
        }
        return restored;
    }

    private static int slots(CompoundTag live, CompoundTag backed, Walker walker, Predicate<String> only, String where) {
        if (!backed.contains("Items", Tag.TAG_LIST)) return 0;
        ListTag items = live.getList("Items", Tag.TAG_COMPOUND);
        Map<Integer, CompoundTag> taken = new HashMap<>();
        for (Tag t : items) taken.put((int) ((CompoundTag) t).getByte("Slot"), (CompoundTag) t);
        int restored = 0;
        for (Tag t : backed.getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag stack = ((CompoundTag) t).copy();
            int slot = stack.getByte("Slot");
            if (taken.containsKey(slot)) {
                restored += held(taken.get(slot), stack, walker, only, where);
                continue;
            }
            if (!only.test(stack.getString("id")) || !walker.stacks().convert(stack, where)) continue;
            taken.put(slot, stack);
            items.add(stack);
            restored++;
        }
        if (restored > 0) live.put("Items", items);
        return restored;
    }

    /**
     * The components of {@code backed} holding the addon's stacks (a shulker box's contents, a bundle's) that {@code
     * live}, the same item, no longer has: the first import set such a component aside whole, as nothing could read it.
     */
    private static int held(CompoundTag live, CompoundTag backed, Walker walker, Predicate<String> only, String where) {
        CompoundTag raw = backed.getCompound("components");
        // a stack that grew since would hand the contents to more items than had them
        if (!holds(raw, only) || live.getInt("count") > backed.getInt("count")) return 0;
        CompoundTag converted = backed.copy();
        if (!walker.stacks().convert(converted, where) || !converted.getString("id").equals(live.getString("id"))) return 0;
        CompoundTag from = converted.getCompound("components"), to = live.getCompound("components");
        int restored = 0;
        for (String key : raw.getAllKeys()) {
            if (!holds(raw.get(key), only) || to.contains(key) || !from.contains(key)) continue;
            to.put(key, from.get(key).copy());
            restored++;
        }
        if (restored > 0) live.put("components", to);
        return restored;
    }

    private static boolean filled(CompoundTag inventory, int slot) {
        return inventory.getList("Items", Tag.TAG_COMPOUND).stream().anyMatch(t -> ((CompoundTag) t).getByte("Slot") == slot);
    }

    /** The block standing now is the one DE's copy converts to, so the slots there mean the same. */
    private static boolean same(Ctx ctx, @Nullable CompoundTag now, @Nullable CompoundTag before) {
        return now != null && before != null && now.getString("Name").equals(BlockStates.convert(before, ctx.target).entry().getString("Name"));
    }

    private static @Nullable CompoundTag entry(CompoundTag section, int i) {
        if (!section.contains("block_states")) return null;
        PackedSection packed = new PackedSection(section.getCompound("block_states"));
        return packed.palette.isEmpty() ? null : (CompoundTag) packed.palette.get(packed.index[i]);
    }

    /** Blocks of the addon in {@code states} where {@code there} has room; everything else in {@code states} becomes air. */
    private static List<Integer> choose(CompoundTag states, @Nullable CompoundTag there, Predicate<String> only) {
        PackedSection de = new PackedSection(states);
        PackedSection now = there == null ? null : new PackedSection(there);
        de.palette.add(airState());
        int air = de.palette.size() - 1;
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < PackedSection.SIZE; i++) {
            if (only.test(name(de, i)) && room(now, i, only)) out.add(i);
            else de.index[i] = air;
        }
        de.store(states);
        return out;
    }

    /** Copies the blocks of {@code from} into {@code to} where it has room, restricted to {@code chosen} when given. */
    private static void copy(CompoundTag from, CompoundTag to, int y, @Nullable Set<Long> chosen, Predicate<String> only, Set<Long> copied) {
        PackedSection source = new PackedSection(from), target = new PackedSection(to);
        int before = copied.size();
        for (int i = 0; i < PackedSection.SIZE; i++) {
            if (AIR.contains(name(source, i)) || chosen != null && !chosen.contains(key(y, i)) || !room(target, i, only)) continue;
            target.palette.add(source.palette.get(source.index[i]).copy());
            target.index[i] = target.palette.size() - 1;
            copied.add(key(y, i));
        }
        if (copied.size() > before) target.store(to);
    }

    /** Air, or the addon's raw block a start without its converter left in place. */
    private static boolean room(@Nullable PackedSection section, int i, Predicate<String> only) {
        if (section == null) return true;
        String name = name(section, i);
        return AIR.contains(name) || only.test(name);
    }

    private static void keepEntities(CompoundTag chunk, Set<Long> chosen) {
        ListTag kept = new ListTag();
        for (Tag t : chunk.getList("block_entities", Tag.TAG_COMPOUND)) if (chosen.contains(key((CompoundTag) t))) kept.add(t);
        chunk.put("block_entities", kept);
        for (String key : List.of("block_ticks", "fluid_ticks", "structures")) chunk.remove(key);
    }

    /** The block entities of the copied blocks, replacing any at those places. */
    private static void moveEntities(CompoundTag from, CompoundTag to, Set<Long> copied) {
        ListTag entities = to.getList("block_entities", Tag.TAG_COMPOUND);
        entities.removeIf(t -> copied.contains(key((CompoundTag) t)));
        for (Tag t : from.getList("block_entities", Tag.TAG_COMPOUND)) if (copied.contains(key((CompoundTag) t))) entities.add(t);
        to.put("block_entities", entities);
    }

    private static String name(PackedSection section, int i) {
        return ((CompoundTag) section.palette.get(section.index[i])).getString("Name");
    }

    private static CompoundTag airState() {
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        return air;
    }

    private static long key(int sectionY, int index) {
        return ((long) sectionY << 12) | index;
    }

    private static long key(CompoundTag be) {
        return key(be.getInt("y") >> 4, PackedSection.indexOf(be.getInt("x"), be.getInt("y"), be.getInt("z")));
    }
}
