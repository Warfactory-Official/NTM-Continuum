package com.hbm.gametest.deimport;

import com.hbm.Constants;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.ImportedCells;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.deimport.DeImport;
import com.hbm.deimport.engine.ChunkBlocks;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Journal;
import com.hbm.deimport.engine.NbtDiff;
import com.hbm.deimport.engine.PackedSection;
import com.hbm.deimport.engine.Parking;
import com.hbm.deimport.engine.Pipeline;
import com.hbm.deimport.engine.RegionIo;
import com.hbm.deimport.engine.Report;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.gate.ImportState;
import com.hbm.deimport.gate.Signature;
import com.hbm.deimport.source.de7.Airships;
import com.hbm.deimport.source.de7.BackupMerge;
import com.hbm.deimport.source.de7.BlockEntities;
import com.hbm.deimport.source.de7.BlockStates;
import com.hbm.deimport.source.de7.ChunkConverter;
import com.hbm.deimport.source.de7.Radiation;
import com.hbm.deimport.steps.LiveStep;
import com.hbm.deimport.steps.PlayerFilesStep;
import com.hbm.deimport.steps.PlayersStep;
import com.hbm.deimport.steps.RbmkDialsStep;
import com.hbm.deimport.steps.RegionsStep;
import com.hbm.deimport.steps.SavedDataStep;
import com.hbm.deimport.steps.SubLevelsStep;
import com.hbm.deimport.target.ContinuumTarget;
import com.hbm.delegacy.DeAddons;
import com.hbm.delegacy.de7.Entities;
import com.hbm.delegacy.de7.Walker;
import com.hbm.handler.pollution.PollutionSavedData;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dev.golem.core.gametest.GameTests;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/** The DE importer against DE-shaped saves written by {@link DeFixtures}; Continuum itself loads what it wrote. */
public final class DeImportGameTests {
    static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    static {
        TESTS.function("deimport_section_pack_round_trip", 20, () -> DeImportGameTests::sectionPackRoundTrip);
        TESTS.function("deimport_chunk_conversion", 20, () -> DeImportGameTests::chunkConversion);
        TESTS.function("deimport_multiblock_rebuild", 40, () -> DeImportGameTests::multiblockRebuild);
        TESTS.function("deimport_signature_gates", 20, () -> DeImportGameTests::signatureGates);
        TESTS.function("deimport_pipeline_runs_once", 40, () -> DeImportGameTests::pipelineRunsOnce);
        TESTS.function("deimport_radiation_pockets", 40, () -> DeImportGameTests::radiationPockets);
        TESTS.function("deimport_block_entity_mappers", 20, () -> DeImportGameTests::blockEntityMappers);
        TESTS.function("deimport_rbmk_dials", 20, () -> DeImportGameTests::rbmkDials);
        TESTS.function("deimport_cables_join_network", 20, () -> DeImportGameTests::cablesJoinNetwork);
        TESTS.function("deimport_saved_data_shapes", 20, () -> DeImportGameTests::savedDataShapes);
        TESTS.function("deimport_airship_plot", 20, () -> DeImportGameTests::airshipPlot);
        TESTS.function("deimport_nothing_lost_silently", 20, () -> DeImportGameTests::nothingLostSilently);
        TESTS.function("deimport_audited_mappings", 20, () -> DeImportGameTests::auditedMappings);
        TESTS.function("deimport_loot_and_references", 20, () -> DeImportGameTests::lootAndReferences);
        TESTS.function("deimport_satellites_and_pack_recipes", 20, () -> DeImportGameTests::satellitesAndPackRecipes);
        TESTS.function("deimport_jigsaw_pieces", 20, () -> DeImportGameTests::jigsawPieces);
        TESTS.function("deimport_deferred_addons", 20, () -> DeImportGameTests::deferredAddons);
        TESTS.function("deimport_late_walker_and_records", 20, () -> DeImportGameTests::lateWalkerAndRecords);
        TESTS.function("deimport_backup_merge", 20, () -> DeImportGameTests::backupMerge);
        TESTS.function("deimport_backup_merge_cells", 20, () -> DeImportGameTests::backupMergeCells);
        TESTS.function("deimport_airship_plot_positions", 20, () -> DeImportGameTests::airshipPlotPositions);
        if (ModList.get().isLoaded("sable")) {
            TESTS.function("deimport_airship_merge", 20, () -> DeSableGameTests::merge);
            TESTS.function("deimport_airship_plots_repaired", 20, () -> DeSableGameTests::plotsRepaired);
        }
    }

    private DeImportGameTests() {}

    private static void sectionPackRoundTrip(GameTestHelper helper) {
        for (int size : new int[]{1, 2, 16, 17, 300}) {
            CompoundTag states = new CompoundTag();
            ListTag palette = new ListTag();
            for (int i = 0; i < size; i++) palette.add(DeFixtures.block("minecraft:stone_" + i));
            states.put("palette", palette);
            PackedSection section = new PackedSection(states);
            for (int i = 0; i < PackedSection.SIZE; i++) section.index[i] = (i * 31) % size;
            section.store(states);
            PackedSection back = new PackedSection(states);
            for (int i = 0; i < PackedSection.SIZE; i++) {
                String want = section.palette.getCompound(section.index[i]).getString("Name");
                String got = back.palette.getCompound(back.index[i]).getString("Name");
                helper.assertTrue(want.equals(got), size + " entries: block " + i + " is " + got + ", not " + want);
            }
        }
        // Two DE states that become one Continuum state share one palette entry.
        CompoundTag states = new CompoundTag();
        ListTag palette = new ListTag();
        palette.add(DeFixtures.block("minecraft:air"));
        palette.add(DeFixtures.block("minecraft:air"));
        states.put("palette", palette);
        PackedSection section = new PackedSection(states);
        section.index[5] = 1;
        section.store(states);
        helper.assertTrue(states.getList("palette", Tag.TAG_COMPOUND).size() == 1 && !states.contains("data"),
                "duplicate palette entries are merged");
        helper.succeed();
    }

    static Ctx ctx(Path world, @Nullable MinecraftServer server) throws IOException {
        return new Ctx(world, new ContinuumTarget(server), new Parking(world), new Report(), new Journal(world));
    }

