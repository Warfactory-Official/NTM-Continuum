package com.hbm.deimport.source.de7;

import com.hbm.deimport.engine.ChunkBlocks;
import com.hbm.deimport.engine.Ctx;
import com.hbm.delegacy.de7.Walker;
import com.hbm.delegacy.DeAddons;
import com.hbm.delegacy.de7.Tables;
import com.hbm.deimport.engine.PackedSection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * One DE terrain chunk to Continuum: palettes, block entities (converted, refunded or synthesized where DE had none),
 * scheduled ticks, structure names and DE's own chunk keys. Multiblock cores are collected for the live rebuild.
 */
public final class ChunkConverter {
    public static final String STAMP = "hbm_data_version";
    /** DE's chunk data version this importer reads; older chunks are brought up first, newer ones left alone. */
    public static final int NEWEST = 7;
    private static final int CHEST_SLOTS = 27;
    /** DE's chunk keys before it stamped chunks, which DE reads as data version 0; no other mod writes it. */
    public static final String OLD_INDEX = "hbm_core_index_22";
    /** Block-state flags DE keeps on the block and Continuum on its block entity: DE block, property, block-entity key. */
    private static final Map<String, Map<String, String>> STATE_TO_ENTITY = Map.of(
            "hbm:machine_fluidtank", Map.of("damaged", "exploded"),
            "hbm:lantern_behemoth", Map.of("broken", "isBroken"));
    /** Recomputed by Continuum every tick (a Sable airship's tilt). */
    private static final Set<String> DERIVED_PROPERTIES = Set.of("tilted");
    private static final Set<String> POSITION_KEYS = Set.of("id", "x", "y", "z", "keepPacked");
    private static final Map<String, String> STRUCTURES = Map.ofEntries(
            Map.entry("hbm:broadcasting_tower", "hbm:broadcaster_tower"), Map.entry("hbm:crashed_plane_1", "hbm:plane1"),
            Map.entry("hbm:crashed_plane_2", "hbm:plane2"), Map.entry("hbm:forest_chem", "hbm:forestchem"),
            Map.entry("hbm:laboratory", "hbm:labolatory"), Map.entry("hbm:null_spawn_ocean", "hbm:null_ocean"),
            Map.entry("hbm:null_spawn_plains", "hbm:null_plains"), Map.entry("hbm:radio_house", "hbm:radio"),
            Map.entry("hbm:ruin_a", "hbm:ruina"), Map.entry("hbm:ruin_b", "hbm:ruinb"), Map.entry("hbm:ruin_c", "hbm:ruinc"),
            Map.entry("hbm:ruin_d", "hbm:ruind"), Map.entry("hbm:ruin_e", "hbm:ruine"), Map.entry("hbm:ruin_f", "hbm:ruinf"),
            Map.entry("hbm:ruin_g", "hbm:ruing"), Map.entry("hbm:ruin_h", "hbm:ruinh"), Map.entry("hbm:ruin_i", "hbm:ruini"),
            Map.entry("hbm:ruin_j", "hbm:ruinj"));

    private static final Map<String, String> PIECES = Map.ofEntries(
            Map.entry("hbm:bunker_bathroom_l", "hbm:ntmbbathrooml"), Map.entry("hbm:bunker_bedroom_l", "hbm:ntmbbedrooml"),
            Map.entry("hbm:bunker_corridor", "hbm:ntmbcorridor"), Map.entry("hbm:bunker_fun_junction", "hbm:ntmbfunjunction"),
            Map.entry("hbm:bunker_laboratory", "hbm:ntmblaboratory"), Map.entry("hbm:bunker_power_room", "hbm:ntmbpowerroom"),
            Map.entry("hbm:bunker_starting_hub", "hbm:ntmbstartinghub"),
            Map.entry("hbm:large_office_corner_piece", "hbm:ntmlargeofficecorner"),
            Map.entry("hbm:large_office_piece", "hbm:ntmlargeoffice"), Map.entry("hbm:house1_piece", "hbm:ntmhouse1"),
            Map.entry("hbm:house2_piece", "hbm:ntmhouse2"), Map.entry("hbm:lab1_piece", "hbm:ntmlab1"),
            Map.entry("hbm:lab2_piece", "hbm:ntmlab2"), Map.entry("hbm:rural_house1_piece", "hbm:ntmruralhouse1"),
            Map.entry("hbm:silo_piece", "hbm:ntmsilocomponent"));

