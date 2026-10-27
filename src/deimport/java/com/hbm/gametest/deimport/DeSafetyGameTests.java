package com.hbm.gametest.deimport;

import com.hbm.Constants;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.RegionIo;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.gate.Signature;
import com.hbm.deimport.source.de7.Airships;
import com.hbm.deimport.source.de7.ChunkConverter;
import com.hbm.deimport.steps.LiveStep;
import com.hbm.deimport.steps.RbmkDialsStep;
import com.hbm.deimport.steps.SavedDataStep;
import com.hbm.deimport.steps.SubLevelsStep;
import com.hbm.delegacy.legacy.LegacyStacks;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dev.golem.core.gametest.GameTests;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.jetbrains.annotations.Nullable;

/** Other mods' data, stores converted in parallel, the gate's edge cases and what large or long imports need. */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class DeSafetyGameTests {
    static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    static {
        TESTS.function("deimport_foreign_block_entities_kept", 20, () -> DeSafetyGameTests::foreignKept);
        TESTS.function("deimport_region_scratch_per_file", 20, () -> DeSafetyGameTests::scratchPerFile);
        TESTS.function("deimport_gate_finds_de_files", 20, () -> DeSafetyGameTests::gateFindsDeFiles);
        TESTS.function("deimport_airship_cells_indexed", 40, () -> DeSafetyGameTests::airshipCellsIndexed);
        TESTS.function("deimport_refunds_written_into_the_chunk", 20, () -> DeSafetyGameTests::refundsInChunk);
        TESTS.function("deimport_damaged_state_file", 20, () -> DeSafetyGameTests::damagedStateFile);
        TESTS.function("deimport_many_records_quickly", 400, () -> DeSafetyGameTests::manyRecords);
        TESTS.function("deimport_rbmk_settings_saved", 40, () -> DeSafetyGameTests::rbmkSaved);
        TESTS.function("deimport_power_graph_moved_aside", 20, () -> DeSafetyGameTests::powerGraphAside);
        TESTS.function("deimport_legacy_counts_over_99", 20, () -> DeSafetyGameTests::legacyBigCounts);
        TESTS.function("deimport_unreadable_airship_file", 20, () -> DeSafetyGameTests::unreadableAirships);
        TESTS.function("deimport_ticks_named_by_position", 20, () -> DeSafetyGameTests::ticksByPosition);
    }

    private DeSafetyGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        GameTests.register(event, TESTS);
    }

    /** A block entity of a mod that is not installed, and a moving piston's, stay in the chunk. */
    private static void foreignKept(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            BlockPos widget = new BlockPos(1, 70, 1), piston = new BlockPos(3, 70, 1);
            DeFixtures.setBlock(chunk, widget, DeFixtures.block("othermod:widget"));
            CompoundTag foreign = DeFixtures.blockEntity("othermod:widget", widget);
            foreign.putInt("charge", 5);
            DeFixtures.setBlock(chunk, piston, DeFixtures.block("minecraft:moving_piston"));
            CompoundTag moving = DeFixtures.blockEntity("minecraft:piston", piston);
            moving.put("blockState", DeFixtures.block("hbm:concrete_black"));
            DeFixtures.addBlockEntity(chunk, foreign);
            DeFixtures.addBlockEntity(chunk, moving);
            CompoundTag out = new ChunkConverter(DeImportGameTests.ctx(world, helper.getLevel().getServer()), "minecraft:overworld").convert(chunk);
            Map<BlockPos, CompoundTag> bes = blockEntities(out);
            helper.assertTrue(bes.containsKey(widget) && bes.get(widget).getInt("charge") == 5, "the missing mod's block entity stays: " + bes);
            helper.assertTrue(bes.containsKey(piston) && bes.get(piston).getCompound("blockState").getString("Name").equals("hbm:concrete_colored"),
                    "the moving piston keeps its block entity, its moving block converted: " + bes.get(piston));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** One region file's oversized chunk moves in only with its own file, not with another file's swap. */
    private static void scratchPerFile(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Path folder = world.resolve("region");
            Files.createDirectories(folder);
            Path big = folder.resolve("r.0.0.mca"), small = folder.resolve("r.1.0.mca");
            CompoundTag oversized = DeFixtures.emptyChunk(0, 0);
            byte[] noise = new byte[1_200_000];
            new Random(1).nextBytes(noise);
            oversized.putByteArray("noise", noise);
            write(big, new ChunkPos(0, 0), oversized);
            write(small, new ChunkPos(32, 0), DeFixtures.emptyChunk(32, 0));
            helper.assertTrue(Files.exists(folder.resolve("c.0.0.mcc")), "the fixture chunk is stored outside its region file");
            try {
                RegionIo.rewrite(big, hooks(true));
                helper.fail("the interrupted rewrite throws");
            } catch (IOException crash) {
                // the crash this test plants before the first file's swap
            }
            RegionIo.rewrite(small, hooks(false));
            try (RegionFile region = RegionIo.open(big)) {
                CompoundTag still = RegionIo.read(region, new ChunkPos(0, 0));
                helper.assertTrue(still != null && still.contains(ChunkConverter.STAMP),
                        "the first file's oversized chunk is still DE's after the second file's swap");
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** DE's doubly wrapped saved data, or a stamped spawn chunk behind unstamped ones, marks a world as DE's. */
    private static void gateFindsDeFiles(GameTestHelper helper) {
        Path wrapped = DeImportGameTests.tempWorld(), spawned = DeImportGameTests.tempWorld();
        try {
            CompoundTag inner = new CompoundTag();
            inner.put("entries", new ListTag());
            CompoundTag outer = new CompoundTag();
            outer.put("data", inner);
            CompoundTag root = new CompoundTag();
            root.put("data", outer);
            Files.createDirectories(wrapped.resolve("data"));
            NbtIo.writeCompressed(root, wrapped.resolve("data/hbm_pollution.dat"));
            helper.assertTrue(Signature.check(wrapped, null).de(), "DE's wrapped saved data");

            CompoundTag level = new CompoundTag(), data = new CompoundTag();
            data.putInt("SpawnX", 80);
            data.putInt("SpawnZ", 80);
            level.put("Data", data);
            NbtIo.writeCompressed(level, spawned.resolve("level.dat"));
            Path region = spawned.resolve("region/r.0.0.mca");
            Files.createDirectories(region.getParent());
            for (int x = 0; x < 4; x++) {
                CompoundTag plain = DeFixtures.emptyChunk(x, 0);
                plain.remove(ChunkConverter.STAMP);
                write(region, new ChunkPos(x, 0), plain);
            }
            write(region, new ChunkPos(5, 5), DeFixtures.emptyChunk(5, 5));
            helper.assertTrue(Signature.check(spawned, null).de(), "the stamped spawn chunk, past the chunks sampled first");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(wrapped);
            DeImportGameTests.delete(spawned);
        }
        helper.succeed();
    }

    /** A machine on an airship gets its cells in the level's core index, which the cells find it through. */
    private static void airshipCellsIndexed(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        ServerLevel level = helper.getLevel();
        Ctx ctx = null;
        try {
            DeFixtures.writeKey(world);
            BlockPos origin = new BlockPos(16_000_002, 70, 16_000_002);
            CompoundTag chunk = DeFixtures.emptyChunk(1_000_000, 1_000_000);
            DeFixtures.standardContent(chunk, origin);
            CompoundTag chunks = new CompoundTag();
            chunks.put(Long.toString(new ChunkPos(origin).toLong()), chunk);
            CompoundTag plot = new CompoundTag();
            plot.put("chunks", chunks);
            CompoundTag ship = new CompoundTag();
            ship.put("plot", plot);
            ctx = DeImportGameTests.ctx(world, level.getServer());
            Airships.convert(ctx, level.dimension().location().toString(), ship, "test ship");
            CompoundTag centrifuge = ctx.plotCells.stream().filter(t -> t.getLong("pos") == origin.asLong()).findFirst().orElse(null);
            helper.assertTrue(centrifuge != null && centrifuge.getLongArray("cells").length > 0, "the centrifuge's cells are listed: " + ctx.plotCells);
            new LiveStep().run(ctx, level.getServer());
            long cell = centrifuge.getLongArray("cells")[0];
            helper.assertTrue(MultiblockCoreIndex.lookup(level, BlockPos.of(cell)) == origin.asLong(), "its cell points at the core");
            helper.assertTrue(!level.hasChunk(origin.getX() >> 4, origin.getZ() >> 4), "without loading the ship's plot");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (ctx != null) {
                for (CompoundTag record : ctx.plotCells)
                    for (long cell : record.getLongArray("cells")) MultiblockCoreIndex.remove(level, BlockPos.of(cell));
                for (CompoundTag record : ctx.conductors) PowerGraph.get(level).removeNode(record.getLong("pos"));
            }
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A refunded machine's stacks are written into the chunk as chests, split to stack size, the next chest above. */
    private static void refundsInChunk(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            BlockPos turbine = new BlockPos(2, 70, 2);
            DeFixtures.setBlock(chunk, turbine, DeFixtures.block("hbm:machine_turbine"));
            CompoundTag[] slots = new CompoundTag[31];
            for (int i = 0; i < 30; i++) slots[i] = DeFixtures.slot(i, DeFixtures.stack("minecraft:dirt", 1));
            slots[30] = DeFixtures.slot(30, DeFixtures.stack("minecraft:stone", 200));
            CompoundTag be = DeFixtures.blockEntity("hbm:turbine", turbine);
            be.put("inventory", DeFixtures.inventory(slots));
            DeFixtures.addBlockEntity(chunk, be);
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            CompoundTag out = new ChunkConverter(ctx, "minecraft:overworld").convert(chunk);
            Map<BlockPos, CompoundTag> bes = blockEntities(out);
            int stone = 0, biggest = 0;
            for (BlockPos pos : List.of(turbine, turbine.above())) {
                CompoundTag chest = bes.get(pos);
                helper.assertTrue(chest != null && chest.getString("id").equals("minecraft:chest"), "a chest at " + pos.toShortString() + ": " + chest);
                for (Tag t : chest.getList("Items", Tag.TAG_COMPOUND)) {
                    CompoundTag stack = (CompoundTag) t;
                    biggest = Math.max(biggest, stack.getInt("count"));
                    if (stack.getString("id").equals("minecraft:stone")) stone += stack.getInt("count");
                }
            }
            helper.assertTrue(stone == 200 && biggest <= 64, "all the stone, in stacks of at most 64: " + stone + ", " + biggest);
            helper.assertTrue(ctx.cores.isEmpty() && ctx.conductors.isEmpty(), "nothing is left for the live phase");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A damaged state file is set aside and the copy saved before it read; with neither, the world has no state. */
    private static void damagedStateFile(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            ImportState state = ImportState.create(world, "1.0.0-beta.12", 7, "test");
            state.status("partial");
            state.save();
            state.save();
            Files.writeString(world.resolve(ImportState.FILE), "{");
            ImportState read = ImportState.read(world);
            helper.assertTrue(read != null && read.status().equals("partial"), "the previous copy is read");
            helper.assertTrue(damaged(world, ImportState.FILE), "the damaged file is kept aside");
            Files.writeString(world.resolve(ImportState.FILE), "{");
            Files.delete(world.resolve(ImportState.PREV));
            helper.assertTrue(ImportState.read(world) == null, "with neither copy readable, the gate decides again");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Many records and leftovers: each live record finds its own earlier parks without a scan of every record. */
    private static void manyRecords(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, server);
            for (int i = 0; i < 100_000; i++) ctx.parking.park("item", "test " + i, "hbm:x", null);
            for (int i = 0; i < 20_000; i++) {
                CompoundTag pile = new CompoundTag();
                pile.putString("dim", "minecraft:overworld");
                pile.putLong("pos", i);
                ctx.add("piles", pile);
            }
            long start = System.nanoTime();
            new LiveStep().run(ctx, server);
            long seconds = (System.nanoTime() - start) / 1_000_000_000L;
            helper.assertTrue(seconds < 15, "20,000 records over 100,000 leftovers took " + seconds + " s");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** RBMK settings are written to the overworld's saved data at once, since the step is done before any autosave. */
    private static void rbmkSaved(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        Path rules = server.getWorldPath(LevelResource.ROOT).resolve("data/golemcore_world_rules.dat");
        try {
            Path file = world.resolve("datapacks/tweaks/data/hbm/hbm/machine_config/rbmk.json");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{\"column_height\": 5}");
            Files.deleteIfExists(rules);
            new RbmkDialsStep().run(DeImportGameTests.ctx(world, server), server);
            CompoundTag saved = Files.isRegularFile(rules) ? NbtIo.readCompressed(rules, NbtAccounter.unlimitedHeap()) : null;
            helper.assertTrue(saved != null && saved.toString().contains("dialColumnHeight"), "the setting is on disk: " + saved);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            RBMKDials.COLUMN_HEIGHT.reset(server);
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** DE's power graph moves out of Continuum's way and still gives later starts DE's pylon wires. */
    private static void powerGraphAside(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            CompoundTag node = new CompoundTag();
            node.putLong("p", 7L);
            ListTag links = new ListTag();
            links.add(LongTag.valueOf(9L));
            node.put("r", links);
            ListTag nodes = new ListTag();
            nodes.add(node);
            CompoundTag graph = new CompoundTag();
            graph.put("nodes", nodes);
            CompoundTag wrapper = new CompoundTag();
            wrapper.put("data", graph);
            CompoundTag root = new CompoundTag();
            root.put("data", wrapper);
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(root, world.resolve("data/hbm_power_graph.dat"));
            new SavedDataStep().run(DeImportGameTests.ctx(world, null), null);
            helper.assertTrue(Files.isRegularFile(world.resolve(Ctx.DE_POWER_GRAPH)) && !Files.exists(world.resolve("data/hbm_power_graph.dat")),
                    "DE's graph is moved aside");
            long[] wires = DeImportGameTests.ctx(world, null).wires("minecraft:overworld").get(7L);
            helper.assertTrue(wires != null && wires.length == 1 && wires[0] == 9L, "a later start still reads its wires");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A storage mod's stack above 99 of a DE item converts and keeps its count. */
    private static void legacyBigCounts(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Path file = server.getWorldPath(LevelResource.ROOT).resolve(LegacyStacks.LEFTOVERS);
        try {
            CompoundTag tag = DeFixtures.stack("hbm:canister_empty", 1);
            CompoundTag components = new CompoundTag();
            components.putInt("hbm:b92_animation", 3);
            tag.put("components", components);
            ItemStack legacy = ItemStack.parse(server.registryAccess(), tag).orElse(ItemStack.EMPTY);
            legacy.setCount(1024);
            ItemStack out = LegacyStacks.convert(legacy, server, "test");
            helper.assertTrue(out.getCount() == 1024 && !LegacyStacks.isLegacy(out), "converted with its count: " + out);
        } finally {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                // a leftover test file in the GameTest world changes no test
            }
        }
        helper.succeed();
    }

    /**
     * An airship storage file Sable cannot open is reported and left as it is; the world still loads. Sable reads a
     * damaged file's header leniently, so the test stands in a folder where the file should be.
     */
    private static void unreadableAirships(GameTestHelper helper) {
        if (!ModList.get().isLoaded("sable")) {
            helper.succeed();
            return;
        }
        Path world = DeImportGameTests.tempWorld();
        try {
            Path file = world.resolve("sublevels/r.0.0.0.slvls");
            Files.createDirectories(file);
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            new SubLevelsStep().run(ctx, helper.getLevel().getServer());
            helper.assertTrue(!ctx.journal.done("sublevels@1", world, file), "the file is tried again on the next start");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A scheduled tick names the block at its position, so a lit lamp's tick names the lit lamp. */
    private static void ticksByPosition(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            BlockPos lamp = new BlockPos(1, 70, 1);
            DeFixtures.setBlock(chunk, lamp, DeFixtures.block("hbm:reinforced_lamp", "lit", "true"));
            CompoundTag tick = new CompoundTag();
            tick.putString("i", "hbm:reinforced_lamp");
            tick.putInt("x", lamp.getX());
            tick.putInt("y", lamp.getY());
            tick.putInt("z", lamp.getZ());
            tick.putInt("t", 2);
            tick.putInt("p", 0);
            ListTag ticks = new ListTag();
            ticks.add(tick);
            chunk.put("block_ticks", ticks);
            CompoundTag out = new ChunkConverter(DeImportGameTests.ctx(world, helper.getLevel().getServer()), "minecraft:overworld").convert(chunk);
            String name = out.getList("block_ticks", Tag.TAG_COMPOUND).getCompound(0).getString("i");
            helper.assertTrue(name.equals("hbm:reinforced_lamp_on"), "the tick names the lit lamp: " + name);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    private static Map<BlockPos, CompoundTag> blockEntities(CompoundTag chunk) {
        Map<BlockPos, CompoundTag> out = new HashMap<>();
        for (Tag t : chunk.getList("block_entities", Tag.TAG_COMPOUND)) {
            CompoundTag be = (CompoundTag) t;
            out.put(new BlockPos(be.getInt("x"), be.getInt("y"), be.getInt("z")), be);
        }
        return out;
    }

    private static void write(Path file, ChunkPos pos, CompoundTag chunk) throws IOException {
        try (RegionFile region = RegionIo.open(file); DataOutputStream out = region.getChunkDataOutputStream(pos)) {
            NbtIo.write(chunk, out);
        }
    }

    /** Hooks that convert by dropping the stamp; {@code crash} throws before the swap, as a killed server would stop. */
    private static RegionIo.Hooks hooks(boolean crash) {
        return new RegionIo.Hooks() {
            @Override
            public @Nullable CompoundTag convert(ChunkPos pos, CompoundTag chunk) {
                chunk.remove(ChunkConverter.STAMP);
                return chunk;
            }

            @Override
            public void damaged(ChunkPos pos, Tag data, String why) {}

            @Override
            public void beforeSwap() throws IOException {
                if (crash) throw new IOException("killed before the swap");
            }
        };
    }

    private static boolean damaged(Path world, String name) throws IOException {
        try (Stream<Path> files = Files.list(world)) {
            return files.anyMatch(p -> p.getFileName().toString().startsWith(name + ".damaged-"));
        }
    }
}
