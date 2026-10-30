package com.hbm.gametest.deimport;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.capability.MachineCaps;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.PackedSection;
import com.hbm.deimport.engine.Report;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.source.de7.BlockStates;
import com.hbm.deimport.steps.AirshipPlotsStep;
import com.hbm.deimport.steps.SubLevelsStep;
import com.hbm.deimport.target.ContinuumTarget;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelRegionFile;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelStorageFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/** Sable's own storage files; only loaded while Sable is installed. */
public final class DeSableGameTests {
    private DeSableGameTests() {}

    /**
     * A ship outside an addon's dimensions gets the addon's block back from DE's copy (a stand-in id), matched by ship
     * id; the backup's file is only read, through a copy that goes again. Storage files Sable cannot open, on either
     * side, and a ship the backup does not have are passed over.
     */
    static void merge(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        Path backup = world.resolve("backup");
        try {
            UUID id = new UUID(4, 2);
            BlockPos lost = new BlockPos(1, 70, 1);
            CompoundTag before = DeFixtures.emptyChunk(0, 0);
            DeFixtures.setBlock(before, lost, DeFixtures.block("testaddon:machine"));
            CompoundTag de = DeFixtures.ship(before), now = DeFixtures.ship(DeFixtures.emptyChunk(0, 0));
            de.putUUID("uuid", id);
            now.putUUID("uuid", id);
            Path deFile = write(backup.resolve("sublevels"), de), liveFile = write(world.resolve("sublevels"), now);
            CompoundTag other = DeFixtures.ship(DeFixtures.emptyChunk(0, 0));
            other.putUUID("uuid", new UUID(4, 3));
            try (SubLevelStorageFile storage = new SubLevelStorageFile(liveFile, liveFile.resolveSibling("r.0.0.r"))) {
                storage.write(1, other);
                // a second copy of the ship, as a crash in Sable's move of it leaves; its holding chunk names the first
                storage.write(3, now);
                storage.flush();
            }
            holding(world.resolve("sublevels"), 0, 1, 2);
            // a ship whose plot no converter can read: its section's data is too short for its palette
            CompoundTag broken = DeFixtures.ship(before), brokenNow = DeFixtures.ship(DeFixtures.emptyChunk(0, 0));
            broken.getCompound("plot").getCompound("chunks").getCompound(Long.toString(ChunkPos.asLong(0, 0))).getCompound("sections")
                    .getCompound(Integer.toString((lost.getY() >> 4) + 4)).getCompound("block_states").putLongArray("data", new long[1]);
            broken.putUUID("uuid", new UUID(4, 4));
            brokenNow.putUUID("uuid", new UUID(4, 4));
            try (SubLevelStorageFile storage = new SubLevelStorageFile(deFile, deFile.resolveSibling("r.0.0.r"))) {
                storage.write(1, broken);
                storage.flush();
            }
            try (SubLevelStorageFile storage = new SubLevelStorageFile(liveFile, liveFile.resolveSibling("r.0.0.r"))) {
                storage.write(2, brokenNow);
                storage.flush();
            }
            byte[] kept = Files.readAllBytes(deFile);
            Files.createDirectories(backup.resolve("sublevels/r.1.0.0.slvls"));
            Ctx ctx = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            int ships = SubLevelsStep.merge(ctx, "minecraft:overworld", world.resolve("sublevels"), backup.resolve("sublevels"), s -> s.startsWith("testaddon:"));
            CompoundTag out, untouched, copy;
            try (SubLevelStorageFile storage = new SubLevelStorageFile(liveFile, liveFile.resolveSibling("r.0.0.r"))) {
                out = storage.read(0);
                untouched = storage.read(1);
                copy = storage.read(3);
            }
            CompoundTag section = out.getCompound("plot").getCompound("chunks").getCompound(Long.toString(ChunkPos.asLong(0, 0)))
                    .getCompound("sections").getCompound(Integer.toString((lost.getY() >> 4) + 4));
            helper.assertTrue(ships == 1 && new PackedSection(section.getCompound("block_states")).palette.stream()
                    .anyMatch(t -> ((CompoundTag) t).getString("Name").equals("testaddon:machine")) && now.equals(copy),
                    "the ship gets its block back, the copy of it Sable left and never loads stays: " + ships);
            Path scratch = world.resolve("sublevels.de-plots");
            Map<Long, UUID> plots = SubLevelsStep.plots(world.resolve("sublevels"), scratch);
            helper.assertTrue(plots != null && plots.isEmpty() && !Files.exists(scratch), "a plot three ships claim tells nothing: " + plots);
            helper.assertTrue(other.equals(untouched), "a ship the backup does not have stays as it was");
            ctx.report.append(world, "test");
            String report = Files.readString(world.resolve(Report.FILE));
            helper.assertTrue(report.contains("backup airships in r.1.0.0.slvls") && report.contains("airship " + new UUID(4, 4) + " could not get its blocks back"),
                    "a backup file Sable cannot open and a ship nothing can read are reported, the rest merged: " + report);

            // a live file Sable cannot open is tried again on the next start; what came back stays as it is
            Files.createDirectories(world.resolve("sublevels/r.1.0.0.slvls"));
            Ctx next = DeImportGameTests.ctx(world, helper.getLevel().getServer());
            int again = SubLevelsStep.merge(next, "minecraft:overworld", world.resolve("sublevels"), backup.resolve("sublevels"), s -> s.startsWith("testaddon:"));
            CompoundTag twice;
            try (SubLevelStorageFile storage = new SubLevelStorageFile(liveFile, liveFile.resolveSibling("r.0.0.r"))) {
                twice = storage.read(0);
            }
            next.report.append(world, "test again");
            report = Files.readString(world.resolve(Report.FILE));
            helper.assertTrue(again == -1 && report.contains("airships in r.1.0.0.slvls cannot be opened") && out.equals(twice),
                    "a live file Sable cannot open asks for another start: " + again);

            // played since: the block that came back is mined; the next start merges only what it did not finish
            write(world.resolve("sublevels"), now);
            Files.delete(world.resolve("sublevels/r.1.0.0.slvls"));
            int third = SubLevelsStep.merge(DeImportGameTests.ctx(world, helper.getLevel().getServer()), "minecraft:overworld",
                    world.resolve("sublevels"), backup.resolve("sublevels"), s -> s.startsWith("testaddon:"));
            helper.assertTrue(third == 0 && now.equals(read(liveFile, 0)), "a ship merged once is not merged again: " + third);
            helper.assertTrue(Arrays.equals(kept, Files.readAllBytes(deFile)) && !Files.exists(world.resolve("sublevels.de-backup")),
                    "the backup's file is untouched and the copy is gone");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            DeImportGameTests.delete(world);
        }
    }