    private final Ctx ctx;
    private final Walker walker;
    private final String dim;
    private final boolean plot;
    /** The DE ids this converter takes; any other id is kept as it is. */
    private final Predicate<String> source;
    private final boolean late;
    /** Multiblock cores in an airship's plot, {pos, block}: rebuilt in the plot, not through the world. */
    public final List<CompoundTag> plotCores = new ArrayList<>();
    /** Pile cores in an airship's plot, {pos, box}: linked inside the ship, whose chunks are all at hand. */
    public final List<CompoundTag> plotPiles = new ArrayList<>();
    private final List<Refund> refunds = new ArrayList<>();

    private record Refund(BlockPos pos, List<CompoundTag> items, String why) {}

    public ChunkConverter(Ctx ctx, String dim) {
        this(ctx, dim, false);
    }

    /** {@code plot}: a Sable airship's chunk, which DE does not stamp and whose positions are not world blocks. */
    public ChunkConverter(Ctx ctx, String dim, boolean plot) {
        this(ctx, dim, plot, DeAddons::isSource);
    }

    /** Converts only the ids {@code only} accepts, in a chunk an import without their converter took DE's stamp off. */
    public ChunkConverter(Ctx ctx, String dim, Predicate<String> only) {
        this(ctx, dim, false, only, false);
    }

    /** Converts only the blocks {@code only} accepts; {@code pristine}: DE's own copy, so what they hold converts whole. */
    public ChunkConverter(Ctx ctx, String dim, boolean plot, Predicate<String> only, boolean pristine) {
        this.ctx = ctx;
        this.walker = pristine ? new Walker(ctx) : new Walker(ctx, only);
        this.dim = dim;
        this.plot = plot;
        this.source = only;
        this.late = true;
    }

    private ChunkConverter(Ctx ctx, String dim, boolean plot, Predicate<String> source) {
        this.ctx = ctx;
        this.walker = new Walker(ctx);
        this.dim = dim;
        this.plot = plot;
        this.source = source;
        this.late = false;
    }

