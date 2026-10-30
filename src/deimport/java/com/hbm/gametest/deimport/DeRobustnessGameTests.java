package com.hbm.gametest.deimport;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.deimport.DeImport;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.NbtDiff;
import com.hbm.deimport.engine.Parking;
import com.hbm.deimport.engine.RegionIo;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.gate.Signature;
import com.hbm.deimport.source.de7.ChunkConverter;
import com.hbm.deimport.source.de7.OldChunks;
import com.hbm.deimport.steps.BackupStep;
import com.hbm.deimport.steps.LiveStep;
import com.hbm.deimport.steps.PlayerFilesStep;
import com.hbm.deimport.steps.PlayersStep;
import com.hbm.deimport.steps.Steps;
import com.hbm.deimport.target.ContinuumTarget;
import com.hbm.delegacy.ContinuumStacks;
import com.hbm.delegacy.de7.Walker;
import com.hbm.delegacy.legacy.LegacyStacks;
import dev.golem.core.gametest.GameTests;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.jetbrains.annotations.Nullable;

/** The importer under crashes, damaged files and odd data: each test plants one way to lose data and checks it is kept. */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class DeRobustnessGameTests {
    static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    private static final String UNIT = "terrain test r.0.0.mca";

    static {
        TESTS.function("deimport_units_survive_a_crash", 20, () -> DeRobustnessGameTests::unitsSurviveACrash);
        TESTS.function("deimport_damaged_files_are_skipped", 20, () -> DeRobustnessGameTests::damagedFilesAreSkipped);
        TESTS.function("deimport_old_and_new_data_versions", 20, () -> DeRobustnessGameTests::dataVersions);
        TESTS.function("deimport_unloadable_entries_parked", 20, () -> DeRobustnessGameTests::unloadableEntriesParked);
        TESTS.function("deimport_parked_stack_keeps_its_container", 20, () -> DeRobustnessGameTests::parkedStackKeepsContainer);
        TESTS.function("deimport_fensu_keeps_its_charge", 20, () -> DeRobustnessGameTests::fensuKeepsCharge);
        TESTS.function("deimport_cut_numbers_parked", 20, () -> DeRobustnessGameTests::cutNumbersParked);
        TESTS.function("deimport_live_waits_for_a_dimension", 20, () -> DeRobustnessGameTests::liveWaitsForDimension);
        TESTS.function("deimport_live_resumes_without_repeating", 40, () -> DeRobustnessGameTests::liveResumes);
        TESTS.function("deimport_entity_rules_repeat_safely", 20, () -> DeRobustnessGameTests::entityRulesRepeat);
        TESTS.function("deimport_backup_leaves_importer_files", 20, () -> DeRobustnessGameTests::backupLeavesImporterFiles);
        TESTS.function("deimport_new_steps_on_imported_worlds", 20, () -> DeRobustnessGameTests::newSteps);
        TESTS.function("deimport_gate_reads_any_version", 20, () -> DeRobustnessGameTests::gateAnyVersion);
        TESTS.function("deimport_legacy_keeps_stacks_until_parked", 20, () -> DeRobustnessGameTests::legacyParksFirst);
        TESTS.function("deimport_stat_rename_chain", 20, () -> DeRobustnessGameTests::statRenameChain);
        TESTS.function("deimport_foreign_compounds_left", 20, () -> DeRobustnessGameTests::foreignCompounds);
        TESTS.function("deimport_refunds_without_entities", 20, () -> DeRobustnessGameTests::refundsWithoutEntities);
        TESTS.function("deimport_crate_key_shapes", 20, () -> DeRobustnessGameTests::crateKeyShapes);
    }

    private DeRobustnessGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        GameTests.register(event, TESTS);
    }

    /** A unit's records are on disk before its store is swapped; an interrupted redo neither duplicates nor drops them. */
    private static void unitsSurviveACrash(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, server);
            Unit unit = ctx.begin(UNIT);
            ctx.parking.park("item", "here", "hbm:x", null);
            ctx.core("minecraft:overworld", 5L, "hbm:machine_centrifuge");
            ctx.commit(unit);
            Unit interrupted = ctx.begin(UNIT);
            ctx.core("minecraft:overworld", 6L, "hbm:machine_centrifuge");
            ctx.abandon(interrupted);

            Ctx restarted = DeImportGameTests.ctx(world, server);
            helper.assertTrue(restarted.cores.size() == 1 && restarted.cores.get(0).getLong("pos") == 5L
                    && restarted.parking.records().size() == 1, "a restart reads the committed unit only: " + restarted.cores);
            Unit redo = restarted.begin(UNIT);
            restarted.core("minecraft:overworld", 7L, "hbm:machine_centrifuge");
            restarted.commit(redo);
            Ctx third = DeImportGameTests.ctx(world, server);
            helper.assertTrue(third.cores.size() == 1 && third.cores.get(0).getLong("pos") == 7L && third.parking.records().isEmpty(),
                    "converting the store again replaces its records: " + third.cores + " " + third.parking.records());

            third.conductor("minecraft:overworld", 9L, false);
            third.flushGlobal();
            helper.assertTrue(DeImportGameTests.ctx(world, server).conductors.size() == 1, "records no unit owns are kept too");
            third.finishLive(Map.of());
            helper.assertTrue(DeImportGameTests.ctx(world, server).cores.isEmpty(), "a finished live phase leaves no work behind");

            Files.writeString(world.resolve(Parking.FILE), "not nbt");
            helper.assertTrue(new Parking(world).records().isEmpty() && damaged(world.resolve(Parking.FILE)),
                    "a damaged leftovers file is set aside, not fatal");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** One unreadable chunk, player file or stats file is parked and passed over; the rest converts. */
    private static void damagedFilesAreSkipped(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        try {
            Path region = world.resolve("region/r.0.0.mca");
            Files.createDirectories(region.getParent());
            try (RegionFile file = RegionIo.open(region)) {
                for (ChunkPos pos : List.of(new ChunkPos(0, 0), new ChunkPos(1, 0))) {
                    CompoundTag chunk = DeFixtures.emptyChunk(pos.x, pos.z);
                    try (DataOutputStream out = file.getChunkDataOutputStream(pos)) {
                        NbtIo.write(chunk, out);
                    }
                }
            }
            corrupt(region, new ChunkPos(1, 0));
            AtomicInteger damaged = new AtomicInteger(), converted = new AtomicInteger();
            int changed = RegionIo.rewrite(region, new RegionIo.Hooks() {
                @Override
                public @Nullable CompoundTag convert(ChunkPos pos, CompoundTag chunk) {
                    converted.incrementAndGet();
                    return chunk;
                }

                @Override
                public void damaged(ChunkPos pos, Tag data, String why) {
                    damaged.incrementAndGet();
                }
            });
            helper.assertTrue(damaged.get() == 1 && converted.get() == 1 && changed == 2,
                    "the damaged chunk is handed over and the other converts: " + damaged + " " + converted);

            Ctx ctx = DeImportGameTests.ctx(world, server);
            Files.createDirectories(world.resolve("playerdata"));
            Files.write(world.resolve("playerdata/00000000-0000-0000-0000-000000000001.dat"), new byte[]{1, 2, 3});
            Files.createDirectories(world.resolve("stats"));
            Files.writeString(world.resolve("stats/00000000-0000-0000-0000-000000000001.json"), "{");
            new PlayersStep().run(ctx, null);
            new PlayerFilesStep().run(ctx, null);
            long players = ctx.parking.records().stream().filter(t -> ((CompoundTag) t).getString("kind").equals("player_file")).count();
            helper.assertTrue(players == 2, "the unreadable player and stats files are parked: " + ctx.parking.records());

            Path garbage = world.resolve("region/r.1.1.mca");
            Files.write(garbage, new byte[9000]);
            helper.assertTrue(Signature.check(world, null).de(), "a damaged region file is passed over; the stamped one still decides");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Chunks DE saved at an older data version get DE's own upgrades; one from a newer DE is parked as it is. */
    private static void dataVersions(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            CompoundTag plushie = DeFixtures.blockEntity("hbm:plushie", new BlockPos(1, 70, 1));
            plushie.putByte("type", (byte) 3);
            CompoundTag table = DeFixtures.blockEntity("hbm:large_launch_table", new BlockPos(2, 70, 1));
            table.putInt("padSize", 2);
            DeFixtures.addBlockEntity(chunk, plushie);
            DeFixtures.addBlockEntity(chunk, table);
            OldChunks.upgrade(chunk, 5);
            helper.assertTrue(plushie.getString("type").equals("hundun") && table.getString("padSize").equals("SIZE_10"),
                    "DE's version 6 names: " + plushie + " " + table);

            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            CompoundTag newer = DeFixtures.emptyChunk(0, 0);
            newer.putInt(ChunkConverter.STAMP, ChunkConverter.NEWEST + 1);
            helper.assertTrue(new ChunkConverter(ctx, "minecraft:overworld").convert(newer) == null
                    && ctx.parking.counts().getOrDefault("chunk", 0) == 1, "a chunk from a newer DE is left alone and parked");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A block entity or entity Continuum cannot load at all would be dropped by vanilla: all of it is parked. */
    private static void unloadableEntriesParked(GameTestHelper helper) {
        ContinuumTarget target = new ContinuumTarget(helper.getLevel().getServer());
        CompoundTag be = DeFixtures.blockEntity("hbm:no_such_block_entity", new BlockPos(1, 70, 1));
        be.putInt("stored", 42);
        Map<String, Tag> lost = target.blockEntityLost("hbm:machine_centrifuge", new CompoundTag(), be);
        helper.assertTrue(lost != null && lost.containsKey("stored"), "an unloadable block entity is parked whole: " + lost);
        CompoundTag entity = DeImportGameTests.entity("minecraft:no_such_entity");
        entity.putInt("stored", 42);
        Map<String, Tag> gone = ContinuumTarget.entityLost(helper.getLevel(), entity, false);
        helper.assertTrue(gone != null && gone.containsKey("stored"), "an unloadable entity is parked whole: " + gone);
        helper.succeed();
    }

    /** A parked stack takes its {slot, item} wrapper along, and leaves an empty entry in a list read by index. */
    private static void parkedStackKeepsContainer(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            ListTag container = new ListTag();
            container.add(wrapped(0, DeFixtures.stack("hbm:test_item", 1)));
            container.add(wrapped(1, DeFixtures.stack("minecraft:stone", 2)));
            CompoundTag shulker = DeFixtures.stack("minecraft:shulker_box", 1);
            CompoundTag components = new CompoundTag();
            components.put("minecraft:container", container);
            shulker.put("components", components);
            CompoundTag chest = DeFixtures.blockEntity("minecraft:chest", new BlockPos(1, 70, 1));
            chest.put("Items", DeFixtures.inventory(DeFixtures.slot(0, shulker)).getList("Items", Tag.TAG_COMPOUND));
            new Walker(ctx).walk(chest, "test");
            ListTag kept = chest.getList("Items", Tag.TAG_COMPOUND).getCompound(0).getCompound("components").getList("minecraft:container", Tag.TAG_COMPOUND);
            helper.assertTrue(kept.size() == 1 && kept.getCompound(0).getCompound("item").getString("id").equals("minecraft:stone"),
                    "the shulker keeps its other stack: " + chest);

            CompoundTag zombie = DeImportGameTests.entity("minecraft:zombie");
            ListTag armor = new ListTag();
            armor.add(new CompoundTag());
            armor.add(DeFixtures.stack("hbm:test_item", 1));
            armor.add(DeFixtures.stack("minecraft:iron_chestplate", 1));
            armor.add(new CompoundTag());
            zombie.put("ArmorItems", armor);
            new Walker(ctx).walk(zombie, "test");
            ListTag after = zombie.getList("ArmorItems", Tag.TAG_COMPOUND);
            helper.assertTrue(after.size() == 4 && after.getCompound(1).isEmpty()
                    && after.getCompound(2).getString("id").equals("minecraft:iron_chestplate"), "the chestplate keeps its slot: " + after);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    private static void fensuKeepsCharge(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            CompoundTag state = new CompoundTag();
            state.putLong("power", 5_000_000_000_000L);
            CompoundTag fensu = DeFixtures.stack("hbm:machine_fensu", 1);
            CompoundTag components = new CompoundTag();
            components.put("hbm:machine_battery_state", state);
            fensu.put("components", components);
            new Walker(ctx).walk(fensu, "test");
            helper.assertTrue(fensu.getString("id").equals("hbm:machine_battery_redd")
                    && fensu.getCompound("components").getString("hbm:battery_redd_power").equals("5000000000000"),
                    "the FEnSU's whole charge is the REDD's: " + fensu);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    private static void cutNumbersParked(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag(), shape = new CompoundTag();
        tag.putLong("big", 5_000_000_000L);
        tag.putLong("small", 7L);
        tag.putDouble("fraction", 2.5);
        tag.putDouble("precise", 0.1);
        shape.putInt("big", 0);
        shape.putInt("small", 0);
        shape.putInt("fraction", 0);
        shape.putFloat("precise", 0F);
        Map<String, Tag> cut = NbtDiff.conformNumbers(tag, shape);
        helper.assertTrue(cut.keySet().equals(Set.of("big", "fraction")), "only values the cast changed are reported: " + cut.keySet());
        helper.succeed();
    }

    /** Live work for a dimension that does not exist yet stays pending, and the step waits for it. */
    private static void liveWaitsForDimension(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, server);
            ctx.conductor("nomod:void", 1L, false);
            LiveStep live = new LiveStep();
            live.run(ctx, server);
            helper.assertTrue(live.waitsFor(ctx) != null && live.waitsFor(ctx).contains("nomod:void"), "the step waits: " + live.waitsFor(ctx));
            helper.assertTrue(DeImportGameTests.ctx(world, server).conductors.size() == 1, "the work is still pending on disk");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** A live phase cut short after one record resumes on the rest without rolling that one again. */
    private static void liveResumes(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        BlockPos first = helper.absolutePos(new BlockPos(1, 1, 1)), second = helper.absolutePos(new BlockPos(3, 1, 1));
        try {
            Ctx ctx = DeImportGameTests.ctx(world, server);
            for (BlockPos pos : List.of(first, second)) {
                helper.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
                ctx.add("loot", DeImportGameTests.loot(helper.getLevel(), pos));
            }
            helper.assertTrue(!new LiveStep().slice(ctx, server, 0L), "a slice past its deadline still does one record");
            Ctx restarted = DeImportGameTests.ctx(world, server);
            new LiveStep().run(restarted, server);
            int once = DeImportGameTests.stacks(helper.getLevel(), second);
            helper.assertTrue(once > 0 && DeImportGameTests.stacks(helper.getLevel(), first) == once,
                    "each chest rolled once, the same table and seed giving the same stacks");
            helper.getLevel().setBlockAndUpdate(first, Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Converting an entity already in Continuum's shape changes nothing. */
    private static void entityRulesRepeat(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            CompoundTag soldier = DeImportGameTests.entity("hbm:entity_undead_soldier");
            soldier.putString("type", "skeleton");
            CompoundTag cog = DeImportGameTests.entity("hbm:entity_stray_cog");
            cog.putBoolean("steel", true);
            new Walker(ctx).walk(soldier, "test");
            new Walker(ctx).walk(cog, "test");
            helper.assertTrue(soldier.getString("type").equals("skeleton") && cog.getBoolean("steel"),
                    "a second pass keeps both: " + soldier + " " + cog);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    private static void backupLeavesImporterFiles(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        Path backup = null;
        try {
            Files.writeString(world.resolve("level.dat"), "x");
            ImportState.create(world, "1.0.0-beta.12", 7, "test").save();
            Files.writeString(world.resolve("hbm_de_import.journal"), "x\n");
            Ctx ctx = DeImportGameTests.ctx(world, null);
            new BackupStep().run(ctx, null);
            try (Stream<Path> siblings = Files.list(world.getParent())) {
                String prefix = world.getFileName() + "_DE_backup_";
                backup = siblings.filter(p -> p.getFileName().toString().startsWith(prefix)).findFirst().orElse(null);
            }
            helper.assertTrue(backup != null && Files.isRegularFile(backup.resolve("level.dat"))
                    && !Files.exists(backup.resolve(ImportState.FILE)) && !Files.exists(backup.resolve("hbm_de_import.journal")),
                    "the backup is the DE world as it was: " + backup);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
            if (backup != null) DeImportGameTests.delete(backup);
        }
        helper.succeed();
    }

    private static void newSteps(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            ImportState state = ImportState.create(world, "1.0.0-beta.12", 7, "test");
            state.status("done");
            for (var step : Steps.all()) state.markDone(step.id());
            helper.assertTrue(!DeImport.hasNewSteps(state), "every step known: nothing to run");
            ImportState older = ImportState.create(world, "1.0.0-beta.12", 7, "test");
            older.status("done");
            for (var step : Steps.all().subList(0, Steps.all().size() - 1)) older.markDone(step.id());
            helper.assertTrue(DeImport.hasNewSteps(older), "a step added later runs in an imported world");
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** DE's files decide, whatever version opened the world last. */
    private static void gateAnyVersion(GameTestHelper helper) {
        Path stamped = DeImportGameTests.tempWorld(), keyed = DeImportGameTests.tempWorld(), plain = DeImportGameTests.tempWorld();
        try {
            Path region = stamped.resolve("region/r.3.-2.mca");
            Files.createDirectories(region.getParent());
            try (RegionFile file = RegionIo.open(region)) {
                ChunkPos pos = new ChunkPos(96, -64);
                CompoundTag chunk = DeFixtures.emptyChunk(pos.x, pos.z);
                try (DataOutputStream out = file.getChunkDataOutputStream(pos)) {
                    NbtIo.write(chunk, out);
                }
            }
            helper.assertTrue(Signature.check(stamped, null).de(), "DE stamps away from spawn, no version change");
            DeFixtures.writeKey(keyed);
            helper.assertTrue(Signature.check(keyed, "1.0.1").de(), "DE's key in a world a Continuum opened first");
            helper.assertTrue(!Signature.check(plain, null).de(), "a world without DE data is left alone");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(stamped);
            DeImportGameTests.delete(keyed);
            DeImportGameTests.delete(plain);
        }
        helper.succeed();
    }

    /** A stack the legacy layer parks from stays as it is in its container until the parked record is on disk. */
    private static void legacyParksFirst(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Path file = server.getWorldPath(LevelResource.ROOT).resolve(LegacyStacks.LEFTOVERS);
        try {
            Files.deleteIfExists(file);
            CompoundTag tag = DeFixtures.stack("hbm:canister_empty", 1);
            CompoundTag components = new CompoundTag();
            components.putInt("hbm:b92_animation", 3);
            tag.put("components", components);
            ItemStack legacy = ItemStack.parse(server.registryAccess(), tag).orElse(ItemStack.EMPTY);
            helper.assertTrue(LegacyStacks.isLegacy(legacy), "a DE stack with a DE-only component");
            SimpleContainer container = new SimpleContainer(legacy.copy());
            Files.createDirectories(file);
            helper.assertTrue(LegacyStacks.convertAll(container, server, "test") == 0 && LegacyStacks.isLegacy(container.getItem(0)),
                    "with nowhere to keep the record, the stack stays as it is");
            Files.delete(file);
            helper.assertTrue(LegacyStacks.convertAll(container, server, "test") == 1 && !LegacyStacks.isLegacy(container.getItem(0))
                    && !container.getItem(0).isEmpty() && Files.isRegularFile(file), "once the parked component is kept, the stack converts");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                if (Files.isDirectory(file)) Files.delete(file);
                else Files.deleteIfExists(file);
            } catch (IOException e) {
                // a leftover test file in the GameTest world changes no test
            }
        }
        helper.succeed();
    }

    /** {@code battery_pack_capacitor_copper} becomes {@code capacitor_copper}, which itself becomes the machine. */
    private static void statRenameChain(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Files.createDirectories(world.resolve("stats"));
            Path file = world.resolve("stats/00000000-0000-0000-0000-000000000002.json");
            Files.writeString(file, "{\"stats\":{\"minecraft:used\":{\"hbm:battery_pack_capacitor_copper\":3,\"hbm:capacitor_copper\":5}},\"DataVersion\":3955}");
            new PlayerFilesStep().run(DeImportGameTests.ctx(world, null), null);
            JsonObject used = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonObject("stats").getAsJsonObject("minecraft:used");
            helper.assertTrue(used.get("hbm:machine_capacitor_copper").getAsLong() == 5 && used.get("hbm:capacitor_copper").getAsLong() == 3,
                    "each count moves once: " + used);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Another mod's compound shaped like a stack, and a storage mod's oversized stack, are left as they were. */
    private static void foreignCompounds(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            CompoundTag big = DeFixtures.stack("minecraft:stone", 120);
            CompoundTag components = new CompoundTag();
            components.putInt("minecraft:repair_cost", 2);
            big.put("components", components);
            CompoundTag drawer = DeFixtures.blockEntity("othermod:drawer", new BlockPos(1, 70, 1));
            drawer.put("Items", DeFixtures.inventory(DeFixtures.slot(0, DeFixtures.stack("othermod:thing", 3)), DeFixtures.slot(1, big))
                    .getList("Items", Tag.TAG_COMPOUND));
            new Walker(ctx).walk(drawer, "test");
            ListTag items = drawer.getList("Items", Tag.TAG_COMPOUND);
            helper.assertTrue(items.size() == 2 && items.getCompound(1).getCompound("components").contains("minecraft:repair_cost"),
                    "both stay, with their components: " + items);
            helper.assertTrue(ctx.parking.counts().getOrDefault("unknown_item", 0) == 1, "the unknown one is copied to the leftovers");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** Refunded blocks without a block entity are recorded; an airship plot's refunds are parked, never placed in the world. */
    private static void refundsWithoutEntities(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(chunk, new BlockPos(1, 70, 1), DeFixtures.block("hbm:test_block"));
            new ChunkConverter(ctx, "minecraft:overworld").convert(chunk);
            helper.assertTrue(ctx.parking.counts().getOrDefault("refunded_block", 0) == 1, "the DE-only block is recorded: " + ctx.parking.counts());

            Ctx plotCtx = DeImportGameTests.ctx(DeImportGameTests.tempWorld(), helper.getLevel().getServer());
            CompoundTag plot = DeFixtures.emptyChunk(0, 0);
            plot.remove(ChunkConverter.STAMP);
            BlockPos turbine = new BlockPos(2, 70, 2);
            DeFixtures.setBlock(plot, turbine, DeFixtures.block("hbm:machine_turbine"));
            CompoundTag be = DeFixtures.blockEntity("hbm:turbine", turbine);
            be.put("inventory", DeFixtures.inventory(DeFixtures.slot(0, DeFixtures.stack("hbm:ingot_steel", 7))));
            DeFixtures.addBlockEntity(plot, be);
            CompoundTag plotOut = new ChunkConverter(plotCtx, "minecraft:overworld", true).convert(plot);
            helper.assertTrue(!plotOut.toString().contains("minecraft:chest") && plotCtx.parking.counts().getOrDefault("item", 0) == 1,
                    "the plot's refund is parked, not put in a chest: " + plotCtx.parking.counts());
            DeImportGameTests.delete(plotCtx.world);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    /** DE's key file inside vanilla's wrapper, as DE writes it, and the bare shape. */
    private static void crateKeyShapes(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        try {
            DeFixtures.writeKey(world);
            byte[] wrapped = ContinuumStacks.lockedContentsKey(world);
            CompoundTag bare = new CompoundTag();
            bare.putByteArray("data", new byte[32]);
            NbtIo.writeCompressed(bare, world.resolve("data/hbm_locked_contents_key.dat"));
            byte[] flat = ContinuumStacks.lockedContentsKey(world);
            helper.assertTrue(wrapped != null && wrapped.length == 32 && flat != null && flat.length == 32, "both shapes give the key");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
        helper.succeed();
    }

    private static CompoundTag wrapped(int slot, CompoundTag stack) {
        CompoundTag entry = new CompoundTag();
        entry.putInt("slot", slot);
        entry.put("item", stack);
        return entry;
    }

    private static boolean damaged(Path file) throws IOException {
        try (Stream<Path> siblings = Files.list(file.getParent())) {
            return siblings.anyMatch(p -> p.getFileName().toString().startsWith(file.getFileName() + ".damaged-"));
        }
    }

    /** Overwrites a chunk's compressed data with bytes no decompressor takes. */
    private static void corrupt(Path region, ChunkPos pos) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(region.toFile(), "rw")) {
            int index = (pos.x & 31) + (pos.z & 31) * 32;
            raf.seek(index * 4L);
            int entry = raf.readInt();
            long start = (long) (entry >>> 8) * 4096;
            raf.seek(start + 5);
            for (int i = 0; i < 64; i++) raf.writeByte(0x5A);
        }
    }
}