    private static void chunkConversion(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            BlockPos origin = new BlockPos(2, 70, 2);
            DeFixtures.writeKey(world);
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.standardContent(chunk, origin);
            Ctx ctx = ctx(world, null);
            CompoundTag out = new ChunkConverter(ctx, "minecraft:overworld").convert(chunk);
            helper.assertTrue(out != null && !out.contains(ChunkConverter.STAMP), "the chunk is converted and unstamped");

            Map<BlockPos, CompoundTag> states = states(out);
            expectBlock(helper, states, origin, "hbm:machine_centrifuge");
            expectBlock(helper, states, origin.above(), "minecraft:air");
            expectBlock(helper, states, origin.south(6), "hbm:concrete_colored");
            helper.assertTrue("black".equals(states.get(origin.south(6)).getCompound("Properties").getString("color")),
                    "concrete_black becomes concrete_colored[color=black]");
            expectBlock(helper, states, origin.south(6).east(), "hbm:lamp_tritium_blue_on");
            expectBlock(helper, states, origin.south(6).east(2), "hbm:sellafield_3");
            expectBlock(helper, states, origin.offset(10, 0, 10), "hbm:machine_zirnox");
            expectBlock(helper, states, origin.south(6).east(3), "hbm:pwr_fuel");
            expectBlock(helper, states, origin.south(6).east(4), "hbm:icf_laser_component_turbo");
            expectBlock(helper, states, origin.south(4), "hbm:fluid_duct_neo");
            expectBlock(helper, states, origin.south(9), "minecraft:chest");

            Map<BlockPos, CompoundTag> bes = new HashMap<>();
            for (Tag t : out.getList("block_entities", Tag.TAG_COMPOUND)) {
                CompoundTag be = (CompoundTag) t;
                bes.put(new BlockPos(be.getInt("x"), be.getInt("y"), be.getInt("z")), be);
            }
            CompoundTag centrifuge = bes.get(origin);
            helper.assertTrue(centrifuge != null && centrifuge.getString("id").equals("hbm:machine_centrifuge"),
                    "centrifuge block entity id: " + centrifuge);
            CompoundTag duct = bes.get(origin.south(4));
            helper.assertTrue(duct != null && duct.getString("type").equals("hbm:oil"), "duct gets DE's graph fluid: " + duct);
            CompoundTag tank = bes.get(origin.offset(5, 0, 1));
            helper.assertTrue(tank != null && tank.getInt("tank") == 16000 && tank.getInt("tank_max") == 256000
                    && tank.getString("tank_type").equals("hbm:oil"), "tank flattened: " + tank);

            // Continuum's own block entities read what the converter wrote.
            BlockEntity loadedCentrifuge = load(helper, origin, states.get(origin), centrifuge);
            CompoundTag saved = loadedCentrifuge.saveWithoutMetadata(helper.getLevel().registryAccess());
            String inv = saved.toString();
            helper.assertTrue(inv.contains("hbm:canned_conserve_ass") && inv.contains("hbm:battery_lead") && inv.contains("1234"),
                    "centrifuge keeps its renamed stacks and the battery charge: " + inv);
            BlockEntity loadedTank = load(helper, origin.offset(5, 0, 1), states.get(origin.offset(5, 0, 1)), tank);
            CompoundTag tankSaved = loadedTank.saveWithoutMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(tankSaved.getInt("tank") == 16000 && tankSaved.getString("tank_type").equals("hbm:oil"),
                    "Continuum's tank holds 16000 mB of oil: " + tankSaved);

            String chest = bes.get(origin.south(9).east(3)).toString();
            helper.assertTrue(chest.contains("hbm:machine_capacitor_copper"), "DE's capacitor block item: " + chest);
            helper.assertTrue(chest.contains("hbm:crashed_bomb_nuke"), "stack inside a shulker inside a chest: " + chest);
            helper.assertTrue(chest.contains("hbm:wire_fine_red_copper"), "mingrade spelling: " + chest);
            helper.assertTrue(!chest.contains("hbm:test_item"), "test_item is parked, not left to vanish");
            helper.assertTrue(chest.contains("hbm:canister_full") && chest.contains("\"hbm:fluid\":\"hbm:diesel\""),
                    "a filled DE canister is Continuum's full canister of diesel: " + chest);
            helper.assertTrue(chest.contains("\"hbm:designator_x\":123") && chest.contains("\"hbm:designator_z\":-456"),
                    "designator target unpacked: " + chest);
            helper.assertTrue(chest.contains("fluid1:\"hbm:watz\"") || chest.contains("\"fluid1\":\"hbm:watz\""),
                    "identifier fluids, with DE's watz_mud renamed: " + chest);
            helper.assertTrue(chest.contains("hbm:battery_redd_power"), "REDD charge kept: " + chest);
            helper.assertTrue(chest.contains("hbm:gun_data") && chest.contains("wear_0:50.0f")
                    && chest.contains("magtype0:\"hbm:stone\"") && chest.contains("magcount0:6") && !chest.contains("timer_0"),
                    "gun wear, modes and its loaded ammo by name: " + chest);
            helper.assertTrue(chest.contains("hbm:ingot_uranium") && !chest.contains("locked_contents"),
                    "a sealed crate opens with the world key: " + chest);
            helper.assertTrue(ctx.parking.counts().getOrDefault("item", 0) == 1, "one stack parked: " + ctx.parking.counts());
            CompoundTag refund = bes.values().stream().filter(be -> be.getString("id").equals("minecraft:chest") && be.toString().contains("hbm:ingot_steel"))
                    .findFirst().orElse(null);
            helper.assertTrue(refund != null && states(out).get(new BlockPos(refund.getInt("x"), refund.getInt("y"), refund.getInt("z")))
                    .getString("Name").equals("minecraft:chest"), "the steam turbine's stacks are in a chest in the chunk: " + refund);
            Set<Long> cores = new HashSet<>();
            for (CompoundTag core : ctx.cores) cores.add(core.getLong("pos"));
            helper.assertTrue(cores.contains(origin.asLong()) && cores.contains(origin.offset(5, 0, 1).asLong()),
                    "multiblock cores collected for the live rebuild: " + ctx.cores);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static BlockEntity load(GameTestHelper helper, BlockPos pos, CompoundTag state, CompoundTag tag) {
        BlockState blockState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), state);
        BlockEntity be = BlockEntity.loadStatic(pos, blockState, tag, helper.getLevel().registryAccess());
        helper.assertTrue(be != null, "Continuum loads " + tag.getString("id"));
        return be;
    }

    private static void expectBlock(GameTestHelper helper, Map<BlockPos, CompoundTag> states, BlockPos pos, String name) {
        CompoundTag state = states.get(pos);
        String got = state == null ? "minecraft:air" : state.getString("Name");
        helper.assertTrue(got.equals(name), pos.toShortString() + " is " + got + ", not " + name);
    }

    /** Every non-air block of a saved chunk by absolute position. */
    private static Map<BlockPos, CompoundTag> states(CompoundTag chunk) {
        Map<BlockPos, CompoundTag> out = new HashMap<>();
        int cx = chunk.getInt("xPos"), cz = chunk.getInt("zPos");
        for (Tag t : chunk.getList("sections", Tag.TAG_COMPOUND)) {
            CompoundTag section = (CompoundTag) t;
            PackedSection packed = new PackedSection(section.getCompound("block_states"));
            int y = section.getByte("Y");
            for (int i = 0; i < PackedSection.SIZE; i++) {
                CompoundTag state = packed.palette.getCompound(packed.index[i]);
                if (state.getString("Name").equals("minecraft:air")) continue;
                out.put(new BlockPos(cx * 16 + (i & 15), y * 16 + (i >> 8), cz * 16 + ((i >> 4) & 15)), state);
            }
        }
        return out;
    }

    private static void multiblockRebuild(GameTestHelper helper) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("hbm:machine_chemical_plant"));
        helper.assertTrue(block instanceof BlockMultiblockCore, "chemical plant is a multiblock");
        BlockMultiblockCore machine = (BlockMultiblockCore) block;
        BlockPos core = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockState state = machine.defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH);
        helper.getLevel().setBlock(core, state, Block.UPDATE_CLIENTS);
        List<BlockPos> cells = new ArrayList<>();
        machine.visitPlacedCells(helper.getLevel(), core, Direction.NORTH, (pos, mask) -> {
            if (!cells.contains(pos)) cells.add(pos.immutable());
        });
        BlockPos stone = cells.get(cells.size() - 1);
        helper.getLevel().setBlock(stone, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);

        ImportedCells.Result result = ImportedCells.rebuild(helper.getLevel(), core);
        helper.assertTrue(result.placed() == cells.size() - 1 && result.blocked() == 1,
                "placed " + result.placed() + " of " + (cells.size() - 1) + ", blocked " + result.blocked());
        helper.assertTrue(helper.getLevel().getBlockState(stone).is(Blocks.STONE), "a player's block is never replaced");
        for (BlockPos cell : cells) {
            if (cell.equals(stone)) continue;
            helper.assertTrue(machine.isCellOf(helper.getLevel().getBlockState(cell)), "cell at " + cell.toShortString());
            helper.assertTrue(MultiblockCoreIndex.lookup(helper.getLevel(), cell) == core.asLong(), "indexed " + cell.toShortString());
        }
        ImportedCells.Result again = ImportedCells.rebuild(helper.getLevel(), core);
        helper.assertTrue(again.placed() == cells.size() - 1, "a second rebuild finds every cell already there");
        helper.succeed();
    }

    private static void signatureGates(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            helper.assertTrue(!Signature.check(world, "1.0.1").de(), "a Continuum version with no DE data is not DE");
            helper.assertTrue(Signature.check(world, DeFixtures.DE_VERSION).de(), "a world DE opened last imports, DE data found or not");
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(new CompoundTag(), world.resolve("data/hbm_locked_contents_key.dat"));
            helper.assertTrue(Signature.check(world, DeFixtures.DE_VERSION).de(), "DE's version and its key file");
            helper.assertTrue(Signature.check(world, "1.0.1").de(), "DE's key file decides whatever version opened the world last");
            writeChunk(world, DeFixtures.emptyChunk(0, 0));
            Signature.Result stamped = Signature.check(world, "1.0.1");
            helper.assertTrue(stamped.de() && stamped.dataVersion() == 7, "a stamped spawn chunk after a Continuum start");
            CompoundTag newer = DeFixtures.emptyChunk(0, 0);
            newer.putInt(ChunkConverter.STAMP, 8);
            writeChunk(world, newer);
            helper.assertTrue(Signature.check(world, DeFixtures.DE_VERSION).dataVersion() > Signature.NEWEST_DATA_VERSION,
                    "a newer DE data version is seen, so the world is refused");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static void pipelineRunsOnce(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.standardContent(chunk, new BlockPos(4, 70, 4));
            writeChunk(world, chunk);
            Path playerFile = world.resolve("playerdata/00000000-0000-0000-0000-000000000001.dat");
            Files.createDirectories(playerFile.getParent());
            CompoundTag player = new CompoundTag();
            player.put("Inventory", new ListTag());
            DeFixtures.dePlayer(player);
            player.getList("Inventory", Tag.TAG_COMPOUND).add(DeFixtures.slot(11, DeFixtures.stack("hbm:battery_pack_capacitor_copper", 1)));
            NbtIo.writeCompressed(player, playerFile);

            CompoundTag impact = new CompoundTag();
            impact.putFloat("dust", 0.5F);
            impact.putBoolean("impact", true);
            CompoundTag wrapped = new CompoundTag();
            wrapped.put("data", impact);
            CompoundTag impactFile = new CompoundTag();
            impactFile.put("data", wrapped);
            impactFile.putInt("DataVersion", 3955);
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(impactFile, world.resolve("data/hbm_impact_data.dat"));

            Path advancements = world.resolve("advancements/00000000-0000-0000-0000-000000000001.json");
            Files.createDirectories(advancements.getParent());
            Files.writeString(advancements, "{\"hbm:bosscreeper\":{\"criteria\":{\"earned\":\"2026-01-01 10:00:00 +0000\"},\"done\":true},"
                    + "\"minecraft:story/root\":{\"criteria\":{\"crafting_table\":\"2026-01-01 10:00:00 +0000\"},\"done\":true},\"DataVersion\":3955}");
            Path stats = world.resolve("stats/00000000-0000-0000-0000-000000000001.json");
            Files.createDirectories(stats.getParent());
            Files.writeString(stats, "{\"stats\":{\"minecraft:mined\":{\"hbm:concrete_black\":3,\"hbm:concrete_colored\":2},"
                    + "\"minecraft:custom\":{\"hbm:legendary\":1,\"minecraft:jump\":9}},\"DataVersion\":3955}");

            List<Step> steps = List.of(new RegionsStep(), new PlayersStep(), new PlayerFilesStep(), new SavedDataStep());
            for (int run = 0; run < 2; run++) {
                ImportState state = ImportState.exists(world) ? ImportState.read(world)
                        : ImportState.create(world, DeFixtures.DE_VERSION, 7, "test");
                Ctx ctx = ctx(world, helper.getLevel().getServer());
                Pipeline pipeline = new Pipeline(steps, id -> true);
                pipeline.run(Step.Phase.FILES, state, ctx, null);
                pipeline.finish(state, ctx, world);
                helper.assertTrue(state.status().equals("done"), "status after run " + run + ": " + state.status());
            }
            CompoundTag converted = NbtIo.readCompressed(playerFile, NbtAccounter.unlimitedHeap());
            String text = converted.toString();
            helper.assertTrue(text.contains("hbm:apple_lead") && !text.contains("apple_lead_nugget"), "player stacks: " + text);
            helper.assertTrue(text.contains("\"hbm:capacitor_copper\"") && !text.contains("machine_capacitor_copper"),
                    "a battery capacitor is renamed once and never again: " + text);
            CompoundTag props = converted.getCompound("neoforge:attachments").getCompound("hbm:living_props");
            helper.assertTrue(props.getDouble("rads") == 250.0 && props.getFloat("shield") == 5F && props.getInt("asbestos") == 40,
                    "DE's two props attachments merged: " + props);
            helper.assertTrue(!new Parking(world).records().isEmpty(), "parked data kept (in the shards until the live phase is done)");
            String adv = Files.readString(advancements);
            helper.assertTrue(adv.contains("hbm:boss_creeper") && adv.contains("nuclear_creeper") && !adv.contains("bosscreeper")
                    && adv.contains("minecraft:story/root"), "a completed DE advancement stays complete under its new name: " + adv);
            String stat = Files.readString(stats);
            helper.assertTrue(stat.contains("\"hbm:concrete_colored\": 5") && !stat.contains("concrete_black")
                    && !stat.contains("legendary") && stat.contains("minecraft:jump"), "stats follow the renames: " + stat);
            CompoundTag unwrapped = NbtIo.readCompressed(world.resolve("data/hbm_impact_data.dat"), NbtAccounter.unlimitedHeap());
            helper.assertTrue(unwrapped.getCompound("data").getBoolean("impact") && unwrapped.getCompound("data").getFloat("dust") == 0.5F,
                    "DE's extra saved-data level is unwrapped once: " + unwrapped);
            try (RegionFile region = RegionIo.open(world.resolve("region/r.0.0.mca"))) {
                CompoundTag back = RegionIo.read(region, new ChunkPos(0, 0));
                helper.assertTrue(back != null && !back.contains(ChunkConverter.STAMP), "region chunk rewritten in place");
                String chunkText = back.toString();
                helper.assertTrue(chunkText.contains("hbm:canister_full") && chunkText.contains("minecraft:dyed_color"),
                        "a filled canister carries the tint Continuum's own canister writes: " + chunkText.length());
            }
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /** DE's pockets decode to one block each, and setting that block gives Continuum's pocket the same density. */
    private static void radiationPockets(GameTestHelper helper) {
        List<Radiation.Pocket> pockets = Radiation.decode(2, -3, DeFixtures.radiationPayload(4, 12.5, 30.0));
        helper.assertTrue(pockets.size() == 2, "two pockets: " + pockets);
        Radiation.Pocket west = pockets.get(0), east = pockets.get(1);
        helper.assertTrue(west.pos().equals(new BlockPos(32, 64, -48)) && west.density() == 12.5, "west pocket: " + west);
        helper.assertTrue(east.pos().equals(new BlockPos(41, 64, -48)) && east.density() == 30.0, "east pocket: " + east);

        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        ContinuumTarget.setRadiation(helper.getLevel(), pos, 30.0);
        double read = RadiationSystemNT.getRadForCoord(helper.getLevel(), pos);
        helper.assertTrue(Math.abs(read - 30.0) < 1e-6, "Continuum holds the imported density: " + read);
        ContinuumTarget.setRadiation(helper.getLevel(), pos, 0.0);
        helper.succeed();
    }

    /** Families DE saves differently, converted and then loaded and saved by Continuum's own block entities. */
    private static void blockEntityMappers(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Ctx ctx = ctx(world, null);
            Walker walker = new Walker(ctx);
            CompoundTag crucible = DeFixtures.blockEntity("hbm:crucible", BlockPos.ZERO);
            crucible.putIntArray("rec", new int[]{30, 72});
            crucible.putIntArray("was", new int[]{30, 8});
            String saved = roundTrip(helper, crucible, "hbm:machine_crucible", ctx, walker);
            helper.assertTrue(saved.contains("steel") && saved.contains("72"), "crucible keeps its molten steel: " + saved);

            CompoundTag arc = DeFixtures.blockEntity("hbm:arc_furnace_large", BlockPos.ZERO);
            arc.putIntArray("liquidMats", new int[]{30});
            arc.putIntArray("liquidAmounts", new int[]{144});
            saved = roundTrip(helper, arc, "hbm:machine_arc_furnace", ctx, walker);
            helper.assertTrue(saved.contains("m0:\"steel\"") && saved.contains("a0:144"), "arc furnace liquid steel: " + saved);

            CompoundTag pad = DeFixtures.blockEntity("hbm:launch1", BlockPos.ZERO);
            pad.put("fuel", DeFixtures.tank("hbm:kerosene", 1000, 24000));
            pad.put("oxidizer", DeFixtures.tank("hbm:peroxide", 500, 24000));
            saved = roundTrip(helper, pad, "hbm:launch_pad", ctx, walker);
            helper.assertTrue(saved.contains("t0:1000") && saved.contains("t1:500") && saved.contains("hbm:kerosene"),
                    "launch pad fuel and oxidizer: " + saved);

            CompoundTag watz = DeFixtures.blockEntity("hbm:watz", BlockPos.ZERO);
            watz.put("locks", DeFixtures.inventory(DeFixtures.slot(3, DeFixtures.stack("hbm:watz_pellet_schrabidium", 1))));
            watz.putBoolean("isLocked", true);
            saved = roundTrip(helper, watz, "hbm:watz", ctx, walker);
            helper.assertTrue(saved.contains("lock3:\"hbm:watz_pellet_schrabidium\"") && saved.contains("isLocked:1b"),
                    "Watz slot locks: " + saved);

            CompoundTag hydrotreater = DeFixtures.blockEntity("hbm:hydrotreater", BlockPos.ZERO);
            hydrotreater.put("inventory", DeFixtures.inventory(DeFixtures.slot(3, DeFixtures.stack("minecraft:iron_ingot", 1))));
            saved = roundTrip(helper, hydrotreater, "hbm:machine_hydrotreater", ctx, walker);
            helper.assertTrue(saved.contains("Slot:5") && saved.contains("minecraft:iron_ingot"), "a hydrotreater's DE slot 3 is its slot 5: " + saved);
            CompoundTag loader = DeFixtures.blockEntity("hbm:pile_loader", BlockPos.ZERO);
            loader.put("inventory", DeFixtures.inventory(DeFixtures.slot(0, DeFixtures.stack("minecraft:iron_ingot", 1))));
            loader.putBoolean("wasRedstone", true);
            saved = roundTrip(helper, loader, "hbm:pile_device", ctx, walker);
            helper.assertTrue(saved.contains("stack:{") && saved.contains("minecraft:iron_ingot") && saved.contains("redstone:1b"),
                    "a pile loader keeps one stack: " + saved);
            CompoundTag caster = DeFixtures.blockEntity("hbm:strand_caster", BlockPos.ZERO);
            caster.put("items", DeFixtures.inventory(DeFixtures.slot(0, DeFixtures.stack("minecraft:iron_ingot", 1))));
            saved = roundTrip(helper, caster, "hbm:machine_strand_caster", ctx, walker);
            helper.assertTrue(saved.contains("minecraft:iron_ingot"), "a strand caster's DE items are its inventory: " + saved);
            CompoundTag storage = DeFixtures.blockEntity("hbm:mass_storage", BlockPos.ZERO);
            storage.put("inventory", DeFixtures.inventory(DeFixtures.slot(1, DeFixtures.stack("minecraft:iron_ingot", 1))));
            storage.putInt("stockpile", 500);
            saved = roundTrip(helper, storage, "hbm:mass_storage_iron", ctx, walker);
            helper.assertTrue(saved.contains("stack:500"), "a mass storage keeps its count: " + saved);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static String roundTrip(GameTestHelper helper, CompoundTag de, String block, Ctx ctx, Walker walker) {
        CompoundTag state = DeFixtures.block(block);
        String type = ctx.target.blockEntityType(block, new CompoundTag());
        helper.assertTrue(type != null, block + " has a block entity");
        BlockEntities.convert(de, type, ctx, walker, "test");
        return load(helper, BlockPos.ZERO, state, de).saveWithoutMetadata(helper.getLevel().registryAccess()).toString();
    }

    /** A world datapack overriding DE's RBMK settings carries over into Continuum's world rules. */
    private static void rbmkDials(GameTestHelper helper) {
        Path world = tempWorld();
        MinecraftServer server = helper.getLevel().getServer();
        try {
            Path file = world.resolve("datapacks/rbmk_tweaks/data/hbm/hbm/machine_config/rbmk.json");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{\"column_height\": 6, \"disable_meltdowns\": true, \"unknown_key\": 1}");
            Ctx ctx = ctx(world, server);
            new RbmkDialsStep().run(ctx, server);
            helper.assertTrue(RBMKDials.COLUMN_HEIGHT.get(helper.getLevel()) == 6, "column height from the datapack");
            helper.assertTrue(RBMKDials.DISABLE_MELTDOWNS.get(helper.getLevel()), "meltdowns off from the datapack");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            RBMKDials.COLUMN_HEIGHT.reset(server);
            RBMKDials.DISABLE_MELTDOWNS.reset(server);
            delete(world);
        }
    }

    /** An imported cable is a block no placement hook saw; the live step adds it to the power graph. */
    private static void cablesJoinNetwork(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            BlockPos cable = new BlockPos(3, 70, 3);
            DeFixtures.setBlock(chunk, cable, DeFixtures.block("hbm:cable"));
            Ctx ctx = ctx(world, null);
            new ChunkConverter(ctx, "minecraft:overworld").convert(chunk);
            helper.assertTrue(ctx.conductors.size() == 1 && ctx.conductors.get(0).getLong("pos") == cable.asLong(),
                    "the converted cable is listed for the live step: " + ctx.conductors);

            BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
            helper.getLevel().setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("hbm:red_cable")).defaultBlockState(),
                    Block.UPDATE_CLIENTS);
            PowerGraph.get(helper.getLevel()).removeNode(pos.asLong());
            helper.assertTrue(!PowerGraph.get(helper.getLevel()).containsCell(pos.asLong()), "a cable written without its hook");
            ContinuumTarget.announceConductor(helper.getLevel(), pos);
            helper.assertTrue(PowerGraph.get(helper.getLevel()).containsCell(pos.asLong()), "announced, it is in the power graph");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /** Saved data where no chunk was saved, Continuum's own annihilator, an unknown satellite type, pack recipe names. */
    private static void satellitesAndPackRecipes(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            ListTag sats = new ListTag();
            for (String type : List.of("radar", "bogus")) {
                CompoundTag sat = new CompoundTag();
                sat.putString("type", type);
                CompoundTag outer = new CompoundTag();
                outer.put("sat", sat);
                outer.put("state", new CompoundTag());
                CompoundTag entry = new CompoundTag();
                entry.putInt("freq", sats.size() + 1);
                entry.put("sat", outer);
                sats.add(entry);
            }
            CompoundTag data = new CompoundTag();
            data.put("sats", sats);
            writeWrapped(world.resolve("dimensions/somemod/orbit"), "hbm_satellites", data);
            writeWrapped(world.resolve("dimensions/somemod/orbit"), "hbm_annihilator", new CompoundTag());
            CompoundTag annihilator = new CompoundTag();
            annihilator.put("pools", new ListTag());
            CompoundTag own = new CompoundTag();
            own.put("data", annihilator);
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(own, world.resolve("data/hbm_annihilator.dat"));
            Ctx ctx = ctx(world, helper.getLevel().getServer());
            new SavedDataStep().run(ctx, null);
            helper.assertTrue(Files.isRegularFile(world.resolve("data/hbm_annihilator.dat")), "Continuum's own annihilator stays");
            helper.assertTrue(!Files.exists(world.resolve("dimensions/somemod/orbit/data/hbm_annihilator.dat")), "DE's is parked");
            CompoundTag out = NbtIo.readCompressed(world.resolve("dimensions/somemod/orbit/data/hbm_satellites.dat"), NbtAccounter.unlimitedHeap());
            SatelliteSavedData loaded = SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE, out.getCompound("data")).getOrThrow();
            helper.assertTrue(loaded.getSatFromFreq(1) != null && loaded.getSatFromFreq(2) == null, "the radar loads, the unknown type is gone: " + out);
            helper.assertTrue(ctx.parking.counts().getOrDefault("satellite", 0) == 1, "and parked: " + ctx.parking.counts());
            ctx.flushGlobal();
            CompoundTag parkedOriginal = null;
            for (Tag t : new Parking(world).records()) {
                CompoundTag record = (CompoundTag) t;
                if (record.getString("where").equals("somemod:orbit hbm_satellites") && record.getString("kind").equals("saved_data")) parkedOriginal = record;
            }
            ListTag originalSats = parkedOriginal == null ? new ListTag()
                    : parkedOriginal.getCompound("nbt").getCompound("data").getCompound("data").getList("sats", Tag.TAG_COMPOUND);
            helper.assertTrue(originalSats.size() == 2 && originalSats.getCompound(1).getCompound("sat").contains("sat", Tag.TAG_COMPOUND),
                    "the leftovers keep DE's file as DE saved it, both satellites nested: " + parkedOriginal);

            CompoundTag assembler = DeFixtures.blockEntity("hbm:assemblymachine", BlockPos.ZERO);
            assembler.putString("recipe0", "ass.package.water");
            String type = ctx.target.blockEntityType("hbm:machine_assembly_machine", new CompoundTag());
            BlockEntities.convert(assembler, type, ctx, new Walker(ctx), "test assembler");
            helper.assertTrue(assembler.getString("recipe0").equals("ass.packagehbmfluid.water"), "the water pack recipe: " + assembler);
            CompoundTag factory = DeFixtures.blockEntity("hbm:assemblyfactory", BlockPos.ZERO);
            factory.putString("recipe2", "ass.unpackage.water");
            BlockEntities.convert(factory, ctx.target.blockEntityType("hbm:machine_assembly_factory", new CompoundTag()), ctx, new Walker(ctx), "test factory");
            helper.assertTrue(factory.getString("recipe2").equals("ass.unpackagehbmfluid.water"), "the factory's unpack recipe: " + factory);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /** Starts as vanilla loads them: a piece that cannot load is parked, the start stays while one is left. */
    private static void jigsawPieces(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Ctx ctx = ctx(world, helper.getLevel().getServer());
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            String house = "minecraft:village/plains/houses/plains_small_house_1";
            CompoundTag starts = new CompoundTag();
            starts.put("minecraft:village_plains", start("minecraft:village_plains", piece(house, "minecraft:empty"),
                    piece(house, "hbm:multiblock_assembly")));
            starts.put("minecraft:village_desert", start("minecraft:village_desert", piece(house, "hbm:multiblock_assembly")));
            starts.put("minecraft:village_savanna", start("minecraft:village_savanna", piece("hbmspace:duna_comms", "minecraft:empty")));
            CompoundTag structures = new CompoundTag();
            structures.put("starts", starts);
            chunk.put("structures", structures);
            new ChunkConverter(ctx, "minecraft:overworld").convert(chunk);
            CompoundTag kept = chunk.getCompound("structures").getCompound("starts");
            helper.assertTrue(kept.getCompound("minecraft:village_plains").getList("Children", Tag.TAG_COMPOUND).size() == 1
                    && kept.contains("minecraft:village_savanna") && !kept.contains("minecraft:village_desert"),
                    "the loadable pieces stay with their starts: " + kept);
            helper.assertTrue(ctx.parking.counts().getOrDefault("structure_piece", 0) == 2
                    && ctx.parking.counts().getOrDefault("structure", 0) == 1, "the rest is parked: " + ctx.parking.counts());
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static CompoundTag piece(String location, String processors) {
        CompoundTag element = new CompoundTag();
        element.putString("element_type", "minecraft:single_pool_element");
        element.putString("location", location);
        element.putString("processors", processors);
        element.putString("projection", "rigid");
        CompoundTag piece = new CompoundTag();
        piece.putString("id", "minecraft:jigsaw");
        piece.put("pool_element", element);
        return piece;
    }

    private static CompoundTag start(String id, CompoundTag... pieces) {
        ListTag children = new ListTag();
        children.addAll(List.of(pieces));
        CompoundTag start = new CompoundTag();
        start.putString("id", id);
        start.putInt("ChunkX", 0);
        start.putInt("ChunkZ", 0);
        start.put("Children", children);
        return start;
    }

    /**
     * An addon left for later (a stand-in id, the same with NTM: Space loaded) keeps its data in DE's shape and nothing
     * waits on it; an import converting with an addon waits for it, -Dhbm.deimport.without or not.
     */
    private static void deferredAddons(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Path mun = world.resolve("dimensions/testaddon/mun");
            writeChunk(mun, DeFixtures.emptyChunk(0, 0));
            CompoundTag data = new CompoundTag();
            data.put("sats", new ListTag());
            writeWrapped(mun, "hbm_satellites", data);
            Files.createDirectories(mun.resolve("sublevels"));
            ImportState state = ImportState.create(world, DeFixtures.DE_VERSION, 7, "test");
            state.put("deferred_addons", "testaddon");
            Ctx ctx = ctx(world, null);
            DeImport.deferred(state, ctx);
            helper.assertTrue(ctx.deferredAddons.contains("testaddon"), "the addon is still left for later: " + ctx.deferredAddons);
            new RegionsStep().run(ctx, null);
            new SavedDataStep().run(ctx, null);
            SubLevelsStep airships = new SubLevelsStep();
            airships.run(ctx, null);
            helper.assertTrue(airships.waitsFor(ctx) == null && Files.isDirectory(mun.resolve("sublevels"))
                    && !ctx.journal.done("sublevels@1", world, mun.resolve("sublevels")), "its airships are left to its converter, and nothing waits");
            CompoundTag props = new CompoundTag();
            props.putFloat("activation", 3.5F);
            CompoundTag attachments = new CompoundTag();
            attachments.put("hbm:living_props", props);
            CompoundTag player = new CompoundTag();
            player.put("neoforge:attachments", attachments);
            Entities.living(player, ctx, "test player");
            helper.assertTrue(player.getCompound("neoforge:attachments").getCompound("hbm:living_props").get("neutrons") instanceof DoubleTag,
                    "neutron activation is the double Continuum reads, with or without the addon: " + player);
            CompoundTag chunk;
            try (RegionFile region = RegionIo.open(mun.resolve("region/r.0.0.mca"))) {
                chunk = RegionIo.read(region, new ChunkPos(0, 0));
            }
            helper.assertTrue(chunk != null && chunk.contains(ChunkConverter.STAMP), "Mun keeps DE's stamp for NTM: Space");
            CompoundTag saved = NbtIo.readCompressed(mun.resolve("data/hbm_satellites.dat"), NbtAccounter.unlimitedHeap());
            helper.assertTrue(saved.getCompound("data").contains("data", Tag.TAG_COMPOUND), "and its saved data stays DE's");
            state.put("addons", "hbmspace");
            String why = DeImport.missingAddon(state);
            helper.assertTrue(DeAddons.has("hbmspace") ? why == null : why != null && why.contains("NTM: Space"),
                    "an import begun with NTM: Space waits for it: " + why);
            System.setProperty("hbm.deimport.without", "hbmspace");
            try {
                helper.assertTrue(Objects.equals(DeImport.missingAddon(state), why), "-Dhbm.deimport.without=hbmspace cannot take it away halfway");
            } finally {
                System.clearProperty("hbm.deimport.without");
            }
            state.put("addons", "");
            state.put("converting_addons", "hbmspace");
            String converting = DeImport.missingAddon(state);
            helper.assertTrue(DeAddons.has("hbmspace") ? converting == null : converting != null,
                    "nor can an addon left for later once its converter has begun: " + converting);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /**
     * What a late addon converter must leave alone: stacks an earlier import converted, inside a block entity of the
     * addon (a stand-in id), and the records an earlier run left for its airships, in any unit's shard.
     */
    private static void lateWalkerAndRecords(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Ctx ctx = ctx(world, null);
            CompoundTag box = new CompoundTag();
            box.putString("id", "testaddon:box");
            ListTag items = new ListTag();
            items.add(DeFixtures.stack("hbm:capacitor_copper", 1));
            box.put("Items", items);
            CompoundTag control = box.copy();
            new Walker(ctx, id -> id.startsWith("testaddon:")).walk(box, "test");
            new Walker(ctx).walk(control, "test");
            helper.assertTrue(box.getList("Items", Tag.TAG_COMPOUND).getCompound(0).getString("id").equals("hbm:capacitor_copper")
                    && !control.getList("Items", Tag.TAG_COMPOUND).getCompound(0).getString("id").equals("hbm:capacitor_copper"),
                    "Continuum's capacitor is not taken for DE's a second time: " + box + " " + control);

            Unit unit = ctx.begin("airships testaddon:duna r.0.0.0.slvls");
            CompoundTag cells = new CompoundTag();
            cells.putString("dim", "testaddon:duna");
            cells.putLong("pos", 0L);
            ctx.add("plotcells", cells);
            CompoundTag cable = new CompoundTag();
            cable.putString("dim", "testaddon:duna");
            cable.putLong("pos", 1L);
            ctx.add("conductors", cable);
            ctx.commit(unit);
            ctx.flushGlobal();
            Ctx next = ctx(world, null);
            int dropped = next.dropEarlier((kind, t) -> t.getString("dim").equals("testaddon:duna") && kind.equals("plotcells"));
            Ctx after = ctx(world, null);
            helper.assertTrue(dropped == 1 && after.list("plotcells").isEmpty() && after.list("conductors").size() == 1,
                    "the stale record leaves its unit's shard, the other stays: " + dropped + " " + after.list("conductors"));
            helper.assertTrue(new Parking(world).records().stream().anyMatch(t -> ((CompoundTag) t).getString("kind").equals("live_record")),
                    "and is kept in the leftovers");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /**
     * A DE addon's content a start without its converter lost, put back from DE's copy (a stand-in addon id): a block
     * where the chunk now has air, never over one built since or for HBM's own ids; container, item frame and armor
     * stand stacks into empty places only; and a ship's block where its plot has room.
     */
    private static void backupMerge(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Ctx ctx = ctx(world, null);
            Predicate<String> addon = id -> id.startsWith("testaddon:");
            BlockPos lost = new BlockPos(1, 70, 1), built = new BlockPos(2, 70, 2), own = new BlockPos(3, 70, 3), chest = new BlockPos(4, 70, 4);
            CompoundTag backed = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(backed, lost, DeFixtures.block("testaddon:machine"));
            DeFixtures.setBlock(backed, built, DeFixtures.block("testaddon:machine"));
            DeFixtures.setBlock(backed, own, DeFixtures.block("hbm:block_steel"));
            DeFixtures.setBlock(backed, chest, DeFixtures.block("minecraft:chest"));
            CompoundTag box = DeFixtures.blockEntity("minecraft:chest", chest);
            ListTag items = new ListTag();
            items.add(DeFixtures.slot(0, DeFixtures.stack("testaddon:part", 1)));
            items.add(DeFixtures.slot(1, DeFixtures.stack("testaddon:part", 2)));
            CompoundTag shulker = DeFixtures.stack("minecraft:shulker_box", 1);
            CompoundTag inside = new CompoundTag();
            ListTag contents = new ListTag();
            CompoundTag inShulker = new CompoundTag();
            inShulker.putInt("slot", 0);
            inShulker.put("item", DeFixtures.stack("testaddon:part", 1));
            contents.add(inShulker);
            inside.put("minecraft:container", contents);
            shulker.put("components", inside);
            items.add(DeFixtures.slot(2, shulker));
            box.put("Items", items);
            DeFixtures.addBlockEntity(backed, box);
            CompoundTag live = DeFixtures.emptyChunk(0, 0);
            live.remove("hbm_data_version");
            DeFixtures.setBlock(live, built, DeFixtures.block("minecraft:stone"));
            DeFixtures.setBlock(live, chest, DeFixtures.block("minecraft:chest"));
            CompoundTag kept = DeFixtures.blockEntity("minecraft:chest", chest);
            ListTag keptItems = new ListTag();
            keptItems.add(DeFixtures.slot(0, DeFixtures.stack("minecraft:stone", 1)));
            kept.put("Items", keptItems);
            DeFixtures.addBlockEntity(live, kept);
            live.put("Heightmaps", new CompoundTag());
            live.putBoolean("isLightOn", true);
            CompoundTag untouched = live.copy();

            int stacks = BackupMerge.containers(ctx, live, backed, new Walker(ctx), addon, "test");
            int blocks = BackupMerge.blocks(ctx, "minecraft:overworld", live, backed, addon);
            helper.assertTrue(blocks == 1 && ChunkBlocks.get(live, lost).getString("Name").equals("testaddon:machine")
                    && ChunkBlocks.get(live, built).getString("Name").equals("minecraft:stone")
                    && ChunkBlocks.get(live, own).getString("Name").equals("minecraft:air"),
                    "only the addon's block where there is room comes back: " + blocks);
            helper.assertTrue(stacks == 1 && kept.getList("Items", Tag.TAG_COMPOUND).size() == 2
                    && kept.getList("Items", Tag.TAG_COMPOUND).getCompound(0).getString("id").equals("minecraft:stone"),
                    "the chest gets back only the addon stack whose slot is empty, not a stack holding one: " + kept);
            helper.assertTrue(!live.contains("Heightmaps") && !live.getBoolean("isLightOn"), "a chunk that got blocks back is lit again on load");
            CompoundTag lacking = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(lacking, lost, DeFixtures.block("testaddon:machine"));
            untouched.getList("sections", Tag.TAG_COMPOUND).removeIf(t -> ((CompoundTag) t).getByte("Y") == lost.getY() >> 4);
            helper.assertTrue(BackupMerge.blocks(ctx, "minecraft:overworld", untouched, lacking, addon) == 0
                    && untouched.contains("Heightmaps") && untouched.getBoolean("isLightOn"), "one that got none keeps its light");

            String crateName = BlockStates.convert(DeFixtures.block("hbm:crate_iron"), ctx.target).entry().getString("Name");
            CompoundTag crateBefore = DeFixtures.emptyChunk(0, 0), crateNow = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(crateBefore, chest, DeFixtures.block("hbm:crate_iron"));
            DeFixtures.setBlock(crateNow, chest, DeFixtures.block(crateName));
            CompoundTag crate = DeFixtures.blockEntity("hbm:crate_iron", chest), crateKept = DeFixtures.blockEntity("hbm:crate_iron", chest);
            crate.put("inventory", DeFixtures.inventory(DeFixtures.slot(3, DeFixtures.stack("testaddon:part", 1))));
            crateKept.put("inventory", DeFixtures.inventory());
            DeFixtures.addBlockEntity(crateBefore, crate);
            DeFixtures.addBlockEntity(crateNow, crateKept);
            CompoundTag furnaceNow = crateNow.copy();
            DeFixtures.setBlock(furnaceNow, chest, DeFixtures.block("minecraft:furnace"));
            helper.assertTrue(BackupMerge.containers(ctx, crateNow, crateBefore, new Walker(ctx), addon, "test") == 1
                    && crateKept.getCompound("inventory").getList("Items", Tag.TAG_COMPOUND).size() == 1,
                    "an HBM crate gets its stack back into its inventory: " + crateKept);
            helper.assertTrue(BackupMerge.containers(ctx, furnaceNow, crateBefore, new Walker(ctx), addon, "test") == 0,
                    "a container replaced since takes nothing");

            // DE's slot 3 of a hydrotreater is Continuum's slot 5; a piston inserter keeps its one stack apart
            BlockPos inserter = new BlockPos(5, 70, 5);
            CompoundTag machinesBefore = DeFixtures.emptyChunk(0, 0), machinesNow = DeFixtures.emptyChunk(0, 0);
            for (BlockPos pos : List.of(chest, inserter)) {
                CompoundTag block = DeFixtures.block(pos == chest ? "hbm:machine_hydrotreater" : "hbm:piston_inserter");
                DeFixtures.setBlock(machinesBefore, pos, block);
                DeFixtures.setBlock(machinesNow, pos, BlockStates.convert(block, ctx.target).entry());
            }
            CompoundTag hydrotreater = DeFixtures.blockEntity("hbm:hydrotreater", chest), hydrotreaterNow = DeFixtures.blockEntity("hbm:machine_hydrotreater", chest);
            hydrotreater.put("inventory", DeFixtures.inventory(DeFixtures.slot(3, DeFixtures.stack("testaddon:part", 1))));
            hydrotreaterNow.put("inventory", DeFixtures.inventory(DeFixtures.slot(3, DeFixtures.stack("minecraft:stone", 1))));
            CompoundTag pusher = DeFixtures.blockEntity("hbm:piston_inserter", inserter), pusherNow = DeFixtures.blockEntity("hbm:piston_inserter", inserter);
            pusher.put("inventory", DeFixtures.inventory(DeFixtures.slot(0, DeFixtures.stack("testaddon:part", 1))));
            DeFixtures.addBlockEntity(machinesBefore, hydrotreater);
            DeFixtures.addBlockEntity(machinesBefore, pusher);
            DeFixtures.addBlockEntity(machinesNow, hydrotreaterNow);
            DeFixtures.addBlockEntity(machinesNow, pusherNow);
            int machines = BackupMerge.containers(ctx, machinesNow, machinesBefore, new Walker(ctx), addon, "test");
            ListTag hydrotreaterItems = hydrotreaterNow.getCompound("inventory").getList("Items", Tag.TAG_COMPOUND);
            helper.assertTrue(machines == 2 && hydrotreaterItems.stream().anyMatch(t -> ((CompoundTag) t).getByte("Slot") == 5
                            && ((CompoundTag) t).getString("id").equals("testaddon:part")) && pusherNow.getCompound("stack").getString("id").equals("testaddon:part"),
                    "machines get their stacks back where Continuum keeps them: " + machines + " " + hydrotreaterNow + " " + pusherNow);

            CompoundTag storageNow = new CompoundTag(), storageBefore = DeFixtures.blockEntity("hbm:mass_storage", chest);
            storageNow.put("inventory", DeFixtures.inventory());
            storageNow.putInt("stack", 0);
            storageNow.putInt("capacity", 300);
            storageBefore.put("inventory", DeFixtures.inventory(DeFixtures.slot(1, DeFixtures.stack("testaddon:part", 1))));
            storageBefore.putInt("stockpile", 500);
            helper.assertTrue(BackupMerge.stacks(storageNow, BlockEntities.layout(storageBefore), new Walker(ctx), addon, "test") == 1
                    && storageNow.getInt("stack") == 300, "a mass storage gets its type back with its count, up to what it holds: " + storageNow);
            CompoundTag reused = new CompoundTag(), reusedBefore = BlockEntities.layout(storageBefore);
            reused.put("inventory", DeFixtures.inventory(DeFixtures.slot(1, DeFixtures.stack("minecraft:diamond", 1))));
            reused.putInt("stack", 0);
            reused.putInt("capacity", 300);
            reusedBefore.getCompound("inventory").getList("Items", Tag.TAG_COMPOUND).add(DeFixtures.slot(2, DeFixtures.stack("testaddon:part", 64)));
            helper.assertTrue(BackupMerge.stacks(reused, reusedBefore, new Walker(ctx), addon, "test") == 1 && reused.getInt("stack") == 0,
                    "one given another type since keeps its count: " + reused);
            CompoundTag pedestal = new CompoundTag(), pedestalBefore = new CompoundTag();
            pedestalBefore.put("item", DeFixtures.stack("testaddon:part", 1));
            helper.assertTrue(BackupMerge.stacks(pedestal, pedestalBefore, new Walker(ctx), addon, "test") == 1
                    && pedestal.getCompound("item").getString("id").equals("testaddon:part"), "a pedestal gets its item back: " + pedestal);

            // the first import set aside a kept stack's component holding the addon's stacks; a diamond stands in for one
            Predicate<String> diamond = id -> id.equals("minecraft:diamond");
            CompoundTag full = DeFixtures.stack("minecraft:shulker_box", 1), fullComponents = new CompoundTag(), inBox = new CompoundTag();
            ListTag boxContents = new ListTag();
            inBox.putInt("slot", 0);
            inBox.put("item", DeFixtures.stack("minecraft:diamond", 3));
            boxContents.add(inBox);
            fullComponents.put("minecraft:container", boxContents);
            full.put("components", fullComponents);
            CompoundTag boxChest = new CompoundTag(), boxChestBefore = new CompoundTag(), shown = new CompoundTag(), shownBefore = new CompoundTag();
            ListTag boxNow = new ListTag(), boxThen = new ListTag();
            boxNow.add(DeFixtures.slot(2, DeFixtures.stack("minecraft:shulker_box", 1)));
            boxThen.add(DeFixtures.slot(2, full.copy()));
            boxChest.put("Items", boxNow);
            boxChestBefore.put("Items", boxThen);
            shown.put("Item", DeFixtures.stack("minecraft:shulker_box", 1));
            shownBefore.put("Item", full.copy());
            int refilled = BackupMerge.stacks(boxChest, boxChestBefore, new Walker(ctx), diamond, "test") + BackupMerge.stacks(shown, shownBefore, new Walker(ctx), diamond, "test");
            helper.assertTrue(refilled == 2 && boxChest.getList("Items", Tag.TAG_COMPOUND).size() == 1
                            && boxChest.getList("Items", Tag.TAG_COMPOUND).getCompound(0).getCompound("components").toString().contains("minecraft:diamond")
                            && shown.getCompound("Item").getCompound("components").toString().contains("minecraft:diamond"),
                    "a shulker box the game kept gets back what it held: " + boxChest + " " + shown);
            helper.assertTrue(BackupMerge.stacks(boxChest, boxChestBefore, new Walker(ctx), diamond, "test") == 0, "and only once");
            CompoundTag grown = new CompoundTag();
            grown.put("Item", DeFixtures.stack("minecraft:shulker_box", 2));
            helper.assertTrue(BackupMerge.stacks(grown, shownBefore, new Walker(ctx), diamond, "test") == 0,
                    "a stack grown since gets nothing, as more items would share what one held");

            CompoundTag frame = new CompoundTag(), frameBefore = new CompoundTag(), filled = new CompoundTag();
            frameBefore.put("Item", DeFixtures.stack("testaddon:part", 1));
            filled.put("Item", DeFixtures.stack("minecraft:stone", 1));
            helper.assertTrue(BackupMerge.stacks(filled, frameBefore, new Walker(ctx), addon, "test") == 0, "a frame holding something keeps it");
            CompoundTag boxFrame = new CompoundTag();
            boxFrame.put("Item", shulker.copy());
            helper.assertTrue(BackupMerge.stacks(new CompoundTag(), boxFrame, new Walker(ctx), addon, "test") == 0,
                    "a stack only holding the addon's, which the game kept, is not given again");
            CompoundTag stand = new CompoundTag(), standBefore = new CompoundTag();
            ListTag armor = new ListTag(), armorBefore = new ListTag();
            for (int i = 0; i < 4; i++) armor.add(i == 2 ? DeFixtures.stack("minecraft:iron_chestplate", 1) : new CompoundTag());
            for (int i = 0; i < 4; i++) armorBefore.add(i >= 2 ? DeFixtures.stack(i == 3 ? "testaddon:helmet" : "testaddon:chestplate", 1) : new CompoundTag());
            stand.put("ArmorItems", armor);
            standBefore.put("ArmorItems", armorBefore);
            int held = BackupMerge.stacks(frame, frameBefore, new Walker(ctx), addon, "test") + BackupMerge.stacks(stand, standBefore, new Walker(ctx), addon, "test");
            helper.assertTrue(held == 2 && frame.getCompound("Item").getString("id").equals("testaddon:part")
                    && stand.getList("ArmorItems", Tag.TAG_COMPOUND).getCompound(3).getString("id").equals("testaddon:helmet")
                    && stand.getList("ArmorItems", Tag.TAG_COMPOUND).getCompound(2).getString("id").equals("minecraft:iron_chestplate"),
                    "the item frame and armor stand get theirs back where the slot is empty: " + frame + " " + stand);

            BlockPos high = new BlockPos(1, 100, 1);
            CompoundTag shipBefore = backed.copy(), liveChunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(shipBefore, high, DeFixtures.block("testaddon:machine"));
            DeFixtures.setBlock(liveChunk, built, DeFixtures.block("minecraft:stone"));
            DeFixtures.setBlock(liveChunk, chest, DeFixtures.block("minecraft:chest"));
            CompoundTag shipBox = DeFixtures.blockEntity("minecraft:chest", chest);
            ListTag shipBoxItems = new ListTag();
            shipBoxItems.add(DeFixtures.slot(0, DeFixtures.stack("minecraft:stone", 1)));
            shipBox.put("Items", shipBoxItems);
            DeFixtures.addBlockEntity(liveChunk, shipBox);
            DeFixtures.setBlock(shipBefore, inserter, DeFixtures.block("hbm:machine_hydrotreater"));
            DeFixtures.setBlock(liveChunk, inserter, BlockStates.convert(DeFixtures.block("hbm:machine_hydrotreater"), ctx.target).entry());
            CompoundTag shipHydrotreater = hydrotreater.copy(), shipHydrotreaterNow = DeFixtures.blockEntity("hbm:machine_hydrotreater", inserter);
            shipHydrotreater.putInt("x", inserter.getX());
            shipHydrotreater.putInt("z", inserter.getZ());
            shipHydrotreaterNow.put("inventory", DeFixtures.inventory(DeFixtures.slot(3, DeFixtures.stack("minecraft:stone", 1))));
            DeFixtures.addBlockEntity(shipBefore, shipHydrotreater);
            DeFixtures.addBlockEntity(liveChunk, shipHydrotreaterNow);
            CompoundTag liveShip = DeFixtures.ship(liveChunk), backedShip = DeFixtures.ship(shipBefore);
            CompoundTag liveChunks = liveShip.getCompound("plot").getCompound("chunks"), backedChunks = backedShip.getCompound("plot").getCompound("chunks");
            liveChunks.getCompound(Long.toString(ChunkPos.asLong(0, 0))).put("heightmaps", new CompoundTag());
            liveChunks.getCompound(Long.toString(ChunkPos.asLong(0, 0))).getCompound("sections").getCompound(Integer.toString((lost.getY() >> 4) + 4))
                    .putByteArray("SkyLight", new byte[2048]);
            // a chunk nothing comes back to keeps its heightmaps, and one only the backup has without the addon's blocks stays out
            CompoundTag bare = DeFixtures.ship(DeFixtures.emptyChunk(0, 0)).getCompound("plot").getCompound("chunks").getCompound(Long.toString(ChunkPos.asLong(0, 0)));
            bare.put("heightmaps", new CompoundTag());
            String quiet = Long.toString(ChunkPos.asLong(3, 0)), empty = Long.toString(ChunkPos.asLong(2, 0));
            liveChunks.put(quiet, bare.copy());
            backedChunks.put(quiet, bare.copy());
            backedChunks.put(empty, bare.copy());
            String gone = Long.toString(ChunkPos.asLong(1, 0));
            backedShip.getCompound("plot").getCompound("chunks").put(gone, DeFixtures.ship(backed.copy()).getCompound("plot").getCompound("chunks").getCompound(Long.toString(ChunkPos.asLong(0, 0))));
            CompoundTag liveSections = liveShip.getCompound("plot").getCompound("chunks").getCompound(Long.toString(ChunkPos.asLong(0, 0))).getCompound("sections");
            String index = Integer.toString((high.getY() >> 4) + 4);
            liveSections.remove(index);
            int onShip = BackupMerge.ship(ctx, "minecraft:overworld", liveShip, backedShip, addon, "test ship");
            CompoundTag shipChunk = liveShip.getCompound("plot").getCompound("chunks").getCompound(Long.toString(ChunkPos.asLong(0, 0)));
            ListTag shipItems = shipChunk.getList("block_entities", Tag.TAG_COMPOUND).getCompound(0).getList("Items", Tag.TAG_COMPOUND);
            CompoundTag goneChunk = liveShip.getCompound("plot").getCompound("chunks").getCompound(gone);
            CompoundTag shipMachine = shipChunk.getList("block_entities", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag) t)
                    .filter(t -> t.getString("id").equals("hbm:machine_hydrotreater")).findFirst().orElseGet(CompoundTag::new);
            helper.assertTrue(shipMachine.getCompound("inventory").getList("Items", Tag.TAG_COMPOUND).stream()
                    .anyMatch(t -> ((CompoundTag) t).getByte("Slot") == 5), "a machine on the ship gets its stack where Continuum keeps it: " + shipMachine);
            helper.assertTrue(!shipChunk.getCompound("sections").getCompound(Integer.toString((lost.getY() >> 4) + 4)).contains("SkyLight")
                            && liveChunks.getCompound(quiet).contains("heightmaps") && !liveChunks.contains(empty),
                    "a ship chunk that got blocks back is lit again, the others stay as they were: " + liveChunks.getAllKeys());
            CompoundTag elsewhere = backedShip.copy(), still = liveShip.copy();
            elsewhere.getCompound("plot").putInt("plot_x", 1);
            helper.assertTrue(BackupMerge.ship(ctx, "minecraft:overworld", still, elsewhere, addon, "test ship") == 0 && still.equals(liveShip),
                    "a copy in another plot is not this ship");
            helper.assertTrue(onShip == 2 + 2 + 2 && new PackedSection(shipChunk.getCompound("sections").getCompound(index).getCompound("block_states")).palette.stream()
                    .anyMatch(t -> ((CompoundTag) t).getString("Name").equals("testaddon:machine")) && shipItems.size() == 2,
                    "the ship gets back its blocks where it has room, one into a section Sable did not save as it was all air, and its chest's stack: "
                            + onShip + " " + shipItems);
            helper.assertTrue(!goneChunk.isEmpty() && goneChunk.getCompound("sections").getAllKeys().size() == 1 && !shipChunk.contains("heightmaps"),
                    "a chunk Sable left out comes back with the addon's blocks only: " + goneChunk.getCompound("sections").getAllKeys());
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    /** A machine of the addon's comes back onto a ship with the cells its plot has room for, and no others listed. */
    private static void backupMergeCells(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Ctx ctx = ctx(world, helper.getLevel().getServer());
            BlockPos core = new BlockPos((10000 << 7 << 4) + 2, 70, (10000 << 7 << 4) + 2);
            CompoundTag machine = DeFixtures.block("hbm:machine_centrifuge", "facing", "north");
            CompoundTag converted = BlockStates.convert(machine, ctx.target).entry();
            List<Long> cells = ctx.target.footprint(converted.getString("Name"), converted.getCompound("Properties"), core.asLong(), ctx.rbmkColumnHeight())
                    .keySet().stream().filter(c -> BlockPos.of(c).getX() >> 4 == core.getX() >> 4 && BlockPos.of(c).getZ() >> 4 == core.getZ() >> 4).toList();
            BlockPos taken = BlockPos.of(cells.get(0));
            CompoundTag before = DeFixtures.emptyChunk(0, 0), now = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(before, core, machine);
            DeFixtures.setBlock(now, taken, DeFixtures.block("minecraft:stone"));
            CompoundTag liveShip = DeFixtures.ship(now);
            int back = BackupMerge.ship(ctx, "minecraft:overworld", liveShip, DeFixtures.ship(before), id -> id.equals("hbm:machine_centrifuge"), "test ship");
            CompoundTag record = ctx.plotCells.stream().filter(t -> t.getLong("pos") == core.asLong()).findFirst().orElse(null);
            CompoundTag chunk = liveShip.getCompound("plot").getCompound("chunks").getCompound(Long.toString(ChunkPos.asLong(0, 0)));
            helper.assertTrue(back > 0 && record != null && cells.size() > 1
                    && Arrays.stream(record.getLongArray("cells")).noneMatch(c -> c == taken.asLong())
                    && Arrays.stream(record.getLongArray("cells")).anyMatch(c -> cells.contains(c)),
                    "the cell where the ship has a block now is left out: " + record + " of " + cells);
            helper.assertTrue(plotBlock(chunk, taken).equals("minecraft:stone") && plotBlock(chunk, core).equals(converted.getString("Name")),
                    "and the block there stays");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static String plotBlock(CompoundTag chunk, BlockPos pos) {
        PackedSection section = new PackedSection(chunk.getCompound("sections").getCompound(Integer.toString((pos.getY() >> 4) + 4)).getCompound("block_states"));
        return ((CompoundTag) section.palette.get(section.index[PackedSection.indexOf(pos.getX(), pos.getY(), pos.getZ())])).getString("Name");
    }

    /**
     * Sable keys a plot's chunks by their place in the plot and its block entities by their place in the level: what
     * the converter adds to a ship (a cable's block entity, its record) is placed where Sable will look.
     */
    private static void airshipPlotPositions(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            Ctx ctx = ctx(world, null);
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(chunk, new BlockPos(1, 64, 1), DeFixtures.block("hbm:cable"));
            CompoundTag ship = DeFixtures.ship(chunk);
            ship.getCompound("plot").putInt("plot_x", 2);
            Unit unit = ctx.begin("test ship");
            Airships.convert(ctx, "minecraft:overworld", ship, "test ship");
            int x = (10000 + 2) << 7 << 4, z = 10000 << 7 << 4;
            ListTag entities = ship.getCompound("plot").getCompound("chunks").getCompound(Long.toString(ChunkPos.asLong(0, 0))).getList("block_entities", Tag.TAG_COMPOUND);
            helper.assertTrue(entities.size() == 1 && entities.getCompound(0).getInt("x") == x + 1 && entities.getCompound(0).getInt("z") == z + 1,
                    "the cable's block entity is at the cable's place in the level: " + entities);
            ctx.commit(unit);
            helper.assertTrue(ctx.conductors.size() == 1 && ctx.conductors.get(0).getLong("pos") == BlockPos.asLong(x + 1, 64, z + 1),
                    "and so is its record: " + ctx.conductors);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static void writeChunk(Path world, CompoundTag chunk) throws IOException {
        Path file = world.resolve("region/r.0.0.mca");
        Files.createDirectories(file.getParent());
        ChunkPos pos = new ChunkPos(chunk.getInt("xPos"), chunk.getInt("zPos"));
        try (RegionFile region = RegionIo.open(file); DataOutputStream out = region.getChunkDataOutputStream(pos)) {
            NbtIo.write(chunk, out);
        }
    }

    /** DE's ordinal pollution list and nested satellites, wrapped as DE writes them, decode in Continuum's codecs. */
    private static void savedDataShapes(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            CompoundTag entry = new CompoundTag();
            entry.putInt("chunkX", 3);
            entry.putInt("chunkZ", -1);
            ListTag values = new ListTag();
            for (float f : new float[]{12.5F, 0F, 3F, 0.25F}) values.add(FloatTag.valueOf(f));
            entry.put("values", values);
            ListTag entries = new ListTag();
            entries.add(entry);
            CompoundTag pollution = new CompoundTag();
            pollution.put("entries", entries);
            writeWrapped(world, "hbm_pollution", pollution);

            CompoundTag sat = new CompoundTag();
            sat.putString("type", "death_ray");
            CompoundTag state = new CompoundTag();
            state.putInt("targetX", 40);
            CompoundTag nested = new CompoundTag();
            nested.put("sat", sat);
            nested.put("state", state);
            CompoundTag satEntry = new CompoundTag();
            satEntry.putInt("freq", 77);
            satEntry.put("sat", nested);
            ListTag sats = new ListTag();
            sats.add(satEntry);
            CompoundTag satellites = new CompoundTag();
            satellites.put("sats", sats);
            writeWrapped(world, "hbm_satellites", satellites);

            Ctx ctx = ctx(world, null);
            new SavedDataStep().run(ctx, null);
            CompoundTag p = NbtIo.readCompressed(world.resolve("data/hbm_pollution.dat"), NbtAccounter.unlimitedHeap()).getCompound("data");
            PollutionSavedData decoded = PollutionSavedData.CODEC.parse(NbtOps.INSTANCE, p).getOrThrow();
            PollutionSavedData.PollutionData region = decoded.pollution.get(new ChunkPos(3, -1));
            helper.assertTrue(region != null && region.get(PollutionType.SOOT) == 12.5F && region.get(PollutionType.HEAVYMETAL) == 3F
                    && region.get(PollutionType.FALLOUT) == 0.25F, "pollution keeps every type: " + p);
            CompoundTag s = NbtIo.readCompressed(world.resolve("data/hbm_satellites.dat"), NbtAccounter.unlimitedHeap()).getCompound("data");
            CompoundTag converted = s.getList("sats", Tag.TAG_COMPOUND).getCompound(0).getCompound("sat");
            Satellite decodedSat = SatelliteType.DISPATCH_CODEC.parse(NbtOps.INSTANCE, converted).getOrThrow();
            helper.assertTrue(decodedSat.type() == SatelliteType.DEATH_RAY, "the death ray satellite decodes: " + s);
            SatelliteSavedData loaded = SatelliteSavedData.CODEC.parse(NbtOps.INSTANCE, s).getOrThrow();
            helper.assertTrue(loaded.getSatFromFreq(77) != null && loaded.getSatFromFreq(77).targetX == 40,
                    "its DE state's target is Continuum's: " + s);
            helper.assertTrue(ctx.parking.counts().getOrDefault("satellite_state", 0) == 0, "nothing of its state is parked");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
        helper.succeed();
    }

    /** A Sable airship's plot: its chunk converts, its multiblocks get their cells there, and what it carries converts. */
    private static void airshipPlot(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            BlockPos origin = new BlockPos(2, 70, 2);
            DeFixtures.writeKey(world);
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.standardContent(chunk, origin);
            chunk.remove("xPos");
            chunk.remove("zPos");
            // Sable keys sections by index above the lowest section, with no Y
            CompoundTag keyedSections = new CompoundTag();
            for (Tag t : chunk.getList("sections", Tag.TAG_COMPOUND)) {
                CompoundTag section = (CompoundTag) t;
                int index = section.getByte("Y") + 4;
                section.remove("Y");
                keyedSections.put(Integer.toString(index), section);
            }
            chunk.put("sections", keyedSections);
            CompoundTag chunks = new CompoundTag();
            chunks.put(Long.toString(new ChunkPos(0, 0).toLong()), chunk);
            CompoundTag plot = new CompoundTag();
            plot.put("chunks", chunks);
            CompoundTag ship = new CompoundTag();
            ship.put("plot", plot);
            ListTag cargo = new ListTag();
            cargo.add(DeFixtures.stack("hbm:wire_fine_mingrade", 5));
            ship.put("cargo", cargo);

            Ctx ctx = ctx(world, null);
            Airships.convert(ctx, "minecraft:overworld", ship, "test airship");
            CompoundTag out = ship.getCompound("plot").getCompound("chunks").getCompound(Long.toString(new ChunkPos(0, 0).toLong()));
            helper.assertTrue(!out.contains("xPos") && out.get("sections") instanceof CompoundTag,
                    "the plot chunk keeps Sable's shape: " + out.getAllKeys());
            ListTag asList = new ListTag();
            for (String index : out.getCompound("sections").getAllKeys()) {
                CompoundTag section = out.getCompound("sections").getCompound(index).copy();
                section.putByte("Y", (byte) (Integer.parseInt(index) - 4));
                asList.add(section);
            }
            out = out.copy();
            out.put("sections", asList);
            Map<BlockPos, CompoundTag> states = states(out);
            expectBlock(helper, states, origin, "hbm:machine_centrifuge");
            helper.assertTrue(states.containsKey(origin.above()) && !states.get(origin.above()).getString("Name").equals("minecraft:air"),
                    "the centrifuge gets its cells inside the plot");
            helper.assertTrue(ctx.report.get("airship cells placed") > 0, "cells were placed");
            helper.assertTrue(out.toString().contains("hbm:canister_full"), "the ship's chest converts: " + out);
            helper.assertTrue(ship.getList("cargo", Tag.TAG_COMPOUND).getCompound(0).getString("id").equals("hbm:wire_fine_red_copper"),
                    "stacks outside the plot convert: " + ship);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
        helper.succeed();
    }

    /** Every kind of data Continuum would drop on load is parked instead, checked against Continuum's own codecs. */
    private static void nothingLostSilently(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            BlockPos pos = new BlockPos(2, 70, 2);
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(chunk, pos, DeFixtures.block("hbm:machine_centrifuge", "facing", "north", "bogus", "x"));
            CompoundTag be = DeFixtures.blockEntity("hbm:centrifuge", pos);
            be.putInt("deOnlyField", 7);
            CompoundTag sword = DeFixtures.stack("minecraft:iron_sword", 1);
            CompoundTag swordComponents = new CompoundTag();
            swordComponents.putString("minecraft:damage", "not a number");
            swordComponents.putInt("minecraft:repair_cost", 3);
            sword.put("components", swordComponents);
            be.put("inventory", DeFixtures.inventory(DeFixtures.slot(0, sword), DeFixtures.slot(1, DeFixtures.stack("hbm:not_an_item", 2))));
            DeFixtures.addBlockEntity(chunk, be);
            ListTag entities = new ListTag();
            entities.add(entity("hbm:not_an_entity"));
            CompoundTag zombie = entity("minecraft:zombie");
            CompoundTag attachments = new CompoundTag();
            CompoundTag unknown = new CompoundTag();
            unknown.putInt("a", 1);
            attachments.put("hbm:unknown_attachment", unknown);
            zombie.put("neoforge:attachments", attachments);
            entities.add(zombie);
            chunk.put("entities", entities);
            CompoundTag chunkAttachments = new CompoundTag();
            chunkAttachments.put("hbm:de_only", unknown.copy());
            chunk.put("neoforge:attachments", chunkAttachments);

            Ctx ctx = ctx(world, helper.getLevel().getServer());
            CompoundTag out = new ChunkConverter(ctx, "minecraft:overworld").convert(chunk);
            Map<String, List<String>> parked = new HashMap<>();
            for (Tag t : ctx.parking.records()) {
                CompoundTag r = (CompoundTag) t;
                parked.computeIfAbsent(r.getString("kind"), k -> new ArrayList<>()).add(r.getString("where") + " " + r.getString("id"));
            }
            helper.assertTrue(parked.getOrDefault("block_entity_field", List.of()).stream().anyMatch(w -> w.contains("deOnlyField")),
                    "a field Continuum's centrifuge does not keep is parked: " + parked);
            helper.assertTrue(parked.getOrDefault("component", List.of()).stream().anyMatch(w -> w.contains("minecraft:damage")),
                    "a component Continuum cannot decode is parked: " + parked);
            helper.assertTrue(parked.getOrDefault("item", List.of()).stream().anyMatch(w -> w.contains("hbm:not_an_item")),
                    "an unknown item is parked: " + parked);
            helper.assertTrue(parked.getOrDefault("block_state", List.of()).stream().anyMatch(w -> w.contains("machine_centrifuge")),
                    "a property the block does not have is parked: " + parked);
            helper.assertTrue(parked.getOrDefault("entity", List.of()).stream().anyMatch(w -> w.contains("hbm:not_an_entity")),
                    "an unknown HBM entity is parked: " + parked);
            helper.assertTrue(parked.getOrDefault("chunk_attachment", List.of()).stream().anyMatch(w -> w.contains("hbm:de_only")),
                    "a DE chunk attachment is parked: " + parked);
            String inv = out.getList("block_entities", Tag.TAG_COMPOUND).getCompound(0).toString();
            helper.assertTrue(inv.contains("minecraft:iron_sword") && inv.contains("repair_cost") && !inv.contains("not a number"),
                    "the sword stays with the components Continuum reads: " + inv);
            helper.assertTrue(ctx.report.get("block entities checked") == 1, "the centrifuge was checked against Continuum");

            helper.assertTrue(ctx.entities.size() == 1, "the zombie with HBM data is kept for the live check: " + ctx.entities);
            Map<String, Tag> lost = ContinuumTarget.entityLost(helper.getLevel(), ctx.entities.get(0).getCompound("nbt"), false);
            helper.assertTrue(lost != null && lost.keySet().stream().anyMatch(k -> k.contains("hbm:unknown_attachment")),
                    "an attachment Continuum does not register is found by the live check: " + lost);

            // An effect's cures are a set: saved in another order, nothing is lost.
            CompoundTag de = new CompoundTag(), out2 = new CompoundTag();
            ListTag cures = new ListTag(), reordered = new ListTag();
            cures.add(StringTag.valueOf("milk"));
            cures.add(StringTag.valueOf("protected_by_totem"));
            reordered.add(StringTag.valueOf("protected_by_totem"));
            reordered.add(StringTag.valueOf("milk"));
            de.put("neoforge:cures", cures);
            out2.put("neoforge:cures", reordered);
            helper.assertTrue(NbtDiff.lost(de, out2, Set.of()).isEmpty(), "reordered cures are not a loss");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
        helper.succeed();
    }

    /**
     * Families the field audit found saving differently, written as DE writes them: Continuum's own block entities keep
     * every field (nothing parked) and read the values DE meant.
     */
    private static void auditedMappings(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            Map<BlockPos, CompoundTag> planted = new HashMap<>();
            BlockPos barrel = new BlockPos(1, 70, 1);
            CompoundTag b = place(chunk, planted, barrel, DeFixtures.block("hbm:barrel_steel"), "hbm:fluid_barrel");
            b.putInt("mode", 2);
            BlockPos redd = new BlockPos(4, 70, 1);
            CompoundTag r = place(chunk, planted, redd, DeFixtures.block("hbm:machine_battery_redd", "facing", "north"), "hbm:battery_redd");
            r.putInt("redLow", 1);
            r.putInt("redHigh", 3);
            r.putInt("priority", 3);
            BlockPos globe = new BlockPos(7, 70, 1);
            place(chunk, planted, globe, DeFixtures.block("hbm:snowglobe"), "hbm:snowglobe").putByte("type", (byte) 3);
            BlockPos tele = new BlockPos(10, 70, 1);
            CompoundTag t = place(chunk, planted, tele, DeFixtures.block("hbm:machine_teleporter"), "hbm:teleblock");
            CompoundTag target = new CompoundTag();
            target.putString("dimension", "minecraft:the_nether");
            target.putIntArray("pos", new int[]{10, 64, -20});
            t.put("target", target);
            BlockPos gascent = new BlockPos(1, 70, 6);
            CompoundTag g = place(chunk, planted, gascent, DeFixtures.block("hbm:machine_gascent", "facing", "north"), "hbm:gas_centrifuge");
            CompoundTag in = new CompoundTag();
            in.putInt("fill", 800);
            in.putString("stage", "meuf6");
            g.put("inputTank", in);

            BlockPos pylon = new BlockPos(13, 70, 6);
            place(chunk, planted, pylon, DeFixtures.block("hbm:red_pylon"), "hbm:pylon_redwire");
            CompoundTag node = new CompoundTag();
            node.putLong("p", pylon.asLong());
            node.putLongArray("r", new long[]{new BlockPos(30, 70, 6).asLong()});
            ListTag nodes = new ListTag();
            nodes.add(node);
            CompoundTag graph = new CompoundTag();
            graph.put("nodes", nodes);
            CompoundTag inner = new CompoundTag();
            inner.put("data", graph);
            CompoundTag graphRoot = new CompoundTag();
            graphRoot.put("data", inner);
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(graphRoot, world.resolve("data/hbm_power_graph.dat"));

            ListTag entities = new ListTag();
            CompoundTag missile = entity("hbm:missile_generic");
            entities.add(missile);
            CompoundTag debris = entity("hbm:zirnox_debris");
            debris.putInt("debtype", 4);
            entities.add(debris);
            chunk.put("entities", entities);

            Ctx ctx = ctx(world, helper.getLevel().getServer());
            CompoundTag out = new ChunkConverter(ctx, "minecraft:overworld").convert(chunk);
            List<String> parked = new ArrayList<>();
            for (Tag rec : ctx.parking.records()) parked.add(((CompoundTag) rec).getString("kind") + " " + ((CompoundTag) rec).getString("where"));
            helper.assertTrue(parked.isEmpty(), "every audited field has a Continuum home: " + parked);
            helper.assertTrue(ctx.report.get("block entities checked") == planted.size(),
                    "each planted block entity was checked: " + ctx.report.get("block entities checked"));

            Map<BlockPos, CompoundTag> bes = new HashMap<>();
            for (Tag be : out.getList("block_entities", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) be;
                bes.put(new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z")), c);
            }
            helper.assertTrue(bes.get(barrel).get("mode") instanceof ShortTag && bes.get(barrel).getShort("mode") == 2,
                    "the barrel's mode is cast to the short Continuum reads: " + bes.get(barrel));
            CompoundTag reddOut = bes.get(redd);
            helper.assertTrue("high".equals(reddOut.getString("priority")) && reddOut.get("redHigh") instanceof ShortTag,
                    "the REDD's priority and redstone modes: " + reddOut);
            helper.assertTrue("lucky38".equals(bes.get(globe).getString("type")), "snowglobe kind by name: " + bes.get(globe));
            var teleTarget = bes.get(tele).getCompound("target");
            helper.assertTrue("minecraft:the_nether".equals(teleTarget.getString("dimension"))
                            && Arrays.equals(teleTarget.getIntArray("pos"), new int[]{10, 64, -20}),
                    "teleporter target: " + bes.get(tele));
            helper.assertTrue(bes.get(gascent).getInt("inputTank") == 800 && "MEUF6".equals(bes.get(gascent).getString("inputTank_type")),
                    "gas centrifuge tank: " + bes.get(gascent));
            helper.assertTrue(bes.get(pylon).getInt("conCount") == 1 && bes.get(pylon).getIntArray("con0")[0] == 30,
                    "the pylon's wire from DE's power graph: " + bes.get(pylon));
            ListTag outEntities = out.getList("entities", Tag.TAG_COMPOUND);
            helper.assertTrue(outEntities.getCompound(0).getString("id").equals("hbm:entity_missile_generic"),
                    "DE's missile id: " + outEntities.getCompound(0).getString("id"));
            helper.assertTrue("concrete".equals(outEntities.getCompound(1).getString("type")), "zirnox debris kind: " + outEntities.getCompound(1));

            CompoundTag lore = componentStack("hbm:book_lore");
            CompoundTag lc = lore.getCompound("components");
            lc.putString("hbm:lore_book_key", "book_lore.resignation_note");
            lc.putInt("hbm:lore_book_pages", 3);
            lc.putInt("hbm:lore_book_cover", 0x123456);
            lc.putInt("hbm:lore_book_title", 0xABCDEF);
            CompoundTag args = new CompoundTag();
            ListTag pageArgs = new ListTag();
            pageArgs.add(StringTag.valueOf("Kelvin"));
            args.put("1", pageArgs);
            lc.put("hbm:lore_book_args", args);
            CompoundTag pellet = componentStack("hbm:icf_pellet");
            CompoundTag icf = new CompoundTag();
            icf.putInt("type1", 1);
            icf.putInt("type2", 2);
            icf.putBoolean("muon", true);
            icf.putLong("depletion", 77L);
            pellet.getCompound("components").put("hbm:icf_pellet", icf);
            CompoundTag pick = componentStack("hbm:schrabidium_pickaxe");
            ListTag presets = new ListTag();
            CompoundTag preset = new CompoundTag();
            preset.putString("area", "hammer");
            preset.putInt("areaLevel", 1);
            preset.putString("harvest", "silk");
            presets.add(preset);
            pick.getCompound("components").put("hbm:tool_presets", presets);
            pick.getCompound("components").putInt("hbm:tool_preset_index", 1);
            Walker walker = new Walker(ctx);
            for (CompoundTag stack : List.of(lore, pellet, pick)) walker.walk(stack, "test");
            CompoundTag book = lore.getCompound("components").getCompound("hbm:book_lore");
            helper.assertTrue(book.getInt("p") == 3 && "Kelvin".equals(book.getCompound("p1").getString("a1")), "lore book: " + lore);
            helper.assertTrue("deuterium".equals(pellet.getCompound("components").getString("hbm:icf_type1"))
                    && pellet.getCompound("components").getLong("hbm:icf_depletion") == 77L, "ICF pellet: " + pellet);
            CompoundTag ability = pick.getCompound("components").getCompound("hbm:tool_ability");
            helper.assertTrue(ability.getInt("ability") == 1 && "tool.ability.silktouch".equals(
                    ability.getList("abilityPresets", Tag.TAG_COMPOUND).getCompound(0).getString("harvest")), "tool presets: " + pick);
            helper.assertTrue(ctx.parking.records().isEmpty(), "no component was parked: " + ctx.parking.records());

            // block states DE spells differently: parts of tall machines, 1.7 metadata facings, renamed flags, volcano modes
            helper.assertTrue(BlockStates.convert(DeFixtures.block("hbm:rbmk_fuel_rod", "part", "column"), ctx.target).kind() == BlockStates.Kind.CELL,
                    "an RBMK column block above its core is a cell");
            CompoundTag holder = BlockStates.convert(DeFixtures.block("hbm:skeleton_holder", "facing", "3"), ctx.target).entry();
            helper.assertTrue("south".equals(holder.getCompound("Properties").getString("facing")), "metadata facing to a name: " + holder);
            CompoundTag bomb = BlockStates.convert(DeFixtures.block("hbm:nuke_fstbmb", "facing", "west"), ctx.target).entry();
            helper.assertTrue("4".equals(bomb.getCompound("Properties").getString("facing")), "a name to metadata facing: " + bomb);
            CompoundTag light = BlockStates.convert(DeFixtures.block("hbm:floodlight", "facing", "7"), ctx.target).entry();
            helper.assertTrue("up".equals(light.getCompound("Properties").getString("facing"))
                    && "true".equals(light.getCompound("Properties").getString("flip")), "floodlight flip: " + light);
            BlockStates.Converted fuel = BlockStates.convert(DeFixtures.block("hbm:block_graphite_fuel", "axis", "y", "shrouded", "true", "pu239", "true"), ctx.target);
            helper.assertTrue("true".equals(fuel.entry().getCompound("Properties").getString("aluminum"))
                    && "true".equals(fuel.entry().getCompound("Properties").getString("out")) && fuel.dropped().isEmpty(), "graphite flags: " + fuel);
            helper.assertTrue(BlockStates.convert(DeFixtures.block("hbm:volcano_core", "mode", "smoldering"), ctx.target).entry()
                    .getString("Name").equals("hbm:volcano_core_smoldering"), "volcano mode to its block");
            CompoundTag vent = BlockStates.convert(DeFixtures.block("hbm:pile_vent", "facing", "west"), ctx.target).entry();
            helper.assertTrue("6".equals(vent.getCompound("Properties").getString("meta")), "pile device kind and side in one meta: " + vent);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
        helper.succeed();
    }

    /** Lazy chest loot, a spawner's mob, a falling block's state and a stat objective, all outside the chunk palette. */
    private static void lootAndReferences(GameTestHelper helper) {
        Path world = tempWorld();
        try {
            MinecraftServer server = helper.getLevel().getServer();
            BlockPos chestPos = new BlockPos(2, 70, 2), safePos = new BlockPos(4, 70, 2), spawnerPos = new BlockPos(6, 70, 2);
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            Map<BlockPos, CompoundTag> planted = new HashMap<>();
            place(chunk, planted, chestPos, DeFixtures.block("minecraft:chest", "facing", "north", "type", "single", "waterlogged", "false"),
                    "minecraft:chest").putString("LootTable", "hbm:chests/ammo_10_15");
            CompoundTag safe = place(chunk, planted, safePos, DeFixtures.block("hbm:safe", "facing", "north"), "hbm:safe");
            safe.putString("LootTable", "hbm:chests/component/unscaled/vault_rusty_3_6");
            safe.putLong("LootTableSeed", 42L);
            CompoundTag mob = new CompoundTag();
            mob.putString("id", "hbm:entity_missile_anti");
            CompoundTag spawnData = new CompoundTag();
            spawnData.put("entity", mob);
            place(chunk, planted, spawnerPos, DeFixtures.block("minecraft:spawner"), "minecraft:mob_spawner").put("SpawnData", spawnData);
            CompoundTag falling = entity("minecraft:falling_block");
            falling.put("BlockState", DeFixtures.block("hbm:concrete_black"));
            ListTag entities = new ListTag();
            entities.add(falling);
            chunk.put("entities", entities);

            Ctx ctx = ctx(world, server);
            CompoundTag out = new ChunkConverter(ctx, "minecraft:overworld").convert(chunk);
            Map<BlockPos, CompoundTag> bes = new HashMap<>();
            for (Tag t : out.getList("block_entities", Tag.TAG_COMPOUND)) {
                CompoundTag be = (CompoundTag) t;
                bes.put(new BlockPos(be.getInt("x"), be.getInt("y"), be.getInt("z")), be);
            }
            helper.assertTrue(bes.get(chestPos).getString("LootTable").equals("hbm_de:chests/ammo_10_15"),
                    "a chest keeps its lazy DE loot, by the table served for it: " + bes.get(chestPos));
            helper.assertTrue(!bes.get(safePos).contains("LootTable") && ctx.loot.size() == 1
                    && ctx.loot.get(0).getString("table").equals("hbm_de:chests/component/unscaled/vault_rusty_3_6")
                    && ctx.loot.get(0).getLong("seed") == 42L, "a safe's lazy loot waits for the live phase: " + ctx.loot);
            helper.assertTrue(ctx.parking.counts().getOrDefault("block_entity_field", 0) == 0, "nothing parked: " + ctx.parking.counts());
            helper.assertTrue(bes.get(spawnerPos).getCompound("SpawnData").getCompound("entity").getString("id").equals("hbm:entity_missile_ab"),
                    "the spawner's mob follows the rename: " + bes.get(spawnerPos));
            CompoundTag state = out.getList("entities", Tag.TAG_COMPOUND).getCompound(0).getCompound("BlockState");
            helper.assertTrue(state.getString("Name").equals("hbm:concrete_colored")
                    && state.getCompound("Properties").getString("color").equals("black"), "a falling block's state converts: " + state);

            for (String table : List.of("hbm_de:chests/ammo_10_15", "hbm_de:chests/component/antenna_5_5")) {
                LootTable loot = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(table)));
                SimpleContainer container = new SimpleContainer(27);
                loot.fill(container, new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                        .create(LootContextParamSets.CHEST), 7L);
                helper.assertTrue(loot != LootTable.EMPTY && !container.isEmpty(), "the served table loads and rolls: " + table);
            }

            BlockPos live = helper.absolutePos(new BlockPos(1, 1, 1));
            helper.getLevel().setBlockAndUpdate(live, ModDecoCrateBlocks.safe.get().defaultBlockState());
            ctx.loot.get(0).putLong("pos", live.asLong());
            new LiveStep().run(ctx, server);
            helper.assertTrue(helper.getLevel().getBlockEntity(live) instanceof Container rolled && !rolled.isEmpty(),
                    "the live phase rolls the safe's loot into Continuum's safe");

            CompoundTag objective = new CompoundTag();
            objective.putString("Name", "concrete");
            objective.putString("CriteriaName", "minecraft.mined:hbm.concrete_black");
            ListTag objectives = new ListTag();
            objectives.add(objective);
            CompoundTag data = new CompoundTag();
            data.put("Objectives", objectives);
            CompoundTag root = new CompoundTag();
            root.put("data", data);
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(root, world.resolve("data/scoreboard.dat"));
            new SavedDataStep().run(ctx, server);
            String criteria = NbtIo.readCompressed(world.resolve("data/scoreboard.dat"), NbtAccounter.unlimitedHeap())
                    .getCompound("data").getList("Objectives", Tag.TAG_COMPOUND).getCompound(0).getString("CriteriaName");
            helper.assertTrue(criteria.equals("minecraft.mined:hbm.concrete_colored"), "a stat objective follows the block: " + criteria);
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(world);
        }
    }

    private static CompoundTag place(CompoundTag chunk, Map<BlockPos, CompoundTag> planted, BlockPos pos, CompoundTag state, String deType) {
        DeFixtures.setBlock(chunk, pos, state);
        CompoundTag be = DeFixtures.blockEntity(deType, pos);
        DeFixtures.addBlockEntity(chunk, be);
        planted.put(pos, be);
        return be;
    }

    private static CompoundTag componentStack(String id) {
        CompoundTag stack = DeFixtures.stack(id, 1);
        stack.put("components", new CompoundTag());
        return stack;
    }

    /** A live loot record for a chest at {@code pos}, rolling a vanilla table with a fixed seed. */
    static CompoundTag loot(ServerLevel level, BlockPos pos) {
        CompoundTag loot = new CompoundTag();
        loot.putString("dim", level.dimension().location().toString());
        loot.putLong("pos", pos.asLong());
        loot.putString("table", "minecraft:chests/simple_dungeon");
        loot.putLong("seed", 7L);
        return loot;
    }

    /** The non-empty slots of the container at {@code pos}. */
    static int stacks(ServerLevel level, BlockPos pos) {
        int stacks = 0;
        if (level.getBlockEntity(pos) instanceof Container container)
            for (int i = 0; i < container.getContainerSize(); i++) if (!container.getItem(i).isEmpty()) stacks++;
        return stacks;
    }

    static CompoundTag entity(String id) {
        CompoundTag e = new CompoundTag();
        e.putString("id", id);
        ListTag pos = new ListTag();
        for (double d : new double[]{4.5, 70, 4.5}) pos.add(DoubleTag.valueOf(d));
        e.put("Pos", pos);
        return e;
    }

    private static void writeWrapped(Path world, String name, CompoundTag inner) throws IOException {
        CompoundTag outer = new CompoundTag();
        outer.put("data", inner);
        CompoundTag root = new CompoundTag();
        root.put("data", outer);
        Files.createDirectories(world.resolve("data"));
        NbtIo.writeCompressed(root, world.resolve("data/" + name + ".dat"));
    }

    static Path tempWorld() {
        try {
            return Files.createTempDirectory("hbm-deimport");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void delete(Path world) {
        try (Stream<Path> files = Files.walk(world)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
        } catch (IOException e) {
            // a temp folder left behind is harmless; the test result stands
        }
    }
}