    /**
     * Returns the converted chunk, or null if it carries no DE stamp and is left as it is. A chunk from a newer DE than
     * this importer knows is parked and left as it is too.
     */
    public @Nullable CompoundTag convert(CompoundTag chunk) {
        if (!plot && !late && !chunk.contains(STAMP) && !chunk.contains(OLD_INDEX)) return null;
        int cx = chunk.getInt("xPos"), cz = chunk.getInt("zPos");
        String where = dim + " chunk " + cx + "," + cz;
        refunds.clear();
        int version = chunk.getInt(STAMP);
        if (!plot && version > NEWEST) {
            ctx.parking.park("chunk", where, "data version " + version, chunk.copy());
            ctx.report.line(where + " was saved by a newer DE (data version " + version + "); left as it is");
            ctx.report.count("chunks from a newer DE");
            return null;
        }
        if (!plot && !late && version < NEWEST) OldChunks.upgrade(chunk, version);

        Map<Integer, Section> sections = new HashMap<>();
        for (Tag t : chunk.getList("sections", Tag.TAG_COMPOUND)) {
            CompoundTag section = (CompoundTag) t;
            if (!section.contains("block_states")) continue;
            sections.put((int) section.getByte("Y"), new Section(section, where));
        }

        // Block entities: Continuum's type for the converted block, or a refund when the block has no counterpart.
        Map<Long, CompoundTag> blockEntities = new HashMap<>();
        Set<Long> deEntities = new HashSet<>();
        for (Tag t : chunk.getList("block_entities", Tag.TAG_COMPOUND)) {
            CompoundTag be = (CompoundTag) t;
            BlockPos pos = new BlockPos(be.getInt("x"), be.getInt("y"), be.getInt("z"));
            deEntities.add(pos.asLong());
            Section section = sections.get(pos.getY() >> 4);
            int slot = section == null ? -1 : section.packed.index[PackedSection.indexOf(pos.getX(), pos.getY(), pos.getZ())];
            String deBlock = slot < 0 ? "" : ((CompoundTag) section.packed.palette.get(slot)).getString("Name");
            // vanilla's and other mods' block entities are their own data, kept even when their mod is missing; only
            // the DE stacks inside them convert
            if (!source.test(deBlock) && !source.test(be.getString("id"))) {
                if (!late) walker.walk(be, where + " " + pos.toShortString());
                blockEntities.put(pos.asLong(), be);
                continue;
            }
            if (section == null) {
                parkFields(be, where + " " + pos.toShortString());
                continue;
            }
            BlockStates.Converted state = section.converted.get(slot);
            if (state.kind() == BlockStates.Kind.REFUND) {
                refund(be, pos, state, where);
                continue;
            }
            CompoundTag entry = state.entry();
            String type = state.kind() == BlockStates.Kind.CELL ? null
                    : ctx.target.blockEntityType(entry.getString("Name"), entry.getCompound("Properties"));
            if (type == null) {
                parkFields(be, where + " " + pos.toShortString());
                continue;
            }
            if (be.getString("id").equals("hbm:pile_core") && be.contains("members", Tag.TAG_INT_ARRAY)) {
                CompoundTag pile = new CompoundTag();
                pile.putString("dim", dim);
                pile.putLong("pos", pos.asLong());
                pile.putIntArray("box", be.getIntArray("members"));
                if (plot) plotPiles.add(pile);
                else ctx.add("piles", pile);
            }
            BlockEntities.convert(be, type, ctx, walker, where + " " + pos.toShortString());
            if (!plot && DeAddons.isTarget(type) && be.contains("LootTable", Tag.TAG_STRING)) {
                CompoundTag loot = new CompoundTag();
                loot.putString("dim", dim);
                loot.putLong("pos", pos.asLong());
                loot.putString("table", be.getString("LootTable"));
                loot.putLong("seed", be.getLong("LootTableSeed"));
                ctx.add("loot", loot);
                be.remove("LootTable");
                be.remove("LootTableSeed");
            }
            wires(be, pos);
            String ductFluid = ctx.ductFluids(dim).get(pos.asLong());
            if (ductFluid != null && !be.contains("type")) be.putString("type", Tables.get().fluid(ductFluid));
            STATE_TO_ENTITY.getOrDefault(deBlock, Map.of()).forEach((property, key) -> {
                if (state.dropped().contains(property)) be.putBoolean(key, "true".equals(state.dropped().getString(property)));
            });
            check(be, entry, where + " " + pos.toShortString());
            blockEntities.put(pos.asLong(), be);
        }

        // Every block that has a Continuum block entity but none saved gets one now, so ticking machines start.
        Map<Long, String> ducts = ductFluids(chunk);
        for (var e : sections.entrySet()) {
            Section section = e.getValue();
            for (int slot = 0; slot < section.converted.size(); slot++) {
                if (section.converted.get(slot).kind() == BlockStates.Kind.REFUND)
                    refundBlocks(section, slot, e.getKey(), cx, cz, deEntities, where);
                CompoundTag entry = section.converted.get(slot).entry();
                String name = entry.getString("Name");
                if (!section.ours.get(slot) || !DeAddons.isTarget(name)) continue;
                boolean core = ctx.target.isMultiblockCore(name);
                boolean conductor = ctx.target.isPlacedConductor(name);
                String type = ctx.target.blockEntityType(name, entry.getCompound("Properties"));
                if (type == null && !core && !conductor) continue;
                for (int i = 0; i < PackedSection.SIZE; i++) {
                    if (section.packed.index[i] != slot) continue;
                    BlockPos pos = new BlockPos(cx * 16 + (i & 15), e.getKey() * 16 + (i >> 8), cz * 16 + ((i >> 4) & 15));
                    if (core && plot) {
                        CompoundTag record = entry.copy();
                        record.putLong("pos", pos.asLong());
                        plotCores.add(record);
                    } else if (core) {
                        ctx.core(dim, pos.asLong(), name);
                    }
                    if (conductor) ctx.conductor(dim, pos.asLong(), plot);
                    if (type == null || blockEntities.containsKey(pos.asLong())) continue;
                    CompoundTag be = new CompoundTag();
                    be.putString("id", type);
                    be.putInt("x", pos.getX());
                    be.putInt("y", pos.getY());
                    be.putInt("z", pos.getZ());
                    String fluid = ducts.getOrDefault(pos.asLong(), ctx.ductFluids(dim).get(pos.asLong()));
                    if (fluid != null) be.putString("type", Tables.get().fluid(fluid));
                    wires(be, pos);
                    blockEntities.put(pos.asLong(), be);
                    ctx.report.count("block entities created");
                }
            }
        }
        ListTag beList = new ListTag();
        beList.addAll(blockEntities.values());
        chunk.put("block_entities", beList);

        renameTicks(chunk.getList("block_ticks", Tag.TAG_COMPOUND), sections);
        for (Section section : sections.values()) section.store();
        Set<Long> taken = new HashSet<>(blockEntities.keySet());
        for (Refund refund : refunds) placeRefund(chunk, beList, taken, refund, where);
        if (late) {
            if (!plot) ctx.report.count("chunks with an addon's blocks converted");
            return chunk;
        }
        structures(chunk.getCompound("structures"), where);
        walker.walkList(chunk.getList("entities", Tag.TAG_COMPOUND), where);

        if (chunk.contains("hbm_rad")) ctx.parking.park("radiation", where, "hbm_rad", chunk.get("hbm_rad"));
        if (chunk.contains("hbm_pending_fallers")) ctx.parking.park("pending_fallers", where, "hbm_pending_fallers", chunk.get("hbm_pending_fallers"));
        for (String key : List.of(STAMP, "hbm_core_index", "hbm_core_index_22", "hbm_duct_fluids", "hbm_rad", "hbm_pending_fallers"))
            chunk.remove(key);
        chunkAttachments(chunk, where);
        ctx.report.count("chunks converted");
        return chunk;
    }

