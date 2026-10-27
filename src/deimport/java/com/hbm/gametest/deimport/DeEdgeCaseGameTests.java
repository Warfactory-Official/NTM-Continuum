package com.hbm.gametest.deimport;

import com.hbm.Constants;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.ImportedCells;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.deimport.DeImport;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Datapacks;
import com.hbm.deimport.engine.Parking;
import com.hbm.deimport.engine.Pipeline;
import com.hbm.deimport.engine.RegionIo;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.gate.Signature;
import com.hbm.deimport.source.de7.Airships;
import com.hbm.deimport.source.de7.BlockEntities;
import com.hbm.deimport.source.de7.ChunkConverter;
import com.hbm.deimport.steps.LiveStep;
import com.hbm.deimport.steps.PlayersStep;
import com.hbm.deimport.steps.RegionsStep;
import com.hbm.deimport.steps.Steps;
import com.hbm.deimport.target.ContinuumTarget;
import com.hbm.delegacy.de7.Walker;
import dev.golem.core.gametest.GameTests;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.jetbrains.annotations.Nullable;

/** Crash points between a store and its records, live work done twice, and the stores and data a first pass missed. */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class DeEdgeCaseGameTests {
    static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    static {
        TESTS.function("deimport_live_saves_its_levels", 40, () -> DeEdgeCaseGameTests::liveSavesItsLevels);
        TESTS.function("deimport_live_records_repeat_safely", 60, () -> DeEdgeCaseGameTests::liveRecordsRepeat);
        TESTS.function("deimport_live_parks_once", 40, () -> DeEdgeCaseGameTests::liveParksOnce);
        TESTS.function("deimport_host_player_written", 20, () -> DeEdgeCaseGameTests::hostPlayerWritten);
        TESTS.function("deimport_rbmk_settings_before_rebuild", 40, () -> DeEdgeCaseGameTests::rbmkBeforeRebuild);
        TESTS.function("deimport_failed_chunk_parked", 20, () -> DeEdgeCaseGameTests::failedChunkParked);
        TESTS.function("deimport_parts_keep_their_records", 20, () -> DeEdgeCaseGameTests::partsKeepRecords);
        TESTS.function("deimport_live_failure_parked", 40, () -> DeEdgeCaseGameTests::liveFailureParked);
        TESTS.function("deimport_step_versions_not_rerun", 20, () -> DeEdgeCaseGameTests::stepVersions);
        TESTS.function("deimport_reopened_by_de_warns", 20, () -> DeEdgeCaseGameTests::reopenedByDe);
        TESTS.function("deimport_unstamped_de_chunks", 20, () -> DeEdgeCaseGameTests::unstampedChunks);
        TESTS.function("deimport_heat_scaled", 20, () -> DeEdgeCaseGameTests::heatScaled);
        TESTS.function("deimport_player_backups_converted", 20, () -> DeEdgeCaseGameTests::playerBackups);
        TESTS.function("deimport_header_refused_chunk_parked", 20, () -> DeEdgeCaseGameTests::headerRefusedChunk);
        TESTS.function("deimport_global_shard_only_on_change", 20, () -> DeEdgeCaseGameTests::globalOnlyOnChange);
        TESTS.function("deimport_datapack_order", 20, () -> DeEdgeCaseGameTests::datapackOrder);
        TESTS.function("deimport_airship_cables_piles_pads", 40, () -> DeEdgeCaseGameTests::airshipLinks);
        TESTS.function("deimport_shard_names_distinct", 20, () -> DeEdgeCaseGameTests::shardNames);
        TESTS.function("deimport_waiting_work_survives_finish", 40, () -> DeEdgeCaseGameTests::waitingSurvives);
        TESTS.function("deimport_foreign_block_entities_untouched", 20, () -> DeEdgeCaseGameTests::foreignUntouched);
    }

    private DeEdgeCaseGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        GameTests.register(event, TESTS);
    }

    /** What the live phase placed is in the level's saved chunks before its records are dropped. */
    private static void liveSavesItsLevels(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        try {
            Ctx ctx = DeImportGameTests.ctx(world, level.getServer());
            level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
            ctx.add("loot", DeImportGameTests.loot(level, pos));
            new LiveStep().run(ctx, level.getServer());
            Optional<CompoundTag> saved = level.getChunkSource().chunkMap.read(new ChunkPos(pos)).join();
            helper.assertTrue(saved.isPresent() && saved.get().toString().contains("hbm_de_live"),
                    "the rolled chest is in the saved chunk before the records go");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Records run again after a crash roll no second loot and adopt their own cells. */
    private static void liveRecordsRepeat(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        BlockPos lootAt = helper.absolutePos(new BlockPos(3, 1, 1)), fresh = helper.absolutePos(new BlockPos(5, 1, 1));
        try {
            level.setBlockAndUpdate(lootAt, Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(fresh, Blocks.CHEST.defaultBlockState());
            for (int run = 0; run < 2; run++) {
                Ctx ctx = DeImportGameTests.ctx(world, server);
                ctx.add("loot", DeImportGameTests.loot(level, lootAt));
                new LiveStep().run(ctx, server);
            }
            Ctx once = DeImportGameTests.ctx(DeImportGameTests.tempWorld(), server);
            CompoundTag single = DeImportGameTests.loot(level, lootAt);
            single.putLong("pos", fresh.asLong());
            once.add("loot", single);
            new LiveStep().run(once, server);
            int stacks = DeImportGameTests.stacks(level, lootAt), expected = DeImportGameTests.stacks(level, fresh);
            helper.assertTrue(expected > 0 && stacks == expected, "the loot rolled once: " + stacks + " stacks, one roll gives " + expected);
            DeImportGameTests.delete(once.world);

            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("hbm:machine_chemical_plant"));
            BlockMultiblockCore machine = (BlockMultiblockCore) block;
            BlockPos core = helper.absolutePos(new BlockPos(3, 2, 5));
            BlockState state = machine.defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH);
            level.setBlock(core, state, Block.UPDATE_CLIENTS);
            ImportedCells.Result first = ImportedCells.rebuild(level, core);
            List<BlockPos> cells = new ArrayList<>(ImportedCells.footprint(machine, core, Direction.NORTH).keySet());
            for (BlockPos cell : cells) MultiblockCoreIndex.remove(level, cell);
            ImportedCells.Result again = ImportedCells.rebuild(level, core);
            helper.assertTrue(again.blocked() == 0 && again.placed() == first.placed(),
                    "cells whose index was lost with a crash are the machine's own: " + again);
            for (BlockPos cell : cells)
                helper.assertTrue(MultiblockCoreIndex.lookup(level, cell) == core.asLong(), "indexed again " + cell.toShortString());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            level.setBlockAndUpdate(lootAt, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(fresh, Blocks.AIR.defaultBlockState());
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A live record that parked before a crash parks the same fields once when it runs again. */
    private static void liveParksOnce(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, server);
            for (String where : List.of("first", "second")) {
                CompoundTag entity = DeImportGameTests.entity("minecraft:no_such_entity");
                entity.putInt("stored", 42);
                ctx.entityConverted(entity, where);
            }
            new LiveStep().slice(ctx, server, 0L);
            Ctx restarted = DeImportGameTests.ctx(world, server);
            new LiveStep().run(restarted, server);
            long first = new Parking(world).records().stream()
                    .filter(t -> ((CompoundTag) t).getString("where").startsWith("first ")).count();
            long second = new Parking(world).records().stream()
                    .filter(t -> ((CompoundTag) t).getString("where").startsWith("second ")).count();
            helper.assertTrue(first > 0 && first == second, "each entity's fields parked once: " + first + " and " + second);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** The singleplayer host is written into level.dat converted, before the world loads. */
    private static void hostPlayerWritten(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            CompoundTag root = new CompoundTag();
            CompoundTag data = new CompoundTag();
            data.putString("LevelName", "test");
            root.put("Data", data);
            NbtIo.writeCompressed(root, world.resolve("level.dat"));
            CompoundTag host = new CompoundTag();
            DeFixtures.dePlayer(host);
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            PlayersStep.host(ctx, new Walker(ctx), host);
            CompoundTag written = NbtIo.readCompressed(world.resolve("level.dat"), NbtAccounter.unlimitedHeap()).getCompound("Data").getCompound("Player");
            helper.assertTrue(written.getBoolean("hbm_de_converted") && !written.toString().contains("hbm:cell_uf6"),
                    "level.dat holds the converted host: " + written);
            helper.assertTrue(host.getBoolean("hbm_de_converted") && host.equals(written), "the server's own copy is the same");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** The other live steps run before the rebuild starts; an airship's RBMK column takes DE's height. */
    private static void rbmkBeforeRebuild(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        try {
            Ctx ctx = DeImportGameTests.ctx(world, level.getServer());
            level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
            ctx.add("loot", DeImportGameTests.loot(level, pos));
            LiveStep live = new LiveStep();
            AtomicBoolean sawLoot = new AtomicBoolean(true);
            Step before = new Step() {
                @Override
                public String id() {
                    return "before@1";
                }

                @Override
                public Phase phase() {
                    return Phase.LIVE;
                }

                @Override
                public boolean applies(Ctx ctx) {
                    return true;
                }

                @Override
                public boolean run(Ctx ctx, @Nullable MinecraftServer server) {
                    sawLoot.set(DeImportGameTests.stacks(level, pos) > 0);
                    return true;
                }
            };
            ImportState state = ImportState.create(world, "1.0.0-beta.12", 7, "test");
            DeImport.Run run = new DeImport.Run(world, state, ctx, new Pipeline(List.of(before, live), id -> true), live);
            helper.assertTrue(run.advance(level.getServer(), Long.MAX_VALUE), "the run finishes");
            helper.assertTrue(!sawLoot.get() && DeImportGameTests.stacks(level, pos) > 0, "the step before the live work ran first");
            Map<Long, CompoundTag> column = new ContinuumTarget(level.getServer()).footprint("hbm:rbmk_blank", new CompoundTag(), 0L, 6);
            helper.assertTrue(column.size() == 5 && column.get(BlockPos.ZERO.above(5).asLong()).toString().contains("top"),
                    "a column six high: " + column);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A chunk the converter throws on keeps its park even when no other chunk in the file changed. */
    private static void failedChunkParked(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Path file = regionWith(world, List.of(new ChunkPos(0, 0)));
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            RegionsStep.rewrite(ctx, "terrain test r.0.0.mca", "test", file, chunk -> {
                ctx.core("minecraft:overworld", 5L, "hbm:machine_centrifuge");
                throw new IllegalStateException("converter failed");
            });
            Ctx restarted = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            long chunks = restarted.parking.records().stream().filter(t -> ((CompoundTag) t).getString("kind").equals("chunk")).count();
            helper.assertTrue(chunks == 1 && restarted.cores.isEmpty(),
                    "the chunk is parked and what it recorded before failing is gone: " + restarted.parking.records() + " " + restarted.cores);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Converting some parts of a store again replaces only their records; the parts passed over keep theirs. */
    private static void partsKeepRecords(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, server);
            Unit unit = ctx.begin("airships test");
            unit.part("0");
            ctx.core("minecraft:overworld", 1L, "hbm:machine_centrifuge");
            ctx.parking.park("item", "ship 0", "hbm:x", null);
            unit.endPart(true);
            unit.part("1");
            ctx.core("minecraft:overworld", 2L, "hbm:machine_centrifuge");
            unit.endPart(true);
            ctx.commit(unit);

            Ctx restarted = DeImportGameTests.ctx(world, server);
            Unit again = restarted.begin("airships test");
            again.part("0");
            again.endPart(false);
            again.part("1");
            restarted.core("minecraft:overworld", 3L, "hbm:machine_centrifuge");
            again.endPart(true);
            restarted.commit(again);

            Ctx third = DeImportGameTests.ctx(world, server);
            List<Long> cores = third.cores.stream().map(t -> t.getLong("pos")).sorted().toList();
            helper.assertTrue(cores.equals(List.of(1L, 3L)) && third.parking.records().size() == 1,
                    "part 0 keeps its records, part 1 has its new ones: " + cores + " " + third.parking.records());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A live record Continuum's code throws on is parked; the phase finishes. */
    private static void liveFailureParked(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, server);
            CompoundTag loot = new CompoundTag();
            loot.putString("dim", dim(helper.getLevel()));
            loot.putLong("pos", helper.absolutePos(new BlockPos(1, 1, 1)).asLong());
            loot.putString("table", "not a table!");
            ctx.add("loot", loot);
            new LiveStep().run(ctx, server);
            long failed = new Parking(world).records().stream().filter(t -> ((CompoundTag) t).getString("kind").equals("live_record")).count();
            helper.assertTrue(failed == 1, "the failed record is kept: " + new Parking(world).records());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A new version of a step that already ran is marked done, never run over stores it converted. */
    private static void stepVersions(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            ImportState state = ImportState.create(world, "1.0.0-beta.12", 7, "test");
            state.markDone("players@1");
            AtomicBoolean ran = new AtomicBoolean();
            Step next = new Step() {
                @Override
                public String id() {
                    return "players@2";
                }

                @Override
                public boolean applies(Ctx ctx) {
                    return true;
                }

                @Override
                public boolean run(Ctx ctx, @Nullable MinecraftServer server) {
                    ran.set(true);
                    return true;
                }
            };
            new Pipeline(List.of(next), id -> true).run(Step.Phase.FILES, state, DeImportGameTests.ctx(world, null), null);
            helper.assertTrue(!ran.get() && state.stepDone("players@2"), "players@2 never ran");

            ImportState done = ImportState.create(world, "1.0.0-beta.12", 7, "test");
            done.status("done");
            for (Step step : Steps.ALL) done.markDone(step.id().substring(0, step.id().indexOf('@')) + "@0");
            helper.assertTrue(!DeImport.hasNewSteps(done), "new versions of known steps are not new steps");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    private static void reopenedByDe(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            ImportState state = ImportState.create(world, "1.0.0-beta.12", 7, "test");
            state.status("done");
            helper.assertTrue(DeImport.reopenedByDe(state, "1.0.0-beta.12") != null, "DE opened the imported world");
            helper.assertTrue(DeImport.reopenedByDe(state, null) == null && DeImport.reopenedByDe(state, "1.0.1") == null,
                    "no warning without DE");
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A chunk from before DE stamped its chunks is recognised by DE's own key and converted as data version 0. */
    private static void unstampedChunks(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            CompoundTag old = DeFixtures.emptyChunk(0, 0);
            old.remove(ChunkConverter.STAMP);
            old.put(ChunkConverter.OLD_INDEX, new CompoundTag());
            CompoundTag plain = DeFixtures.emptyChunk(0, 0);
            plain.remove(ChunkConverter.STAMP);
            helper.assertTrue(new ChunkConverter(ctx, "minecraft:overworld").convert(old.copy()) != null, "DE's unstamped chunk converts");
            helper.assertTrue(new ChunkConverter(ctx, "minecraft:overworld").convert(plain) == null, "a plain unstamped chunk is left");
            Path region = world.resolve("region/r.0.0.mca");
            Files.createDirectories(region.getParent());
            try (RegionFile file = RegionIo.open(region); DataOutputStream out = file.getChunkDataOutputStream(new ChunkPos(0, 0))) {
                NbtIo.write(old, out);
            }
            Signature.Result result = Signature.check(world, null);
            helper.assertTrue(result.de() && result.dataVersion() == 0, "the gate takes it as DE's: " + result);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** DE's heat deadline becomes the share of heat left; a deadline already past is cold. */
    private static void heatScaled(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            CompoundTag hot = DeFixtures.stack("hbm:ingot_chainsteel", 1);
            CompoundTag components = new CompoundTag();
            components.put("hbm:hot_until", LongTag.valueOf(50L));
            hot.put("components", components);
            CompoundTag cold = hot.copy();
            cold.getCompound("components").put("hbm:hot_until", LongTag.valueOf(-5L));
            new Walker(ctx).walk(hot, "test");
            new Walker(ctx).walk(cold, "test");
            helper.assertTrue(hot.getCompound("components").getInt("hbm:heat") == 50, "half of DE's heat is left: " + hot);
            helper.assertTrue(!cold.getCompound("components").contains("hbm:heat"), "a stack that cooled in a chest is cold: " + cold);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Vanilla loads {@code .dat_old} when a player file is damaged, so it is converted too. */
    private static void playerBackups(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Files.createDirectories(world.resolve("playerdata"));
            Path backup = world.resolve("playerdata/00000000-0000-0000-0000-000000000003.dat_old");
            CompoundTag player = new CompoundTag();
            DeFixtures.dePlayer(player);
            NbtIo.writeCompressed(player, backup);
            new PlayersStep().run(DeImportGameTests.ctx(world, helper.getLevel().getServer()), null);
            CompoundTag after = NbtIo.readCompressed(backup, NbtAccounter.unlimitedHeap());
            helper.assertTrue(after.getBoolean("hbm_de_converted") && !after.toString().contains("hbm:cell_uf6"), "the backup converts: " + after);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A chunk the header allocates but vanilla's check refuses is handed over with its bytes, not dropped. */
    private static void headerRefusedChunk(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Path file = regionWith(world, List.of(new ChunkPos(0, 0)));
            try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
                raf.seek(4L);
                raf.writeInt((1000 << 8) | 1);
            }
            List<ChunkPos> damaged = new ArrayList<>();
            AtomicInteger converted = new AtomicInteger();
            RegionIo.rewrite(file, new RegionIo.Hooks() {
                @Override
                public @Nullable CompoundTag convert(ChunkPos pos, CompoundTag chunk) {
                    converted.incrementAndGet();
                    return chunk;
                }

                @Override
                public void damaged(ChunkPos pos, Tag data, String why) {
                    damaged.add(pos);
                }
            });
            helper.assertTrue(damaged.equals(List.of(new ChunkPos(1, 0))) && converted.get() == 1,
                    "the refused chunk is handed over: " + damaged);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** The shard of records no unit owns is written only when they changed. */
    private static void globalOnlyOnChange(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, null);
            ctx.conductor("minecraft:overworld", 1L, false);
            ctx.flushGlobal();
            Path shards = world.resolve(Parking.SHARDS);
            helper.assertTrue(count(shards) == 1, "written once it changed");
            clear(shards);
            ctx.flushGlobal();
            helper.assertTrue(count(shards) == 0, "not rewritten without a change");
            ctx.conductor("minecraft:overworld", 2L, false);
            ctx.flushGlobal();
            helper.assertTrue(count(shards) == 1, "written again after a change");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Datapacks in vanilla's order: level.dat's enabled list, then new packs; disabled ones are not read. */
    private static void datapackOrder(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            for (Map.Entry<String, Integer> pack : Map.of("a", 5, "b", 6, "c", 7).entrySet()) {
                Path file = world.resolve("datapacks/" + pack.getKey() + "/data/hbm/hbm/machine_config/rbmk.json");
                Files.createDirectories(file.getParent());
                Files.writeString(file, "{\"column_height\": " + pack.getValue() + "}");
            }
            helper.assertTrue(Integer.valueOf(7).equals(DeImportGameTests.ctx(world, null).rbmkColumnHeight()),
                    "without level.dat every pack is new and the last one wins");
            CompoundTag root = new CompoundTag(), data = new CompoundTag(), packs = new CompoundTag();
            ListTag enabled = new ListTag(), disabled = new ListTag();
            enabled.add(StringTag.valueOf("vanilla"));
            enabled.add(StringTag.valueOf("file/b"));
            enabled.add(StringTag.valueOf("file/a"));
            disabled.add(StringTag.valueOf("file/c"));
            packs.put("Enabled", enabled);
            packs.put("Disabled", disabled);
            data.put("DataPacks", packs);
            root.put("Data", data);
            NbtIo.writeCompressed(root, world.resolve("level.dat"));
            List<String> order = Datapacks.ordered(world).stream().map(p -> p.getFileName().toString()).toList();
            helper.assertTrue(order.equals(List.of("b", "a")), "enabled order, disabled left out: " + order);
            helper.assertTrue(Integer.valueOf(5).equals(DeImportGameTests.ctx(world, null).rbmkColumnHeight()), "the top enabled pack wins");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** An airship's cables join the power graph without loading the ship, its piles link and a powered pad can fall back. */
    private static void airshipLinks(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        ServerLevel level = helper.getLevel();
        BlockPos cable = new BlockPos(16_000_003, 70, 16_000_003);
        try {
            Ctx ctx = DeImportGameTests.ctx(world, level.getServer());
            CompoundTag plotChunk = DeFixtures.emptyChunk(1_000_000, 1_000_000);
            DeFixtures.setBlock(plotChunk, cable, DeFixtures.block("hbm:cable"));
            Unit unit = ctx.begin("airships plot test");
            new ChunkConverter(ctx, dim(level), true).convert(plotChunk);
            ctx.commit(unit);
            new LiveStep().run(ctx, level.getServer());
            helper.assertTrue(PowerGraph.get(level).containsCell(cable.asLong()), "the ship's cable is in the power graph");
            helper.assertTrue(!level.hasChunk(cable.getX() >> 4, cable.getZ() >> 4), "without loading the ship's plot");

            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            BlockPos core = new BlockPos(1, 70, 1), member = new BlockPos(2, 70, 1);
            DeFixtures.setBlock(chunk, core, DeFixtures.block("hbm:pile_block"));
            DeFixtures.setBlock(chunk, member, DeFixtures.block("hbm:pile_block"));
            CompoundTag pileCore = DeFixtures.blockEntity("hbm:pile_core", core);
            pileCore.putIntArray("members", new int[]{1, 70, 1, 2, 70, 1});
            DeFixtures.addBlockEntity(chunk, pileCore);
            DeFixtures.addBlockEntity(chunk, DeFixtures.blockEntity("hbm:pile_block", member));
            CompoundTag chunks = new CompoundTag();
            chunks.put(Long.toString(new ChunkPos(0, 0).toLong()), chunk);
            CompoundTag plot = new CompoundTag();
            plot.put("chunks", chunks);
            CompoundTag ship = new CompoundTag();
            ship.put("plot", plot);
            Airships.convert(ctx, dim(level), ship, "test ship");
            CompoundTag linked = null;
            for (Tag t : chunks.getCompound(Long.toString(new ChunkPos(0, 0).toLong())).getList("block_entities", Tag.TAG_COMPOUND))
                if (((CompoundTag) t).getInt("x") == member.getX()) linked = (CompoundTag) t;
            helper.assertTrue(linked != null && linked.getInt("cX") == 1 && linked.getInt("cY") == 70, "the ship's pile member points at its core: " + linked);

            CompoundTag pad = DeFixtures.blockEntity("hbm:launch1", new BlockPos(4, 70, 4));
            pad.putBoolean("redstone", true);
            BlockEntities.convert(pad, "hbm:launch_pad", ctx, null, "test");
            int[] activated = pad.getIntArray("activatedBlocks");
            helper.assertTrue(activated.length == 3 && activated[0] == 4 && pad.getInt("redstonePower") == 1,
                    "a powered pad counts its core, so the count can fall to nothing: " + pad);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            PowerGraph.get(level).removeNode(cable.asLong());
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Two unit names that read alike as file names keep their own shards. */
    private static void shardNames(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, null);
            Unit slash = ctx.begin("terrain mod:a/b r.0.0.mca");
            ctx.core("mod:a/b", 1L, "hbm:machine_centrifuge");
            ctx.commit(slash);
            Unit underscore = ctx.begin("terrain mod:a_b r.0.0.mca");
            ctx.core("mod:a_b", 2L, "hbm:machine_centrifuge");
            ctx.commit(underscore);
            helper.assertTrue(DeImportGameTests.ctx(world, null).cores.size() == 2, "both units' records survive a restart");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Finishing the live phase writes the waiting work to its own shard before the others go. */
    private static void waitingSurvives(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        try {
            Ctx ctx = DeImportGameTests.ctx(world, server);
            Unit unit = ctx.begin("terrain test r.0.0.mca");
            ctx.conductor("nomod:void", 1L, false);
            ctx.conductor(dim(helper.getLevel()), pos.asLong(), false);
            ctx.commit(unit);
            new LiveStep().run(ctx, server);
            Path shards = world.resolve(Parking.SHARDS);
            Ctx restarted = DeImportGameTests.ctx(world, server);
            helper.assertTrue(count(shards) == 1 && restarted.conductors.size() == 1
                    && restarted.conductors.get(0).getString("dim").equals("nomod:void"), "only the waiting work is left: " + restarted.conductors);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Another mod's or vanilla's block entity in a DE chunk is not load-checked, so its numbers stay as they were. */
    private static void foreignUntouched(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            BlockPos pos = new BlockPos(1, 70, 1);
            BlockEntity fresh = ((EntityBlock) Blocks.FURNACE).newBlockEntity(pos, Blocks.FURNACE.defaultBlockState());
            CompoundTag shape = fresh.saveWithoutMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(shape.get("CookTime") instanceof IntTag, "a furnace saves its cook time as an int: " + shape);
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(chunk, pos, DeFixtures.block("minecraft:furnace"));
            CompoundTag furnace = DeFixtures.blockEntity("minecraft:furnace", pos);
            furnace.put("CookTime", LongTag.valueOf(5L));
            DeFixtures.addBlockEntity(chunk, furnace);
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            CompoundTag out = new ChunkConverter(ctx, "minecraft:overworld").convert(chunk);
            CompoundTag after = out.getList("block_entities", Tag.TAG_COMPOUND).getCompound(0);
            helper.assertTrue(after.get("CookTime") instanceof LongTag, "the foreign block entity is left as it was: " + after);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    private static String dim(ServerLevel level) {
        return level.dimension().location().toString();
    }

    private static Path regionWith(Path world, List<ChunkPos> positions) throws IOException {
        Path file = world.resolve("region/r.0.0.mca");
        Files.createDirectories(file.getParent());
        try (RegionFile region = RegionIo.open(file)) {
            for (ChunkPos pos : positions) {
                try (DataOutputStream out = region.getChunkDataOutputStream(pos)) {
                    NbtIo.write(DeFixtures.emptyChunk(pos.x, pos.z), out);
                }
            }
        }
        return file;
    }

    private static long count(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return 0;
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".dat")).count();
        }
    }

    private static void clear(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.toList()) Files.delete(p);
        }
    }
}