    /**
     * A world imported before airships converted on Sable's grid: a cable whose block entity sits at the cable's place in
     * the plot, a centrifuge whose records were put at that place too. The repair moves the block entity, records both
     * again on the grid, and the live phase takes out what was put near the origin, never a real cable's node.
     */
    static void plotsRepaired(GameTestHelper helper) {
        Path world = DeImportGameTests.tempWorld();
        ServerLevel level = helper.getLevel();
        BlockPos phantom = helper.absolutePos(new BlockPos(1, 1, 1)), real = helper.absolutePos(new BlockPos(2, 1, 1)), cell = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos owned = helper.absolutePos(new BlockPos(3, 1, 1));
        try {
            Ctx earlier = DeImportGameTests.ctx(world, level.getServer());
            String dim = level.dimension().location().toString();
            int dx = 10000 << 7 << 4;
            BlockPos cable = new BlockPos(1, 64, 1), core = new BlockPos(5, 64, 5);
            // an older run cut short before its live phase: its records at the plot place wait in a shard
            Unit cut = earlier.begin("airships " + dim + " r.0.0.0.slvls");
            CompoundTag cutCells = new CompoundTag();
            cutCells.putString("dim", dim);
            cutCells.putLong("pos", core.asLong());
            cutCells.putLongArray("cells", new long[]{core.above().asLong()});
            earlier.add("plotcells", cutCells);
            earlier.conductor(dim, cable.asLong(), true);
            earlier.commit(cut);
            Ctx ctx = DeImportGameTests.ctx(world, level.getServer());
            CompoundTag chunk = DeFixtures.emptyChunk(0, 0);
            CompoundTag cableState = BlockStates.convert(DeFixtures.block("hbm:cable"), ctx.target).entry();
            DeFixtures.setBlock(chunk, cable, cableState);
            CompoundTag stray = DeFixtures.blockEntity(ctx.target.blockEntityType(cableState.getString("Name"), new CompoundTag()), cable);
            DeFixtures.addBlockEntity(chunk, stray);
            CompoundTag machine = BlockStates.convert(DeFixtures.block("hbm:machine_centrifuge", "facing", "north"), ctx.target).entry();
            DeFixtures.setBlock(chunk, core, machine);
            Map<Long, CompoundTag> footprint = ctx.target.footprint(machine.getString("Name"), machine.getCompound("Properties"),
                    core.offset(dx, 0, dx).asLong(), ctx.rbmkColumnHeight());
            for (Map.Entry<Long, CompoundTag> c : footprint.entrySet()) DeFixtures.setBlock(chunk, BlockPos.of(c.getKey()), c.getValue());
            CompoundTag ship = DeFixtures.ship(chunk);
            ship.putUUID("uuid", new UUID(5, 1));
            Path folder = world.resolve("sublevels");
            Path file = write(folder, ship);
            // an older importer marked each storage file, not the folder
            ctx.journal.mark("sublevels@1", world, file);
            new AirshipPlotsStep().run(ctx, level.getServer());
            CompoundTag be = read(file, 0).getCompound("plot").getCompound("chunks").getCompound(Long.toString(ChunkPos.asLong(0, 0)))
                    .getList("block_entities", Tag.TAG_COMPOUND).getCompound(0);
            helper.assertTrue(be.getInt("x") == cable.getX() + dx && be.getInt("z") == cable.getZ() + dx,
                    "the cable's block entity goes to the cable's place on Sable's grid: " + be);
            helper.assertTrue(ctx.conductors.size() == 1 && ctx.conductors.get(0).getLong("pos") == cable.offset(dx, 0, dx).asLong()
                    && ctx.conductors.get(0).getBoolean("plot") && ctx.plotCells.size() == 1
                    && ctx.plotCells.get(0).getLong("pos") == core.offset(dx, 0, dx).asLong() && ctx.plotCells.get(0).getLongArray("cells").length == footprint.size(),
                    "the cable and the centrifuge are recorded there, its cells as cells and never as cores: " + ctx.conductors + " " + ctx.plotCells);
            helper.assertTrue(ctx.stale.size() == 2 && ctx.stale.stream().anyMatch(t -> t.getLong("pos") == cable.asLong() && t.getBoolean("conductor"))
                    && ctx.stale.stream().anyMatch(t -> t.getLong("pos") == core.asLong() && t.getLongArray("cells").length == footprint.size())
                    && ctx.journal.done(AirshipPlotsStep.ID, world, folder) && ctx.report.get("misplaced airship records of an earlier run moved to the leftovers") == 2,
                    "what the older import put at the plot place goes, the cut-short run's records too: " + ctx.stale);

            PowerGraph.get(level).addNode(phantom.asLong(), CableData.INSTANCE, MachineCaps.FACE_MASK_ALL);
            MultiblockCoreIndex.put(level, cell, phantom);
            CompoundTag lost = new CompoundTag();
            lost.putBoolean("conductor", true);
            lost.putLongArray("cells", new long[]{cell.asLong()});
            helper.setBlock(new BlockPos(2, 1, 1), BuiltInRegistries.BLOCK.get(ResourceLocation.parse(cableState.getString("Name"))));
            PowerGraph.get(level).addNode(real.asLong(), CableData.INSTANCE, MachineCaps.FACE_MASK_ALL);
            CompoundTag standing = new CompoundTag();
            standing.putBoolean("conductor", true);
            helper.setBlock(new BlockPos(3, 1, 1), Blocks.CHEST);
            PowerGraph.get(level).addNode(owned.asLong(), CableData.INSTANCE, MachineCaps.FACE_MASK_ALL);
            int removed = ContinuumTarget.dropStale(level, phantom, lost) + ContinuumTarget.dropStale(level, real, standing)
                    + ContinuumTarget.dropStale(level, owned, standing);
            helper.assertTrue(removed == 2 && !PowerGraph.get(level).containsCell(phantom.asLong()) && MultiblockCoreIndex.lookup(level, cell) != phantom.asLong()
                    && PowerGraph.get(level).containsCell(real.asLong()) && PowerGraph.get(level).containsCell(owned.asLong()),
                    "a misplaced node and index entry go, a real cable's node and a block entity's stay: " + removed);

            // a folder holding a conversion an earlier run made is left for the repair
            Path flagged = world.resolve("DIM-1/sublevels");
            CompoundTag marked = DeFixtures.ship(DeFixtures.emptyChunk(0, 0));
            marked.putBoolean("hbm_de_converted", true);
            write(flagged, marked);
            SubLevelsStep.convert(ctx, "minecraft:the_nether", flagged);
            helper.assertTrue(ctx.journal.done("sublevels@1", world, flagged) && !ctx.journal.done(AirshipPlotsStep.ID, world, flagged),
                    "a folder with an earlier run's conversions waits for the repair");
            helper.succeed();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            PowerGraph.get(level).removeNode(phantom.asLong());
            PowerGraph.get(level).removeNode(real.asLong());
            PowerGraph.get(level).removeNode(owned.asLong());
            MultiblockCoreIndex.remove(level, cell);
            DeImportGameTests.delete(world);
        }
    }

    /** A holding region file {@code r.0.0.slvlr} in {@code folder} naming {@code indices} of {@code r.0.0.0.slvls}. */
    private static void holding(Path folder, int... indices) throws IOException {
        CompoundTag chunk = new CompoundTag();
        chunk.putIntArray("pointers", indices);
        try (SubLevelRegionFile region = new SubLevelRegionFile(folder.resolve("r.0.0.slvlr"), folder.resolve("r.0.0.r"))) {
            region.write(0, chunk);
            region.flush();
        }
    }

    /** A storage file {@code r.0.0.0.slvls} in {@code folder} holding {@code ship} at index 0. */
    public static Path write(Path folder, CompoundTag ship) throws IOException {
        Files.createDirectories(folder);
        Path file = folder.resolve("r.0.0.0.slvls");
        try (SubLevelStorageFile storage = new SubLevelStorageFile(file, folder.resolve("r.0.0.r"))) {
            storage.write(0, ship);
            storage.flush();
        }
        return file;
    }

    public static CompoundTag read(Path file, int index) throws IOException {
        try (SubLevelStorageFile storage = new SubLevelStorageFile(file, file.resolveSibling("r.0.0.r"))) {
            return storage.read(index);
        }
    }
}