    /** DE keeps a pylon's wires in its power graph; Continuum on the pylon as {@code conCount} and {@code con<i>}. */
    private void wires(CompoundTag be, BlockPos pos) {
        long[] links = ctx.wires(dim).get(pos.asLong());
        if (links == null || be.contains("conCount")) return;
        be.putInt("conCount", links.length);
        for (int i = 0; i < links.length; i++) {
            BlockPos to = BlockPos.of(links[i]);
            be.putIntArray("con" + i, new int[]{to.getX(), to.getY(), to.getZ()});
        }
        ctx.report.count("pylon wires restored", links.length);
    }

    /** A DE block entity with no Continuum block entity to go to: parked if it holds anything beyond its position. */
    private void parkFields(CompoundTag be, String where) {
        if (be.getAllKeys().stream().anyMatch(k -> !POSITION_KEYS.contains(k))) {
            ctx.parking.park("block_entity", where, be.getString("id"), be);
            ctx.report.count("block entities parked");
        }
    }

    /** Parks every field Continuum's own block entity does not keep when it loads and saves the converted tag. */
    private void check(CompoundTag be, CompoundTag entry, String where) {
        Map<String, Tag> lost = ctx.target.blockEntityLost(entry.getString("Name"), entry.getCompound("Properties"), be);
        if (lost == null) {
            ctx.report.count("block entities unchecked");
            return;
        }
        for (Map.Entry<String, Tag> field : lost.entrySet()) {
            ctx.parking.park("block_entity_field", where + " " + field.getKey(), be.getString("id"), field.getValue());
        }
        ctx.report.count("block entity fields parked", lost.size());
        ctx.report.count("block entities checked");
    }

    private void refund(CompoundTag be, BlockPos pos, BlockStates.Converted state, String where) {
        List<CompoundTag> items = new ArrayList<>(BlockEntities.contents(be));
        if (!state.refundItem().isEmpty()) items.add(item(state.refundItem(), 1));
        for (CompoundTag item : items) walker.walk(item, where);
        ctx.parking.park("block_entity", where + " " + pos.toShortString(), be.getString("id"), be);
        handBack(pos, items, "no Continuum block for " + be.getString("id"), where);
        ctx.report.count("blocks refunded");
    }

    /** Refunded blocks without a block entity: their state is parked with every position, their item handed back. */
    private void refundBlocks(Section section, int slot, int sectionY, int cx, int cz, Set<Long> deEntities, String where) {
        List<Long> positions = new ArrayList<>();
        for (int i = 0; i < PackedSection.SIZE; i++) {
            if (section.packed.index[i] != slot) continue;
            BlockPos pos = new BlockPos(cx * 16 + (i & 15), sectionY * 16 + (i >> 8), cz * 16 + ((i >> 4) & 15));
            if (!deEntities.contains(pos.asLong())) positions.add(pos.asLong());
        }
        if (positions.isEmpty()) return;
        CompoundTag record = ((CompoundTag) section.packed.palette.get(slot)).copy();
        record.putLongArray("positions", positions.stream().mapToLong(Long::longValue).toArray());
        ctx.parking.park("refunded_block", where + " section " + sectionY, record.getString("Name"), record);
        ctx.report.count("blocks refunded", positions.size());
        String item = section.converted.get(slot).refundItem();
        if (!item.isEmpty()) handBack(BlockPos.of(positions.get(0)), List.of(item(item, positions.size())),
                "no Continuum block for " + record.getString("Name"), where);
    }

    /** Items for a chest where the block stood; an airship's plot is no place in the world, so its items are parked. */
    private void handBack(BlockPos pos, List<CompoundTag> items, String why, String where) {
        if (!plot) {
            if (!items.isEmpty()) refunds.add(new Refund(pos, items, why));
            return;
        }
        for (CompoundTag item : items) ctx.parking.park("item", where + " " + pos.toShortString() + " (" + why + ")", item.getString("id"), item);
    }

    /**
     * A refund's chests, written into the converted chunk where its block stood and upward through blocks a chest may
     * replace, so they are saved with the chunk itself: never lost to a crash, never placed twice. A stack that is no
     * item here, or finds no room below the chunk's top section, is parked.
     */
    private void placeRefund(CompoundTag chunk, ListTag beList, Set<Long> taken, Refund refund, String where) {
        String at = where + " refund at " + refund.pos.toShortString();
        List<CompoundTag> stacks = new ArrayList<>();
        for (CompoundTag item : refund.items) {
            int max = ctx.target.maxStackSize(item.getString("id"));
            List<String> bad = ctx.target.undecodable(item);
            if (max <= 0 || bad == null || !bad.isEmpty()) {
                ctx.parking.park("item", at, item.getString("id"), item);
                continue;
            }
            for (int left = Math.max(1, item.getInt("count")); left > 0; left -= max) {
                CompoundTag stack = item.copy();
                stack.putInt("count", Math.min(left, max));
                stacks.add(stack);
            }
        }
        BlockPos pos = refund.pos;
        int next = 0;
        while (next < stacks.size()) {
            CompoundTag there;
            while ((there = ChunkBlocks.get(chunk, pos)) != null && (taken.contains(pos.asLong()) || !ctx.target.canHoldRefund(there))) pos = pos.above();
            if (there == null) break;
            ChunkBlocks.set(chunk, pos, ctx.target.refundChest());
            ListTag items = new ListTag();
            for (int slot = 0; slot < CHEST_SLOTS && next < stacks.size(); slot++) {
                CompoundTag stack = stacks.get(next++).copy();
                stack.putByte("Slot", (byte) slot);
                items.add(stack);
            }
            CompoundTag chest = new CompoundTag();
            chest.putString("id", "minecraft:chest");
            chest.putInt("x", pos.getX());
            chest.putInt("y", pos.getY());
            chest.putInt("z", pos.getZ());
            chest.put("Items", items);
            beList.add(chest);
            taken.add(pos.asLong());
            pos = pos.above();
        }
        for (CompoundTag stack : stacks.subList(next, stacks.size()))
            ctx.parking.park("item", at + " (no room for a chest)", stack.getString("id"), stack);
        ctx.report.line("refund at " + dim + " " + refund.pos.toShortString() + ": " + refund.items.size() + " stack(s), " + refund.why);
        ctx.report.count("refund stacks", next);
    }

    private static CompoundTag item(String id, int count) {
        CompoundTag item = new CompoundTag();
        item.putString("id", id);
        item.putInt("count", count);
        return item;
    }

    private static Map<Long, String> ductFluids(CompoundTag chunk) {
        Map<Long, String> out = new HashMap<>();
        CompoundTag ducts = chunk.getCompound("hbm_duct_fluids");
        long[] positions = ducts.getLongArray("pos");
        ListTag fluids = ducts.getList("fluid", Tag.TAG_STRING);
        for (int i = 0; i < positions.length && i < fluids.size(); i++) out.put(positions[i], fluids.getString(i));
        return out;
    }

    /**
     * A scheduled tick names the block it is for: the block its position converted to, which keeps the state a
     * property-dependent rename picks (a lit lamp), else what the bare id converts to.
     */
    private void renameTicks(ListTag ticks, Map<Integer, Section> sections) {
        for (int i = ticks.size() - 1; i >= 0; i--) {
            CompoundTag tick = ticks.getCompound(i);
            String id = tick.getString("i");
            if (!source.test(id)) continue;
            String name = null;
            Section section = sections.get(tick.getInt("y") >> 4);
            if (section != null) {
                int slot = section.packed.index[PackedSection.indexOf(tick.getInt("x"), tick.getInt("y"), tick.getInt("z"))];
                if (((CompoundTag) section.packed.palette.get(slot)).getString("Name").equals(id))
                    name = section.converted.get(slot).entry().getString("Name");
            }
            if (name == null) {
                CompoundTag probe = new CompoundTag();
                probe.putString("Name", id);
                name = BlockStates.convert(probe, ctx.target).entry().getString("Name");
            }
            if (name.equals("minecraft:air")) ticks.remove(i);
            else tick.putString("i", name);
        }
    }

    /**
     * Structure starts and references by Continuum's names, pieces included; a start or reference Continuum cannot
     * load (vanilla drops it) is parked. The placed blocks stay either way.
     */
    private void structures(CompoundTag structures, String where) {
        for (String group : List.of("starts", "References")) {
            CompoundTag map = structures.getCompound(group);
            for (String key : new ArrayList<>(map.getAllKeys())) {
                // an addon's "park" row keeps DE's name, which no game knows, so the start is parked under it
                String mapped = STRUCTURES.getOrDefault(key, WorldTables.get().structures.getOrDefault(key, DeAddons.rename(key)));
                String to = mapped.equals("park") ? key : mapped;
                Tag value = map.get(key);
                map.remove(key);
                if (value instanceof CompoundTag start && start.contains("id")) {
                    start.put("id", StringTag.valueOf(to));
                    for (Tag t : start.getList("Children", Tag.TAG_COMPOUND)) {
                        CompoundTag piece = (CompoundTag) t;
                        piece.putString("id", PIECES.getOrDefault(piece.getString("id"), piece.getString("id")));
                    }
                }
                if (!loadable(to, value, where)) {
                    ctx.parking.park("structure", where + " " + group, to, value);
                    ctx.report.count("structure records parked");
                    continue;
                }
                map.put(to, value);
            }
        }
    }

    /** As vanilla loads a start: a piece it cannot load is dropped (here parked), and the start stays while any is left. */
    private boolean loadable(String structure, Tag value, String where) {
        if (!ctx.target.hasStructure(structure)) return false;
        if (!(value instanceof CompoundTag start) || start.getList("Children", Tag.TAG_COMPOUND).isEmpty()) return true;
        ListTag children = start.getList("Children", Tag.TAG_COMPOUND);
        for (int i = children.size() - 1; i >= 0; i--) {
            if (ctx.target.loadsPiece(children.getCompound(i))) continue;
            ctx.parking.park("structure_piece", where, structure, children.remove(i));
            ctx.report.count("structure pieces parked");
        }
        return !children.isEmpty();
    }

    /**
     * DE's per-chunk record of conductors it generated, which Continuum rebuilds from the blocks; any other HBM chunk
     * attachment has no Continuum reader and is parked.
     */
    private void chunkAttachments(CompoundTag chunk, String where) {
        CompoundTag attachments = chunk.getCompound("neoforge:attachments");
        attachments.remove("hbm:generated_conductors");
        for (String key : List.copyOf(attachments.getAllKeys())) {
            if (!DeAddons.isSource(key)) continue;
            ctx.parking.park("chunk_attachment", where, key, attachments.get(key));
            attachments.remove(key);
        }
        if (attachments.isEmpty()) chunk.remove("neoforge:attachments");
    }

    /** A section's biomes by Continuum's names: an addon's DE biomes have its namespace. */
    private void biomes(CompoundTag biomes) {
        ListTag palette = biomes.getList("palette", Tag.TAG_STRING);
        for (int i = 0; i < palette.size(); i++) if (source.test(palette.getString(i))) palette.set(i, StringTag.valueOf(biome(palette.getString(i))));
    }

    public static String biome(String id) {
        return DeAddons.isSource(id) ? WorldTables.get().biomes.getOrDefault(id, DeAddons.rename(id)) : id;
    }

    private final class Section {
        final CompoundTag tag;
        final PackedSection packed;
        final List<BlockStates.Converted> converted = new ArrayList<>();
        /** Per palette slot: converted by this converter, so its block entity or core is this run's to make. */
        final List<Boolean> ours = new ArrayList<>();

        Section(CompoundTag section, String where) {
            biomes(section.getCompound("biomes"));
            this.tag = section.getCompound("block_states");
            this.packed = new PackedSection(tag);
            for (Tag entry : packed.palette) {
                boolean taken = source.test(((CompoundTag) entry).getString("Name"));
                BlockStates.Converted c = taken ? BlockStates.convert((CompoundTag) entry, ctx.target)
                        : new BlockStates.Converted((CompoundTag) entry, BlockStates.Kind.PLAIN, "");
                converted.add(c);
                ours.add(taken || !late);
                CompoundTag lost = c.dropped().copy();
                String deBlock = ((CompoundTag) entry).getString("Name");
                for (String key : List.copyOf(lost.getAllKeys())) {
                    if (DERIVED_PROPERTIES.contains(key) || STATE_TO_ENTITY.getOrDefault(deBlock, Map.of()).containsKey(key)) lost.remove(key);
                }
                if (!lost.isEmpty()) {
                    ctx.parking.park("block_state", where + " section " + section.getByte("Y"), deBlock, lost);
                    ctx.report.count("block state properties parked", lost.size());
                }
                if (c.kind() == BlockStates.Kind.CELL) ctx.report.count("multiblock cells cleared");
            }
        }

        void store() {
            ListTag palette = new ListTag();
            for (BlockStates.Converted c : converted) palette.add(c.entry());
            packed.palette.clear();
            packed.palette.addAll(palette);
            packed.store(tag);
        }
    }
}
